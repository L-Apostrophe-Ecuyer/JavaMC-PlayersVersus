#!/usr/bin/env python3
"""The pale villager's textures: vanilla's villager with its skin gone white, and its spawn egg.

Usage: pale-villager.py <vanilla dir> <assets/players-versus/textures dir>

<vanilla dir> holds vanilla's villager.png, and when there villager_baby.png and villager_spawn_egg.png (the api-diff
workflow's "asset" focus lines print them).

- The skin (the villager's warm browns) goes a cold white, each shade kept: lighter skin whiter, the shadows grey.
- The dark browns (brows, mouth) go dark grey; the eyes and the robe's greys stay. The clothes are vanilla's own
  (VillagerProfessionLayer draws the type and profession over the base skin), so they stay as they were.
- The egg: vanilla's villager egg (the villager's head) the same way.
"""
import sys
from pathlib import Path

from PIL import Image


def luminance(rgb):
    r, g, b = rgb[:3]
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255.0


def pale(rgb):
    r, g, b = rgb[:3]
    warm = r > g >= b - 4 and r - b > 25
    if not warm:
        return None
    lum = luminance(rgb)
    if lum <= 0.3:
        v = round(lum * 255 * 0.95)
        return (v, v, v + 2)
    v = round(190 + (lum - 0.42) / 0.18 * 42)
    v = max(120, min(244, v))
    return (v - 3, v, min(255, v + 4))


def recolour(image):
    out = image.convert("RGBA").copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            p = pixels[x, y]
            if p[3] == 0:
                continue
            new = pale(p)
            if new is not None:
                pixels[x, y] = new + (p[3],)
    return out


def main(vanilla, textures):
    vanilla, textures = Path(vanilla), Path(textures)
    (textures / "entity").mkdir(parents=True, exist_ok=True)
    (textures / "item").mkdir(parents=True, exist_ok=True)
    recolour(Image.open(vanilla / "villager.png")).save(textures / "entity" / "pale_villager.png")
    if (vanilla / "villager_baby.png").exists():
        recolour(Image.open(vanilla / "villager_baby.png")).save(textures / "entity" / "pale_villager_baby.png")
    if (vanilla / "villager_spawn_egg.png").exists():
        recolour(Image.open(vanilla / "villager_spawn_egg.png")).save(textures / "item" / "pale_villager_spawn_egg.png")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
