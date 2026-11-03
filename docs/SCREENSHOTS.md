# Screenshot capture protocol

This native Kotlin/Compose application has a built-in deterministic simulator. Screenshots should be captured **from the actual installed app**, not drawn or fabricated.

## Capture from a safe demo

1. Build and install a signed debug APK per [INSTALL_ANDROID.md](INSTALL_ANDROID.md).
2. Open the app with **no car or OBD adapter connected**.
3. Choose the explicitly labelled DEMO/simulator screen.
4. On a test device with ADB enabled for a trusted computer:

```sh
mkdir -p docs/screenshots
adb exec-out screencap -p > docs/screenshots/bmw-forge-x-demo.png
```

5. Verify the file is a genuine device capture, then redact any VIN, plate, paired Bluetooth MAC or live ECU data before committing.

## Required gallery labels

Each capture must say whether it is a simulator run or an authorised on-vehicle observation. Show no coding writes or multi-ECU functionality as working: those are not implemented. No screenshots have been claimed to exist by adding this guide.
