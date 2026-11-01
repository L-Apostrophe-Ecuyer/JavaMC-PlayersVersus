#!/usr/bin/env python3
"""The pale villagers' textures: villagers the withering took, half villager, half illager.

Usage: pale-villager.py <vanilla dir> <assets/players-versus/textures dir>

<vanilla dir> holds vanilla's villager.png and pillager.png, and when there villager_baby.png, villager_spawn_egg.png,
wandering_trader.png and wandering_trader_spawn_egg.png (the api-diff workflow's "asset" focus lines print them).

- The skin: the villager's warm browns go the illager's ashen grey, a little paler than an illager's, each shade kept.
- The face, on the villager's head (the same layout as the illager's): the illager's scowling unibrow, its eyes (the
  whites kept, the irises a sickly green between the villager's and the pillager's) and its grim mouth, in the ashen
  palette. The villager's big nose stays, greyed.
- The clothes stay the villager's own: the profession and biome clothes are drawn over the skin (VillagerProfessionLayer),
  and the robe's greys aren't touched. The wandering trader keeps its blue robe.
- The children (whose texture has another layout) and the eggs (the villager's and the trader's heads) only grey.
"""
import sys
from pathlib import Path

from PIL import Image

SKIN = (164, 168, 167)          # the ashen skin at the villager's lightest skin shade
SKIN_LUMINANCE = 0.6            # the villager's lightest skin shade's luminance
BROW, BROW_DARK = (58, 54, 52), (40, 37, 36)
IRIS = (44, 112, 62)
MOUTH = (92, 90, 89)
FACE = (8, 11, 16, 18)          # the head's front: x from, y from, x to, y to (the brow, eyes and mouth rows)


def luminance(rgb):
    r, g, b = rgb[:3]
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255.0


def ashen(rgb):
    """A warm skin or brown pixel in the ashen palette, or None for anything else (clothes, eyes)."""
    r, g, b = rgb[:3]
    if not (r > g >= b - 4 and r - b > 25):
        return None
    lum = luminance(rgb)
    if lum <= 0.3:
        v = round(lum * 255 * 0.95)
        return (v, v, v)
    scale = lum / SKIN_LUMINANCE
    return tuple(max(0, min(255, round(c * scale))) for c in SKIN)


def recolour(image):
    out = image.convert("RGBA").copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            p = pixels[x, y]
            if p[3] == 0:
                continue
            new = ashen(p)
            if new is not None:
                pixels[x, y] = new + (p[3],)
    return out


def illager_face(image, illager):
    """The illager's brow, eyes and mouth over the (already ashen) villager head's front."""
    out = image.copy()
    pixels, features = out.load(), illager.convert("RGBA").load()
    x0, y0, x1, y1 = FACE
    for y in range(y0, y1):
        for x in range(x0, x1):
            r, g, b, a = features[x, y]
            if a == 0:
                continue
            lum = luminance((r, g, b))
            if r > 240 and g > 240 and b > 240:
                pixels[x, y] = (236, 236, 232, 255)
            elif g > r + 20 or b > r + 20:
                pixels[x, y] = IRIS + (255,)
            elif lum < 0.2:
                pixels[x, y] = (BROW_DARK if lum < 0.17 else BROW) + (255,)
            elif lum < 0.4:
                pixels[x, y] = MOUTH + (255,)
    return out


def main(vanilla, textures):
    vanilla, textures = Path(vanilla), Path(textures)
    (textures / "entity").mkdir(parents=True, exist_ok=True)
    (textures / "item").mkdir(parents=True, exist_ok=True)
    illager = Image.open(vanilla / "pillager.png")
    illager_face(recolour(Image.open(vanilla / "villager.png")), illager).save(textures / "entity" / "pale_villager.png")
    if (vanilla / "wandering_trader.png").exists():
        illager_face(recolour(Image.open(vanilla / "wandering_trader.png")), illager).save(textures / "entity" / "pale_wandering_trader.png")
    for source, target in [("villager_baby.png", "entity/pale_villager_baby.png"),
                           ("villager_spawn_egg.png", "item/pale_villager_spawn_egg.png"),
                           ("wandering_trader_spawn_egg.png", "item/pale_wandering_trader_spawn_egg.png")]:
        if (vanilla / source).exists():
            recolour(Image.open(vanilla / source)).save(textures / target)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
