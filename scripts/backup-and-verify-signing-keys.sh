#!/usr/bin/env bash
# Termux / Linux. Run locally. Never paste private keys or passwords into chat.
set -euo pipefail
umask 077
ROOT="${XDG_CONFIG_HOME:-$HOME/.config}/android-release-keys"
DEST="${1:-$HOME/android-signing-backup-$(date +%Y%m%d).tar.gz.enc}"
for command_name in tar openssl keytool sha256sum; do
  command -v "$command_name" >/dev/null || { echo "Missing: $command_name"; exit 1; }
done
test -d "$ROOT" || { echo "Signing key folder not found: $ROOT"; exit 1; }
for app in vehicle-forge-x Patternx gpu-efficiency-lab-android net-intel-app-v9; do
  path="$ROOT/damunozx123-debug_$app"
  test -s "$path/release.p12" && test -s "$path/release-password.txt" ||
    { echo "Missing key/password for $app"; exit 1; }
  KEY_PW="$(cat "$path/release-password.txt")"
  export KEY_PW
  if ! keytool -list -storetype PKCS12 -keystore "$path/release.p12" -storepass:env KEY_PW >/dev/null 2>&1; then
    unset KEY_PW
    echo "Keystore failed validation for $app"
    exit 1
  fi
  unset KEY_PW
  echo "Verified keystore: $app"
done
test ! -e "$DEST" || { echo "Backup already exists: $DEST. Refusing overwrite."; exit 1; }
test ! -e "$DEST.sha256" || { echo "Checksum already exists. Refusing overwrite."; exit 1; }
test -w "$(dirname "$DEST")" || { echo "Destination folder is not writable"; exit 1; }
echo "Create a STRONG password for the encrypted backup. Store it separately."
tar -czf - -C "$(dirname "$ROOT")" "$(basename "$ROOT")" |
  openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 -out "$DEST"
chmod 600 "$DEST"
echo "Re-enter the archive password to verify that it decrypts and the tar reads correctly."
if ! openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -in "$DEST" | tar -tzf - >/dev/null; then
  echo "Archive verification FAILED; do not delete original keys."
  exit 1
fi
sha256sum "$DEST" > "$DEST.sha256"
chmod 600 "$DEST.sha256"
echo "Archive verified: $DEST"
echo "Checksum: $DEST.sha256"
echo "COPY BOTH files to trusted OFF-DEVICE storage before publishing APKs."
echo "Do NOT delete ~/.config/android-release-keys or store the password with the backup."
