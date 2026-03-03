package dev.taxmachine.gymapp.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.taxmachine.gymapp.db.*
import dev.taxmachine.gymapp.ui.components.GraphPoint
import dev.taxmachine.gymapp.ui.components.LineGraph
import dev.taxmachine.gymapp.utils.CalculationUtils

@Composable
fun AddSplitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var splitName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Split") },
        text = {
            OutlinedTextField(
                value = splitName,
                onValueChange = { splitName = it },
                label = { Text("Split Name (e.g. Push)") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = {
                if (splitName.isNotBlank()) {
                    onConfirm(splitName)
                }
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onSave: (String, Float, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }
    var reps by remember { mutableStateOf("") }
    val units = listOf("kg", "lbs")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Exercise") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    var unitExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = !unitExpanded },
                        modifier = Modifier.width(100.dp)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Reps") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    val w = weight.toFloatOrNull() ?: 0f
                    val r = reps.toIntOrNull() ?: 0
                    onSave(name, w, unit, r)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun LogWeightDialog(
    exercise: ExerciseEntity,
    onDismiss: () -> Unit,
    onLog: (Float) -> Unit
) {
    var newWeight by remember { mutableStateOf(exercise.weight.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log New Weight") },
        text = {
            OutlinedTextField(
                value = newWeight,
                onValueChange = { newWeight = it },
                label = { Text("New Weight (${exercise.weightUnit})") }
            )
        },
        confirmButton = {
            Button(onClick = {
                val w = newWeight.toFloatOrNull() ?: 0f
                onLog(w)
            }) { Text("Log") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ExerciseProgressionDialog(
    exercise: ExerciseEntity,
    logs: List<WeightLogEntity>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${exercise.name} Progression") },
        text = {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp).padding(16.dp)) {
                    if (logs.size < 2) {
                        Text("Need at least 2 logs to show progression.", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LineGraph(
                            points = logs.map { GraphPoint(it.timestamp, it.weight) },
                            unit = exercise.weightUnit
                        )
                    }
                }
                if (logs.isNotEmpty()) {
                    PerformanceMetricsSummary(logs, exercise.weightUnit)
                }
            }
        },
        confirmButton = { Button(onClick = { onDismiss() }) { Text("Close") } }
    )
}

@Composable
fun PerformanceMetricsSummary(logs: List<WeightLogEntity>, unit: String) {
    val stats = remember(logs) {
        CalculationUtils.calculateWorkoutStats(logs)
    }

    if (stats == null) return

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Stats Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Starting Weight: ${stats.start}$unit", style = MaterialTheme.typography.bodySmall)
            Text("Personal Best: ${stats.personalBest}$unit", style = MaterialTheme.typography.bodySmall)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Volume: ${"%.0f".format(stats.volume)}$unit", style = MaterialTheme.typography.bodySmall)
            Text("Current: ${stats.current}$unit", style = MaterialTheme.typography.bodySmall)
        }
        
        val progressColor = if (stats.difference >= 0) Color(0xFF4CAF50) else Color.Red
        Text(
            text = "Total Progress: ${if (stats.difference >= 0) "+" else ""}${"%.1f".format(stats.difference)}$unit (${"%.1f".format(stats.percentage)}%)",
            color = progressColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
