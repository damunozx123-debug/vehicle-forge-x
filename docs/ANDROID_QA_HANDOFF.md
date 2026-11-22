# Vehicle Forge X Android QA handoff

Scope: Android 16 Redmi Note 14 Pro+, USB ADB 52fd242b, separate DEV package only. No vehicle connection, ECU scan, coding or DTC clearing. Do not uninstall or clear com.dan.bmwforgex. The local AI testing engine is out of scope.

## BlackArch prerequisites

```bash
cd ~/dan-workspace/repos/vehicle-forge-x
git fetch origin
git switch fix/android-qa-lifecycle-20261122
git pull --ff-only
git rev-parse HEAD
git status --short
command -v adb
adb devices -l
```

Ensure USB-identified Redmi appears as 52fd242b device. When unauthorized, approve the USB-debugging prompt on the phone. Android SDK must be installed and referenced by ANDROID_HOME, ANDROID_SDK_ROOT or local.properties. GitHub CI SDK setup does not prove BlackArch SDK readiness.

## Automated QA command

```bash
bash -n scripts/android_qa_smoke.sh
python3 -m unittest discover -s scripts -p 'test_android_qa_*.py' -v
bash scripts/android_qa_smoke.sh 52fd242b
```

The script runs Android JVM tests and assembles debug, verifies the APK application ID exactly com.dan.bmwforgex.dev before install, records SHA-256, checks the specified ADB serial, installs only the dev APK via adb install -r, launches com.dan.bmwforgex.MainActivity, checks PID, and collects redacted process-specific logcat.

Evidence: ~/.local/state/vehicle-forge-x/android-qa/YYYYMMDDTHHMMSSZ/QA_REPORT.md. Every result is tied to the local commit. The script never claims manual simulator or navigation PASS.

Redaction masks common VIN-like strings, adapter MACs, email addresses, IPv4 addresses, NZ phone numbers, plate fields and typical credentials. This is best effort, not a guarantee: inspect locally before sharing.

## Manual acceptance matrix (DEV only)

| Test | On-device action | PASS criteria |
|---|---|---|
| A01 | Open Vehicle Forge DEV | App renders, no fatal exception |
| A02 | Tap Simulator from Live | DEMO label and changing synthetic sensor readings |
| A03 | Navigate Live / Faults / Garage / Coding / Lab / Live | All five tabs render; no crash |
| A04 | Rotate the screen while on Faults | Remains on Faults |
| A05 | In simulator mode, choose Save report for local AI | Android file picker opens and report is clearly marked SYNTHETIC DEMO |
| A06 | Rotate while report picker is open, then save | Saved text is nonempty, valid and contains no user identifiers |
| A07 | Inspect Bluetooth permission request without pairing a car | Denial and grant flows respond without crash |
| A08 | Inspect production app status | Existing production package and data unchanged |
| A09 | Background, reopen, lock and unlock | App remains stable and does not auto-connect to a car |

Inspect A05/A06 saved text locally using the Android Files app. No actual vehicle data should be used.

## Status language

- SOURCE REVIEWED: confirmed from branch code.
- CI PASS: specific GitHub run succeeded for a specific SHA.
- ADB PASS: evidence from the matching local APK, phone, serial, process and log.
- MANUAL PASS: user actually exercised each UI case.
- UNKNOWN: no device evidence or incomplete manual case.

Do not merge PR #16 or the QA follow-up solely because GitHub CI passes. This is strictly offline/simulator QA.
