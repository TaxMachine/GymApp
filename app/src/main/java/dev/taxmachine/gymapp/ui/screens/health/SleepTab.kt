package dev.taxmachine.gymapp.ui.screens.health

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.taxmachine.gymapp.db.GymDao
import dev.taxmachine.gymapp.db.HealthSleepLogEntity
import dev.taxmachine.gymapp.db.HealthSleepStageEntity
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

private object SleepGraphConstants {
    val AWAKE_COLOR = Color(0xFFFF5252)
    val REM_COLOR = Color(0xFF81C784)
    val LIGHT_COLOR = Color(0xFF40C4FF)
    val DEEP_COLOR = Color(0xFF3F51B5)
    
    // Score Colors
    val SCORE_POOR = Color(0xFFFF5252)    // Red
    val SCORE_FAIR = Color(0xFFFFB74D)    // Orange
    val SCORE_GOOD = Color(0xFFAED581)    // Light Green
    const val SCORE_EXCELLENT_HEX = 0xFF66BB6A // Green
    val SCORE_EXCELLENT = Color(SCORE_EXCELLENT_HEX)

    const val Y_REM = 0f
    const val Y_LIGHT = 0.33f
    const val Y_DEEP = 0.66f
    const val Y_AWAKE = 1f
    
    const val STAGE_FILL_ALPHA = 0.3f
    val LINE_STROKE_WIDTH = 3.dp
    val GRAPH_HEIGHT = 150.dp
    
    val HEART_RATE_ICON_SIZE = 16.dp
    val LEGEND_DOT_SIZE = 8.dp
    val LEGEND_FONT_SIZE = 10.sp

    val SCORE_GRAPH_HEIGHT = 100.dp

    fun getScoreColor(score: Int): Color {
        return when {
            score >= 85 -> SCORE_EXCELLENT
            score >= 70 -> SCORE_GOOD
            score >= 50 -> SCORE_FAIR
            else -> SCORE_POOR
        }
    }
}

