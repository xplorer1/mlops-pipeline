# TTPod Resurrection Project
## Engineering Handoff / Implementation Specification
**Document version:** 1.0  
**Target:** Modern Android clean-room recreation of classic late-era TTPod  
**Reference era:** TTPod 8.4.x / 9.0.x Android, with 10.0.7 used as secondary archaeology  
**Primary product values:** faithful, offline-first, lightweight, fast, customizable, durable, open source

---

# 1. Mission

Recreate the Android TTPod experience as faithfully as practical while making the application safe, maintainable, open source, and compatible with modern Android.

This is not “a modern music app inspired by TTPod.” The goal is preservation-level behavioral fidelity:

- local/offline music playback
- fast library scanning and folder browsing
- online song search through legally authorized providers
- streaming when a provider permits it
- downloading when a provider permits downloads
- downloaded songs becoming normal offline/local songs
- synchronized lyrics
- automatic lyric retrieval
- album/artist artwork
- animated lyric/player presentation
- visualizers
- EQ and sound effects
- skins/themes
- floating/desktop-style lyrics where Android permits
- playlists, favorites, queue, playback modes
- sleep timer
- headset/Bluetooth controls
- widgets
- tag editing where modern storage rules permit
- landscape/stage presentation
- persistent playback/resume state

The reconstructed application must remain useful with **zero internet connection**.

Core rule:

> Opening and playing a local song must never depend on a network request, account, analytics SDK, remote configuration service, or web view.

---

# 2. What “exact recreation” means

There are two different kinds of fidelity.

## 2.1 Player fidelity

The local application can be recreated extremely closely:

- navigation structure
- screen layout
- typography scale
- icon placement
- album-art treatment
- controls
- list density
- gestures
- transitions
- animation timing
- lyric scrolling
- visualizer behavior
- playback rules
- queue behavior
- scan behavior
- theme/skin behavior
- EQ UI
- settings
- widgets
- offline behavior

These should be measured from the reference APK rather than guessed.

## 2.2 Online-service fidelity

The original TTPod servers/catalogs no longer exist as a usable public service. Therefore the remake must reproduce the **workflow** rather than illegally reproduce an old catalog:

`Search -> inspect result -> play/preview -> choose quality -> download -> appear in local library`

Implement online music as a provider interface. A provider may expose:

- search
- metadata
- artwork
- lyrics
- stream URL
- downloadable variants
- artist/album information

Only integrate services whose API and content rights authorize the requested use. Do not scrape protected services, bypass DRM, or implement unauthorized downloading.

---

# 3. Reference baseline

The remembered UI is closest to later Android TTPod rather than the very early 3.x-era design.

Use this hierarchy:

1. **TTPod 8.4.1** — preferred “classic TTPod” behavior/UI reference.
2. **TTPod 9.0.4** — secondary reference for late-era changes.
3. **TTPod 10.0.7** — archaeology only until its behavior is understood.
4. Earlier 7.x/6.x builds — consult only when a beloved feature disappeared or changed later.

Do not let 10.0.7 redefine the product merely because it has the highest version number.

The two APKs already downloaded should be retained privately as reference material. If possible, additionally obtain a verified 8.4.1 archive copy because that version is especially useful for the intended reconstruction.

---

# 4. Clean-room and preservation rules

The project should be a clean-room reimplementation.

Allowed reference activities:

- run the old APK
- observe UI and behavior
- capture private screenshots/video for measurement
- inspect manifest/package metadata
- inspect resource dimensions/names for research
- identify file formats and externally observable protocol/data behavior where legally permitted
- record screen flows, timing, settings, and user-visible strings
- document skin package structure
- compare versions

Do **not**:

- copy decompiled Java/Kotlin source into the new project
- copy proprietary native libraries
- publish old APKs in the repository
- redistribute proprietary artwork, logos, icons, bundled skins, or fonts without permission
- present the new project as an official Alibaba/TTPod release
- commit private reference screenshots/assets to a public repository unless rights permit it

Ideal repository separation:

```text
project/
  app/
  core/
  feature/
  docs/
  tools/
  reference-private/       # gitignored
    apks/
    screenshots/
    recordings/
    notes/
    extracted-metadata/
```

`reference-private/` must be in `.gitignore`.

For public branding, use a distinct project name and newly created icon unless trademark permission is obtained. Internally, the specification can continue to call the project “TTPod Resurrection.”

---

# 5. First action: fingerprint and compare the downloaded APKs

Do not install either APK on a personal daily-use phone first.

Use an isolated emulator/test device. Begin with networking disabled because these old applications may contain dead endpoints, insecure HTTP URLs, obsolete SDKs, and broad permissions.

Create:

```text
reference-private/apks/
  uptodown/
  downloadit/
```

Place the downloaded APKs there.

Record:

```bash
sha256sum reference-private/apks/**/*.apk
```

Inspect signatures:

```bash
apksigner verify --verbose --print-certs path/to/app.apk
```

Inspect package/version metadata:

```bash
apkanalyzer manifest application-id path/to/app.apk
apkanalyzer manifest version-name path/to/app.apk
apkanalyzer manifest version-code path/to/app.apk
aapt2 dump badging path/to/app.apk
```

