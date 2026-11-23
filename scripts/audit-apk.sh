#!/usr/bin/env bash
# Audit a built Android APK for trust/privacy regressions.
# Checks:
# 1. Permission allowlist (merged manifest)
# 2. Exported component allowlist
# 3. Debuggable/cleartext/backup flags
#
# Usage: scripts/audit-apk.sh <apk-path>
set -euo pipefail

APK="${1:?Usage: scripts/audit-apk.sh <apk-path>}"
if [[ ! -f "$APK" ]]; then
  echo "ERROR: APK not found: $APK" >&2
  exit 1
fi

find_tool() {
  local name="$1"
  if command -v "$name" >/dev/null 2>&1; then
    command -v "$name"
    return 0
  fi
  local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [[ -n "$sdk" ]]; then
    local found
    found=$(find "$sdk" -type f -name "$name" 2>/dev/null | sort | tail -n 1 || true)
    if [[ -n "$found" ]]; then
      echo "$found"
      return 0
    fi
  fi
  return 1
}

APKANALYZER="$(find_tool apkanalyzer || true)"
AAPT2="$(find_tool aapt2 || true)"
AAPT="$(find_tool aapt || true)"

echo "== APK Audit =="
echo "APK: $APK"
echo "apkanalyzer: ${APKANALYZER:-not found}"
echo "aapt2: ${AAPT2:-not found}"
echo "aapt: ${AAPT:-not found}"
echo

fail=0
warn=0
fail_msg() {
  echo "FAIL: $*" >&2
  fail=1
}
pass_msg() {
  echo "PASS: $*"
}
warn_msg() {
  echo "WARN: $*"
  warn=1
}

# ---- App ID / package ----
APP_ID=""
if [[ -n "$APKANALYZER" ]]; then
  APP_ID=$("$APKANALYZER" manifest application-id "$APK" 2>/dev/null | tr -d '\r' | head -n 1 || true)
fi
if [[ -z "$APP_ID" && -n "$AAPT2" ]]; then
  APP_ID=$("$AAPT2" dump badging "$APK" 2>/dev/null | sed -n "s/^package: name='\([^']*\)'.*/\1/p" | head -n 1 || true)
fi
if [[ -z "$APP_ID" && -n "$AAPT" ]]; then
  APP_ID=$("$AAPT" dump badging "$APK" 2>/dev/null | sed -n "s/^package: name='\([^']*\)'.*/\1/p" | head -n 1 || true)
fi
echo "Application ID: ${APP_ID:-unknown}"
echo

