# Implementation feature matrix

Updated 2026-10-21. Implemented does not mean release accepted.
Reference evidence: [REFERENCE_VERSION](REFERENCE_VERSION.md).
Test evidence: [M1 acceptance](acceptance/M1.md).

| ID | Feature / evidence label | Milestone | Implementation / remaining acceptance |
| --- | --- | --- | --- |
| REF | 8.4.1 identity, observed/user-confirmed | M0 | Baseline captured; identity decision and queue observation pending |
| UI | Direct 8.4.1 screen reconstruction, observed/reconstructed | M2 | Main screens aligned at measured anchors; screenshots compared; secondary destinations and drawer implemented; exact art and full fidelity acceptance remain |
| BUILD | CLI project, reconstruction | M1 | Debug/release/lint passed; API 23 installed/tested; API 37 blocked by system graphics crash |
| LIB | Scan/folders/picker, platform adaptation | M1-3 | MediaStore scan, multi-file selection and recursive user-granted folder indexing implemented; successful rescans preserve organization for absent tracks; grant revocation, relink and scale acceptance remain |
| PLAY | Service/session/queue, reconstruction | M1-2 | Playback/background/checkpoint tested; queue reorder implemented; single-track force-stop recovery verified paused; broader lifecycle cases remain |
| LRC | Parser/import/timeline, reconstruction | M2 | Parser/import preservation tested; real SAF picker and highlighted display verified; bounded discovery in a user-selected sidecar tree implemented; manual scroll/seek remains |
| ART | Embedded art/original fallback, reconstruction | M2 | Bounded async decode implemented; artwork fixtures remain |
| ORG | Playlists/favorites/artists/albums/search | M3 | Room-backed playlists with duplicate ordered entries, rename/reorder/removal, favorites/search/history and category drill-down implemented; sorting and 10k scale remain |
| SKIN | Classic/alternative/importer | M4 | Two built-ins plus constrained staged ZIP import with rollback passed on API 23; richer slots, preview/removal and fidelity acceptance remain |
| DSP | PCM equalizer, reconstruction | M4 | Ten-band software presets and bit-exact bypass use the proven Media3 PCM path; response/headroom/listening/power acceptance remain |
| VIS | Stage modes, reconstruction | M4/M6 | Live PCM waveform implemented; agreed identity set, FFT spectrum and fidelity acceptance remain |
| ONLINE | Lyrics/skins versus music providers | Split | Explicit LRCLIB lookup remains; authorized skin catalog still needs a source and hosting; online music streaming/download is deferred |
| DAILY | Widget/overlay/export/accessibility | M6 | Sleep timer, home-screen playback widget and JSON organization backup/restore implemented; floating overlay and full accessibility remain |
| RELEASE | Hardware/performance/signed upgrade | M7 | Open; development build only |
