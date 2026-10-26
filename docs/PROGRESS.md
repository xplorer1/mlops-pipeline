# Implementation progress

Updated 2026-10-21. Work is authorized by the instruction to proceed.
The approved plan and review hashes remain unchanged.

## Implemented development foundation

- Three-module Kotlin project, API 23 minimum / API 37 target, CMD build tools.
- Local audio scan/file picker and songs/folders UI.
- Media3 service/session, queue checkpoint and paused recovery implementation.
- Room library, saved lyric bytes and per-song delay.
- LRC parser, import/replacement and active-line display.
- MY dashboard, Local music, mini-player, full player and bottom queue rebuilt
  from the 8.4.1 reference. See [visual evidence](acceptance/UI_FIDELITY.md).
- Original vector controls, bounded embedded-art loading, artwork/lyric swipes.
- Basic favorite membership, title/artist search, group selection, queue append,
  play-next and removal. These do not complete the planned library milestone.
- Dedicated search, favorites, recent history, scan, settings and timer pages;
  blue drawer and inline artist/album/folder navigation. See
  [secondary-page evidence](acceptance/NAVIGATION.md) and [route map](NAVIGATION.md).
- Service-recorded listening history and service-enforced preset/custom sleep timer.
- Queue entries can be reordered in the queue sheet; the service checkpoint preserves
  the resulting order. A second built-in player skin is selectable and persisted.
- Room-backed playlists support creation, duplicate ordered entries, playback and deletion.
  The explicit v1-to-v3 migration preserves existing tracks, lyrics and queue state.
- Successful MediaStore rescans mark missing files unavailable without deleting their
  playlist membership or lyric selection.
- Constrained data-only skin ZIP import uses staged validation and rollback. Packages
  support a manifest, colors and an optional bounded PNG background.
- A ten-band software PCM equalizer provides eight presets and unity bypass. The third
  player page renders a live waveform from the same lock-free PCM path.
- Explicit LRCLIB lookup can save a confirmed synchronized/plain result for offline use.
  Local launch and playback never initiate provider requests.
- Private-use branding now identifies the app as `boomtrack` while retaining the
  development application ID so existing installs upgrade in place.
- Playlists can be renamed and their entries can be moved or removed without losing
  duplicate ordering semantics.
- A user-selected lyrics folder supports bounded sidecar LRC discovery and offline import.
- A home-screen widget exposes the current track plus play/pause and next controls through
  the existing media session.
- JSON backup/export restores playlists, favorites, selected skin and equalizer preset;
  media files remain external and missing URIs are reported during restore.
- A user-granted music folder can now be indexed recursively without copying the audio;
  its durable document grant supports later playback and rescanning.
- The active 0.4 scope is local-music-first. Explicit LRCLIB lookup remains available;
  online music streaming/download is deferred. A downloadable skin catalog still needs
  original or redistribution-authorized packages and a hosting source.

## Verified

- Seven domain tests passed.
- Android 6/API 23: the previous six deterministic app integration tests, four PCM/EQ tests and one
  explicit live LRCLIB provider test passed. The recreated AVD caught and verified the
  fix for an API 23 custom-view inflation crash.
- Debug and optimized unsigned release builds passed.
- Latest UI build: zero lint errors, 167 warnings (localization, accessibility,
  style and dependency notices remain). Debug and optimized release compile.
- Latest UI regression: seven app and four PCM/EQ device tests passed on API 23;
  coverage includes scan/navigation, migration, playlists, missing files, skin rollback,
  persisted listening history, timer behavior and equalizer processing.
- The expanded suite includes passing backup/restore and playlist-edit assertions.
- Manual UI checks passed for page swipe, saved favorites and queue append/remove.
- Real system-picker LRC import, highlighted lyric display and paused recovery
  after force-stop/relaunch verified manually on API 23.
- Android 17/API 37 is blocked by a system graphics crash before app install.

## Open gates

G0: proposed full identity set awaits user selection. Reference home/library/player
and basic queue states were captured; deeper behavior remains incomplete.
See [IDENTITY_SET](IDENTITY_SET.md).

G1: current-platform device verification is blocked; broader SAF grant/revocation
checks and model review remain. No milestone gate is claimed passed.

G2: lyric scroll/seek,
layout/fidelity/accessibility, physical listening, format/lifecycle and long
screen-off checks remain. This build is a foundation demo, not an accepted alpha.

M3-M7 remain incomplete. Playlist editing, missing-file state, skin import, EQ/waveform,
sidecar discovery, a widget and organization backup now exist. Sorting,
scale/relink, skin format depth, measured audio quality, the agreed visualizer identity set,
floating lyrics, accessibility, device coverage and release signing remain. Online artwork,
music providers and downloads are deferred product scope rather than release blockers.

See [M1 evidence](acceptance/M1.md) for actual checks and limitations.
