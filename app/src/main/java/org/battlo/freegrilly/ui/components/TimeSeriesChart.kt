package org.battlo.freegrilly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.battlo.freegrilly.data.history.TempSample
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Temperature-over-time line chart with a real (time-proportional) X axis, so a long cook
 * is shown to scale instead of squeezing every sample into equal slots. Draws the optional
 * target as a dashed line and labels the time span below.
 */
@Composable
fun TimeSeriesChart(
    data: List<TempSample>,
    targetTemp: Float?,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 3f,
) {
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            if (data.size < 2) return@Canvas
            val t0 = data.first().tsMs
            val t1 = data.last().tsMs
            val span = (t1 - t0).coerceAtLeast(1L).toFloat()

            val minV = data.minOf { it.value }
            val maxBase = data.maxOf { it.value }
            val maxV = maxOf(maxBase, (targetTemp ?: maxBase) + 1f)
            val range = maxOf(maxV - minV, 1f)

            fun xOf(ts: Long): Float = (ts - t0).toFloat() / span * size.width
            fun yOf(v: Float): Float = size.height - ((v - minV) / range) * size.height

            val path = Path()
            path.moveTo(xOf(data[0].tsMs), yOf(data[0].value))
            for (i in 1 until data.size) path.lineTo(xOf(data[i].tsMs), yOf(data[i].value))
            drawPath(path, color, style = Stroke(strokeWidth))

            if (targetTemp != null) {
                val ty = yOf(targetTemp)
                if (ty in 0f..size.height) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(0f, ty),
                        end = Offset(size.width, ty),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f)),
                        strokeWidth = 1f,
                    )
                }
            }
        }

        if (data.size >= 2) {
            val t0 = data.first().tsMs
            val t1 = data.last().tsMs
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(clock(t0), style = MaterialTheme.typography.labelSmall, color = axisColor)
                Text(duration(t1 - t0), style = MaterialTheme.typography.labelSmall, color = axisColor)
                Text(clock(t1), style = MaterialTheme.typography.labelSmall, color = axisColor)
            }
        }
    }
}

private fun clock(tsMs: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(tsMs))

private fun duration(ms: Long): String {
    val totalMin = (ms / 60_000L).toInt()
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${m}min" else "${m}min"
}
