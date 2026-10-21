#!/usr/bin/env bash
# Gradle launcher for distributable source snapshots where the wrapper JAR is unavailable.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
CACHE="${GRADLE_FORGE_CACHE:-$HOME/.gradle/forge-x-bootstrap}"
GRADLE="$CACHE/gradle-9.3.1/bin/gradle"
if [ ! -x "$GRADLE" ]; then
    command -v curl >/dev/null || { echo 'Install curl'; exit 1; }
    command -v unzip >/dev/null || { echo 'Install unzip'; exit 1; }
    mkdir -p "$CACHE"
    curl --fail --location --retry 2 --output "$CACHE/gradle.zip" \
      'https://services.gradle.org/distributions/gradle-9.3.1-bin.zip'
    unzip -q -o "$CACHE/gradle.zip" -d "$CACHE"
    rm "$CACHE/gradle.zip"
fi
cd "$HERE"
exec "$GRADLE" "$@"
