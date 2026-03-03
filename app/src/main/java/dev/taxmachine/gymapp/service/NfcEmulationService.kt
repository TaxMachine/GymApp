package dev.taxmachine.gymapp.service

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import dev.taxmachine.gymapp.utils.NfcUtils

/**
 * NfcEmulationService handles Host Card Emulation (HCE).
 * 
 * Note on ISO 15693 (NFC-V): 
 * Android HCE officially supports only ISO 14443-4 (ISO-DEP). 
 * True hardware-level spoofing of ISO 15693 is usually not possible on standard Android 
 * because the NFC controller handles the low-level protocol before it reaches this service.
 * 
 * However, many modern gym readers are multi-protocol and might attempt to use 
 * proprietary tunneling or simply check for ISO 7816-4 APDUs that mimic the 
 * ISO 15693 command set if they detect a phone.
 */
class NfcEmulationService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return STATUS_FAILED

        val hexCommand = commandApdu.joinToString("") { "%02x".format(it) }
        Log.d("NfcEmulationService", "Command received: $hexCommand")

        // 1. Handle SELECT AID (Standard HCE entry point)
        // If the reader supports ISO 14443-4, it will send this first.
        if (hexCommand.startsWith("00a40400")) {
            Log.d("NfcEmulationService", "SELECT AID detected.")
            return STATUS_SUCCESS
        }

        val activeTagHex = activeTagData ?: return STATUS_FAILED
        val tagBytes = NfcUtils.hexToBytes(activeTagHex)

        // 2. Handle Mifare / NTAG commands (e.g., 0x30 READ)
        if (commandApdu.size >= 2 && commandApdu[0] == 0x30.toByte()) {
            val pageOffset = commandApdu[1].toInt() and 0xFF
            val byteOffset = pageOffset * 4
            
            if (byteOffset < tagBytes.size) {
                val responseSize = minOf(16, tagBytes.size - byteOffset)
                val response = tagBytes.copyOfRange(byteOffset, byteOffset + responseSize)
                Log.d("NfcEmulationService", "Mifare READ page $pageOffset")
                return response + STATUS_SUCCESS
            }
        }

        // 3. Handle ISO 15693 (NFC-V) commands
        // ISO 15693 response format: [Response Flags] [Data] [Optional SW1 SW2]
        if (commandApdu.size >= 2) {
            val command = commandApdu[1].toInt() and 0xFF
            
            when (command) {
                0x01 -> { // Inventory
                    Log.d("NfcEmulationService", "ISO 15693 INVENTORY")
                    // Response: [Flags] [DSFID] [UID (8 bytes)]
                    val uid = if (tagBytes.size >= 8) tagBytes.take(8).toByteArray() else ByteArray(8)
                    return byteArrayOf(0x00, 0x00, *uid) + STATUS_SUCCESS
                }
                0x20 -> { // Read Single Block
                    val blockNumber = if (commandApdu.size >= 3) commandApdu[2].toInt() and 0xFF else 0
                    val offset = blockNumber * 4
                    if (offset < tagBytes.size) {
                        val responseSize = minOf(4, tagBytes.size - offset)
                        val data = tagBytes.copyOfRange(offset, offset + responseSize)
                        Log.d("NfcEmulationService", "ISO 15693 READ BLOCK: $blockNumber")
                        return byteArrayOf(0x00) + data + STATUS_SUCCESS
                    }
                }
                0x23 -> { // Read Multiple Blocks
                    val firstBlock = if (commandApdu.size >= 3) commandApdu[2].toInt() and 0xFF else 0
                    val numBlocks = if (commandApdu.size >= 4) (commandApdu[3].toInt() and 0xFF) + 1 else 1
                    val offset = firstBlock * 4
                    val totalSize = numBlocks * 4
                    if (offset < tagBytes.size) {
                        val responseSize = minOf(totalSize, tagBytes.size - offset)
                        val data = tagBytes.copyOfRange(offset, offset + responseSize)
                        Log.d("NfcEmulationService", "ISO 15693 READ MULTI: $firstBlock ($numBlocks)")
                        return byteArrayOf(0x00) + data + STATUS_SUCCESS
                    }
                }
                0x2B -> { // Get System Information
                    Log.d("NfcEmulationService", "ISO 15693 GET INFO")
                    val uid = if (tagBytes.size >= 8) tagBytes.take(8).toByteArray() else ByteArray(8)
                    val info = byteArrayOf(
                        0x00, // Response Flags
                        0x0F, // Info flags: UID, DSFID, AFI, Memory present
                        *uid,
                        0x00, // DSFID
                        0x00, // AFI
                        0x3F, // 64 blocks
                        0x03, // 4 bytes per block
                        0x01  // IC Reference
                    )
                    return info + STATUS_SUCCESS
                }
            }
        }

        // 4. HID iCLASS tunneling (common for gym badges)
        // Some readers use specific command headers like 0xFF
        if (commandApdu[0] == 0xFF.toByte()) {
            Log.d("NfcEmulationService", "Possible HID iCLASS / Tunneling detected")
            // This is complex and usually requires specific keys/handshakes
            // Returning STATUS_SUCCESS may allow the reader to continue probing
            return STATUS_SUCCESS
        }

        return STATUS_SUCCESS
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
