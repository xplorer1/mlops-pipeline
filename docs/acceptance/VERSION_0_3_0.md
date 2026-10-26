# boomtrack 0.3.0 development acceptance

Updated 2026-10-21.

## Included

- Visible `boomtrack` branding with the existing development package retained for upgrades.
- Playlist rename, entry reorder and entry removal.
- Bounded sidecar LRC discovery inside a user-selected document tree.
- Home-screen now-playing widget with play/pause and next.
- JSON export and restore for playlists, favorites, selected skin and equalizer preset.
  Audio and artwork are deliberately not embedded; restore reports unavailable media URIs.

## Automated evidence

- Debug and optimized unsigned release APKs build; lint completes with zero errors.
- API 23 passes seven app integration tests, including backup/restore and expanded playlist
  migration/edit assertions.
- API 23 passes four PCM/equalizer tests.

## Unverified gates

- Widget interaction and sidecar discovery still require manual device acceptance.
- API 37, physical-device behavior, accessibility, floating lyrics, online artwork/music,
  provider downloads and production signing remain open.

This is a development build, not a production release.