# ---- Permissions allowlist ----
EXPECTED_PERMISSIONS=(
  android.permission.ACCESS_NETWORK_STATE
  android.permission.FOREGROUND_SERVICE
  android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK
  android.permission.INTERNET
  android.permission.POST_NOTIFICATIONS
  android.permission.WAKE_LOCK
)
# Android 13+ dynamic receiver hardening: AGP injects an app-scoped signature
# permission. Expected and safe when it matches the app ID.
if [[ -n "$APP_ID" ]]; then
  EXPECTED_PERMISSIONS+=("${APP_ID}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
fi

get_permissions() {
  if [[ -n "$APKANALYZER" ]]; then
    "$APKANALYZER" manifest permissions "$APK" | sed '/^$/d' | sort -u
  elif [[ -n "$AAPT2" ]]; then
    "$AAPT2" dump permissions "$APK" | sed -n "s/^uses-permission: name='\([^']*\)'.*/\1/p" | sort -u
  elif [[ -n "$AAPT" ]]; then
    "$AAPT" dump permissions "$APK" | sed -n "s/^uses-permission: name='\([^']*\)'.*/\1/p" | sort -u
  else
    echo "ERROR: No apkanalyzer/aapt2/aapt found; cannot inspect APK." >&2
    exit 2
  fi
}

echo "-- Permission allowlist --"
actual_perms=$(get_permissions)
expected_perms=$(printf '%s\n' "${EXPECTED_PERMISSIONS[@]}" | sort -u)

if [[ "$actual_perms" == "$expected_perms" ]]; then
  pass_msg "Permissions match allowlist"
else
  unexpected=$(comm -13 <(printf '%s\n' $expected_perms) <(printf '%s\n' $actual_perms) || true)
  missing=$(comm -23 <(printf '%s\n' $expected_perms) <(printf '%s\n' $actual_perms) || true)
  if [[ -n "$unexpected" ]]; then
    fail_msg "Unexpected permissions detected"
  fi
  if [[ -n "$missing" ]]; then
    fail_msg "Expected permissions missing"
  fi
  echo "  Expected:"
  printf '    %s\n' $expected_perms
  echo "  Actual:"
  if [[ -n "$actual_perms" ]]; then
    printf '    %s\n' $actual_perms
  else
    echo "    <none>"
  fi
  if [[ -n "$unexpected" ]]; then
    echo "  Unexpected:"
    printf '%s\n' "$unexpected" | sed 's/^/    /'
  fi
  if [[ -n "$missing" ]]; then
    echo "  Missing:"
    printf '%s\n' "$missing" | sed 's/^/    /'
  fi
fi
echo

# ---- Manifest checks ----
get_manifest() {
  if [[ -n "$APKANALYZER" ]]; then
    "$APKANALYZER" manifest print "$APK"
  elif [[ -n "$AAPT2" ]]; then
    "$AAPT2" dump xmltree --file AndroidManifest.xml "$APK"
  elif [[ -n "$AAPT" ]]; then
    "$AAPT" dump xmltree "$APK" AndroidManifest.xml
  else
    return 2
  fi
}

manifest=$(get_manifest)

echo "-- Manifest checks --"

# Debuggable
if printf '%s\n' "$manifest" | grep -Eq 'android:debuggable(="true"|[^=]*true)'; then
  fail_msg "APK is debuggable"
else
  pass_msg "APK is not debuggable"
fi

# Exported component allowlist.
# Allowed app components (exact names or suffixes resolved against app ID).
ALLOWED_EXPORTED=(
  "activity:${APP_ID}.MainActivity"
  "receiver:${APP_ID}.widget.PianobarWidgetProvider"
  # AndroidX ProfileInstaller; may be merged by AGP/Jetpack. Exported with DUMP permission.
  "receiver:androidx.profileinstaller.ProfileInstallReceiver"
)

is_allowed_exported() {
  local tag="$1" name="$2"
  local allowed
  for allowed in "${ALLOWED_EXPORTED[@]}"; do
    if [[ "$allowed" == "${tag}:${name}" ]]; then
      return 0
    fi
  done
  return 1
}

exported_components=""
if command -v python3 >/dev/null 2>&1; then
  exported_components=$(python3 - "$manifest" <<'PY'
import sys, xml.etree.ElementTree as ET
manifest = sys.argv[1]
try:
    root = ET.fromstring(manifest)
except Exception:
    sys.exit(3)
ns_android = 'http://schemas.android.com/apk/res/android'
pkg = root.attrib.get('package', '')
def full_name(name):
    if not name:
        return name
    if name.startswith('.'):
        return pkg + name
    if '.' not in name:
        return pkg + '.' + name
    return name
for tag in ['activity', 'activity-alias', 'service', 'receiver', 'provider']:
    for el in root.iter(tag):
        exported = el.attrib.get(f'{{{ns_android}}}exported')
        if exported == 'true':
            name = full_name(el.attrib.get(f'{{{ns_android}}}name', ''))
            perm = el.attrib.get(f'{{{ns_android}}}permission', '')
            print(f'{tag}\t{name}\t{perm}')
PY
) || exported_components=""
fi

if [[ -n "$exported_components" ]]; then
  echo "  Exported components (merged manifest):"
  while IFS=$'\t' read -r tag name perm; do
    [[ -z "$tag" ]] && continue
    echo "    - $tag $name${perm:+ (permission: $perm)}"
    if is_allowed_exported "$tag" "$name"; then
      :
    else
      fail_msg "Unexpected exported component: $tag $name"
    fi
  done <<< "$exported_components"
  # Ensure expected app components are present
  if printf '%s\n' "$exported_components" | grep -q "activity	${APP_ID}.MainActivity"; then
    pass_msg "Expected exported launcher activity present"
  else
    fail_msg "Expected exported launcher activity missing: ${APP_ID}.MainActivity"
  fi
  if printf '%s\n' "$exported_components" | grep -q "receiver	${APP_ID}.widget.PianobarWidgetProvider"; then
    pass_msg "Expected exported widget receiver present"
  else
    fail_msg "Expected exported widget receiver missing: ${APP_ID}.widget.PianobarWidgetProvider"
  fi
else
  warn_msg "Could not parse exported components via XML; falling back to heuristic"
  exported_lines=$(printf '%s\n' "$manifest" | grep -n 'android:exported="true"\|exported(.*0xffffffff' || true)
  if [[ -n "$exported_lines" ]]; then
    printf '%s\n' "$exported_lines" | sed 's/^/    /'
  fi
  fail_msg "Exported component parsing failed; refusing to pass blindly"
fi

# Cleartext traffic - currently allowed by app; report as warning.
if printf '%s\n' "$manifest" | grep -Eq 'usesCleartextTraffic(="true"|[^=]*0xffffffff)'; then
  warn_msg "usesCleartextTraffic is enabled; confirm this is required and documented"
else
  pass_msg "usesCleartextTraffic is not enabled"
fi

# Backup flags - informational
if printf '%s\n' "$manifest" | grep -Eq 'allowBackup(="true"|[^=]*0xffffffff)'; then
  echo "INFO: android:allowBackup is true (review backup rules/data exposure)"
fi

echo
if [[ "$fail" -ne 0 ]]; then
  echo "APK AUDIT FAILED" >&2
  exit 1
fi
echo "APK AUDIT PASSED"
if [[ "$warn" -ne 0 ]]; then
  echo "APK AUDIT PASSED WITH WARNINGS"
fi