Inspect archive structure:

```bash
unzip -l path/to/app.apk
```

Generate an APK metadata report containing:

- file name
- source site
- SHA-256
- package name
- versionName
- versionCode
- minSdk
- targetSdk
- signing certificate SHA-256
- supported ABIs
- native libraries
- declared permissions
- activities
- services
- receivers
- providers
- asset folders
- notable bundled skin/theme resources

Known historical package lineage to verify:

```text
com.sds.android.ttpod
```

If the two APKs claim the same version but have different certificate fingerprints or materially different resources, treat that as a red flag and investigate before running them.

Do not assume an archive site's upload date is the software's original release date.

---

# 6. Reference-lab procedure

## 6.1 Emulator matrix

Try the old APK on an Android version close to its original era first.

Suggested reference environments:

- Android 4.4
- Android 5.1
- Android 6.0

Then test whether it launches on a current device/emulator.

If an APK contains ARM-only native libraries, use an emulator/device capable of running that ABI.

## 6.2 First launch with network disabled

Document:

- splash screen
- first-run prompts
- storage permissions
- home screen
- tab structure
- default skin
- default library categories
- empty state behavior
- settings hierarchy
- player layout
- rotation behavior

Add a handful of test tracks containing:

- MP3 with embedded art
- MP3 without art
- FLAC
- M4A/AAC
- WAV
- a track with an LRC file
- a track with non-Latin metadata
- a long title
- multiple-disc album metadata if possible

Use legally owned/test audio only.

## 6.3 Network-enabled observation

Only after offline behavior has been captured:

- enable network on the isolated reference environment
- observe which old network features still respond
- do not enter personal credentials
- do not depend on any surviving endpoint for the new implementation
- record error states and old workflow even when servers are dead

The objective is behavior documentation, not revival of obsolete servers.

## 6.4 Screen observation template

Create one markdown file per screen:

```text
docs/reference/screens/S20-now-playing.md
```

Template:

```markdown
# Screen ID / Name

## Entry paths
- ...

## Screenshot references
- private file names only

## Layout
- top bar height:
- bottom controls height:
- list row height:
- margins:
- art aspect ratio:
- typography:
- icon sizes:

## States
- playing
- paused
- buffering
- no artwork
- lyrics available
- lyrics unavailable
- shuffle
- repeat one
- repeat all

## Gestures
- tap:
- long press:
- swipe:
- horizontal page:
- vertical scroll:

## Animation
- property:
- duration:
- interpolator:
- loop behavior:

## Behavior
- ...

## Open questions
- ...
```

Never implement a screen from memory when the reference APK can answer the question.

---

# 7. Product principles

## 7.1 Offline first

Offline mode must support:

- app startup
- library browsing
- folders
- local search
- playback
- queue
- favorites
- playlists
- saved lyrics
- saved artwork
- EQ/effects
- visualizer
- skins
- sleep timer
- widgets

Network failures must never block local functionality.

## 7.2 Lightweight

Avoid:

- embedded browser frameworks for primary UI
- Electron-like architecture
- permanent background sync
- large analytics SDKs
- ad SDKs
- unnecessary dependency trees
- polling services
- loading remote configuration on startup

Use lazy initialization for:

- online provider client
- artwork networking
- lyric networking
- download manager UI
- optional codec pack

## 7.3 Fast

Use MediaStore as the primary local media index rather than recursively walking every directory at every launch.

Cache normalized metadata in the app database.

The UI should become usable before expensive artwork or enrichment tasks finish.

## 7.4 Durable

The project must survive loss of any single external service.

Provider integrations are adapters, not the product core.

---

# 8. Android technical baseline

As of September 2026, build the app for current Android requirements.

Recommended:

```text
language: Kotlin
compileSdk: 36
targetSdk: 36
minSdk: 23
JDK: 17 or the version required by the selected current Android Gradle Plugin
UI: XML Views + custom Views + ViewBinding
playback: AndroidX Media3 / ExoPlayer
database: AndroidX Room 2.x
async durable jobs: WorkManager when necessary
local media index: MediaStore
```

Why XML/custom Views instead of making the core UI Compose-first:

- TTPod's player is highly bespoke
- exact pixel placement is important
- custom drawing is central
- the goal includes minimizing runtime/dependency weight
- old-style transitions and tightly controlled list density are straightforward with custom Views

Do not prohibit Compose forever. It simply is not required for the first fidelity build.

At the time this specification was prepared, Media3 1.11.0 is stable. Use the current stable version available when the agent creates the project; do not use an alpha/beta build without a concrete reason.

---

# 9. Repository architecture

Use modules based on responsibility rather than one giant `app` module.

```text
root/
  app/

  core-model/
  core-database/
  core-media/
  core-library/
  core-audiofx/
  core-lyrics/
  core-artwork/
  core-provider/
  core-download/
  core-skins/
  core-settings/

  feature-home/
  feature-library/
  feature-player/
  feature-search/
  feature-downloads/
  feature-effects/
  feature-skins/
  feature-settings/

  benchmark/
  docs/
  tools/
```

If this becomes too granular early, begin with:

```text
app
core
player
online
```

and split only when module boundaries stabilize.

Do not create abstraction for abstraction's sake.

