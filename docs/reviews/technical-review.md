# Independent technical review

Reviewer: technical expert agent, independent of plan author  
Date: 2026-09-27  
Plan: `docs/IMPLEMENTATION_PLAN.md`  
Revision reviewed: **1.0 — review draft**  
SHA-256: `A1701C8319DA406489703534EA05E24C245831390B6BB1CE2005C54EB9C64E67`  
Verdict: **REQUEST CHANGES**

## Blocking finding

**T1 — Section 7, local lyric implementation: imported file offset has the wrong sign.** The proposed `raw timestamp + file offset + user offset` applies positive file offset as delay. Established LRC parser documentation treats a positive `[offset:]` value as making lyrics appear sooner. A user-facing delay adjustment can use the opposite sign, but must not silently redefine imported file metadata. See the primary [paroles parser documentation](https://github.com/Clarkkkk/paroles) and [lrc-kit documentation](https://github.com/weirongxu/lrc-kit/blob/master/README.md).

Concrete fix: specify milliseconds, use `effective cue = raw timestamp - fileOffset + userDelay`, retain positive user delay as later presentation, and require independent fixtures for both controls and their composition. A 10-second raw cue with file offset +500 is 9.5 seconds; with file offset -500 it is 10.5 seconds; with file offset zero and user delay +500 it is 10.5 seconds. Specify handling for effective negative cues, and test import/export normalization without applying an offset twice.

## Nonblocking suggestions

- Sections 6 and 12: before implementing the sleep timer, settle whether paused time counts toward its deadline. Monotonic clock time and accumulated playing time are different product behaviors; either can be implemented reliably when explicit.
- Sections 11–13: add a signed-upgrade installation test before release, preserving application ID/signing identity, existing DB state, selections, and granted URIs. Keep signing keys out of the repository. Database migration tests alone do not establish an installable upgrade path.

## Assessment

The three-module architecture is proportionate and permits a service independent of Activity lifetime. The playback/session authority, bounded invalid-track handling, paused recovery, queue-entry identities, and migration policy are coherent. Storage correctly distinguishes audio permissions from sidecar access, includes a denied-permission picker route, and avoids assuming MediaStore IDs or SAF permissions remain stable forever.

The DSP spike is necessary rather than ceremonial: actual PCM representation, custom processor integration, offload bypass, reset behavior, and physical energy/audio measurements must close its gate. The plan correctly avoids promising proprietary acoustic equivalence. Skin parsing limits and staging are credible. Download publication explicitly handles the non-atomic Room/MediaStore boundary, durable user-visible destinations, resource identity on resume, and version-appropriate execution.

The original handoff's 1.0 obligations remain represented, including authorized downloads, floating lyrics, primary visualization, skins, and offline survival. Provider selection and physical hardware remain unresolved implementation gates rather than falsely completed facts. The 45–72-day range is plausible as a planning range with the stated contingency and re-estimation, not a delivery commitment.

I checked current [official Media3 release notes](https://developer.android.com/jetpack/androidx/releases/media3): stable 1.11.1 is listed, and Media3 raised its minimum SDK to 23 in 1.9.0. The proposed minimum is therefore compatible in principle, while the full dependency/toolchain and install gates remain necessary. No production code or actual build was reviewed or certified.

Approval is withheld only for T1. After correction, review the complete final document and issue approval against its new exact byte hash; this record does not approve a future revision automatically.

## Round two: final technical verdict

Revision reviewed: **1.1 — review candidate**  
SHA-256 independently verified: `DB75AAD5E23808C494916425E635D698EB57F99A7E48A83E384B9346C42375EA`  
Verdict: **APPROVE**  
Unresolved technical blockers: **None**

I reread the revised plan, including its expanded identity and provider gates. T1 is resolved: file metadata and user delay now have distinct signs, milliseconds are explicit, positive/negative/composed fixtures are specified, negative effective cue times are handled, and each adjustment is applied once. The sleep-timer behavior is explicit. The signed-upgrade test and owner-controlled signing-key custody address both nonblocking suggestions.

The added requirement to complete every agreed primary visualizer/stage mode has a defined M0 decision, M6 implementation location, G7 coverage, and an estimate adjustment if the set exceeds assumptions. The four provider journeys now distinguish streaming from download rights and prove permitted offline lyric/art persistence. These changes strengthen acceptance without undermining the technical boundaries.

This approval applies only to the exact revision and hash above. It approves the implementation plan and its gates, not completed software, actual device compatibility, provider availability, or a delivery guarantee. Any change to the plan bytes requires renewed review as specified in Section 15.
