@echo off
setlocal
cd /d "%~dp0.."
call tools\reference-env.cmd
set "CLASSIC_DEVICE=classic_api23"
set "CLASSIC_PORT=5584"
set "CLASSIC_GRAPHICS=-gpu swiftshader_indirect"
if "%~1"=="37" (
  set "CLASSIC_DEVICE=classic_api37"
  set "CLASSIC_PORT=5582"
  set "CLASSIC_GRAPHICS=-gpu swiftshader"
  echo API 37 currently has a system graphics crash on this host. See docs\acceptance\M1.md.
)
if not exist "%ANDROID_AVD_HOME%\%CLASSIC_DEVICE%.ini" (
  echo Development AVD missing. See docs\BUILD.md for setup.
  exit /b 1
)
adb -s emulator-%CLASSIC_PORT% get-state >nul 2>&1
if not errorlevel 1 exit /b 0
set "CLASSIC_EMULATOR=%ANDROID_HOME%\emulator\emulator.exe"
if "%CLASSIC_PORT%"=="5582" if exist "%CD%\.tools\stable-emulator\emulator\emulator.exe" set "CLASSIC_EMULATOR=%CD%\.tools\stable-emulator\emulator\emulator.exe"
start "Classic Player development" /b "%CLASSIC_EMULATOR%" -avd %CLASSIC_DEVICE% -port %CLASSIC_PORT% -no-snapshot %CLASSIC_GRAPHICS% -no-boot-anim -memory 2048 >"%TTPOD_ROOT%\reference-private\logs\%CLASSIC_DEVICE%.log" 2>&1
