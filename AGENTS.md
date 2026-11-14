# AGENTS.md — Engineering & Collaboration Guidelines

This document outlines key architectural guidelines, critical operational constraints, and hard-earned lessons learned while engineering **Pianobar for Android** with AI coding agents.

---

## 1. Ground Truth & Honesty (CRITICAL)

### Never Lie, Fake Implementations, or Mock When Real Code is Required
- **No Mock APIs / Hardcoded Responses**: In early iterations, AI agents attempted to simulate radio streams with static dummy arrays and fake loading loops. This was unacceptable. Pianobar works because it implements the actual Pandora JSON-RPC partner protocol (partner login, Blowfish 128-bit ECB encryption, user authentication, station retrieval, playlist batching, and audio stream URLs).
- **Be Explicit About Capabilities**: If a feature is failing or a dependency is needed, diagnose the root cause with real compiler logs, tests, or decompilation tools. Never invent false success reports.
- **Fail Fast & Report Truthfully**: When a build fails, state the exact task and stack trace instead of claiming success.

---

## 2. Android Publishing & Packaging Lessons

### Google Play Package Name vs. Namespace
- Google Play strictly enforces unique application IDs.
- Never use `com.example` in `applicationId` or `namespace` if the app is destined for Google Play.
- When refactoring the package name:
  1. Synchronize `namespace = "com.aistudio.pianobar.kxuvzq"` and `defaultConfig.applicationId = "com.aistudio.pianobar.kxuvzq"` in `app/build.gradle.kts`.
  2. Move all source directories under `app/src/main/java/`, `test/`, and `androidTest/` to match the exact package hierarchy.
  3. Update package declarations and import statements across all Kotlin and XML files.
  4. Never leave dangling `com.example` references in manifest intent filters or theme styles.

### Version Code Management
- Play Console tracks every uploaded bundle by integer `versionCode`.
- Once a `versionCode` (e.g. 11, 12, 13) has been committed or uploaded, Google Play permanently disallows reusing it.
- When publishing fails with `Version code X has already been used`:
  - Increment `versionCode = X + 1` in `app/build.gradle.kts`.
  - Set `versionName = "<new versionCode>.0"`.

### Dependency Pruning & APK Optimization
- Do not blindly remove core SDKs (Firebase/Google Services plugins) if the platform's cloud build/export pipeline relies on them during pre-flight checks.
- When optimizing APK size:
  - Remove truly unused large image drawables (such as intermediate AI-generated JPGs in `res/drawable/`).
  - Use proper alpha masking on PNGs instead of saving 1024x1024 uncompressed JPGs with solid corner fills.
  - Enable R8 tree-shaking (`isMinifyEnabled = true`, `isShrinkResources = true`) only for release configurations when appropriate ProGuard rules are configured.

---

## 3. UI, Assets & Aesthetics

### Avoid "AI Slop"
- Do not clutter clean hacker/terminal applications with artificial purple/cyan gradients, floating sparkles (`AutoAwesome`), or over-decorated bubbly cards.
- Respect the original spirit of the CLI tool: high contrast, authentic terminal prompt icons (`[i]`, `[#]`, `[+]`, `[-]`, `[s]`, `[z]`), clear typography, and responsive controls.

### Splash Screen & Launcher Icons
- Android 12+ generates a system splash screen from the app icon (`windowSplashScreenBackground` and adaptive icon layers).
- **The Foreground Asset MUST Have Transparent Corners**: If a square/squircle graphic has white or solid pixels in its outer corners (e.g., from an exported JPG), Android will render that white fill inside the circular splash mask, resulting in an ugly "black logo inside a white box on a black background".
- Always use transparent PNGs for `ic_launcher_foreground` and mipmaps with proper alpha transparency around the logo contours.
- Configure `android:windowSplashScreenBackground` and `android:windowSplashScreenIconBackgroundColor` to `@android:color/transparent` (or match the dark background `#0D1117`) in `themes.xml`.

### Lockscreen & Media Notification Controls (DO NOT REVERT)
- **No Love (`+`) or Ban (`-`) on Lockscreen/Notification**: Never add Thumbs Up / Love or Thumbs Down / Ban buttons, `MediaSession` custom actions (`addCustomAction`), or `onSetRating` handlers to `PianobarPlaybackService` or the lockscreen media player.
- Keep lockscreen and notification controls strictly limited to **Play/Pause**, **Skip Next**, and **Close/Stop** (with `setShowActionsInCompactView(0, 1)`).
- Song feedback (`love`/`ban`) belongs exclusively inside the in-app UI and CLI terminal.
- Never silently restore previously removed UI/service controls while fixing an unrelated bug (such as fixing missing lockscreen album artwork).

---

## 4. Architecture & State Management

- **Single Player Manager**: `PianobarPlayerManager` operates as a singleton orchestrating playback state, station queues, sleep timers, and CLI command execution.
- **Service Decoupling**: `PianobarPlaybackService` runs in the foreground to keep audio alive when the app is backgrounded, receiving commands via explicit intents and reporting state via StateFlow.
- **Persistent Preferences**: Always persist essential user preferences (such as `lastStationId` and audio bitrate) to `PianobarPreferences` backed by hardware-backed Android KeyStore encryption (`SecureEnclaveKeyStore`).
