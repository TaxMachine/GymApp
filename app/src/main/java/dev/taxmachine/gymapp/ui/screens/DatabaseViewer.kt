package dev.taxmachine.gymapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.taxmachine.gymapp.db.AppLogLevel
import dev.taxmachine.gymapp.db.GymDao
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseViewer(dao: GymDao, onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Logs", "Badges", "Splits", "Exercises", "W. Logs", "Supps", "S. Logs", "Themes", "Sleep", "Weight", "Nutrition")
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Database Viewer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = { scope.launch { dao.clearAppLogs() } }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    val logs by dao.getAllAppLogs().collectAsState(initial = emptyList())
                    AppLogTableView(logs)
                }
                1 -> TableView(dao.getAllBadges().collectAsState(initial = emptyList()).value)
                2 -> TableView(dao.getAllSplits().collectAsState(initial = emptyList()).value)
                3 -> TableView(dao.getAllExercises().collectAsState(initial = emptyList()).value)
                4 -> TableView(dao.getAllWeightLogs().collectAsState(initial = emptyList()).value)
                5 -> TableView(dao.getAllSupplements().collectAsState(initial = emptyList()).value)
                6 -> TableView(dao.getAllSupplementLogs().collectAsState(initial = emptyList()).value)
                7 -> TableView(dao.getAllCustomThemeColors().collectAsState(initial = emptyList()).value)
                8 -> TableView(dao.getAllHealthSleepLogs().collectAsState(initial = emptyList()).value)
                9 -> TableView(dao.getAllHealthWeightLogs().collectAsState(initial = emptyList()).value)
                10 -> TableView(dao.getAllHealthNutritionLogs().collectAsState(initial = emptyList()).value)
            }
        }
    }
}

@Composable
fun AppLogTableView(data: List<dev.taxmachine.gymapp.db.AppLogEntity>) {
    if (data.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No system logs")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(data) { item ->
                val color = when(item.level) {
                    AppLogLevel.ERROR -> MaterialTheme.colorScheme.errorContainer
                    AppLogLevel.WARNING -> Color(0xFFFFF9C4) // Yellowish
                    AppLogLevel.INFO -> MaterialTheme.colorScheme.secondaryContainer
                    AppLogLevel.DEBUG -> MaterialTheme.colorScheme.surfaceVariant
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = color)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.level} | ${item.tag}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            val date = java.time.Instant.ofEpochMilli(item.timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
                            Text(date.toString().substring(0, 8), style = MaterialTheme.typography.labelSmall)
                        }
                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun <T> TableView(data: List<T>) {
    if (data.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No data in this table")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(data) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = item.toString(),
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}
