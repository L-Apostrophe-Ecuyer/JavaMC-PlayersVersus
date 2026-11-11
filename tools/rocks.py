#!/usr/bin/env python3
"""Writes the rocks' data: run from the repository root.

    python3 tools/rocks.py

- Small rocks, feature type players-versus:rock (RockFeature), each within 16 blocks of its origin: a boulder, a pile, a
  crag. Placed here and there in the middle biomes (rock/sparse, a selector of the three) and in clusters in meadows
  (rock/meadow_cluster), on rock ground only (the players-versus:rock_ground filter); the rocky biomes place them with
  placed features of their own (tools/transition-biomes.py).
- Large rocks, structure type players-versus:rock_formation (RockFormationStructure), on the world's grid of the
  structure set rock_formations: a monolith, spires, towers, great boulders, a massif. They generate in the biomes of
  the tag #players-versus:has_structure/rock_formation: the rocky biomes (tools/transition-biomes.py adds them).

A rock is a template (RockTemplate): its bodies, each a shape (RockShape) with sizes the seed picks from, and where
around the formation's middle it stands.
"""
import json
import os

MOD = "players-versus"
DATA = os.path.join("src", "main", "resources", "data")

FEATURES = {
    "boulder": {"bodies": [{"shape": "boulder"}]},
    "boulder_pile": {"bodies": [{"shape": "pile"}]},
    "crag": {"bodies": [{"shape": "crag"}]},
}

FORMATIONS = {
    # The leaning egg with its overgrown top and ramp, a stone or two fallen from it
    "monolith": ({"bodies": [{"shape": "monolith"},
                             {"shape": "boulder", "count": [0, 2], "distance": [17, 22], "cover": 0.2}]}, 3),
    # A great fluted spire with its needle, now and then a tower beyond, piles of fallen rock
    "spires": ({"bodies": [{"shape": "spire"},
                           {"shape": "tower", "count": [0, 1], "distance": [30, 40]},
                           {"shape": "pile", "count": [1, 2], "distance": [23, 30]}]}, 2),
    # A tower and needles around it
    "towers": ({"bodies": [{"shape": "tower"},
                           {"shape": "needle", "count": [1, 2], "distance": [15, 20]}]}, 2),
    # A rounded boulder as tall as a house, smaller ones and piles around it
    "great_boulders": ({"bodies": [{"shape": "boulder", "height": [12, 18], "radius": [7, 10], "cover": 0.1},
                                   {"shape": "boulder", "count": [1, 3], "distance": [14, 19], "height": [5, 9]},
                                   {"shape": "pile", "count": [0, 2], "distance": [16, 22]}]}, 3),
    # A wide mass of fluted cliffs with several tops
    "massif": ({"bodies": [{"shape": "massif"}]}, 1),
}

FORMATION_BIOMES = f"#{MOD}:has_structure/rock_formation"


def bare(feature):
    """A feature placed as it is: a selector's choice, placed where the selector is."""
    return {"feature": feature, "placement": []}


# Here and there in the middle biomes: a boulder mostly, now and then a pile or a crag. A random selector tries its
# choices in turn: a pile in 0.25, a crag in 0.75 x 0.2 = 0.15, a boulder in the rest
SPARSE = {"type": "minecraft:random_selector", "default": bare(f"{MOD}:rock/boulder"),
          "features": [{"chance": 0.25, "feature": bare(f"{MOD}:rock/boulder_pile")}, {"chance": 0.2, "feature": bare(f"{MOD}:rock/crag")}]}


def rock_placement(rarity, count=None, climate=True):
    """Where a rock goes: in about one chunk of `rarity` (of those `count` times), on rock ground ({MOD}:rock_ground)."""
    placement = [{"type": "minecraft:rarity_filter", "chance": rarity}]
    if count:
        placement.append({"type": "minecraft:count", "count": {"type": "minecraft:uniform", "min_inclusive": count[0], "max_inclusive": count[1]}})
    ground = {"type": f"{MOD}:rock_ground"}
    if not climate:
        ground["climate"] = False
    placement += [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}, ground, {"type": "minecraft:biome"}]
    return placement


