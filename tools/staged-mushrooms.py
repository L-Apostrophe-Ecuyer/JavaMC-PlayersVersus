#!/usr/bin/env python3
"""Staged mushrooms' and Conversion's assets: textures, block models, blockstates and the pale shroom's item.

Usage: staged-mushrooms.py <vanilla dir> <assets/players-versus dir>

<vanilla dir> holds vanilla's brown_mushroom.png and red_mushroom.png (block textures), and the heart sprites full.png,
half.png, hardcore_full.png, hardcore_half.png, container.png and container_hardcore.png (the api-diff workflow's
"asset" focus lines print them).

- A tall mushroom: vanilla's, its cap two pixels up and its stem two longer.
- A pale shroom: the brown mushroom in pale greys, a little cool; its tall one likewise.
- Two or three mushrooms together: small crosses of the mushroom (cropped to it), the tall one among them; three adds a
  smaller one.
- Conversion's white hearts: vanilla's hearts in whites and greys; their containers, for white hearts emptied, grey
  inside.
"""
import json
import sys
from pathlib import Path

from PIL import Image


def luminance(rgb):
    r, g, b = rgb[:3]
    return 0.299 * r + 0.587 * g + 0.114 * b


def taller(image, cap_rows, stem_rows, by=2):
    """The mushroom with its cap `by` rows up and its stem `by` rows longer (its top rows repeated)."""
    image = image.convert("RGBA")
    out = Image.new("RGBA", image.size, (0, 0, 0, 0))
    for y in cap_rows:
        for x in range(image.width):
            out.putpixel((x, y - by), image.getpixel((x, y)))
    stem = list(stem_rows)
    # The stem's rows: its top (the cap's shadow) once, the others twice, as many as make it `by` longer
    rows = ([stem[0]] + [y for y in stem[1:] for _ in range(2)])[:len(stem) + by]
    top = stem[0] - by
    for i, y in enumerate(rows):
        for x in range(image.width):
            out.putpixel((x, top + i), image.getpixel((x, y)))
    return out


def pale(image):
    """Pale greys, a little cool, keeping each pixel's shade."""
    out = image.convert("RGBA").copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            p = pixels[x, y]
            if p[3] == 0:
                continue
            v = min(255, round(luminance(p) * 1.05 + 45))
            pixels[x, y] = (v, v, min(255, v + 4), p[3])
    return out


def white_heart(image):
    """A heart in whites: the red and its shadow white and grey, the highlight white, hardcore's dark grey."""
    out = image.convert("RGBA").copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            if g > 150:  # the highlight
                pixels[x, y] = (255, 255, 255, a)
            elif r > 230:  # the red
                pixels[x, y] = (226, 226, 220, a)
            elif r > 150:  # its shadow
                pixels[x, y] = (168, 168, 162, a)
            else:  # hardcore's dark eyes
                pixels[x, y] = (110, 110, 106, a)
    return out


def white_container(image):
    """A container whose inside is grey: a white heart emptied."""
    out = image.convert("RGBA").copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = pixels[x, y]
            if a and r > 0:
                pixels[x, y] = (104, 104, 100, a)
    return out


def cross(cx, cz, width, height, texture, uv):
    """A small cross of a mushroom standing at (cx, cz): two planes, turned 45 degrees."""
    rotation = {"origin": [cx, 0, cz], "axis": "y", "angle": 45}
    face = {"uv": uv, "texture": "#" + texture}
    half = width / 2
    return [
        {"from": [cx - half, 0, cz], "to": [cx + half, height, cz], "rotation": rotation, "shade_direction_override": "up",
         "faces": {"north": face, "south": face}},
        {"from": [cx, 0, cz - half], "to": [cx, height, cz + half], "rotation": rotation, "shade_direction_override": "up",
         "faces": {"west": face, "east": face}},
    ]


def model(textures, crosses):
    elements = [element for c in crosses for element in cross(*c)]
    return {"ambientocclusion": False, "textures": dict(textures, particle="#short"), "elements": elements}


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def main(vanilla, assets):
    vanilla, assets = Path(vanilla), Path(assets)
    blocks, hearts = assets / "textures" / "block", assets / "textures" / "gui" / "sprites" / "hud" / "heart"
    hearts.mkdir(parents=True, exist_ok=True)
    brown, red = Image.open(vanilla / "brown_mushroom.png"), Image.open(vanilla / "red_mushroom.png")
    # Brown: cap rows 8-11, stem 12-15; red: cap 9-12, stem 13-15
    taller(brown, range(8, 12), range(12, 16)).save(blocks / "brown_mushroom_tall.png")
    taller(red, range(9, 13), range(13, 16)).save(blocks / "red_mushroom_tall.png")
    pale(brown).save(blocks / "pale_shroom.png")
    pale(taller(brown, range(8, 12), range(12, 16))).save(blocks / "pale_shroom_tall.png")

    for name in ("full", "half", "hardcore_full", "hardcore_half"):
        white_heart(Image.open(vanilla / f"{name}.png")).save(hearts / f"conversion_{name}.png")
    for name in ("container", "container_hardcore"):
        white_container(Image.open(vanilla / f"{name}.png")).save(hearts / f"conversion_{name}.png")

    # Each kind's mushroom cropped: its uv and size (width, height) short and tall
    kinds = {
        "brown_mushrooms": ("minecraft:block/brown_mushroom", "players-versus:block/brown_mushroom_tall", [4, 8, 12, 16], 8, [4, 6, 12, 16], 10),
        "red_mushrooms": ("minecraft:block/red_mushroom", "players-versus:block/red_mushroom_tall", [4, 9, 12, 16], 7, [4, 7, 12, 16], 9),
        "pale_shroom": ("players-versus:block/pale_shroom", "players-versus:block/pale_shroom_tall", [4, 8, 12, 16], 8, [4, 6, 12, 16], 10),
    }
    models = assets / "models" / "block"
    for name, (short, tall, short_uv, short_height, tall_uv, tall_height) in kinds.items():
        textures = {"short": short, "tall": tall}
        two = [(5.0, 6.0, 8, short_height, "short", short_uv), (10.5, 10.0, 8, tall_height, "tall", tall_uv)]
        three = [(9.5, 6.0, 8, tall_height, "tall", tall_uv), (5.0, 10.5, 8, short_height, "short", short_uv),
                 (12.0, 12.0, 6, short_height * 0.75, "short", short_uv)]
        write_json(models / f"{name}_2.json", model(textures, two))
        write_json(models / f"{name}_3.json", model(textures, three))
        one = short if name != "pale_shroom" else "players-versus:block/pale_shroom_1"
        variants = {"mushrooms=1": {"model": one}}
        for stage in (2, 3):
            variants[f"mushrooms={stage}"] = {"model": f"players-versus:block/{name}_{stage}"}
        write_json(assets / "blockstates" / f"{name}.json", {"variants": variants})

    write_json(models / "pale_shroom_1.json", {"parent": "minecraft:block/cross", "textures": {"cross": "players-versus:block/pale_shroom"}})
    write_json(assets / "models" / "item" / "pale_shroom.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "players-versus:block/pale_shroom"}})
    write_json(assets / "items" / "pale_shroom.json", {"model": {"type": "minecraft:model", "model": "players-versus:item/pale_shroom"}})


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
