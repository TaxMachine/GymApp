package dev.taxmachine.gymapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.taxmachine.gymapp.ui.screens.calculator.InjectionTab
import dev.taxmachine.gymapp.ui.screens.calculator.NasalTab

@Composable
fun PeptideCalculatorScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Injection", "Nasal Spray")

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
        
        when (selectedTab) {
            0 -> InjectionTab()
            1 -> NasalTab()
        }
    }
}
