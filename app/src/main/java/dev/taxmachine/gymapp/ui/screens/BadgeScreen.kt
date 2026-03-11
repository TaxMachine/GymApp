package dev.taxmachine.gymapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.taxmachine.gymapp.db.BadgeEntity

@Composable
fun BadgeScreen(
    badges: List<BadgeEntity>,
    onBadgeClick: (BadgeEntity) -> Unit,
    onEmulateClick: (BadgeEntity) -> Unit,
    onDeleteBadge: (BadgeEntity) -> Unit,
    emulatingBadgeId: String?
) {
    if (badges.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No badges added yet. Press + to scan.")
        }
    } else {
        val onBadgeClickStable = remember(onBadgeClick) { onBadgeClick }
        val onEmulateClickStable = remember(onEmulateClick) { onEmulateClick }
        val onDeleteBadgeStable = remember(onDeleteBadge) { onDeleteBadge }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = badges,
                key = { it.id },
                contentType = { "badge" }
            ) { badge ->
                BadgeItem(
                    badge = badge,
                    isEmulating = emulatingBadgeId == badge.id,
                    onClick = onBadgeClickStable,
                    onEmulate = onEmulateClickStable,
                    onDelete = onDeleteBadgeStable
                )
            }
        }
    }
}

@Composable
fun BadgeItem(
    badge: BadgeEntity,
    isEmulating: Boolean,
    onClick: (BadgeEntity) -> Unit,
    onEmulate: (BadgeEntity) -> Unit,
    onDelete: (BadgeEntity) -> Unit
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val containerColor = remember(primaryContainer) {
        primaryContainer.copy(alpha = 0.3f)
    }
    
    val onCardClick = remember(badge, onClick) { { onClick(badge) } }
    val onEmulateAction = remember(badge, onEmulate) { { onEmulate(badge) } }
    val onDeleteAction = remember(badge, onDelete) { { onDelete(badge) } }

    val isNfcV = badge.protocol.contains("NFC-V") || badge.protocol.contains("15693")

    Card(
        onClick = onCardClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column {
            ListItem(
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
                supportingContent = { 
                    Column {
                        Text("Protocol: ${badge.protocol}", style = MaterialTheme.typography.bodySmall)
                        Text("ID: ${badge.id}", style = MaterialTheme.typography.bodySmall)
                    }
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = onEmulateAction,
                            enabled = !isNfcV,
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
                        IconButton(onClick = onDeleteAction) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                        }
                    }
                }
            )
            
            if (isNfcV) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "NFC-V cannot be emulated by Android hardware.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
