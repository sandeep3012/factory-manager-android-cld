package com.kulhad.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulhad.manager.ui.theme.BadgeAmberBg
import com.kulhad.manager.ui.theme.BadgeAmberText
import com.kulhad.manager.ui.theme.BadgeBlueBg
import com.kulhad.manager.ui.theme.BadgeBlueText
import com.kulhad.manager.ui.theme.BadgeGreenBg
import com.kulhad.manager.ui.theme.BadgeGreenText
import com.kulhad.manager.ui.theme.BadgePurpleBg
import com.kulhad.manager.ui.theme.BadgePurpleText
import com.kulhad.manager.ui.theme.BadgeRedBg
import com.kulhad.manager.ui.theme.BadgeRedText

enum class BadgeType { SUCCESS, ERROR, WARNING, INFO, PURPLE }

@Composable
fun StatusBadge(
    text: String,
    type: BadgeType,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (type) {
        BadgeType.SUCCESS -> BadgeGreenBg to BadgeGreenText
        BadgeType.ERROR -> BadgeRedBg to BadgeRedText
        BadgeType.WARNING -> BadgeAmberBg to BadgeAmberText
        BadgeType.INFO -> BadgeBlueBg to BadgeBlueText
        BadgeType.PURPLE -> BadgePurpleBg to BadgePurpleText
    }
    Text(
        text = text.uppercase(),
        color = fg,
        fontSize = 9.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.5.sp,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
