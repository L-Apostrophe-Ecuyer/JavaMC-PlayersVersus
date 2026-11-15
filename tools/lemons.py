#!/usr/bin/env python3
"""Writes the lemons' assets and data: run from the repository root with vanilla's extracted assets and data.

    python3 tools/lemons.py <vanilla>     (<vanilla> holds assets/minecraft and data/minecraft, from the game's jars)

The lemon bush (LemonBushBlock) is a tiny tree two blocks tall: its lower half a short oak stem, its upper half a bushy
crown (boxes of oak leaves' texture in a fixed citrus green) carrying its stage as the lemon leaves carry theirs, the
blossoms' or lemons' overlay on its faces; its item a tiny tree's picture. The orchards (lemon_bushes, at every stage) and the sapling (lemon_bush_plant, bare) place it.

Lemon leaves (LemonLeavesBlock) are oak's leaves that fruit: bare, in blossom, hung with lemons, the leaves of the
lemon trees earlier worlds have (nothing generates or grows them now). Their texture is oak leaves' (the biome tints
it, layer 0); the blossoms and lemons are an untinted second layer drawn only on the leaves' own pixels, so the fruit
stays yellow in every biome. Also the lemon (item), the lemon sapling (oak's, glossier, a lemon on it) and its pot,
Sour's effect icon (a lemon slice), the blockstates, models and item definitions, the loot tables (leaves as oak's
without the apple, lemons when fruiting; the bush as the rose bush, from its lower half, with its lemons; the picking
tables, 2 to 6), the tags and the names in every language file.
"""
import json
import os
import random
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
BLOSSOM = (255, 252, 240, 255)
BLOSSOM_HEART = (246, 214, 92, 255)


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


def leaf_mask(leaves):
    """The pixels the leaves' texture covers: fruit only hangs where there are leaves."""
    return {(x, y) for y in range(16) for x in range(16) if leaves.getpixel((x, y))[3] > 0}


def blossoms(mask):
    """White five-pixel blossoms with a yellow heart, scattered over the leaves."""
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for cx, cy in [(3, 3), (11, 2), (7, 7), (13, 10), (2, 11), (9, 13)]:
        for dx, dy in [(0, -1), (-1, 0), (1, 0), (0, 1)]:
            if (cx + dx, cy + dy) in mask:
                image.putpixel((cx + dx, cy + dy), BLOSSOM)
        if (cx, cy) in mask:
            image.putpixel((cx, cy), BLOSSOM_HEART)
    return image


def lemon_at(image, x, y, mask):
    """A hanging lemon three pixels wide and four tall: a stem, a lit top, a shaded underside and a tip."""
    shape = {
        (1, 0): STEM,
        (0, 1): LEMON_LIGHT, (1, 1): LEMON_LIGHT, (2, 1): LEMON,
        (0, 2): LEMON, (1, 2): LEMON, (2, 2): LEMON_SHADE,
        (1, 3): LEMON_DARK,
    }
    for (dx, dy), colour in shape.items():
        px, py = x + dx, y + dy
        if 0 <= px < 16 and 0 <= py < 16 and ((px, py) in mask or colour is not STEM):
            image.putpixel((px, py), colour)


def lemons(mask):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x, y in [(2, 1), (10, 3), (6, 8), (12, 11), (1, 11)]:
        lemon_at(image, x, y, mask)
    return image


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


def sapling(oak_sapling):
    """Oak's sapling, its leaves a glossier, darker green, with a lemon at the top."""
    image = oak_sapling.copy().convert("RGBA")
    for y in range(16):
        for x in range(16):
            r, g, b, a = image.getpixel((x, y))
            if a and g > r and g > b:
                image.putpixel((x, y), (int(r * 0.75), min(255, int(g * 0.95)), int(b * 0.7), a))
    for (x, y), colour in {(10, 3): LEMON_LIGHT, (11, 3): LEMON, (10, 4): LEMON, (11, 4): LEMON_SHADE}.items():
        image.putpixel((x, y), colour)
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
                import math
                angle = (math.degrees(math.atan2(dy, dx)) + 360) % 60
                colour = (255, 250, 222, 255) if angle < 9 or angle > 54 or distance < 1.2 else LEMON_LIGHT if dy < 0 else LEMON
            image.putpixel((x, y), colour)
    return image


