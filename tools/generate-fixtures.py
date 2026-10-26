"""Generate an original instrumental test rendering and exactly timed LRC. No packages needed."""
from pathlib import Path
import math
import struct
import wave

root = Path(__file__).resolve().parent.parent / "reference-private" / "media"
root.mkdir(parents=True, exist_ok=True)
sample_rate = 22050
beat = 0.6
notes = {"C": 261.63, "D": 293.66, "E": 329.63, "F": 349.23, "G": 392.0, "A": 440.0}
score = [
    ("CCGGAAG", "Twinkle, twinkle, little star"),
    ("FFEEDDC", "How I wonder what you are"),
    ("GGFFEED", "Up above the world so high"),
    ("GGFFEED", "Like a diamond in the sky"),
    ("CCGGAAG", "Twinkle, twinkle, little star"),
    ("FFEEDDC", "How I wonder what you are"),
]
elapsed = 1.2
lyrics = ["[ti:TwinkleDemo]", "[ar:Reference Demo]", "[00:00.00]Instrumental lyric timing demo"]
with wave.open(str(root / "TwinkleDemo.wav"), "wb") as output:
    output.setparams((1, 2, sample_rate, 0, "NONE", "not compressed"))
    output.writeframes(bytes(round(sample_rate * elapsed) * 2))
    for _ in range(3):
        for melody, words in score:
            lyrics.append(f"[{int(elapsed // 60):02d}:{elapsed % 60:05.2f}]{words}")
            for index, note in enumerate(melody):
                duration = beat * (2 if index == 6 else 1)
                samples = bytearray()
                for i in range(round(sample_rate * duration)):
                    t = i / sample_rate
                    envelope = min(t / 0.015, 1) * math.exp(-3 * t) * min((duration - t) / 0.04, 1)
                    value = 0.22 * envelope * (math.sin(2 * math.pi * notes[note] * t) + 0.25 * math.sin(4 * math.pi * notes[note] * t))
                    samples.extend(struct.pack("<h", round(value * 32767)))
                output.writeframes(samples)
                elapsed += duration
(root / "TwinkleDemo.lrc").write_text("\n".join(lyrics) + "\n", encoding="utf-8")
print(f"Created {elapsed:.1f}s WAV and matching LRC in {root}")
