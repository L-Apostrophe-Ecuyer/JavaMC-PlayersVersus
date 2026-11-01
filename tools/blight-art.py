#!/usr/bin/env python3
"""Blight's art: the effect's icon (textures/mob_effect/blight.png) and the player zombie's spawn egg
(textures/item/player_zombie_spawn_egg.png).

Usage: blight-art.py <assets/players-versus dir>   (needs Pillow)

- The icon: a white heart, as the bar's blighted hearts are, with a dark crack through it and spores about it.
- The egg: the zombie chicken's egg (same shape and shading), its green gone grey and its spots a player's skin, with a
  shirt's blue for the spot's heart.
"""
import sys
from pathlib import Path

from PIL import Image

HEART = [
    "..................",
    "..................",
    "...####....####...",
    "..#oooo#..#oooo#..",
    ".#ooWWWo##oWWooo#.",
    ".#oWWWooooooooso#.",
    ".#oWWooooooooooo#.",
    ".#ooooooxoooooso#.",
    ".#ooooooxxooooso#.",
    "..#ooooxxoooosso#.",
    "..#sooooxxooosso#.",
    "...#sooooxooosso#.",
    "....#ssooxoosso#..",
    ".....#ssoooss#....",
    "......#sssss#.....",
    ".......#sss#......",
    "........###.......",
    "..................",
]
HEART_COLOURS = {
    "#": (74, 74, 70, 255),      # outline
    "o": (226, 226, 218, 255),  # body
    "W": (250, 250, 246, 255),  # shine
    "s": (178, 178, 168, 255),  # shade
    "x": (58, 66, 54, 255),     # crack
}
SPORES = [(1, 1), (16, 2), (0, 9), (17, 12), (2, 15), (15, 16)]
SPORE = (156, 176, 140, 255)

# The zombie chicken's egg, colour by colour (its pixels are found by these colours)
EGG = {
    (39, 55, 33): (38, 46, 38),       # dark edge
    (70, 96, 58): (88, 106, 86),
    (93, 128, 77): (116, 136, 108),   # body
    (106, 146, 88): (134, 154, 124),  # shine
    (55, 76, 46): (66, 82, 64),
    (25, 35, 21): (24, 30, 24),       # darkest edge
    (198, 177, 126): (230, 176, 138),  # spots: skin
    (174, 149, 105): (200, 146, 112),
    (94, 87, 75): (122, 84, 64),
    (135, 116, 82): (170, 118, 90),
    (110, 34, 30): (56, 128, 136),    # the spots' hearts: a shirt's blue
    (90, 28, 25): (42, 100, 108),
    (126, 39, 34): (70, 150, 158),
    (60, 19, 16): (28, 68, 74),
}


def icon(path: Path) -> None:
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for y, row in enumerate(HEART):
        for x, ch in enumerate(row):
            if ch in HEART_COLOURS:
                img.putpixel((x, y), HEART_COLOURS[ch])
    for x, y in SPORES:
        if img.getpixel((x, y))[3] == 0:
            img.putpixel((x, y), SPORE)
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def egg(source: Path, path: Path) -> None:
    img = Image.open(source).convert("RGBA")
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a and (r, g, b) in EGG:
                img.putpixel((x, y), EGG[(r, g, b)] + (a,))
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def main() -> None:
    assets = Path(sys.argv[1])
    icon(assets / "textures/mob_effect/blight.png")
    egg(assets / "textures/item/zombie_chicken_spawn_egg.png", assets / "textures/item/player_zombie_spawn_egg.png")


if __name__ == "__main__":
    main()
