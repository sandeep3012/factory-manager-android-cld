package com.kulhad.manager.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = PrimaryBlueLight,
    secondary = InfoBlue,
    onSecondary = TextPrimary,
    tertiary = PurpleAccent,
    onTertiary = TextPrimary,
    background = BgDeep,
    onBackground = TextPrimary,
    surface = SurfaceNav,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = BorderLine,
    error = ErrorRed,
    onError = TextPrimary
)

// Light scheme — provided per spec ("Deep navy dark mode + clean light mode")
// but the app is dark-first; light is a sane fallback.
private val LightColors = lightColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = InfoBlue,
    onSecondary = TextPrimary,
    tertiary = PurpleAccent,
    onTertiary = TextPrimary,
    background = TextPrimary,
    onBackground = BgDeep,
    surface = TextPrimary,
    onSurface = BgDeep,
    surfaceVariant = PrimaryBlueLight,
    onSurfaceVariant = BgDeep,
    outline = TextTertiary,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun KulhadTheme(
    darkTheme: Boolean = true, // dark-first per spec
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Status bar colour is set explicitly; navigation bar colour is left
                // transparent so enableEdgeToEdge() + BottomNavBar.navigationBarsPadding()
                // handle the nav-bar area correctly on all device nav modes.
                window.statusBarColor = StatusBarColor.toArgb()
                WindowCompat.getInsetsController(window, view)
                    .isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = KulhadTypography,
        content = content
    )
}

// Override default isSystemInDarkTheme — keeps a single source of truth
@Composable
fun rememberThemeIsDark(): Boolean = isSystemInDarkTheme() || true
