@echo off
setlocal
call "%~dp0reference-env.cmd"
adb -s %TTPOD_SERIAL% emu kill
