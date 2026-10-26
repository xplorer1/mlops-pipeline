@echo off
setlocal
call "%~dp0reference-env.cmd"
adb -s %TTPOD_SERIAL% root
if errorlevel 1 exit /b 1
adb -s %TTPOD_SERIAL% wait-for-device
adb -s %TTPOD_SERIAL% shell "iptables -C OUTPUT ! -o lo -j REJECT 2>/dev/null || iptables -I OUTPUT ! -o lo -j REJECT"
if errorlevel 1 exit /b 1
adb -s %TTPOD_SERIAL% shell "ip6tables -C OUTPUT ! -o lo -j REJECT 2>/dev/null || ip6tables -I OUTPUT ! -o lo -j REJECT"
if errorlevel 1 exit /b 1
adb -s %TTPOD_SERIAL% shell settings put global airplane_mode_on 1
adb -s %TTPOD_SERIAL% shell am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true
adb -s %TTPOD_SERIAL% shell svc wifi disable
adb -s %TTPOD_SERIAL% shell svc data disable
exit /b %errorlevel%
