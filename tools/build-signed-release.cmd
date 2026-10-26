@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-signed-release.ps1"
exit /b %errorlevel%
