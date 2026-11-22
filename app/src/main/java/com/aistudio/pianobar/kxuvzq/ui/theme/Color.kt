package com.aistudio.pianobar.kxuvzq.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Typographic personality associated with each palette.
 * Kept as a pure Kotlin enum (without Android resource references) so unit tests
 * can inspect themes without an Android Context.
 */
enum class PaletteTypographyStyle {
    MODERN_DARK,   // Plus Jakarta Sans + JetBrains Mono
    CRT_MONO,      // IBM Plex Mono display + Plus Jakarta Sans body
    TECHNICAL,     // Plus Jakarta Sans + IBM Plex Mono telemetry
    DEVELOPER,     // JetBrains Mono headings + Plus Jakarta Sans body
    EDITORIAL,     // Newsreader serif headings + Plus Jakarta Sans + IBM Plex Mono
    AI_SLOP        // High-energy sans display
}

/**
 * A complete terminal palette. Names are semantic roles, so components never
 * decide which raw hex value a theme means.
 */
data class PianobarPalette(
    val id: String,
    val name: String,
    val caption: String,
    val isDark: Boolean,
    val typographyStyle: PaletteTypographyStyle = PaletteTypographyStyle.MODERN_DARK,

    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,

    val accent: Color,
    val accentAlt: Color,
    val info: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,

    val onAccent: Color,
    val onAccentAlt: Color,
    val onSuccess: Color,

    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,

    val terminalBackground: Color,
    val terminalHeader: Color,
    val terminalSurface: Color,
    val terminalBorder: Color,
    val terminalPrimary: Color,
    val terminalText: Color,
    val terminalPrompt: Color,
    val terminalOnAccent: Color,
    val terminalWarning: Color,
    val terminalError: Color,
    val terminalInfo: Color,
    val terminalMuted: Color,

    val inputBackground: Color,
    val mutedSurface: Color,
    val disabledContent: Color,
    val dragHandle: Color
)

/** Oceanic slate-indigo terminal palette: crisp contrast and quiet depth. */
val MidnightPalette = PianobarPalette(
    id = "midnight",
    name = "Midnight",
    caption = "Deep oceanic slate · phosphor green",
    isDark = true,
    typographyStyle = PaletteTypographyStyle.MODERN_DARK,
    background = Color(0xFF0B1018),
    surface = Color(0xFF121923),
    surfaceElevated = Color(0xFF192230),
    border = Color(0xFF283548),
    accent = Color(0xFF58A6FF),
    accentAlt = Color(0xFFA78BFA),
    info = Color(0xFF60A5FA),
    success = Color(0xFF3FB950),
    warning = Color(0xFFE0A82E),
    danger = Color(0xFFF85149),
    onAccent = Color(0xFF041326),
    onAccentAlt = Color(0xFF0D0821),
    onSuccess = Color(0xFF041B0A),
    textPrimary = Color(0xFFF0F6FC),
    textSecondary = Color(0xFF9FB0C4),
    textTertiary = Color(0xFF7D90A8),
    terminalBackground = Color(0xFF080C12),
    terminalHeader = Color(0xFF04070B),
    terminalSurface = Color(0xFF0E141E),
    terminalBorder = Color(0xFF1E293B),
    terminalPrimary = Color(0xFFF0F6FC),
    terminalText = Color(0xFF3FB950),
    terminalPrompt = Color(0xFF58A6FF),
    terminalOnAccent = Color(0xFF041326),
    terminalWarning = Color(0xFFE0A82E),
    terminalError = Color(0xFFF85149),
    terminalInfo = Color(0xFF94A3B8),
    terminalMuted = Color(0xFF7D90A8),
    inputBackground = Color(0xFF192230),
    mutedSurface = Color(0xFF192230),
    disabledContent = Color(0xFF7D90A8),
    dragHandle = Color(0xFF3B4B62)
)

/** Warm monochrome CRT: P3 amber phosphor on roasted obsidian. */
val AmberCrtPalette = PianobarPalette(
    id = "amber-crt",
    name = "Amber CRT",
    caption = "P3 phosphor · IBM Plex Mono",
    isDark = true,
    typographyStyle = PaletteTypographyStyle.CRT_MONO,
    background = Color(0xFF100B04),
    surface = Color(0xFF191207),
    surfaceElevated = Color(0xFF251B0B),
    border = Color(0xFF473619),
    accent = Color(0xFFFFB000),
    accentAlt = Color(0xFFFF9233),
    info = Color(0xFFE0BA78),
    success = Color(0xFFFFD166),
    warning = Color(0xFFFF9538),
    danger = Color(0xFFFF6B57),
    onAccent = Color(0xFF1A1002),
    onAccentAlt = Color(0xFF1A0E02),
    onSuccess = Color(0xFF1A1202),
    textPrimary = Color(0xFFFFE8BA),
    textSecondary = Color(0xFFD5B47C),
    textTertiary = Color(0xFFB39158),
    terminalBackground = Color(0xFF0B0803),
    terminalHeader = Color(0xFF060401),
    terminalSurface = Color(0xFF140F05),
    terminalBorder = Color(0xFF3B2D14),
    terminalPrimary = Color(0xFFFFE8BA),
    terminalText = Color(0xFFFFB000),
    terminalPrompt = Color(0xFFFFD166),
    terminalOnAccent = Color(0xFF1A1002),
    terminalWarning = Color(0xFFFF9538),
    terminalError = Color(0xFFFF6B57),
    terminalInfo = Color(0xFFD5B47C),
    terminalMuted = Color(0xFFB39158),
    inputBackground = Color(0xFF251B0B),
    mutedSurface = Color(0xFF251B0B),
    disabledContent = Color(0xFFB39158),
    dragHandle = Color(0xFF6B5228)
)

