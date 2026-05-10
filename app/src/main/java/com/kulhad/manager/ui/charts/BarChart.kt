package com.kulhad.manager.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.TextTertiary

/** Lightweight vertical bar chart (Compose-only — no Vico dependency for snappy small charts). */
@Composable
fun SimpleBarChart(
    values: List<Float>,
    labels: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    barColor: Color = PrimaryBlue,
    chartHeight: Dp = 60.dp,
    perBarColor: ((Int, Float) -> Color)? = null,
    showLabels: Boolean = true
) {
    val maxV = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            values.forEachIndexed { idx, v ->
                val frac = (v / maxV).coerceIn(0f, 1f)
                val color = perBarColor?.invoke(idx, v) ?: barColor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((chartHeight.value * frac).dp.coerceAtLeast(2.dp))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(color)
                    )
                }
            }
        }
        if (showLabels && labels.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        color = TextTertiary,
                        fontSize = 9.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

/** Horizontal bar chart for "top N" lists. */
@Composable
fun HorizontalBarChart(
    items: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    barColor: Color = PrimaryBlue,
    rowHeight: Dp = 16.dp
) {
    val maxV = (items.maxOfOrNull { it.second } ?: 0f).coerceAtLeast(1f)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (label, v) ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = label, color = TextTertiary, fontSize = 10.sp)
                    Text(text = v.toInt().toString(), color = TextTertiary, fontSize = 10.sp)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(OverlayWhite07)
                ) {
                    val frac = (v / maxV).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(frac)
                            .height(rowHeight)
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor)
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 5.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(3.dp))
            .background(OverlayWhite07)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
    }
}
