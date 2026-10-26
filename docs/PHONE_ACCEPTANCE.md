# Physical phone acceptance

These checks cannot be replaced by an emulator. They require the intended phone, a paired Bluetooth headset or speaker, and at least two real songs long enough for an extended test.

## Preparation

1. Build the signed APK using `tools\build-signed-release.cmd`.
2. Enable USB debugging, connect the phone, unlock it, and accept the computer authorization prompt.
3. Run `adb devices -l`. Use the physical device serial below; do not use an `emulator-*` serial.
4. Install with `adb -s PHONE_SERIAL install -r app\build\outputs\apk\release\app-release.apk`.
5. In boomtrack, choose the music folder or individual files and start a real song.

## Screen-off endurance gate

1. Record the starting title and playback position.
2. Turn the screen off and leave playback running for at least 30 minutes.
3. During the interval, verify audibly that tracks advance. After 30 minutes, turn the screen on.
4. Pass only if audio never stopped unexpectedly, notification controls still work, and boomtrack shows the correct current song and position.

## Bluetooth gate

1. Pair and select a Bluetooth headset or speaker before starting playback.
2. Verify play, pause, next, previous, and headset-disconnect behavior from the Bluetooth controls.
3. Switch output phone speaker to Bluetooth and back while playing.
4. Take a call or trigger another audio-focus owner, then end it. Pass only if boomtrack pauses or ducks and resumes sensibly without playing from the wrong output.

## Evidence

While playback is active, save these diagnostics from CMD:

```bat
adb -s PHONE_SERIAL shell getprop ro.build.version.release > phone-android-version.txt
adb -s PHONE_SERIAL shell dumpsys media_session > phone-media-session.txt
adb -s PHONE_SERIAL shell dumpsys audio > phone-audio-route.txt
adb -s PHONE_SERIAL shell dumpsys activity services org.ttpod.recreation.dev > phone-playback-service.txt
```

Also record phone model, Android version, song formats tested, start/end time, Bluetooth device, interruptions attempted, and any audible failure. A command log without audible confirmation is not a pass.
