package com.kulhad.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulhad.manager.ui.theme.PrimaryBlueDark
import com.kulhad.manager.ui.theme.PurpleAccent
import com.kulhad.manager.ui.theme.Success
import com.kulhad.manager.ui.theme.WarningAmber

private val avatarPalette = listOf(
    PrimaryBlueDark,
    Success,
    PurpleAccent,
    WarningAmber,
    Color(0xFFEC4899),
    Color(0xFF06B6D4),
    Color(0xFFF97316),
    Color(0xFF8B5CF6)
)

private fun colorFor(name: String): Color {
    val hash = name.fold(0) { acc, c -> acc * 31 + c.code }
    val idx = (hash and Int.MAX_VALUE) % avatarPalette.size
    return avatarPalette[idx]
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts[0].first()}${parts[1].first()}".uppercase()
    }
}

@Composable
fun WorkerAvatar(
    name: String,
    size: Dp = 26.dp,
    fontSize: Int = 9
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colorFor(name)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialsOf(name),
            color = Color.White,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.W600
        )
    }
}
