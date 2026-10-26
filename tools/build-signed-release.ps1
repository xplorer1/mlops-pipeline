$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$portableJava = Join-Path $root '.tools\temurin21\jdk-21.0.12.1+1'
if (-not (Test-Path -LiteralPath "$portableJava\bin\java.exe")) { throw "Portable JDK not found: $portableJava" }
# Both Gradle and Android's apksigner.bat consult JAVA_HOME. Override the stale
# machine value for this process only; the user's global environment is untouched.
$env:JAVA_HOME = $portableJava

if (-not $env:BOOMTRACK_KEYSTORE) { $env:BOOMTRACK_KEYSTORE = Join-Path $env:USERPROFILE '.boomtrack\boomtrack-release.jks' }
if (-not $env:BOOMTRACK_KEY_ALIAS) { $env:BOOMTRACK_KEY_ALIAS = 'boomtrack' }
if (-not (Test-Path -LiteralPath $env:BOOMTRACK_KEYSTORE)) { throw "Release key not found: $env:BOOMTRACK_KEYSTORE" }

function Read-PlainSecret([string]$Prompt) {
    $secure = Read-Host $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer); $secure.Dispose() }
}

try {
    if (-not $env:BOOMTRACK_STORE_PASSWORD) { $env:BOOMTRACK_STORE_PASSWORD = Read-PlainSecret 'Keystore password' }
    if (-not $env:BOOMTRACK_KEY_PASSWORD) { $env:BOOMTRACK_KEY_PASSWORD = Read-PlainSecret 'Key password (often the same)' }
    # Cleaning is not required for correctness and causes intermittent Windows
    # locks on Gradle's immutable dependency cache. Release compilation still
    # recompiles every input whose content changed.
    & "$PSScriptRoot\build.cmd" :app:assembleRelease --console=plain --no-daemon
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    $apksigner = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Recurse -Filter apksigner.bat |
        Sort-Object FullName -Descending | Select-Object -First 1
    if (-not $apksigner) { throw 'apksigner was not found in the Android SDK.' }
    $apk = Join-Path $root 'app\build\outputs\apk\release\app-release.apk'
    & $apksigner.FullName verify --verbose --print-certs $apk
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Output "SIGNED_APK=$apk"
} finally {
    Remove-Item Env:BOOMTRACK_STORE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:BOOMTRACK_KEY_PASSWORD -ErrorAction SilentlyContinue
}