/** Cold sapphire-slate: high-contrast winter instrument panel. */
val IcePalette = PianobarPalette(
    id = "ice",
    name = "Ice",
    caption = "Arctic slate · crisp technical sans",
    isDark = true,
    typographyStyle = PaletteTypographyStyle.TECHNICAL,
    background = Color(0xFF08101C),
    surface = Color(0xFF0F1A2C),
    surfaceElevated = Color(0xFF16253E),
    border = Color(0xFF2B4163),
    accent = Color(0xFF7DD3FC),
    accentAlt = Color(0xFFA5B4FC),
    info = Color(0xFF60A5FA),
    success = Color(0xFF5EEAD4),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFFB7185),
    onAccent = Color(0xFF041422),
    onAccentAlt = Color(0xFF090E24),
    onSuccess = Color(0xFF041C19),
    textPrimary = Color(0xFFEBF4FF),
    textSecondary = Color(0xFFA8BDD6),
    textTertiary = Color(0xFF819BB8),
    terminalBackground = Color(0xFF060C16),
    terminalHeader = Color(0xFF03070E),
    terminalSurface = Color(0xFF0C1524),
    terminalBorder = Color(0xFF223654),
    terminalPrimary = Color(0xFFEBF4FF),
    terminalText = Color(0xFF67E8F9),
    terminalPrompt = Color(0xFFA5B4FC),
    terminalOnAccent = Color(0xFF041422),
    terminalWarning = Color(0xFFFBBF24),
    terminalError = Color(0xFFFB7185),
    terminalInfo = Color(0xFFA8BDD6),
    terminalMuted = Color(0xFF819BB8),
    inputBackground = Color(0xFF16253E),
    mutedSurface = Color(0xFF16253E),
    disabledContent = Color(0xFF819BB8),
    dragHandle = Color(0xFF3E5980)
)

/** Solarized dark, perceptually balanced surfaces and AA+ text contrast. */
val SolarizedPalette = PianobarPalette(
    id = "solarized",
    name = "Solarized",
    caption = "Developer dark · teal anchor",
    isDark = true,
    typographyStyle = PaletteTypographyStyle.DEVELOPER,
    background = Color(0xFF00242D),
    surface = Color(0xFF06323D),
    surfaceElevated = Color(0xFF0B404E),
    border = Color(0xFF1F5D6C),
    accent = Color(0xFF3CC7BC),
    accentAlt = Color(0xFF9CA4F0),
    info = Color(0xFF6EB9EB),
    success = Color(0xFFA2BA00),
    warning = Color(0xFFE0AA00),
    danger = Color(0xFFFF7E75),
    onAccent = Color(0xFF002028),
    onAccentAlt = Color(0xFF080A26),
    onSuccess = Color(0xFF0E1A00),
    textPrimary = Color(0xFFF0EAD6),
    textSecondary = Color(0xFFA5B5B5),
    textTertiary = Color(0xFF8FA7AA),
    terminalBackground = Color(0xFF001D24),
    terminalHeader = Color(0xFF00161C),
    terminalSurface = Color(0xFF052A34),
    terminalBorder = Color(0xFF1B5260),
    terminalPrimary = Color(0xFFF0EAD6),
    terminalText = Color(0xFFA2BA00),
    terminalPrompt = Color(0xFF3CC7BC),
    terminalOnAccent = Color(0xFF002028),
    terminalWarning = Color(0xFFE0AA00),
    terminalError = Color(0xFFFF7E75),
    terminalInfo = Color(0xFFA5B5B5),
    terminalMuted = Color(0xFF8FA7AA),
    inputBackground = Color(0xFF0B404E),
    mutedSurface = Color(0xFF0B404E),
    disabledContent = Color(0xFF8FA7AA),
    dragHandle = Color(0xFF3A6874)
)

