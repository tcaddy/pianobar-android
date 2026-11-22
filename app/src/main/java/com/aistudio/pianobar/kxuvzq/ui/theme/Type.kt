package com.aistudio.pianobar.kxuvzq.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aistudio.pianobar.kxuvzq.R

val PlusJakartaFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
)

val NewsreaderFontFamily = FontFamily(
    Font(R.font.newsreader, FontWeight.Normal),
    Font(R.font.newsreader, FontWeight.Medium),
    Font(R.font.newsreader, FontWeight.SemiBold),
    Font(R.font.newsreader, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

val IbmPlexMonoFontFamily = FontFamily(
    Font(R.font.ibm_plex_mono, FontWeight.Normal),
    Font(R.font.ibm_plex_mono, FontWeight.Medium),
    Font(R.font.ibm_plex_mono, FontWeight.SemiBold),
    Font(R.font.ibm_plex_mono, FontWeight.Bold)
)

fun displayFontFor(style: PaletteTypographyStyle): FontFamily = when (style) {
    PaletteTypographyStyle.EDITORIAL -> NewsreaderFontFamily
    PaletteTypographyStyle.CRT_MONO -> IbmPlexMonoFontFamily
    PaletteTypographyStyle.DEVELOPER -> JetBrainsMonoFontFamily
    PaletteTypographyStyle.TECHNICAL -> PlusJakartaFontFamily
    PaletteTypographyStyle.MODERN_DARK -> PlusJakartaFontFamily
    PaletteTypographyStyle.AI_SLOP -> FontFamily.SansSerif
}

fun bodyFontFor(style: PaletteTypographyStyle): FontFamily = when (style) {
    PaletteTypographyStyle.AI_SLOP -> FontFamily.SansSerif
    else -> PlusJakartaFontFamily
}

fun monoFontFor(style: PaletteTypographyStyle): FontFamily = when (style) {
    PaletteTypographyStyle.CRT_MONO,
    PaletteTypographyStyle.TECHNICAL,
    PaletteTypographyStyle.EDITORIAL -> IbmPlexMonoFontFamily
    PaletteTypographyStyle.MODERN_DARK,
    PaletteTypographyStyle.DEVELOPER -> JetBrainsMonoFontFamily
    PaletteTypographyStyle.AI_SLOP -> FontFamily.Monospace
}

fun typographyFor(style: PaletteTypographyStyle): Typography {
    val display = displayFontFor(style)
    val body = bodyFontFor(style)
    val mono = monoFontFor(style)

    return Typography(
        headlineMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.4).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 21.sp,
            lineHeight = 27.sp,
            letterSpacing = (-0.3).sp
        ),
        titleLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.2).sp
        ),
        titleMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.sp
        ),
        titleSmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 13.5.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodySmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            letterSpacing = 0.15.sp
        ),
        labelLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = mono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp
        ),
        labelSmall = TextStyle(
            fontFamily = mono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.3.sp
        )
    )
}

val Typography = typographyFor(PaletteTypographyStyle.MODERN_DARK)

val DisplayFontFamily: FontFamily
    @Composable get() = displayFontFor(LocalPianobarPalette.current.typographyStyle)

val BodyFontFamily: FontFamily
    @Composable get() = bodyFontFor(LocalPianobarPalette.current.typographyStyle)

val MonoFontFamily: FontFamily
    @Composable get() = monoFontFor(LocalPianobarPalette.current.typographyStyle)
