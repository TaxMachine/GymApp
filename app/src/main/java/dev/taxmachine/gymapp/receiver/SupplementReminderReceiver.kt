package dev.taxmachine.gymapp.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dev.taxmachine.gymapp.db.GymDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class SupplementReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val dao = GymDatabase.getDatabase(context).gymDao()
        CoroutineScope(Dispatchers.IO).launch {
            val supplements = dao.getAllSupplements().first()
            if (supplements.isEmpty()) return@launch

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "supplement_reminders"
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Supplement Reminders", NotificationManager.IMPORTANCE_DEFAULT)
                notificationManager.createNotificationChannel(channel)
            }

            val morningSupps = supplements.filter { it.timing.name.startsWith("MORNING") }
            if (morningSupps.isNotEmpty()) {
                val contentText = morningSupps.joinToString("\n") { 
                    "${it.name}: ${it.dosage}${it.unit.label} (${it.timing.label})" 
                }
                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Morning Supplements")
                    .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
                
                notificationManager.notify(1, notification)
            }
        }
    }

    companion object {
        fun scheduleReminder(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SupplementReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                }
            }

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }
}