def citrus(colour):
    """A leaf's green made a citrus leaf's: glossier and darker, as the sapling's."""
    r, g, b, a = colour
    return (int(r * 0.75), min(255, int(g * 0.95)), int(b * 0.7), a)


# The bush's crown: oak leaves' texture in a fixed citrus green (untinted: the fruit's overlays are drawn on its pixels)
BUSH_GREEN = (96, 150, 56)


def bush_leaves(oak_leaves):
    image = oak_leaves.copy().convert("RGBA")
    for y in range(16):
        for x in range(16):
            r, g, b, a = image.getpixel((x, y))
            if a:
                image.putpixel((x, y), citrus((r * BUSH_GREEN[0] // 255, g * BUSH_GREEN[1] // 255, b * BUSH_GREEN[2] // 255, a)))
    return image


BARK, BARK_DARK = (125, 96, 58, 255), (84, 63, 37, 255)


def bush_item(leaves):
    """The bush's item: a tiny tree, a round crown of its leaves hung with lemons on a short trunk."""
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(10, 16):
        image.putpixel((7, y), BARK)
        image.putpixel((8, y), BARK_DARK)
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 6.5, (y - 5.5) / 5.0
            if dx * dx + dy * dy <= 1.0:
                r, g, b, _ = leaves.getpixel((x, y)) if leaves.getpixel((x, y))[3] else citrus((70, 120, 40, 255))
                # Lit from the top left, shaded to the bottom right
                light = 1.15 - 0.3 * (dx + dy + 1.0) / 2.0
                image.putpixel((x, y), (min(255, int(r * light)), min(255, int(g * light)), min(255, int(b * light)), 255))
    for x, y in [(4, 5), (9, 3), (11, 7), (6, 8)]:
        lemon_at(image, x - 1, y - 1, set())
    return image


def box(start, end, texture, inflate=0.0):
    """A model element from start to end (pixels), every face of the texture at one pixel a pixel, centred on it."""
    start = [c - inflate for c in start]
    end = [c + inflate for c in end]
    width, height, depth = (end[i] - start[i] for i in range(3))

    def uv(w, h):
        u, v = (16 - min(16, w)) / 2, (16 - min(16, h)) / 2
        return [u, v, u + min(16, w), v + min(16, h)]

    faces = {face: {"uv": uv(width, height) if face in ("north", "south") else uv(depth, height) if face in ("east", "west") else uv(width, depth),
                    "texture": texture} for face in ["down", "up", "north", "south", "west", "east"]}
    return {"from": start, "to": end, "faces": faces}


# The crown: a box of leaves reaching three pixels down over the trunk's top, a wider band round its middle, a cap
CROWN = [([2, -3, 2], [14, 12, 14]), ([0, 1, 3], [16, 9, 13]), ([3, 1, 0], [13, 9, 16]), ([4, 12, 4], [12, 15, 12])]


def crown_model(overlay):
    elements = [box(start, end, "#leaves") for start, end in CROWN]
    if overlay:
        elements += [box(start, end, "#overlay", inflate=0.05) for start, end in CROWN]
    textures = {"particle": f"{MOD}:block/lemon_bush_leaves", "leaves": f"{MOD}:block/lemon_bush_leaves"}
    if overlay:
        textures["overlay"] = f"{MOD}:block/{overlay}"
    return {"parent": "minecraft:block/block", "textures": textures, "elements": elements}


def trunk_model():
    """A short oak stem, flared at its foot."""
    elements = [box([6.5, 0, 6.5], [9.5, 2, 9.5], "#bark"), box([7, 2, 7], [9, 16, 9], "#bark")]
    for element in elements:
        element["faces"]["up"]["texture"] = element["faces"]["down"]["texture"] = "#end"
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"particle": "minecraft:block/oak_log", "bark": "minecraft:block/oak_log", "end": "minecraft:block/oak_log_top"},
            "elements": elements}


# The bush's stages, by FRUIT: the crown bare, in blossom, hung with lemons (the lemon leaves' own overlays)
CROWN_STAGES = [("", None), ("_blossoms", "lemon_leaves_blossoms"), ("_lemons", "lemon_leaves_lemons")]


def leaves_model(overlay):
    """Vanilla's leaves model, then the same cube again in an untinted overlay (blossoms or lemons)."""
    faces = {}
    overlay_faces = {}
    for face in ["down", "up", "north", "south", "west", "east"]:
        faces[face] = {"uv": [0, 0, 16, 16], "texture": "#all", "tintindex": 0, "cullface": face}
        overlay_faces[face] = {"uv": [0, 0, 16, 16], "texture": "#overlay", "cullface": face}
    return {
        "parent": "minecraft:block/block",
        "textures": {"particle": "#all", "all": f"{MOD}:block/lemon_leaves", "overlay": f"{MOD}:block/{overlay}"},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces},
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": overlay_faces},
        ],
    }


