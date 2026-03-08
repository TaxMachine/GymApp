package dev.taxmachine.gymapp.ui.screens.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.taxmachine.gymapp.utils.CalculationUtils

@Composable
fun InjectionTab() {
    var peptideMass by remember { mutableStateOf("5") } // mg
    var bacWater by remember { mutableStateOf("2") } // ml
    var desiredDose by remember { mutableStateOf("250") } // mcg
    var doseUnit by remember { mutableStateOf("mcg") } // mcg or mg

    val mass = peptideMass.toDoubleOrNull() ?: 0.0
    val water = bacWater.toDoubleOrNull() ?: 0.0
    val doseRaw = desiredDose.toDoubleOrNull() ?: 0.0
    val doseMcg = if (doseUnit == "mg") doseRaw * 1000 else doseRaw

    val resultUnits = CalculationUtils.calculatePeptideDose(mass, water, doseMcg)
    val totalDoses = CalculationUtils.calculateTotalDoses(mass, doseMcg)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Calculate,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Injection Calculator",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        OutlinedTextField(
            value = peptideMass,
            onValueChange = { peptideMass = it },
            label = { Text("Vial Mass (mg)") },
            placeholder = { Text("e.g. 5, 50, 100, 600") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = bacWater,
            onValueChange = { bacWater = it },
            label = { Text("BAC Water (ml)") },
            placeholder = { Text("e.g. 2 or 3") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = desiredDose,
                onValueChange = { desiredDose = it },
                label = { Text("Desired Dose") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Unit", style = MaterialTheme.typography.labelSmall)
                Row {
                    FilterChip(
                        selected = doseUnit == "mcg",
                        onClick = { doseUnit = "mcg" },
                        label = { Text("mcg") }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = doseUnit == "mg",
                        onClick = { doseUnit = "mg" },
                        label = { Text("mg") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Result", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "%.1f".format(resultUnits),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Units", style = MaterialTheme.typography.headlineSmall)
                }
                Text("on a 100-unit insulin syringe", style = MaterialTheme.typography.bodyMedium)
                
                if (resultUnits > 100) {
                    Text(
                        "Note: Requires ${"%.2f".format(resultUnits / 100)} full 1ml syringes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                Text(
                    text = "%.1f".format(totalDoses),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("Total doses per vial", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Works for Peptides, GHK-Cu, NAD+, Glutathione, etc. Syringe unit calculation based on U-100 (1ml = 100 units).",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
