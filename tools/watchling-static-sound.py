#!/usr/bin/env python3
"""The watchling's static sound (assets/players-versus/sounds/watchling/static.ogg): a 2-second loop, mono.

Usage: watchling-static-sound.py <assets/players-versus dir>   (needs numpy and ffmpeg with libvorbis)

- A hiss: white noise, its lows cut, warbling (its loudness swaying a few times a second), as a dead channel's snow.
- Crackles: sharp clicks scattered through it, about 30 a second.
- Under it a drone: 55 and 57.5 Hz together, beating slowly against each other, with a little of their octave.
Every sway and tone turns a whole number of times in the 2 seconds, so the loop joins seamlessly. The client plays it
looped, louder and higher the thicker the static (WatchlingStaticSound).
"""
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import numpy as np

RATE, SECONDS, SEED = 44100, 2.0, 11


def static():
    rng = np.random.default_rng(SEED)
    n = int(RATE * SECONDS)
    t = np.arange(n) / RATE
    noise = rng.standard_normal(n)
    # Cut the lows: a first-order high-pass at about 700 Hz, run twice round so the loop's ends agree
    alpha = 1.0 / (1.0 + 2 * np.pi * 700 / RATE)
    hiss = np.zeros(n)
    previous_in = previous_out = 0.0
    for _ in range(2):
        for i in range(n):
            previous_out = alpha * (previous_out + noise[i] - previous_in)
            previous_in = noise[i]
            hiss[i] = previous_out
    hiss /= np.max(np.abs(hiss))
    sway = 1.0 + 0.35 * (np.sin(2 * np.pi * 3.5 * t) * 0.5 + np.sin(2 * np.pi * 6.5 * t + 1.0) * 0.3 + np.sin(2 * np.pi * 11.5 * t + 2.0) * 0.2)
    sound = 0.4 * hiss * sway
    # Crackles, none at the very ends
    click = np.exp(-np.arange(int(0.003 * RATE)) / (0.0006 * RATE))
    for start in rng.integers(len(click), n - 2 * len(click), size=int(30 * SECONDS)):
        sound[start:start + len(click)] += rng.choice([-1.0, 1.0]) * rng.uniform(0.3, 0.7) * click
    drone = 0.16 * np.sin(2 * np.pi * 55.0 * t) + 0.16 * np.sin(2 * np.pi * 57.5 * t) + 0.05 * np.sin(2 * np.pi * 110.0 * t)
    sound += drone
    return 0.9 * sound / np.max(np.abs(sound))


def main(assets):
    out = Path(assets) / "sounds" / "watchling" / "static.ogg"
    out.parent.mkdir(parents=True, exist_ok=True)
    samples = (static() * 32767).astype(np.int16)
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "static.wav"
        with wave.open(str(wav), "wb") as file:
            file.setnchannels(1)
            file.setsampwidth(2)
            file.setframerate(RATE)
            file.writeframes(samples.tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "4", str(out)], check=True)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
