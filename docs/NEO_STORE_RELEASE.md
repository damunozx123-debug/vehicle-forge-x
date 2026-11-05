# Neo Store / F-Droid release gate

**Status: branding prepared, not submitted or approved.**

Neo Store is a client for F-Droid-compatible repositories, not a submission destination. This repository is public under MIT and has a native Kotlin/Compose Android app, but the published beta APK is debug-signed and not suitable for production release distribution.

## Required for listing
1. Verify all dependencies, assets, and licences are FOSS-compatible. Confirm unique applicationId `com.dan.bmwforgex`; keep it stable.
2. Supply an enduring **release signing key**, kept outside the repository. Use signed, non-debuggable, non-testOnly APKs and GitHub tagged releases. Never commit keystores.
3. Add genuine Android device screenshots to `fastlane/metadata/android/en-US/images/phoneScreenshots/`, plus a 512×512 PNG icon at `images/icon.png` and a 1024×500 PNG at `images/featureGraphic.png`. Do not pass off concept mockups as screenshots.
4. Run `./TEST_CORE.sh` and `./gradlew testDebugUnitTest assembleDebug` in a configured SDK environment; smoke-test Bluetooth permissions, simulator, exports and low-data states on actual Android.
5. Publish signed APK and source tag. Prefer F-Droid reproducible builds; alternatively a signed personal F-Droid-compatible index makes the app accessible through Neo Store.
6. Once evidence exists, submit to the F-Droid inclusion process. The F-Droid team decides whether it qualifies.

Branding: **Vehicle Forge** is the user-facing short name; GitHub repository and package ID remain unchanged. Logo concept uses a diagnostic ECG line across a car silhouette. Fastlane text is in `fastlane/metadata/android/en-US/`.

**Do not auto-publish a debug APK or claim an F-Droid listing from the presence of these files.**
