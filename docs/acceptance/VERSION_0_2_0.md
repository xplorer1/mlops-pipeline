# 0.2.0 development increment

Verified 2026-10-21 on the recreated isolated `classic_api23` AVD (Android 6/API 23).
This is development evidence, not physical-device, current-Android or release acceptance.

## Implemented

- Room schema v3 with playlists, ordered duplicate entries and explicit v1-to-v2-to-v3 migrations.
- MediaStore missing-file reconciliation that preserves playlists and saved lyrics.
- Validated data-only skin ZIP import with staging, atomic activation and rollback.
- Ten-band PCM equalizer presets and bit-exact bypass in the Media3 audio sink.
- Lock-free PCM waveform page that stops redrawing when hidden.
- User-initiated LRCLIB exact lookup, confirmation and durable local lyric save.

## Automated evidence

- `:app:assembleDebug`, `:app:assembleRelease`, `:app:lintDebug`, both Android-test
  compilation tasks and `:domain:test` passed together.
- Six deterministic app tests passed on API 23. They cover launch/navigation,
  background playback/timer/history, lyric replacement, INTERNET declaration,
  a real v1-to-v3 migration, duplicate playlist entries, missing-file preservation,
  and valid/unsafe skin import rollback.
- Four playback tests passed on API 23. Unity bypass is bit-exact; an enabled EQ
  changes PCM; flush and 44.1/48 kHz mono/stereo configuration remain finite/usable.
- One separately invoked live LRCLIB test fetched the provider's documented record
  and parsed its returned lyrics. It is excluded from the deterministic runner so
  provider downtime cannot break local-player CI.
- A first runtime exposed an API 23 launch crash because the custom waveform view
  lacked the XML constructor. The constructor was fixed and the complete device
  suites then passed.

## Open gates

- No claim of audible EQ quality, clipping/pop absence, acoustic equivalence, FFT
  spectrum fidelity, battery performance or physical-route behavior.
- Skin tests do not yet cover ZIP bombs with many large entries, corrupt PNG decode,
  case collision variants, update/removal UX or a preview-before-activation screen.
- LRCLIB proves only the explicit lyric path. Artwork, music search, streaming,
  downloads and their rights/provider evidence remain absent.
- API 37 still has the recorded emulator graphics failure. A physical phone and the
  full supported API/device matrix remain release gates.