---

# 10. Playback architecture

Use one playback service as the source of truth.

```text
UI
  |
MediaController
  |
PlaybackService : MediaSessionService
  |
MediaSession
  |
ExoPlayer
  |
AudioProcessor chain / platform decoder
```

The Activity must not own playback.

Responsibilities of `PlaybackService`:

- create/release ExoPlayer
- create MediaSession
- manage queue
- expose media controls
- audio focus
- headset/Bluetooth transport controls
- notification/system media UI
- playback resumption
- save state
- attach DSP
- publish current metadata

Persist at minimum:

- queue media IDs
- current queue index
- current position
- shuffle state
- repeat mode
- selected EQ profile
- playback speed if eventually exposed
- last active playlist/source

On application UI launch, connect through `MediaController`.

---

# 11. Local library

## 11.1 Permission model

Modern Android:

- Android 13+: request `READ_MEDIA_AUDIO`
- older supported Android: request the appropriate legacy read permission where needed
- avoid broad file permissions
- use Storage Access Framework for user-selected arbitrary folders/files when appropriate

## 11.2 Scan behavior

Primary source:

```text
MediaStore.Audio.Media
```

Read:

- content URI / media ID
- title
- artist
- album
- album ID
- duration
- track number
- year
- MIME type
- size
- relative path / folder information when available
- date modified
- composer if available
- genre when available

Normalize into the app database.

Do not duplicate audio bytes.

## 11.3 Library views

Must eventually provide:

- All songs
- Artists
- Albums
- Folders
- Genres if reference version exposes them
- Playlists
- Favorites
- Recently played
- Recently added
- Downloads
- Search

Exact naming/order must be taken from the selected reference version.

## 11.4 Incremental refresh

Do not full-rescan on every launch.

Use:

- MediaStore generation/version or change observation where available
- modified timestamp comparison
- a lightweight reconciliation pass
- explicit manual “scan music” function

A 10,000-track library must not make startup feel frozen.

---

# 12. Core data model

Suggested entities.

## Track

```text
id: Long
mediaStoreId: Long?
contentUri: String
sourceType: LOCAL | DOWNLOADED | PROVIDER_STREAM
providerId: String?
providerTrackId: String?

title: String
sortTitle: String?
artistId: Long?
artistName: String
albumId: Long?
albumName: String
albumArtist: String?
genre: String?
year: Int?
trackNumber: Int?
discNumber: Int?

durationMs: Long
mimeType: String?
bitrate: Int?
sampleRate: Int?
channels: Int?

fileName: String?
relativePath: String?
fileSize: Long?
dateAdded: Long?
dateModified: Long?

artworkId: Long?
lyricId: Long?

favorite: Boolean
playCount: Long
lastPlayedAt: Long?
```

## Artist

```text
id
name
sortName
artworkId
```

## Album

```text
id
name
artistName
year
artworkId
```

## Playlist

```text
id
name
createdAt
updatedAt
type: USER | SMART | SYSTEM
```

## PlaylistEntry

```text
playlistId
trackId
position
addedAt
```

## Lyric

```text
id
trackId
format: LRC | ENHANCED_LRC | PLAIN
language
source
rawText
offsetMs
checksum
```

## Artwork

```text
id
kind: TRACK | ALBUM | ARTIST
localUri
remoteSource
etag
updatedAt
```

## Download

```text
id
provider
providerTrackId
status
qualityLabel
bitrate
targetUri
bytesDownloaded
bytesTotal
createdAt
completedAt
errorCode
```

## PlaybackHistory

```text
trackId
playedAt
completedFraction
```

## Skin

```text
id
displayName
packageUri
version
enabled
manifestVersion
```

---

# 13. Screen inventory

Use this as the initial capture checklist, not as proof that every label existed in exactly this form.

## S00 — Launch / splash

Verify:

- logo
- duration
- whether library loads behind it
- transition
- cold/warm launch difference

## S10 — Main / My Music

Capture:

- top navigation
- local categories
- mini-player placement
- badges/counts
- background
- tab behavior

## S11 — Songs

Capture:

- row height
- art size
- title/artist typography
- index/fast scroll
- sort options
- context menu
- multi-select if present

## S12 — Artists

## S13 — Albums

## S14 — Folders

Folder browsing is a first-class feature, not a hidden import screen.

## S15 — Playlists / favorites

## S20 — Now Playing

This is the highest-fidelity screen.

Capture separately:

- portrait artwork page
- portrait lyric page
- visualization page
- paused state
- playing state
- no artwork
- no lyrics
- queue open
- repeat modes
- shuffle
- progress dragging
- overflow menu
- landscape

Measure:

- artwork bounds
- progress bar
- elapsed/remaining labels
- title baseline
- artist baseline
- transport icon positions
- lyric line spacing
- active/inactive lyric treatment
- background blur/dim
- swipe thresholds
- animation timing

## S21 — Lyrics

Requirements:

- synchronized LRC
- smooth active-line following
- tap/seek interaction if reference supports it
- manual time offset
- lyric search
- replace lyric
- save local
- graceful plain-text fallback

## S22 — Artwork / image page

## S23 — Visualizer / landscape stage

Reproduce reference visualizers one by one.

