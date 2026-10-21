# Execute on Windows with PowerShell: powershell -ExecutionPolicy Bypass -File .\BUILD_WINDOWS.ps1 assembleDebug
param([Parameter(ValueFromRemainingArguments=$true)][string[]]$GradleArgs)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $MyInvocation.MyCommand.Path
$cache = Join-Path $env:USERPROFILE '.gradle\forge-x-bootstrap'
$gradle = Join-Path $cache 'gradle-9.3.1\bin\gradle.bat'
if (!(Test-Path $gradle)) {
    New-Item -ItemType Directory -Force -Path $cache | Out-Null
    $archive = Join-Path $cache 'gradle.zip'
    Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-9.3.1-bin.zip' -OutFile $archive
    Expand-Archive -Path $archive -DestinationPath $cache -Force
    Remove-Item $archive
}
Push-Location $repo
try {
    if (!$GradleArgs -or $GradleArgs.Count -eq 0) { $GradleArgs = @('assembleDebug') }
    & $gradle @GradleArgs
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
} finally { Pop-Location }
