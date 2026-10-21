#!/usr/bin/env bash
# Run with `bash PUBLISH_GITHUB.sh [--public]` (private by default).
set -euo pipefail
cd "$(dirname "$0")"
command -v gh >/dev/null || { echo 'Install GitHub CLI (gh) first.' >&2; exit 1; }
command -v git >/dev/null || { echo 'Install Git first.' >&2; exit 1; }
if ! gh auth status >/dev/null 2>&1; then gh auth login; fi
VISIBILITY=--private
case "${1:-}" in
  '') ;;
  --public) VISIBILITY=--public ;;
  *) echo 'Usage: bash PUBLISH_GITHUB.sh [--public]' >&2; exit 2 ;;
esac
if [ -n "$(git status --porcelain)" ]; then
  echo 'Working tree is dirty. Review uncommitted changes before publishing.' >&2
  exit 1
fi
gh repo create BMW-Forge-X "$VISIBILITY" --source . --remote origin --push
