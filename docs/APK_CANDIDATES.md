# TTPod APK candidates

Checked 2026-09-27. Archive publication dates are not treated as software release dates.
APKs and reference captures remain private and gitignored.

## Current candidate: 8.4.1

- Source page: https://www.cr173.com/soft/1620162.html
- Published file link: https://cz.197942.com/cr10/com.sds.android.ttpod.apk
- Local file: `reference-private/apks/cr173/ttpod-8.4.1.apk`
- Size: 16,485,515 bytes.
- Package: `com.sds.android.ttpod`; version name 8.4.1; version code 8410.
- minSdk 8; targetSdk 22; native ABI armeabi; application label TTPod.
- File SHA-256: `5c8450a5add1d81cdbc725c2c5c25fe8651e21a02aee80d552299e20ae125fe5`.
- `apksigner verify --print-certs` passes.
- Signer SHA-256: `ef36b110e2ef5f53981932bb44e82c160409525d1d4d18ebdacd772e1711daab`.
- Matches the signer of the original 9.0.4 and retained 10.0.7 files. This establishes
  signature continuity, not a malware audit or independent publisher verification.
- Installed and launched in the offline Android 9 reference emulator.
- Main screen is mostly English, with some Chinese labels/promotional text.
- User recognized this as the remembered interface and subsequently confirmed
  audible music playback. It is the selected reference for the recreation plan.

## Downloaded but not installed: 8.4.0

- Source page: https://www.pgyer.com/apk/apk/com.sds.android.ttpod
- Published file link: https://storage.appmeme.com/com.sds.android.ttpod--8400.apk
- Local file: `reference-private/apks/pgyer/ttpod-8.4.0.apk`
- Size: 16,492,023 bytes.
- Package: `com.sds.android.ttpod`; version name 8.4.0; version code 8400.
- minSdk 8; targetSdk 22; native ABI armeabi.
- File SHA-256: `0e052c0fe56b4c97c0e743f95f2f411ecbe59eb2ef538a8c3ac9d3944e64e595`.
- Signature verifies, but its signer differs from the original files.
- Signer SHA-256: `a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc`.
- Certificate subject identifies Android / android@android.com rather than TTPod.
- Treat as an unverified/re-signed candidate, not an authoritative original reference.

## Removed: 9.0.4

The user explicitly rejected the Alibaba Planet interface and authorized deletion.
Uninstalled `com.sds.android.ttpod` from emulator-5580 and deleted only the root
`ttpod.apk` after checking its known SHA-256. Historical screenshots and inspection
notes remain. The original `ttpod-10-0-7.apk` is retained, untested at runtime.

## Other attempted archives

No usable 6.x/7.x APK was acquired in this pass. Uptodown URLs returned 404;
APKFiles' 7.9.2 file link returned 429; ApkBe's historical download pages require
CAPTCHA. Several other mirrors had missing files, inaccessible hosts, or redirects.
The browser helper could not initialize in this session; downloads were performed
from inspected archive links using curl. No CAPTCHA was bypassed.
