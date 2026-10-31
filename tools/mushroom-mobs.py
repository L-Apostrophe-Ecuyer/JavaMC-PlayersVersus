#!/usr/bin/env python3
"""The mushroom creeper's and mushroom spider's textures and spawn eggs, recoloured from vanilla's.

Usage: mushroom-mobs.py <vanilla dir> <assets/players-versus/textures dir>

<vanilla dir> holds vanilla's creeper.png, spider.png, creeper_spawn_egg.png, spider_spawn_egg.png,
brown_mushroom_block.png and mushroom_stem.png (the api-diff workflow's "asset" focus lines print them). Each pixel
keeps its shade among the texture's pixels and takes the colour of the brown mushroom cap or the stem: the creeper's head
and body are cap, the lightest of their mottling stem-pale, its feet stem and its face a dark cap; the spider's body is
cap likewise, its legs stem. The spider's eyes and the eggs' outlines keep vanilla's pixels.
"""
import sys
from pathlib import Path

from PIL import Image


def mean_colour(path):
    """The texture's average colour."""
    pixels = [p for p in Image.open(path).convert("RGBA").get_flattened_data() if p[3] > 0]
    return tuple(sum(p[i] for p in pixels) / len(pixels) for i in range(3))


def luminance(rgb):
    r, g, b = rgb[:3]
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255.0


# The shades each colour spans, darkest to lightest: wider than the blocks' own, so vanilla's mottling shows through
CAP_SHADES, STEM_SHADES, FACE_SHADES = (0.18, 0.62), (0.55, 0.88), (0.04, 0.12)


def tint(colour, shade):
    """The colour, made as light as the shade (a luminance)."""
    scale = shade / luminance(colour)
    return tuple(min(255, round(c * scale)) for c in colour)


def recolour(image, region_of, colours, keep):
    """Each pixel, unless kept, takes its region's colour at the shade it has among the image's pixels."""
    image = image.convert("RGBA")
    shades = [luminance(p) for p in image.get_flattened_data() if p[3] > 0 and not keep(p)]
    low, high = min(shades), max(shades)
    out = image.copy()
    pixels = out.load()
    for y in range(image.height):
        for x in range(image.width):
            pixel = pixels[x, y]
            if pixel[3] == 0 or keep(pixel):
                continue
            fraction = (luminance(pixel) - low) / (high - low or 1.0)
            region = region_of(x, y, pixel)
            if region == "speckled":
                # The lightest of vanilla's mottling turns stem-pale: the stalks of mushrooms grown through the skin
                region = "stem" if fraction > 0.78 else "cap"
            colour, (darkest, lightest) = colours[region]
            pixels[x, y] = tint(colour, darkest + fraction * (lightest - darkest)) + (pixel[3],)
    return out


def dark(pixel):
    """A face's, an eye's or an outline's pixel: very dark or strongly red."""
    r, g, b = pixel[:3]
    return luminance(pixel) < 0.12 or (r > 120 and r > 2 * g and r > 2 * b)


def creeper_region(x, y, pixel):
    if 8 <= x < 16 and 8 <= y < 16 and luminance(pixel) < 0.3:
        return "face"  # the face, on the head's front
    if y >= 16 and x < 16:
        return "stem"  # the feet
    if y >= 16:
        return "speckled"  # the body
    if 8 <= x < 24 and y < 8:
        return "cap"  # the head's top and bottom
    return "speckled"  # the head's sides


def spider_region(x, y, pixel):
    if 18 <= x < 58 and y < 4:
        return "stem"  # the legs
    return "speckled"


def main(vanilla, textures):
    vanilla, textures = Path(vanilla), Path(textures)
    cap, stem = mean_colour(vanilla / "brown_mushroom_block.png"), mean_colour(vanilla / "mushroom_stem.png")
    colours = {"cap": (cap, CAP_SHADES), "stem": (stem, STEM_SHADES), "face": (cap, FACE_SHADES)}
    entity, item = textures / "entity", textures / "item"
    recolour(Image.open(vanilla / "creeper.png"), creeper_region, colours, dark).save(entity / "mushroom_creeper.png")
    recolour(Image.open(vanilla / "spider.png"), spider_region, colours, dark).save(entity / "mushroom_spider.png")
    # The eggs: cap all over, vanilla's outline kept
    outline = lambda p: luminance(p) < 0.06
    recolour(Image.open(vanilla / "creeper_spawn_egg.png"), lambda x, y, p: "cap", colours, outline).save(item / "mushroom_creeper_spawn_egg.png")
    recolour(Image.open(vanilla / "spider_spawn_egg.png"), lambda x, y, p: "speckled", colours,
             lambda p: outline(p) or dark(p)).save(item / "mushroom_spider_spawn_egg.png")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
