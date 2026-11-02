#!/usr/bin/env python3
"""The watchling's spawn egg, and the static it puts on the screen of whoever looks at it. (Its skin and glowing eyes
are tools/watchling-skin.py's.)

Usage: watchling.py <vanilla dir> <assets/players-versus/textures dir>

<vanilla dir> holds vanilla's creeper_spawn_egg.png (the api-diff workflow's "asset" focus lines print it).

- The egg: the creeper egg's shape, the watchling's plum with yellow spots (its eyes).
- The static: grey snow, every pixel its own shade and mostly opaque, seeded so it doesn't change between runs.
"""
import random
import sys
from pathlib import Path

from PIL import Image

EGG_BASE, EGG_SPOTS = (64, 46, 64), (242, 201, 76)


def luminance(rgb):
    r, g, b = rgb[:3]
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255.0


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
    for directory in ("item", "gui"):
        (textures / directory).mkdir(parents=True, exist_ok=True)
    egg(Image.open(vanilla / "creeper_spawn_egg.png")).save(textures / "item" / "watchling_spawn_egg.png")
    static().save(textures / "gui" / "watchling_static.png")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
