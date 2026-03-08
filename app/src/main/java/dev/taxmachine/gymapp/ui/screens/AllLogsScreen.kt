package dev.taxmachine.gymapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.taxmachine.gymapp.db.*
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed class LogEntry(val timestamp: Long) {
    data class Weight(val log: WeightLogEntity, val exerciseName: String) : LogEntry(log.timestamp)
    data class Supplement(val log: SupplementLogEntity, val supplementName: String) : LogEntry(log.timestamp)
    data class HealthWeight(val log: HealthWeightLogEntity) : LogEntry(log.timestamp)
    data class HealthSleep(val log: HealthSleepLogEntity) : LogEntry(log.startTime)
    data class HealthNutrition(val log: HealthNutritionLogEntity) : LogEntry(log.timestamp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllLogsScreen(dao: GymDao, onBack: () -> Unit) {
    val weightLogs by dao.getAllWeightLogs().collectAsState(initial = emptyList())
    val suppLogs by dao.getAllSupplementLogs().collectAsState(initial = emptyList())
    val healthWeightLogs by dao.getAllHealthWeightLogs().collectAsState(initial = emptyList())
    val healthSleepLogs by dao.getAllHealthSleepLogs().collectAsState(initial = emptyList())
    val healthNutritionLogs by dao.getAllHealthNutritionLogs().collectAsState(initial = emptyList())
    
    val exercises by dao.getAllExercises().collectAsState(initial = emptyList())
    val supplements by dao.getAllSupplements().collectAsState(initial = emptyList())

    val allLogs = remember(weightLogs, suppLogs, healthWeightLogs, healthSleepLogs, healthNutritionLogs, exercises, supplements) {
        val list = mutableListOf<LogEntry>()
        
        val exerciseMap = exercises.associateBy { it.id }
        val supplementMap = supplements.associateBy { it.uid }

        weightLogs.forEach { log ->
            val name = exerciseMap[log.exerciseId]?.name ?: "Unknown Exercise"
            list.add(LogEntry.Weight(log, name))
        }
        
        suppLogs.forEach { log ->
            val name = supplementMap[log.supplementUid]?.name ?: "Unknown Supplement"
            list.add(LogEntry.Supplement(log, name))
        }

        healthWeightLogs.forEach { list.add(LogEntry.HealthWeight(it)) }
        healthSleepLogs.forEach { list.add(LogEntry.HealthSleep(it)) }
        healthNutritionLogs.forEach { list.add(LogEntry.HealthNutrition(it)) }

        list.sortByDescending { it.timestamp }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Activity Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (allLogs.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No logs found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allLogs) { entry ->
                    LogCard(entry)
                }
            }
        }
    }
}

@Composable
fun LogCard(entry: LogEntry) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm").withZone(ZoneId.systemDefault())
    val dateStr = formatter.format(Instant.ofEpochMilli(entry.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when(entry) {
                is LogEntry.Weight -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                is LogEntry.Supplement -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = when(entry) {
                        is LogEntry.Weight -> "Workout"
                        is LogEntry.Supplement -> "Supplement"
                        is LogEntry.HealthWeight -> "Health: Weight"
                        is LogEntry.HealthSleep -> "Health: Sleep"
                        is LogEntry.HealthNutrition -> "Health: Nutrition"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(dateStr, style = MaterialTheme.typography.labelSmall)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = when(entry) {
                    is LogEntry.Weight -> "${entry.exerciseName}: ${entry.log.weight} ${entry.log.reps} reps"
                    is LogEntry.Supplement -> "${entry.supplementName}: ${entry.log.dosage}"
                    is LogEntry.HealthWeight -> "Weight: ${entry.log.weightKg} kg"
                    is LogEntry.HealthSleep -> "Sleep: ${entry.log.durationMinutes} mins (Score: ${entry.log.sleepScore})"
                    is LogEntry.HealthNutrition -> "Nutrition: ${entry.log.energyKcal} kcal"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
