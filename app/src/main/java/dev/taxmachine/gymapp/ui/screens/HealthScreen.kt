package dev.taxmachine.gymapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.taxmachine.gymapp.db.GymDao
import dev.taxmachine.gymapp.health.HealthConnectManager
import dev.taxmachine.gymapp.ui.screens.health.NutritionTab
import dev.taxmachine.gymapp.ui.screens.health.SleepTab
import dev.taxmachine.gymapp.ui.screens.health.WeightTab

@Composable
fun HealthScreen(
    healthConnectManager: HealthConnectManager, 
    dao: GymDao, // Pass dao directly from MainScreen
    onRequestPermissions: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Sleep", "Weight", "Nutrition")

    var hasPermissions by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        hasPermissions = healthConnectManager.hasAllPermissions()
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (!hasPermissions) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Health Connect permissions are required to view this data.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRequestPermissions) {
                        Text("Grant Permissions")
                    }
                }
            }
        } else {
            when (selectedTab) {
                0 -> SleepTab(dao)
                1 -> WeightTab(dao)
                2 -> NutritionTab(dao)
            }
        }
    }
}
