# Implementation plan review record

Plan: [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md)  
Date: 2026-09-27  
Overall verdict: **APPROVED by both independent reviewers**

The user requested two independent reviewers, one technical and one product,
after the draft was completed. Both must approve the same final plan bytes.
This record is separate from the plan so recording verdicts does not invalidate
the reviewed file's checksum.

## Draft reviewed

- Revision: 1.0.
- SHA-256: `A1701C8319DA406489703534EA05E24C245831390B6BB1CE2005C54EB9C64E67`.
- Technical reviewer: `/root/technical_review` — **REQUEST CHANGES**.
- Product reviewer: `/root/product_review` — **REQUEST CHANGES**.

## Findings and resolutions

| ID | Reviewer | Finding | Revision 1.1 resolution |
| --- | --- | --- | --- |
| T1 | Technical | Imported LRC offset sign would delay lyrics instead of advancing them | Separate file offset from user delay; corrected formula; positive/negative/composed fixtures and negative effective-time behavior in §7 |
| P1 | Product | One reconstructed visualizer could close a release claiming preservation of the primary classic modes | M0 defines a user-agreed named identity set; beta demonstrates one member; M6/G7 require the full agreed set, with per-mode evidence/briefs and tests |
| P2 | Product | Online scope included lyrics, art, and streaming, but G5 proved only the download path | Four real-provider journeys in §10, explicitly required by G5/G7, including permitted durable lyric/art selections and offline use |

Nonblocking suggestions also incorporated: explicit paused-time/skip behavior for
the sleep timer, signed in-place upgrade and private signing-key custody,
legacy-tab replacement mapping, and failed lyric import followed by recovery.

Revision 1.1 SHA-256:
`DB75AAD5E23808C494916425E635D698EB57F99A7E48A83E384B9346C42375EA`.
Both reviewers re-read the complete revision and independently verified this
checksum before approving it.

## Final agreement

| Reviewer | Final verdict | Reviewed revision | Unresolved blockers |
| --- | --- | --- | --- |
| Technical expert — `/root/technical_review` | **APPROVE** | 1.1, checksum above | None |
| Product expert — `/root/product_review` | **APPROVE** | 1.1, checksum above | None |

Technical final statement: "T1 is resolved; both optional suggestions are addressed.
Expanded identity and provider gates remain technically coherent. No unresolved
technical blockers."

Product final statement: "Both product blockers are resolved. The plan now requires
the complete user-agreed preservation identity set and all four real-provider
journeys before 1.0."

Full independent records, including the original requests for changes:

- [Technical review](reviews/technical-review.md)
- [Product review](reviews/product-review.md)

Both reviewers agree this exact plan is ready to execute with its stated milestone
gates. Provider authorization, hardware validation, user agreement on the identity
set, and release acceptance remain future work; they have not been certified as
complete by approving the plan. Implementation has not begun.

Any subsequent plan edit changes the reviewed bytes and requires renewed review
from both experts. The review record itself is not part of the plan checksum.
