#!/usr/bin/env bash
# Isolated Android QA for Vehicle Forge X. Never touches production app data or an ECU.
set -Eeuo pipefail
umask 077

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
SERIAL="${1:-52fd242b}"
DEV="com.dan.bmwforgex.dev"
RELEASE="com.dan.bmwforgex"
ACTIVITY="$DEV/com.dan.bmwforgex.MainActivity"
APK="${2:-$ROOT/app/build/outputs/apk/debug/app-debug.apk}"
EVIDENCE_ROOT="${XDG_STATE_HOME:-$HOME/.local/state}/vehicle-forge-x/android-qa"
RUN_DIR="$EVIDENCE_ROOT/$(date -u +%Y%m%dT%H%M%SZ)"
REDACTOR="$ROOT/scripts/android_qa_evidence.py"

die() { printf 'FAIL: %s\n' "$*" >&2; exit 1; }
command -v git >/dev/null || die "git missing"
command -v adb >/dev/null || die "adb missing"
command -v python3 >/dev/null || die "python3 missing"
command -v sha256sum >/dev/null || die "sha256sum missing"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$SDK" && -f "$ROOT/local.properties" ]]; then
  SDK="$(sed -n 's/^sdk.dir=//p' "$ROOT/local.properties" | tail -n 1)"
fi
[[ -n "$SDK" && -d "$SDK" ]] || die "Android SDK not configured (ANDROID_HOME/ANDROID_SDK_ROOT or local.properties)"
[[ -d "$SDK/platforms" && -d "$SDK/build-tools" ]] || die "SDK platforms/build-tools absent: $SDK"

mkdir -p "$RUN_DIR"
chmod 700 "$RUN_DIR"
printf 'Evidence directory: %s\n' "$RUN_DIR"
git rev-parse HEAD > "$RUN_DIR/git-head.txt"
git branch --show-current > "$RUN_DIR/git-branch.txt"
git status --short > "$RUN_DIR/git-worktree.txt"
printf 'SDK=%s\n' "$SDK" | python3 "$REDACTOR" > "$RUN_DIR/sdk-check.txt"

# Android builds and JVM tests. The release build and signing tasks are never used.
if ! ./gradlew --no-daemon --console=plain testDebugUnitTest assembleDebug 2>&1 \
    | python3 "$REDACTOR" > "$RUN_DIR/gradle-redacted.txt"; then
    die "Android unit tests/build failed; inspect $RUN_DIR/gradle-redacted.txt"
fi
[[ -f "$APK" ]] || die "debug APK missing: $APK"

AAPT="$(command -v aapt || true)"
if [[ -z "$AAPT" ]]; then
  AAPT="$(find "$SDK/build-tools" -type f -name aapt -perm /111 | sort -V | tail -n 1)"
fi
[[ -n "$AAPT" && -x "$AAPT" ]] || die "Cannot inspect APK identity safely (aapt unavailable)"
ACTUAL_PACKAGE="$("$AAPT" dump badging "$APK" | awk -F"'" '/^package: name=/{print $2; exit}')"
[[ "$ACTUAL_PACKAGE" == "$DEV" ]] || die "Refusing to install APK package '$ACTUAL_PACKAGE'; expected only $DEV"
sha256sum "$APK" > "$RUN_DIR/apk-sha256.txt"

# Target exact authorised USB serial. Do not silently switch to wireless devices.
[[ "$(adb -s "$SERIAL" get-state 2>/dev/null | tr -d '\r')" == "device" ]] \
  || die "ADB device '$SERIAL' is not online/authorised"
DEVICE_SDK="$(adb -s "$SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')"
DEVICE_MODEL="$(adb -s "$SERIAL" shell getprop ro.product.model | tr -d '\r')"
printf 'ADB_SERIAL=%s\nMODEL=%s\nSDK_INT=%s\n' "$SERIAL" "$DEVICE_MODEL" "$DEVICE_SDK" \
  | python3 "$REDACTOR" > "$RUN_DIR/device-redacted.txt"
[[ "$DEVICE_SDK" == "36" ]] || printf 'WARNING: device SDK is %s (Android 16 expected SDK 36)\n' "$DEVICE_SDK"

# Read-only check of release package; never uninstall, disable, launch or modify it.
if adb -s "$SERIAL" shell pm path "$RELEASE" | grep -q '^package:'; then
  echo "Production package present; left untouched" > "$RUN_DIR/production-check.txt"
else
  echo "Production package not reported by pm path; no action taken" > "$RUN_DIR/production-check.txt"
fi

if ! adb -s "$SERIAL" install -r "$APK" 2>&1 | python3 "$REDACTOR" > "$RUN_DIR/install-redacted.txt"; then
  die "DEV install failed, perhaps mismatched signing key. No uninstall/data clear attempted."
fi
if ! adb -s "$SERIAL" shell am start -W -n "$ACTIVITY" 2>&1 \
    | python3 "$REDACTOR" > "$RUN_DIR/launch-redacted.txt"; then
  die "DEV app launch failed"
fi
sleep 4
PID="$(adb -s "$SERIAL" shell pidof "$DEV" 2>/dev/null | tr -d '\r' | awk '{print $1}')"
[[ "$PID" =~ ^[0-9]+$ ]] || die "DEV process not running; process-specific logs unavailable"

# Only the dev app PID is collected. Raw logcat is piped directly into the redactor.
adb -s "$SERIAL" logcat -d "--pid=$PID" -t 800 -v threadtime 2>&1 \
  | python3 "$REDACTOR" > "$RUN_DIR/logcat-app-redacted.txt"
adb -s "$SERIAL" shell dumpsys package "$DEV" 2>/dev/null \
  | grep -E 'android.permission.(BLUETOOTH_CONNECT|INTERNET|BLUETOOTH_ADMIN)' \
  | python3 "$REDACTOR" > "$RUN_DIR/permissions-redacted.txt" || true

CRASH="PASS"
if grep -Ei 'FATAL EXCEPTION|AndroidRuntime: FATAL' "$RUN_DIR/logcat-app-redacted.txt" >/dev/null; then
  CRASH="FAIL"
fi

cat > "$RUN_DIR/QA_REPORT.md" <<EOF
# Vehicle Forge X Android QA evidence
- Commit: $(git rev-parse HEAD)
- Branch: $(git branch --show-current)
- Device serial: $SERIAL
- Device model: $DEVICE_MODEL
- Android SDK: $DEVICE_SDK
- Debug package: $ACTUAL_PACKAGE
- Debug APK SHA-256: $(sha256sum "$APK" | awk '{print $1}')
- Android unit tests and debug assembly: PASS
- Debug-only install: PASS
- Explicit MainActivity launch and process PID: PASS ($PID)
- Current-process fatal-exception log check: $CRASH (limited to sampled PID logs)
- Bluetooth permission: INSPECT permissions-redacted.txt (not changed by script)
- Simulator start and data: NOT TESTED (manual)
- Live/Faults/Garage/Coding/Lab navigation: NOT TESTED (manual)
- File-picker save/cancel and rotation: NOT TESTED (manual)
- Production package and data: NO WRITES
- Vehicle connection, scanning and ECU writes: NOT PERFORMED
EOF
printf '\nApp launched as isolated DEV. PID: %s | crash sample: %s\n' "$PID" "$CRASH"
printf 'Evidence: %s/QA_REPORT.md\n' "$RUN_DIR"
printf 'Manual: tap Simulator, check readings, visit five tabs; return to Faults and save a DEMO report.\n'
printf 'Manual: verify BT permission request without pairing a real adapter. Rotate while file picker is open.\n'
[[ "$CRASH" == "PASS" ]] || exit 1