Do not begin by making a generic equalizer animation and calling it done.

## S24 — Queue

Requirements:

- current-track indication
- drag reorder if reference supports it
- remove
- play next
- clear
- queue persistence

## S25 — Track actions

Possible actions to verify:

- play
- play next
- add to queue
- add to playlist
- favorite
- album
- artist
- lyrics
- artwork
- tag info/editor
- delete/remove
- share
- ringtone if historically present

Do not implement obsolete/system-invasive actions merely because an old menu had them; record them and decide compatibility safely.

## S30 — Search

One search box should eventually support:

- local music
- online provider results

The exact reference layout may separate these; reproduce whichever behavior is observed.

## S31 — Search results

Sections may include:

- songs
- albums
- artists
- local matches

## S32 — Online track/album/artist

Provider-backed.

## S33 — Download quality chooser

Recreate the old mental model:

```text
Track
  -> Download
    -> available qualities
      -> choose
        -> download progress
          -> normal offline track
```

Do not hard-code 64/128/192/320 unless the active provider actually supplies those legal variants. The UI can present quality labels dynamically.

## S34 — Downloads

States:

- queued
- downloading
- paused if supported
- completed
- failed
- retry

## S40 — Equalizer

Target TTPod-like 10-band presentation.

## S41 — Effects

Verify reference support for:

- bass
- reverb
- virtualization/spatial effect
- presets
- per-genre EQ mapping

## S42 — Skins

## S43 — Sleep timer

Must keep working with screen off.

Verify:

- fixed duration
- stop after current track
- fade out
- cancel timer
- visible countdown

## S44 — Scan/music folders

## S45 — Floating lyrics

Optional feature requiring explicit user opt-in to Android's “display over other apps” capability.

Never request this permission at first launch.

## S46 — Tag editor

Modern Android storage restrictions mean editing may require user-granted write access to a file/URI.

Support common tags where feasible:

- title
- artist
- album
- track
- year
- genre

## S50 — Settings

## S60 — Home-screen widgets

Rebuild the important classic widget sizes after the core app is stable.

---

# 14. Lyrics subsystem

Priority order for lyric resolution:

```text
1. manually selected local lyric
2. sidecar .lrc beside audio file
3. embedded synchronized lyrics if supported
4. cached remote lyric
5. remote lyric provider search
6. plain text / no lyrics state
```

File naming candidates:

```text
Song Title.lrc
Artist - Song Title.lrc
same-base-name-as-audio.lrc
```

Parser requirements:

- standard `[mm:ss.xx]`
- multiple timestamps per line
- global offset
- metadata tags
- UTF-8
- BOM handling
- malformed-line tolerance

Internal normalized representation:

```kotlin
data class LyricLine(
    val startMs: Long,
    val endMs: Long?,
    val text: String
)
```

Rendering:

- binary search current line by position
- animate only when current-line index changes
- avoid rebuilding all views every playback tick
- render current line, surrounding lines, and scroll target efficiently

Persist manual offset per lyric.

---

# 15. Artwork subsystem

Resolution order:

```text
1. user-selected artwork
2. embedded track artwork
3. local folder image
4. cached album artwork
5. cached artist artwork
6. provider lookup
7. generated/default placeholder
```

Network artwork must never block playback.

Store full-size downloaded art in cache or persistent media directory based on user choice.

Generate smaller display thumbnails for list views.

Avoid decoding 3000x3000 images for a 48dp row.

---

# 16. Audio format strategy

The historic player advertised broad format support including formats such as MP3, AAC/M4A, FLAC, WAV, ALAC, WMA, and APE in some versions.

Modern implementation:

## Tier 1 — platform/Media3

Support immediately:

- MP3
- AAC/M4A
- FLAC
- WAV
- Ogg/Vorbis
- Opus where supported

## Tier 2 — Media3 software decoders

Use Media3-supported software extensions where they improve consistency.

## Tier 3 — optional native codec module

For formats that are not reliably decoded across Android devices, create:

```text
core-codec-ffmpeg/
```

Build only required audio decoders.

Do not ship an enormous generic FFmpeg build if a small decoder configuration can provide parity.

Release App Bundles should use ABI splits so one device does not receive native libraries for every architecture.

APE/WMA compatibility must be confirmed with real files before declaring parity complete.

---

# 17. Deterministic 10-band EQ and DSP

Do not rely solely on `android.media.audiofx.Equalizer` if the goal is identical behavior on every phone. Device implementations can expose different band counts/ranges.

Preferred architecture:

```text
Decoder
 -> PCM
 -> TtpodEqAudioProcessor
 -> optional Bass Processor
 -> optional Reverb Processor
 -> visualizer tap
 -> AudioTrack output
```

Implement a deterministic 10-band software EQ using biquad filters.

Initial center-frequency candidates should be taken from the original UI/reference APK, not assumed. If the reference cannot be recovered, use a conventional 10-band set only as a temporary placeholder.

Presets should also be captured from the reference when possible.

Platform `AudioEffect` APIs may be used as:

- comparison reference
- compatibility fallback
- temporary implementation

but not as the final source of exact cross-device EQ behavior.

Virtualizer is deprecated on newer Android versions, so do not make the old platform Virtualizer API the architectural foundation.

