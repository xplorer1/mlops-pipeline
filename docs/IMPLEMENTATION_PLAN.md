# TTPod recreation: implementation and release plan

Revision: **1.1 — review candidate**  
Prepared: **2026-09-27**  
Review status: See [the review record](PLAN_REVIEW.md) for both reviewers' verdicts and the approved checksum.  
Scope of this document: planning only; no production implementation is authorized by this document alone.

## 1. Outcome and governing decisions

Build an Android music player that feels recognizably like the user's remembered TTPod 8.4.1, plays local music reliably, and remains useful without any server. Reimplement lyrics, skins, and online integrations under our control. Do not spend the project trying to restore the original backend.

The [original engineering handoff](../TTPod_Resurrection_Engineering_Handoff_v1.md) remains the preservation vision. This plan supplies sequencing, decisions, acceptance criteria, and explicit limits. Where current user decisions or verified evidence differ from that handoff, this document records the correction. Deferred features remain visible; an early alpha must not be called the completed preservation release.

Decisions already supported by the conversation:

- Android first; local playback is the foundation.
- TTPod **8.4.1** is the selected visual reference. The user recognized it and confirmed audible music playback.
- 9.0.4's Alibaba Planet interface was rejected and the APK removed at the user's request. It is not a design authority. Retained 10.0.7 is optional archaeology, not a reason to change the design.
- Old lyric and skin services are not dependencies of the new app. Their observed failures do not establish that every original endpoint is permanently dead.
- New code and original/licensed assets; no copied proprietary implementation, logos, or bundled skins in the release.
- No account, advertising, telemetry, remote configuration, or network access required for local listening.
- English is the initial product language. Preserve the player identity, not untranslated promotions or unusable account screens.
- Both independent reviewers must approve the same final plan revision before this plan is ready to implement. Review approval is not a claim that software has been built or validated.

## 2. What we actually know

| Evidence | Status | Consequence |
| --- | --- | --- |
| 8.4.1 launches on the Android 9 x86 emulator with ARM translation | Verified | Keep this private lab for observing reachable behavior |
| User recognizes the 8.4.1 interface | User confirmed | Stop searching versions unless a specific important feature is missing |
| Daisy Bell plays; progress advances; Android reports active playback | Verified; user also heard it | The reference is usable for player observation |
| Emulator Wi-Fi internet is validated | Verified | Failure of old online screens cannot be blamed only on airplane mode |
| Online lyric requests fail/time out; skin screen remains loading | Observed | Recreate workflows; do not design against surviving legacy endpoints |
| Local demo LRC is detected/parsed but lines do not display | Observed, cause unresolved | Do not infer original lyric behavior from this failure; test our own renderer independently |
| Exact queue rules, EQ presets, visualizers, skin capabilities, lyric interactions | Incomplete | Measure reachable behavior, then label gaps and use explicit fallback specifications |
| Modern app project, release build, physical-device compatibility | Not yet implemented/verified | Emulator success for the old app is not evidence for the new one |

Evidence index: [APK candidates](APK_CANDIDATES.md), [reference operation and playback observations](RUN_REFERENCE.md), private captures under `reference-private/screenshots/`. Reference APK hash and certificate are recorded in APK_CANDIDATES; signature continuity is not a general security certification.

Each future feature decision receives one label: **observed**, **user-confirmed**, **reconstruction** (designed because evidence is unavailable), or **platform deviation**. Screenshots and measurements support observed claims. Reconstruction must never be relabeled as historical fact just because the implementation looks convincing.

## 3. Product scope and release ladder

Primary user: someone who already owns/imports music and wants TTPod's player, folder browsing, lyrics, and customization on a modern Android phone. No cloud account or streaming subscription is needed. Initial distribution is a privately shared installable APK; a public open-source release comes after hardening and branding decisions.

| Capability | Local alpha A | Identity beta B | Preservation 1.0 |
| --- | --- | --- | --- |
| Permissions, MediaStore songs, user-selected files, basic folders | Required | Refined | Required |
| Playback, seek, queue, repeat/shuffle, notification, recovery | Required | Required | Required |
| Recognizable home, mini-player, now-playing layout | Required | Fidelity pass | Fidelity sign-off |
| Local LRC import, synchronized display, offset; embedded/local art | Required | Refined | Required |
| Artists/albums, playlists, favorites, local search, recent history | Next milestone | Required | Required |
| Original classic skin and one alternative; safe local skin import | Built-in classic only | Required | Required; export included |
| Deterministic ten-band EQ; classic visualizer/stage identity | Deferred | EQ plus one agreed mode | EQ plus the full agreed primary-mode set |
| Landscape player, sleep timer, headset controls | Basic safe rotation/headset behavior | Required | Required |
| Floating lyrics; at least one home-screen widget | Deferred | Deferred | Required where platform permits |
| Authorized lyric/art lookup; online music search/stream/download | Deferred | Optional preview only | Required with a real authorized provider |
| Offline survival after provider loss | Required | Required | Required |
| Actual tag editing, shake controls, optional legacy codecs, extra effects/visualizers | Deferred | Deferred | Conditional parity backlog, see below |

