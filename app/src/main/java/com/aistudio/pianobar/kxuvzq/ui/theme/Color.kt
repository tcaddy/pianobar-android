package com.aistudio.pianobar.kxuvzq.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * A complete terminal palette. Names are semantic roles, so components never
 * decide which raw hex value a theme means.
 */
data class PianobarPalette(
    val id: String,
    val name: String,
    val caption: String,
    val isDark: Boolean,

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

/** GitHub-dark terminal palette: the app's original identity. */
val MidnightPalette = PianobarPalette(
    id = "midnight",
    name = "Midnight",
    caption = "GitHub dark · phosphor green",
    isDark = true,
    background = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    surfaceElevated = Color(0xFF21262D),
    border = Color(0xFF30363D),
    accent = Color(0xFF58A6FF),
    accentAlt = Color(0xFFBC8CFF),
    info = Color(0xFF388BFD),
    success = Color(0xFF3FB950),
    warning = Color(0xFFD29922),
    danger = Color(0xFFF85149),
    onAccent = Color(0xFF000000),
    onAccentAlt = Color(0xFF0D1117),
    onSuccess = Color(0xFF000000),
    textPrimary = Color(0xFFF0F6FC),
    textSecondary = Color(0xFF8B949E),
    textTertiary = Color(0xFF484F58),
    terminalBackground = Color(0xFF090D13),
    terminalHeader = Color(0xFF030508),
    terminalSurface = Color(0xFF080C14),
    terminalBorder = Color(0xFF1C2538),
    terminalPrimary = Color(0xFFF0F6FC),
    terminalText = Color(0xFF3FB950),
    terminalPrompt = Color(0xFF58A6FF),
    terminalOnAccent = Color(0xFF090D13),
    terminalWarning = Color(0xFFD29922),
    terminalError = Color(0xFFF85149),
    terminalInfo = Color(0xFF8899A6),
    terminalMuted = Color(0xFF718093),
    inputBackground = Color(0xFF1E293B),
    mutedSurface = Color(0xFF1E293B),
    disabledContent = Color(0xFF64748B),
    dragHandle = Color(0xFF334155)
)

/** Warm monochrome CRT: amber phosphor on deep brown-black. */
val AmberCrtPalette = PianobarPalette(
    id = "amber-crt",
    name = "Amber CRT",
    caption = "Warm phosphor · monochrome warmth",
    isDark = true,
    background = Color(0xFF120E08),
    surface = Color(0xFF1C150B),
    surfaceElevated = Color(0xFF2A2012),
    border = Color(0xFF42341A),
    accent = Color(0xFFFFB000),
    accentAlt = Color(0xFFFF8A3D),
    info = Color(0xFFC7A876),
    success = Color(0xFFFFD166),
    warning = Color(0xFFFF8C42),
    danger = Color(0xFFFF5E5B),
    onAccent = Color(0xFF1A1204),
    onAccentAlt = Color(0xFF1A1204),
    onSuccess = Color(0xFF1A1204),
    textPrimary = Color(0xFFFFE7B2),
    textSecondary = Color(0xFFC7A876),
    textTertiary = Color(0xFFA18352),
    terminalBackground = Color(0xFF0C0905),
    terminalHeader = Color(0xFF070502),
    terminalSurface = Color(0xFF141007),
    terminalBorder = Color(0xFF3A2C14),
    terminalPrimary = Color(0xFFFFE7B2),
    terminalText = Color(0xFFFFB000),
    terminalPrompt = Color(0xFFFFD166),
    terminalOnAccent = Color(0xFF0C0905),
    terminalWarning = Color(0xFFFF8C42),
    terminalError = Color(0xFFFF5E5B),
    terminalInfo = Color(0xFFC7A876),
    terminalMuted = Color(0xFF9A7C50),
    inputBackground = Color(0xFF2A2012),
    mutedSurface = Color(0xFF2A2012),
    disabledContent = Color(0xFF9A7C50),
    dragHandle = Color(0xFF6B5228)
)

/** Cold blue-slate: a quiet, high-contrast winter terminal. */
val IcePalette = PianobarPalette(
    id = "ice",
    name = "Ice",
    caption = "Blue slate · cool precision",
    isDark = true,
    background = Color(0xFF0B1220),
    surface = Color(0xFF111B2E),
    surfaceElevated = Color(0xFF18263D),
    border = Color(0xFF2B3E5F),
    accent = Color(0xFF7DD3FC),
    accentAlt = Color(0xFFA5B4FC),
    info = Color(0xFF60A5FA),
    success = Color(0xFF5EEAD4),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFFB7185),
    onAccent = Color(0xFF071320),
    onAccentAlt = Color(0xFF0B1220),
    onSuccess = Color(0xFF071320),
    textPrimary = Color(0xFFE6F1FF),
    textSecondary = Color(0xFF9FB3CC),
    textTertiary = Color(0xFF6B8299),
    terminalBackground = Color(0xFF070D18),
    terminalHeader = Color(0xFF040810),
    terminalSurface = Color(0xFF0C1626),
    terminalBorder = Color(0xFF243955),
    terminalPrimary = Color(0xFFE6F1FF),
    terminalText = Color(0xFF67E8F9),
    terminalPrompt = Color(0xFFA5B4FC),
    terminalOnAccent = Color(0xFF070D18),
    terminalWarning = Color(0xFFFBBF24),
    terminalError = Color(0xFFFB7185),
    terminalInfo = Color(0xFF9FB3CC),
    terminalMuted = Color(0xFF6B8299),
    inputBackground = Color(0xFF18263D),
    mutedSurface = Color(0xFF18263D),
    disabledContent = Color(0xFF9FB3CC),
    dragHandle = Color(0xFF3D587D)
)

