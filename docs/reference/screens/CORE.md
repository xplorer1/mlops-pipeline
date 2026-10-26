# Priority screen observations

Reference resolution 360×640dp; measurements approximate until aligned diffs.
Source screenshots private under `reference-private/screenshots/`.

## Home / library

`ttpod-8.4.1-first-launch.png`: blue navigation band, MY/online tabs, 2-column
local categories and a bottom mini-player. `m0-home.png` is the later Discovery
loading state, not a successful home capture. Empty/dead online states must not
be copied into the offline app. The first build uses a reduced local home.

## Songs

Observed header “Local music”; tabs ALL / ARTIST / ALBUM / FOLDER; Random Play
row and song count; title then artist in each song row, right action affordance;
mini-player remains anchored at bottom. Scan Media → Fast scan → Complete adds
local files. Title/artist occupy roughly 48–56dp list rows at this scale.

## Player

`ttpod-8.4.1-local-lyrics.png`: dark blue background; header near x50dp/y45dp;
title then artist; body occupies most height; progress around y476dp; large
center play/pause near y536dp; previous/next and queue flank it. Three page dots
above transport. Horizontal paging and back to library do not stop playback.
The capture shows “Good TTPod” instead of working lyrics, so lyric timing and
line styles remain reconstruction. No proprietary background is reused.

## Queue / modes

Queue entry comes from the lower-right music/list control. Exact reorder/remove
and previous thresholds have not been proven; use the plan's documented fallback
semantics. Do not label those semantics “observed”.

## Effects

`m0-effects.png`: preset tiles and Custom/Edit/Manage. Preset names are recorded
in REFERENCE_VERSION. Band frequencies and preset curves not yet measured.
Skins and original visualizer animation are unavailable/unverified; use the
IDENTITY_SET briefs and retain that uncertainty.