/** Editorial paper & ink inspired by Impeccable's "Paper and Instruments" craft. */
val NewsprintPalette = PianobarPalette(
    id = "newsprint",
    name = "Newsprint",
    caption = "Archival paper · Newsreader serif",
    isDark = false,
    typographyStyle = PaletteTypographyStyle.EDITORIAL,
    background = Color(0xFFF2EDE3),
    surface = Color(0xFFFCF9F2),
    surfaceElevated = Color(0xFFE7E1D3),
    border = Color(0xFFCBBFA9),
    accent = Color(0xFF19569A),
    accentAlt = Color(0xFF6D3991),
    info = Color(0xFF19569A),
    success = Color(0xFF1A6B40),
    warning = Color(0xFF8C5A00),
    danger = Color(0xFFB3261E),
    onAccent = Color(0xFFFFFFFF),
    onAccentAlt = Color(0xFFFFFFFF),
    onSuccess = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF181714),
    textSecondary = Color(0xFF524C3F),
    textTertiary = Color(0xFF6D6657),
    terminalBackground = Color(0xFF181714),
    terminalHeader = Color(0xFF100F0D),
    terminalSurface = Color(0xFF23211C),
    terminalBorder = Color(0xFF4A453B),
    terminalPrimary = Color(0xFFF2EDE3),
    terminalText = Color(0xFFA7C957),
    terminalPrompt = Color(0xFF7FC8F8),
    terminalOnAccent = Color(0xFF181714),
    terminalWarning = Color(0xFFF0B429),
    terminalError = Color(0xFFF87171),
    terminalInfo = Color(0xFFC8D3E0),
    terminalMuted = Color(0xFF9F9888),
    inputBackground = Color(0xFFFFFFFF),
    mutedSurface = Color(0xFFE7E1D3),
    disabledContent = Color(0xFF6D6657),
    dragHandle = Color(0xFFB0A795)
)

/**
 * An intentionally loud, opt-in parody of LLM-generated SaaS pages:
 * slate-950 glass, violet/cyan glow, emerald status, pink errors.
 */
val AiSlopPalette = PianobarPalette(
    id = "ai-slop",
    name = "AI Slop",
    caption = "Violet → cyan · peak vibecoded",
    isDark = true,
    typographyStyle = PaletteTypographyStyle.AI_SLOP,
    background = Color(0xFF020617),
    surface = Color(0xFF0B1120),
    surfaceElevated = Color(0xFF171333),
    border = Color(0xFF3B2F63),
    accent = Color(0xFFC084FC),
    accentAlt = Color(0xFF22D3EE),
    info = Color(0xFF818CF8),
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFF472B6),
    onAccent = Color(0xFF0B0514),
    onAccentAlt = Color(0xFF020617),
    onSuccess = Color(0xFF02120C),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFFCBD5E1),
    textTertiary = Color(0xFF94A3B8),
    terminalBackground = Color(0xFF060615),
    terminalHeader = Color(0xFF03030B),
    terminalSurface = Color(0xFF0D0A1F),
    terminalBorder = Color(0xFF3B2F63),
    terminalPrimary = Color(0xFFF8FAFC),
    terminalText = Color(0xFF67E8F9),
    terminalPrompt = Color(0xFFF0ABFC),
    terminalOnAccent = Color(0xFF060615),
    terminalWarning = Color(0xFFFBBF24),
    terminalError = Color(0xFFFDA4AF),
    terminalInfo = Color(0xFFA5B4FC),
    terminalMuted = Color(0xFF94A3B8),
    inputBackground = Color(0xFF171333),
    mutedSurface = Color(0xFF171333),
    disabledContent = Color(0xFF94A3B8),
    dragHandle = Color(0xFF64748B)
)

object PianobarThemes {
    const val DEFAULT_ID = "midnight"

    val all: List<PianobarPalette> = listOf(
        MidnightPalette,
        AmberCrtPalette,
        IcePalette,
        SolarizedPalette,
        NewsprintPalette,
        AiSlopPalette
    )

    fun fromId(id: String?): PianobarPalette =
        all.firstOrNull { it.id == id } ?: MidnightPalette
}

val PianoDarkBg: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.background

val PianoSurface: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.surface

val PianoSurfaceElevated: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.surfaceElevated

val PianoSurfaceBorder: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.border

val PianoCyan: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.accent

val PianoBlue: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.info

val PianoPurple: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.accentAlt

val PianoGreen: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.success

val PianoAmber: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.warning

val PianoRed: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.danger

val PianoOnAccent: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.onAccent

val PianoOnAccentAlt: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.onAccentAlt

val PianoOnSuccess: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.onSuccess

val PianoInputBackground: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.inputBackground

val PianoMutedSurface: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.mutedSurface

val PianoDisabledContent: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.disabledContent

val PianoDragHandle: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.dragHandle

val TextPrimary: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.textPrimary

val TextSecondary: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.textSecondary

val TextTertiary: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.textTertiary

val TerminalBg: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.terminalBackground

val TerminalHeader: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.terminalHeader

val TerminalSurface: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.terminalSurface

val TerminalBorderColor: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.terminalBorder

val TerminalPrimary: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.terminalPrimary

val TerminalText: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.terminalText

val TerminalPrompt: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.terminalPrompt

val TerminalOnAccent: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.terminalOnAccent

val TerminalWarn: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.terminalWarning

val TerminalError: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.terminalError

val TerminalInfo: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.terminalInfo

val TerminalMuted: Color
    @androidx.compose.runtime.Composable get() = LocalPianobarPalette.current.terminalMuted
