@echo off
setlocal
call "%~dp0reference-env.cmd"
if exist "%ANDROID_AVD_HOME%\ttpod_reference9.ini" (
  echo Reference device already exists; leaving its data intact.
  exit /b 0
)
if not exist "%ANDROID_AVD_HOME%" mkdir "%ANDROID_AVD_HOME%"
if not exist "%ANDROID_USER_HOME%" mkdir "%ANDROID_USER_HOME%"
echo no | call "%ANDROID_HOME%\cmdline-tools\latest\bin\avdmanager.bat" create avd -n ttpod_reference9 -k "system-images;android-28;google_apis;x86" -p "%ANDROID_AVD_HOME%\ttpod_reference9.avd" --device "Nexus 5" --sdcard 512M
exit /b %errorlevel%
