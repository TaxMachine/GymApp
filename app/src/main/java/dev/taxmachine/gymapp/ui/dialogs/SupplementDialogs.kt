package dev.taxmachine.gymapp.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.taxmachine.gymapp.db.*
import dev.taxmachine.gymapp.ui.components.AppDropdownMenu
import dev.taxmachine.gymapp.ui.components.GraphPoint
import dev.taxmachine.gymapp.ui.components.LineGraph
import dev.taxmachine.gymapp.utils.CalculationUtils

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
                    AppDropdownMenu(
                        label = "Unit",
                        options = DosingUnit.entries,
                        selectedOption = unit,
                        onOptionSelected = { unit = it },
                        optionLabel = { it.label },
                        modifier = Modifier.width(100.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                AppDropdownMenu(
                    label = "Timing",
                    options = AdministrationTiming.entries,
                    selectedOption = timing,
                    onOptionSelected = { timing = it },
                    optionLabel = { it.label }
                )
                Spacer(modifier = Modifier.height(8.dp))

                AppDropdownMenu(
                    label = "Frequency",
                    options = AdministrationFrequency.entries,
                    selectedOption = frequency,
                    onOptionSelected = { frequency = it },
                    optionLabel = { it.label }
                )
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

@Composable
fun SupplementProgressionDialog(
    supplement: SupplementEntity,
    logs: List<SupplementLogEntity>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${supplement.name} Dosage History") },
        text = {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp).padding(16.dp)) {
                    if (logs.size < 2) {
                        Text("Need at least 2 dosage updates to show history.", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LineGraph(
                            points = logs.map { GraphPoint(it.timestamp, it.dosage) },
                            unit = supplement.unit.label,
                            primaryColor = MaterialTheme.colorScheme.tertiary,
                            secondaryColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (logs.isNotEmpty()) {
                    SupplementStatsSummary(logs, supplement.unit.label)
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun SupplementStatsSummary(logs: List<SupplementLogEntity>, unit: String) {
    val stats = remember(logs) {
        CalculationUtils.calculateSupplementStats(logs)
    }

    if (stats == null) return

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
