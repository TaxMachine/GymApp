package dev.taxmachine.gymapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.taxmachine.gymapp.db.*

@Composable
fun SupplementScreen(
    dao: GymDao,
    supplements: List<SupplementEntity>,
    onDelete: (SupplementEntity) -> Unit,
    onToggleActive: (SupplementEntity) -> Unit,
    onUpdateDosage: (SupplementEntity, Float) -> Unit,
    onOverrideDosage: (SupplementEntity, String) -> Unit,
    onShowGraph: (SupplementEntity) -> Unit
) {
    var supplementToLogProgress by remember { mutableStateOf<SupplementEntity?>(null) }
    var supplementToOverride by remember { mutableStateOf<SupplementEntity?>(null) }

    // Stable internal callbacks
    val onLogProgressInternal = remember { { s: SupplementEntity -> supplementToLogProgress = s } }
    val onOverrideInternal = remember { { s: SupplementEntity -> supplementToOverride = s } }

    if (supplements.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No supplements added yet. Press + to add.")
        }
    } else {
        // Group and sort supplements efficiently
        val groupedSupplements = remember(supplements) {
            supplements.groupBy { it.isActive }.toSortedMap(compareByDescending { it })
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            groupedSupplements.forEach { (isActive, items) ->
                item(key = "header_${isActive}", contentType = "header") {
                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                        Text(
                            text = if (isActive) "Active Supplements" else "Discontinued Supplements",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 4.dp),
                            thickness = 1.dp,
                            color = (if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline).copy(alpha = 0.2f)
                        )
                    }
                }

                items(
                    items = items, 
                    key = { it.uid },
                    contentType = { "supplement" }
                ) { s ->
                    SupplementItem(
                        supplement = s,
                        onShowGraph = onShowGraph,
                        onUpdateDosage = onLogProgressInternal,
                        onOverrideDosage = onOverrideInternal,
                        onToggleActive = onToggleActive,
                        onDelete = onDelete
                    )
                }
            }
        }
    }

    // Dialogs
    if (supplementToLogProgress != null) {
        val s = supplementToLogProgress!!
        var newDosage by remember(s) { mutableStateOf(s.dosage) }
        AlertDialog(
            onDismissRequest = { supplementToLogProgress = null },
            title = { Text("Log Dose for ${s.name}") },
            text = {
                OutlinedTextField(
                    value = newDosage,
                    onValueChange = { newDosage = it },
                    label = { Text("Dose (${s.unit.label})") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val d = newDosage.toFloatOrNull() ?: 0f
                    onUpdateDosage(s, d)
                    supplementToLogProgress = null
                }) { Text("Log") }
            },
            dismissButton = {
                TextButton(onClick = { supplementToLogProgress = null }) { Text("Cancel") }
            }
        )
    }

    if (supplementToOverride != null) {
        val s = supplementToOverride!!
        var newDosage by remember(s) { mutableStateOf(s.dosage) }
        AlertDialog(
            onDismissRequest = { supplementToOverride = null },
            title = { Text("Override Dosage for ${s.name}") },
            text = {
                OutlinedTextField(
                    value = newDosage,
                    onValueChange = { newValue -> newDosage = newValue },
                    label = { Text("Current Dosage (${s.unit.label})") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    onOverrideDosage(s, newDosage)
                    supplementToOverride = null
                }) { Text("Override") }
            },
            dismissButton = {
                TextButton(onClick = { supplementToOverride = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SupplementItem(
    supplement: SupplementEntity,
    onShowGraph: (SupplementEntity) -> Unit,
    onUpdateDosage: (SupplementEntity) -> Unit,
    onOverrideDosage: (SupplementEntity) -> Unit,
    onToggleActive: (SupplementEntity) -> Unit,
    onDelete: (SupplementEntity) -> Unit
) {
    val isActive = supplement.isActive
    val contentAlpha = if (isActive) 1f else 0.6f
    
    // Calculate colors once per recomposition
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    // Manual layout to reduce node overhead and improve scroll performance
    Card(
        onClick = { onShowGraph(supplement) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading Icon
            Icon(
                imageVector = if (supplement.isInjectable) Icons.Default.Vaccines else Icons.Default.Medication,
                contentDescription = null,
                tint = (if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline).copy(alpha = contentAlpha),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = supplement.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                Text(
                    text = "${supplement.dosage}${supplement.unit.label} - ${supplement.frequency.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
            }
            
            // Trailing Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isActive) {
                    Row {
                        IconButton(onClick = { onUpdateDosage(supplement) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { onOverrideDosage(supplement) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Row {
                    IconButton(onClick = { onToggleActive(supplement) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = (if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary).copy(alpha = contentAlpha),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { onDelete(supplement) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
