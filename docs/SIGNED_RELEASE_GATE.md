# Signed Android release: operational checklist

## Security model
- The `build` job compiles and tests unsigned Android source without any Android signing secrets.
- The `sign` job downloads only the same workflow run's APK artifact. It sees the GitHub signing secrets **only within the signing step**. It does not check out or execute repository source.
- When `.github/workflows/signed-release.yml` changes on `main`, a **one-time signed verification run** is triggered automatically. It publishes an Actions artifact called `vehicle-forge-signed-verification`, retained for 7 days, **not a GitHub Release or Neo Store listing**.
- Manual `workflow_dispatch` can create a GitHub Release only if the version is exactly `v0.3.1`, the existing tag/release is unused, and both `backup_confirmed` and `device_tested` inputs are explicitly `true`.

## Step 1. Protect the permanent signing identities
The four original signing identities live on the user's Termux device in `~/.config/android-release-keys/`. The GitHub workflow cannot verify or create a safe off-device backup.

Run this in the `vehicle-forge-x` local checkout:

```bash
cd ~/vehicle-forge-x
git pull --ff-only
bash scripts/backup-and-verify-signing-keys.sh
```

It validates all four local PKCS12 keystores, prompts for a separate archive password, creates an AES-256-CBC/PBKDF2 encrypted backup, decrypts it to verify archive integrity, and creates a SHA-256 checksum. Securely copy the encrypted archive **and its checksum** off the device and preserve the password separately. Do not upload raw keystores or password files to any Git repository.

## Step 2. Download and test a signed APK candidate

You can verify all four currently available signed test candidates against the **original local keystores** in one command:

```bash
cd ~/vehicle-forge-x
git pull --ff-only
bash scripts/download-verify-signed-candidates.sh
```

This downloads artifacts, checks SHA256 files, extracts signer SHA256 fingerprints and compares each to the original PKCS12 keystore. It refuses stale or missing artifacts and signature mismatches. It does **not** install, publish or upload private signing files.

Use GitHub **Actions → Signed Android verification and release**, choose the successful `push` run on `main`, download the `*-signed-verification` artifact, and extract it.

Verify `SHA256SUMS.txt` and `SIGNING_CERTIFICATE.txt`, then test `vehicle-forge.apk` on a real Android phone. At minimum check installation, launching, permissions, the main workflow and crashes; on upgrade tests use the same signing certificate. Do not confuse existing debug-signed previews with the new permanent-release identity.

If the signing verification job fails, inspect its GitHub Actions logs, repair it and run the verifier again before attempting a production release.

## Step 3. Publish the reviewed version
After the off-device backup and the real-device acceptance check **both succeed**, the user can run this from Termux:

```bash
gh workflow run signed-release.yml \
  -R damunozx123-debug/vehicle-forge-x \
  --ref main \
  -f version_tag=v0.3.1 \
  -f backup_confirmed=true \
  -f device_tested=true
```

The workflow rejects wrong package/version identity and existing tags. It publishes an APK, signing certificate and SHA256 checksum under GitHub Releases if all steps pass.

**Never tick the release confirmations without doing the corresponding checks.** For private projects, release assets remain tied to repository access. Making a repository public or submitting it to official F-Droid requires a deliberate independent review.

## Step 4. Neo Store
Neo Store is an F-Droid-compatible client. A GitHub Release alone does **not** create a Neo Store entry. A signed F-Droid index hosted over HTTPS, or F-Droid's own source-build inclusion review, is a separate release gate.
