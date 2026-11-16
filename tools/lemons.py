#!/usr/bin/env python3
"""Writes the lemon's assets: run from the repository root.

    python3 tools/lemons.py

The lemon (item: its texture, model and item definition), Sour's effect icon (a lemon slice) and their names in every
language file. Nothing grows lemons now: the lemon trees of earlier versions (stripped oak logs under lemon leaves) and
the two-block lemon bushes that followed them are gone. The helpers here (write_json, add_to_tag, add_names) are
tools/cypress.py's too.
"""
import json
import os
import math
import sys

from PIL import Image

MOD = "players-versus"
ASSETS = os.path.join("src", "main", "resources", "assets")
DATA = os.path.join("src", "main", "resources", "data")

LEMON = (247, 224, 74, 255)
LEMON_LIGHT = (255, 244, 140, 255)
LEMON_SHADE = (205, 168, 40, 255)
LEMON_DARK = (150, 116, 26, 255)
STEM = (86, 112, 38, 255)


def write_json(root, path, value):
    path = os.path.join(root, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as file:
        json.dump(value, file, indent=2)
        file.write("\n")


def save(image, path):
    path = os.path.join(ASSETS, MOD, "textures", path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    image.save(path)


def lemon_item():
    """The lemon: a plump oval with pointed ends, lit from the top left, a leaf at its stem."""
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rows = {  # y: (first x, last x)
        3: (6, 8), 4: (4, 10), 5: (3, 11), 6: (2, 12), 7: (2, 13), 8: (2, 13), 9: (2, 12), 10: (3, 12), 11: (4, 11), 12: (6, 9),
    }
    for y, (first, last) in rows.items():
        for x in range(first, last + 1):
            edge = x in (first, last) or y in (3, 12)
            lit = x + y < 12
            shade = x + y > 19
            colour = LEMON_DARK if edge and not lit else LEMON_SHADE if shade else LEMON_LIGHT if lit else LEMON
            image.putpixel((x, y), colour)
    for x, y in [(1, 8), (14, 7)]:  # the pointed ends
        image.putpixel((x, y), LEMON_DARK)
    image.putpixel((5, 5), (255, 255, 220, 255))  # a glint
    for x, y in [(9, 2), (10, 1), (11, 1), (11, 2), (12, 1)]:
        image.putpixel((x, y), STEM)
    image.putpixel((8, 2), (60, 80, 28, 255))
    return image


def sour_icon():
    """Sour's icon, 18 pixels square: a lemon slice, rind, pith and segments."""
    image = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    centre = 8.5
    for y in range(18):
        for x in range(18):
            dx, dy = x - centre, y - centre
            distance = (dx * dx + dy * dy) ** 0.5
            if distance > 8.2:
                continue
            if distance > 7.0:
                colour = LEMON_SHADE
            elif distance > 6.0:
                colour = (255, 250, 222, 255)
            else:
                # six segments split by pale lines through the middle
                angle = (math.degrees(math.atan2(dy, dx)) + 360) % 60
                colour = (255, 250, 222, 255) if angle < 9 or angle > 54 or distance < 1.2 else LEMON_LIGHT if dy < 0 else LEMON
            image.putpixel((x, y), colour)
    return image


def assets():
    save(lemon_item(), "item/lemon.png")
    save(sour_icon(), "mob_effect/sour.png")
    models = f"{MOD}/models"
    write_json(ASSETS, f"{models}/item/lemon.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/lemon"}})
    write_json(ASSETS, f"{MOD}/items/lemon.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/lemon"}})


def add_to_tag(path, *values):
    full = os.path.join(DATA, path)
    tag = {"replace": False, "values": []}
    if os.path.exists(full):
        with open(full) as file:
            tag = json.load(file)
    for value in values:
        if value not in tag["values"]:
            tag["values"].append(value)
    write_json(DATA, path, tag)


NAMES = {
    "en": {
        "item.players-versus.lemon": "Lemon",
        "effect.players-versus.sour": "Sour",
    },
    "fr": {
        "item.players-versus.lemon": "Citron",
        "effect.players-versus.sour": "Acidité",
    },
}


def add_names(names):
    """
    Adds these names to every language file of both namespaces the mod keeps them in (English or French by file), as a
    group of their own before the closing brace: the files group their names with blank lines, which rewriting them
    whole would lose. Names a file already has are left as they are.
    """
    for namespace in [MOD, "minecraft"]:
        folder = os.path.join(ASSETS, namespace, "lang")
        for file_name in sorted(os.listdir(folder)):
            path = os.path.join(folder, file_name)
            with open(path, encoding="utf-8") as file:
                text = file.read()
            present = json.loads(text)
            new = {key: value for key, value in names["fr" if file_name.startswith("fr_") else "en"].items() if key not in present}
            if not new:
                continue
            body = text.rstrip()
            assert body.endswith("}"), path
            body = body[:-1].rstrip()
            lines = ",\n".join(f"  {json.dumps(key, ensure_ascii=False)}: {json.dumps(value, ensure_ascii=False)}" for key, value in new.items())
            with open(path, "w", encoding="utf-8", newline="\n") as file:
                file.write(body + ",\n\n" + lines + "\n}\n")


def main():
    assets()
    add_names(NAMES)


if __name__ == "__main__":
    if len(sys.argv) != 1:
        sys.exit(__doc__)
    main()
