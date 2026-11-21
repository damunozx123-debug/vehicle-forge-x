# Vehicle Forge X: BlackArch + ADB + local Ollama

**Scope:** The Android application is tested on your phone, not on the car. This workflow sends no OBD, CAN, ISO-TP, UDS, coding, clearing or flashing commands. ADB is only used to install or launch the Android app, collect process-specific logs and retrieve an intentionally saved privacy-safe report.

## Setup on BlackArch

Prerequisites: JDK 17, Kotlin CLI, configured Android SDK, Android platform tools (ADB), and a local Ollama instance on 127.0.0.1:11434 with a suitable installed model.

    cd ~/dan-workspace/repos/vehicle-forge-x
    git fetch origin
    git switch feature/local-ai-adb-diagnostics-20261122
    git pull --ff-only
    ollama list
    ollama pull qwen2.5-coder:3b
    export DAN_OLLAMA_MODEL=qwen2.5-coder:3b
    adb devices

The script never downloads an AI model or contacts a cloud AI API. If a different model is already installed, use the --model option.

## 1. Automated build + Android smoke test

    python3 scripts/dan_local_lab.py test --adb --review

Runs TEST_CORE.sh, Gradle unit tests and debug APK assembly. Installs the debug APK as **com.dan.bmwforgex.dev** using adb install -r, launches its **com.dan.bmwforgex.MainActivity** component, verifies a running process and saves process-specific logs. Release builds still use **com.dan.bmwforgex**. Both can coexist without conflicting signatures. If an older dev build has an incompatible signature, installation stops rather than uninstalling or resetting data.

Use --serial DEVICE if multiple devices are connected. Without --adb, only desktop tests run. --review asks Ollama to assess results even when passing.

Optional restricted AI patch mode:

    python3 scripts/dan_local_lab.py test --adb --auto-fix
    git diff --check
    git diff

Auto-fix only considers small unified diffs to pure-Kotlin parser and test files. It requires a clean non-main branch, blocks transport/security/coding files, reruns core tests and leaves successful changes uncommitted for review. It never drives the vehicle, merges, publishes or uploads captures.

## 2. Let the local model read BMW diagnostic codes

1. Park your 2013 BMW F10 528i and connect a compatible Bluetooth Classic SPP OBD adapter manually in Vehicle Forge X.
2. In Faults, perform the generic stored/pending/permanent scan. Never clear faults merely for testing.
3. Tap **Save report for local AI (no identifiers)**, choose Android Downloads and use the suggested filename **vehicle-forge-diagnostic.txt**.
4. With an authorised ADB link from BlackArch to your Redmi, run:

    python3 scripts/dan_local_lab.py diagnose --pull-adb

If you already transferred the report another way:

    python3 scripts/dan_local_lab.py diagnose --report ~/Downloads/vehicle-forge-diagnostic.txt

The analysis reports FACTS, POSSIBLE CAUSES, PRIORITISED SAFE CHECKS, MISSING EVIDENCE and SAFETY. Generic OBD-II codes do not imply full BMW all-module coverage, and a diagnosis from an LLM is not a proven component failure. The analyser labels simulated reports DEMO.

## Evidence, privacy and boundaries

Outputs are stored outside Git under ~/.local/share/vehicle-forge/lab/YYYYMMDDTHHMMSSZ/. Redaction masks common VINs, MACs and credential key/value strings. Logs are restricted to the app process, but users should still inspect local evidence before sharing anything.

Not automated: Bluetooth pairing, tapping Scan, BMW manufacturer-specific module scans, ECU writes, DTC clearing, remote car control, production signing, release publishing or automatic GitHub changes. CI success is not evidence of vehicle compatibility.

If the local model is unavailable, confirm Ollama is running and inspect ollama list. If the Android build fails because of the SDK, configure the SDK rather than applying an AI code patch.