---

# 18. Visualizer architecture

Prefer deriving visualization data from the player's own PCM pipeline instead of requesting microphone/recording permissions.

Create a visualization tap in the audio processor chain:

```text
PCM frames
 -> ring buffer
 -> windowing
 -> FFT
 -> magnitude bins
 -> visualization state
```

Advantages:

- no microphone permission
- deterministic behavior
- only visualizes this app's audio
- easier testing
- easier recreation of multiple TTPod visualizers

Render using custom Views/Canvas.

Do not update faster than the display meaningfully needs. Target approximately display refresh cadence, while keeping FFT work off the main UI path.

Each classic visualization should be implemented as a named preset with captured parameters.

---

# 19. Online search/provider system

Create provider-neutral interfaces.

Example:

```kotlin
interface MusicProvider {
    val id: String
    val displayName: String

    suspend fun search(query: String, page: Int): SearchPage
    suspend fun getTrack(id: String): ProviderTrack
    suspend fun getAlbum(id: String): ProviderAlbum?
    suspend fun getArtist(id: String): ProviderArtist?
    suspend fun getStream(id: String): StreamSource?
    suspend fun getDownloadOptions(id: String): List<DownloadOption>
}
```

Separate optional providers:

```kotlin
interface LyricsProvider
interface ArtworkProvider
```

Do not make lyrics/artwork dependent on the song catalog provider.

Provider result:

```kotlin
data class ProviderTrack(
    val provider: String,
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long?,
    val artworkUrl: String?,
    val streamable: Boolean,
    val downloadable: Boolean
)
```

Download option:

```kotlin
data class DownloadOption(
    val id: String,
    val label: String,
    val mimeType: String,
    val bitrate: Int?,
    val estimatedBytes: Long?
)
```

Providers must explicitly tell the UI whether streaming/downloading is allowed.

---

# 20. Download subsystem

User experience:

```text
Search
 -> Song
 -> Download
 -> Quality
 -> Progress
 -> Completed
 -> Visible in Downloads
 -> Visible as an offline/local track
```

Storage:

Prefer writing durable user music into shared media storage, for example a user-visible `Music/<project-name>/` collection using MediaStore.

Do not store a user's downloaded music only in app-private storage if they reasonably expect the files to survive app removal.

On completion:

1. finalize MediaStore item
2. write metadata where supported
3. trigger/rescan local database reconciliation
4. link provider ID to new local content URI
5. fetch/cache lyric and art optionally
6. expose immediately in local library

Handle:

- duplicate downloads
- interrupted downloads
- low storage
- network loss
- invalid content length
- checksum if provider supplies it
- canceled download
- app restart during download

Use WorkManager only if its reliability semantics help. Do not route actual active playback through WorkManager.

---

# 21. Skin engine

Treat skins as a major feature.

Long-term goal: determine whether classic TTPod skin packages can be parsed compatibly without redistributing proprietary skin assets.

If direct compatibility is impractical, define a modern open format.

Proposed:

```text
skin-name.ttpodskin
```

Internally ZIP:

```text
manifest.json
colors.json
layout.json
drawable/
font/
preview/
```

Example manifest:

```json
{
  "formatVersion": 1,
  "id": "org.example.skin.classic",
  "name": "Classic",
  "author": "Example",
  "minAppVersion": 1,
  "playerLayout": "layout.json"
}
```

Security rules:

- no executable code
- no JavaScript
- no native libraries
- path traversal prevention
- maximum decompressed size
- maximum image dimensions
- validate manifest before extraction
- do not allow arbitrary file writes

A skin may control:

- colors
- backgrounds
- player chrome
- icon assets
- typography choices
- lyric colors
- visualization parameters
- spacing/layout values within safe constraints

Do not let skins change application permissions or execute code.

---

# 22. Floating lyrics

Classic desktop/mini lyrics are part of the TTPod identity.

Modern implementation:

- user enables feature manually
- explain that Android requires “display over other apps”
- open the system permission screen only after user chooses the feature
- show a movable lyric overlay
- lock/unlock overlay position
- current and next line
- optional transparency
- close action
- no overlay while permission is absent

Do not request overlay permission during onboarding.

---

# 23. Playback behavior checklist

The agent must verify every item against the reference:

- Normal
- Shuffle
- Repeat all
- Repeat one
- next/previous semantics
- what previous does after N seconds into a song
- queue insertion
- play-next
- queue reordering
- behavior when current file disappears
- behavior when headphones disconnect
- Bluetooth media buttons
- wired headset buttons
- audio focus loss
- temporary focus loss
- phone-call interruption
- notification controls
- lock-screen controls
- seek
- resume after process death
- resume after device reboot if enabled
- gap between tracks
- crossfade if historically present
- playback speed if historically present
- fade-out on sleep timer

Items not found in the reference should not be silently invented as “classic TTPod.”

---

# 24. Media metadata editing

The app database must never pretend a tag edit succeeded if it only changed cached metadata.

Differentiate:

- **library override** — app-local metadata
- **file tag write** — actual audio-file modification

If writing a file tag:

- acquire proper URI write access
- write to a temporary file when necessary
- preserve audio data
- atomically replace if platform/storage model allows
- retain original on failure
- trigger MediaStore update
- validate tags after write

