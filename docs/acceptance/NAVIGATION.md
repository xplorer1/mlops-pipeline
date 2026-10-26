# Secondary page implementation — 2026-09-28

This pass replaces destination dialogs while preserving the accepted MY, local-song and full-player layouts. Version 0.1.2-dev (code 3).

## 0.1.3 development increment — 2026-10-21

- Queue rows now expose move-up and move-down controls backed by Media3's
  service-owned queue; the existing checkpoint stores the resulting order.
- Background & Skin now selects either Classic blue or Midnight violet. The
  choice is persisted and applied when the Activity is recreated.
- Version advanced to 0.1.3-dev (code 4). Debug assembly, seven domain tests,
  and lint passed with zero errors and 146 warnings. The debug APK SHA-256 is
  `A9BD6E75FFF64B42562B7A96C22F5D10B3CD59BF074A4912191C56A5B03E79C2`.
- Device smoke testing could not run because the previously used `classic_api23`
  AVD is no longer present. No device-level claim is made for this increment.

This increment does not claim third-party skin import, EQ, visualizers, online
providers, modern-device certification, production signing, or release readiness.

## Implemented

- Blue side drawer that moves the main page aside; back/outside dismissal.
- Artist, album and folder lists under the existing tabs, with counts and song drill-down.
- Dedicated live search, favorites and recent-play pages. Search/list destinations remain underneath the full player.
- Page navigation and search text restoration; category and filter restoration; covered pages cannot receive touches or accessibility navigation.
- Management and scan pages with actual scan feedback and Android file import.
- Audio/MV download destinations with completed/in-progress states; service-dependent pages explicitly identify unavailable content.
- Settings/About, background preview, effects preset layout, and track-information destinations.
- Preset/custom sleep timer and cancellation. The playback service enforces the persisted deadline after the Activity closes.
- Listening history is recorded by the service, including background playback, and limited to the latest 100 distinct track URIs.

## Reference evidence

Fresh private captures: `reference-artists.png`, `reference-drawer.png`, `reference-search.png`, `reference-sleep.png`, `reference-scan.png`, `reference-downloads.png`. These were inspected from the preserved TTPod 8.4.1 API 28 emulator. The reference emulator was stopped after inspection; its disk state and APK were preserved.

Private comparison: `reference-private/navigation-comparison.html` pairs the reference and rebuilt screens and includes opacity overlays. Assets in the shipped app are independently drawn vectors and backgrounds.

Sleep Mode retains the 48dp toolbar, 52dp information strip, and approximately 44dp choice rows. Artist lists retain the existing toolbar/tabs and use 80dp category rows. The drawer is 277dp wide at the 360dp reference viewport.

## Deliberate adaptations and unfinished work

- Sleep pauses audio instead of forcibly exiting the app. It is a service timer, not an alarm-clock guarantee after process death.
- Search matches local song titles/artists; reference category-specific search is not reproduced.
- Scan offers Android document import as well as MediaStore scanning; its lower actions differ from the original single-button screen.
- Drawer omits the original network/night toggles. Skin import/store and effect processing are unfinished; disabled preset representations do not apply audio processing.
- Online discovery, recommendations, downloads, account and recognition content are unavailable. Their destination states are reconstructed rather than fake server responses.
- Download artwork, exact icon contours, animations, full secondary-page fidelity and landscape visual acceptance remain open.
- The third player dot still opens reconstructed track information; the original visualizer/stage remains unfinished.
- History/favorites use URIs; moved-file reconciliation and playlist organization remain future work.

## Verification

Final debug and optimized unsigned release builds passed, alongside the seven domain tests (unchanged tests were up-to-date). Lint reports zero errors and 144 warnings; the new warnings are chiefly localization/KTX style suggestions. Build log: `reference-private/navigation-final-build.log`.

API 23: all four app integration tests and both PCM tests passed. Logs: `reference-private/navigation-device-tests.log`, `reference-private/tests-emulator-5584-app.txt`, and `reference-private/tests-emulator-5584-pcm.txt`. The PowerShell capture reports ADB's successful push progress as a NativeCommandError; the complete JUnit results independently show `OK (4 tests)` and `OK (2 tests)`. The targeted timer run also passed in `reference-private/navigation-sleep-test.log`.

The updated debug APK is installed on emulator-5584. Debug SHA-256: `16E52DC4F8C3DFA09F02F36EA3EF057FE1CBCA815EFB0A3CD0609498FCFEFE2B`.

Manual checks confirmed timer selection/cancellation, preserved search text after rotation, and portrait page rendering. These checks do not constitute full landscape fidelity acceptance. The frozen, jointly approved implementation plan is unchanged: SHA-256 `DB75AAD5E23808C494916425E635D698EB57F99A7E48A83E384B9346C42375EA`.

An initial history assertion incorrectly searched JSON text for an unescaped URI. The persisted history was inspected and the assertion changed to parse JSON. The targeted service/history/sleep test then passed. The final suite includes a navigation test for inline categories, drill-down/back, covered-page isolation, live search, and returning from the player to the same query.

API 37 remains blocked by the previously documented emulator graphics failure. This pass does not establish current-Android, physical-device or complete-interface acceptance.
