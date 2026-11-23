#!/usr/bin/env bash
# Run Exodus Privacy tracker analysis on an APK.
# Usage: scripts/exodus-audit.sh <apk-path> [json-report-path]
#
# Prefers local exodus-standalone; falls back to Docker image.
set -euo pipefail

APK="${1:?Usage: scripts/exodus-audit.sh <apk-path> [json-report-path]}"
REPORT="${2:-exodus-report.json}"

if [[ ! -f "$APK" ]]; then
  echo "ERROR: APK not found: $APK" >&2
  exit 1
fi

echo "== Exodus Privacy tracker scan =="
echo "APK: $APK"
echo "Report: $REPORT"

if command -v exodus-standalone >/dev/null 2>&1; then
  # CLI options vary by version; support common forms.
  if exodus-standalone --help 2>&1 | grep -q -- '-j'; then
    exodus-standalone -j -o "$REPORT" "$APK"
  else
    exodus-standalone --json "$APK" > "$REPORT"
  fi
elif command -v docker >/dev/null 2>&1; then
  APK_DIR=$(cd "$(dirname "$APK")" && pwd)
  APK_NAME=$(basename "$APK")
  REPORT_DIR=$(cd "$(dirname "$REPORT")" && pwd)
  REPORT_NAME=$(basename "$REPORT")
  docker run --rm -v "$APK_DIR:/apk" -v "$REPORT_DIR:/out" exodusprivacy/exodus-standalone \
    -j -o "/out/$REPORT_NAME" "/apk/$APK_NAME"
else
  echo "ERROR: exodus-standalone or docker is required." >&2
  exit 2
fi

# Basic pass/fail: no trackers detected.
# exodus-standalone JSON shape can vary by version; handle common cases.
python3 - "$REPORT" <<'PY'
import json, sys
path = sys.argv[1]
with open(path, 'r', encoding='utf-8') as f:
    data = json.load(f)

# Possible shapes: {"trackers": [...]}, [{"trackers": [...]}], {"reports":[...]}
candidates = []
if isinstance(data, dict):
    if 'trackers' in data:
        candidates.append(data)
    for key in ('reports', 'apks'):
        if isinstance(data.get(key), list):
            candidates.extend([x for x in data[key] if isinstance(x, dict)])
elif isinstance(data, list):
    candidates.extend([x for x in data if isinstance(x, dict)])

trackers = []
for c in candidates:
    if isinstance(c.get('trackers'), list):
        trackers = c['trackers']
        break

print(f"Trackers detected: {len(trackers)}")
for t in trackers:
    if isinstance(t, dict):
        print(f" - {t.get('name', t)}")
    else:
        print(f" - {t}")

if len(trackers) != 0:
    print('FAIL: Known trackers detected', file=sys.stderr)
    sys.exit(1)
print('PASS: No known trackers detected')
PY
