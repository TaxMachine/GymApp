package dev.taxmachine.gymapp.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.taxmachine.gymapp.db.AdministrationFrequency
import dev.taxmachine.gymapp.db.GymDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class SupplementReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleReminder(context)
            return
        }

        if (action != ACTION_MORNING_REMINDER && action != ACTION_NIGHT_REMINDER) {
            return
        }

        val type = if (action == ACTION_MORNING_REMINDER) "morning" else "night"
        val dao = GymDatabase.getDatabase(context).gymDao()
        
        CoroutineScope(Dispatchers.IO).launch {
            val allSupplements = dao.getAllSupplements().first().filter { it.isActive }
            if (allSupplements.isEmpty()) {
                scheduleReminder(context, forceNextDayMorning = (type == "morning"))
                return@launch
            }

            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val isMonWedFri = dayOfWeek == Calendar.MONDAY || dayOfWeek == Calendar.WEDNESDAY || dayOfWeek == Calendar.FRIDAY

            val supplementsToNotify = if (type == "morning") {
                allSupplements.filter { 
                    it.timing.name.startsWith("MORNING") && 
                    (it.frequency == AdministrationFrequency.EVERY_DAY || 
                     (it.frequency == AdministrationFrequency.EVERY_OTHER_DAY && isMonWedFri))
                }
            } else {
                allSupplements.filter { it.timing.name == "BEFORE_BED" }
            }

            if (supplementsToNotify.isNotEmpty()) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId = "supplement_reminders"
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(channelId, "Supplement Reminders", NotificationManager.IMPORTANCE_HIGH)
                    notificationManager.createNotificationChannel(channel)
                }

                val title = if (type == "morning") "Morning Supplements" else "Bedtime Supplements"
                val contentText = supplementsToNotify.joinToString("\n") { 
                    "${it.name}: ${it.dosage}${it.unit.label}" 
                }

                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()
                
                notificationManager.notify(if (type == "morning") 1 else 2, notification)
            }
            
            // Re-schedule. If we just did morning, ensure we don't trigger again today if there's another alarm
            scheduleReminder(context, forceNextDayMorning = (type == "morning"))
        }
    }

    companion object {
        private const val TAG = "SuppReminder"
        private const val ACTION_MORNING_REMINDER = "dev.taxmachine.gymapp.ACTION_MORNING_REMINDER"
        private const val ACTION_NIGHT_REMINDER = "dev.taxmachine.gymapp.ACTION_NIGHT_REMINDER"

        fun scheduleReminder(context: Context, forceNextDayMorning: Boolean = false) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // 1. Schedule Morning Reminder (Synced to Morning Alarm + 10 mins)
            val morningIntent = Intent(context, SupplementReminderReceiver::class.java).apply { 
                action = ACTION_MORNING_REMINDER
            }
            val morningPendingIntent = PendingIntent.getBroadcast(
                context, 1, morningIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val nextAlarm = alarmManager.nextAlarmClock
            var morningTime: Long = 0

            // Try to sync with system alarm if it's in the morning (4 AM to 11 AM)
            if (nextAlarm != null && !forceNextDayMorning) {
                val alarmCal = Calendar.getInstance().apply { timeInMillis = nextAlarm.triggerTime }
                val hour = alarmCal.get(Calendar.HOUR_OF_DAY)
                if (hour in 4..10 && nextAlarm.triggerTime > System.currentTimeMillis()) {
                    morningTime = nextAlarm.triggerTime + (10 * 60 * 1000)
                }
            }

            // Fallback if no suitable morning alarm is found
            if (morningTime <= System.currentTimeMillis()) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 8)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (before(Calendar.getInstance()) || forceNextDayMorning) {
                        add(Calendar.DATE, 1)
                    }
                }
                morningTime = cal.timeInMillis
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, morningTime, morningPendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, morningTime, morningPendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, morningTime, morningPendingIntent)
            }

            // 2. Schedule Night Reminder (10 PM)
            val nightIntent = Intent(context, SupplementReminderReceiver::class.java).apply { 
                action = ACTION_NIGHT_REMINDER
            }
            val nightPendingIntent = PendingIntent.getBroadcast(
                context, 2, nightIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val nightTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 22)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
            }.timeInMillis

            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nightTime, nightPendingIntent)
        }
    }
}
