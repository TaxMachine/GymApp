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
import dev.taxmachine.gymapp.ui.components.GraphPoint
import dev.taxmachine.gymapp.ui.components.LineGraph
import dev.taxmachine.gymapp.utils.CalculationUtils

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
