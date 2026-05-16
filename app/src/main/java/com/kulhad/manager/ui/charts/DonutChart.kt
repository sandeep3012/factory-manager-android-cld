package com.kulhad.manager.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Two-segment donut: green-ish "good" portion + red-ish "bad" portion.
 * Renders a full grey circle if both values are 0.
 */
@Composable
fun DonutChart(
    primaryValue: Float,
    secondaryValue: Float,
    modifier: Modifier = Modifier,
    size: Dp = 77.dp,
    primaryColor: Color = Color(0xFF34D399),
    secondaryColor: Color = Color(0xFFF87171),
    strokeWidth: Dp = 10.dp,
    cutout: Float = 0.7f
) {
    Canvas(modifier = modifier.size(size)) {
        val total = primaryValue + secondaryValue
        val sw = strokeWidth.toPx()
        val arcSize = Size(this.size.width - sw, this.size.height - sw)
        val topLeft = Offset(sw / 2f, sw / 2f)

        if (total <= 0f) {
            drawArc(
                color = Color(0xFF334155),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
            return@Canvas
        }

        val primaryAngle = 360f * (primaryValue / total)
        drawArc(
            color = primaryColor,
            startAngle = -90f,
            sweepAngle = primaryAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
        drawArc(
            color = secondaryColor,
            startAngle = -90f + primaryAngle,
            sweepAngle = 360f - primaryAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
        // cutout reference (kept for clarity; visual cutout achieved via stroke)
        @Suppress("UNUSED_VARIABLE") val unused = cutout
    }
}

/** Multi-segment donut for expense breakdowns. */
@Composable
fun MultiSegmentDonut(
    segments: List<Pair<Color, Float>>,
    modifier: Modifier = Modifier,
    size: Dp = 77.dp,
    strokeWidth: Dp = 10.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val total = segments.sumOf { it.second.toDouble() }.toFloat()
        val sw = strokeWidth.toPx()
        val arcSize = Size(this.size.width - sw, this.size.height - sw)
        val topLeft = Offset(sw / 2f, sw / 2f)

        if (total <= 0f) {
            drawArc(
                color = Color(0xFF334155),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
            return@Canvas
        }

        var startAngle = -90f
        segments.forEach { (color, value) ->
            val sweep = 360f * (value / total)
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Butt)
            )
            startAngle += sweep
        }
    }
}
