package com.kulhad.manager.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.Success

/** Simple line chart. Renders nothing if values is empty. */
@Composable
fun SimpleLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Success,
    chartHeight: Dp = 60.dp,
    showGrid: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth().height(chartHeight)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            if (values.isEmpty()) return@Canvas
            val maxV = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
            val minV = (values.minOrNull() ?: 0f)
            val range = (maxV - minV).coerceAtLeast(1f)
            val w = size.width
            val h = size.height
            val padY = h * 0.1f

            if (showGrid) {
                val gridStroke = Stroke(
                    width = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
                listOf(0.25f, 0.5f, 0.75f).forEach { f ->
                    val y = h * f
                    drawLine(
                        color = OverlayWhite07,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = gridStroke.width,
                        pathEffect = gridStroke.pathEffect
                    )
                }
            }

            val path = Path()
            values.forEachIndexed { i, v ->
                val x = if (values.size == 1) w / 2f else w * i / (values.size - 1).toFloat()
                val frac = (v - minV) / range
                val y = h - padY - (h - 2 * padY) * frac
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )

            // dots
            values.forEachIndexed { i, v ->
                val x = if (values.size == 1) w / 2f else w * i / (values.size - 1).toFloat()
                val frac = (v - minV) / range
                val y = h - padY - (h - 2 * padY) * frac
                drawCircle(color = lineColor, radius = 4f, center = Offset(x, y))
            }
        }
    }
}
