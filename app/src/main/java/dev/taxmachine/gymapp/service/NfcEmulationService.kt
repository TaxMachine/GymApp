package dev.taxmachine.gymapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.nfc.cardemulation.HostApduService
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.taxmachine.gymapp.db.BadgeHistoryEntity
import dev.taxmachine.gymapp.db.GymDatabase
import dev.taxmachine.gymapp.utils.NfcUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BadgeUpdateEvent(val badgeName: String, val newCode: String)

/**
 * NfcEmulationService handles Host Card Emulation (HCE).
 */
class NfcEmulationService : HostApduService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return STATUS_FAILED

        val hexCommand = NfcUtils.bytesToHex(commandApdu)
        Log.d(TAG, "Command received: $hexCommand")

        if (hexCommand.startsWith(SELECT_AID_HEADER)) {
            Log.d(TAG, "SELECT AID detected.")
            return STATUS_SUCCESS
        }

        val activeBadgeId = currentBadgeId ?: return STATUS_FAILED
        val activeTagHex = activeTagData ?: return STATUS_FAILED
        val tagBytes = NfcUtils.hexToBytes(activeTagHex)

        // 1. Mifare / NTAG WRITE (Command 0xA2)
        if (commandApdu.size >= 6 && commandApdu[0] == CMD_MIFARE_WRITE) {
            val pageOffset = commandApdu[1].toInt() and 0xFF
            val byteOffset = pageOffset * MIFARE_PAGE_SIZE
            val newData = commandApdu.copyOfRange(2, 6)
            
            Log.d(TAG, "Mifare WRITE page $pageOffset: ${NfcUtils.bytesToHex(newData)}")
            
            if (byteOffset + MIFARE_PAGE_SIZE <= tagBytes.size) {
                val updatedBytes = tagBytes.copyOf()
                System.arraycopy(newData, 0, updatedBytes, byteOffset, MIFARE_PAGE_SIZE)
                updateBadgeData(activeBadgeId, activeTagHex, NfcUtils.bytesToHex(updatedBytes))
                return STATUS_SUCCESS
            }
        }

        // 2. Mifare / NTAG READ (Command 0x30)
        if (commandApdu.size >= 2 && commandApdu[0] == CMD_MIFARE_READ) {
            val pageOffset = commandApdu[1].toInt() and 0xFF
            val byteOffset = pageOffset * MIFARE_PAGE_SIZE
            if (byteOffset < tagBytes.size) {
                val responseSize = minOf(MIFARE_READ_RESPONSE_SIZE, tagBytes.size - byteOffset)
                val response = tagBytes.copyOfRange(byteOffset, byteOffset + responseSize)
                return response + STATUS_SUCCESS
            }
        }

        // 3. ISO 15693 (NFC-V) commands
        if (commandApdu.size >= 2) {
            val command = commandApdu[1].toInt() and 0xFF
            when (command) {
                CMD_ISO15693_INVENTORY -> {
                    val uid = if (tagBytes.size >= ISO15693_UID_SIZE) {
                        tagBytes.take(ISO15693_UID_SIZE).toByteArray()
                    } else {
                        ByteArray(ISO15693_UID_SIZE)
                    }
                    return byteArrayOf(0x00, 0x00, *uid) + STATUS_SUCCESS
                }
                CMD_ISO15693_WRITE_SINGLE_BLOCK -> {
                    val blockNumber = if (commandApdu.size >= 3) commandApdu[2].toInt() and 0xFF else 0
                    val offset = blockNumber * ISO15693_BLOCK_SIZE
                    if (commandApdu.size >= 7 && offset + ISO15693_BLOCK_SIZE <= tagBytes.size) {
                        val newData = commandApdu.copyOfRange(3, 7)
                        val updatedBytes = tagBytes.copyOf()
                        System.arraycopy(newData, 0, updatedBytes, offset, ISO15693_BLOCK_SIZE)
                        updateBadgeData(activeBadgeId, activeTagHex, NfcUtils.bytesToHex(updatedBytes))
                        return byteArrayOf(0x00) + STATUS_SUCCESS
                    }
                }
                CMD_ISO15693_READ_SINGLE_BLOCK -> {
                    val blockNumber = if (commandApdu.size >= 3) commandApdu[2].toInt() and 0xFF else 0
                    val offset = blockNumber * ISO15693_BLOCK_SIZE
                    if (offset < tagBytes.size) {
                        val data = tagBytes.copyOfRange(offset, minOf(offset + ISO15693_BLOCK_SIZE, tagBytes.size))
                        return byteArrayOf(0x00) + data + STATUS_SUCCESS
                    }
                }
            }
        }

        return STATUS_SUCCESS
    }

    private fun updateBadgeData(badgeId: String, oldHex: String, newHex: String) {
        activeTagData = newHex // Update in-memory cache
        serviceScope.launch {
            val db = GymDatabase.getDatabase(applicationContext)
            val dao = db.gymDao()
            val badge = dao.getBadgeById(badgeId)
            if (badge != null) {
                dao.updateBadge(badge.copy(tagData = newHex))
                dao.insertBadgeHistory(BadgeHistoryEntity(badgeId = badgeId, oldData = oldHex, newData = newHex))
                Log.d(TAG, "Badge $badgeId updated and history logged.")
                _updateEvent.value = BadgeUpdateEvent(badge.name, newHex)
                sendBadgeUpdateNotification(badge.name)
            }
        }
    }

    private fun sendBadgeUpdateNotification(badgeName: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "badge_updates"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Badge Updates", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Notifications when a badge receives a remote update"
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Badge Updated")
            .setContentText("Badge '$badgeName' has been updated by the reader.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(badgeName.hashCode(), notification)
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: $reason")
    }

    companion object {
        private const val TAG = "NfcEmulationService"
        
        var currentBadgeId: String? = null
        var activeTagData: String? = null

        private val _updateEvent = MutableStateFlow<BadgeUpdateEvent?>(null)
        val updateEvent = _updateEvent.asStateFlow()

        fun clearUpdateEvent() {
            _updateEvent.value = null
        }

        // APDU Status Codes
        private val STATUS_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val STATUS_FAILED = byteArrayOf(0x6F.toByte(), 0x00.toByte())

        // Command Headers
        private const val SELECT_AID_HEADER = "00a40400"

        // Mifare / NTAG Commands & Constants
        private const val CMD_MIFARE_READ = 0x30.toByte()
        private const val CMD_MIFARE_WRITE = 0xA2.toByte()
        private const val MIFARE_PAGE_SIZE = 4
        private const val MIFARE_READ_RESPONSE_SIZE = 16

        // ISO 15693 Commands & Constants
        private const val CMD_ISO15693_INVENTORY = 0x01
        private const val CMD_ISO15693_READ_SINGLE_BLOCK = 0x20
        private const val CMD_ISO15693_WRITE_SINGLE_BLOCK = 0x21
        private const val ISO15693_BLOCK_SIZE = 4
        private const val ISO15693_UID_SIZE = 8
    }
}
