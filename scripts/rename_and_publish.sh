#!/usr/bin/env bash
# One-time publication of Vehicle Forge X using the user's own GitHub CLI session.
# This script changes repository settings, never vehicle or phone settings.
set -euo pipefail

OLD="damunozx123-debug/Bmw-forge-x"
NEW="damunozx123-debug/vehicle-forge-x"
command -v gh >/dev/null || { echo "Install and authenticate GitHub CLI: pkg install gh git; gh auth login" >&2; exit 1; }
gh auth status >/dev/null || { echo "Run gh auth login first." >&2; exit 1; }

if gh repo view "$NEW" --json nameWithOwner >/dev/null 2>&1; then
  TARGET="$NEW"
elif gh repo view "$OLD" --json nameWithOwner >/dev/null 2>&1; then
  TARGET="$OLD"
else
  echo "Neither expected repository is accessible. Refusing to change anything." >&2
  exit 1
fi

# Release only if the most recent code build/test run passed. This checks
# software build status, not physical-vehicle coding compatibility.
STATUS="$(gh run list -R "$TARGET" --workflow verify.yml --limit 1 --json conclusion,status --jq '.[0] | if .status == "completed" then .conclusion else .status end')"
if [[ "$STATUS" != "success" ]]; then
  echo "Latest Android/protocol CI is not successful ($STATUS). Refusing publication." >&2
  exit 1
fi

echo "Publishing $TARGET as $NEW. The entire git history, Actions logs and code will become public."
echo "Ensure no real VINs, API credentials, customer data or confidential code exist in repository history."
echo "Type PUBLISH to confirm you have reviewed the historical content:"
read -r ANSWER
[[ "$ANSWER" == "PUBLISH" ]] || { echo "Cancelled; repository remains unchanged."; exit 1; }

if [[ "$TARGET" == "$OLD" ]]; then
  gh repo rename -R "$OLD" vehicle-forge-x --yes
fi

gh repo edit "$NEW" \
  --visibility public \
  --accept-visibility-change-consequences \
  --description "Vehicle Forge X: open-source multi-brand OBD-II diagnostics, safe coding plans and offline simulations"

gh repo view "$NEW" --json nameWithOwner,visibility --jq '"Published: " + .nameWithOwner + " (" + .visibility + ")"'
echo "If you cloned the old repo, run: git remote set-url origin https://github.com/$NEW.git"