@Composable
fun SleepTab(dao: GymDao) {
    val logs by dao.getAllHealthSleepLogs().collectAsState(initial = emptyList())
    
    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No sleep data found. Tap refresh to sync.")
        }
    } else {
        // Find the "significant" session for Last Night (longest of the 3 most recent sessions)
        // This avoids picking up tiny "wake up" or "nap" sessions as the main display
        val lastNight = remember(logs) {
            logs.take(3).maxByOrNull { it.durationMinutes } ?: logs.firstOrNull()
        }
        
        var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
        
        // When associating logs by date, we keep the longest session for each date
        val logsByDate = remember(logs) {
            logs.sortedBy { it.durationMinutes }.associateBy { 
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
                SleepScoreHistoryCard(logs = logs)
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
fun SleepScoreHistoryCard(logs: List<HealthSleepLogEntity>) {
    val last30DaysLogs = remember(logs) {
        val thirtyDaysAgo = Instant.now().minusSeconds(30L * 24 * 60 * 60).toEpochMilli()
        logs.filter { it.startTime >= thirtyDaysAgo }.reversed()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sleep Score (Last 30 Days)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (last30DaysLogs.isNotEmpty()) {
                SleepScoreGraph(logs = last30DaysLogs, modifier = Modifier.fillMaxWidth().height(SleepGraphConstants.SCORE_GRAPH_HEIGHT))
            } else {
                Box(Modifier.fillMaxWidth().height(SleepGraphConstants.SCORE_GRAPH_HEIGHT), contentAlignment = Alignment.Center) {
                    Text("No historical data available", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun SleepScoreGraph(logs: List<HealthSleepLogEntity>, modifier: Modifier = Modifier) {
    val scores = remember(logs) { logs.map { it.sleepScore.toFloat() } }
    val maxScore = 100f
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Column(modifier = modifier) {
        Box(modifier = Modifier.weight(1f)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (logs.size < 2) {
                    if (logs.isNotEmpty()) {
                        val x = size.width / 2
                        val y = size.height - (scores[0] / maxScore * size.height)
                        drawCircle(primaryColor, radius = 4.dp.toPx(), center = Offset(x, y))
                    }
                    return@Canvas
                }

                val width = size.width
                val height = size.height
                val spacing = width / (logs.size - 1)

                val points = scores.mapIndexed { index, score ->
                    val x = index * spacing
                    val y = height - (score / maxScore * height)
                    Offset(x, y)
                }

                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }

                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 2.dp.toPx())
                )
                
                points.forEach { point ->
                    drawCircle(
                        color = primaryColor,
                        radius = 3.dp.toPx(),
                        center = point
                    )
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            val formatter = DateTimeFormatter.ofPattern("MMM dd")
            val startText = Instant.ofEpochMilli(logs.first().startTime).atZone(ZoneId.systemDefault()).format(formatter)
            val endText = Instant.ofEpochMilli(logs.last().startTime).atZone(ZoneId.systemDefault()).format(formatter)
            
            Text(startText, style = MaterialTheme.typography.labelSmall, color = labelColor)
            Text(endText, style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
    }
}

@Composable
fun SleepSessionGraphCard(title: String, session: HealthSleepLogEntity, dao: GymDao) {
    val stages by dao.getSleepStagesForSession(session.id).collectAsState(initial = emptyList())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Score: ${session.sleepScore}",
                        style = MaterialTheme.typography.labelLarge,
                        color = SleepGraphConstants.getScoreColor(session.sleepScore),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.Favorite, 
                        contentDescription = null, 
                        tint = Color.Red, 
                        modifier = Modifier.size(SleepGraphConstants.HEART_RATE_ICON_SIZE)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (session.avgHeartRate > 0) "${session.avgHeartRate} bpm" else "-- bpm", 
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (stages.isNotEmpty()) {
                SleepStageGraph(stages = stages, modifier = Modifier.fillMaxWidth().height(SleepGraphConstants.GRAPH_HEIGHT + 24.dp))
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StageLegend("Awake", SleepGraphConstants.AWAKE_COLOR)
                    StageLegend("REM", SleepGraphConstants.REM_COLOR)
                    StageLegend("Light", SleepGraphConstants.LIGHT_COLOR)
                    StageLegend("Deep", SleepGraphConstants.DEEP_COLOR)
                }
            } else {
                Box(Modifier.fillMaxWidth().height(SleepGraphConstants.GRAPH_HEIGHT), contentAlignment = Alignment.Center) {
                    Text("No stage data available", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun SleepStageGraph(stages: List<HealthSleepStageEntity>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    fun stageToY(stage: Int): Float {
        return when (stage) {
            6 -> SleepGraphConstants.Y_REM
            2, 4 -> SleepGraphConstants.Y_LIGHT
            5 -> SleepGraphConstants.Y_DEEP
            1 -> SleepGraphConstants.Y_AWAKE
            else -> SleepGraphConstants.Y_LIGHT
        }
    }

    val sortedStages = remember(stages) { stages.sortedBy { it.startTime } }
    val sessionStart = sortedStages.firstOrNull()?.startTime ?: 0L
    val sessionEnd = sortedStages.lastOrNull()?.endTime ?: 0L
    val totalDuration = (sessionEnd - sessionStart).coerceAtLeast(1L).toFloat()

    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Column(modifier = modifier) {
        Row(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("REM", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = labelColor)
                Text("Light", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = labelColor)
                Text("Deep", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = labelColor)
                Text("Awake", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = labelColor)
            }

            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val width = size.width
                val height = size.height

                val path = Path()
                var first = true

                sortedStages.forEach { stage ->
                    val startX = ((stage.startTime - sessionStart) / totalDuration) * width
                    val endX = ((stage.endTime - sessionStart) / totalDuration) * width
                    val y = stageToY(stage.stage) * height

                    if (first) {
                        path.moveTo(startX, y)
                        first = false
                    } else {
                        path.lineTo(startX, y)
                    }
                    path.lineTo(endX, y)

                    drawRect(
                        color = when (stage.stage) {
                            1 -> SleepGraphConstants.AWAKE_COLOR
                            6 -> SleepGraphConstants.REM_COLOR
                            2, 4 -> SleepGraphConstants.LIGHT_COLOR
                            5 -> SleepGraphConstants.DEEP_COLOR
                            else -> SleepGraphConstants.LIGHT_COLOR
                        }.copy(alpha = SleepGraphConstants.STAGE_FILL_ALPHA),
                        topLeft = Offset(startX, y),
                        size = androidx.compose.ui.geometry.Size(endX - startX, height - y)
                    )
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = SleepGraphConstants.LINE_STROKE_WIDTH.toPx())
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val startTimeText = Instant.ofEpochMilli(sessionStart).atZone(ZoneId.systemDefault()).format(timeFormatter)
            val endTimeText = Instant.ofEpochMilli(sessionEnd).atZone(ZoneId.systemDefault()).format(timeFormatter)
            
            Text(startTimeText, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = labelColor)
            
            if (totalDuration > 4 * 60 * 60 * 1000) {
                val midTimeText = Instant.ofEpochMilli(sessionStart + (totalDuration / 2).toLong()).atZone(ZoneId.systemDefault()).format(timeFormatter)
                Text(midTimeText, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = labelColor)
            }
            
            Text(endTimeText, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = labelColor)
        }
    }
}

@Composable
fun StageLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(SleepGraphConstants.LEGEND_DOT_SIZE).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = SleepGraphConstants.LEGEND_FONT_SIZE)
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
        logs.sortedBy { it.durationMinutes }.associateBy { 
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
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        else Modifier
                                    )
                                    .padding(if (isSelected) 2.dp else 0.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (log != null) SleepGraphConstants.getScoreColor(log.sleepScore).copy(alpha = 0.6f)
                                        else Color.Transparent
                                    )
                                    .clickable { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayIndex.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (log != null) Color.White else MaterialTheme.colorScheme.onSurface
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
