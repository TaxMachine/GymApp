package dev.taxmachine.gymapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.taxmachine.gymapp.db.BadgeEntity

@Composable
fun BadgeScreen(
    badges: List<BadgeEntity>,
    isScanning: MutableState<Boolean>,
    onBadgeClick: (BadgeEntity) -> Unit,
    onEmulateClick: (BadgeEntity) -> Unit,
    onDeleteBadge: (BadgeEntity) -> Unit,
    emulatingBadgeId: String?
) {
    if (isScanning.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Hold your NFC tag near the phone...")
                Button(onClick = { isScanning.value = false }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Cancel")
                }
            }
        }
    } else if (badges.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No badges added yet. Press + to scan.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(badges, key = { it.id }) { badge ->
                val isEmulating = emulatingBadgeId == badge.id
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    ListItem(
                        modifier = Modifier.clickable { onBadgeClick(badge) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(Icons.Default.Nfc, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        headlineContent = { 
                            Text(
                                badge.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            ) 
                        },
                        supportingContent = { Text("ID: ${badge.id}", style = MaterialTheme.typography.bodySmall) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { onEmulateClick(badge) },
                                    colors = if (isEmulating) {
                                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    } else {
                                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(if (isEmulating) "Stop" else "Emulate", style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { onDeleteBadge(badge) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
