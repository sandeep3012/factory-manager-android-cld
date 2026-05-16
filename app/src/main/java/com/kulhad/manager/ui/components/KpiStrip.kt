package com.kulhad.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextSecondary

/**
 * Compact 3-column KPI strip matching HTML design screen 3 (Labor management).
 * Each cell shows a bold value in a semantic color and a dimmed label below.
 */
@Composable
fun KpiStrip(
    items: List<Triple<String, String, Color>>,   // (value, label, valueColor)
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { (value, label, color) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceCard)
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = value,
                    color = color,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.W600
                )
                Text(
                    text = label,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    letterSpacing = 0.36.sp
                )
            }
        }
    }
}

/** Two-column stat grid used on Sales, Expense screens. */
@Composable
fun StatGrid(
    items: List<Triple<String, String, Color>>,   // (value, label, valueColor)
    modifier: Modifier = Modifier
) {
    val rows = items.chunked(2)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (value, label, color) ->
                    StatCard(
                        value = value,
                        label = label,
                        valueColor = color,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Pad if odd
                if (row.size == 1) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