Create instrumentation tests using disposable sample files.

---

# 25. Settings model

Use DataStore or another lightweight persistent preference store.

Categories to reconstruct/verify:

```text
Playback
Audio effects
Lyrics
Artwork
Library / scanning
Downloads
Network
Skins / appearance
Headset controls
Shake controls
Sleep timer
Floating lyrics
Widgets
Advanced
About
```

Do not invent a giant modern settings system before capturing the reference.

---

# 26. Shake-to-change-track

If present in the selected reference:

- use accelerometer only while enabled
- unregister listener when unnecessary
- debounce aggressively
- configurable sensitivity only if reference exposes it
- disabled by default in the modern build unless user opts in

Avoid keeping sensors active continuously without reason.

---

# 27. Home-screen widgets

Implement only after playback state is stable.

Possible variants to verify from reference:

- compact controls
- artwork + controls
- lyrics widget
- larger player widget

Widgets must use the same playback service rather than implementing an independent player.

---

# 28. Performance budgets

These are engineering targets, not contractual guarantees. Measure on one defined mid-range reference device.

## Startup

- no network on critical startup path
- home UI visible quickly
- library list comes from local DB immediately
- reconciliation runs asynchronously

## Playback

- no audible interruption when opening player screen
- screen navigation must not recreate player
- artwork decoding must not block audio thread
- no allocations in tight PCM processing loops where avoidable

## Memory

- bound artwork cache
- no retaining full-size bitmaps unnecessarily
- release visualizer buffers when not visible
- avoid multiple player instances

## Battery

- no periodic background polling
- network only on user action or explicitly configured refresh
- sensor listeners only while feature is active
- correct foreground media-service lifecycle

## APK size

Track size regressions in CI.

If optional native codecs cause substantial growth, publish/ship them in a way that preserves the lightweight base experience where practical.

---

# 29. Privacy defaults

Default behavior:

- no account required
- no analytics
- no ads
- no contact access
- no location
- no microphone
- no camera
- no background network polling

Permissions should be requested at the moment the relevant feature is used.

Required/possible permissions include:

- audio library read
- internet for online features
- media foreground service
- overlay only for floating lyrics
- notifications only where applicable to user-visible non-exempt notifications

Document every permission in `docs/PERMISSIONS.md`.

---

# 30. Error handling

The application should remain pleasant when services fail.

Examples:

**Lyrics provider unavailable**

```text
Local playback continues.
Cached/local lyrics remain available.
UI offers retry later.
```

**Artwork provider unavailable**

```text
Use embedded/local/cached artwork.
No blocking spinner over playback.
```

**Music provider unavailable**

```text
Online search shows provider error.
Local search and playback still work.
```

**Download interrupted**

```text
Mark failed/paused state.
Retain safe resumable state if provider supports ranges.
Never leave a corrupt file presented as completed.
```

---

# 31. Testing strategy

## Unit tests

- LRC parser
- lyric timeline lookup
- metadata normalization
- queue operations
- shuffle determinism with seed
- repeat semantics
- search result mapping
- download state machine
- skin manifest validation
- path traversal rejection
- EQ filter coefficient generation
- FFT normalization

## Database tests

- migrations
- duplicate detection
- playlist ordering
- provider-local track linking

## Instrumentation tests

- MediaStore scan
- permission flows
- background playback
- notification controls
- Bluetooth/media-button commands where testable
- file tag writes
- shared-storage downloads

## Screenshot tests

Create modern-app screenshots at the same logical resolution/state as private reference captures.

Use image-diff tooling.

Reference screenshot files should remain private unless redistribution rights are clear.

For each screen keep:

```text
reference screenshot
implementation screenshot
diff image
notes
```

Pixel difference is more useful than arguing from memory.

## Offline acceptance suite

Run with airplane mode from cold launch:

- launch
- browse library
- open folders
- search local tracks
- play
- seek
- next/previous
- shuffle
- repeat
- lyrics
- artwork
- EQ
- visualizer
- widget
- sleep timer

All must work.

---

# 32. Compatibility test matrix

Minimum:

```text
Android 7 / API 24
Android 10 / API 29
Android 13 / API 33
Android 16 / API 36
```

Also test:

- small phone
- large phone
- 60 Hz
- high-refresh device
- light/dark system mode regardless of app skin
- portrait
- landscape
- Bluetooth headphones
- wired headset if hardware available

If minSdk 23 creates dependency or maintenance friction, raising it requires an explicit project decision rather than accidental library drift.

---

# 33. Visual-fidelity workflow

For every major screen:

1. capture reference at fixed emulator resolution
2. note density and font scale
3. reproduce same state in remake
4. screenshot remake
5. align images
6. generate pixel diff
7. correct layout
8. repeat
9. then test other densities

Do not start by “modernizing” the design.

The target is the remembered application.

Accessibility improvements can be added without replacing the visual identity:

- content descriptions
- minimum usable hit targets where possible
- scalable text option
- screen-reader semantics
- reduced-motion option

---

# 34. Suggested implementation order

## Phase A — Archaeology and specification

Deliverables:

