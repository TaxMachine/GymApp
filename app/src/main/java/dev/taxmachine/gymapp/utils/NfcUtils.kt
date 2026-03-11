package dev.taxmachine.gymapp.utils

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.nfc.tech.NfcV
import android.util.Log
import dev.taxmachine.gymapp.db.BadgeEntity

object NfcUtils {

    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    fun bytesToHex(bytes: ByteArray): String {
        val result = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val i = byte.toInt() and 0xFF
            result.append(HEX_CHARS[i shr 4])
            result.append(HEX_CHARS[i and 0x0F])
        }
        return result.toString()
    }

    fun hexToBytes(hex: String): ByteArray {
        if (hex.length % 2 != 0) return byteArrayOf()
        val result = ByteArray(hex.length / 2)
        for (i in 0 until hex.length step 2) {
            val first = Character.digit(hex[i], 16)
            val second = Character.digit(hex[i + 1], 16)
            if (first == -1 || second == -1) return byteArrayOf()
            result[i / 2] = ((first shl 4) or second).toByte()
        }
        return result
    }

    fun getTagProtocol(tag: Tag): String {
        val techList = tag.techList
        return when {
            techList.contains(MifareClassic::class.java.name) -> "Mifare Classic"
            techList.contains(MifareUltralight::class.java.name) -> "Mifare Ultralight / NTAG"
            techList.contains(NfcV::class.java.name) -> "ISO 15693 (NFC-V)"
            techList.contains(IsoDep::class.java.name) -> "ISO 14443-4 (ISO-DEP)"
            techList.contains(NfcA::class.java.name) -> "ISO 14443-3A (NFC-A)"
            else -> "Unknown Protocol"
        }
    }

    fun readTagFullData(tag: Tag): String {
        val nfcV = NfcV.get(tag)
        if (nfcV != null) {
            try {
                nfcV.connect()
                val fullData = mutableListOf<Byte>()
                for (i in 0 until 64) {
                    val response = try {
                        nfcV.transceive(byteArrayOf(0x02.toByte(), 0x20.toByte(), i.toByte()))
                    } catch (e: Exception) { null }
                    
                    if (response != null && response.size > 1 && response[0] == 0x00.toByte()) {
                        for (j in 1 until response.size) {
                            fullData.add(response[j])
                        }
                    } else {
                        break
                    }
                }
                nfcV.close()
                if (fullData.isNotEmpty()) {
                    return bytesToHex(fullData.toByteArray())
                }
            } catch (e: Exception) {
                Log.e("NFC", "ISO 15693 read error", e)
            }
        }

        val mu = MifareUltralight.get(tag)
        if (mu != null) {
            try {
                mu.connect()
                val fullData = mutableListOf<Byte>()
                for (i in 0 until 256 step 4) {
                    val data = try { mu.readPages(i) } catch (e: Exception) { null }
                    if (data != null && data.size >= 16) {
                        for (b in data) fullData.add(b)
                    } else {
                        break
                    }
                }
                mu.close()
                if (fullData.isNotEmpty()) {
                    return bytesToHex(fullData.toByteArray())
                }
            } catch (e: Exception) {
                Log.e("NFC", "Ultralight read error", e)
            }
        }

        val mc = MifareClassic.get(tag)
        if (mc != null) {
            try {
                mc.connect()
                val fullData = mutableListOf<Byte>()
                val keys = listOf(
                    MifareClassic.KEY_DEFAULT,
                    byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
                    byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte()),
                    ByteArray(6) { 0 }
                )
                for (s in 0 until mc.sectorCount) {
                    var authenticated = false
                    for (key in keys) {
                        if (mc.authenticateSectorWithKeyA(s, key) || mc.authenticateSectorWithKeyB(s, key)) {
                            authenticated = true
                            break
                        }
                    }
                    if (authenticated) {
                        val firstBlock = mc.sectorToBlock(s)
                        for (b in 0 until mc.getBlockCountInSector(s)) {
                            try {
                                val blockData = mc.readBlock(firstBlock + b)
                                for (byte in blockData) fullData.add(byte)
                            } catch (e: Exception) {
                                repeat(16) { fullData.add(0) }
                            }
                        }
                    } else {
                        repeat(mc.getBlockCountInSector(s) * 16) { fullData.add(0) }
                    }
                }
                mc.close()
                return bytesToHex(fullData.toByteArray())
            } catch (e: Exception) {
                Log.e("NFC", "MifareClassic read error", e)
            }
        }

        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            try {
                nfcA.connect()
                val fullData = mutableListOf<Byte>()
                for (page in 0 until 256 step 4) {
                    val response = try {
                        nfcA.transceive(byteArrayOf(0x30, page.toByte()))
                    } catch (e: Exception) { null }
                    
                    if (response != null && response.size >= 16) {
                        for (b in response) fullData.add(b)
                    } else {
                        break
                    }
                }
                nfcA.close()
                if (fullData.isNotEmpty()) {
                    return bytesToHex(fullData.toByteArray())
                }
            } catch (e: Exception) {
                Log.e("NFC", "Generic NfcA read error", e)
            }
        }
        
        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            try {
                isoDep.connect()
                val result = isoDep.hiLayerResponse ?: tag.id
                isoDep.close()
                return bytesToHex(result)
            } catch (e: Exception) {
                Log.e("NFC", "ISO-DEP read error", e)
            }
        }
        return bytesToHex(tag.id)
    }

    fun formatHexDump(hexString: String): String {
        if (hexString.isBlank()) return "No data"
        
        val sb = StringBuilder()
        val bytes = hexToBytes(hexString)
        
        for (i in bytes.indices step 16) {
            // Offset
            sb.append("%04X  ".format(i))
            
            // Hex values
            for (j in 0 until 16) {
                if (i + j < bytes.size) {
                    val b = bytes[i + j].toInt() and 0xFF
                    sb.append("%02X ".format(b))
                } else {
                    sb.append("   ")
                }
                if (j == 7) sb.append(" ")
            }
            
            sb.append(" |")
            
            // ASCII values
            for (j in 0 until 16) {
                if (i + j < bytes.size) {
                    val b = bytes[i + j].toInt() and 0xFF
                    if (b in 32..126) {
                        sb.append(b.toChar())
                    } else {
                        sb.append('.')
                    }
                }
            }
            sb.append("|\n")
        }
        
        return sb.toString()
    }

    fun convertToNfcFormat(badge: BadgeEntity): String {
        val bytes = hexToBytes(badge.tagData)
        if (bytes.isEmpty()) return ""
        
        val sb = StringBuilder()
        sb.append("Filetype: Flipper NFC device\n")
        sb.append("Version: 3\n")
        
        if (badge.protocol.contains("NFC-V") || badge.protocol.contains("15693")) {
            sb.append("Device type: ISO15693\n")
        } else {
            sb.append("Device type: NTAG213\n")
        }
        
        sb.append("UID: ")
        val uidBytes = hexToBytes(badge.id)
        for (i in uidBytes.indices) {
            val b = uidBytes[i].toInt() and 0xFF
            sb.append(HEX_CHARS[b shr 4].uppercaseChar())
            sb.append(HEX_CHARS[b and 0x0F].uppercaseChar())
            if (i < uidBytes.size - 1) sb.append(' ')
        }
        
        if (badge.protocol.contains("NFC-V") || badge.protocol.contains("15693")) {
            sb.append("\n# ISO15693 specific data\n")
            // NFC-V usually has 4-byte blocks
            for (i in 0 until (bytes.size / 4)) {
                sb.append("Block $i: ")
                for (j in 0 until 4) {
                    val idx = i * 4 + j
                    if (idx < bytes.size) {
                        val b = bytes[idx].toInt() and 0xFF
                        sb.append(HEX_CHARS[b shr 4].uppercaseChar())
                        sb.append(HEX_CHARS[b and 0x0F].uppercaseChar())
                        if (j < 3) sb.append(' ')
                    }
                }
                sb.append('\n')
            }
        } else {
            sb.append("\nATQA: 44 00\nSAK: 00\n")
            for (i in 0 until (bytes.size / 4)) {
                sb.append("Page $i: ")
                for (j in 0 until 4) {
                    val idx = i * 4 + j
                    if (idx < bytes.size) {
                        val b = bytes[idx].toInt() and 0xFF
                        sb.append(HEX_CHARS[b shr 4].uppercaseChar())
                        sb.append(HEX_CHARS[b and 0x0F].uppercaseChar())
                        if (j < 3) sb.append(' ')
                    }
                }
                sb.append('\n')
            }
        }
        return sb.toString()
    }
}
