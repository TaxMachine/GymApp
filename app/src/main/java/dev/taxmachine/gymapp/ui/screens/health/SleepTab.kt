package dev.taxmachine.gymapp.ui.screens.health

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.taxmachine.gymapp.db.GymDao
import dev.taxmachine.gymapp.db.HealthSleepLogEntity
import dev.taxmachine.gymapp.ui.components.GraphPoint
import dev.taxmachine.gymapp.ui.components.LineGraph
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Composable
fun SleepTab(dao: GymDao) {
    val logs by dao.getAllHealthSleepLogs().collectAsState(initial = emptyList())
    
    if (logs.isEmpty()) {
        EmptyState("No sleep data found. Tap refresh to sync.")
    } else {
        val lastNight = remember(logs) { logs.firstOrNull() }
        var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
        
        val logsByDate = remember(logs) {
            logs.associateBy { 
                Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
            }
        }
        val selectedLog = remember(selectedDate, logsByDate) { logsByDate[selectedDate] }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                if (lastNight != null) {
                    SleepSessionGraphCard(
                        title = "Last Night (${DateTimeFormatter.ofPattern("MMM dd").format(Instant.ofEpochMilli(lastNight.startTime).atZone(ZoneId.systemDefault()))})",
                        session = lastNight,
                        dao = dao
                    )
                }
            }

            item {
                Text(
                    "Sleep Calendar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SleepCalendar(
                    logs = logs,
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )
            }

            item {
                AnimatedVisibility(visible = selectedLog != null) {
                    if (selectedLog != null) {
                        SleepSessionGraphCard(
                            title = "Details: ${selectedDate?.format(DateTimeFormatter.ofPattern("MMM dd"))}",
                            session = selectedLog,
                            dao = dao
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SleepSessionGraphCard(title: String, session: HealthSleepLogEntity, dao: GymDao) {
    val stages by dao.getSleepStagesForSession(session.id).collectAsState(initial = emptyList())
    
    val graphPoints = remember(stages) {
        stages.map { GraphPoint(it.startTime, it.stage.toFloat()) }
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Score: ${session.sleepScore}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (graphPoints.isNotEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    LineGraph(
                        points = graphPoints,
                        unit = "",
                        primaryColor = MaterialTheme.colorScheme.secondary,
                        secondaryColor = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StageLegend("Deep", MaterialTheme.colorScheme.primary)
                    StageLegend("Light", MaterialTheme.colorScheme.secondary)
                    StageLegend("REM", MaterialTheme.colorScheme.tertiary)
                    StageLegend("Awake", MaterialTheme.colorScheme.error)
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No stage data available for this session", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun StageLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
    }
}

@Composable
fun SleepCalendar(
    logs: List<HealthSleepLogEntity>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val daysInMonth = remember(currentMonth) { currentMonth.lengthOfMonth() }
    val firstDayOfWeek = remember(currentMonth) { currentMonth.atDay(1).dayOfWeek.value % 7 }
    
    val logsByDate = remember(logs) {
        logs.associateBy { 
            Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, null) }
            Text(text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}", fontWeight = FontWeight.Bold)
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, null) }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(text = day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        val totalCells = ((daysInMonth + firstDayOfWeek + 6) / 7) * 7
        Column {
            for (row in 0 until (totalCells / 7)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val dayIndex = row * 7 + col - firstDayOfWeek + 1
                        if (dayIndex in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayIndex)
                            val log = logsByDate[date]
                            val isSelected = date == selectedDate
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            log != null -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayIndex.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
