# Vehicle Forge X - Android install & first-run verification

**v0.2 beta:** native Android OBD-II diagnostic reader plus multi-brand coding *planner*. No real ECU writes.

## Get the CI APK

1. Open [Vehicle Forge X Android and protocol checks](https://github.com/damunozx123-debug/Bmw-forge-x/actions/workflows/verify.yml).
2. Choose a **completed successful run on the commit you intend to use**.
3. Under **Artifacts**, download `vehicle-forge-x-debug-apk`; unzip `app-debug.apk` and `SHA256SUMS.txt`.
4. Run `sha256sum app-debug.apk` and compare with `SHA256SUMS.txt`.
5. Install the APK on a test Android phone via the trusted-source installation prompt; revoke that permission after installation if no longer needed.

The APK is a debug build, **not** a Play Store release. Separate GitHub runners may use different debug signing keys, so in-place updates are not guaranteed. Back up any local CSV exports and garage information before reinstalling.

## Build locally

Requires Java 17, Android SDK API 37 and build-tools 36.0.0, Gradle 9.3.1:

```sh
git clone https://github.com/damunozx123-debug/Bmw-forge-x.git
cd Bmw-forge-x
./gradlew testDebugUnitTest assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Test safely

1. Launch the **DEMO** (no adapter) and check that all live readings clearly say synthetic.
2. Navigate the generic DTC, garage and sensor sections.
3. In **Coding**, choose a make and candidate comfort feature, preview prerequisites and run the **offline** synthetic coding sandbox. Check the screen states **Real ECU coding write access: DISABLED**.
4. Export a VIN-masked coding plan and verify it contains no full VIN.
5. On a parked vehicle you own or are authorised to test, pair a Bluetooth Classic SPP-compatible ELM327 device. Use only standard read-only OBD-II functions in this build. BLE-only adapters are not supported.
6. Verify missing PIDs or data do not get labelled as successful measurements.

**Not implemented:** manufacturer-specific coding, UDS vehicle write services, ECU flash, immobilizer work, battery registration or complete BMW/other-brand module scans.

See [multi-brand coding engineering scope](MULTI_BRAND_CODING.md), [screenshot capture](SCREENSHOTS.md) and [public release procedure](PUBLISH_PUBLIC.md).
