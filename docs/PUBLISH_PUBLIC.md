# Publish Vehicle Forge X to GitHub (admin permissions required)

The connected ChatGPT GitHub integration can change repository files but **cannot rename repositories or change visibility**. This script performs those administrative actions using the GitHub CLI on your own device.

## Android Termux / Linux

```sh
pkg install gh git -y      # Termux only; Linux users install gh/git with their distro manager
gh auth login
gh repo clone damunozx123-debug/Bmw-forge-x
cd Bmw-forge-x
bash scripts/rename_and_publish.sh
```

The script refuses publication unless the latest `verify.yml` run completed successfully, then asks for a literal **PUBLISH** confirmation. It renames the remote to `vehicle-forge-x`, switches it to public, updates the description, and verifies the resulting visibility.

**Before typing PUBLISH:** examine all Git history, GitHub Actions logs, issues, any release assets and screenshots for customer or personal information, VINs, tokens, API keys and licensed databases. History remains public after changing visibility. Audit any old secrets and rotate compromised credentials before publishing.

Reference: [GitHub CLI rename](https://cli.github.com/manual/gh_repo_rename) and [visibility change](https://cli.github.com/manual/gh_repo_edit). `--accept-visibility-change-consequences` is required.

## Without a shell

On the GitHub repository page: **Settings → General → Repository name**: `vehicle-forge-x` → Rename. Then **Settings → General → Danger Zone → Change repository visibility → Public**, confirm the consequences.

The existing Android application ID remains `com.dan.bmwforgex` so the Java/Kotlin source package and existing installation data are not silently relocated during a cosmetic rename.
