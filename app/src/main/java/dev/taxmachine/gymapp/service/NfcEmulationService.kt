package dev.taxmachine.gymapp.service

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

class NfcEmulationService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return STATUS_FAILED

        val hexCommand = commandApdu.joinToString("") { "%02x".format(it) }
        Log.d("NfcEmulationService", "Command APDU received: $hexCommand")

        // Respond to SELECT AID (common for many readers to "handshake")
        if (hexCommand.startsWith("00a40400")) {
             return STATUS_SUCCESS
        }

        val activeTagHex = activeTagData ?: return STATUS_FAILED
        val tagBytes = hexToBytes(activeTagHex)

        // Handle common NFC commands
        // 0x30 is the READ command for MIFARE Ultralight / NTAG
        if (commandApdu.size >= 2 && commandApdu[0] == 0x30.toByte()) {
            val pageOffset = commandApdu[1].toInt() and 0xFF
            val byteOffset = pageOffset * 4
            
            if (byteOffset < tagBytes.size) {
                val responseSize = minOf(16, tagBytes.size - byteOffset)
                val response = tagBytes.copyOfRange(byteOffset, byteOffset + responseSize)
                Log.d("NfcEmulationService", "Responding to READ page $pageOffset with ${response.size} bytes")
                // For a 16-byte response, we append SUCCESS status
                return if (response.size == 16) response + STATUS_SUCCESS else response
            }
        }

        // Default response for other commands
        return STATUS_SUCCESS
    }

    private fun hexToBytes(hex: String): ByteArray {
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    override fun onDeactivated(reason: Int) {
        Log.d("NfcEmulationService", "Deactivated: $reason")
    }

    companion object {
        var activeTagData: String? = null
        private val STATUS_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val STATUS_FAILED = byteArrayOf(0x6F.toByte(), 0x00.toByte())
    }
}
