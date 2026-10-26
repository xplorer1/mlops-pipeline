# Building and running the development app from CMD

This is the new independent application, not the private original TTPod APK.
The reviewed implementation plan is unchanged; see PROGRESS.md for actual gates.

## Requirements

- Android SDK with platform `platforms;android-37.0` and build-tools 36.0.0.
- A compatible JDK; this workspace uses its portable Eclipse Temurin JDK 21 LTS
  under `.tools` with Java/Kotlin target 17. Set `TTPOD_JAVA_HOME` to override it.
- Python 3 only for generating disposable test audio; no Python packages needed.

Gradle 9.6.0 wrapper is checksum pinned. AGP 9.4.0 supplies built-in Kotlin 2.2.10;
other versions are in `gradle/libs.versions.toml`. Downloads/cache stay in
`.gradle-user`, which is ignored. `local.properties` contains the local SDK path
and must not be shared. Neither global JAVA_HOME nor the reference AVD is changed.

## Build

```bat
cd /d C:\Users\STUDENT\projects\ttpod
tools\build.cmd
tools\build.cmd :app:assembleRelease :app:lintDebug --console=plain
```

Default command runs domain tests and builds the debug APK at
`app\build\outputs\apk\debug\app-debug.apk`. Release output is unsigned unless
the permanent signing environment is present; see `docs/RELEASE_SIGNING.md`.
An unsigned APK is not distributable.
Do not run two Gradle builds simultaneously against this checkout.

## Separate development devices

AVDs `classic_api37` (port 5582) and `classic_api23` (port 5584) live under
`reference-private\avds`. They are independent of `ttpod_reference9`.
To recreate a missing device after installing the corresponding SDK image:

```bat
call tools\reference-env.cmd
echo no | call "%ANDROID_HOME%\cmdline-tools\latest\bin\avdmanager.bat" create avd -n classic_api37 -k "system-images;android-37.0;google_apis;x86_64" -p "%ANDROID_AVD_HOME%\classic_api37.avd" --device "Nexus 5"
echo no | call "%ANDROID_HOME%\cmdline-tools\latest\bin\avdmanager.bat" create avd -n classic_api23 -k "system-images;android-23;default;x86_64" -p "%ANDROID_AVD_HOME%\classic_api23.avd" --device "Nexus 5"
```

Launch the verified API 23 demo with `tools\start-dev.cmd` (or an explicit `23`).
`tools\start-dev.cmd 37` is available for API 37 diagnosis, but that image
currently crashes inside SurfaceFlinger on this host before app installation.
Wait until Android is fully booted (first boot can take
several minutes). Stop the other emulator if memory is constrained.

The older installed emulator 35.6.11 hit a graphics assertion with API 37. This
workspace uses a separate stable 37.1.11 runtime under
`.tools\stable-emulator\emulator`, preferred by start-dev.cmd for API 37 only.
Use a current stable emulator for API 37; do not assume the reference runtime is
compatible. The original SDK emulator remains available for reference operation.

On this Windows host the API 37 image hit a SurfaceFlinger graphics assertion
even with the newer emulator. Disabling GLDMA alone did not resolve it. The dev
experiment disabling `GLDirectMem` and `HasSharedSlotsHostMemoryAllocator`
also failed. Runtime 37.3.1 preview reproduced the crash. No API 37 app tests
have passed; use API 23 for the current demo and keep G1 open.
Configure the new AVD's
`disk.dataPartition.size=4G` before its first boot: the generated 800 MB default
fills before our APK can install. Resetting an already-created data partition
requires a deliberate wipe of that disposable dev AVD, never the reference AVD.
Wait for both `sys.boot_completed=1` and a working package service before install.

For a local `sdk.dir` property use `C\:/...` with the drive colon escaped.
This workspace's lint cached the earlier malformed property until analysis was
rerun. `tools/build.cmd :app:lintDebug :app:assembleRelease --rerun-tasks` cleared
that stale result and passed; ordinary subsequent builds need no forced rerun.

```bat
call tools\reference-env.cmd
adb -s emulator-5584 shell getprop sys.boot_completed
adb -s emulator-5584 install -r app\build\outputs\apk\debug\app-debug.apk
tools\seed-device.cmd emulator-5584
adb -s emulator-5584 shell am start -n org.ttpod.recreation.dev/org.ttpod.recreation.MainActivity
```

In the app: Scan music → grant audio access → select TwinkleDemo → Lyrics →
Import LRC → Downloads → TwinkleDemo.lrc. The melody and cue file are generated
fixtures, not bundled production music. Lyrics are copied into the local DB;
bad imports preserve the previous selection. The delay controls adjust timing
by 0.5 seconds; long-press the delay label to reset.

On API 23 an ADB-pushed file may not appear in the Downloads shortcut. In the
picker choose More options > Show SD card, then the navigation drawer > Internal
storage > Download > TwinkleDemo.lrc. This uses the actual system document picker.

## Device tests

Seed the fixture first and grant the test app audio permission (or grant it
through the UI). On API 37 use READ_MEDIA_AUDIO; API 23 uses
READ_EXTERNAL_STORAGE. Explicitly select the target to avoid running against
the original reference device:

```bat
set ANDROID_SERIAL=emulator-5582
tools\build.cmd :app:connectedDebugAndroidTest :playback:connectedDebugAndroidTest --console=plain
```

The modern Gradle output collector mishandles the API 23 `am` command format.
Use the direct-ADB runner for that device (also supported for API 37):

```bat
tools\build.cmd :app:assembleDebug :app:assembleDebugAndroidTest :playback:assembleDebugAndroidTest --console=plain
tools\test-device.cmd emulator-5584
```

The direct runner installs/grants/seeds the isolated test device and saves output
under `reference-private\tests-<serial>-*.txt`; it checks JUnit success text
because `am instrument` can return shell success even when assertions fail.
Gradle reports, when used, appear under each module's
`build\reports\androidTests\connected`. Test output does not replace physical
listening, long screen-off, or release acceptance.

Android 16/API 36 is the current stable runtime gate on this host. Its AVD is
`classic_api36` on port 5586. The API 36 image exposed a MediaStore edge case
where an indexed audio row had a null duration; boomtrack now reads file
metadata as a fallback, and the full app/playback suites pass there.
