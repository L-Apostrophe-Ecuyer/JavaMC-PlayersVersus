#!/usr/bin/env python3
"""The zombie wolf's and the pale wolf's textures, and their spawn eggs, from vanilla's pale wolf.

Usage: wolves.py <vanilla dir> <assets/players-versus/textures dir>

<vanilla dir> holds vanilla's wolf.png, wolf_tame.png, wolf_angry.png (textures/entity/wolf), zombie.png
(textures/entity/zombie) and wolf_spawn_egg.png (textures/item); the api-diff workflow's "asset" focus lines print them.

- The zombie wolf (wild, tame and angry, as a wolf has): the fur (the wolf's greys and whites) takes the zombie's skin
  green, each keeping its shade, a little greyer than the zombie, as the zombie chicken's feathers do; the muzzle's tan
  loses some of its colour; the eyes and the nose stay.
- The pale wolf (wild and angry: it's never tamed): the fur takes the pale oak's bark greys (the pale creeper's), darker
  than the wolf's; the muzzle and the brows go a dark bark brown; the eyes are the creaking's orange, and glow
  (pale_wolf_eyes.png, the eyes alone, as the pale spider's).
- The eggs: vanilla's wolf egg the same ways.
"""
import sys
from pathlib import Path

from PIL import Image

# The pale oak's bark, light to dark (the pale creeper's and pale spider's colours)
BARK = [(221, 206, 205), (210, 198, 199), (184, 169, 169), (158, 145, 144), (125, 114, 112), (94, 83, 80)]
DARK_BARK = (67, 59, 57)
# The creaking's orange: the eyes' rims and their bright middles
EYE = (236, 114, 20)
EYE_BRIGHT = (239, 163, 55)
# The wolf's eyes on the head's front face (texture 64 x 32): rims outside, pupils inside
EYE_RIMS = [(4, 6), (9, 6)]
EYE_PUPILS = [(5, 6), (8, 6)]


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


def toward(rgb, colour, amount):
    return tuple(round(c + (t - c) * amount) for c, t in zip(rgb[:3], colour))


def bark(shade):
    """The bark's colour for a shade from 0 (darkest) to 1 (lightest), between its tones."""
    tones = list(reversed(BARK))
    position = min(max(shade, 0.0), 1.0) * (len(tones) - 1)
    low = int(position)
    high = min(low + 1, len(tones) - 1)
    return toward(tones[low], tones[high], position - low)


def is_fur(rgb):
    r, g, b = rgb[:3]
    return max(r, g, b) - min(r, g, b) < 16 and 140 <= r < 250


def is_white(rgb):
    return rgb[:3] == (255, 255, 255)


def zombie_wolf(wolf, zombie):
    green = toward_grey(mean_green(zombie), 0.25)
    out = wolf.convert("RGBA").copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = pixels[x, y]
            if a == 0 or is_white((r, g, b)) or max(r, g, b) < 90:
                continue  # the eyes' whites, the pupils, the nose and the brows
            if is_fur((r, g, b)):
                # Fur: the wolf's greys (about 0.58 to 0.88) as the zombie's green from mid to light
                shade = 0.20 + (luminance((r, g, b)) - 0.58) / 0.30 * 0.22
                pixels[x, y] = tint(green, shade) + (a,)
            elif r > 150 and g < 60:
                continue  # an angry wolf's red eyes
            else:
                pixels[x, y] = toward_grey((r, g, b), 0.45) + (a,)
    return out


def pale_wolf(wolf):
    out = wolf.convert("RGBA").copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            if (x, y) in EYE_RIMS:
                pixels[x, y] = EYE + (a,)
            elif (x, y) in EYE_PUPILS:
                pixels[x, y] = EYE_BRIGHT + (a,)
            elif is_fur((r, g, b)):
                # Fur: the wolf's greys as the bark's mid greys, well darker than the wolf (ashen, not white)
                shade = 0.12 + (luminance((r, g, b)) - 0.55) / 0.35 * 0.5
                pixels[x, y] = bark(shade) + (a,)
            elif max(r, g, b) < 90 or is_white((r, g, b)):
                pixels[x, y] = DARK_BARK + (a,)
            else:
                # The muzzle's tan: a dark bark brown, its shades kept
                pixels[x, y] = bark((luminance((r, g, b)) - 0.4) / 0.4 * 0.35) + (a,)
    return out


def pale_wolf_eyes(wolf):
    out = Image.new("RGBA", wolf.size, (0, 0, 0, 0))
    for xy in EYE_RIMS:
        out.putpixel(xy, EYE + (255,))
    for xy in EYE_PUPILS:
        out.putpixel(xy, EYE_BRIGHT + (255,))
    return out


def zombie_egg(egg, zombie):
    green = toward_grey(mean_green(zombie), 0.25)
    out = egg.convert("RGBA").copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            if max(r, g, b) - min(r, g, b) < 16:
                # Fur and outline: the light greys to the fur's greens, the dark outline to a dark green
                shade = 0.10 + (luminance((r, g, b)) - 0.2) / 0.7 * 0.32
                pixels[x, y] = tint(green, max(shade, 0.06)) + (a,)
            else:
                pixels[x, y] = toward_grey((r, g, b), 0.45) + (a,)
    return out


def pale_egg(egg):
    out = egg.convert("RGBA").copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            if max(r, g, b) - min(r, g, b) < 16:
                shade = (luminance((r, g, b)) - 0.2) / 0.7
                pixels[x, y] = (bark(0.12 + shade * 0.5) if shade > 0.05 else DARK_BARK) + (a,)
            else:
                pixels[x, y] = bark((luminance((r, g, b)) - 0.4) / 0.4 * 0.35) + (a,)
    return out


def main(vanilla, textures):
    vanilla, textures = Path(vanilla), Path(textures)
    entity, item = textures / "entity", textures / "item"
    entity.mkdir(parents=True, exist_ok=True)
    item.mkdir(parents=True, exist_ok=True)
    zombie = Image.open(vanilla / "zombie.png")
    for kind in ("", "_tame", "_angry"):
        zombie_wolf(Image.open(vanilla / f"wolf{kind}.png"), zombie).save(entity / f"zombie_wolf{kind}.png")
    for kind in ("", "_angry"):
        pale_wolf(Image.open(vanilla / f"wolf{kind}.png")).save(entity / f"pale_wolf{kind}.png")
    pale_wolf_eyes(Image.open(vanilla / "wolf.png")).save(entity / "pale_wolf_eyes.png")
    egg = Image.open(vanilla / "wolf_spawn_egg.png")
    zombie_egg(egg, zombie).save(item / "zombie_wolf_spawn_egg.png")
    pale_egg(egg).save(item / "pale_wolf_spawn_egg.png")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
