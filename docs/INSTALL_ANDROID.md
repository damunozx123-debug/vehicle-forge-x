# BMW Forge X — Android installation and controlled smoke test

This is an **experimental read-only OBD-II app**. No coding, ECU flashing, adaptation or multi-ECU BMW programming is implemented.

## GitHub debug build

Open [Android and protocol checks](https://github.com/damunozx123-debug/Bmw-forge-x/actions/workflows/verify.yml) and choose a successful build on the version you intend to install. If an Android APK artifact is present, extract it and verify its filename and signing status. A workflow may provide an **unsigned** APK: an unsigned file will not install directly. Do not confuse a successfully compiled APK with an installable or vehicle-tested release.

## Local Android Studio build

1. Install Android Studio with JDK 17 and Android SDK API 37.
2. Clone the repository, open its root directory and allow Gradle to sync.
3. Run `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` (or the documented Windows build helper).
4. Only install `app/build/outputs/apk/debug/app-debug.apk` after a successful **signed debug** build.
5. Connect an Android 8+ test device, enable USB debugging only for trusted machines, then `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Safe first use

Start in the app's **DEMO/simulator mode** without any vehicle connection. Confirm the UI marks simulated readings. Pair a compatible **Bluetooth Classic SPP** Vgate/ELM327 adapter from Android Bluetooth settings; Bluetooth LE-only adapters are not supported. When parked, with permission, connect and use read-only standard OBD2 live data/fault retrieval. Check that displayed trouble codes are not claimed to be confirmed component diagnoses. Never test while driving.

**Do not:** attempt coding, clear safety-system faults casually, run active ECU commands or treat this app as a qualified repair tool. Standard SAE engine diagnostics are not full BMW F10 ECU coverage.

## Verification

The repository contains protocol unit tests and a CI workflow; real adapter and vehicle integration are separate acceptance gates. See [SCREENSHOTS.md](SCREENSHOTS.md). All screenshots must be labelled DEMO unless captured from a consented, appropriately redacted physical test.
