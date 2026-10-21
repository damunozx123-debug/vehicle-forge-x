# BMW Forge X

**Native Android BMW-focused OBD engineering prototype. Version 0.1.0**

Not affiliated with BMW, BimmerCode, BimmerLink, bimmer-tool, Vgate or CarJam.
This project is original application code informed by publicly documented protocols and open-source research. It does **not** contain proprietary BMW databases or copied commercial application code.

## F10 coding workspace (early preview)

The native **Coding** tab now lists selected verified *feature names* for BMW F10/F11, their documented ECU dependencies, desired values, and a fail-closed readiness check. **It does not yet change real vehicle coding.** Generic SAE OBD2 PIDs are not BMW FDL coding datasets. A tested module transport, exact CAFD software mapping, read/backup/encode/write/read-back implementation, safe supply checks, and matching ECU identity are still required. The transaction model and offline blocking tests are implemented in `CodingModel.kt` and `CodingPlanTest.kt`. No physical ECU writes are wired up.

Reference: https://bimmercode.app/vehicles/f10/

## This version actually implements

- Native Kotlin/Jetpack Compose Android application, no WebView, no HTTP dashboard.
- Vgate / ELM327 **Bluetooth Classic SPP** transport with pairing via Android settings, asynchronous serialized reads, initialization and timeouts.
- ECU-advertised mode 01 PID capability discovery, adaptive RPM-priority polling, 22 standard sensor decoders, live RPM gauge, sparklines, support checks and units.
- SAE fault scan: stored (03), pending (07), permanent (0A). Offline human-oriented DTC knowledge with first diagnostic checks and caveats.
- Mode 01/01 raw readiness summary; Mode 02 freeze-frame zero readout; Mode 09/02 VIN parsing on common ELM output formats.
- Read-only lab terminal with strict allowlist. Command/response journal including latency.
- Contextual conservative alerts for temperature, fuel trim, supply voltage and manifold pressure estimation; never labels them confirmed diagnoses.
- Optional CSV recording with timestamps and raw replies, Android Storage Access Framework export.
- Saved manual NZ registration/vehicle details (private app storage), ECU VIN comparison by eye, optional *user-supplied* CarJam developer key basic vehicle lookup.
- F10/F11 module overview, engine-specific troubleshooting workflows and honest roadmap.
- Deterministic simulator so the UI can be exercised without a connected car. Visibly identified as DEMO.
- No cloud telemetry; CarJam lookup is the only remote request path and only when explicitly initiated.

### Capabilities explicitly NOT implemented

- **BMW multi-ECU diagnostics** (DSC, EGS, CAS, ACSM, KOMBI, gateway). Standard global-OBD fault services cannot substitute for this.
- **Coding** (BimmerCode-style FDL, FA/VO backup/restore), writing configurations, flashing, battery registration, adaptations, service resets, DPF regeneration, ABS service procedures, active tests.
- ENET/HSFZ native transport, UDS/KWP ECU-specific services, high-speed bus logging, CAN FD, or protocol security.
- BLE GATT, USB serial and Wi-Fi ELM327 (Bluetooth Classic SPP only).
- A validated BMW F10 vehicle database or verified fault definitions for every engine/module combination.
- Automatically resolving NZ plates without a **licensed API credential**; CarJam integration has not been tested with a credential in this environment.

These are engineering priorities, not missing buttons to be magically filled with pretend functionality.

## Android build (Windows laptop)

**Requirements:** recent Android Studio, Android SDK Platform **37**, Android SDK Build Tools, internet on first build, JDK **17** (Android Studio bundled JBR typically provides it). AGP 9.1.1 needs Gradle 9.3.1. The Compose BOM is 2026.09.00.

1. Unzip project; open the `BMW-Forge-X` directory in Android Studio.
2. Wait for Gradle sync and install SDK 37 when prompted.
3. Select `app` → **Run** on the connected Android phone (USB debugging enabled) or use **Build > Build APK(s)**.
4. Alternatively, open PowerShell in project directory and run:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\BUILD_WINDOWS.ps1 assembleDebug
   ```
5. Result on a successful build: `app/build/outputs/apk/debug/app-debug.apk`.
6. Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

Linux with JDK17 and Android SDK37:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

`gradlew` and `BUILD_WINDOWS.ps1` in this archive are **source-release bootstrap launchers**, because this container has no Gradle wrapper JAR. They download and invoke Gradle 9.3.1 from the official distribution. They are not a claim that the project has compiled here. A normal Android Studio checkout can generate a standard signed Gradle wrapper once it has a working Gradle install. For supply-chain hardening, pin Gradle distribution SHA-256 and commit the standard wrapper files prior to publishing releases.

**Build verification status:** The native APK was NOT compiled, installed or run in this environment: no Android SDK, Gradle or Android emulator was available, and container DNS/outbound access was unavailable. The isolated Kotlin diagnostic protocol core **was compiled and executed successfully** using the local `kotlinc` JVM compiler, passing 23 targeted checks. The native app remains unverified on device.

## How to connect to your Vgate adapter

1. With car stationary, handbrake on and ignition in appropriate state, insert Vgate into OBD-II connector.
2. Pair the adapter in Android system Bluetooth settings. The first release uses **Bluetooth Classic SPP**; if your adapter is BLE-only, it will not appear in the paired SPP list.
3. In BMW Forge X, select **Paired Vgate**, grant the Android Nearby Devices permission, and select the paired adapter.
4. App initializes ELM327, requests supported SAE PID bitmap, attempts VIN and reads general DTCs, then begins polling.
5. For an F10 diagnostic session, record data while stationary / with a passenger operating the phone. Export CSV before starting a new session; recording is in-memory in v0.1.

The official BimmerCode Android manual also recommends closing other OBD apps and disconnecting conflicting Bluetooth links when troubleshooting.

**If using Vgate vLinker BM+**: the manufacturer-recognized BimmerCode ecosystem supports it for BMW F-series, but this new app's implementation has NOT been verified against that exact hardware.

## NZ plate identification

- Enter a plate and make/model manually, saving it locally.
- Optional: supply *your own* CarJam Developer Connection API key in the app for a basic vehicle record request. Key is held in the UI session, not written to preferences; provider receives it via its documented HTTPS query scheme.
- The request uses `basic=1`, `owner=0`, `owners=0` and JSON output. No owner/name/address queries.
- It is **not** a free unrestricted plate-to-model API. Results may require credits, permitted access and adapting field mapping to actual paid-response schema.
- ECU VIN is independent of vehicle registry data and can be incomplete/unsupported.
- CarJam's API restrictions and NZTA vehicle-information access rules apply.

For production, move API credentials to an authorised backend if using a shared service key, restrict data retention, and implement schema fixtures from an actual licensed response.

## Folder map

```
app/src/main/java/com/dan/bmwforgex/
  MainActivity.kt           Compose interface + pairing/storage permission launchers
  ForgeViewModel.kt         Session orchestration, polling, scans, CSV, garage
  core/ObdCore.kt          ELM/SAE response parser, DTCs, VIN, sensor formulas
  core/FreezeFrame.kt      Mode 02 snapshot parser
  core/FaultKnowledge.kt   Offline explanations and F10 reference profile
  core/Insights.kt         Conservative multi-sensor checks
  data/Transport.kt        RFCOMM/ELM transport and simulator
  data/PlateLookup.kt      Optional licensed NZ plate request
