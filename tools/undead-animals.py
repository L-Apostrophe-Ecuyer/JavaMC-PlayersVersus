#!/usr/bin/env python3
"""The zombie chicken's textures: vanilla's temperate chicken gone the zombie's green, and its spawn egg.

Usage: undead-animals.py <vanilla dir> <assets/players-versus/textures dir>

<vanilla dir> holds vanilla's chicken_temperate.png, zombie.png and chicken_spawn_egg.png (the api-diff workflow's
"asset" focus lines print them).

- The feathers (the chicken's greys and whites) take the zombie's skin green, each keeping its shade; a little greyer
  than the zombie, as dead feathers would be.
- The wattle's red dries to a dark brownish red; the beak and legs lose some of their colour; the eyes stay.
- The egg: vanilla's chicken egg (the chicken's head) the same way: its whites and greys the zombie's greens, its
  outline a dark green, its wattle dried red, its beak faded.
"""
import sys
from pathlib import Path

from PIL import Image

WATTLE = (110, 34, 30)


def luminance(rgb):
    r, g, b = rgb[:3]
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255.0


def mean_green(image):
    """The zombie skin's average green: its greenish pixels (not the shirt's cyan or the trousers' blue)."""
    pixels = [p for p in image.convert("RGBA").get_flattened_data() if p[3] > 0 and p[1] > p[0] and p[1] > p[2] + 20]
    return tuple(sum(p[i] for p in pixels) / len(pixels) for i in range(3))


def tint(colour, shade):
    scale = shade / max(luminance(colour), 1e-3)
    return tuple(min(255, round(c * scale)) for c in colour)


def toward_grey(rgb, amount):
    grey = luminance(rgb) * 255
    return tuple(round(c + (grey - c) * amount) for c in rgb[:3])


def zombie_chicken(chicken, zombie):
    green = toward_grey(mean_green(zombie), 0.25)
    out = chicken.convert("RGBA").copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            if r == g == b:
                if r < 40:
                    continue  # the eyes
                # Feathers: white 255 to grey 198, as the zombie's green from light to mid
                shade = 0.28 + (r - 190) / 65.0 * 0.22
                pixels[x, y] = tint(green, shade) + (a,)
            elif r > 200 and g < 60:
                pixels[x, y] = WATTLE + (a,)
            else:
                pixels[x, y] = toward_grey((r, g, b), 0.45) + (a,)
    return out


def egg(image, zombie):
    green = toward_grey(mean_green(zombie), 0.25)
    out = image.convert("RGBA").copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            if max(r, g, b) - min(r, g, b) < 12:
                # Feathers and outline: light grey to the feathers' greens, the dark outline to a dark green
                shade = 0.12 + (luminance((r, g, b)) - 0.4) / 0.6 * 0.38
                pixels[x, y] = tint(green, max(shade, 0.08)) + (a,)
            elif r > 120 and g < 80:
                # The wattle, its shades kept
                scale = r / 246.0 + (0.15 if g > 40 else 0.0)
                pixels[x, y] = tuple(min(255, round(c * scale)) for c in WATTLE) + (a,)
            else:
                pixels[x, y] = toward_grey((r, g, b), 0.45) + (a,)
    return out


def main(vanilla, textures):
    vanilla, textures = Path(vanilla), Path(textures)
    (textures / "entity").mkdir(parents=True, exist_ok=True)
    (textures / "item").mkdir(parents=True, exist_ok=True)
    zombie_chicken(Image.open(vanilla / "chicken_temperate.png"), Image.open(vanilla / "zombie.png")).save(textures / "entity" / "zombie_chicken.png")
    egg(Image.open(vanilla / "chicken_spawn_egg.png"), Image.open(vanilla / "zombie.png")).save(textures / "item" / "zombie_chicken_spawn_egg.png")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
