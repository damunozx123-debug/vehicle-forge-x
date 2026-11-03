#!/usr/bin/env bash
# Retained for backwards compatibility. This repository already exists on GitHub.
# Old versions of this script attempted to create a duplicate BMW-only repository.
set -euo pipefail
exec bash "$(dirname "$0")/scripts/rename_and_publish.sh"
