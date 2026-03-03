package dev.taxmachine.gymapp.ui.dialogs

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.taxmachine.gymapp.db.BadgeEntity
import dev.taxmachine.gymapp.utils.NfcUtils

@Composable
fun BadgeNamingDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var badgeName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name your Badge") },
        text = {
            OutlinedTextField(
                value = badgeName,
                onValueChange = { badgeName = it },
                label = { Text("Badge Name") }
            )
        },
        confirmButton = {
            Button(onClick = { onSave(badgeName) }) {
                Text("Save")
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
        title = { Text(badge.name) },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                Text("ID: ${badge.id}", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Full Memory Dump (Hexdump):", style = MaterialTheme.typography.labelMedium)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 4.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = NfcUtils.formatHexDump(badge.tagData),
                        modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
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