**Alpha is usable software, not full parity.** Its signature journey is: install → grant access or choose files → find a song → play → open the recognizable player → import an LRC → see timed lyrics → lock the screen and keep listening → reopen to the same queue.

**Beta restores identity.** It adds useful library organization, two original visual treatments, skin import, EQ, a working audio-reactive visualizer, and landscape presentation.

**1.0 retains the original handoff's release obligations:** offline library, reliable playback, local synchronized lyrics/art, ten-band EQ, the agreed primary classic visualizer/stage modes, skins, floating lyrics, and an authorized download that becomes durable local music. M0 establishes the required identity set as described below; a single generic spectrum effect cannot stand in for that set. If no suitable provider is available, ship a clearly named offline beta or seek an explicit scope revision; do not declare the full 1.0 complete using a fake provider.

Conditional backlog: real tag editing, shake gestures, crossfade, advanced bass/reverb/spatial effects, extra historical visualizers, expanded widgets, enhanced word-level lyrics, APE/WMA/ALAC decoders, and direct compatibility with old skin packages. During reference work, record which are observed and important. Missing items may be deferred with a documented parity limitation; do not claim exact/full historical parity. This backlog does not weaken the required 1.0 features in the table.

Not part of the planned first release: iOS/desktop, social feeds, live rooms, video/MV catalog, recommendation engine, cloud library sync, user accounts, skin marketplace/community moderation, DRM bypass, or restoration of the original servers. The old promotional tabs will not become misleading nonfunctional buttons. Keep the recognizable home/player organization; document the replacement or omission of online destinations.

## 4. Reference and design method

Time-box initial observation to **two focused working days**, then continue small captures when a feature needs them. Do not delay all coding until every unreachable screen is understood. Do not repeat the already-completed APK hunt.

Create `docs/REFERENCE_VERSION.md`, `docs/FEATURE_MATRIX.md`, `docs/NAVIGATION.md`, `docs/DECISIONS.md`, and `docs/reference/screens/`. Link the original handoff rather than maintaining a duplicate copy. Each feature-matrix row carries ID, evidence label, reference note, release milestone, implementation state, test, and any deviation.

M0 navigation documentation explicitly maps each legacy tab/destination to its retained, replaced, deferred, or omitted destination. M0 also inventories reachable visualizer/stage modes and records unavailable ones. Propose a named **1.0 identity set** for the user's approval: each required mode has an observation record or an explicit reconstruction brief, controls, representative visual state, and acceptance test. This is one focused fidelity decision after presenting the concrete evidence, not approval for every screen. A missing observation is not grounds to silently shrink the set. G0 closes only when the set is agreed; if evidence is unavailable, agree the reconstruction brief instead of extending archaeology indefinitely. Beta proves at least one member; 1.0 must implement every agreed primary mode. Additional nonprimary modes stay in the conditional backlog.

Capture these first:

| Screen group | Required observations |
| --- | --- |
| Home and mini-player | Categories/order, counts, entry/return paths, mini-player actions, empty state |
| Songs and folders | Row density, title/artist truncation, scan action, sorting, context actions |
| Player and queue | Page order/direction, controls, progress seeking, queue entry, repeat/shuffle, back behavior |
| Lyrics and artwork | Reachable page structure, placeholder behavior, selection UI; record unavailable animation details |
| EQ, settings, sleep, skins | Reachable controls, ranges, presets, menu structure; gaps explicitly recorded |

For each: save private screenshot/video path; capture emulator size/density/font scale, logical dp/sp measurements, state and input steps. Exclude system bars, proprietary imagery, and changing song text from pixel-diff scores. Compare control anchors, spacing, typography, density, gestures, and transitions. Pixel equality with copyrighted art is not a goal.

A primary screen is accepted when its reference/state record exists, the implementer has attached an aligned comparison with deviations, and the product reviewer/user can complete the same task without a major layout or interaction mismatch. Initial numeric target: main control anchors within **4dp** and list/text baselines within **3dp** at the fixed reference configuration; these are proposed quality budgets, not already-measured results. Larger deviations need a stated accessibility/platform reason and product acceptance.

Unreachable features use a written reconstruction brief, not another indefinite research loop. For example: lyric line-following, tap-to-seek, and skin import may be well-designed replacements without claiming that their exact gestures match 8.4.1. Get the user's visual feedback at the alpha and beta demos, especially on lyrics, skins, and visualizer choices.

## 5. Technical baseline and boundaries

Kotlin, XML Views/ViewBinding, custom Canvas views for lyrics/visualizers, Room for durable relational state, DataStore for preferences, Media3 for playback/session. Manual constructor injection initially. Avoid dependency-heavy UI/DI frameworks unless a measured need arises.