def assets(vanilla):
    textures = os.path.join(vanilla, "assets", "minecraft", "textures")
    oak_leaves = Image.open(os.path.join(textures, "block", "oak_leaves.png")).convert("RGBA")
    mask = leaf_mask(oak_leaves)
    save(oak_leaves, "block/lemon_leaves.png")
    with open(os.path.join(textures, "block", "oak_leaves.png.mcmeta")) as file:
        meta = json.load(file)
    write_json(ASSETS, f"{MOD}/textures/block/lemon_leaves.png.mcmeta", meta)
    save(blossoms(mask), "block/lemon_leaves_blossoms.png")
    save(lemons(mask), "block/lemon_leaves_lemons.png")
    save(lemon_item(), "item/lemon.png")
    save(sapling(Image.open(os.path.join(textures, "block", "oak_sapling.png"))), "block/lemon_sapling.png")
    save(sour_icon(), "mob_effect/sour.png")

    models = f"{MOD}/models"
    write_json(ASSETS, f"{models}/block/lemon_leaves.json", {"parent": "minecraft:block/leaves", "textures": {"all": f"{MOD}:block/lemon_leaves"}})
    write_json(ASSETS, f"{models}/block/lemon_leaves_blossoms.json", leaves_model("lemon_leaves_blossoms"))
    write_json(ASSETS, f"{models}/block/lemon_leaves_lemons.json", leaves_model("lemon_leaves_lemons"))
    write_json(ASSETS, f"{MOD}/blockstates/lemon_leaves.json", {"variants": {
        "fruit=0": {"model": f"{MOD}:block/lemon_leaves"},
        "fruit=1": {"model": f"{MOD}:block/lemon_leaves_blossoms"},
        "fruit=2": {"model": f"{MOD}:block/lemon_leaves_lemons"},
    }})
    # The leaves' item shows them with lemons, tinted as vanilla tints oak leaves' item
    write_json(ASSETS, f"{MOD}/items/lemon_leaves.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/lemon_leaves_lemons",
                                                                 "tints": [{"type": "minecraft:constant", "value": -12012264}]}})

    write_json(ASSETS, f"{models}/block/lemon_sapling.json", {"parent": "minecraft:block/cross", "textures": {"cross": f"{MOD}:block/lemon_sapling"}})
    write_json(ASSETS, f"{MOD}/blockstates/lemon_sapling.json", {"variants": {"": {"model": f"{MOD}:block/lemon_sapling"}}})
    write_json(ASSETS, f"{models}/item/lemon_sapling.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:block/lemon_sapling"}})
    write_json(ASSETS, f"{MOD}/items/lemon_sapling.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/lemon_sapling"}})
    write_json(ASSETS, f"{models}/block/potted_lemon_sapling.json", {"parent": "minecraft:block/flower_pot_cross", "textures": {"plant": f"{MOD}:block/lemon_sapling"}})
    write_json(ASSETS, f"{MOD}/blockstates/potted_lemon_sapling.json", {"variants": {"": {"model": f"{MOD}:block/potted_lemon_sapling"}}})

    write_json(ASSETS, f"{models}/item/lemon.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/lemon"}})
    write_json(ASSETS, f"{MOD}/items/lemon.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/lemon"}})

    # The lemon bush: a tiny tree, its lower half a trunk at every stage, its upper half a crown carrying its stage
    leaves = bush_leaves(oak_leaves)
    save(leaves, "block/lemon_bush_leaves.png")
    write_json(ASSETS, f"{models}/block/lemon_bush_trunk.json", trunk_model())
    variants = {}
    for fruit, (suffix, overlay) in enumerate(CROWN_STAGES):
        write_json(ASSETS, f"{models}/block/lemon_bush_crown{suffix}.json", crown_model(overlay))
        variants[f"fruit={fruit},half=lower"] = {"model": f"{MOD}:block/lemon_bush_trunk"}
        variants[f"fruit={fruit},half=upper"] = {"model": f"{MOD}:block/lemon_bush_crown{suffix}"}
    write_json(ASSETS, f"{MOD}/blockstates/lemon_bush.json", {"variants": variants})
    save(bush_item(leaves), "item/lemon_bush.png")
    write_json(ASSETS, f"{models}/item/lemon_bush.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/lemon_bush"}})
    write_json(ASSETS, f"{MOD}/items/lemon_bush.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/lemon_bush"}})


