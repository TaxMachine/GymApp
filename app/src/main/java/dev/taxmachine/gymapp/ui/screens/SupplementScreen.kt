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
import dev.taxmachine.gymapp.ui.dialogs.SupplementProgressionDialog

@Composable
fun SupplementScreen(
    dao: GymDao,
    supplements: List<SupplementEntity>,
    onDelete: (SupplementEntity) -> Unit,
    onUpdateDosage: (SupplementEntity, Float) -> Unit,
    onOverrideDosage: (SupplementEntity, String) -> Unit
) {
    var supplementToShowGraph by remember { mutableStateOf<SupplementEntity?>(null) }
    var supplementToLogProgress by remember { mutableStateOf<SupplementEntity?>(null) }
    var supplementToOverride by remember { mutableStateOf<SupplementEntity?>(null) }

    if (supplements.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No supplements added yet. Press + to add.")
        }
    } else {
        val groupedSupplements = remember(supplements) {
            supplements.groupBy { it.timing }.toSortedMap(compareBy { it.ordinal })
        }

        // Stable callbacks to ensure SupplementItem remains skippable
        val onShowGraphInternal = remember { { s: SupplementEntity -> supplementToShowGraph = s } }
        val onLogProgressInternal = remember { { s: SupplementEntity -> supplementToLogProgress = s } }
        val onOverrideInternal = remember { { s: SupplementEntity -> supplementToOverride = s } }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            groupedSupplements.forEach { (timing, items) ->
                item(key = "header_${timing.name}", contentType = "header") {
                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                        Text(
                            text = timing.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
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
                        onShowGraph = onShowGraphInternal,
                        onUpdateDosage = onLogProgressInternal,
                        onOverrideDosage = onOverrideInternal,
                        onDelete = onDelete
                    )
                }
            }
        }
    }

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
                    onValueChange = { newDosage = it },
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

    if (supplementToShowGraph != null) {
        val s = supplementToShowGraph!!
        val logs by remember(dao, s.uid) { dao.getLogsForSupplement(s.uid) }.collectAsState(initial = emptyList())
        SupplementProgressionDialog(
            supplement = s,
            logs = logs,
            onDismiss = { supplementToShowGraph = null }
        )
    }
}

@Composable
fun SupplementItem(
    supplement: SupplementEntity,
    onShowGraph: (SupplementEntity) -> Unit,
    onUpdateDosage: (SupplementEntity) -> Unit,
    onOverrideDosage: (SupplementEntity) -> Unit,
    onDelete: (SupplementEntity) -> Unit
) {
    Card(
        onClick = { onShowGraph(supplement) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        )
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { 
                Text(
                    supplement.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                ) 
            },
            supportingContent = {
                Text("${supplement.dosage}${supplement.unit.label} - ${supplement.frequency.label}", style = MaterialTheme.typography.bodyMedium)
            },
            leadingContent = {
                Icon(
                    imageVector = if (supplement.isInjectable) Icons.Default.Vaccines else Icons.Default.Medication,
                    contentDescription = if (supplement.isInjectable) "Injectable" else "Capsule",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            },
            trailingContent = {
                Row {
                    IconButton(onClick = { onUpdateDosage(supplement) }) {
                        Icon(Icons.Default.History, contentDescription = "Log Progress", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onOverrideDosage(supplement) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Override Dosage", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = { onDelete(supplement) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    }
                }
            }
        )
    }
}
