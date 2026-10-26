@echo off
setlocal
call "%~dp0reference-env.cmd"
adb -s %TTPOD_SERIAL% root
if errorlevel 1 exit /b 1
adb -s %TTPOD_SERIAL% wait-for-device
adb -s %TTPOD_SERIAL% shell "if iptables -C OUTPUT ! -o lo -j REJECT 2>/dev/null; then iptables -D OUTPUT ! -o lo -j REJECT; fi"
if errorlevel 1 exit /b 1
adb -s %TTPOD_SERIAL% shell "if ip6tables -C OUTPUT ! -o lo -j REJECT 2>/dev/null; then ip6tables -D OUTPUT ! -o lo -j REJECT; fi"
if errorlevel 1 exit /b 1
adb -s %TTPOD_SERIAL% shell settings put global airplane_mode_on 0
adb -s %TTPOD_SERIAL% shell am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false
adb -s %TTPOD_SERIAL% shell svc wifi enable
if errorlevel 1 exit /b 1
echo Internet enabled for the reference emulator. Old TTPod services may still be unavailable.
