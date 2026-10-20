#!/usr/bin/env python3
"""Writes the tree blocks' (TreeBlock) assets and data, one set per wood: run from the repository root.

A tree block draws each face as its wood's vanilla log does, with the log's texture (bark) or the stripped log's (cut):
its blockstate is a multipart of 36 parts (3 axes x 6 faces x bark or cut), each a model of one face of vanilla's
log models (cube_column standing, cube_column_horizontal lying), turned as vanilla's log blockstate turns them. Also
writes each wood's item model (its vanilla log's), loot table (its vanilla log) and its place in its wood's log tag,
and puts every tree block in the vanilla tags that name logs one by one (natural logs, what keeps leaves alive, what
cocoa grows on). The mod's own lists of its blocks (blocks_motion_no_leaves, mangrove_logs_can_grow_through) are kept
by hand.
"""
import json
import os

MOD = "players-versus"
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "cherry", "dark_oak", "pale_oak", "mangrove", "poplar"]
FACES = ["down", "up", "north", "south", "west", "east"]

# Vanilla's log blockstate: the model and its turn for each axis
TURNS = {"y": {}, "z": {"x": 90}, "x": {"x": 90, "y": 90}}

# For each axis, the model's face that its turn brings to each side of the block: x 90 takes the model's up to the
# north, its north down, its down south and its south up; y 90 then takes north to east, east to south and so on
SHOWN = {
    "y": {face: face for face in FACES},
    "z": {"north": "up", "south": "down", "up": "south", "down": "north", "east": "east", "west": "west"},
    "x": {"east": "up", "west": "down", "down": "north", "up": "south", "south": "east", "north": "west"},
}

RESOURCES = os.path.join("src", "main", "resources")


def write(path, data):
    path = os.path.join(RESOURCES, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as file:
        json.dump(data, file, indent=2)
        file.write("\n")


def face_model(axis, model_face):
    """The one-face parent for this face of this axis's model: lying, the log's up face is turned half round."""
    return "up_horizontal" if axis != "y" and model_face == "up" else model_face


def parents():
    for name in FACES + ["up_horizontal"]:
        face = "up" if name == "up_horizontal" else name
        element = {"texture": "#texture", "cullface": face}
        if name == "up_horizontal":
            element["rotation"] = 180
        write(f"assets/{MOD}/models/block/tree_face/{name}.json", {
            "parent": "minecraft:block/block",
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {face: element}}],
        })


def wood(name):
    log, stripped = f"minecraft:block/{name}_log", f"minecraft:block/stripped_{name}_log"
    for parent in FACES + ["up_horizontal"]:
        end = parent in ("up", "down", "up_horizontal")
        for look, texture in (("bark", log), ("cut", stripped)):
            write(f"assets/{MOD}/models/block/{name}_tree/{parent}_{look}.json", {
                "parent": f"{MOD}:block/tree_face/{parent}",
                "textures": {"texture": texture + ("_top" if end else ""), "particle": log},
            })
    parts = []
    for axis, turn in TURNS.items():
        for side in FACES:
            for look, value in (("bark", "true"), ("cut", "false")):
                apply = {"model": f"{MOD}:block/{name}_tree/{face_model(axis, SHOWN[axis][side])}_{look}", **turn}
                parts.append({"when": {"axis": axis, side: value}, "apply": apply})
    write(f"assets/{MOD}/blockstates/{name}_tree.json", {"multipart": parts})
    write(f"assets/{MOD}/items/{name}_tree.json", {"model": {"type": "minecraft:model", "model": f"minecraft:block/{name}_log"}})
    write(f"data/{MOD}/loot_table/blocks/{name}_tree.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"minecraft:{name}_log"}],
            "condition": {"type": "minecraft:survives_explosion"},
        }],
        "random_sequence": f"{MOD}:blocks/{name}_tree",
    })
    write(f"data/minecraft/tags/block/{name}_logs.json", {"replace": False, "values": [f"{MOD}:{name}_tree"]})


def tags():
    """The vanilla tags that list each wood's log by name rather than through its wood's tag."""
    trees = [f"{MOD}:{name}_tree" for name in WOODS]
    write("data/minecraft/tags/block/overworld_natural_logs.json", {"replace": False, "values": trees})
    write("data/minecraft/tags/block/prevents_nearby_leaf_decay.json", {"replace": False, "values": trees})
    write("data/minecraft/tags/block/supports_cocoa.json", {"replace": False, "values": [f"{MOD}:jungle_tree"]})


if __name__ == "__main__":
    parents()
    for name in WOODS:
        wood(name)
    tags()
