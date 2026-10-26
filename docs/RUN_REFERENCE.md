# Running the TTPod reference from CMD

Current installed candidate: **8.4.1**. The user rejected 9.0.4; its root
`ttpod.apk` file and emulator installation were removed on 2026-09-27.
See [APK_CANDIDATES.md](APK_CANDIDATES.md) for downloads and signature checks.

This uses the existing Android SDK, not Android Studio. The dedicated device is
Android 9 / API 28, Google APIs x86 revision 12, with ARM translation. This is a
compatibility environment, not evidence of exact Android 5/6 behavior.

Google documents ARM translation on Android 9 x86 images:
https://developer.android.com/studio/releases/emulator#3000-february-19-2020

## Start and stop

Open CMD:

```bat
cd /d C:\Users\STUDENT\projects\ttpod
tools\start-reference.cmd
```

The script opens the emulator, waits for boot, blocks guest IPv4/IPv6 outbound
traffic except loopback, disables Wi-Fi/mobile data, and opens installed TTPod.
No personal account is needed. The blocking rules apply inside this virtual phone.

To start with internet enabled instead:

```bat
tools\start-reference.cmd online
```

For an already running emulator, use `tools\reference-online.cmd` or
`tools\reference-offline.cmd` to switch connectivity. Android reported a validated
Wi-Fi internet connection after enabling it on 2026-09-27. That does not guarantee
the old TTPod services still work: online lyric requests returned network errors
and connection timeouts, and Change skin remained on its loading screen.

## Test music

Test media is stored locally in `reference-private\media` and on the emulator in
`/sdcard/Music/TTPodReference/`. Daisy Bell (Edward M. Favor, 1894) played
successfully: its timeline advanced and Android reported an active MediaPlayer.
Source: https://commons.wikimedia.org/wiki/File:Daisy_Bell_sung_by_Edward_M._Favor.ogg

`TwinkleDemo.wav` is a generated instrumental melody, with a matching local
`TwinkleDemo.lrc` lyric guide. It is test content, not a commercial recording.
The app detected and parsed the local lyric file, but its player still displayed
"Good TTPod" instead of the lines. Synchronized lyric display is unresolved;
the screenshot named `ttpod-8.4.1-local-lyrics.png` records that failed display.
To play either file directly from CMD:

```bat
call tools\reference-env.cmd
adb -s emulator-5580 shell am start -a android.intent.action.VIEW -d file:///sdcard/Music/TTPodReference/DaisyBell.ogg -t audio/ogg -p com.sds.android.ttpod
adb -s emulator-5580 shell am start -a android.intent.action.VIEW -d file:///sdcard/Music/TTPodReference/TwinkleDemo.wav -t audio/wav -p com.sds.android.ttpod
```

To add your own tracks, push them into that folder, then use Local music >
three-dot menu > Scan Media > Fast scan > Complete.

Stop it:

```bat
tools\stop-reference.cmd
```

The device data persists under `reference-private\avds`. Snapshots are disabled.
Logs and reference screenshots also stay under gitignored `reference-private`.

## Environment

`tools\reference-env.cmd` sets process-local paths. The installed Java is
`C:\Program Files\Java\jdk-20`; the pre-existing system JAVA_HOME points to a
missing jdk-17. These scripts do not change global environment variables.

For manual ADB commands in CMD:

```bat
call tools\reference-env.cmd
adb -s emulator-5580 devices
adb -s emulator-5580 shell getprop ro.product.cpu.abilist
```

## Recreate the device if absent

Requires the installed `system-images;android-28;google_apis;x86` SDK package:

```bat
tools\create-reference.cmd
```

This command refuses to overwrite an existing device.

To install the verified 8.4.1 APK after starting the offline emulator:

```bat
call tools\reference-env.cmd
adb -s emulator-5580 install -r reference-private\apks\cr173\ttpod-8.4.1.apk
adb -s emulator-5580 shell monkey -p com.sds.android.ttpod -c android.intent.category.LAUNCHER 1
```

Both supplied APKs use the same package name. Do not install 10.0.7 over 9.0.4
unless you intend to upgrade that device; use a separate virtual device for
side-by-side comparison.

## APK inspection, 2026-09-27

| File | Version | Target SDK | Native ABI | SHA-256 |
| --- | --- | --- | --- | --- |
| ttpod.apk | 9.0.4 | 21 | armeabi | 0b44c6441c6e74fc161179f54865bf47dd495ed05685a11473b785240aea8d29 |
| ttpod-10-0-7.apk | 10.0.7 | 22 | armeabi | 1b8363c0dddf60869d89802edc0ddd68c40e2db4ef6afe6053da3b3d4b8cfdb7 |

Both APKs pass `apksigner verify --print-certs` and share signer certificate
SHA-256 `ef36b110e2ef5f53981932bb44e82c160409525d1d4d18ebdacd772e1711daab`.
This establishes signature consistency, not independent publisher provenance.

## Verification

Verified on 2026-09-27:

- Emulator 35.6.11.0 runs the Android 9 reference device using WHPX.
- Guest ABI list: `x86,armeabi-v7a,armeabi`.
- Installed package reports version 9.0.4 and primary CPU ABI `armeabi`.
- Launch reaches `com.ali.music.entertainment.presentation.view.home.MainActivity`.
- Splash screen visibly says ALIBABA PLANET. The main interface has four Chinese
  bottom tabs; it is not yet established that this matches the desired classic UI.
- Guest IPv4 and IPv6 OUTPUT reject rules are installed, with loopback exempted.
- Screenshots: `reference-private/screenshots/ttpod-9.0.4-first-launch.png` and
  `ttpod-9.0.4-main.png`.

The first initialization was interrupted while switching from the sandbox to the
desktop session and showed an encryption failure. Only the new reference AVD was
reset, and the subsequent initialization completed. The launcher now also checks
that Android is not in its minimal encryption-recovery framework.

Subsequently, 9.0.4 was rejected and removed. The downloaded 8.4.1 installs and
reaches `com.sds.android.ttpod/.activities.MainActivity` offline. Its main screen
uses English labels such as Local music, My download, Recent play and My favorite,
with some remaining Chinese text. The screenshot is
`reference-private/screenshots/ttpod-8.4.1-first-launch.png`.

10.0.7 has been inspected but has not been installed or launched. Audio playback
is verified for 8.4.1 as described above; full feature parity is not established.
