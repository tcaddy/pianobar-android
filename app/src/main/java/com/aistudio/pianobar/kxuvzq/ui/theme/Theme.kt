package com.aistudio.pianobar.kxuvzq.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Supplies the selected palette to every terminal color accessor. */
internal val LocalPianobarPalette = compositionLocalOf { MidnightPalette }

private fun PianobarPalette.asColorScheme() =
    if (isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = surfaceElevated,
            onPrimaryContainer = accent,
            secondary = accentAlt,
            onSecondary = onAccentAlt,
            secondaryContainer = surfaceElevated,
            onSecondaryContainer = accentAlt,
            tertiary = success,
            onTertiary = onSuccess,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceElevated,
            onSurfaceVariant = textSecondary,
            outline = border,
            error = danger,
            onError = onAccent
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = surfaceElevated,
            onPrimaryContainer = accent,
            secondary = accentAlt,
            onSecondary = onAccentAlt,
            secondaryContainer = surfaceElevated,
            onSecondaryContainer = accentAlt,
            tertiary = success,
            onTertiary = onSuccess,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceElevated,
            onSurfaceVariant = textSecondary,
            outline = border,
            error = danger,
            onError = onAccent
        )
    }

@Composable
fun MyApplicationTheme(
    themeId: String = PianobarThemes.DEFAULT_ID,
    content: @Composable () -> Unit
) {
    val palette = PianobarThemes.fromId(themeId)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val backgroundColor = palette.background.toArgb()
                window.setBackgroundDrawable(ColorDrawable(backgroundColor))
                window.statusBarColor = backgroundColor
                window.navigationBarColor = backgroundColor
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !palette.isDark
                insetsController.isAppearanceLightNavigationBars = !palette.isDark
            }
        }
    }

    CompositionLocalProvider(LocalPianobarPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.asColorScheme(),
            typography = typographyFor(palette.typographyStyle),
            content = content
        )
    }
}
