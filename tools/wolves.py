#!/usr/bin/env python3
"""The zombie wolf's and the pale wolf's textures, and their spawn eggs, from vanilla's pale wolf.

Usage: wolves.py <vanilla dir> <assets/players-versus/textures dir>

<vanilla dir> holds vanilla's wolf.png, wolf_tame.png, wolf_angry.png (textures/entity/wolf), zombie.png
(textures/entity/zombie) and wolf_spawn_egg.png (textures/item); the api-diff workflow's "asset" focus lines print them.

- The zombie wolf (wild, tame and angry, as a wolf has): the fur (the wolf's greys and whites) takes the zombie's skin
  green, each keeping its shade, a little greyer than the zombie, as the zombie chicken's feathers do; the muzzle's tan
  loses some of its colour; the eyes and the nose stay.
- The pale wolf (wild and angry: it's never tamed) is a wolf's skeleton: bone (the pale oak's palest bark tones, shaded
  as the wolf's fur was), its body and chest a rib cage (every other row of their sides gone, around a spine and a
  breastbone), its tail a string of vertebrae, its legs thin; a skull with dark sockets, the creaking's orange eyes
  glowing in them (pale_wolf_eyes.png, the eyes alone, as the pale spider's), a nose hole and teeth. The model draws
  cut out and both-sided, so the gaps show through.
- The eggs: vanilla's wolf egg the same ways (the pale one a bone-white skull with orange eyes).
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

# The skeleton's colours: bone (light to shaded), the dark of sockets, nose and gaps between teeth, and teeth
SOCKET = (40, 35, 34)
TEETH = (236, 232, 229)
# The eye sockets around the eyes, on the head's front face
SOCKETS = [(3, 6), (10, 6), (4, 5), (5, 5), (8, 5), (9, 5), (4, 7), (5, 7), (8, 7), (9, 7)]

# The wolf model's faces (vanilla's 64 x 32 layout). The body (box 6 x 9 x 6 at 18, 14) and the mane (8 x 6 x 7 at 21, 0)
# lie along the wolf, their faces' rows running from the shoulders back: their sides' rows are the ribs.
BODY_SIDES = [(18, 24), (30, 36)]          # columns of the body's left and right faces, rows 20-28
BODY_BELLY, BODY_BACK = (24, 30), (36, 42)  # the belly (breastbone down its middle) and the back (the spine)
BODY_ROWS = range(20, 29)
BODY_RIBS = {20, 22, 24, 27, 28}           # rows of bone: three ribs, then the hips; the rest are gaps
MANE_SIDES = [(21, 28), (36, 43)]
MANE_BELLY, MANE_BACK = (28, 36), (43, 51)
MANE_ROWS = range(7, 13)
MANE_RIBS = {7, 9, 11, 12}
TAIL = (9, 17)                              # the tail's four faces, rows 20-27: every other row a vertebra
TAIL_ROWS = range(20, 28)
LEGS = (0, 8)                               # the legs' four faces, rows 20-27: thin between the joints
LEG_ROWS = range(22, 26)
MUZZLE_TEETH_ROW = 16                       # the muzzle's sides' lowest row: the teeth
MUZZLE_TEETH = range(0, 14)
NOSE = [(5, 14), (5, 15)]


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


def bone(rgb):
    """The bone for one of the wolf's pixels: its fur's shade among the bark's palest tones."""
    return bark(0.5 + (luminance(rgb) - 0.55) / 0.35 * 0.5)


def middle(columns):
    """The middle two columns of a face's span."""
    centre = (columns[0] + columns[1]) // 2
    return {centre - 1, centre}


def skeletal(x, y):
    """Whether the skeleton has no bone at a pixel of the wolf's: the gaps between ribs, vertebrae and thin leg bones."""
    if y in BODY_ROWS:
        if any(a <= x < b for a, b in BODY_SIDES):
            return y not in BODY_RIBS
        if BODY_BELLY[0] <= x < BODY_BELLY[1] or BODY_BACK[0] <= x < BODY_BACK[1]:
            span = BODY_BELLY if x < BODY_BELLY[1] else BODY_BACK
            return y not in BODY_RIBS and x not in middle(span)
    if y in MANE_ROWS:
        if any(a <= x < b for a, b in MANE_SIDES):
            return y not in MANE_RIBS
        if MANE_BELLY[0] <= x < MANE_BELLY[1] or MANE_BACK[0] <= x < MANE_BACK[1]:
            span = MANE_BELLY if x < MANE_BELLY[1] else MANE_BACK
            return y not in MANE_RIBS and x not in middle(span)
    if TAIL[0] <= x < TAIL[1] and y in TAIL_ROWS:
        return y % 2 == 1
    if LEGS[0] <= x < LEGS[1] and y in LEG_ROWS:
        return x % 2 == 1  # each face keeps one column: a thin bone
    return False


def pale_wolf(wolf):
    out = wolf.convert("RGBA").copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            if skeletal(x, y):
                pixels[x, y] = (0, 0, 0, 0)
            elif (x, y) in EYE_RIMS:
                pixels[x, y] = EYE + (a,)
            elif (x, y) in EYE_PUPILS:
                pixels[x, y] = EYE_BRIGHT + (a,)
            elif (x, y) in SOCKETS or (x, y) in NOSE or max(r, g, b) < 90:
                pixels[x, y] = SOCKET + (a,)
            elif y == MUZZLE_TEETH_ROW and x in MUZZLE_TEETH:
                pixels[x, y] = (TEETH if x % 2 == 0 else SOCKET) + (a,)
            else:
                pixels[x, y] = bone((r, g, b)) + (a,)
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


# The wolf egg's head (16 x 16): eye sockets, the eyes in them, the nose and the mouth (teeth)
EGG_SOCKETS = [(5, 6), (6, 6), (10, 6), (11, 6)]
EGG_EYES = [(5, 7), (11, 7)]
EGG_DARK_EYES = [(6, 7), (10, 7)]
EGG_MOUTH_ROW = 11


def pale_egg(egg):
    out = egg.convert("RGBA").copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            if (x, y) in EGG_EYES:
                pixels[x, y] = EYE + (a,)
            elif (x, y) in EGG_SOCKETS or (x, y) in EGG_DARK_EYES or luminance((r, g, b)) < 0.3:
                pixels[x, y] = SOCKET + (a,)
            elif y == EGG_MOUTH_ROW and max(r, g, b) - min(r, g, b) < 16 and luminance((r, g, b)) < 0.5:
                pixels[x, y] = (TEETH if x % 2 == 0 else SOCKET) + (a,)
            else:
                pixels[x, y] = bark(0.45 + (luminance((r, g, b)) - 0.3) / 0.6 * 0.55) + (a,)
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
