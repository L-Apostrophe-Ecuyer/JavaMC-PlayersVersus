#!/usr/bin/env python3
"""Writes the rocks' data: run from the repository root.

    python3 tools/rocks.py

- Small rocks, feature type players-versus:rock (RockFeature), each within 16 blocks of its origin: a boulder (two or
  three rounded stones), a pile, a crag. Biomes place them with placed features of their own (tools/transition-biomes.py
  for the rocky biomes).
- Large rocks, structure type players-versus:rock_formation (RockFormationStructure), on the world's grid of the
  structure set rock_formations: a monolith, spires, towers, great boulders, a massif. They generate in the biomes of
  the tag #players-versus:has_structure/rock_formation, empty until their looks are approved.

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


def write_json(path, value):
    path = os.path.join(DATA, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as file:
        json.dump(value, file, indent=2)
        file.write("\n")


def main():
    for name, rock in FEATURES.items():
        write_json(f"{MOD}/worldgen/feature/rock/{name}.json", {"type": f"{MOD}:rock", "rock": rock})
    for name, (rock, _) in FORMATIONS.items():
        write_json(f"{MOD}/worldgen/structure/rock/{name}.json", {
            "type": f"{MOD}:rock_formation", "biomes": FORMATION_BIOMES, "step": "surface_structures",
            "spawn_overrides": {}, "terrain_adaptation": "none", "rock": rock})
    write_json(f"{MOD}/worldgen/structure_set/rock_formations.json", {
        "structures": [{"structure": f"{MOD}:rock/{name}", "weight": weight} for name, (_, weight) in FORMATIONS.items()],
        # About one formation in every ten chunks by ten, none closer than four chunks to the next
        "placement": {"type": "minecraft:random_spread", "salt": 1734120691, "spacing": 10, "separation": 4}})
    tag = os.path.join(DATA, MOD, "tags", "worldgen", "biome", "has_structure", "rock_formation.json")
    if not os.path.exists(tag):
        write_json(f"{MOD}/tags/worldgen/biome/has_structure/rock_formation.json", {"replace": False, "values": []})


if __name__ == "__main__":
    main()