def loot(vanilla):
    with open(os.path.join(vanilla, "data", "minecraft", "loot_table", "blocks", "oak_leaves.json")) as file:
        oak = json.load(file)
    leaves = json.loads(json.dumps(oak).replace("minecraft:oak_leaves", f"{MOD}:lemon_leaves").replace("minecraft:oak_sapling", f"{MOD}:lemon_sapling"))
    # Oak's apple is no lemon tree's: its pool goes, and fruiting leaves drop their lemons
    leaves["pools"] = [pool for pool in leaves["pools"] if "minecraft:apple" not in json.dumps(pool)]
    # (26.4's loot tables: one "condition" and "modifier"s, as vanilla's sweet berry bush has them)
    leaves["pools"].append({
        "rolls": 1,
        "condition": {"type": "minecraft:match_block", "blocks": f"{MOD}:lemon_leaves", "state": {"fruit": "2"}},
        "entries": [{"type": "minecraft:item", "name": f"{MOD}:lemon", "modifier": [
            {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 6}},
            {"type": "minecraft:explosion_decay"}]}],
    })
    leaves["random_sequence"] = f"{MOD}:blocks/lemon_leaves"
    write_json(DATA, f"{MOD}/loot_table/blocks/lemon_leaves.json", leaves)

    with open(os.path.join(vanilla, "data", "minecraft", "loot_table", "harvest", "sweet_berry_bush.json")) as file:
        picking = json.load(file)
    write_json(DATA, f"{MOD}/loot_table/harvest/lemon_leaves.json", {
        "type": picking["type"],
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:lemon",
                                            "modifier": {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 6}}}]}],
        "random_sequence": f"{MOD}:harvest/lemon_leaves",
    })
    # The bush drops as the rose bush does, from its lower half (breaking either half breaks both), and its lemons with it
    with open(os.path.join(vanilla, "data", "minecraft", "loot_table", "blocks", "rose_bush.json")) as file:
        bush_table = json.loads(file.read().replace("minecraft:rose_bush", f"{MOD}:lemon_bush"))
    bush_table["pools"].append({
        "rolls": 1,
        "condition": {"type": "minecraft:match_block", "blocks": f"{MOD}:lemon_bush", "state": {"half": "lower", "fruit": "2"}},
        "entries": [{"type": "minecraft:item", "name": f"{MOD}:lemon", "modifier": [
            {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 6}},
            {"type": "minecraft:explosion_decay"}]}],
    })
    bush_table["random_sequence"] = f"{MOD}:blocks/lemon_bush"
    write_json(DATA, f"{MOD}/loot_table/blocks/lemon_bush.json", bush_table)
    write_json(DATA, f"{MOD}/loot_table/harvest/lemon_bush.json", {
        "type": picking["type"],
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:lemon",
                                            "modifier": {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 6}}}]}],
        "random_sequence": f"{MOD}:harvest/lemon_bush",
    })
    for name, oak_name in [("lemon_sapling", "oak_sapling"), ("potted_lemon_sapling", "potted_oak_sapling")]:
        with open(os.path.join(vanilla, "data", "minecraft", "loot_table", "blocks", f"{oak_name}.json")) as file:
            table = json.loads(file.read().replace("minecraft:oak_sapling", f"{MOD}:lemon_sapling").replace("minecraft:potted_oak_sapling", f"{MOD}:potted_lemon_sapling"))
        table["random_sequence"] = f"{MOD}:blocks/{name}"
        write_json(DATA, f"{MOD}/loot_table/blocks/{name}.json", table)


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


def tags():
    add_to_tag("minecraft/tags/block/leaves.json", f"{MOD}:lemon_leaves")
    add_to_tag("minecraft/tags/item/leaves.json", f"{MOD}:lemon_leaves")
    add_to_tag("minecraft/tags/block/saplings.json", f"{MOD}:lemon_sapling")
    add_to_tag("minecraft/tags/item/saplings.json", f"{MOD}:lemon_sapling")
    add_to_tag("minecraft/tags/block/flower_pots.json", f"{MOD}:potted_lemon_sapling")
    add_to_tag(f"{MOD}/tags/block/sheds_fallen_leaves.json", f"{MOD}:lemon_leaves")
    # The bush as the rose bush: bees come to it, trees and huge mushrooms grow through it, water washes it away (but it's
    # no flower: no dye, no bee food)
    for tag in ["bee_attractive", "replaceable_by_trees", "replaceable_by_mushrooms", "washed_away_by_fluids"]:
        add_to_tag(f"minecraft/tags/block/{tag}.json", f"{MOD}:lemon_bush")


