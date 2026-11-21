# Design system: Pianobar for Android

The product is a terminal-first Pandora client. Material 3 supplies structure and
interaction; the visual identity lives in a small set of semantic color roles.
Every surface is a dark or paper "terminal", never a marketing page.

## Non-negotiable constraints

- Keep the CLI vocabulary: authentic prompt glyphs (`[i]`, `[#]`, `[+]`, `[-]`,
  `[?]`, `[>]`, `[z]`), monospace logs, high contrast, restrained controls.
- No gradients, floating sparkles, or decorative icon tiles. Color earns its
  place through hierarchy, state, and selection only.
- Lockscreen and notification controls stay limited to Play/Pause, Skip, and
  Close. Love/Ban remain in-app.
- Launcher foreground assets must have transparent corners; the system splash
  stays the brand dark `#0D1117`.

## Palette roles

`PianobarPalette` (`app/.../ui/theme/Color.kt`) defines all roles. Components
read them through composable accessors (`PianoCyan`, `TextPrimary`,
`TerminalText`, ...), which resolve through `LocalPianobarPalette`. Never use a
raw hex value in a component.

Role groups:

- Surfaces: `background`, `surface`, `surfaceElevated`, `border`
- Actions and meaning: `accent`, `accentAlt`, `info`, `success`, `warning`,
  `danger`, plus matching `onAccent`, `onAccentAlt`, `onSuccess`
- Text: `textPrimary`, `textSecondary`, `textTertiary`
- Terminal inset: `terminalBackground`, `terminalHeader`, `terminalSurface`,
  `terminalBorder`, `terminalPrimary`, `terminalText`, `terminalPrompt`,
  `terminalOnAccent`, `terminalWarning`, `terminalError`, `terminalInfo`,
  `terminalMuted`
- Controls: `inputBackground`, `mutedSurface`, `disabledContent`, `dragHandle`

## Generated themes

| Id | Name | World | Ground | Accent |
| --- | --- | --- | --- | --- |
| `midnight` | Midnight | Original GitHub-dark terminal | `#0D1117` | `#58A6FF` |
| `amber-crt` | Amber CRT | Warm monochrome phosphor monitor | `#120E08` | `#FFB000` |
| `ice` | Ice | Cold blue-slate precision terminal | `#0B1220` | `#7DD3FC` |
| `solarized` | Solarized | Classic developer dark, brightened for AA | `#002B36` | `#3BBFB4` |
| `newsprint` | Newsprint | Warm paper UI with a dark CRT inset | `#F4F1EA` | `#1D5FA8` |
| `ai-slop` | AI Slop | Opt-in parody: violet/cyan glow on slate-950 | `#020617` | `#C084FC` |

`ai-slop` is a deliberate, self-contained joke: it pairs the purple-to-cyan,
emerald-status, pink-error color language of LLM-generated SaaS pages with
Slopstrap-inspired ambient glowing orbs, tech grid lines, a pulsing dual-ring
status pill, gradient borders/text/FAB, and a KPI bento strip—all strictly
gated behind `isAiSlopTheme` so the five core palettes stay clean and flat. It
is never the default.

`midnight` is the default and the fallback for unknown stored ids. The selected
id is persisted in `PianobarPreferences` and exposed through `PlayerState`, so
the settings sheet applies a palette instantly and the next cold start keeps it.

## Contrast floor

All body and terminal text pairs, accent text, and semantic status colors are
checked at WCAG AA (4.5:1 minimum) against every surface they appear on. The
light theme keeps its terminal inset dark; terminal text is drawn from the
`terminal*` roles, never from light-theme `textPrimary`.
