#!/usr/bin/env python3
"""The pale wolf's howls (assets/players-versus/sounds/pale_wolf/howl1-3.ogg): mono, about three seconds each.

Usage: pale-wolf-howl.py <assets/players-versus dir>   (needs numpy and ffmpeg with libvorbis)

Vanilla's wolves no longer howl (26.4 ships no howl sounds), so the pale wolf's are made here. Each is a wolf's howl as
it carries through a forest at night, gone a little hollow:
- A voice: a fundamental gliding up from low (the howl's swell), held with a slow drift and a vibrato, then falling
  away; its overtones (an "oo" vowel: strong second and third, weak above), and a second voice a hair off pitch
  beside it, as the pale wolf's hollowness.
- Breath: a little band-passed noise, most at the swell.
- The forest: echoes off the trees (a handful of fading delays), so it carries and lingers.
The three differ in pitch, length and shape: a long rising one, one that breaks upward at its height, a short low one.
"""
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import numpy as np

RATE = 44100


def smooth(x):
    """Smoothstep of x clipped to 0..1."""
    x = np.clip(x, 0.0, 1.0)
    return x * x * (3 - 2 * x)


def contour(t, low, high, end, swell, fall, length, brk=None):
    """The fundamental through the howl: up from low to high over the swell, held, down to end over the fall."""
    f = low + (high - low) * smooth(t / swell)
    if brk is not None:
        at, rise = brk
        f = f + rise * smooth((t - at) / 0.12) * (1 - smooth((t - at - 0.5) / 0.3))
    f = f + (end - high) * smooth((t - (length - fall)) / fall)
    drift = 1 + 0.012 * np.sin(2 * np.pi * 0.7 * t + 0.4)
    vibrato = 1 + 0.014 * np.sin(2 * np.pi * 5.2 * t) * smooth((t - swell * 0.8) / 0.4)
    return f * drift * vibrato


def voice(f, rng):
    phase = 2 * np.pi * np.cumsum(f) / RATE
    tone = (np.sin(phase) + 0.45 * np.sin(2 * phase + 0.3) + 0.22 * np.sin(3 * phase + 1.1)
            + 0.07 * np.sin(4 * phase + 2.0) + 0.03 * np.sin(5 * phase + 0.7))
    return tone


def band_noise(n, rng, low, high):
    spectrum = np.fft.rfft(rng.standard_normal(n))
    freqs = np.fft.rfftfreq(n, 1 / RATE)
    spectrum[(freqs < low) | (freqs > high)] = 0
    noise = np.fft.irfft(spectrum, n)
    return noise / (np.abs(noise).max() + 1e-9)


def echoes(signal, taps):
    out = np.copy(signal)
    for delay, gain in taps:
        d = int(delay * RATE)
        out[d:] += gain * signal[:-d]
    return out


def howl(seed, low, high, end, length, swell, fall, brk=None):
    rng = np.random.default_rng(seed)
    tail = 1.2
    n = int(RATE * (length + tail))
    t = np.arange(n) / RATE
    f = contour(t, low, high, end, swell, fall, length, brk)
    envelope = smooth(t / (swell * 0.7)) * (1 - smooth((t - (length - fall * 0.9)) / (fall * 0.9)))
    tremolo = 1 + 0.06 * np.sin(2 * np.pi * 3.1 * t)
    sound = voice(f, rng) + 0.35 * voice(f * 1.004, rng)
    sound = sound * envelope * tremolo
    breath = band_noise(n, rng, 600, 3200) * envelope * (0.05 + 0.08 * (1 - smooth(t / (swell * 1.5))))
    dry = sound + breath
    wet = echoes(dry, [(0.13, 0.32), (0.21, 0.24), (0.34, 0.17), (0.47, 0.11), (0.66, 0.07), (0.88, 0.04)])
    out = 0.75 * dry + 0.45 * wet
    # A soft edge on the very end of the tail
    out *= 1 - smooth((t - (length + tail - 0.3)) / 0.3)
    return out / np.abs(out).max() * 0.89


HOWLS = {
    "howl1": dict(seed=3, low=300, high=540, end=380, length=3.0, swell=0.7, fall=0.9),
    "howl2": dict(seed=7, low=320, high=500, end=330, length=2.8, swell=0.5, fall=0.8, brk=(1.1, 70)),
    "howl3": dict(seed=13, low=250, high=430, end=300, length=2.2, swell=0.6, fall=0.7),
}


def write_ogg(samples, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "howl.wav"
        with wave.open(str(wav), "wb") as out:
            out.setnchannels(1)
            out.setsampwidth(2)
            out.setframerate(RATE)
            out.writeframes((samples * 32767).astype(np.int16).tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "4", str(path)], check=True)


def main(assets):
    for name, shape in HOWLS.items():
        write_ogg(howl(**shape), Path(assets) / "sounds" / "pale_wolf" / f"{name}.ogg")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
