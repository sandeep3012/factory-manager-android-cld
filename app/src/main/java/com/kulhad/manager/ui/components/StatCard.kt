package com.kulhad.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextPrimary
import com.kulhad.manager.ui.theme.TextSecondary

@Composable
fun StatCard(
    value: String,
    label: String,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier,
    bgColor: Color = SurfaceCard
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.W600
        )
        Text(
            text = label.uppercase(),
            color = TextSecondary,
            fontSize = 11.sp,
            letterSpacing = 0.6.sp
        )
    }
}