# The rocky jungle's: crags mostly (as karst stands among the trees); the rocky birch forest's: boulders, now and then a crag
CRAGGY = {"type": "minecraft:random_selector", "default": bare(f"{MOD}:rock/boulder"),
          "features": [{"chance": 0.5, "feature": bare(f"{MOD}:rock/crag")}, {"chance": 0.2, "feature": bare(f"{MOD}:rock/boulder_pile")}]}
BOULDERS = {"type": "minecraft:random_selector", "default": bare(f"{MOD}:rock/boulder"),
            "features": [{"chance": 0.25, "feature": bare(f"{MOD}:rock/crag")}]}


def rocky_placement(count):
    """In a rocky biome: `count` places a chunk, kept on rock ground."""
    return [{"type": "minecraft:count", "count": {"type": "minecraft:uniform", "min_inclusive": count[0], "max_inclusive": count[1]}},
            {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}, {"type": f"{MOD}:rock_ground"},
            {"type": "minecraft:biome"}]


# Placed features: their biomes list them in the step before trees (CustomWorldgen adds them to vanilla's and the mod's
# middle biomes; the rocky biomes list theirs, tools/transition-biomes.py). A meadow's cluster skips the rocks' climate:
# meadows are cool, high and little eroded
PLACED = {
    # Many: about 9 places in 10 are refused even there (the ground rises and falls), so many are tried; on a level
    # stretch they stand close, a field of rocks
    "rocky_plains": {"feature": f"{MOD}:rock/sparse", "placement": rocky_placement((2, 4))},
    "rocky_jungle": {"feature": f"{MOD}:rock/craggy", "placement": rocky_placement((2, 5))},
    "rocky_birch_forest": {"feature": f"{MOD}:rock/boulders", "placement": rocky_placement((4, 8))},
    # Most of the middle biomes' ground is hilly: a place is tried in one chunk of 5, and kept on level ground
    "sparse": {"feature": f"{MOD}:rock/sparse", "placement": rock_placement(5)},
    "meadow_cluster": {"feature": f"{MOD}:rock/sparse", "placement": rock_placement(48, count=(2, 4), climate=False)},
}


def write_json(path, value):
    path = os.path.join(DATA, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as file:
        json.dump(value, file, indent=2)
        file.write("\n")


def main():
    for name, rock in FEATURES.items():
        write_json(f"{MOD}/worldgen/feature/rock/{name}.json", {"type": f"{MOD}:rock", "rock": rock})
    write_json(f"{MOD}/worldgen/feature/rock/sparse.json", SPARSE)
    write_json(f"{MOD}/worldgen/feature/rock/craggy.json", CRAGGY)
    write_json(f"{MOD}/worldgen/feature/rock/boulders.json", BOULDERS)
    for name, placed in PLACED.items():
        write_json(f"{MOD}/worldgen/placed_feature/rock/{name}.json", placed)
    for name, (rock, _) in FORMATIONS.items():
        write_json(f"{MOD}/worldgen/structure/rock/{name}.json", {
            "type": f"{MOD}:rock_formation", "biomes": FORMATION_BIOMES, "step": "surface_structures",
            "spawn_overrides": {}, "terrain_adaptation": "none", "rock": rock})
    write_json(f"{MOD}/worldgen/structure_set/rock_formations.json", {
        "structures": [{"structure": f"{MOD}:rock/{name}", "weight": weight} for name, (_, weight) in FORMATIONS.items()],
        # In the rocky biomes, about one formation in every seven chunks by seven, none closer than three to the next
        "placement": {"type": "minecraft:random_spread", "salt": 1734120691, "spacing": 7, "separation": 3}})
    tag = os.path.join(DATA, MOD, "tags", "worldgen", "biome", "has_structure", "rock_formation.json")
    if not os.path.exists(tag):
        write_json(f"{MOD}/tags/worldgen/biome/has_structure/rock_formation.json", {"replace": False, "values": []})


if __name__ == "__main__":
    main()
