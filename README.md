# Pianobar for Android

An authentic Android client inspired by the open-source console client [pianobar](https://6xq.net/pianobar/) by Lars-Dominik Braun and contributors ([Codeberg: purplesym/pianobar](https://codeberg.org/purplesym/pianobar), formerly [GitHub: PromyLOPh/pianobar](https://github.com/PromyLOPh/pianobar)).

Pianobar brings the minimalist, hacker-friendly Pandora radio experience to modern Android with Jetpack Compose, Material Design 3, real background media streaming, and an integrated retro CLI mode.

It is designed as a **minimal, privacy-respecting personal app**: no ads, no analytics, no trackers, and no unnecessary background services. It uses only the minimal Google build integration required for packaging/export in Google AI Studio, so the owner can build and install it as a personal app without adding telemetry or third-party tracking SDKs. Release builds use R8 shrinking/minification to keep the download small and runtime overhead low.

---

## Features

- **Direct Pandora Tuner Integration**:
  - Implements the Pandora JSON-RPC partner protocol (Android handshake, Blowfish 128-bit ECB encryption for partner payloads, and TLS session tokens).
  - High-bitrate audio streaming (AAC 192 kbps / 64 kbps).
  - Personalized station sync, track feedback (love `+`, ban `-`, tired `t`, bookmark `b`), and Music Genome explanations (`e`).
- **Pianobar CLI Terminal Mode**:
  - Full ASCII terminal interface matching original pianobar console prompts (`[i]`, `[#]`, `[+]`, `[-]`, `[?]`, `[>]`, `[z]`).
  - Single-character command shortcuts (`p` play/pause, `n` skip, `+` love, `-` ban, `t` snooze, `b` bookmark, `e` explain, `s` stations, `h` history, `sleep` sleep timer, `q` quit).
- **Modern Jetpack Compose GUI**:
  - Terminal aesthetic with six selectable palettes: Midnight, Amber CRT, Ice, Solarized, Newsprint (light), and AI Slop (an intentionally loud violet/cyan parody theme inspired by [SlopStrap](https://slopstrap.mikelacher.com)). Choose one under Settings → Appearance; the choice is remembered across launches.
  - Real-time animated audio visualizer bars responding to playback state.
  - Searchable, alphabetized station browser.
  - Last station memory: automatically restores your last played station on startup.
- **Sleep Timer**:
  - Built-in timer controls in Settings (presets: 15m, 30m, 45m, 60m) and via CLI (`sleep` or `sleep <minutes>`).
  - Automatically stops playback when the timer expires.
- **Background Media Playback**:
  - Foreground media service (`mediaPlayback`) with Android `MediaSessionCompat` integration.
  - Lockscreen & notification drawer media controls (Play/Pause, Skip).
  - Audio focus management for phone calls and headphone disconnects.
- **Secure Enclave Credential Storage**:
  - Hardware-isolated encryption using Android KeyStore (AES-256 GCM) for secure credential persistence on device.

---

## Attribution & Links

- **Original Pianobar**: [https://6xq.net/pianobar/](https://6xq.net/pianobar/)
- **Original Source Repository**: [https://codeberg.org/purplesym/pianobar](https://codeberg.org/purplesym/pianobar) (moved from [GitHub: PromyLOPh/pianobar](https://github.com/PromyLOPh/pianobar))
- **Author**: Lars-Dominik Braun (PromyLOPh) and the Pianobar open-source community.
- **License**: The original pianobar C implementation is licensed under the MIT/X11 license.

---

## Minimal by Design / AI Studio Personal App

- **No tracking or ads**: No analytics, crash-reporting SDKs, ad SDKs, or third-party trackers.
- **Minimal permissions**: Only network/audio/foreground-service/notification permissions needed for Pandora streaming and media controls.
- **Low overhead**: Lightweight Compose UI, no background telemetry, and R8 minification/resource shrinking enabled for release builds to keep download/runtime footprint small.
- **AI Studio packaging**: Includes only the minimal Google build/plugin integration required for Google AI Studio export/signing workflows, so the owner can package and install it as a personal app.

## Tech Stack & Architecture

- **Language**: Kotlin 2.2+
- **UI Toolkit**: Jetpack Compose with Material 3 (M3)
- **Architecture**: MVVM with Kotlin Coroutines & StateFlow
- **Networking**: OkHttp
- **Media Engine**: Android `MediaPlayer` wrapped in foreground `MediaSessionService`
- **Security**: Android KeyStore Hardware TEE/StrongBox (AES-GCM-256)
- **Target SDK**: Android 16 (API 36)
- **Min SDK**: Android 7.0 (API 24)

---

## Building from Source

```bash
# Clone the repository
git clone https://github.com/your-username/pianobar-android.git
cd pianobar-android

# Run unit tests
gradle :app:testDebugUnitTest

# Assemble debug APK
gradle :app:assembleDebug

# Assemble release APK
gradle :app:assembleRelease
```
