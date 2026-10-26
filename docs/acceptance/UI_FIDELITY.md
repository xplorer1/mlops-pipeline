# TTPod 8.4.1 interface reconstruction

Updated 2026-09-28. The user's correction is the governing requirement: reproduce
the selected version's interface directly, rather than modernizing its layout.
The approved implementation plan is unchanged; this is implementation evidence.

The user accepted the current main-screen look. The subsequent
[secondary-page pass](NAVIGATION.md) supersedes the navigation limitations below;
the measurements and verification here describe the 0.1.1-dev build.

## Reference observations

Private reference images are observations only, never bundled app assets.
The current APK was reopened on the preserved API 28 emulator. Captures use
1080 x 1920 pixels, 480 dpi (360 x 640 dp); app checks use the same dimensions
on API 23. OS status/navigation glyphs differ between those Android versions.

- `ttpod-8.4.1-first-launch.png`: MY dashboard, 52 dp navigation, 184 dp profile
  region, two columns of 73 dp category tiles with 10 dp gaps, favorites section.
- `reference-local.png`: 48 dp Local music toolbar, 38 dp information strip,
  40 dp ALL / ARTIST / ALBUM / FOLDER tabs, 80 dp Random Play row, numbered
  56 dp song rows, persistent 60 dp mini-player.
- `reference-player-live.png`: 52 dp title bar, artist beneath, expansive body,
  page indicators, comment/favorite/share actions, seek near y=476 dp, transport
  center near y=536 dp. Loaded reference uses blue controls and a blue play disc
  with a white glyph. Earlier incomplete frames had white controls.
- `reference-queue.png`: queue opens from the bottom with count, current-item
  indicator, song rows and per-item removal. The old center alert was not faithful.

## Scope and honest departures

This pass replaces the scaffold's oversized scan buttons and generic list/player
layout. Original vector controls are drawn from observations; no extracted APK
assets or original implementation are used. A newly drawn background approximates
the reference skin and requires further visual refinement.

- Profile identifies a local listener instead of advertising a nonexistent login.
- Online dashboard destinations disclose their unavailable replacement service.
  No fake download or network success state is shown.
- English replaces remaining Chinese strings and the lyric tip describes local
  import. The reference comments button is currently used for local lyric import.
- In 0.1.1-dev, Recent play opened the queue. Version 0.1.2-dev replaces this
  with a dedicated service-recorded listening-history page.
- Favorite membership uses local URI preferences for this build; relinking/export
  and migration to the planned organization model remain necessary.
- Version 0.1.2-dev replaces artist/album/folder dialogs with inline category
  lists and adds the drawer and destination pages. Exact secondary artwork,
  animation and landscape fidelity still require further work.
- Third player dot opens track information; the original third presentation has
  not been accepted as reproduced. No visualizer/skin/EQ completion is claimed.

## Verification

Development build 0.1.1-dev (version code 2) replaces 0.1.0-dev in place.
Private comparison: `reference-private/fidelity-comparison.html`, with paired
screens and an opacity overlay. Rebuilt captures are `fidelity-home.png`,
`fidelity-local.png`, `fidelity-player.png`, and `fidelity-queue.png` in the
private screenshots directory.

Measured portrait anchors at 1080 x 1920 (pixels):

| Anchor | Reference | Rebuilt |
| --- | --- | --- |
| Local toolbar bottom | 216 | 216 |
| Local tabs bottom | 450 | 450 |
| First song row top | 691 | 690 |
| Mini-player top / bottom | 1596 / 1776 | 1596 / 1776 |
| Full-player seek center Y | 1428 | 1428 |
| Full-player play disc top / bottom | 1518 / 1698 | 1518 / 1698 |

These anchors demonstrate layout alignment, not an overall pixel-error score.
Remaining differences include background artwork, icon contours, some labels,
OS bars, and the secondary screens described above. Successful local lyric
rendering is reconstruction because the reference lyric service fails.

Final build and device logs are `reference-private/fidelity-final-build.log`
and `reference-private/fidelity-device-tests.log`; results are recorded in
PROGRESS.md. Debug and optimized unsigned release builds passed; lint has zero
errors and 130 warnings. The final device run passed all three app integration
tests and both PCM tests. An initial test-only null accessibility-root failure
was corrected with a bounded readiness wait; the subsequent run passed.
`reference-private/fidelity-test-build.log` records that test rebuild and lint.
The seven unchanged domain tests remain passing in the build command.

Manual checks confirmed artwork/lyrics swiping, favorite membership retained
through the development update, actual PCM playback, queue append and removal.
Queue screenshot uses two entries for the same generated fixture; the temporary
extra entry was removed afterward. API 37 remains blocked as recorded in M1;
no new modern-platform or physical-device claim is made.

Debug APK SHA-256:
`B80A799B421BFAB8659CF76B9132839308270CB79F1B3942B5B5F6069BD897F0`.
The reviewed plan's checksum remains unchanged. This is not full-interface or
release acceptance; the remaining differences above are still work to do.
