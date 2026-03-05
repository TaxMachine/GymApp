package dev.taxmachine.gymapp.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.taxmachine.gymapp.db.ExerciseEntity
import dev.taxmachine.gymapp.db.SupplementEntity
import dev.taxmachine.gymapp.db.WeightLogEntity
import dev.taxmachine.gymapp.db.SupplementLogEntity
import dev.taxmachine.gymapp.ui.components.GraphPoint
import dev.taxmachine.gymapp.ui.components.LineGraph
import dev.taxmachine.gymapp.ui.dialogs.PerformanceMetricsSummary
import dev.taxmachine.gymapp.ui.dialogs.SupplementStatsSummary
import dev.taxmachine.gymapp.utils.CalculationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressionScreen(
    exercise: ExerciseEntity,
    logs: List<WeightLogEntity>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gym_prefs", Context.MODE_PRIVATE) }
    val displayUnit = remember { prefs.getString("weight_unit", "kg") ?: "kg" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ProgressionSection(
                title = "Weight Progression ($displayUnit)",
                points = logs.map { log ->
                    GraphPoint(
                        log.timestamp,
                        CalculationUtils.convertWeight(log.weight, exercise.weightUnit, displayUnit)
                    )
                },
                unit = displayUnit
            )

            ProgressionSection(
                title = "Reps Progression",
                points = logs.map { GraphPoint(it.timestamp, it.reps.toFloat()) },
                unit = "reps",
                primaryColor = MaterialTheme.colorScheme.tertiary
            )

            PerformanceMetricsSummary(logs, exercise.weightUnit, exercise.isBodyweight)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementProgressionScreen(
    supplement: SupplementEntity,
    logs: List<SupplementLogEntity>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(supplement.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ProgressionSection(
                title = "Dosage History (${supplement.unit.label})",
                points = logs.map { GraphPoint(it.timestamp, it.dosage) },
                unit = supplement.unit.label,
                primaryColor = MaterialTheme.colorScheme.tertiary
            )

            SupplementStatsSummary(logs, supplement.unit.label)
        }
    }
}

@Composable
private fun ProgressionSection(
    title: String,
    points: List<GraphPoint>,
    unit: String,
    primaryColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(250.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (points.size < 2) {
                    Text("Need at least 2 logs to show progression.", modifier = Modifier.align(Alignment.Center))
                } else {
                    LineGraph(
                        points = points,
                        unit = unit,
                        primaryColor = primaryColor
                    )
                }
            }
        }
    }
}
