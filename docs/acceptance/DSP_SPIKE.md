# PCM integration spike

Scope: prove a supported insertion point for a future equalizer/visualizer.
This is not an equalizer implementation or an audio-quality certification.

## Implementation

`PlaybackService` supplies a `DefaultAudioSink` through a custom Media3
`DefaultRenderersFactory`. Its processor chain contains `PcmProbe`, a
`BaseAudioProcessor` that accepts PCM16 and passes samples unchanged. Offload is
disabled explicitly so local playback traverses the processor. The service dump
exposes a processed-byte counter for integration evidence.

## Verified

- Media3 1.11.1 compiles this integration in debug and optimized release builds.
- API 23 device tests check exact sample preservation, flush, 44.1/48 kHz format
  changes and an empty input drain. Both tests pass.
- Service playback of generated PCM WAV advances and continues with the Activity
  closed. An initial empty-buffer self-copy failure was reproduced and fixed by
  returning immediately when no bytes remain; a regression test now covers it.
- Manual final-build service dump records 749700 processed bytes while playing
  at 27730 ms; `reference-private/pcm-live.txt` proves the probe is in the actual
  playback path in addition to its isolated tests.

## Remaining risk and M4 boundary

- No EQ coefficients, FFT, gain staging, clipping protection or visualizer exist.
- Compressed formats, channel-count transitions and decoder route changes still
  need integration fixtures; a pass-through WAV is not codec coverage.
- Float/high-resolution paths must be explicitly handled or converted to the
  agreed processing format before EQ ships; do not silently bypass processing.
- Keep coefficient changes bounded and smooth; avoid allocation/locking on the
  audio thread. Rendering should consume a bounded copy and stop when hidden.
- Offload being disabled may increase power use. Measure physical-device CPU,
  underruns and 60-minute EQ-off/on runs before accepting beta performance.
- If the route cannot satisfy these tests, retain the working local-player
  foundation and keep G4 open; do not substitute a cosmetic equalizer or pretend
  the system equalizer is the specified deterministic ten-band implementation.
