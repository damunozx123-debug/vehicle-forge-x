# Signed Android release and Neo Store distribution

**Status:** Release automation is implemented, but no production APK, signed F-Droid index, official F-Droid listing or real-device acceptance test is claimed.

## Release integrity
- Native package ID: `com.dan.bmwforgex`. Do not rename it to improve marketing; changing IDs breaks Android updates.
- Proposed store title: **Vehicle Forge**. Root MIT covers project-owned software; audit any third-party fonts, data, photos and other content before making source public.
- Private repositories must remain private until credentials, test fixtures, personal data, copyright and dependency usage have been reviewed.
- Production signing keys are long-lived secrets. These must be backed up **outside** this environment.

## One-time signing setup (trusted device with GitHub CLI and Java 17)
In a locally cloned checkout, install `gh`, `keytool` (OpenJDK 17) and `openssl`, then run:

```sh
bash scripts/setup-release-signing.sh damunozx123-debug/vehicle-forge-x
```

The script creates a **unique per-repository keystore** under `~/.config/android-release-keys/`, refuses to overwrite existing keys, and uploads four Android signing secrets via `gh secret set`. It **never commits them**. Securely back up the private keystore plus password file before making any release.

## Build and sign
1. Ensure unit/build CI passes and app runs on a real Android device.
2. Update `versionCode` and `versionName` as appropriate. `versionCode` must increase for each update.
3. On GitHub, open **Actions → Publish verified signed Android APK → Run workflow**, select branch `main`, and enter tag `v0.3.0-beta.3` only if it exactly matches the current `versionName`.
4. The workflow fails if a key is missing, checks the application ID and version name, verifies the APK signature and ZIP alignment, publishes a SHA256 checksum, and creates a GitHub release with that tag.
5. Verify the signed APK on an actual supported phone, including installation, update from the previous **release-signed** version, permissions, offline behavior where promised, and export/import behavior.

If the release tag already exists, raise the app version instead of overwriting an immutable published release. The existing debug APKs are for testers and might not update in place.

## Make it visible in Neo Store
Neo Store reads **F-Droid-compatible repositories**. A GitHub Releases APK alone is not a Neo Store listing.
- **Route A (recommended):** Prepare an independently reproducible build recipe and request official F-Droid inclusion through the F-Droid GitLab `fdroiddata` merge request process after public-source and dependency checks.
- **Route B:** Use `fdroidserver` on a trusted machine to create a separate signed repository index, upload only the public `repo/` folder (never `config.yml` or keystore) to HTTPS hosting, and distribute its repository URL and signer fingerprint to Neo Store users.
- Supply generated `fastlane/metadata/android/en-US/images/icon.png` (512x512), `featureGraphic.png` (1024x500), and authentic Android screenshots captured by the emulator job. Emulator screenshots must be reviewed for accuracy and sensitive data before store use.

No repository URL or APK release is being represented as live until the signed index is independently fetched and tested in Neo Store.

## Suggested next steps
- Use the included GitHub Action to generate the two static graphics.
- Use the emulator screenshot workflow to collect genuine screenshots and verify the package launches.
- Back up and register signing keys on your own device.
- Carry out the first signed release; only then publish a signed F-Droid-compatible index and request official inclusion.
