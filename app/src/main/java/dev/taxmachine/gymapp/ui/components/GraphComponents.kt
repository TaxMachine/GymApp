package dev.taxmachine.gymapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

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
