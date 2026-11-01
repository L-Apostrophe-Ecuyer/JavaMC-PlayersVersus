#!/usr/bin/env python3
"""The watchling's taller head, three eyes and big arms (textures/entity/watchling.png and watchling_eyes.png).

Usage: watchling-head.py <assets/players-versus dir>   (needs Pillow)

The watchling's model (WatchlingModel) is the enderman's with a head 12 tall instead of 8 (and no jaw layer under it,
which is never seen). Its head box is therefore 8 x 12 x 8 from texel (0, 0): the top and bottom at rows 0 to 8, the four
sides at rows 8 to 20. This reshapes tools/watchling-source.png (the enderman layout, tools/watchling.py's output) to it:

- the head's sides (rows 8 to 16) are stretched down to rows 8 to 20 (nearest neighbour), and the old jaw layer below
  is cleared;
- the arms are 4 x 30 x 4 (the enderman's 2 x 30 x 2) from texel (0, 30): the texture is 64 x 64 now, and the arm's
  faces are the enderman's arm faces drawn twice as wide;
- the eyes the enderman's face has are painted out (the watchling's are the glowing ones), and the glowing texture gets
  three: two side by side and one above and between them, as Minecraft Dungeons' watchling has.
"""
import sys
from pathlib import Path

from PIL import Image

EYE_CORE, EYE_EDGE = (255, 226, 102), (239, 163, 55)
# The three eyes on the front face (its texels (8, 8) to (16, 20)): left and right, and above and between them
EYES = [(1, 6), (5, 6), (3, 3)]
FACE_X, FACE_Y = 8, 8


SIZE = (64, 64)


def sides(source: Image.Image) -> Image.Image:
    """The source on a 64 x 64 canvas, with the head's sides stretched to 12 rows and the jaw layer cleared."""
    out = Image.new("RGBA", SIZE, (0, 0, 0, 0))
    # Everything but the head's sides and the jaw layer stays where it was
    out.paste(source, (0, 0))
    for x in range(0, 32):
        for y in range(8, 32):
            out.putpixel((x, y), (0, 0, 0, 0))
    for x in range(0, 32):
        for y in range(8, 20):
            out.putpixel((x, y), source.getpixel((x, 8 + (y - 8) * 8 // 12)))
    return out


def arms(source: Image.Image, out: Image.Image) -> None:
    """Draws the arm's six faces at texel (0, 30), from the enderman's (2 x 30 x 2 from texel (56, 0)), twice as wide."""
    # Top and bottom: 2 x 2 each at (58, 0) and (60, 0), now 4 x 4 at (4, 30) and (8, 30)
    for sx, dx in ((58, 4), (60, 8)):
        for x in range(4):
            for y in range(4):
                out.putpixel((dx + x, 30 + y), source.getpixel((sx + x // 2, y // 2)))
    # The four sides: 2 x 30 each from (56, 2), (58, 2), (60, 2) and (62, 2), now 4 x 30 at (0, 34), (4, 34), (8, 34), (12, 34)
    for face in range(4):
        for x in range(4):
            for y in range(30):
                out.putpixel((face * 4 + x, 34 + y), source.getpixel((56 + face * 2 + x // 2, 2 + y)))


def dark(face: Image.Image) -> tuple:
    """The darkest colour on the front face: what its eyes are painted out with."""
    pixels = [face.getpixel((x, y)) for x in range(FACE_X, FACE_X + 8) for y in range(FACE_Y, FACE_Y + 12)]
    pixels = [p for p in pixels if p[3] > 0]
    return min(pixels, key=lambda p: p[0] + p[1] + p[2])


def paint_out_eyes(image: Image.Image) -> None:
    fill = dark(image)
    for x in range(FACE_X, FACE_X + 8):
        for y in range(FACE_Y, FACE_Y + 12):
            r, g, b, a = image.getpixel((x, y))
            # The enderman's face has pale texels where its eyes are
            if a and (r + g + b) / 3 > 90:
                image.putpixel((x, y), fill)


def glow() -> Image.Image:
    out = Image.new("RGBA", SIZE, (0, 0, 0, 0))
    for ex, ey in EYES:
        for dx in range(2):
            out.putpixel((FACE_X + ex + dx, FACE_Y + ey), EYE_CORE + (255,))
            out.putpixel((FACE_X + ex + dx, FACE_Y + ey + 1), EYE_EDGE + (255,))
    return out


def main() -> None:
    assets = Path(sys.argv[1])
    source = Image.open(Path(__file__).parent / "watchling-source.png").convert("RGBA")
    body = sides(source)
    arms(source, body)
    paint_out_eyes(body)
    body.save(assets / "textures/entity/watchling.png")
    glow().save(assets / "textures/entity/watchling_eyes.png")


if __name__ == "__main__":
    main()