- APK hashes/signatures
- version comparison
- screen map
- navigation graph
- feature matrix
- player screenshots
- lyric screenshots
- EQ/effects screenshots
- skin screenshots
- settings inventory
- behavior notes

Exit criterion:

The agent can describe the selected TTPod version screen-by-screen without guessing.

## Phase B — Local-player vertical slice

Implement:

- project skeleton
- MediaStore permission
- song scan
- songs list
- PlaybackService
- MediaSession
- ExoPlayer
- now-playing screen
- seek
- play/pause
- previous/next
- queue
- notification
- resume state

Exit criterion:

A user can install the app, scan local music, close the UI, and keep listening reliably.

## Phase C — Library parity

Implement:

- artists
- albums
- folders
- playlists
- favorites
- local search
- recently played/added if in reference
- scan controls

## Phase D — TTPod player identity

Implement:

- precise player layout
- lyric page
- album-art behavior
- background treatment
- swipe/page interactions
- animations
- queue UI
- landscape stage

This is the point where it should start emotionally feeling like TTPod.

## Phase E — Lyrics and artwork

Implement:

- LRC
- local lookup
- embedded art
- cached art
- provider interfaces
- lyric offset
- replacement workflows

## Phase F — DSP and visualization

Implement:

- 10-band software EQ
- presets
- bass
- reference effects
- FFT pipeline
- classic visualizers

## Phase G — Online search/download

Implement provider abstraction first.

Then one legal test/default provider.

Implement:

- online search
- stream
- download variants
- download quality UI
- download state
- MediaStore save
- local-library reconciliation

Exit criterion:

A legally downloadable provider track can travel through the same conceptual workflow remembered from TTPod.

## Phase H — Skins and floating lyrics

Implement:

- skin package
- default recreated classic skin
- import/export
- overlay lyrics
- safety validation

## Phase I — Widgets and polish

Implement:

- widgets
- sleep fade
- sensor controls
- metadata editor
- remaining settings
- parity bugs

## Phase J — Hardening/release

- compatibility testing
- benchmark
- dependency audit
- permission audit
- accessibility
- crash handling
- release signing
- open-source documentation

---

# 35. “Definition of done” for 1.0

The first real preservation release is complete only when:

1. It can function indefinitely as a local player with no server.
2. Core UI matches the selected TTPod reference closely.
3. Local songs can be browsed by songs/artists/albums/folders.
4. Playback survives screen-off/background use.
5. Queue, shuffle, repeat, seek, headset controls work.
6. Synchronized local lyrics work.
7. Artwork works offline.
8. 10-band EQ works consistently.
9. At least the primary classic visualizer/stage modes are recreated.
10. Skins are supported.
11. Floating lyrics work when explicitly enabled.
12. Downloads from an authorized provider become durable offline tracks.
13. The online provider can disappear without breaking the local player.
14. No account, ad, analytics, or network request is required to play local music.
15. Automated tests cover core parser/queue/database/download behavior.
16. The app targets current Android requirements.
17. The source repository contains no proprietary TTPod code or unlicensed assets.

---

# 36. Agent task list: begin here

The implementing agent should execute in this order.

## Task 1 — Create project records

Create:

```text
docs/PROJECT.md
docs/FEATURE_MATRIX.md
docs/REFERENCE_VERSION.md
docs/NAVIGATION.md
docs/PERMISSIONS.md
docs/ARCHITECTURE.md
docs/reference/screens/
reference-private/
```

Copy this handoff into `docs/PROJECT.md`.

## Task 2 — Analyze the two APKs

Produce:

```text
reference-private/apk-report.md
```

Include hashes, signatures, package/version info, permissions, ABIs, and notable differences.

## Task 3 — Identify the target build

Determine whether the downloaded 9.x/10.x builds match the desired remembered UI.

If not, obtain/reference 8.4.1 privately.

Set:

```text
REFERENCE_UI_VERSION=
REFERENCE_BEHAVIOR_VERSION=
```

They may be the same version.

## Task 4 — Build screen inventory

Do not write production UI yet.

Capture every reachable screen and populate `docs/reference/screens`.

## Task 5 — Create the modern Android project

Start with:

```text
compileSdk 36
targetSdk 36
minSdk 23
Kotlin
XML Views
ViewBinding
Media3
Room
```

Use stable dependencies.

## Task 6 — Implement the smallest complete playback path

```text
Permission
 -> MediaStore
 -> Songs list
 -> tap song
 -> PlaybackService
 -> Now Playing
 -> background playback
```

Nothing online is needed yet.

## Task 7 — Write tests before expanding

At minimum:

- queue behavior
- state persistence
- library normalization

Then continue through the implementation phases above.

---

# 37. Important architectural invariants

These rules should be enforced in code review.

### Invariant 1

`feature-player` does not know how online catalog APIs work.

### Invariant 2

`core-media` does not depend on Activity/Fragment classes.

### Invariant 3

No network request occurs merely because a local song starts.

### Invariant 4

Downloaded tracks have durable local URIs.

### Invariant 5

Provider track IDs are not used as the primary identity for local files.

### Invariant 6

The playback queue survives UI recreation.

### Invariant 7

Lyrics rendering does not perform network I/O.

### Invariant 8

Artwork decoding does not run on the main thread.

