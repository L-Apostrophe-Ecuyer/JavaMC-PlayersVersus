#!/usr/bin/env python3
"""The watchling's skin and glowing eyes (textures/entity/watchling.png and watchling_eyes.png, 64 x 64).

Usage: watchling-skin.py <assets/players-versus dir>   (needs Pillow)

The watchling (WatchlingModel) is a cube head on a long body, arms that hang to the ground and thin legs, its skin
the plum and near-black of obsidian, its eyes many and glowing the creaking's yellow. The boxes and where their faces
are (a box w x h x d from texel (u, v): top (u+d, v), bottom (u+d+w, v), then the sides from v+d: right (u), front
(u+d), left (u+d+w), back (u+2d+w)):

- head 8 x 8 x 8 from (0, 0); its bottom face is the roof of its mouth;
- jaw 8 x 4 x 8 from (32, 0), inside the head, left behind when the head lifts: fangs and teeth along its top, dark
  inside;
- body 8 x 20 x 4 from (0, 16);
- arms 4 x 40 x 4 from (24, 16) (the left one mirrored);
- legs 2 x 22 x 2 from (40, 16) (the left one mirrored).

The skin: plum, each texel a little lighter or darker, broken by near-black (greenish) dashes two to four texels long,
as obsidian and the enderman's skin are. The eyes are slits three texels wide, yellow at the ends and white-hot in the
middle: on the skin and, the same, on the glowing texture. Seeded, so it comes out the same every time.
"""
import random
import sys
from pathlib import Path

from PIL import Image

SIZE = 64
SEED = 23

PLUM = [(72, 52, 72), (64, 46, 64), (57, 41, 57), (50, 36, 50)]
DARK = [(38, 40, 36), (31, 33, 29), (25, 26, 23)]
EYE_END, EYE_MIDDLE = (250, 196, 70), (255, 244, 196)
CAVITY, TOOTH = (12, 9, 13), (214, 206, 170)

# Boxes: name -> (u, v, w, h, d)
HEAD = (0, 0, 8, 8, 8)
JAW = (32, 0, 8, 4, 8)
BODY = (0, 16, 8, 20, 4)
ARM = (24, 16, 4, 40, 4)
LEG = (40, 16, 2, 22, 2)


def faces(box):
    """The six faces of a box: name -> (x, y, width, height) on the texture."""
    u, v, w, h, d = box
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }


# The eyes: (box, face, column, row) of each slit's left texel on that face
EYES = [
    # The head: one low on the face, two on its left side, one on the right, one behind
    (HEAD, "front", 1, 6),
    (HEAD, "left", 1, 2), (HEAD, "left", 4, 5),
    (HEAD, "right", 3, 4),
    (HEAD, "back", 4, 3),
    # The body: four down its front, right and left by turns; two behind; one on each side
    (BODY, "front", 4, 2), (BODY, "front", 1, 5), (BODY, "front", 4, 10), (BODY, "front", 1, 14),
    (BODY, "back", 3, 4), (BODY, "back", 1, 12),
    (BODY, "right", 0, 8), (BODY, "left", 1, 16),
]

# The jaw's front, top row first (c: the dark inside, t: a tooth, s: skin); the head lifts by three, so the lower three
# rows show: fangs, then teeth, then the jaw
JAW_FRONT = ["cccccccc", "ctcccctc", "tctcctct", "ssssssss"]


def skin(image, rng, area):
    x0, y0, w, h = area
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            image.putpixel((x, y), rng.choice(PLUM) + (255,))
    # Dark dashes, about a third of the face
    for _ in range(max(1, w * h // 7)):
        length = rng.randint(2, 4)
        x, y = rng.randrange(x0, x0 + w), rng.randrange(y0, y0 + h)
        colour = rng.choice(DARK) + (255,)
        for dx in range(length):
            if x + dx < x0 + w:
                image.putpixel((x + dx, y), colour)
        if rng.random() < 0.25 and y + 1 < y0 + h:
            image.putpixel((x, y + 1), colour)


def paint(image, rng, box):
    for area in faces(box).values():
        skin(image, rng, area)


def mouth(image, rng):
    """The head's bottom (the roof of the mouth) dark; the jaw dark inside, with fangs and teeth, skin outside."""
    x0, y0, w, h = faces(HEAD)["bottom"]
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            image.putpixel((x, y), CAVITY + (255,))
    jaw = faces(JAW)
    paint(image, rng, JAW)
    x0, y0, w, h = jaw["top"]
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            image.putpixel((x, y), CAVITY + (255,))
    colour = {"c": CAVITY + (255,), "t": TOOTH + (255,)}
    for name in ("front", "right", "left"):
        x0, y0, w, h = jaw[name]
        for row in range(h):
            for col in range(w):
                kind = JAW_FRONT[row][col % 8] if name == "front" else ("c" if JAW_FRONT[row][0] != "s" else "s")
                if kind != "s":
                    image.putpixel((x0 + col, y0 + row), colour[kind])


def eyes(*images):
    for box, face, col, row in EYES:
        x0, y0, w, h = faces(box)[face]
        for dx, colour in enumerate((EYE_END, EYE_MIDDLE, EYE_END)):
            if col + dx < w:
                for image in images:
                    image.putpixel((x0 + col + dx, y0 + row), colour + (255,))


def main(assets):
    rng = random.Random(SEED)
    body = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for box in (HEAD, BODY, ARM, LEG):
        paint(body, rng, box)
    mouth(body, rng)
    glow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    eyes(body, glow)
    entity = Path(assets) / "textures" / "entity"
    entity.mkdir(parents=True, exist_ok=True)
    body.save(entity / "watchling.png")
    glow.save(entity / "watchling_eyes.png")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
