# Durable release signing

Android updates must be signed by the same private key forever. Losing this key or its passwords means future builds cannot update the installed app; a new app identity and reinstall would be required.

1. Open CMD at the repository root and run `tools\create-release-key.cmd` once.
2. Back up `%USERPROFILE%\.boomtrack\boomtrack-release.jks` and the passwords in two separate, secure places. Never add them to this repository or cloud-sync them unencrypted.
3. Run `tools\build-signed-release.cmd`. It securely prompts for the keystore and key passwords; they are not echoed or stored by the script. The default alias is `boomtrack`; override `BOOMTRACK_KEY_ALIAS` only if needed.
4. The command performs a clean release build and verifies the APK certificate. The output is `app\build\outputs\apk\release\app-release.apk`.

`BOOMTRACK_KEYSTORE` may point to a different JKS. Ordinary release builds remain unsigned when the required environment variables are absent, preventing accidental signing with a disposable key.