The handoff's version numbers are historical recommendations, not a frozen toolchain. Official Media3 release notes currently list **1.11.1** stable. Use it as the G1 candidate, then pin a compatible AGP/Gradle/Kotlin/KSP/Room/JDK set in the wrapper/version catalog and record successful command-line builds. AGP's release documentation lists compatibility requirements; do not assume the machine's existing JDK 20 or the broken global JDK 17 path is suitable. Use a project-local build environment. [Media3 releases](https://developer.android.com/jetpack/androidx/releases/media3), [AGP compatibility](https://developer.android.com/build/releases/gradle-plugin).

Proposed **minSdk 23**, retained from the handoff; dependency compatibility and an API 23 install/play test are G1 gates. If current dependencies cannot support it, select a maintained compatible version or propose raising minSdk explicitly. No silent minimum-version drift. `compileSdk`/`targetSdk` will be pinned to the latest stable SDK verified at G1 (at least the handoff's API 36 baseline); the current Android 17 documentation must also be evaluated. Preview/QPR beta SDKs are not required for the release. Record the exact chosen API and resulting behavior changes before building UI. [Android 17 release notes](https://developer.android.com/about/versions/17/release-notes).

Start with three Gradle modules; organize features as packages until they merit isolation:

```text
app/       composition, navigation, Views, controllers, Android adapters,
           library DB/repositories, settings, permissions
domain/    Android-free queue rules, identities, LRC parser/timeline,
           state models and test fixtures
playback/  MediaSessionService, ExoPlayer, queue execution, recovery,
           DSP/timing interfaces
docs/      decisions, feature matrix, screen specs, acceptance evidence
tools/     CMD build/install/test helpers
```

Dependencies: `app → playback → domain`, `app → domain`; never `playback → app`. App composition supplies repository interfaces defined by domain/playback. A service can construct required dependencies without an Activity. Extract skins, networking, and downloads when their milestones begin, rather than create twenty empty modules now. Do not bundle an HTTP client in the alpha without a consumer.

One activity/navigation host; one playback service and one player. UI sends commands through MediaController and renders service state. Parsing/rendering cannot call providers. UI recreation must not create a player. Artwork decoding and library queries run off the main thread. No work on the PCM callback may wait for DB, disk, networking, or UI.

## 6. Playback and persistence contract

Use a `MediaSessionService` with the media playback foreground-service declarations and platform media notification. Media3 supplies the service/session structure; integration must still cover current start restrictions and audio-focus rules. Controller access is restricted to necessary commands; validate media IDs/URIs from external intents and do not expose arbitrary file/network operations. [Background playback](https://developer.android.com/media/media3/session/background-playback), [audio focus](https://developer.android.com/media/optimize/audio-focus).

Define behavior before wiring controls. The following are **fallback product decisions**, replaced only by verified reference behavior or explicit product revision:

- Queue entries have their own IDs, so the same track can appear twice. Keep original order plus a persisted shuffle permutation; adding/removing entries must not recreate the entire player.
- Normal mode stops at the final item; repeat-all wraps; repeat-one repeats on natural completion. Explicit Next advances despite repeat-one. Previous restarts the current song after 3 seconds, otherwise goes to the previous queue entry. Document boundary behavior at the first/last item.
- Play next inserts immediately after the active entry; add to queue appends. Removing the active entry plays its successor if playing, stays paused if paused, and stops if the queue becomes empty.
- Seeking clamps to the playable duration; disable it for unknown/unseekable sources. Show requested position immediately and reconcile to actual player position.
- Pause for noisy-route/headset disconnect. Follow focus loss/ducking policy; resume only if the app paused temporarily for focus and the user has not since paused. No automatic surprise playback on reboot.
- Missing/unreadable track: explain the failure and allow locating/removing it. During automatic advance skip invalid entries with a bound of one traversal, then stop. Do not delete playlists or spin forever.
- Save queue, active entry, position, shuffle/repeat on changes and every 5 seconds while playing, transactionally where appropriate. Restore paused after process death/reboot; user resumes through UI/system controls. A killed process may lose up to the latest checkpoint; uninterrupted playback through OS termination is not promised.
- Swiping away the Activity must not stop active playback. Android force-stop is respected. Reboot does not start playback or a permanent scanning service.

Sleep timer in beta: a duration timer uses a monotonic elapsed-time deadline that continues while paused; resuming after expiry clears play intent and requires a fresh user Play action. Cancel, stop after current track, optional short fade, and visible countdown are supported. Stop-after-current waits for natural completion and remains armed across pause; explicitly skipping the track stops playback rather than moving the timer to another song. These are fallback decisions, documented if the reference differs. Execute in the playback service; no exact-alarm permission by default. Process death stops audio anyway; recovery must not resume an expired timer session automatically.

Persist Room entities for Track, MediaLocator, QueueEntry/QueueSnapshot, Playlist/PlaylistEntry, History, LyricSelection/Offset, and ArtworkSelection. Track gets an internal stable ID; MediaStore locator uniqueness includes **volume + row ID**, not row ID alone. SAF uses granted document URIs. Provider IDs are namespaced mappings, not local primary keys. Duplicate playlist/queue entries are intentional when the user adds them twice. Preserve favorites/playlist membership when a file becomes unavailable; a relink operation is explicit. Never assume URI stability through reinstall or volume replacement.

Export Room schema from v1; migration fixtures preserve queue/playlists/favorites/lyric associations. No destructive migration in release. Histories are bounded; private paths/tokens are excluded from release logs. Media bytes are not duplicated merely to index them.

## 7. Library, storage, lyrics, and artwork

### Library access

Use MediaStore for shared audio, and the system picker for explicitly selected files/folders. API 33+ uses `READ_MEDIA_AUDIO`; supported older versions request legacy read access where needed, capped at API 32 for broad audio reads. Permission denial leads to a useful empty state and a picker path, not a crash or repeated modal. No `MANAGE_EXTERNAL_STORAGE`. On rescans distinguish permission revoked/unmounted volume/query failure from a successful empty result before marking tracks missing. [MediaStore access](https://developer.android.com/training/data-storage/shared/media).

Scanning is cancellable, batched, and reports progress. Show cached rows immediately; run reconciliation in the background. Observe changes while active; use available generation/version information with a fallback reconciliation for older APIs. Avoid full startup scans and automatic recursive walks of all storage. Folder browsing represents accessible media locations; it is not a promise of unrestricted filesystem access.

### The important sidecar constraint

Permission to read audio does **not** guarantee access to adjacent `.lrc` or folder artwork files. Use `ACTION_OPEN_DOCUMENT` to import a lyric/image or `ACTION_OPEN_DOCUMENT_TREE` for a supported user-selected music folder; retain grants when offered and explain re-selection if revoked. Search sibling sidecars only inside a granted tree or other location actually readable by the app. For manual imports, copy a bounded lyric into private durable storage and associate it with the track so it remains usable after the picker grant changes. There is no silent whole-device LRC scan. [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files).

### Local lyric implementation

Resolution: manual selection → readable exact-basename sidecar → readable `Artist - Title.lrc` → supported embedded lyrics → stored/cached selection → plain-text/no-lyrics state. Remote lookup is a separate explicit action introduced later; starting a local song never makes a request.

Parser: UTF-8/BOM and UTF-16 BOM; explicit encoding fallback selection for legacy text; `[mm:ss.xx]` and millisecond precision; multiple timestamps per line; metadata; global offset; blank lines; stable ordering for equal times; invalid/negative/overflowing timestamps; CRLF/LF; maximum **1MiB and 20,000 timed lines**. Malformed lines are skipped with an import warning, not an app crash. Enhanced word timing is later; ordinary LRC remains the reliable baseline.

Normalize once. Render from current playback position with binary search, not a separate accumulating timer. All internal timestamps/offsets use milliseconds. Adopt the LRC convention in which positive file `[offset:]` advances the lyrics, while the separately labeled user **delay** control is positive to delay them: `effectiveTimeMs = rawTimestampMs - fileOffsetMs + userDelayMs`. This sign distinction is documented in existing parser implementations; it is not inferred from the broken reference display. [Paroles offset documentation](https://github.com/Clarkkkk/paroles). Apply each adjustment exactly once; persist the user's delay per track/lyric selection. Required fixtures: a 10,000ms cue with file `+500` displays at 9,500ms; file `-500` at 10,500ms; user delay `+500` alone at 10,500ms; file `+500` and user delay `+500` cancel. Preserve valid negative effective times as already-active at position zero rather than wrapping or double-clamping timestamps. Recompute after seek, pause/resume, song change, and route change. Pause does not advance lyrics. Animate visible content only; manual scroll temporarily suspends following and provides a return-to-current action. Tap-to-seek is a reconstruction unless observed in the reference, with clear visual feedback and no accidental seek from scrolling.

Alpha includes import/replace, no-lyrics state, offset adjustment/reset, and synchronized display. A lyric failure never blocks transport controls. G2 includes malformed/oversized import → understandable error with the current selection preserved → valid replacement → synchronized display. Test timing against a generated score with known cues, not arbitrary timestamps attached to a real recording.

### Artwork

Manual selection → embedded image → readable folder image → stored/cached art → original placeholder. Decode off-thread at display size, cap input dimensions/bytes, and bound memory/disk caches. User-chosen images are durable selections, not expendable cache. Album grouping must not merge unrelated albums just because titles match. Later remote artwork is user-initiated and independently replaceable.

## 8. Skins and visual identity

Alpha ships a newly drawn classic-inspired default, with reusable style tokens from the start. Beta supplies a second clearly different original skin and **local package import**, proving this is more than a color preference. No skin server is required.

Define a versioned ZIP format with manifest, colors, original image/icon assets, lyric/visualizer styling, and constrained spacing/background options for known screen slots. Initial packages cannot run code, supply arbitrary layouts, fetch URLs, replace accessibility labels, remove required controls, or intercept playback actions. Typography uses a small bundled licensed/system choice set initially; custom font loading waits for validation work. Direct legacy-package compatibility is not assumed.

Initial import limits: **10MiB compressed, 40MiB expanded, 256 files, 4096px maximum image edge and 24 megapixels total**; validate actual extracted bytes rather than trusting ZIP headers. Reject traversal, absolute paths, symlinks, nested archives, duplicate/case-colliding names, unsupported manifest versions, and undeclared asset types. Extract into staging under app-private storage, validate referenced assets and numeric ranges, preview, then atomically activate. Failed import leaves the current skin intact. Record attribution/license fields for bundled/distributed skins.

Switching skins during playback preserves player, queue, seek position, and lyrics. Incompatible/corrupted skins fall back to the built-in classic. Keep a settings recovery action independent of custom styling. Export user-created selections in 1.0 only for assets the user can legitimately export; do not auto-export original reference assets.

Downloaded skins later use the same importer and version checks. A marketplace, executable plugins, unrestricted layout language, and user-account system are not prerequisites.

## 9. Audio processing and visualizers

Alpha uses supported Media3/platform decoding: test MP3, AAC/M4A, FLAC, PCM WAV, Ogg/Vorbis; Opus is advertised only where verified in the chosen API/decoder matrix. Unsupported formats produce an actionable error and leave the queue usable. Do not claim APE/WMA coverage based on a file extension.

Run an early **two-day maximum DSP feasibility spike** before committing the full beta implementation: prove a custom PCM processor and visualization tap in the pinned Media3 version, including sample-rate/channel changes and seek/reset. Custom processing must not silently be bypassed by audio offload/passthrough; begin with offload disabled where processing is required, then measure the battery cost. The PCM representation and pipeline integration are explicit decisions from this spike. [Media3 audio sink API](https://developer.android.com/reference/androidx/media3/exoplayer/audio/DefaultAudioSink).

Beta EQ: ten software biquad bands, frequencies/preset curves measured when reachable, otherwise a labeled documented reconstruction. Preamp/headroom and smoothly applied coefficient changes avoid clipping and switching pops. Test impulse/frequency response, unity bypass, gain bounds, finite output, multiple sample rates/channels, and rapid preset changes. Do not claim acoustic equivalence to proprietary effects. Bass/reverb/spatial effects require separate evidence and quality tests before being advertised.

Visualizer: read only this player's PCM through a bounded nonblocking ring buffer; worker computes windowed FFT; UI draws at an appropriate refresh rate. No microphone permission. Verify silence is quiet, a test tone lights the expected band, and pause/seek transitions do not show stale data. Stop FFT/drawing when invisible. One member of the user-agreed identity set is required for beta; record whether observed or reconstructed. M6 completes any remaining required primary modes with their individual fidelity/behavior tests before G7. Landscape stage reuses the same playback state and processing, not a second player.

## 10. Online replacements and durable downloads

Create separate `LyricsProvider`, `ArtworkProvider`, and `MusicProvider` contracts only when their milestone begins. Capabilities specify search, streaming, downloads, pagination, rate limits, attribution, caching constraints, and expiry. Hide unavailable actions with an understandable explanation; do not imply every provider offers every capability. No original TTPod endpoint becomes a production dependency.

Before selecting a real provider, record its official API documentation, authorization/terms relevant to the planned actions, credentials model, geographic/catalog limits, and evidence for permitted downloading/caching. Provider selection is an **external dependency**, not already solved. A small authorized/openly licensed catalog may satisfy the workflow; it must not be marketed as a replacement for a commercial catalog. API access alone does not imply download rights. Embedded app secrets are not confidential; if a provider requires a confidential backend, assess that dependency explicitly or choose another provider. This plan does not authorize building an account/backend platform by stealth.

Lookup UI states: idle, searching, results, no match, canceled, offline, timeout/rate-limited, unavailable. Bounded timeout/retry with backoff; explicit retry after failure, never endless loading. User confirms ambiguous lyric matches. Cancelling navigation cancels nonessential lookup. Tests use deterministic fake providers, but G5 requires all of these real authorized paths, possibly using different providers:

1. Lyric search → confirmed match → permitted durable local save → synchronized rendering after restart with network disabled.
2. Artwork lookup → selection → permitted durable local save → correct display after restart with network disabled.
3. Music search → selected stream → audible playback, pause/seek where supported, background controls, and understandable expiry/network-loss recovery.
4. Music search → authorized download option → verified durable publication → playback as a normal local song after restart and provider loss.

Each evidence record names the provider, test content, capabilities, source/attribution, and storage permission. A provider allowing only transient lyric/art caching does not satisfy the durable-selection gate; select a compatible source or seek an explicit scope change. Streaming does not imply offline rights. These gates prove the supported workflow, not comprehensive catalog availability.

Downloads use a durable DB state machine:

```text
queued → running → verifying → publishing → completed
             ↘ interrupted/failed → retry
queued/running → canceled
```

Pause/resume is shown only if supported; range resumes validate resource identity/ETag, otherwise restart. On modern Android write user music through MediaStore pending items; on older supported devices use a user-selected writable destination through SAF, copying from private staging only after verification. Show storage requirements/failures. Verify MIME/container/readability, supplied checksum when available, and complete byte count when known. Unknown content length is allowed; HTTP success alone is not proof of valid media.

Make publication idempotent: persist destination URI and job identity before finalization, reconcile interrupted publication after restart, and link the local track exactly once. MediaStore and Room are not one atomic transaction; recovery must handle published media with a missing DB row and stale pending rows. Never display an incomplete file as completed. Cancellation cleans only the operation's temporary/pending item; never remove unrelated user files. Avoid overwriting existing music silently; offer reuse or a distinct filename.

Choose the transfer executor during the milestone based on OS/version and operation length: user-initiated transfer jobs where supported for long explicit downloads, an appropriate foreground/user-visible fallback on older APIs, and WorkManager for suitable short deferrable work. It is not the playback engine. Verify quota/background restrictions rather than assuming all long transfers fit WorkManager. [Android transfer options](https://developer.android.com/develop/background-work/background-tasks/data-transfer-options).

Completed downloads appear in Songs, Folders, and Downloads, survive provider failure, and remain user-visible shared files after app removal. Confirm this with disposable media and an uninstall/reinstall test. Download history/art/lyrics stored only in app data may be removed on uninstall; explain/export these separately rather than promising everything survives.

## 11. Android integration, accessibility, and privacy

In 1.0 add one useful widget plus opt-in floating lyrics driven by the same playback service. Ask for overlay access only after the user enables it; no overlay on onboarding or when permission is revoked. Provide move, lock/unlock, opacity, and close controls; exclude sensitive lock-screen display by default and document OS restrictions. Stopping playback must not keep an indefinite hidden service alive just to preserve an overlay.

Provide readable text, accessible control descriptions and focus order, usable hit targets, contrast checks for bundled skins, font scaling, and reduced motion. Custom lyrics/visualizers must not spam screen-reader announcements on every frame. Preserve the visual layout at normal font scale, allow scrolling/reflow at larger scales, and document necessary deviations. Handle system insets, edge-to-edge requirements, back navigation, rotation, and large-screen resizing; do not hard-code the emulator's pixel dimensions.

No contacts, location, microphone, camera, broad filesystem access, analytics, or startup remote requests. `INTERNET` is introduced with real online features. Media playback/service, storage, overlay, and any notification permissions each have a documented purpose and version behavior. Do not request notification permission just to block playback; test media-session notification behavior separately from download notifications.

Release logs omit full file paths, lyrics, queries, tokens, and private URLs. Diagnostics are local/opt-in. Export only after user action. No code downloads in skins or providers. Public code/assets need provenance and dependency licenses; private screenshots/APKs remain ignored. Public project name/icon/license and application ID must be settled before a distributable release; use a clearly temporary independent dev namespace until then, never `com.sds.android.ttpod`.

## 12. Milestones, dependencies, and gates

One milestone closes only with an installable build (where applicable), evidence links, test results, known issues, and feature-matrix updates. A demo that merely opens is not a playback/lifecycle gate. Review findings block only affected work; no 1.0 claim while a required gate is open.

| Milestone | Work and deliverables | Depends on | Exit gate |
| --- | --- | --- | --- |
| **M0: reference contract** | Screen notes/navigation including legacy-tab mapping, feature matrix, fallback semantics, original-asset inventory, selected reference record, proposed primary visualizer/stage identity set | Approved plan | **G0:** enough evidence for home/songs/player/queue; unavailable features labeled; user agrees named required identity set/reconstruction briefs; no broad APK hunt remaining |
| **M1: build and risk spikes** | CLI Gradle project, pinned toolchain, minimal install, MediaStore/SAF probes, service smoke, DSP feasibility report | M0 | **G1:** debug build from clean checkout; API 23 and selected current API install; local playback service/sidecar permission path proven; DSP risks resolved or concrete fallback/revised milestone |
| **M2: local alpha** | Home/songs/folders, scan, player/queue/repeat/shuffle, recovery, notification, local art/LRC/import/offset, built-in original classic styling | M1 | **G2:** alpha journey offline including failed-import recovery; background/screen-off audio and user-audible real-phone check; deterministic lyric/queue tests; user can recognize and use the player |
| **M3: library completion** | Artists/albums, playlists/favorites, local search/history, sorting/reconciliation, missing-file handling | M2 | **G3:** 10k-track test library remains usable; rescan preserves organization; grant revocation/removable-volume cases pass |
| **M4: identity beta** | Fidelity refinement, two skins and importer, ten-band EQ, primary visualizer, landscape, sleep timer | M3; DSP spike passed | **G4:** beta journey with live skin switch, correct lyric cues, audible EQ verification, audio-reactive visualizer, screen-off sleep; no broken player state |
| **M5: online workflow** | Provider selection record, lyric/art lookup, music search/stream, validated durable download, failure states | M3; integrate with M4 before release | **G5:** all four real-provider journeys in §10 pass, including permitted offline lyric/art persistence and streaming; interrupted publication/retry tested; provider loss leaves local playback intact |
| **M6: daily-use integration** | Widget, floating lyrics, remaining agreed primary visualizer/stage modes, export of user organization/settings/skin selections, accessibility, full device matrix | M4 | **G6:** widget/session consistency, required identity-set tests, permission denial/revocation, accessible core journey, export/import with explicit URI relinking |
| **M7: release candidate** | Cross-milestone regression, performance/battery, assets/license review, final branding/package, signed release artifacts, upgrade validation and docs | M4–M6 | **G7:** every required 1.0 row and agreed primary identity mode passes; all G5 online paths evidenced; signed upgrade preserves user data/access; two review sign-offs plus user acceptance; no critical playback/data-loss/security defects |

Critical path: M0 → M1 → M2 → M3 → M4 → M6 → M7. M5 can proceed after M3, but provider authorization may become the release bottleneck. No separate backend or marketplace is on the critical path.

First implementation tasks after the plan is accepted:

1. Create the M0 evidence/decision files and fixture manifest; capture the reachable priority screens.
2. Lock SDK/dependency/JDK compatibility; create independent modern-app AVD(s), keeping the original lab intact.
3. Prove shared-audio enumeration, manual LRC selection, and a playback service from CMD.
4. Run the bounded DSP spike; record actual pipeline compatibility and tests.
5. Implement one song-list→player→background→reopen path, then lyrics/queue/error states. Do not begin with online search or a theme marketplace.

## 13. Acceptance tests and measurement

### Fixtures and reproducibility

Use self-generated/licensed audio with a provenance manifest: MP3 with/without embedded art, FLAC, AAC/M4A, WAV, Vorbis; Unicode/long metadata, two albums sharing a title, duplicate files on different volumes, broken tags/art, missing files, granted/revoked SAF files, and timed/plain/malformed lyrics. Generate a separate 10k-row/track library for scale; distinguish synthetic DB benchmarks from actual MediaStore scan tests. No private personal music committed.

Keep `docs/acceptance/<milestone>.md` with device/API/build ID, commands/actions, expected/actual, pass/fail, and artifact paths. Screenshots do not prove audible output; instrumented player state does not prove a phone speaker/Bluetooth route works. Pair automation with physical listening tests.

| Area | Required proof |
| --- | --- |
| Queue | Duplicate IDs, reorder/remove-current, previous threshold, all repeat/shuffle modes, empty/broken queue; persisted state and rapid UI/session commands |
| Lyrics | Known cue boundaries, both offset signs, encoding and malformed input, pause/seek, manual scroll, import/replacement and process recreation |
| Playback lifecycle | 60-minute screen-off session; task removal; focus interruption; notification/lock-screen controls; kill/reopen with checkpoint; reboot remains silent |
| Storage/library | Grant/deny/revoke, tree selection, unmount, incremental change, failure vs deletion, restart mid-scan, no duplicate logical rows from retries |
| Audio | Real format samples, route changes, EQ frequency response/headroom, no transition pops, correct FFT response to silence/tone |
| Preservation identity | Every named primary mode in the M0 user-agreed set meets its observation/reconstruction brief; beta's first mode alone cannot close 1.0 |
| Skins | Valid import/switch/restart, invalid manifest/assets, traversal/ZIP bomb/duplicate paths, rollback/recovery, no executable/network content |
| Providers/downloads | Offline/timeout/429/no match, changing resource, unknown length, corrupt content, cancellation, disk full, process death at each publication boundary, deduplication |
| Real online paths | All four §10 journeys, attribution/storage evidence, offline lyric/art persistence, streaming behavior, and durable downloaded music |
| Privacy/offline | Cold start/local play with network unavailable; later network-enabled test shows zero requests caused by local start; no polling when idle |
| Accessibility | Screen reader core journey, 1.0/1.3/2.0 font scale, reduced motion, controls usable with both bundled skins |

Compatibility matrix for the **new** app: API 23 minimum, 29 scoped-storage transition, 32 legacy-read ceiling, 33 audio/notification permissions, 34 foreground/transfer changes, 35 focus/target behavior, 36, and the stable current API chosen at G1. Consolidate duplicate behavior checks, but install/smoke each supported boundary before release. Small phone, large screen, portrait/landscape, 60Hz and high refresh where available. At least one physical ARM64 phone with wired/USB or Bluetooth output; emulator-only certification cannot close G7. User's phone model/API and available test hardware are currently unknown and will be collected before device-specific validation, not invented here.

Initial performance budgets (acceptance targets, not results):

| Measure | Target and method |
| --- | --- |
| Cold usable home with cached 10k library | p95 ≤ 2s across 20 release-like starts on a named mid-range physical device |
| Tap-to-audible local playback | p95 ≤ 500ms across 30 starts with cached metadata, valid local files; separate decoder/route cold cases |
| Local search | p95 ≤ 200ms after debounce, 10k library |
| Foreground timed lyric cue | ≤ 100ms presentation error against known cues; report Bluetooth acoustic latency separately |
| UI smoothness | ≥ 95% frames within one display interval in scripted library/player flows; track worst stalls |
| Memory | Initial target ≤ 150MiB steady-state PSS in normal player/library flow; separate stress-art/visualizer cases |
| Base release APK | Initial target ≤ 25MiB excluding optional codec packs; report per-ABI artifact size |
| Background cost | No FFT/UI redraw when invisible; no periodic network/scanning; compare 60-minute EQ-off/on energy/CPU and underruns on the same phone |

Record baseline hardware/build/thermal conditions at G1/G2. Budget changes require an explained decision and product/technical review, not silently changing thresholds after a failing run. Battery percentage alone is too noisy for a precise universal power promise.

CI once project infrastructure exists: dependency pin/verification, compile, lint, meaningful domain/DB/parser tests, debug APK artifact, current/minimum API instrumented smoke; expand with each subsystem. Run release/R8 builds before G2 and G7 to catch minification-specific failures. Physical audio, overlays, Bluetooth, and long screen-off validation remain explicit manual gates when CI cannot cover them.

Before private distribution, settle the distributable application ID and signing identity; a temporary development namespace is not a seamless-upgrade promise. Keep the signing key and credentials outside the repository/artifacts, with a recoverable owner-controlled backup. G7 installs an older signed test build, creates playlists/favorites/lyric selections and SAF grants, then upgrades in place with the same identity. Verify signature/version, Room migration, queue/settings, retained URI access, and playback; no uninstall or destructive migration may substitute for an upgrade test.

## 14. Effort, risks, and decision points

This is several subsystems, not a weekend UI clone. The estimate assumes one experienced Android implementer, existing SDK access, timely feedback, and no custom codec/backend requirement. It is a planning range, not a delivery promise; agent assistance does not remove hardware validation time.

| Work | Focused engineering days |
| --- | --- |
| M0 reference contract | 2–3 |
| M1 build and risk spikes | 3–5 |
| M2 local alpha | 8–12 |
| M3 library completion | 5–8 |
| M4 identity beta | 10–16 |
| M5 authorized online/download workflow | 7–12, excluding external approvals |
| M6 integration/accessibility/export | 5–8 |
| M7 hardening/release | 5–8 |

Total: **45–72 focused engineering days**, plus roughly **20–30% contingency** for compatibility/fidelity fixes and external waiting. Alpha: about **13–20 focused days** including M0/M1. Re-estimate after G1 and G2 from actual throughput. If time is constrained, reduce the release label/scope explicitly; do not sacrifice background reliability, user data integrity, or pretend a generic player is faithful.

The M4/M6 range assumes the agreed primary identity set is small (roughly one to three modes) and uses the shared PCM/rendering foundation. Re-estimate at G0 if the evidence/user-approved set is larger or includes materially different rendering; retain its explicit release gate rather than fitting it invisibly into the estimate. M5's range includes the four acceptance journeys, but catalog/provider approvals remain external waiting time.

| Risk | Early check | Response / gate |
| --- | --- | --- |
| Unreachable original behavior | M0 evidence classification | Reconstruct and label; don't stall the entire app |
| Modern sidecar access fails | M1 SAF probe on current API | Manual import/granted-tree lookup; no all-files permission workaround |
| DSP bypass/offload or CPU cost | M1 spike, M4 physical benchmark | Disable offload for processing, bound work; revise beta estimate if needed |
| No suitable authorized provider | Selection begins by M3 | Offline milestones continue; G5/full 1.0 remains open |
| No physical test phone | Inventory before G2 | Emulator alpha can be demonstrated, but physical playback/release gates stay open |
| Default style feels generic | G2 comparison and user demo | Correct player/navigation before adding more features |
| Skin format grows into a UI engine | M4 constrained schema | Fixed supported slots, no arbitrary layout/code |
| Library identity loses playlists | M1 model review, M3 migrations | Stable internal IDs, missing states, explicit relinking |
| Broad scope crowds out usable release | Each milestone review | Ship honest alpha/beta; retain traceable deferred features |

Questions that do not block writing this plan: final public name/icon/license, user's phone details, preferred alternative-skin look, and exact provider. Resolve each before its affected milestone. User approval is needed for a material product-scope change, final public branding, and public distribution—not every reversible implementation choice.

## 15. Review and change control

Two reviewers work independently **after this draft is complete**:

- Technical expert: Android/toolchain feasibility, architecture, storage, lifecycle, persistence, audio/DSP, provider/download safety, tests and measurable gates.
- Product expert: faithful identity, scope sequencing, user journeys, recovery states, usability, accessibility, honest parity/release claims, realistic dependencies and effort.

Each must issue **APPROVE** or **REQUEST CHANGES**, with blocking findings separated from nonblocking suggestions. Approval means the plan is coherent and safe to execute with its stated milestone gates, not that future dependencies have already been solved. Reviewers may disagree and must not be pressured to rubber-stamp.

Record review findings, resolutions, final verdicts, reviewed revision, and SHA-256 of this file in `docs/PLAN_REVIEW.md`. Both must approve identical final bytes. If a revision changes those bytes after either sign-off, resubmit it to both. An unresolved material disagreement keeps the plan unapproved and must be surfaced to the user.

Once approved, the next step is M0/M1 implementation following the user's go-ahead. Do not scaffold production code or claim the project has started merely because the planning review passes.
