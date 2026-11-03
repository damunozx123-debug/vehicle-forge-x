# Vehicle Forge X

**Open-source-ready native Android OBD-II diagnostics and multi-brand coding research platform. v0.2.0 beta.**

> Status: works at source level as a generic OBD-II diagnostic app and offline coding *planner*. **Real ECU programming is not implemented.** Never interpret a simulated coding result as a vehicle change.

Previously **BMW Forge X**. The original BMW F10 coding reference remains available. Not affiliated with any vehicle manufacturer, BimmerCode, BimmerLink, Vgate or CarJam.

## What you can use today

- **Broad OBD-II diagnostics** on compatible vehicles and adapters: SAE Mode 01 sensor readings, advertised PID discovery, live trends, voltage, generic stored/pending/permanent fault codes, readiness bits, basic freeze frame, VIN attempts and CSV exports.
- **Native Android** Kotlin/Jetpack Compose interface with paired **Bluetooth Classic SPP** ELM327/Vgate adapters. BLE-only, Wi-Fi and USB OBD adapters are not yet implemented.
- **23-make vehicle selection for coding plans** covering BMW/MINI, VW Group, Mercedes-Benz, Ford, Toyota, Honda, Hyundai/Kia, Mazda, Nissan, Subaru, Volvo, Peugeot/Citroen, Renault and others. This list is for organising plans, **not verified feature compatibility**.
- **8 candidate coding categories** such as lock feedback, automatic locking, mirrors, lighting, climate memory, display units, windows and start/stop preference.
- **Safe coding simulator:** synthetic byte-state change, before/after SHA-256 and simulated read-back. It is fully disconnected from the physical vehicle interface.
- **Configuration plan export:** documents the chosen option, possible ECU family and exact missing prerequisites while masking the VIN.
- **Offline UDS ReadDataByIdentifier response decoder** for diagnostic research (parsing only, no UDS requests sent).
- **BMW F10 reference catalogue retained** for legacy feature research, without unsupported CAFD claims.
- **Offline sensor simulator** so contributors can inspect the app without a car or Bluetooth adapter.

## What this version does NOT do

There is **no universal car coding**. There are no real ECU configuration writes, manufacturer-specific parameter datasets, live UDS 0x2E writes, ECU security-access operations, ECU flashing, battery registration, DPF regeneration, service resets, vehicle-specific all-module scans or active tests. Generic OBD-II does not grant access to those functions. See [coding architecture and release gates](docs/MULTI_BRAND_CODING.md).

## Build and install

Prerequisites: JDK 17, Android SDK 37, Gradle 9.3.1 and required SDK components.

```sh
git clone https://github.com/damunozx123-debug/Bmw-forge-x.git
cd Bmw-forge-x
./gradlew testDebugUnitTest assembleDebug
```

The clone URL above remains the current URL until the repository has been renamed. When the name changes, use `damunozx123-debug/vehicle-forge-x`.

A successful debug build creates `app/build/outputs/apk/debug/app-debug.apk`. For CI, see [Android build workflow](.github/workflows/verify.yml). **APK signing and installation should be tested on your device; the GitHub build does not prove physical-car compatibility.**

[Android install instructions](docs/INSTALL_ANDROID.md) · [Screenshots](docs/SCREENSHOTS.md)

## Quick start

1. Open the app and tap the offline **DEMO** option to see synthetic engine readings.
2. For a real vehicle you own or are authorised to inspect, pair a compatible Bluetooth Classic ELM327 adapter in Android settings while the car is parked. Connect in the app.
3. Only ECU-advertised standard PIDs are shown. Export scan data if needed.
4. Open **Coding** to choose a make and candidate feature. Use the **offline sandbox** to preview a synthetic change. The app will always block real writes.
5. If preparing an actual feature, export a VIN-masked configuration plan and source the exact OEM ECU/firmware coding definition before attempting any separately validated implementation.

## Source structure

```text
app/src/main/java/com/dan/bmwforgex/
├── MainActivity.kt                  Android UI
├── ForgeViewModel.kt               Live session, data capture and garage
├── core/
│   ├── ObdCore.kt                   Generic SAE OBD response decoding
│   ├── CodingModel.kt               Legacy BMW catalog + fail-closed transaction gate
│   ├── UniversalCoding.kt           Multi-brand planning, sandbox and UDS response parser
│   └── ...                          Fault knowledge, insights and CSV/report helpers
└── data/
    ├── Transport.kt                 Bluetooth SPP and no-hardware simulator
    └── PlateLookup.kt               Optional licensed NZ plate lookup
```

Tests: `./TEST_CORE.sh` and `./gradlew testDebugUnitTest`. All coding tests use synthetic data. The Android application ID remains `com.dan.bmwforgex` to avoid breaking existing application data merely for a branding change.

## Safety and public release

This project is intended for authorised diagnostics and responsible open-source collaboration. Do not operate it while driving. Writing to a vehicle module without exact manufacturer documentation and hardware testing can cause irreversible damage or unsafe behaviour.

- [Vehicle coding architecture](docs/MULTI_BRAND_CODING.md)
- [Public repository rename and publication](docs/PUBLISH_PUBLIC.md)
- [Security policy](SECURITY.md)
- [Contribution rules](CONTRIBUTING.md)
- [MIT licence](LICENSE)

The repository currently uses its historical slug. Visibility changes require repository-admin GitHub permissions; changing its in-app name **does not automatically make GitHub public**.
