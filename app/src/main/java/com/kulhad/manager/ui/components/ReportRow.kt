package com.kulhad.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

/**
 * Key-value row with an optional divider at the bottom.
 * Matches HTML screens 8 (Finance) and 9 (Reports) report rows.
 */
@Composable
fun ReportRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
    labelColor: Color = TextSecondary,
    bold: Boolean = false,
    showDivider: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = labelColor,
                fontSize = if (bold) 14.sp else 12.sp,
                fontWeight = if (bold) FontWeight.W600 else FontWeight.W400
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = if (bold) 14.sp else 13.sp,
                fontWeight = if (bold) FontWeight.W600 else FontWeight.W500
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(OverlayWhite07)
            )
        }
    }
}

/**
 * A card-wrapped block of ReportRows for grouping breakdown items.
 */
@Composable
fun ReportRowGroup(
    rows: List<Triple<String, String, Color>>,   // (label, value, valueColor)
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEachIndexed { idx, (label, value, color) ->
            ReportRow(
                label = label,
                value = value,
                valueColor = color,
                showDivider = idx < rows.lastIndex
            )
        }
    }
}
