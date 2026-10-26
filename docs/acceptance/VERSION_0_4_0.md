# boomtrack 0.4.0 development acceptance

Updated 2026-10-26.

## Scope

boomtrack is a local-music-first player. Online music catalogue, streaming and audio
download providers are deferred. Explicit LRCLIB lookup remains an optional user action.

## Included

- MediaStore discovery for shared audio.
- Multi-file selection through Android's document picker.
- Recursive indexing of a user-selected folder and its subfolders, bounded to 10,000
  entries and backed by a persistent read grant.
- Metadata extraction without copying audio into app-private storage.
- Existing local playback, background session, queue, playlists, favorites, history,
  embedded artwork, local/sidecar lyrics, equalizer, waveform, widget and backup features.

## Evidence

- Debug build and Android-test compilation passed.
- Recreated the deleted API 23 AVD from the already-installed system image.
- API 23 passed seven application tests and four playback/equalizer tests.
- The device suite covers the Internet permission required by explicit lyric lookup.

## Open local-player gates

- Manually exercise folder selection with a real multi-format collection.
- Add explicit missing-file relinking rather than requiring restoration at the same URI.
- Validate format coverage, long screen-off playback and audio output on a physical phone.
- Complete accessibility cleanup and current-Android validation.
- Create an owner-controlled signing key before treating an APK as a durable release.
- Select or create an authorized skin catalog before enabling network skin installation;
  old TTPod skins are not assumed redistributable merely because they were free to users.
