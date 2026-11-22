# Trust, Privacy, and Security Audit

This document explains the controls and CI checks used to keep **Pianobar for Android** minimal, transparent, and privacy-respecting. It is intended for owners, reviewers, and contributors who need confidence that the app is not nefarious.

## Trust goals

- **No tracking or ads**: no analytics, crash-reporting, ad SDKs, or third-party trackers.
- **Minimal permissions**: only permissions required for Pandora streaming, media playback, and notifications.
- **Minimal attack surface**: only expected Android components are exported; release builds are not debuggable.
- **Low overhead**: small dependency set, no background telemetry, R8 minification/resource shrinking for release builds.
- **Transparent AI Studio packaging**: the project includes only the minimal Google build integration required for Google AI Studio export/signing workflows. It does **not** include Firebase/Analytics/Ads SDKs.

## Current baseline (review when changed)

### Android permissions

Declared in `app/src/main/AndroidManifest.xml`:

| Permission | Why needed | Expected |
|---|---|---|
| `INTERNET` | Pandora API and audio stream requests | Yes |
| `ACCESS_NETWORK_STATE` | Network availability checks/error handling | Yes |
| `WAKE_LOCK` | Keep playback active during streaming | Yes |
| `FOREGROUND_SERVICE` | Foreground playback service | Yes |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Media playback foreground service type (Android 14+) | Yes |
| `POST_NOTIFICATIONS` | Media notification controls on Android 13+ | Yes |
| `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Android 13+/AGP-injected signature permission for dynamic receiver export hardening; app-scoped, not a runtime user permission | Yes, when app-scoped |

Any new permission should be treated as a trust review event. CI fails if the merged APK permissions differ from this allowlist.

### Exported components

| Component | Exported | Why |
|---|---:|---|
| `MainActivity` | Yes | Launcher activity (`MAIN`/`LAUNCHER`) |
| `PianobarWidgetProvider` | Yes | Receives `APPWIDGET_UPDATE` broadcasts |
| `androidx.profileinstaller.ProfileInstallReceiver` | Yes | Jetpack ProfileInstaller receiver merged by dependencies; exported with `android.permission.DUMP`, not app-public functionality |
| `PianobarPlaybackService` | No | Internal foreground media service |

CI inspects the merged manifest and fails on unexpected exported components. For merged dependency components, allow only explicitly reviewed components with documented justification.

### Manifest flags

| Flag | Current | Notes |
|---|---|---|
| `android:debuggable` (release APK) | Should be `false` | CI checks release APK is not debuggable. |
| `android:usesCleartextTraffic` | `true` | Currently enabled in manifest. Review whether required for stream endpoints; if removed, tighten the audit. CI currently reports this as a warning. |
| `android:allowBackup` | `true` | Review backup rules to ensure no sensitive data is exposed unexpectedly. |

## CI audit checks (easy to hard)

The checks below are implemented or documented in order of easiest to hardest.

### 1. Android Lint (easy, implemented)

**What it catches:** common manifest/permission/code issues, including unused permissions and obvious Android API misuse.

**Command:**

```bash
./gradlew :app:lintRelease
```

**CI workflow:** `.github/workflows/android-ci.yml` runs lint and uploads the lint report artifact.

**Limitations:** Lint is not a malware/tracker scanner. It is a hygiene check.

### 2. APK permission/component allowlist (easy, implemented)

**What it catches:** unexpected permissions or exported components added by code or merged dependencies.

**Script:** `scripts/audit-apk.sh`

**Command:**

```bash
./gradlew :app:assembleRelease
scripts/audit-apk.sh app/build/outputs/apk/release/app-release.apk
```

**Checks performed:**

- Exact permission allowlist matches the expected app permissions, including the app-scoped `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` injected by modern Android/AGP.
- Release APK is not debuggable.
- Exported components are checked against an allowlist; expected app components are `MainActivity` and `PianobarWidgetProvider`. Reviewed dependency-merged components (currently `androidx.profileinstaller.ProfileInstallReceiver`) are allowed only when explicitly documented.
- Reports `usesCleartextTraffic` and backup flags for review.

**Limitations:** This is a policy/allowlist gate, not behavioral analysis. It catches manifest drift but cannot prove runtime behavior.

### 3. Exodus Privacy tracker scan (moderate, implemented)

**What it catches:** known tracker SDK signatures in the APK.

**Script:** `scripts/exodus-audit.sh`

**Install (local, either option):**

```bash
# Option A: Docker (recommended; matches CI)
docker run --rm exodusprivacy/exodus-standalone --help

