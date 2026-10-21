# Run from PowerShell after installing GitHub CLI: winget install GitHub.cli
# Default visibility: PRIVATE. For public publishing: .\PUBLISH_GITHUB.ps1 -Public
param([switch]$Public)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { throw 'Install GitHub CLI first: winget install GitHub.cli' }
if (-not (Get-Command git -ErrorAction SilentlyContinue)) { throw 'Install Git first: winget install Git.Git' }
gh auth status 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) { gh auth login }
$visibility = if ($Public) { '--public' } else { '--private' }
if ((git status --porcelain).Length -gt 0) { throw 'Working tree has uncommitted changes. Review them before publishing.' }
gh repo create BMW-Forge-X $visibility --source . --remote origin --push
if ($LASTEXITCODE -ne 0) { throw 'GitHub publishing failed. Check whether the repository name exists or your permissions.' }
