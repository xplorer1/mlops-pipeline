# boomtrack

A private, offline-first Android music player based on the remembered TTPod 8.4.1
local-player experience. Original TTPod code, APKs and artwork are not part of the app.
Online music catalogue and audio downloads are outside the current product scope.
Network access is limited to explicit lyrics and future authorized skin downloads.

- [Approved implementation plan](docs/IMPLEMENTATION_PLAN.md)
- [Independent plan reviews](docs/PLAN_REVIEW.md)
- [Build and run from CMD](docs/BUILD.md)
- [Implementation progress and open gates](docs/PROGRESS.md)
- [8.4.1 interface comparison and remaining differences](docs/acceptance/UI_FIDELITY.md)
- [Reference baseline](docs/REFERENCE_VERSION.md)

Three modules: `domain` (pure Kotlin parser/state), `playback` (Media3 service),
and `app` (Views, library access and Room). The product name is boomtrack. The
development package identity and release signing remain provisional; this repository
does not claim an official TTPod affiliation.

Music can be discovered through Android MediaStore, selected as individual files,
or indexed recursively from a user-granted folder. boomtrack stores document references
and organization data; it does not copy a selected music collection into private storage.
