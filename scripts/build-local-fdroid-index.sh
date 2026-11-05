#!/usr/bin/env bash
# Generates a signed F-Droid-compatible repository on a TRUSTED LOCAL MACHINE.
# Never upload config.yml, private key files, or the entire working directory.
set -euo pipefail
umask 077
if (($# == 0)); then
  echo "Usage: bash scripts/build-local-fdroid-index.sh /path/to/signed-release.apk [more signed APKs]"
  exit 2
fi
for cmd in fdroid apksigner; do
  command -v "$cmd" >/dev/null || { echo "Install $cmd first"; exit 1; }
done
# Resolve source paths before changing the working directory.
APKS=()
for input in "$@"; do
  [[ -f "$input" ]] || { echo "APK not found: $input"; exit 1; }
  APKS+=("$(realpath "$input")")
done
FDROID_WORKDIR="${FDROID_WORKDIR:-$HOME/.local/share/munoz-fdroid}"
mkdir -p "$FDROID_WORKDIR"
cd "$FDROID_WORKDIR"
if [[ ! -f config.yml ]]; then
  echo "Initializing a NEW repository-signing identity in $FDROID_WORKDIR"
  fdroid init
  chmod 600 config.yml
fi
mkdir -p repo metadata
for apk in "${APKS[@]}"; do
  [[ -f "$apk" ]] || { echo "APK not found: $apk"; exit 1; }
  b="$(basename "$apk")"
  if [[ "$b" =~ [Dd][Ee][Bb][Uu][Gg] ]]; then
    echo "Refusing to include debug-named APK: $b"
    exit 1
  fi
  apksigner verify --verbose "$apk" >/dev/null || { echo "APK failed signature verification: $b"; exit 1; }
  cp -f "$apk" "repo/$b"
done
fdroid update --create-metadata
[[ -s repo/index-v2.json ]] || { echo "Missing signed modern repository index"; exit 1; }
echo "Local index prepared at $FDROID_WORKDIR/repo"
echo "Do not publish $FDROID_WORKDIR/config.yml or the private signing keystore."
echo "Publish ONLY the repo/ directory over HTTPS and test the fingerprint with Neo Store."
