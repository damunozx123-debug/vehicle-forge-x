#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
kotlinc app/src/main/java/com/dan/bmwforgex/core/*.kt core_smoke_test.kt coding_smoke_test.kt bimmer_report_smoke_test.kt -include-runtime -d core-smoke.jar
java -jar core-smoke.jar
