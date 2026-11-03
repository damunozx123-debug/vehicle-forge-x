#!/usr/bin/env bash
# Publish a GitHub Actions Android APK that passed build/tests, using the owner's
# local authenticated gh session. No PAT in source and no rebuild required.
set -euo pipefail
REPO="damunozx123-debug/vehicle-forge-x"
TAG="${1:-}"
RUN_ID="${2:-}"
if [[ ! "$TAG" =~ ^v[0-9]+\.[0-9]+\.[0-9]+-beta\.[0-9]+$ || ! "$RUN_ID" =~ ^[0-9]+$ ]]; then
  echo "Usage: bash scripts/publish_ci_apk.sh v0.3.0-beta.2 41588553944" >&2
  exit 2
fi
command -v gh >/dev/null || { echo "Install GitHub CLI (Termux: pkg install gh)"; exit 1; }
command -v sha256sum >/dev/null || { echo "Install coreutils"; exit 1; }
gh auth status >/dev/null || { echo "Log in using: gh auth login"; exit 1; }

# Explicitly verify the expected test/build steps, even when the publishing job
# failed solely at the release API step.
STEPS="$(gh run view "$RUN_ID" -R "$REPO" --json jobs --jq '[.jobs[].steps[] | select(.name=="Run protocol, offline coding and diagnostics smoke tests" or .name=="Android JUnit tests and debug APK") | .conclusion]')"
if [[ "$STEPS" != *"success"* ]]; then
  echo "No passing build evidence in this run"; exit 1
fi
SUCCESS_COUNT="$(gh run view "$RUN_ID" -R "$REPO" --json jobs --jq '[.jobs[].steps[] | select((.name=="Run protocol, offline coding and diagnostics smoke tests" or .name=="Android JUnit tests and debug APK") and .conclusion=="success")] | length')"
if [[ "$SUCCESS_COUNT" != "2" ]]; then
  echo "Not both required test/build steps succeeded. Refusing release."; exit 1
fi

if gh release view "$TAG" -R "$REPO" >/dev/null 2>&1; then
  echo "Release $TAG already exists. Refusing to overwrite public APK."; exit 1
fi

DIR="$(mktemp -d)"
trap 'rm -rf "$DIR"' EXIT
gh run download "$RUN_ID" -R "$REPO" -n "vehicle-forge-x-$TAG" -D "$DIR"
(cd "$DIR" && sha256sum -c SHA256SUMS.txt)
test -s "$DIR/vehicle-forge-x-beta.apk"

COMMIT="$(gh run view "$RUN_ID" -R "$REPO" --json headSha --jq '.headSha')"
echo "Build commit: $COMMIT"
echo "About to publish a PUBLIC prerelease $TAG with this tested APK."
echo "This is an unstable debug APK, not a real ECU coding tool."
read -r -p "Type PUBLISH to confirm: " CONFIRM
[[ "$CONFIRM" == "PUBLISH" ]] || { echo "Cancelled"; exit 1; }

gh release create "$TAG" -R "$REPO" \
  "$DIR/vehicle-forge-x-beta.apk" "$DIR/SHA256SUMS.txt" \
  --target "$COMMIT" --prerelease \
  --title "Vehicle Forge X $TAG - experimental diagnostics" \
  --notes "Direct Android beta APK, built and tested in GitHub Actions run $RUN_ID. Generic OBD-II reads, free diagnostic reporting, offline research only. No real ECU coding, security access, reprogramming or emissions tuning. Debug signing may change between builds; test on a parked authorised vehicle. Verify SHA256SUMS.txt."
echo "Released: https://github.com/$REPO/releases/tag/$TAG"