/** Solarized dark, brightened where the classic values failed AA on surfaces. */
val SolarizedPalette = PianobarPalette(
    id = "solarized",
    name = "Solarized",
    caption = "Classic developer dark · teal anchor",
    isDark = true,
    background = Color(0xFF002B36),
    surface = Color(0xFF073642),
    surfaceElevated = Color(0xFF0A4050),
    border = Color(0xFF1A5260),
    accent = Color(0xFF3BBFB4),
    accentAlt = Color(0xFF9BA2EE),
    info = Color(0xFF6CB6E8),
    success = Color(0xFF9CB300),
    warning = Color(0xFFD9A400),
    danger = Color(0xFFFF857C),
    onAccent = Color(0xFF002B36),
    onAccentAlt = Color(0xFF002B36),
    onSuccess = Color(0xFF002B36),
    textPrimary = Color(0xFFEEE8D5),
    textSecondary = Color(0xFF9BA9A9),
    textTertiary = Color(0xFF8BA1A9),
    terminalBackground = Color(0xFF00212A),
    terminalHeader = Color(0xFF001A22),
    terminalSurface = Color(0xFF052C38),
    terminalBorder = Color(0xFF1A5260),
    terminalPrimary = Color(0xFFEEE8D5),
    terminalText = Color(0xFF9CB300),
    terminalPrompt = Color(0xFF3BBFB4),
    terminalOnAccent = Color(0xFF00212A),
    terminalWarning = Color(0xFFD9A400),
    terminalError = Color(0xFFFF857C),
    terminalInfo = Color(0xFF9BA9A9),
    terminalMuted = Color(0xFF8BA1A9),
    inputBackground = Color(0xFF0A4050),
    mutedSurface = Color(0xFF0A4050),
    disabledContent = Color(0xFF9BA9A9),
    dragHandle = Color(0xFF3A6570)
)

