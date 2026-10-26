@echo off
setlocal
cd /d "%~dp0.."
set "DEBUG="
if defined TTPOD_JAVA_HOME (set "JAVA_HOME=%TTPOD_JAVA_HOME%") else (set "JAVA_HOME=%CD%\.tools\temurin21\jdk-21.0.12.1+1")
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo Set TTPOD_JAVA_HOME to an installed compatible JDK.
  exit /b 1
)
set "GRADLE_USER_HOME=%CD%\.gradle-user"
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
if "%~1"=="" (call gradlew.bat :domain:test :app:assembleDebug --console=plain) else (call gradlew.bat %*)
exit /b %errorlevel%
