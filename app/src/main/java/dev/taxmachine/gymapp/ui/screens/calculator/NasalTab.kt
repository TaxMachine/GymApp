package dev.taxmachine.gymapp.ui.screens.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
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
fun NasalTab() {
    var peptideMass by remember { mutableStateOf("10") } // mg
    var totalVolume by remember { mutableStateOf("10") } // ml
    var sprayVolume by remember { mutableStateOf("0.1") } // ml per spray
    var desiredDose by remember { mutableStateOf("200") } // mcg

    val mass = peptideMass.toDoubleOrNull() ?: 0.0
    val volume = totalVolume.toDoubleOrNull() ?: 0.0
    val sprayVol = sprayVolume.toDoubleOrNull() ?: 0.0
    val doseMcg = desiredDose.toDoubleOrNull() ?: 0.0

    val mcgPerSpray = CalculationUtils.calculateMcgPerSpray(mass, volume, sprayVol)
    val spraysNeeded = CalculationUtils.calculateNasalDose(mass, volume, sprayVol, doseMcg)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Air,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Nasal Spray Calculator",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        OutlinedTextField(
            value = peptideMass,
            onValueChange = { peptideMass = it },
            label = { Text("Total Peptide Mass (mg)") },
            placeholder = { Text("e.g. 10, 50") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = totalVolume,
            onValueChange = { totalVolume = it },
            label = { Text("Total Liquid Volume (ml)") },
            placeholder = { Text("e.g. 10 or 30") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = sprayVolume,
            onValueChange = { sprayVolume = it },
            label = { Text("Volume per Spray (ml)") },
            placeholder = { Text("Default is 0.1ml") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = desiredDose,
            onValueChange = { desiredDose = it },
            label = { Text("Desired Dose (mcg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Results", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "%.1f".format(mcgPerSpray),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("mcg / spray", style = MaterialTheme.typography.titleMedium)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "%.1f".format(spraysNeeded),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sprays", style = MaterialTheme.typography.headlineSmall)
                }
                Text("needed for desired dose", style = MaterialTheme.typography.bodyMedium)
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
                    "Standard nasal sprayers typically deliver 0.1ml per spray. Verify your sprayer's specifications for accuracy.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
