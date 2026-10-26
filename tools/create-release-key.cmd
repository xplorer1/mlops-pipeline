@echo off
setlocal
if "%BOOMTRACK_KEYSTORE%"=="" set "BOOMTRACK_KEYSTORE=%USERPROFILE%\.boomtrack\boomtrack-release.jks"
if "%BOOMTRACK_KEY_ALIAS%"=="" set "BOOMTRACK_KEY_ALIAS=boomtrack"
if defined TTPOD_JAVA_HOME (set "BOOMTRACK_JAVA_HOME=%TTPOD_JAVA_HOME%") else (set "BOOMTRACK_JAVA_HOME=%~dp0..\.tools\temurin21\jdk-21.0.12.1+1")
set "BOOMTRACK_KEYTOOL=%BOOMTRACK_JAVA_HOME%\bin\keytool.exe"
if not exist "%BOOMTRACK_KEYTOOL%" (
  echo keytool was not found. Set TTPOD_JAVA_HOME to an installed JDK.
  exit /b 1
)
if exist "%BOOMTRACK_KEYSTORE%" (
  echo Refusing to overwrite the existing key: %BOOMTRACK_KEYSTORE%
  exit /b 2
)
for %%I in ("%BOOMTRACK_KEYSTORE%") do if not exist "%%~dpI" mkdir "%%~dpI"
echo Creating the permanent boomtrack update key at:
echo %BOOMTRACK_KEYSTORE%
echo You will be prompted for passwords and certificate identity. Back up this JKS and its passwords.
"%BOOMTRACK_KEYTOOL%" -genkeypair -v -keystore "%BOOMTRACK_KEYSTORE%" -alias "%BOOMTRACK_KEY_ALIAS%" -keyalg RSA -keysize 4096 -validity 10000
exit /b %errorlevel%
