package org.battlo.freegrilly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun Sparkline(
    data: List<Float>,
    targetTemp: Float,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 3f,
) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val minVal = data.min()
        val maxVal = maxOf(data.max(), targetTemp + 1f)
        val range = maxOf(maxVal - minVal, 1f)
        val width = size.width
        val height = size.height

        fun xOf(index: Int): Float = index.toFloat() / (data.size - 1) * width
        fun yOf(value: Float): Float = height - ((value - minVal) / range) * height

        val path = Path()
        path.moveTo(xOf(0), yOf(data[0]))
        for (i in 1 until data.size) {
            path.lineTo(xOf(i), yOf(data[i]))
        }
        drawPath(path, color, style = Stroke(strokeWidth))

        val targetY = yOf(targetTemp)
        if (targetY in 0f..height) {
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(0f, targetY),
                end = Offset(width, targetY),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f)),
                strokeWidth = 1f,
            )
        }
    }
}
