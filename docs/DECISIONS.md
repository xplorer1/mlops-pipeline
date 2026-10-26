# Implementation decisions

The user authorized implementation on 2026-09-27 after both experts approved
plan revision 1.1. This log records implementation details without altering the
reviewed plan/checksum.

- D01: use a separate `org.ttpod.recreation.dev` application ID for development.
  Label it “Classic Player Dev”; it is not final public branding or an official app.
- D02: retain API 23 minimum; select API 37 stable platform (37.0 SDK package).
  AGP 9.4.0, Gradle 9.6.0, built-in Kotlin 2.2.10, KSP 2.3.12,
  Media3 1.11.1 and Room 2.8.5 passed the CLI debug/release build and lint.
- D03: use existing JDK 20 process-locally if the pinned build verifies it;
  compile Java/Kotlin bytecode to 17. Do not repair global JAVA_HOME implicitly.
- D04: stage M0 identity approval separately from independent M1 build/parser
  probes. G0 remains open until the user chooses the reconstructed identity set;
  no beta visualizer implementation is implied by the foundation work.
- D05: first milestone has no INTERNET permission, no remote providers, and no
  extracted proprietary assets. Draw simple original control vectors.
- D06: manual LRC picker is the first durable lyric-access path. Automatic
  sidecar discovery in user-granted trees remains a later alpha task.
- D07: keep the original emulator installation intact. Use a workspace-local
  stable emulator 37.1.11 for the API 37 dev AVD. Test disabling GLDirectMem and
  HasSharedSlotsHostMemoryAllocator after GLDMA alone failed to resolve a
  SurfaceFlinger assertion; enlarge the new AVD data
  partition from 800 MB to 4 GB after the package installer reported no space.
  These are observed lab workarounds, not application requirements.
- D08: debug signing and the provisional app ID are for development only.
  The optimized unsigned APK verifies the release build path; production key
  ownership, application identity and upgrade validation remain release gates.

Version sources: [AGP](https://developer.android.com/build/releases/gradle-plugin),
[Media3](https://developer.android.com/jetpack/androidx/releases/media3),
[Room](https://developer.android.com/jetpack/androidx/releases/room).

## 2026-09-28: interface fidelity correction

- D09: the user requires direct recreation of the selected 8.4.1 interface.
  The reduced scaffold is rejected as the design target. Prioritize matching
  measured layout, controls and navigation before expanding unrelated features.
- D10: retain independently drawn assets and honest service states. The profile
  is local-only; unavailable online destinations are disclosed rather than
  connected to the original servers or populated with invented activity.
- D11: use in-process Kotlin compilation on this Windows workspace. A previously
  sandboxed Gradle daemon retained restricted access to Kotlin's user-directory
  markers and a cached compiler dependency; a fresh process resolved that build
  environment issue. No global JDK or SDK permissions were changed.


## D12 — Page navigation and local service state (2026-09-28)

Replace destination dialogs with pages and preserve the source page beneath the full player. Queue and contextual/native chooser flows remain overlays. Covering a page makes its base views invisible, preventing touch-through and duplicate accessibility targets. Routes and search text are saved for Activity recreation.

Recent play is recorded by the playback service, rather than approximated from the current queue. Sleep timers also belong to the service; their deadline survives leaving the Activity and pauses audio. These URI-based preferences are bounded development storage, not a completed organization model. Online, skin-import and EQ destinations disclose unsupported capabilities.

## D13 — Private-use boomtrack identity (2026-10-21)

Use `boomtrack` as the visible application and service identity. Retain
`org.ttpod.recreation.dev` as the application ID so the existing development installation
upgrades instead of becoming a second app. A public release may choose a new package ID,
but that would require an explicit migration or a clean install. This decision does not
imply ownership of the historical TTPod name or assets.

## D14 — Local-only active scope (2026-10-26)

Defer online catalogue, streaming and download providers. The active product is a reliable
offline player for media already owned or selected by the user. Remove the Internet
permission and the online-lyrics action from the UI. Preserve unavailable historical tabs
only where they are needed for interface fidelity, routing them to useful local actions.
Add persistent folder indexing through Android's Storage Access Framework; do not copy a
whole collection into app-private storage merely to index it. Clarification on 2026-10-26:
this defers online music only. Explicit LRCLIB lookup remains in scope, and downloadable
skins may be added from an original or redistribution-authorized catalog.