app/src/test/               Unit tests ready for Android Studio
core_smoke_test.kt          Independent JVM test driver
REVIEW_AND_UPGRADES.md      Critique, scorecard and release gates
FEATURE_MATRIX.md          Implemented vs competitor functionality
```

## Standalone JVM protocol tests (no Android SDK)

```bash
kotlinc app/src/main/java/com/dan/bmwforgex/core/*.kt core_smoke_test.kt \
  -include-runtime -d core-smoke.jar
java -jar core-smoke.jar
```

These tests use synthetic responses. They validate neither Bluetooth radio performance nor compatibility with a real BMW.

## Sources used for design research

- Google/Android: [Compose BOM](https://developer.android.com/develop/ui/compose/bom), [AGP 9.1.1](https://developer.android.com/build/releases/agp-9-1-0-release-notes), Android Bluetooth runtime permissions.
- [BimmerCode supported adapters](https://bimmercode.app/adapters/) and [Android pairing manual](https://bimmercode.app/manual/).
- [BimmerLink features](https://bimmerlink.app/), [bimmer-tool feature list](https://bimmer-tool.com/index.php/application/).
- [AndrOBD (GPL-3)](https://github.com/fr3ts0n/AndrOBD), [OBDForge (GPL-3)](https://github.com/edwardlthompson/OBDForge), [AndroidOBD (MIT)](https://github.com/barnhill/AndroidOBD). Research and architecture reference only; no source copied or linked.
- [BMW F10 ENET N55 dashboard (MIT)](https://github.com/kaiwen-z/bmw-enet-tool-public-wenz77-on-bimmerforums), [klartext Rust BMW ENET (AGPL-3)](https://github.com/HadiCherkaoui/klartext), for future transport work, **not imported into this source**.
- [ELM327 official datasheet](https://www.elmelectronics.com/wp-content/uploads/2017/01/ELM327DS.pdf), [python-OBD command reference](https://github.com/cloud-rocket/python-OBD/blob/master/docs/Commands.md).
- [CarJam vehicle API docs](https://www.carjam.co.nz/dev:page?t=vehicle-queries), [CarJam API terms](https://www.carjam.co.nz/cms/developer-connection-api-terms-and-conditions/), [NZTA registry access rules](https://www.nzta.govt.nz/vehicles/how-the-motor-vehicle-register-affects-you/who-can-access-register-information).
- Reddit BMW discussions were used for field concerns about adapters, reconnection and coding backups, never as authoritative protocol specifications.

## Legal and physical safety

Use only on vehicles you own or have explicit authorization to test. Never operate the UI while driving; have a passenger perform data capture if moving. OBD commands can affect ECU state even if nominally read-only, and repeatedly polling a weak/unstable ECU can cause faults. **Do not write, flash, force DPF regeneration or adjust safety systems without manufacturer procedure, a stable supply and a verified recovery path.** All those writes are intentionally absent here.

## GitHub repository development

- Workflow: `.github/workflows/verify.yml` runs JVM protocol checks and attempts an Android debug build on every push/PR.
- CI is **not yet verified on GitHub Actions** and may require dependency/SDK adjustments.
- Executable JARs, build products, local API keys and signing keys are deliberately omitted from version control.
- `SECURITY.md` and `CONTRIBUTING.md` document disclosure, privacy and quality gates.

## Publish to your GitHub account

This prepared source snapshot contains a local initial Git commit on `main` and excludes binary outputs and credentials. Publishing requires logging into your own GitHub account. The default is **private**.

On Windows PowerShell, after installing `gh` and Git:

```powershell
.\PUBLISH_GITHUB.ps1            # create private GitHub repo and push
.\PUBLISH_GITHUB.ps1 -Public    # alternatively, publish publicly
```

On Linux, macOS or Termux with `git` and `gh` installed:

```bash
bash PUBLISH_GITHUB.sh           # private
bash PUBLISH_GITHUB.sh --public  # public
```

These scripts prompt for GitHub sign-in when needed and do not create or push to any account without valid credentials. For a repository name conflict, choose another name or set an existing remote manually.
