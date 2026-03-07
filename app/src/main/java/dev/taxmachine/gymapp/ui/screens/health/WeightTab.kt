package dev.taxmachine.gymapp.ui.screens.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.taxmachine.gymapp.db.GymDao
import dev.taxmachine.gymapp.db.HealthWeightLogEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WeightTab(dao: GymDao) {
    val logs by dao.getAllHealthWeightLogs().collectAsState(initial = emptyList())
    if (logs.isEmpty()) {
        EmptyState("No weight data found. Tap refresh to sync.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs, key = { it.id }) { log -> WeightLogCard(log) }
        }
    }
}

@Composable
fun WeightLogCard(log: HealthWeightLogEntity) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm").withZone(ZoneId.systemDefault())
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(formatter.format(Instant.ofEpochMilli(log.timestamp)), style = MaterialTheme.typography.labelMedium)
            Text("${log.weightKg} kg", style = MaterialTheme.typography.titleMedium)
        }
    }
}
