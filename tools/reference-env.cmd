@echo off
rem Called by other scripts; environment changes stay inside their setlocal.
set "TTPOD_ROOT=%~dp0.."
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
set "ANDROID_USER_HOME=%TTPOD_ROOT%\reference-private\android-user"
set "ANDROID_AVD_HOME=%TTPOD_ROOT%\reference-private\avds"
set "JAVA_HOME=%TTPOD_ROOT%\.tools\temurin21\jdk-21.0.12.1+1"
set "DEBUG="
set "PATH=%ANDROID_HOME%\platform-tools;%JAVA_HOME%\bin;%PATH%"
set "TTPOD_SERIAL=emulator-5580"