/** High-contrast paper UI with a dark terminal inset. */
val NewsprintPalette = PianobarPalette(
    id = "newsprint",
    name = "Newsprint",
    caption = "Warm paper · ink and dark CRT",
    isDark = false,
    background = Color(0xFFF4F1EA),
    surface = Color(0xFFFFFDF8),
    surfaceElevated = Color(0xFFEAE5D9),
    border = Color(0xFFCFC7B6),
    accent = Color(0xFF1D5FA8),
    accentAlt = Color(0xFF7A3E9D),
    info = Color(0xFF1D5FA8),
    success = Color(0xFF1E7145),
    warning = Color(0xFF8A5A00),
    danger = Color(0xFFB3261E),
    onAccent = Color(0xFFFFFFFF),
    onAccentAlt = Color(0xFFFFFFFF),
    onSuccess = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF1C1B18),
    textSecondary = Color(0xFF5A5448),
    textTertiary = Color(0xFF827B6C),
    terminalBackground = Color(0xFF1C1B18),
    terminalHeader = Color(0xFF121110),
    terminalSurface = Color(0xFF26251F),
    terminalBorder = Color(0xFF4A463C),
    terminalPrimary = Color(0xFFF4F1EA),
    terminalText = Color(0xFFA7C957),
    terminalPrompt = Color(0xFF7FC8F8),
    terminalOnAccent = Color(0xFF1C1B18),
    terminalWarning = Color(0xFFF0B429),
    terminalError = Color(0xFFF87171),
    terminalInfo = Color(0xFFC8D3E0),
    terminalMuted = Color(0xFF9A9382),
    inputBackground = Color(0xFFFFFFFF),
    mutedSurface = Color(0xFFEAE5D9),
    disabledContent = Color(0xFF827B6C),
    dragHandle = Color(0xFFB8B0A0)
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
    @Composable get() = LocalPianobarPalette.current.background

val PianoSurface: Color
    @Composable get() = LocalPianobarPalette.current.surface

val PianoSurfaceElevated: Color
    @Composable get() = LocalPianobarPalette.current.surfaceElevated

val PianoSurfaceBorder: Color
    @Composable get() = LocalPianobarPalette.current.border

val PianoCyan: Color
    @Composable get() = LocalPianobarPalette.current.accent

val PianoBlue: Color
    @Composable get() = LocalPianobarPalette.current.info

val PianoPurple: Color
    @Composable get() = LocalPianobarPalette.current.accentAlt

val PianoGreen: Color
    @Composable get() = LocalPianobarPalette.current.success

val PianoAmber: Color
    @Composable get() = LocalPianobarPalette.current.warning

val PianoRed: Color
    @Composable get() = LocalPianobarPalette.current.danger

val PianoOnAccent: Color
    @Composable get() = LocalPianobarPalette.current.onAccent

val PianoOnAccentAlt: Color
    @Composable get() = LocalPianobarPalette.current.onAccentAlt

val PianoOnSuccess: Color
    @Composable get() = LocalPianobarPalette.current.onSuccess

val PianoInputBackground: Color
    @Composable get() = LocalPianobarPalette.current.inputBackground

val PianoMutedSurface: Color
    @Composable get() = LocalPianobarPalette.current.mutedSurface

val PianoDisabledContent: Color
    @Composable get() = LocalPianobarPalette.current.disabledContent

val PianoDragHandle: Color
    @Composable get() = LocalPianobarPalette.current.dragHandle

val TextPrimary: Color
    @Composable get() = LocalPianobarPalette.current.textPrimary

val TextSecondary: Color
    @Composable get() = LocalPianobarPalette.current.textSecondary

val TextTertiary: Color
    @Composable get() = LocalPianobarPalette.current.textTertiary

val TerminalBg: Color
    @Composable get() = LocalPianobarPalette.current.terminalBackground

val TerminalHeader: Color
    @Composable get() = LocalPianobarPalette.current.terminalHeader

val TerminalSurface: Color
    @Composable get() = LocalPianobarPalette.current.terminalSurface

val TerminalBorderColor: Color
    @Composable get() = LocalPianobarPalette.current.terminalBorder

val TerminalPrimary: Color
    @Composable get() = LocalPianobarPalette.current.terminalPrimary

val TerminalText: Color
    @Composable get() = LocalPianobarPalette.current.terminalText

val TerminalPrompt: Color
    @Composable get() = LocalPianobarPalette.current.terminalPrompt

val TerminalOnAccent: Color
    @Composable get() = LocalPianobarPalette.current.terminalOnAccent

val TerminalWarn: Color
    @Composable get() = LocalPianobarPalette.current.terminalWarning

val TerminalError: Color
    @Composable get() = LocalPianobarPalette.current.terminalError

val TerminalInfo: Color
    @Composable get() = LocalPianobarPalette.current.terminalInfo

val TerminalMuted: Color
    @Composable get() = LocalPianobarPalette.current.terminalMuted
