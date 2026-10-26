# Local test fixtures

Run `python tools/generate-fixtures.py`. It creates `TwinkleDemo.wav` and a UTF-8
`TwinkleDemo.lrc` under ignored `reference-private/media/`.

This is an original synthesized instrumental rendering of the traditional melody,
not a recording copied from a commercial release. It uses known score timings:
1.2 seconds of lead-in, 0.6-second beats, six 4.8-second phrases repeated three
times, total 87.6 seconds. Lyrics are the traditional public-domain first verse.
The generated file has no embedded album image; it exercises the original fallback.

Tests use cues computed from the score, including signed file/user offsets. The
earlier private Daisy Bell download remains reference-lab media only; it is not
bundled in the application. Expanded format/metadata fixtures from the approved
plan remain to be added before G2/G3 format and scale acceptance.
