package dev.taxmachine.gymapp.ui.dialogs

import android.content.Intent
import android.nfc.Tag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.taxmachine.gymapp.db.BadgeEntity
import dev.taxmachine.gymapp.ui.components.InfoRow
import dev.taxmachine.gymapp.utils.NfcUtils

@Composable
fun NfcScanningDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Nfc, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Ready to Scan")
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                Text("Hold your NFC tag near the back of your phone to scan.")
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BadgeNamingDialog(
    tag: Tag,
    scannedData: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var badgeName by remember { mutableStateOf("") }
    val uid = remember(tag) { tag.id.joinToString("") { "%02x".format(it) } }
    val protocol = remember(tag) { NfcUtils.getTagProtocol(tag) }
    val techList = remember(tag) { tag.techList.joinToString("\n") { it.substringAfterLast(".") } }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        title = { Text("New Badge Scanned") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = badgeName,
                    onValueChange = { badgeName = it },
                    label = { Text("Badge Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. My Gym Pass") }
                )
                
                Column {
                    Text("Device Info", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    InfoRow("UID", uid, isCode = true)
                    InfoRow("Protocol", protocol)
                    
                    Text("Technologies:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                    Text(techList, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }

                Column {
                    Text("Memory Dump", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Box(modifier = Modifier.heightIn(max = 200.dp).padding(8.dp).verticalScroll(rememberScrollState())) {
                            Text(
                                text = NfcUtils.formatHexDump(scannedData),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(badgeName, protocol, techList) },
                enabled = badgeName.isNotBlank()
            ) {
                Text("Save Badge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Discard")
            }
        }
    )
}

@Composable
fun BadgeDetailDialog(
    badge: BadgeEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(badge.name)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                InfoRow("UID", badge.id, isCode = true)
                InfoRow("Protocol", badge.protocol)
                
                if (badge.techList.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Technologies:", style = MaterialTheme.typography.labelMedium)
                    Text(badge.techList, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Memory Dump:", style = MaterialTheme.typography.labelMedium)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Box(modifier = Modifier.heightIn(max = 300.dp).padding(8.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            text = NfcUtils.formatHexDump(badge.tagData),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = {
                    val nfcContent = NfcUtils.convertToNfcFormat(badge)
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, nfcContent)
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TITLE, "${badge.name}.nfc")
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                }) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Share .nfc")
                }
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
