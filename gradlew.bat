@echo off
powershell -ExecutionPolicy Bypass -File "%~dp0BUILD_WINDOWS.ps1" %*
exit /b %errorlevel%