# Option B: local CLI, if available in your environment
# (package availability varies; see Exodus-Privacy/exodus-standalone)
```

**Command:**

```bash
./gradlew :app:assembleRelease
scripts/exodus-audit.sh app/build/outputs/apk/release/app-release.apk exodus-report.json
```

**CI workflow:** runs `exodusprivacy/exodus-standalone` via Docker, writes `exodus-report.json`, and fails if known trackers are detected.

**Limitations:** Signature-based. It can miss unknown/custom trackers and cannot prove absence of all tracking. It should be combined with dependency review and manifest allowlists.

### 4. MobSF (harder, future TODO)

**What it would catch:** deeper static-analysis findings: manifest/security misconfigurations, SDK/API usage signals, certificate/debug issues, and broader code smells.

**Why not implemented now:** MobSF is heavier to run and maintain in CI (Docker/service setup, runtime, report triage, false-positive management). It is valuable as a periodic deep audit, but the easy checks above provide strong baseline confidence with low maintenance.

**Future TODO:**

- Add a manual or scheduled MobSF workflow (Docker-based) for release candidates.
- Store MobSF reports as CI artifacts.
- Define a severity threshold for failing builds (e.g., fail on high-severity confirmed findings).

## Dependency and build review checklist

When reviewing changes, especially dependency bumps or build config changes:

- [ ] No new analytics, ads, crash reporting, or tracking SDKs added.
- [ ] No new permissions added without updating this document and the allowlist script.
- [ ] No new exported components/services/providers/receivers without justification.
- [ ] Release build remains minified/shrunk (`isMinifyEnabled=true`, `isShrinkResources=true`).
- [ ] `google-services` remains optional/pass-through unless a real Google service is intentionally added and documented.
- [ ] Any new network endpoint or cleartext requirement is documented.
- [ ] Sensitive data is not written to logs/backups.

## Local audit quickstart

```bash
# 1) Lint
./gradlew :app:lintRelease

# 2) Build release APK
./gradlew :app:assembleRelease

# 3) Permission/component audit
scripts/audit-apk.sh app/build/outputs/apk/release/app-release.apk

# 4) Tracker scan
python3 -m pip install --upgrade exodus-standalone
scripts/exodus-audit.sh app/build/outputs/apk/release/app-release.apk exodus-report.json
```

## Interpreting results

- **PASS** means the check found no policy violations in its scope.
- **PASS does not mean perfect security.** It means the app passed the specific automated guardrails.
- Any allowlist change should be intentional, documented in this file, and explained in the PR/commit message.

## Remaining lint / audit TODOs

From `lint-results-release` (0 errors, 57 warnings), remaining items not cleaned up in the easy pass:

### Safe-ish follow-ups
- **`UseKtx`**: Replace eligible platform calls with Android KTX equivalents (`SharedPreferences.edit`, `String.toUri`, bitmap/canvas helpers, `ColorDrawable` setup) where dependencies already support them.
- **`NestedWeights` (widget_pianobar_compact.xml)**: Restructure compact widget layout to avoid nested weights; verify RemoteViews/widget rendering on small widget sizes.
- **`IconLauncherShape`**: If desired, provide a dedicated round launcher asset with a truly circular silhouette. Current adaptive round icon XML is kept; old duplicate round PNGs were removed.
- **Dependency/version warnings**: Evaluate Gradle/AGP/library updates deliberately. Treat **OkHttp 4.12.1 → 5.x** as a major migration (API/behavior changes), not a routine bump.
- **`UseTomlInstead`**: Move `robolectricSdks` dependency coordinate into `gradle/libs.versions.toml` for consistency.

### Intentional / review-required
- **`OldTargetApi` (`targetSdk = 36`)**: Confirm against current Play/Android requirements before bumping; update only after compatibility testing.
- **`usesCleartextTraffic="true"`**: Audit whether all Pandora endpoints used by the app require/benefit from cleartext; if not required, disable and tighten the trust audit.
- **MobSF**: Add a heavier periodic/manual static-analysis audit (Docker-based MobSF workflow, artifact retention, severity threshold) as noted above.

### Process
- When lint warnings are intentionally accepted, prefer narrow suppressions with justification (`@SuppressLint`, `tools:ignore`, or lint config) over broad baseline suppression.
- Re-run `./gradlew :app:lintRelease` and the trust audit scripts after cleanup to confirm warning count decreases without behavior regressions.
