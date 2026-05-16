package com.kulhad.manager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulhad.manager.ui.theme.OverlayWhite15
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.SuccessDark
import com.kulhad.manager.ui.theme.TextPrimary

enum class KulhadButtonStyle { PRIMARY, SUCCESS, OUTLINE }

@Composable
fun KulhadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: KulhadButtonStyle = KulhadButtonStyle.PRIMARY,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(12.dp)
    val padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    when (style) {
        KulhadButtonStyle.PRIMARY -> Button(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                disabledContainerColor = PrimaryBlue.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.6f)
            ),
            contentPadding = padding
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.W500)
            }
        }

        KulhadButtonStyle.SUCCESS -> Button(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = SuccessDark,
                contentColor = Color.White,
                disabledContainerColor = SuccessDark.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.6f)
            ),
            contentPadding = padding
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.W500)
            }
        }

        KulhadButtonStyle.OUTLINE -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextPrimary
            ),
            border = BorderStroke(0.5.dp, OverlayWhite15),
            contentPadding = padding
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = text, fontSize = 14.sp)
            }
        }
    }
}
