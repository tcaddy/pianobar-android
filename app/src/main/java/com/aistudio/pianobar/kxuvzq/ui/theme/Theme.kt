package com.aistudio.pianobar.kxuvzq.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PianobarDarkColorScheme = darkColorScheme(
    primary = PianoCyan,
    onPrimary = PianoDarkBg,
    primaryContainer = PianoSurfaceElevated,
    onPrimaryContainer = PianoCyan,
    secondary = PianoPurple,
    onSecondary = PianoDarkBg,
    secondaryContainer = PianoSurfaceElevated,
    onSecondaryContainer = PianoPurple,
    tertiary = PianoGreen,
    onTertiary = PianoDarkBg,
    background = PianoDarkBg,
    onBackground = TextPrimary,
    surface = PianoSurface,
    onSurface = TextPrimary,
    surfaceVariant = PianoSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = PianoSurfaceBorder,
    error = PianoRed,
    onError = PianoDarkBg
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = PianobarDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = PianoDarkBg.toArgb()
                window.navigationBarColor = PianoDarkBg.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
