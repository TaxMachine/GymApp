package dev.taxmachine.gymapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.taxmachine.gymapp.db.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SupplementScreen(
    dao: GymDao,
    supplements: List<SupplementEntity>,
    onDelete: (SupplementEntity) -> Unit
) {
    var supplementToShowGraph by remember { mutableStateOf<SupplementEntity?>(null) }
    var supplementToUpdateDosage by remember { mutableStateOf<SupplementEntity?>(null) }
    val scope = rememberCoroutineScope()

    if (supplements.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No supplements added yet. Press + to add.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(supplements, key = { it.uid }) { s ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { supplementToShowGraph = s },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { 
                            Text(
                                s.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            ) 
                        },
                        supportingContent = {
                            Column {
                                Text("${s.dosage}${s.unit.label} - ${s.timing.label}", style = MaterialTheme.typography.bodyMedium)
                                Text(s.frequency.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                            }
                        },
                        leadingContent = {
                            Icon(
                                imageVector = if (s.isInjectable) Icons.Default.Vaccines else Icons.Default.Medication,
                                contentDescription = if (s.isInjectable) "Injectable" else "Capsule",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { supplementToUpdateDosage = s }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Update Dosage", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onDelete(s) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (supplementToUpdateDosage != null) {
        var newDosage by remember { mutableStateOf(supplementToUpdateDosage!!.dosage) }
        AlertDialog(
            onDismissRequest = { supplementToUpdateDosage = null },
            title = { Text("Update Dosage for ${supplementToUpdateDosage!!.name}") },
            text = {
                OutlinedTextField(
                    value = newDosage,
                    onValueChange = { newDosage = it },
                    label = { Text("New Dosage (${supplementToUpdateDosage!!.unit.label})") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val d = newDosage.toFloatOrNull() ?: 0f
                    scope.launch {
                        dao.updateSupplement(supplementToUpdateDosage!!.copy(dosage = newDosage))
                        dao.insertSupplementLog(SupplementLogEntity(supplementUid = supplementToUpdateDosage!!.uid, dosage = d))
                        supplementToUpdateDosage = null
                    }
                }) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { supplementToUpdateDosage = null }) { Text("Cancel") }
            }
        )
    }

    if (supplementToShowGraph != null) {
        val logs by dao.getLogsForSupplement(supplementToShowGraph!!.uid).collectAsState(initial = emptyList())
        AlertDialog(
            onDismissRequest = { supplementToShowGraph = null },
            title = { Text("${supplementToShowGraph!!.name} Dosage History") },
            text = {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(220.dp).padding(16.dp)) {
                        if (logs.size < 2) {
                            Text("Need at least 2 dosage updates to show history.", modifier = Modifier.align(Alignment.Center))
                        } else {
                            SupplementProgressionGraph(logs, supplementToShowGraph!!.unit.label)
                        }
                    }
                    if (logs.isNotEmpty()) {
                        SupplementStatsSummary(logs, supplementToShowGraph!!.unit.label)
                    }
                }
            },
            confirmButton = { Button(onClick = { supplementToShowGraph = null }) { Text("Close") } }
        )
    }
}

@Composable
fun SupplementStatsSummary(logs: List<SupplementLogEntity>, unit: String) {
    val stats = remember(logs) {
        val firstDose = logs.first().dosage
        val lastDose = logs.last().dosage
        val diff = lastDose - firstDose
        val maxDose = logs.maxOf { it.dosage }
        
        object {
            val start = firstDose
            val current = lastDose
            val difference = diff
            val peak = maxDose
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("History Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Starting Dose: ${stats.start}$unit", style = MaterialTheme.typography.bodySmall)
            Text("Peak Dose: ${stats.peak}$unit", style = MaterialTheme.typography.bodySmall)
        }
        
        val progressColor = if (stats.difference >= 0) MaterialTheme.colorScheme.primary else Color.Red
        Text(
            text = "Total Change: ${if (stats.difference >= 0) "+" else ""}${"%.1f".format(stats.difference)}$unit",
            color = progressColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun SupplementProgressionGraph(logs: List<SupplementLogEntity>, unit: String) {
    val dosages = remember(logs) { logs.map { it.dosage } }
    val minDose = remember(dosages) { (dosages.minOrNull() ?: 0f) * 0.9f }
    val maxDose = remember(dosages) { (dosages.maxOrNull() ?: 1f) * 1.1f }
    val doseRange = remember(minDose, maxDose) { if (maxDose == minDose) 1f else maxDose - minDose }
    
    val primaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    
    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${"%.1f".format(maxDose)}$unit", style = MaterialTheme.typography.labelSmall, color = labelColor)
            val timeRange = if (logs.size >= 2) {
                val duration = logs.last().timestamp - logs.first().timestamp
                val days = duration / (1000 * 60 * 60 * 24)
                "${days}d range"
            } else ""
            Text(timeRange, style = MaterialTheme.typography.labelSmall, color = labelColor, fontWeight = FontWeight.Bold)
        }

        Canvas(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp)) {
            if (logs.size < 2) return@Canvas

            val width = size.width
            val height = size.height
            val spacing = width / (logs.size - 1)

            val points = logs.mapIndexed { index, log ->
                val x = index * spacing
                val y = height - ((log.dosage - minDose) / doseRange * height)
                Offset(x, y)
            }

            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 4f)
            )
            
            points.forEach { point ->
                drawCircle(
                    color = secondaryColor,
                    radius = 6f,
                    center = point
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dateFormat.format(Date(logs.first().timestamp)), style = MaterialTheme.typography.labelSmall, color = labelColor)
            Text("${"%.1f".format(minDose)}$unit", style = MaterialTheme.typography.labelSmall, color = labelColor)
            Text(dateFormat.format(Date(logs.last().timestamp)), style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementAddDialog(onDismiss: () -> Unit, onSave: (SupplementEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(DosingUnit.MG) }
    var timing by remember { mutableStateOf(AdministrationTiming.MORNING_FASTED) }
    var frequency by remember { mutableStateOf(AdministrationFrequency.EVERY_DAY) }
    var isInjectable by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Supplement") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = dosage, onValueChange = { dosage = it }, label = { Text("Dosage") }, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    var unitExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = !unitExpanded }, modifier = Modifier.width(100.dp)) {
                        OutlinedTextField(value = unit.label, onValueChange = {}, readOnly = true, label = { Text("Unit") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable))
                        ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            DosingUnit.entries.forEach { u -> DropdownMenuItem(text = { Text(u.label) }, onClick = { unit = u; unitExpanded = false }) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                var timingExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = timingExpanded, onExpandedChange = { timingExpanded = !timingExpanded }) {
                    OutlinedTextField(value = timing.label, onValueChange = {}, readOnly = true, label = { Text("Timing") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timingExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
                    ExposedDropdownMenu(expanded = timingExpanded, onDismissRequest = { timingExpanded = false }) {
                        AdministrationTiming.entries.forEach { t -> DropdownMenuItem(text = { Text(t.label) }, onClick = { timing = t; timingExpanded = false }) }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                var freqExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = freqExpanded, onExpandedChange = { freqExpanded = !freqExpanded }) {
                    OutlinedTextField(value = frequency.label, onValueChange = {}, readOnly = true, label = { Text("Frequency") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
                    ExposedDropdownMenu(expanded = freqExpanded, onDismissRequest = { freqExpanded = false }) {
                        AdministrationFrequency.entries.forEach { f -> DropdownMenuItem(text = { Text(f.label) }, onClick = { frequency = f; freqExpanded = false }) }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isInjectable, onCheckedChange = { isInjectable = it })
                    Text("Injectable")
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(SupplementEntity(name = name, dosage = dosage, unit = unit, timing = timing, frequency = frequency, isInjectable = isInjectable)) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
