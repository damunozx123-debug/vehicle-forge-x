#!/usr/bin/env bash
# Run on your own trusted computer/Termux only; NEVER commit release credentials.
set -euo pipefail
umask 077
REPO="${1:?Usage: bash scripts/setup-release-signing.sh OWNER/REPOSITORY}"
for cmd in keytool openssl base64 gh; do command -v "$cmd" >/dev/null || { echo "Missing: $cmd"; exit 1; }; done
gh auth status >/dev/null
SAFE_NAME="${REPO//\//_}"
DIR="${XDG_CONFIG_HOME:-$HOME/.config}/android-release-keys/$SAFE_NAME"
mkdir -p "$DIR"
KEY="$DIR/release.p12"
PASSFILE="$DIR/release-password.txt"
if [[ -e "$KEY" || -e "$PASSFILE" ]]; then
  echo "Release key or password file already exists. Refusing to overwrite or rotate it."
  exit 1
fi
PW="$(openssl rand -hex 32)"
export KEY_PW="$PW"
keytool -genkeypair -v -storetype PKCS12 -keystore "$KEY" -alias release \
  -keyalg RSA -keysize 3072 -validity 10000 \
  -dname "CN=Android Release, O=Independent Developer, C=NZ" \
  -storepass:env KEY_PW -keypass:env KEY_PW >/dev/null
printf '%s\n' "$PW" > "$PASSFILE"
chmod 600 "$KEY" "$PASSFILE"
base64 "$KEY" | tr -d '\n' | gh secret set ANDROID_SIGNING_KEY_BASE64 --repo "$REPO"
printf '%s' "$PW" | gh secret set ANDROID_KEYSTORE_PASSWORD --repo "$REPO"
printf '%s' "$PW" | gh secret set ANDROID_KEY_PASSWORD --repo "$REPO"
printf '%s' release | gh secret set ANDROID_KEY_ALIAS --repo "$REPO"
unset PW KEY_PW
echo "Signing secrets uploaded for $REPO."
echo "CRITICAL: securely back up BOTH $KEY and $PASSFILE outside this phone/computer."
echo "If lost, existing installs cannot be updated with the same signing identity."
