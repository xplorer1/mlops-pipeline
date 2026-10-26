@echo off
setlocal
cd /d "%~dp0.."
set "CLASSIC_SERIAL=%~1"
if "%CLASSIC_SERIAL%"=="" set "CLASSIC_SERIAL=emulator-5584"
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
for /f %%a in ('"%ADB%" -s %CLASSIC_SERIAL% shell getprop ro.build.version.sdk') do set "CLASSIC_API=%%a"
if not defined CLASSIC_API exit /b 1
"%ADB%" -s %CLASSIC_SERIAL% install -r app\build\outputs\apk\debug\app-debug.apk
if errorlevel 1 exit /b 1
"%ADB%" -s %CLASSIC_SERIAL% install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
if errorlevel 1 exit /b 1
if %CLASSIC_API% GEQ 33 (
  "%ADB%" -s %CLASSIC_SERIAL% shell pm grant org.ttpod.recreation.dev android.permission.READ_MEDIA_AUDIO
) else (
  "%ADB%" -s %CLASSIC_SERIAL% shell pm grant org.ttpod.recreation.dev android.permission.READ_EXTERNAL_STORAGE
)
if errorlevel 1 exit /b 1
call tools\seed-device.cmd %CLASSIC_SERIAL%
if errorlevel 1 exit /b 1
"%ADB%" -s %CLASSIC_SERIAL% shell am instrument -w -r -e class org.ttpod.recreation.PlayerIntegrationTest org.ttpod.recreation.dev.test/androidx.test.runner.AndroidJUnitRunner >"reference-private\tests-%CLASSIC_SERIAL%-app.txt" 2>&1
type "reference-private\tests-%CLASSIC_SERIAL%-app.txt"
findstr /c:"OK (8 tests)" "reference-private\tests-%CLASSIC_SERIAL%-app.txt" >nul
if errorlevel 1 exit /b 1
rem Test-only APKs hold no user data. Remove an older differently signed copy so
rem a stale test binary cannot be mistaken for the just-built suite.
"%ADB%" -s %CLASSIC_SERIAL% uninstall org.ttpod.recreation.playback.test >nul 2>&1
"%ADB%" -s %CLASSIC_SERIAL% install playback\build\outputs\apk\androidTest\debug\playback-debug-androidTest.apk
if errorlevel 1 exit /b 1
"%ADB%" -s %CLASSIC_SERIAL% shell am instrument -w -r -e package org.ttpod.recreation.playback org.ttpod.recreation.playback.test/androidx.test.runner.AndroidJUnitRunner >"reference-private\tests-%CLASSIC_SERIAL%-pcm.txt" 2>&1
type "reference-private\tests-%CLASSIC_SERIAL%-pcm.txt"
findstr /c:"OK (4 tests)" "reference-private\tests-%CLASSIC_SERIAL%-pcm.txt" >nul
if errorlevel 1 exit /b 1
exit /b 0
