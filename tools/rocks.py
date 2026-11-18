#!/usr/bin/env python3
"""Writes the rocks' data: run from the repository root.

    python3 tools/rocks.py

- Small rocks, feature type players-versus:rock (RockFeature), each within 16 blocks of its origin: a boulder, a pile, a
  crag. The rocky jungle's only (among its crags they belong; alone on a plain they looked out of place): placed
  everywhere in it, on steeper ground too, on rock ground (the players-versus:rock_ground filter; tools/transition-biomes.py
  lists the placed feature in the biome).
- Large rocks, structure type players-versus:rock_formation (RockFormationStructure), on the world's grid of the
  structure set rock_formations: a monolith, spires, towers, great boulders, a massif. They generate in the biomes of
  the tag #players-versus:has_structure/rock_formation: the rocky biomes (tools/transition-biomes.py adds them).
- The same formations capped for the cold (packed ice in place of grass and moss, rock under it, no plants): icy_*, in
  the same set, in the biomes of #players-versus:has_structure/icy_rock_formation (the rocky snowy plains); and
  scattered_icy_*, further apart (their own set, scattered_icy_rock_formations), in the biomes of
  #players-versus:has_structure/scattered_icy_rock_formation (the cold plains).

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

# The formations' kinds: gray in the rocky biomes, icy in the snowy ones, by their biome tags (tools/transition-biomes.py
# fills them)
FORMATION_TAGS = {"": "rock_formation", "icy_": "icy_rock_formation", "scattered_icy_": "scattered_icy_rock_formation"}

# Capped for the cold: packed ice where grass and moss would grow, and hanging down the faces from the edges as moss
# would; the rock itself under it in place of the earth (dirt, packed mud); nothing growing on it
ICY = {"grass": "minecraft:packed_ice", "moss": "minecraft:packed_ice", "soil": "minecraft:stone", "mud": "minecraft:stone", "plants": []}


def bare(feature):
    """A feature placed as it is: a selector's choice, placed where the selector is."""
    return {"feature": feature, "placement": []}


def uniform(low, high):
    return {"type": "minecraft:uniform", "min_inclusive": low, "max_inclusive": high}


# The rocky jungle's: crags mostly (as karst stands among the trees). A random selector tries its choices in turn: a crag
# in 0.5, a pile in 0.5 x 0.2 = 0.1, a boulder in the rest
CRAGGY = {"type": "minecraft:random_selector", "default": bare(f"{MOD}:rock/boulder"),
          "features": [{"chance": 0.5, "feature": bare(f"{MOD}:rock/crag")}, {"chance": 0.2, "feature": bare(f"{MOD}:rock/boulder_pile")}]}


# How far the ground may rise and fall under a rocky biome's rock, over the 16 blocks read: its hillsides too (the
# rock's foot goes on down to the ground where it falls away)
ROCKY_LEVEL = 6


def rocky_placement(count, level=ROCKY_LEVEL):
    """In a rocky biome: `count` places a chunk, kept on ground as steep as `level`. The biome is the rocks' climate (none
    is in the 3D-noise terrain's)."""
    return [{"type": "minecraft:count", "count": uniform(*count)},
            {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
            {"type": f"{MOD}:rock_ground", "climate": False, "level": level}, {"type": "minecraft:biome"}]


# Placed features: their biome lists them in the step before trees (tools/transition-biomes.py)
PLACED = {
    # Everywhere: several places a chunk, on its hillsides too, about one rock a chunk kept (the jungle's ground is hilly:
    # many places are tried)
    "rocky_jungle": {"feature": f"{MOD}:rock/craggy", "placement": rocky_placement((6, 9))},
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
    write_json(f"{MOD}/worldgen/feature/rock/craggy.json", CRAGGY)
    for name, placed in PLACED.items():
        write_json(f"{MOD}/worldgen/placed_feature/rock/{name}.json", placed)
    for kind, tag in FORMATION_TAGS.items():
        for name, (rock, _) in FORMATIONS.items():
            write_json(f"{MOD}/worldgen/structure/rock/{kind}{name}.json", {
                "type": f"{MOD}:rock_formation", "biomes": f"#{MOD}:has_structure/{tag}", "step": "surface_structures",
                "spawn_overrides": {}, "terrain_adaptation": "none", "rock": dict(rock, palette=ICY) if kind else rock})
        path = f"{MOD}/tags/worldgen/biome/has_structure/{tag}.json"
        if not os.path.exists(os.path.join(DATA, path)):
            write_json(path, {"replace": False, "values": []})
    # A set tries its formations in turn, by weight, until one may stand where it is: the gray ones in the rocky biomes,
    # the icy ones in the rocky snowy plains
    write_json(f"{MOD}/worldgen/structure_set/rock_formations.json", {
        "structures": [{"structure": f"{MOD}:rock/{kind}{name}", "weight": weight} for kind in ("", "icy_") for name, (_, weight) in FORMATIONS.items()],
        # In the rocky biomes, about one formation in every seven chunks by seven, none closer than three to the next
        "placement": {"type": "minecraft:random_spread", "salt": 1734120691, "spacing": 7, "separation": 3}})
    write_json(f"{MOD}/worldgen/structure_set/scattered_icy_rock_formations.json", {
        "structures": [{"structure": f"{MOD}:rock/scattered_icy_{name}", "weight": weight} for name, (_, weight) in FORMATIONS.items()],
        # In the cold plains, a third as many: one in every twelve chunks by twelve, none closer than five to the next
        "placement": {"type": "minecraft:random_spread", "salt": 905181357, "spacing": 12, "separation": 5}})


if __name__ == "__main__":
    main()
