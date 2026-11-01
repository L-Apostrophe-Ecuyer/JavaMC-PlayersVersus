#!/usr/bin/env python3
"""The watchling's textures: vanilla's enderman with a creaking's yellow eyes, its spawn egg, and the static it puts
on the screen of whoever looks at it.

Usage: watchling.py <vanilla dir> <assets/players-versus/textures dir>

<vanilla dir> holds vanilla's enderman.png, enderman_eyes.png and creeper_spawn_egg.png (the api-diff workflow's
"asset" focus lines print them).

- The body: the enderman's, its white eyes a pale yellow.
- The glowing eyes: the enderman's purple eye strips in the creaking's colours, yellow at the middle, orange about it
  (the pale spider's eyes have the same orange).
- The egg: the creeper egg's shape, black with yellow spots.
- The static: grey snow, every pixel its own shade and mostly opaque, seeded so it doesn't change between runs.
"""
import random
import sys
from pathlib import Path

from PIL import Image

EYE_WHITE = (255, 236, 160)
EYE_CORE, EYE_EDGE = (255, 226, 102), (239, 163, 55)
EGG_BASE, EGG_SPOTS = (24, 22, 26), (242, 201, 76)


def luminance(rgb):
    r, g, b = rgb[:3]
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255.0


def body(image):
    out = image.convert("RGBA").copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = pixels[x, y]
            if a and r > 200 and g > 200 and b > 200:
                pixels[x, y] = EYE_WHITE + (a,)
    return out


def eyes(image):
    out = image.convert("RGBA").copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            # The strips' middle has no green; their ends are lighter
            pixels[x, y] = (EYE_CORE if g < 60 else EYE_EDGE) + (a,)
    return out


def egg(image):
    out = image.convert("RGBA").copy()
    pixels = out.load()
    shades = [luminance(p) for p in out.get_flattened_data() if p[3] > 0]
    low, high = min(shades), max(shades)
    for y in range(out.height):
        for x in range(out.width):
            p = pixels[x, y]
            if p[3] == 0:
                continue
            fraction = (luminance(p) - low) / (high - low or 1.0)
            if fraction < 0.12:
                continue  # the outline
            # The creeper egg is green with darker spots: the green turns black, the spots yellow
            spot = p[1] < p[0] + 30 and fraction < 0.45
            base = EGG_SPOTS if spot else EGG_BASE
            shade = 0.75 + 0.5 * fraction
            pixels[x, y] = tuple(min(255, round(c * shade)) for c in base) + (p[3],)
    return out


def static(size=128, seed=7):
    rng = random.Random(seed)
    out = Image.new("RGBA", (size, size))
    pixels = out.load()
    for y in range(size):
        for x in range(size):
            v = rng.randrange(40, 256)
            pixels[x, y] = (v, v, v, rng.randrange(170, 256))
    return out


def main(vanilla, textures):
    vanilla, textures = Path(vanilla), Path(textures)
    for directory in ("entity", "item", "gui"):
        (textures / directory).mkdir(parents=True, exist_ok=True)
    body(Image.open(vanilla / "enderman.png")).save(textures / "entity" / "watchling.png")
    eyes(Image.open(vanilla / "enderman_eyes.png")).save(textures / "entity" / "watchling_eyes.png")
    egg(Image.open(vanilla / "creeper_spawn_egg.png")).save(textures / "item" / "watchling_spawn_egg.png")
    static().save(textures / "gui" / "watchling_static.png")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
