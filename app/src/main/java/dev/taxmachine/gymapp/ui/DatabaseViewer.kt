package dev.taxmachine.gymapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.taxmachine.gymapp.db.GymDao

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseViewer(dao: GymDao, onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Badges", "Splits", "Exercises", "W. Logs", "Supps", "S. Logs")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Database Viewer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                0 -> TableView(dao.getAllBadges().collectAsState(initial = emptyList()).value)
                1 -> TableView(dao.getAllSplits().collectAsState(initial = emptyList()).value)
                2 -> TableView(dao.getAllExercises().collectAsState(initial = emptyList()).value)
                3 -> TableView(dao.getAllWeightLogs().collectAsState(initial = emptyList()).value)
                4 -> TableView(dao.getAllSupplements().collectAsState(initial = emptyList()).value)
                5 -> TableView(dao.getAllSupplementLogs().collectAsState(initial = emptyList()).value)
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
