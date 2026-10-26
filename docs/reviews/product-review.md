# Independent product review

Reviewer role: product expert  
Date: 2026-09-27  
Final verdict: **APPROVE**  
Approved plan revision: **1.1**  
Approved file: `docs/IMPLEMENTATION_PLAN.md`  
SHA-256: `DB75AAD5E23808C494916425E635D698EB57F99A7E48A83E384B9346C42375EA`

## Scope

Reviewed the complete implementation plan against the original engineering handoff and the user's selection of TTPod 8.4.1, confirmed playback, and decision to rebuild unavailable lyric/skin functionality. Assessed preservation fidelity, release sequencing, journeys and recovery, accessibility, honest parity claims, ownership of decisions, effort assumptions, and external dependencies. This is approval of a plan with future acceptance gates, not certification of implemented software or provider availability.

## Round one: request changes

Revision 1.0 SHA-256: `A1701C8319DA406489703534EA05E24C245831390B6BB1CE2005C54EB9C64E67`.

| Finding | Severity | Required correction | Resolution in 1.1 |
| --- | --- | --- | --- |
| P1: Sections 3/9/12 permitted one reconstructed visualizer to satisfy preservation release while asserting the original primary classic visualizer/stage obligation was retained | Blocking | Identify the required primary modes with evidence or reconstruction briefs; user agrees the identity set; final release checks all agreed modes | Sections 3/4 establish the set and G0 user decision; section 9 separates beta's first mode from the complete set; M6/G7 and section 13 require all modes. Section 14 states the estimate's one-to-three-mode assumption and requires re-estimation if larger |
| P2: Sections 3/10/12 required lyric/art lookup and search/stream/download, but the real-provider release gate only demonstrated download | Blocking | Require real lookup, permitted persistence and offline presentation for lyrics/art, plus streaming and durable download acceptance; otherwise explicitly revise scope | Section 10 specifies four real-provider journeys and durable-storage evidence; G5/G7 and section 13 require every journey |
| Legacy destinations needed explicit mapping | Nonblocking | Include retained/replaced/deferred/omitted destinations in M0 navigation work | Section 4 and M0 now explicitly require the mapping |
| Alpha lyric failure recovery needed an end-to-end check | Nonblocking | Demonstrate failed import, preserved selection, successful replacement and timed display | Section 7 and G2 now require this journey |

## Final assessment

Both blockers are resolved. No unresolved product blockers remain in the reviewed revision.

The plan preserves a recognizable local player as the first useful result, rather than beginning with a catalog. It distinguishes observations from reconstructions, treats accessibility deviations explicitly, preserves user data during recovery, and prevents a generic spectrum effect from silently satisfying preservation identity. Alpha, beta, and 1.0 labels now have enforceable and appropriately different commitments.

The provider risk is visible and bounded: a limited authorized catalog can demonstrate workflows without implying commercial-catalog equivalence, but missing required provider paths keep full 1.0 open. Physical listening and user fidelity checks remain future gates. The engineering estimate is an assumption-based range, with re-estimation tied to evidence and implementation throughput.

Approval applies only to the checksum above. Any change to the plan bytes requires both reviewers to review the new revision. The original handoff was not edited, and no application implementation was performed as part of this review.
