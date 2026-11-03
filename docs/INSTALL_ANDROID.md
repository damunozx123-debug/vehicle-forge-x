# Vehicle Forge X - install on Android

Latest packaged and CI-tested beta: **v0.3.0-beta.1**. The repository's main branch may contain newer, unshipped changes. Do not assume the APK includes every recent commit.

## No computer needed: download and install

1. On your phone, open **[Download the Android APK directly](https://github.com/damunozx123-debug/vehicle-forge-x/releases/download/v0.3.0-beta.1/vehicle-forge-x-beta.apk)**. The file is a ~12 MB Android package.
2. In Android Downloads / Files, tap `vehicle-forge-x-beta.apk`.
3. If your phone blocks installing APKs, open **Settings → Apps → Special app access → Install unknown apps**, choose **the browser/file app that opened this APK**, and permit it to install this app. Exact menu wording varies on Xiaomi/HyperOS, Samsung, and stock Android.
4. Confirm the Android installation prompt, then open **Vehicle Forge X**.
5. Tap **▶ Simulator** on the Live tab. It deliberately shows fake sensor readings. This is a test, not a scan of your car.
6. Open **Faults** to see fault reports, **Garage** to save a vehicle description locally, **Coding** to see proposed but non-operational config changes and **Lab** for offline diagnostics research.
7. Return to Android's Install unknown apps settings and revoke that permission if you don't need sideloading afterward.

**Direct APK:** https://github.com/damunozx123-debug/vehicle-forge-x/releases/download/v0.3.0-beta.1/vehicle-forge-x-beta.apk

**Checksum:** https://github.com/damunozx123-debug/vehicle-forge-x/releases/download/v0.3.0-beta.1/SHA256SUMS.txt

The APK and SHA file were published by a GitHub Actions job after JVM protocol and Android unit tests passed. To verify integrity on Termux: `sha256sum ~/storage/downloads/vehicle-forge-x-beta.apk` and compare with the SHA256SUMS file. No physical car compatibility is implied by CI.

## A real car: read-only free diagnostics

1. Confirm a compatible **Bluetooth Classic SPP** ELM327 or Vgate adapter. A BLE-only reader is **not** currently supported.
2. Stop the car, park safely and turn ignition on when required. Don't touch the app while driving.
3. Plug adapter into the vehicle's OBD port. In phone **Settings → Bluetooth**, pair the adapter using its own manufacturer documentation.
4. Launch Vehicle Forge X → **Live → Paired ELM327** → grant Nearby Devices/Bluetooth access → tap the paired reader.
5. The app requests supported **standard** PID data, attempts VIN, and reads generic emissions-related codes. Unsupported PIDs must not appear as valid readings.
6. Open **Faults → Scan stored/pending/permanent**. Use **Share privacy-safe fault report** to send the generic code summary without VIN, plate or MAC address. Codes are clues, not proof a part failed.
7. The Coding tab offers plans and an offline sandbox only. **No write to ECU is enabled.**

## Troubleshooting

| Problem | What to check |
| --- | --- |
| Android refuses APK | Grant temporary permission to the specific installer browser/files app; make sure the APK fully downloaded |
| "App not installed" while updating | An older debug APK may have a *different signing key*. Back up your data before uninstalling the old app. Installing over an unrelatedly signed version fails |
| Paired device not listed | Bluetooth Classic SPP vs BLE-only, Android Nearby Devices permission, paired status in Settings; restart app |
| Paired but no ECU responds | Ignition state, OBD port power, protocol/adapter compatibility, other OBD apps holding the adapter, engine ECU availability |
| Codes appear in another specialist tool but not here | Manufacturer-specific ABS/body/gateway codes aren't in SAE generic OBD |
| No live sensor displayed | ECU may not advertise that PID; first connect and check protocol and supported sensors |
| Need coding/flashing | **Not available** in this beta. Pro driver requires exact ECU software mapping, backup, safe supply, bench testing and consent |

## Developers: build from source

```bash
git clone https://github.com/damunozx123-debug/vehicle-forge-x.git
cd vehicle-forge-x
./TEST_CORE.sh
./gradlew testDebugUnitTest assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK17, Android SDK platform 37, build tools 36.0.0 and Gradle 9.3.1. Android Studio can install the SDK components during sync. On Windows, open the root project in Android Studio and choose `app` → Run.

For source or CI development check [Android verification workflow](https://github.com/damunozx123-debug/vehicle-forge-x/actions/workflows/verify.yml) and [public releases](https://github.com/damunozx123-debug/vehicle-forge-x/releases).

Debug-signed APKs are for testing, not a production or Play Store release. Do not delete an installation until you have exported any data you wish to preserve.
