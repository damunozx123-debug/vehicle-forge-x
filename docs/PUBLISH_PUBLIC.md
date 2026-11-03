# Vehicle Forge X publication and release status

**The repository is already PUBLIC and renamed.** Current URL: https://github.com/damunozx123-debug/vehicle-forge-x . Do not create or rename another repository.

## Direct Android installer

Public v0.3.0-beta.3 APK:
https://github.com/damunozx123-debug/vehicle-forge-x/releases/download/v0.3.0-beta.3/vehicle-forge-x-beta.apk

[Install on Android](INSTALL_ANDROID.md). The beta is not signed with a persistent release signing key; install and updates remain experimental.

## Automated beta releases

`.github/workflows/publish-beta.yml` compiles, runs Kotlin and Android tests, checksums the resulting APK and attempts to publish a GitHub prerelease with a direct APK link. The APK artifact exists even if GitHub refuses release publication.

Earlier, the **v0.3.0-beta.2** run https://github.com/damunozx123-debug/vehicle-forge-x/actions/runs/41588553944 succeeded in all compilation/test steps but its final `gh release create` failed: **HTTP 403: Resource not accessible by integration**. It was a GitHub workflow token permission problem, not an Android compile failure. **The subsequent v0.3.0-beta.3 build and direct GitHub release succeeded.** See https://github.com/damunozx123-debug/vehicle-forge-x/releases/tag/v0.3.0-beta.3.

### If release automation reports 403 again

Inspect **Repository Settings → Actions → General → Workflow permissions**. If available, select **Read and write permissions**, save, then rerun the publishing workflow after verifying its code. The workflow already requests `permissions: contents: write`. Repository policy may prevent this setting; do not bypass restrictions or put personal tokens into YAML.

If you cannot grant Actions release rights, publish a **tested artifact** using your authenticated GitHub CLI session, not by checking secrets into the project:

```sh
# Termux
pkg install gh git coreutils -y
gh auth login
gh repo clone damunozx123-debug/vehicle-forge-x
cd vehicle-forge-x

# Example: publish the beta.2 build from passing CI steps.
bash scripts/publish_ci_apk.sh v0.3.0-beta.2 41588553944
```

This helper confirms relevant CI steps passed, downloads the artifact, verifies its SHA-256, asks for confirmation, then publishes to GitHub Releases under your existing GitHub account. Never publish a release without verifying its build commit and signing provenance. For later betas use the matching tag, workflow run ID and artifact name.

Do not use an unsigned/unvalidated ECU coding build on a customer car. The debug APK is diagnostic beta software only.

## Future stable release requirement

Before Play Store/workshop distribution, produce a non-debug release signing key stored in GitHub encrypted secrets or another trusted signing infrastructure, rotate/recover carefully, use reproducible build inputs and verify an **in-place signed update** on Android. Never commit keystore material to a public repository.
