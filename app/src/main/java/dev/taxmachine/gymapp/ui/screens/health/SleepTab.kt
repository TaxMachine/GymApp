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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.taxmachine.gymapp.db.GymDao
import dev.taxmachine.gymapp.db.HealthSleepLogEntity
import dev.taxmachine.gymapp.ui.components.RangeGauge
import dev.taxmachine.gymapp.ui.components.SleepStageGraph
import dev.taxmachine.gymapp.ui.components.StageLegend
import dev.taxmachine.gymapp.utils.DataUtils
import kotlinx.coroutines.launch
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
        val lastNight = remember(logs) {
            logs.take(3).maxByOrNull { it.durationMinutes } ?: logs.firstOrNull()
        }
        
        var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
        
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(graphicsLayer)
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                            DataUtils.saveBitmapToGallery(context, bitmap, "sleep_graph_${session.id}")
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    
                    Text(
                        text = "Score: ${session.sleepScore}",
                        style = MaterialTheme.typography.labelLarge,
                        color = SleepGraphConstants.getScoreColor(session.sleepScore),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.Favorite, 
                        contentDescription = null, 
                        tint = Color.Red, 
                        modifier = Modifier.size(16.dp)
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
                SleepStageGraph(stages = stages, modifier = Modifier.fillMaxWidth().height(150.dp + 24.dp))
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StageLegend("Awake", SleepGraphConstants.AWAKE_COLOR)
                    StageLegend("REM", SleepGraphConstants.REM_COLOR)
                    StageLegend("Light", SleepGraphConstants.LIGHT_COLOR)
                    StageLegend("Deep", SleepGraphConstants.DEEP_COLOR)
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                // Gauges Section
                val deepMinutes = remember(stages) { stages.filter { it.stage == 5 }.sumOf { it.endTime - it.startTime } / 60000f }
                val remMinutes = remember(stages) { stages.filter { it.stage == 6 }.sumOf { it.endTime - it.startTime } / 60000f }
                val lightMinutes = remember(stages) { stages.filter { it.stage in listOf(2, 4) }.sumOf { it.endTime - it.startTime } / 60000f }
                val awakeMinutes = remember(stages) { stages.filter { it.stage == 1 }.sumOf { it.endTime - it.startTime } / 60000f }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        RangeGauge(
                            label = "Deep Sleep",
                            value = deepMinutes,
                            min = 0f,
                            max = 180f,
                            greenRange = 60f..120f,
                            suffix = "m",
                            color = SleepGraphConstants.DEEP_COLOR
                        )
                        RangeGauge(
                            label = "REM Sleep",
                            value = remMinutes,
                            min = 0f,
                            max = 180f,
                            greenRange = 90f..120f,
                            suffix = "m",
                            color = SleepGraphConstants.REM_COLOR
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        RangeGauge(
                            label = "Light Sleep",
                            value = lightMinutes,
                            min = 0f,
                            max = 480f,
                            greenRange = 200f..350f,
                            suffix = "m",
                            color = SleepGraphConstants.LIGHT_COLOR
                        )
                        RangeGauge(
                            label = "Awake",
                            value = awakeMinutes,
                            min = 0f,
                            max = 120f,
                            greenRange = 0f..45f,
                            suffix = "m",
                            color = SleepGraphConstants.AWAKE_COLOR
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        RangeGauge(
                            label = "Heart Rate",
                            value = session.avgHeartRate.toFloat(),
                            min = 40f,
                            max = 110f,
                            greenRange = 40f..65f,
                            suffix = " bpm",
                            color = Color(0xFFEF5350)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("No stage data available", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
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
