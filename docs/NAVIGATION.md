# Navigation map — 2026-09-28

TTPod 8.4.1 is the visual reference. The accepted MY and full-player layouts are preserved. This map distinguishes usable local destinations from pages whose original service is unavailable; showing a destination does not mean its remote feature is implemented.

| Entry | Destination and behavior |
| --- | --- |
| MY / hamburger | Blue side drawer; closes on Back or outside tap |
| Local music / ALL | Existing numbered song list; play and contextual queue actions |
| ARTIST / ALBUM / FOLDER | Inline category list with counts; tap a category for its songs; Back returns to categories |
| Search | Dedicated search page with live title/artist matching and playable results |
| Favorite songs | Dedicated list backed by persisted favorite membership |
| Recent play | Up to 100 distinct songs, newest first; recorded by the playback service, including background playback; clear history does not remove files |
| Manage / library actions | Management page linking scanning, Android file import, a sidecar-lyrics folder, local music, and favorites |
| Scan Media | Dedicated scan page with real scan status/results and local-library return |
| Audio / MV download | Separate completed/in-progress views with honest empty and provider-unavailable states |
| Discovery / Recommendations | Destination pages explaining the unavailable service and linking local music |
| Settings | Navigable local settings index and About page |
| Sleep Mode | Preset/custom timers; playback service pauses when deadline is reached; cancellation available |
| Playlists | Creates and renames Room-backed local playlists; song menus add duplicate ordered entries; playlist page plays, reorders, removes entries or deletes the list |
| Background & Skin | Selects bundled or validated imported data-only skins and persists the choice |
| Audio Effect | Selects unity bypass or one of eight ten-band software EQ presets |
| Backup and restore | Settings exports/imports JSON organization state through Android's document picker; audio files are not copied |
| Home-screen widget | Shows current title/artist and controls play/pause and next through the playback media session |
| Account / DiDi / Music bag | Explicit unavailable-service destinations |
| Player options | Context menu for local/online lyrics, timing, artwork/lyrics selection, shuffle and track information |
| Player third page | Live PCM waveform visualizer; artwork and lyrics remain the first two pages |
| Queue | Bottom sheet with playable, reorderable entries and removal; intentionally remains an overlay |
| Share / import | Android chooser/file picker; intentionally remains a native modal flow |

## State and limits

Secondary-page Back restores the prior destination. Opening the full player from a song list leaves that list underneath. Page routes and search text survive Activity recreation; library category/filter state is saved separately. The mini-player stays on library pages and is hidden on full-screen settings/scan pages.

The sleep timer stores a deadline and is enforced by the playback service, not the Activity. It pauses audio rather than forcibly exiting the application. It is not an alarm-clock guarantee after Android kills the process; a restarted service observes an expired deadline. History and favorites currently use track URIs, so moved files are not automatically relinked. Queue move controls operate on the service-owned queue, so the existing checkpoint persists the new order.

Still outside this pass: original online catalog content, custom EQ band editing, skin marketplace/richer slots, the agreed primary spectrum stages, floating lyrics, exact secondary artwork, drawer animation, complete landscape parity, and current-API validation. The waveform is a working reconstruction, not a claim that the original third player page has been reproduced.
