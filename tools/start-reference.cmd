@echo off
setlocal
call "%~dp0reference-env.cmd"
if not exist "%ANDROID_AVD_HOME%\ttpod_reference9.ini" (
  echo Reference device has not been created. See docs\RUN_REFERENCE.md.
  exit /b 1
)
adb -s %TTPOD_SERIAL% get-state >nul 2>&1
if errorlevel 1 (
  start "TTPod reference emulator" /b "%ANDROID_HOME%\emulator\emulator.exe" -avd ttpod_reference9 -port 5580 -no-snapshot -gpu swiftshader_indirect -no-boot-anim >"%TTPOD_ROOT%\reference-private\logs\emulator.log" 2>&1
)
echo Waiting for the reference emulator to boot...
for /l %%n in (1,1,120) do (
  adb -s %TTPOD_SERIAL% shell getprop sys.boot_completed 2>nul | findstr /x "1" >nul
  if not errorlevel 1 (
    adb -s %TTPOD_SERIAL% shell getprop vold.decrypt 2>nul | findstr "trigger_restart_min_framework" >nul
    if errorlevel 1 goto booted
  )
  powershell.exe -NoProfile -Command "Start-Sleep -Seconds 2"
)
echo Boot timed out. See reference-private\logs\emulator.log.
exit /b 1
:booted
if /i "%~1"=="online" goto enable_online
call "%~dp0reference-offline.cmd"
goto network_ready
:enable_online
call "%~dp0reference-online.cmd"
:network_ready
if errorlevel 1 exit /b 1
adb -s %TTPOD_SERIAL% shell input keyevent 82
adb -s %TTPOD_SERIAL% shell monkey -p com.sds.android.ttpod -c android.intent.category.LAUNCHER 1
if errorlevel 1 (
  echo Emulator is ready but TTPod could not be opened. Install the APK as described in docs\RUN_REFERENCE.md.
  exit /b 1
)
echo Reference emulator ready. Default launch is offline; the online argument enables internet.
