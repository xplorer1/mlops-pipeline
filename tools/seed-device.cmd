@echo off
setlocal
cd /d "%~dp0.."
set "CLASSIC_SERIAL=%~1"
if "%CLASSIC_SERIAL%"=="" set "CLASSIC_SERIAL=emulator-5584"
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
python tools\generate-fixtures.py
if errorlevel 1 exit /b 1
"%ADB%" -s %CLASSIC_SERIAL% shell mkdir -p /sdcard/Music/ClassicPlayer /sdcard/Download
if errorlevel 1 exit /b 1
"%ADB%" -s %CLASSIC_SERIAL% push reference-private\media\TwinkleDemo.wav /sdcard/Music/ClassicPlayer/TwinkleDemo.wav
if errorlevel 1 exit /b 1
"%ADB%" -s %CLASSIC_SERIAL% push reference-private\media\TwinkleDemo.lrc /sdcard/Download/TwinkleDemo.lrc
if errorlevel 1 exit /b 1
"%ADB%" -s %CLASSIC_SERIAL% shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/ClassicPlayer/TwinkleDemo.wav
if exist "reference-private\media\Edward M. Favor - Daisy Bell.ogg" (
  "%ADB%" -s %CLASSIC_SERIAL% push "reference-private\media\Edward M. Favor - Daisy Bell.ogg" "/sdcard/Music/ClassicPlayer/DaisyBell.ogg"
  if errorlevel 1 exit /b 1
  "%ADB%" -s %CLASSIC_SERIAL% shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/ClassicPlayer/DaisyBell.ogg
)
exit /b %errorlevel%
