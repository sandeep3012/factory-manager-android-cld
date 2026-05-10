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
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

/**
 * Full-width hero card matching HTML design screens 2, 5, 8.
 * Shows a prominent colored value with an optional label above,
 * optional change/trend text below, and optional trailing composable
 * (e.g. a donut chart or secondary stat block).
 */
@Composable
fun HeroCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Success,
    change: String? = null,
    changeColor: Color = Success,
    trailingContent: @Composable (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label.uppercase(),
                color = TextSecondary,
                fontSize = 8.sp,
                letterSpacing = 0.6.sp
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.W600
            )
            if (change != null) {
                Text(
                    text = change,
                    color = changeColor,
                    fontSize = 9.sp
                )
            }
            extraContent?.invoke()
        }
        trailingContent?.invoke()
    }
}

/**
 * Hero card with a secondary stat block on the right side
 * (used in Payroll screen — total + rate-per-piece).
 */
@Composable
fun HeroCardDual(
    primaryLabel: String,
    primaryValue: String,
    primaryColor: Color = TextPrimary,
    secondaryLabel: String,
    secondaryValue: String,
    secondaryColor: Color = TextSecondary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = primaryLabel, color = TextSecondary, fontSize = 8.sp, letterSpacing = 0.6.sp)
            Text(text = primaryValue, color = primaryColor, fontSize = 20.sp, fontWeight = FontWeight.W600)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = secondaryLabel, color = TextSecondary, fontSize = 8.sp)
            Text(text = secondaryValue, color = secondaryColor, fontSize = 16.sp, fontWeight = FontWeight.W500)
        }
    }
}