NAMES = {
    "en": {
        "item.players-versus.lemon": "Lemon",
        "block.players-versus.lemon_leaves": "Lemon Leaves",
        "block.players-versus.lemon_sapling": "Lemon Sapling",
        "block.players-versus.potted_lemon_sapling": "Potted Lemon Sapling",
        "block.players-versus.lemon_bush": "Lemon Bush",
        "effect.players-versus.sour": "Sour",
    },
    "fr": {
        "item.players-versus.lemon": "Citron",
        "block.players-versus.lemon_leaves": "Feuilles de citronnier",
        "block.players-versus.lemon_sapling": "Pousse de citronnier",
        "block.players-versus.potted_lemon_sapling": "Pousse de citronnier en pot",
        "block.players-versus.lemon_bush": "Buisson de citronnier",
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


def worldgen():
    """
    The lemon bush's features: lemon_bush_plant, a bare bush, which the lemon sapling grows into; lemon_bushes, a bush at
    any stage (bare 4, in blossom 2, fruiting 4), which the Tuscan grove's orchards place in patches (their placement,
    lemon_orchards, is the grove's: tools/transition-biomes.py).

    Then the lemon tree of earlier versions (feature lemon_bush, kept for /place; nothing grows or generates it now): two
    or three stripped oak logs under oak's round crown (a blob of radius 2, three layers), which on so short a trunk
    hangs nearly to the ground. Its leaves start at every stage, some already fruiting; now and then a beehive.
    """
    bush = {"id": f"{MOD}:lemon_bush", "properties": {"fruit": "0", "half": "lower"}}
    write_json(DATA, f"{MOD}/worldgen/feature/lemon_bush_plant.json", {"type": "minecraft:simple_block", "to_place": bush})
    write_json(DATA, f"{MOD}/worldgen/feature/lemon_bushes.json", {"type": "minecraft:simple_block", "to_place": {"type": "minecraft:weighted", "entries": [
        {"data": {"id": f"{MOD}:lemon_bush", "properties": {"fruit": fruit, "half": "lower"}}, "weight": weight}
        for fruit, weight in [("0", 4), ("1", 2), ("2", 4)]]}})
    leaves = {"distance": "7", "persistent": "false", "waterlogged": "false"}
    write_json(DATA, f"{MOD}/worldgen/feature/lemon_bush.json", {
        "type": "minecraft:tree",
        "below_trunk_provider": "minecraft:soil_beneath_tree",
        "decorators": [{"type": "minecraft:beehive", "probability": 0.02}],
        "foliage_placer": {"type": "minecraft:blob_foliage_placer", "height": 3, "offset": 0, "radius": 2},
        "foliage_provider": {"type": "minecraft:weighted", "entries": [
            {"data": {"id": f"{MOD}:lemon_leaves", "properties": dict(leaves, fruit="0")}, "weight": 4},
            {"data": {"id": f"{MOD}:lemon_leaves", "properties": dict(leaves, fruit="1")}, "weight": 2},
            {"data": {"id": f"{MOD}:lemon_leaves", "properties": dict(leaves, fruit="2")}, "weight": 4}]},
        "ignore_vines": True,
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 1},
        "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": 2, "height_rand_a": 1, "height_rand_b": 0},
        "trunk_provider": {"id": "minecraft:stripped_oak_log", "properties": {"axis": "y"}},
    })
    write_json(DATA, f"{MOD}/worldgen/placed_feature/lemon_bush_checked.json", {
        "feature": f"{MOD}:lemon_bush",
        "placement": [{"type": "minecraft:block_predicate_filter",
                       "predicate": {"type": "minecraft:would_survive", "state": {"id": f"{MOD}:lemon_sapling", "properties": {"stage": "0"}}}}],
    })


def main(vanilla):
    random.seed(20261107)
    assets(vanilla)
    worldgen()
    loot(vanilla)
    tags()
    add_names(NAMES)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
