# Draft F-Droid inclusion checklist: Vehicle Forge

Official F-Droid packaging is a separate external review by the F-Droid maintainers. This file is **not a submitted request**.

- Source: https://github.com/damunozx123-debug/vehicle-forge-x
- Android package ID: `com.dan.bmwforgex`
- Licence: MIT (root `LICENSE`)
- Category: System (vehicle diagnostics)
- Build: Android Gradle Plugin 9.1.1, Gradle 9.3.1, Kotlin Compose 2.3.21, compile SDK API 37
- Minimum Android: API 26
- Purpose: authorised read-only OBD-II fault and sensor diagnostics; simulator and export
- Explicitly not implemented: ECU programming/flashing, manufacturer-wide ECU support
- Third-party dependencies: Jetpack Compose BOM, androidx Activity/Lifecycle, kotlinx-coroutines, JUnit; verify license and F-Droid-buildability at inclusion time.
- Audit the Android Internet permission for necessity and document any outgoing endpoints.
- Publish and verify a stable version tag matching `versionName`/`versionCode`; release APK must use enduring key or F-Droid's signing process.
- Screenshots must come from a genuine Android build, not edited mock-ups.
- F-Droid submission: https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/ and fdroiddata GitLab merge-request workflow.

Build reproducibility is not yet established; acceptance depends on the F-Droid maintainers.
