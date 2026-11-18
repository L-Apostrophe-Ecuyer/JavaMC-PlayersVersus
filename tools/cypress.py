#!/usr/bin/env python3
"""Writes the cypress leaves' assets and data: run from the repository root with vanilla's extracted assets and data.

    python3 tools/cypress.py <vanilla>     (<vanilla> holds assets/minecraft and data/minecraft, from the game's jars)

Cypress leaves are the cypress's lighter leaves, its highlights among its spruce leaves (CypressFoliagePlacer): spruce
leaves' texture given azalea leaves' colours, shade for shade (the darkest of spruce's greys takes azalea's darkest
green, the lightest its lightest), untinted, so they're the same yellow-green in every biome, and mipmapped as spruce
leaves are (their texture's metadata: dark_cutout, so fast leaves, drawn opaque, show their gaps dark). Otherwise they're
spruce leaves: their loot table (spruce saplings and sticks; themselves to shears or silk touch), the leaves tags, the
names.
"""
import json
import os
import sys

from PIL import Image

from lemons import ASSETS, DATA, MOD, add_names, write_json
from tagfiles import add_to_tag, prune_all, use_vanilla


def luminance(colour):
    r, g, b = colour[:3]
    return 0.299 * r + 0.587 * g + 0.114 * b


def recoloured(spruce, azalea):
    """Spruce's leaves, each pixel taking the azalea colour at the same rank of brightness."""
    spruce, azalea = spruce.convert("RGBA"), azalea.convert("RGBA")
    shades = sorted({luminance(spruce.getpixel((x, y))) for y in range(16) for x in range(16) if spruce.getpixel((x, y))[3] > 0})
    greens = sorted((azalea.getpixel((x, y)) for y in range(16) for x in range(16) if azalea.getpixel((x, y))[3] > 0), key=luminance)
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            pixel = spruce.getpixel((x, y))
            if pixel[3] == 0:
                continue
            rank = shades.index(luminance(pixel)) / max(1, len(shades) - 1)
            r, g, b, _ = greens[round(rank * (len(greens) - 1))]
            image.putpixel((x, y), (r, g, b, pixel[3]))
    return image


def main(vanilla):
    textures = os.path.join(vanilla, "assets", "minecraft", "textures", "block")
    image = recoloured(Image.open(os.path.join(textures, "spruce_leaves.png")), Image.open(os.path.join(textures, "azalea_leaves.png")))
    path = os.path.join(ASSETS, MOD, "textures", "block", "cypress_leaves.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    image.save(path)
    with open(os.path.join(textures, "spruce_leaves.png.mcmeta")) as file:
        write_json(ASSETS, f"{MOD}/textures/block/cypress_leaves.png.mcmeta", json.load(file))
    # Untinted, as the azalea's leaves are (cube_all, no tint index)
    write_json(ASSETS, f"{MOD}/models/block/cypress_leaves.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/cypress_leaves"}})
    write_json(ASSETS, f"{MOD}/blockstates/cypress_leaves.json", {"variants": {"": {"model": f"{MOD}:block/cypress_leaves"}}})
    write_json(ASSETS, f"{MOD}/items/cypress_leaves.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/cypress_leaves"}})

    with open(os.path.join(vanilla, "data", "minecraft", "loot_table", "blocks", "spruce_leaves.json")) as file:
        table = json.loads(file.read().replace("minecraft:spruce_leaves", f"{MOD}:cypress_leaves"))
    table["random_sequence"] = f"{MOD}:blocks/cypress_leaves"
    write_json(DATA, f"{MOD}/loot_table/blocks/cypress_leaves.json", table)

    use_vanilla(vanilla)
    add_to_tag("minecraft/tags/block/leaves.json", f"{MOD}:cypress_leaves")
    add_to_tag("minecraft/tags/item/leaves.json", f"{MOD}:cypress_leaves")
    prune_all()
    add_names({"en": {"block.players-versus.cypress_leaves": "Cypress Leaves"},
               "fr": {"block.players-versus.cypress_leaves": "Feuilles de cyprès"}})


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