### Invariant 9

Skins cannot execute code.

### Invariant 10

Reference APK code/assets never enter production source by copy/paste.

---

# 38. Proposed package layout

Example:

```text
org.example.player
  app/
  data/
    db/
    mediastore/
    settings/
  playback/
    service/
    session/
    queue/
    audiofx/
  library/
    songs/
    albums/
    artists/
    folders/
    playlists/
  player/
    nowplaying/
    lyrics/
    artwork/
    visualizer/
    queue/
  online/
    provider/
    search/
    downloads/
  skins/
  widgets/
  common/
```

Use the final public namespace, not `com.sds.android.ttpod`.

---

# 39. Provisional Gradle dependency philosophy

Keep the dependency graph small.

Required categories:

```text
AndroidX core/appcompat/activity/fragment/lifecycle
Media3 common/exoplayer/session
Room runtime + KSP compiler
DataStore preferences
WorkManager only where durable work is needed
```

Networking:

Use one HTTP stack for all providers. Do not ship multiple clients accidentally.

Images:

Use one image solution. If a third-party image loader is selected, configure a bounded cache and measure its APK impact.

Avoid:

- Firebase unless a concrete feature truly requires it
- analytics SDKs
- ad SDKs
- multiple JSON libraries
- multiple DI frameworks
- unnecessary reflection-heavy frameworks

Manual constructor injection is acceptable for a project of this size until dependency wiring actually becomes painful.

---

# 40. Build flavors

Suggested:

```text
debug
release
```

Optional later:

```text
oss
provider-enabled
```

Do not let build-flavor complexity slow the first local-player vertical slice.

---

# 41. Logging

Use structured debug logging but protect user library privacy.

Do not log:

- full user file paths in release
- search history in release diagnostics
- provider authorization tokens
- private download URLs

Debug reference builds may log more locally, but logs must not be uploaded automatically.

---

# 42. Crash resilience

Playback service failure should not corrupt queue state.

Database writes for:

- queue
- playlist
- favorites
- download completion

must be transactional where appropriate.

A malformed song tag, image, LRC file, or skin package must not crash the entire library scan.

---

# 43. Migration and backups

Database schemas must use proper migrations from the beginning.

User-created durable data:

- playlists
- favorites
- custom lyric offsets
- custom lyrics
- downloaded artwork choices
- skins
- settings

should be exportable in a later release.

Do not bind the user's library organization to a cloud account.

---

# 44. Public-source project files

Eventually include:

```text
README.md
CONTRIBUTING.md
LICENSE
SECURITY.md
PRIVACY.md
docs/ARCHITECTURE.md
docs/SKIN_FORMAT.md
docs/PROVIDER_API.md
docs/REFERENCE_METHOD.md
```

`REFERENCE_METHOD.md` should explain that the project is a clean-room behavioral reimplementation and does not contain proprietary original source.

---

# 45. Open questions the agent must answer from the reference APK

Do not guess these:

1. Exact default home tabs in the selected version.
2. Exact player-page swipe direction/order.
3. Which visualizers shipped in the target version.
4. Exact EQ center frequencies and preset curves.
5. Whether EQ was global, per-track, or per-genre in that version.
6. Exact sleep-timer options.
7. Exact shake gesture behavior.
8. Exact queue semantics.
9. Whether crossfade/gapless existed in the target build.
10. Exact skin package format.
11. Exact floating-lyrics controls.
12. Whether lyric lines were tappable-to-seek.
13. Artwork transition timing.
14. Whether artist images rotate automatically.
15. Search result grouping/order.
16. Download-quality options for that build.
17. Where completed downloads appeared in the UI.
18. File naming/storage conventions.
19. How duplicate downloads were handled.
20. Which settings changed between 8.4.x, 9.0.x, and 10.0.7.

Every answer should be written into the project docs.

---

# 46. Product decisions already made

These should not be re-litigated unless implementation evidence requires change.

- Android is the first/primary platform.
- Late classic Android TTPod is the reference.
- 8.4.x/9.0.x are more important than simply using the numerically latest build.
- The app is offline-first.
- The app should be lightweight.
- Local/imported music is a first-class feature.
- Online search is a first-class feature.
- Authorized music downloading is a first-class feature.
- Downloaded tracks become offline tracks.
- Lyrics are a first-class feature.
- Artwork is a first-class feature.
- Skins are a first-class feature.
- Visualizers are a first-class feature.
- EQ/effects are a first-class feature.
- No account is required for local use.
- No ads.
- No mandatory analytics.
- No cloud dependency for local playback.
- The new implementation should be open source.
- Proprietary original code/assets are reference-only, not copied into the release.

---

# 47. Final instruction to the implementing agent

Treat TTPod as a piece of software worth preserving.

Do not “improve” its identity away.

When faced with a choice between:

- a fashionable modern music-app pattern, and
- behavior verified in the TTPod reference,

choose the verified TTPod behavior unless modern Android security/platform rules make it impossible.

When platform rules make exact behavior impossible, preserve the user workflow and document the deviation.

The first milestone is not an online catalog.

The first milestone is the moment a local song begins playing and the reconstructed player screen feels recognizably like TTPod again.

After that, restore the rest systematically.
