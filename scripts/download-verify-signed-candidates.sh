#!/usr/bin/env bash
# Download and compare official release-candidate certificates to original local keys.
# Never logs or uploads signing passwords or private key material.
set -euo pipefail
umask 077

for cmd in gh keytool sha256sum awk tr grep; do
  command -v "$cmd" >/dev/null || { echo "Missing command: $cmd"; exit 1; }
done
gh auth status >/dev/null
ROOT="${XDG_CONFIG_HOME:-$HOME/.config}/android-release-keys"
OUT="${1:-$HOME/signed-android-candidates}"
mkdir -p "$OUT"
errors=0

for spec in \
  'vehicle-forge-x:vehicle-forge' \
  'Patternx:patternx-astrology' \
  'gpu-efficiency-lab-android:gpu-wattlab' \
  'net-intel-app-v9:netintel'; do
  app="${spec%%:*}"
  slug="${spec#*:}"
  repo="damunozx123-debug/$app"
  keydir="$ROOT/damunozx123-debug_$app"
  echo "== $app =="
  if [[ ! -s "$keydir/release.p12" || ! -s "$keydir/release-password.txt" ]]; then
    echo 'Missing original local key and/or password; cannot verify update identity.'
    errors=$((errors + 1))
    continue
  fi
  run_id="$(gh run list -R "$repo" \
    --workflow signed-release.yml --branch main --event push --status success \
    --limit 10 --json databaseId --jq '.[0].databaseId' || true)"
  if [[ ! "$run_id" =~ ^[0-9]+$ ]]; then
    echo 'No successful signed-verification workflow run was found.'
    errors=$((errors + 1))
    continue
  fi
  dir="$OUT/$app-$run_id"
  if [[ -e "$dir" ]]; then
    echo "Destination exists. Refusing to overwrite: $dir"
    errors=$((errors + 1))
    continue
  fi
  mkdir -p "$dir"
  if ! gh run download "$run_id" -R "$repo" -n "$slug-signed-verification" -D "$dir"; then
    echo 'Could not retrieve signed artifact (check run status/7-day expiration).'
    errors=$((errors + 1))
    continue
  fi
  if [[ ! -s "$dir/$slug.apk" || ! -s "$dir/SHA256SUMS.txt" ||
        ! -s "$dir/SIGNING_CERTIFICATE.txt" ]]; then
    echo 'Signed APK, checksum or signer-certificate evidence is missing.'
    errors=$((errors + 1))
    continue
  fi
  if ! (cd "$dir" && sha256sum --check SHA256SUMS.txt); then
    echo 'Signed APK checksum is invalid.'
    errors=$((errors + 1))
    continue
  fi
  KEY_PW="$(cat "$keydir/release-password.txt")"
  export KEY_PW
  local_fp="$(keytool -list -v -storetype PKCS12 -keystore "$keydir/release.p12" \
    -storepass:env KEY_PW 2>/dev/null | awk '/SHA256:/{sub(/^.*SHA256:[[:space:]]*/, ""); print; exit}' | tr -d ':' | tr '[:upper:]' '[:lower:]')"
  unset KEY_PW
  artifact_fp="$(awk '/certificate SHA-256 digest:/{print $NF; exit}' "$dir/SIGNING_CERTIFICATE.txt" \
    | tr -d ':' | tr '[:upper:]' '[:lower:]')"
  if [[ -z "$local_fp" || -z "$artifact_fp" || "$local_fp" != "$artifact_fp" ]]; then
    echo 'Certificate mismatch: APK is not signed by the original local signing identity.'
    errors=$((errors + 1))
    continue
  fi
  echo "Certificate identity MATCHES your original keystore."
  echo "Candidate: $dir/$slug.apk"
done

if (( errors > 0 )); then
  echo "Verification incomplete: $errors app(s) failed. DO NOT publish those versions."
  exit 1
fi
echo 'All 4 candidate archives and original signing identities verified.'
echo 'Still required: real-device acceptance and off-device backup before GitHub release.'
