package com.kulhad.manager.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kulhad.manager.ui.theme.ErrorRed
import com.kulhad.manager.ui.theme.OverlayWhite07
import com.kulhad.manager.ui.theme.OverlayWhite15
import com.kulhad.manager.ui.theme.PrimaryBlue
import com.kulhad.manager.ui.theme.SurfaceCard
import com.kulhad.manager.ui.theme.TextField
import com.kulhad.manager.ui.theme.TextSecondary
import com.kulhad.manager.ui.theme.TextTertiary

/**
 * Floating-label text input used throughout the app.
 *
 * Visual behaviour (Material 3 OutlinedTextField):
 *  - Unfocused + empty : label sits inside the field, border is barely visible
 *                        (OverlayWhite15 — ~15 % white over SurfaceCard).
 *  - Focused or filled : label animates upward onto the border, scales to 75 %,
 *                        turns PrimaryBlue; border turns PrimaryBlue.
 *  - Error (isError)   : border and label turn ErrorRed; helper text turns ErrorRed.
 *  - Disabled          : text, label, and border fade to TextTertiary / OverlayWhite07.
 *
 * All existing callers keep their current parameter names — fully backward-compatible.
 * The [isError] parameter is new and defaults to false so no call site changes.
 */
@Composable
fun KulhadTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    helper: String? = null,
    isError: Boolean = false
) {
    OutlinedTextField(
        value          = value,
        onValueChange  = onValueChange,
        label          = { Text(text = label) },
        modifier       = modifier.fillMaxWidth(),
        placeholder    = if (placeholder.isNotEmpty()) {
                            { Text(text = placeholder, color = TextTertiary) }
                         } else null,
        singleLine     = true,
        isError        = isError,
        visualTransformation = if (isPassword) PasswordVisualTransformation()
                               else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText  = if (helper != null) {
                              { Text(text = helper, fontSize = 11.sp) }
                          } else null,
        shape  = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(

            // ── Input text ────────────────────────────────────────────────────
            focusedTextColor   = TextField,
            unfocusedTextColor = TextField,
            disabledTextColor  = TextTertiary,
            errorTextColor     = TextField,

            // ── Container (filled background) ─────────────────────────────────
            focusedContainerColor   = SurfaceCard,
            unfocusedContainerColor = SurfaceCard,
            disabledContainerColor  = SurfaceCard,
            errorContainerColor     = SurfaceCard,

            // ── Cursor ────────────────────────────────────────────────────────
            cursorColor      = PrimaryBlue,
            errorCursorColor = ErrorRed,

            // ── Text selection handles + highlight ────────────────────────────
            selectionColors = TextSelectionColors(
                handleColor      = PrimaryBlue,
                backgroundColor  = PrimaryBlue.copy(alpha = 0.25f)
            ),

            // ── Border / outline ──────────────────────────────────────────────
            focusedBorderColor   = PrimaryBlue,
            unfocusedBorderColor = OverlayWhite15,
            disabledBorderColor  = OverlayWhite07,
            errorBorderColor     = ErrorRed,

            // ── Floating label ────────────────────────────────────────────────
            focusedLabelColor   = PrimaryBlue,
            unfocusedLabelColor = TextSecondary,
            disabledLabelColor  = TextTertiary,
            errorLabelColor     = ErrorRed,

            // ── Supporting / helper text ──────────────────────────────────────
            focusedSupportingTextColor   = TextTertiary,
            unfocusedSupportingTextColor = TextTertiary,
            disabledSupportingTextColor  = TextTertiary,
            errorSupportingTextColor     = ErrorRed,

            // ── Placeholder ───────────────────────────────────────────────────
            focusedPlaceholderColor   = TextTertiary,
            unfocusedPlaceholderColor = TextTertiary,
            disabledPlaceholderColor  = TextTertiary,
            errorPlaceholderColor     = TextTertiary,
        )
    )
}
