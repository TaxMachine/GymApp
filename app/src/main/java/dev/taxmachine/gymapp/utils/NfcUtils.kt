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

    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hexToBytes(hex: String): ByteArray {
        return try {
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: Exception) {
            byteArrayOf()
        }
    }

    fun readTagFullData(tag: Tag): String {
        // Try ISO 15693 (NFC-V) first as requested
        val nfcV = NfcV.get(tag)
        if (nfcV != null) {
            try {
                nfcV.connect()
                val fullData = mutableListOf<Byte>()
                // ISO 15693 tags typically have blocks. We'll try to read up to 64 blocks.
                for (i in 0 until 64) {
                    val response = try {
                        // Flags: 0x02 (High data rate), Command: 0x20 (Read single block)
                        nfcV.transceive(byteArrayOf(0x02.toByte(), 0x20.toByte(), i.toByte()))
                    } catch (e: Exception) { null }
                    
                    if (response != null && response.size >= 1) {
                        // First byte is usually response flags (0x00 = success)
                        if (response[0] == 0x00.toByte() && response.size > 1) {
                            fullData.addAll(response.slice(1 until response.size))
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                }
                nfcV.close()
                if (fullData.isNotEmpty()) {
                    Log.d("NFC", "Successfully read ISO 15693 tag")
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
                        fullData.addAll(data.toList())
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
                        if (mc.authenticateSectorWithKeyA(s, key)) {
                            authenticated = true
                            break
                        } else if (mc.authenticateSectorWithKeyB(s, key)) {
                            authenticated = true
                            break
                        }
                    }
                    if (authenticated) {
                        val firstBlock = mc.sectorToBlock(s)
                        for (b in 0 until mc.getBlockCountInSector(s)) {
                            try {
                                val blockData = mc.readBlock(firstBlock + b)
                                fullData.addAll(blockData.toList())
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
                        fullData.addAll(response.toList())
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

    fun formatHexDump(hex: String): String {
        if (hex.isEmpty()) return "No data"
        val bytes = hexToBytes(hex)
        if (bytes.isEmpty() && hex.isNotEmpty()) return "Invalid data format: $hex"
        
        val sb = StringBuilder()
        for (i in bytes.indices step 16) {
            sb.append("%04x: ".format(i))
            val chunk = bytes.toList().subList(i, minOf(i + 16, bytes.size))
            for (j in 0 until 16) {
                if (j < chunk.size) sb.append("%02x ".format(chunk[j]))
                else sb.append("   ")
                if (j == 7) sb.append(" ")
            }
            sb.append(" |")
            for (j in chunk.indices) {
                val c = chunk[j].toInt().toChar()
                if (c in ' '..'~') sb.append(c) else sb.append('.')
            }
            sb.append("|\n")
        }
        return sb.toString()
    }

    fun convertToNfcFormat(badge: BadgeEntity): String {
        val hex = badge.tagData
        if (hex.isEmpty()) return ""
        val bytes = hexToBytes(hex)
        if (bytes.isEmpty()) return ""
        
        val sb = StringBuilder()
        sb.append("Filetype: Flipper NFC device\n")
        sb.append("Version: 3\n")
        sb.append("Device type: NTAG213\n")
        sb.append("UID: ${badge.id.chunked(2).joinToString(" ").uppercase()}\n")
        sb.append("ATQA: 44 00\n")
        sb.append("SAK: 00\n")
        for (i in 0 until (bytes.size / 4)) {
            val pageData = bytes.slice(i * 4 until minOf((i + 1) * 4, bytes.size)).joinToString(" ") { "%02X".format(it) }
            sb.append("Page $i: $pageData\n")
        }
        return sb.toString()
    }
}
