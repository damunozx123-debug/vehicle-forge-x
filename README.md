# Vehicle Forge X

**Free, open-source Android vehicle diagnostics. Multi-brand ECU research and future paid coding support.**

[![Android CI](https://github.com/damunozx123-debug/vehicle-forge-x/actions/workflows/verify.yml/badge.svg)](https://github.com/damunozx123-debug/vehicle-forge-x/actions/workflows/verify.yml)
[![MIT License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 📲 Install on Android: direct APK

**[⬇ Download Vehicle Forge X Android beta APK](https://github.com/damunozx123-debug/vehicle-forge-x/releases/download/v0.3.0-beta.1/vehicle-forge-x-beta.apk)**

This is the current **tested CI beta** (v0.3.0-beta.1). Later development commits may not yet be in that APK. [Browse all releases](https://github.com/damunozx123-debug/vehicle-forge-x/releases) for the newest downloadable version.

1. Tap the direct APK link above on your Android phone and download the file.
2. Open the downloaded `vehicle-forge-x-beta.apk` from Downloads. If Android blocks it, grant **Install unknown apps** permission **to the app you opened the download with**, then retry. Menu wording varies by Android vendor.
3. Choose **Install** → **Open**. Start with **▶ Simulator**; no adapter or car is required.
4. On a parked vehicle you own or are authorised to diagnose, pair a compatible **Bluetooth Classic SPP ELM327/Vgate** reader in system Bluetooth settings. Then choose **Paired ELM327** inside the app.
5. In **Faults**, scan and share a VIN-safe diagnostic report. **Coding** shows offline-only research and synthetic simulations, not real ECU modifications.

**[📖 Full installation and troubleshooting guide](docs/INSTALL_ANDROID.md)** · **[SHA-256 checksum and release details](https://github.com/damunozx123-debug/vehicle-forge-x/releases/tag/v0.3.0-beta.1)** · **[Screenshots and provenance](docs/SCREENSHOTS.md)**

**Important:** This publicly distributed APK is **debug signed**. It may not update in place if rebuilt with a different debug signing certificate. Back up any private information/exports before removing an older installation. APK build tests do not prove compatibility with your particular adapter or vehicle.

## Free forever versus future Pro

| Free diagnostics: available in source | Pro: planned, not for sale |
| --- | --- |
| Generic SAE OBD-II DTCs: stored, pending and permanent | Verified manufacturer-specific *comfort/body* coding |
| Offline code explanations and first-check suggestions | Exact ECU firmware/version-matched coding profiles |
| Basic live PID readings, readiness, freeze frame, VIN attempts | Original configuration backup + guarded write + read-back |
| CSV exports and privacy-safe fault report sharing | Approved transport drivers with test evidence |
| Offline BMW report comparison and UDS DTC response analysis | Ongoing compatibility updates and support |
| Source code and simulator, forever free | No pricing or payments until a feature works on hardware |

**Fault codes (DTCs) are not configuration values.** We will not promise to "edit codes" as if changing P0301 repairs a misfire. Pro will target explicitly defined options for verified, authorised vehicles. There are **no real coding or flashing functions** in the current version.

## What is implemented

- Native Kotlin / Jetpack Compose Android app; no WebView.
- Standard SAE sensor polling on compatible OBD-II vehicles, DTC readout (03 / 07 / 0A), optional freeze frame, readiness, VIN parsing and local CSV.
- Bluetooth Classic SPP ELM327/Vgate adapter transport, serialised commands, timeouts and fail-closed read-only lab allowlist.
- Free offline DTC report sharing, intentionally without VIN or number plate.
- Offline UDS service 0x19 DTC response and 0x22 DID response decoding, for **already assembled** replies. These functions do not send UDS frames.
- 23-make *research* selector and eight candidate comfort/body feature types; **not** 23 verified coding implementations.
- Synthetic coding simulator with validated byte-mask/offset/firmware checks and before/after digests.
- Preserved BMW F10 reference feature catalogue and existing scan comparison tools.

### Not implemented or not demonstrated on physical hardware

- All-ECU manufacturer scans, authenticated ISO-TP/UDS live diagnostic sessions, manufacturer coding profiles, verified EEPROM/CAFD/as-built writes or recovery routines.
- ECU firmware flashing, power/boost/fuel tuning, immobilizer/security access, DPF regeneration, airbag or brake control changes.
- Bluetooth BLE GATT, Wi-Fi/USB adapter transports, Android store billing, a Pro subscription service or signed production releases.
- A comprehensive test matrix of actual makes/models/adapter chipsets.

## For developers

Requirements: Android Studio or JDK 17, Android SDK API 37 and build tools 36.0.0. Gradle 9.3.1 is bootstrapped by the repository launcher.

```bash
git clone https://github.com/damunozx123-debug/vehicle-forge-x.git
cd vehicle-forge-x

./TEST_CORE.sh
./gradlew testDebugUnitTest assembleDebug
```

APK path: `app/build/outputs/apk/debug/app-debug.apk`. In Android Studio, open the root folder, wait for Gradle sync, select `app`, then Run on a connected test device.

The application ID is still `com.dan.bmwforgex` to preserve existing package-data compatibility during this rebrand. Device tests are still required.

## Research, safety and contributions

- [Deep research: 2026 ECU diagnostics and coding across makes](docs/ECU_CODING_RESEARCH_2026.md)
- [Multi-brand coding architecture and safety gates](docs/MULTI_BRAND_CODING.md)
- [Implemented versus proposed compatibility](FEATURE_MATRIX.md)
- [Install guide](docs/INSTALL_ANDROID.md)
- [Source contributions](CONTRIBUTING.md)
- [Security and responsible disclosure](SECURITY.md)
- [MIT licence](LICENSE)

The GitHub code is publicly accessible. Contributions are welcome with test evidence and lawful data sources. No manufacturer-owned datasets, commercial software binaries, unlicensed calibration files, secret tokens or real VINs belong in this repository.

Only diagnose vehicles you own or are authorised to inspect. Use a parked vehicle with a safe electrical supply. Even read-only requests can upset unstable ECUs. All proposed writes are locked pending proof of hardware compatibility, complete backups, relevant permissions and a validated restore procedure.

*Not affiliated with BMW, Volkswagen Group, Ford, Toyota, Honda, Vgate, BimmerCode or any other brand named in the research.*
