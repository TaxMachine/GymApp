package dev.taxmachine.gymapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.taxmachine.gymapp.db.HealthSleepStageEntity
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

object SleepGraphDefaults {
    val AWAKE_COLOR = Color(0xFFFF5252)
    val REM_COLOR = Color(0xFF81C784)
    val LIGHT_COLOR = Color(0xFF40C4FF)
    val DEEP_COLOR = Color(0xFF3F51B5)
    
    val SCORE_POOR = Color(0xFFFF5252)
    val SCORE_FAIR = Color(0xFFFFB74D)
    val SCORE_GOOD = Color(0xFFAED581)
    val SCORE_EXCELLENT = Color(0xFF66BB6A)

    const val Y_REM = 0f
    const val Y_LIGHT = 0.33f
    const val Y_DEEP = 0.66f
    const val Y_AWAKE = 1f
    
    const val STAGE_FILL_ALPHA = 0.3f
    val LINE_STROKE_WIDTH = 3.dp
    val GRAPH_HEIGHT = 150.dp
    
    val LEGEND_DOT_SIZE = 8.dp
    val LEGEND_FONT_SIZE = 10.sp

    fun getScoreColor(score: Int): Color {
        return when {
            score >= 85 -> SCORE_EXCELLENT
            score >= 70 -> SCORE_GOOD
            score >= 50 -> SCORE_FAIR
            else -> SCORE_POOR
        }
    }
}

data class GraphPoint(val timestamp: Long, val value: Float)

@Composable
fun LineGraph(
    points: List<GraphPoint>,
    unit: String,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary
) {
    if (points.isEmpty()) return

    val values = remember(points) { points.map { it.value } }
    val minVal = remember(values) { (values.minOrNull() ?: 0f) * 0.9f }
    val maxVal = remember(values) { (values.maxOrNull() ?: 1f) * 1.1f }
    val range = remember(minVal, maxVal) { if (maxVal == minVal) 1f else maxVal - minVal }
    
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${"%.1f".format(maxVal)}$unit", style = MaterialTheme.typography.labelSmall, color = labelColor)
            val timeRange = if (points.size >= 2) {
                val duration = points.last().timestamp - points.first().timestamp
                val days = duration / (1000 * 60 * 60 * 24)
                "${days}d range"
            } else ""
            Text(timeRange, style = MaterialTheme.typography.labelSmall, color = labelColor, fontWeight = FontWeight.Bold)
        }

        Canvas(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp)) {
            if (points.size < 2) return@Canvas

            val width = size.width
            val height = size.height
            val spacing = width / (points.size - 1)

            val canvasPoints = points.mapIndexed { index, point ->
                val x = index * spacing
                val y = height - ((point.value - minVal) / range * height)
                Offset(x, y)
            }

            val path = Path().apply {
                moveTo(canvasPoints.first().x, canvasPoints.first().y)
                canvasPoints.drop(1).forEach { lineTo(it.x, it.y) }
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 4f)
            )
            
            canvasPoints.forEach { point ->
                drawCircle(
                    color = secondaryColor,
                    radius = 6f,
                    center = point
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dateFormat.format(Date(points.first().timestamp)), style = MaterialTheme.typography.labelSmall, color = labelColor)
            Text("${"%.1f".format(minVal)}$unit", style = MaterialTheme.typography.labelSmall, color = labelColor)
            Text(dateFormat.format(Date(points.last().timestamp)), style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
    }
}

@Composable
fun RowScope.RangeGauge(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    greenRange: ClosedFloatingPointRange<Float>,
    suffix: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isGood = value in greenRange
    val statusColor = if (isGood) Color(0xFF66BB6A) else Color(0xFFFF5252)

    Column(modifier = modifier.weight(1f).padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text("${value.toInt()}$suffix", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = statusColor)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.CenterStart) {
            Canvas(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)) {
                val width = size.width
                val height = size.height
                val totalRange = (max - min).coerceAtLeast(1f)
                
                val gStart = ((greenRange.start - min) / totalRange).coerceIn(0f, 1f) * width
                val gEnd = ((greenRange.endInclusive - min) / totalRange).coerceIn(0f, 1f) * width
                
                drawRect(color = Color(0xFFFF5252).copy(alpha = 0.2f), topLeft = Offset(0f, 0f), size = Size(gStart, height))
                drawRect(color = Color(0xFF66BB6A).copy(alpha = 0.4f), topLeft = Offset(gStart, 0f), size = Size(gEnd - gStart, height))
                drawRect(color = Color(0xFFFF5252).copy(alpha = 0.2f), topLeft = Offset(gEnd, 0f), size = Size(width - gEnd, height))
            }
            
            Canvas(modifier = Modifier.fillMaxWidth().height(18.dp)) {
                val width = size.width
                val height = size.height
                val markerX = ((value - min) / (max - min).coerceAtLeast(1f)).coerceIn(0f, 1f) * width
                
                drawCircle(color = color.copy(alpha = 0.3f), radius = 8.dp.toPx(), center = Offset(markerX, height / 2))
                drawCircle(color = color, radius = 5.dp.toPx(), center = Offset(markerX, height / 2))
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(markerX, height / 2))
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
            6 -> SleepGraphDefaults.Y_REM
            2, 4 -> SleepGraphDefaults.Y_LIGHT
            5 -> SleepGraphDefaults.Y_DEEP
            1 -> SleepGraphDefaults.Y_AWAKE
            else -> SleepGraphDefaults.Y_LIGHT
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
                            1 -> SleepGraphDefaults.AWAKE_COLOR
                            6 -> SleepGraphDefaults.REM_COLOR
                            2, 4 -> SleepGraphDefaults.LIGHT_COLOR
                            5 -> SleepGraphDefaults.DEEP_COLOR
                            else -> SleepGraphDefaults.LIGHT_COLOR
                        }.copy(alpha = SleepGraphDefaults.STAGE_FILL_ALPHA),
                        topLeft = Offset(startX, y),
                        size = Size(endX - startX, height - y)
                    )
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = SleepGraphDefaults.LINE_STROKE_WIDTH.toPx())
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
        Box(modifier = Modifier.size(SleepGraphDefaults.LEGEND_DOT_SIZE).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = SleepGraphDefaults.LEGEND_FONT_SIZE)
    }
}
