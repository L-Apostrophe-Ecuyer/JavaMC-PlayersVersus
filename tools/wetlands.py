#!/usr/bin/env python3
"""Writes the wetlands: the marsh ground, the cold swamp and the marsh clearings. Run from the repository root.

    python3 tools/wetlands.py <vanilla>     (<vanilla> holds data/minecraft, from the game's jar)

- The marsh (MarshFeature, feature type players-versus:marsh): the ground of the swamps from one smooth noise, run once a
  chunk before anything grows: islands and channels near the water line, rings of gray mud, brown mud, an edge block
  and grass around every pool. The swamp's own (rooted dirt at the edge), the cold swamp's (podzol), and the swamp
  rim's (the humid oak forest: drier, its ground's blocks only, no islands dug or raised).
- The cold swamp (PvBiomeLayout puts it in the cold half of swamps, next to taigas): the swamp's underground, spruce and
  birch by noise as the swamp's trees are, ferns, berries and the taiga's grass, cooler colours, the swamp's and the
  taiga's animals, cold frogs, witch huts.
- The marsh clearings: where the trees' noise leaves none (noise_based_count with a negative ratio: the trees' own
  noise, upside down), tall grass, reeds and blue orchids fill the islands.
"""
import copy
import json
import os
import sys

MOD = "players-versus"
DATA = os.path.join("src", "main", "resources", "data")
VEGETATION = 9
SPAWNS = "minecraft:gameplay/natural_mob_spawns"

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
transition = __import__("transition-biomes")
lemons = __import__("lemons")
write_json = transition.write_json


def vanilla_placed(vanilla, name):
    with open(os.path.join(vanilla, "data", "minecraft", "worldgen", "placed_feature", name + ".json")) as file:
        return json.load(file)


def marsh():
    swamps = {"marsh": {"biomes": ["minecraft:swamp"], "edge": {"id": "minecraft:rooted_dirt"}},
              "marsh_cold": {"biomes": [f"{MOD}:cold_swamp"], "edge": {"id": "minecraft:podzol", "properties": {"snowy": "false"}}},
              "marsh_rim": {"biomes": [f"{MOD}:humid_oak_forest"], "edge": {"id": "minecraft:rooted_dirt"}, "dryness": 0.25, "reshape": False}}
    for name, config in swamps.items():
        write_json(f"{MOD}/worldgen/feature/{name}.json", dict({"type": f"{MOD}:marsh"}, **config))
        # No placement: it runs once a chunk and shapes the chunk's columns of its biomes itself
        write_json(f"{MOD}/worldgen/placed_feature/{name}.json", {"feature": f"{MOD}:{name}", "placement": []})


def clearings(vanilla):
    """Tall grass, reeds and blue orchids where the swamp's trees leave clearings: their noise, upside down."""
    for name, source, ratio in [("marsh_tall_grass", "patch_tall_grass", -3), ("marsh_reeds", "patch_sugar_cane_swamp", -3),
                                ("marsh_flowers", "flower_swamp", -1)]:
        placed = vanilla_placed(vanilla, source)
        placement = [step for step in placed["placement"] if step["type"] != "minecraft:rarity_filter"]
        # The swamp's trees: noise_based_count, ratio 9, factor 8, offset 0.05; where they have none, these have most
        placement.insert(0, {"type": "minecraft:noise_based_count", "noise_to_count_ratio": ratio, "noise_factor": 8, "noise_offset": 0.05})
        write_json(f"{MOD}/worldgen/placed_feature/{name}.json", {"feature": placed["feature"], "placement": placement})


MARSH_CLEARINGS = [f"{MOD}:marsh_tall_grass", f"{MOD}:marsh_reeds", f"{MOD}:marsh_flowers"]


def swamp():
    path = os.path.join(DATA, "minecraft", "worldgen", "biome", "swamp.json")
    with open(path) as file:
        biome = json.load(file)
    plants = [f for f in biome["features"][VEGETATION] if f not in [f"{MOD}:swamp_patch_vegetation", f"{MOD}:marsh"] + MARSH_CLEARINGS]
    trees = plants.index(f"{MOD}:trees_swamp")
    biome["features"][VEGETATION] = [f"{MOD}:marsh"] + plants[:trees + 1] + MARSH_CLEARINGS + plants[trees + 1:]
    write_json("minecraft/worldgen/biome/swamp.json", biome)

    # The small mangrove's ground ring: gray mud only, as the marsh's wettest ring
    path = os.path.join(DATA, MOD, "worldgen", "feature", "swamp_small_mangrove.json")
    with open(path) as file:
        mangrove = json.load(file)
    for decorator in mangrove["decorators"]:
        if decorator["type"] == "minecraft:alter_ground":
            decorator["provider"]["rules"][0]["then"] = {"id": f"{MOD}:gray_mud"}
    write_json(f"{MOD}/worldgen/feature/swamp_small_mangrove.json", mangrove)


def cold_swamp(vanilla):
    main, taiga = transition.biome(vanilla, "minecraft:swamp"), transition.biome(vanilla, "minecraft:taiga")
    made = copy.deepcopy(main)
    made["temperature"] = 0.25
    made["downfall"] = 0.8
    made["has_precipitation"] = True
    # The swamp's colours, cooled toward the taiga's
    made["effects"] = {"water_color": "#3a7484", "grass_color": "#6a8a4a", "foliage_color": "#4f7a46", "dry_foliage_color": "#7b5334"}
    made["attributes"]["minecraft:visual/water_fog_color"] = "#16262b"
    made["features"][VEGETATION] = [
        f"{MOD}:marsh_cold", "minecraft:glow_lichen", "minecraft:patch_large_fern", f"{MOD}:trees_cold_swamp", *MARSH_CLEARINGS,
        "minecraft:patch_grass_taiga_2", "minecraft:patch_waterlily", "minecraft:brown_mushroom_taiga", "minecraft:red_mushroom_taiga",
        "minecraft:patch_sugar_cane_swamp", "minecraft:patch_pumpkin", "minecraft:patch_firefly_bush_near_water_swamp",
        "minecraft:seagrass_swamp", "minecraft:patch_berry_common"]
    made["attributes"][SPAWNS]["argument"]["spawns_by_category"]["creature"] = transition.merged_creatures([main, taiga])
    write_json(f"{MOD}/worldgen/biome/cold_swamp.json", made)

    write_json(f"{MOD}/worldgen/feature/trees_cold_swamp.json", transition.selector(
        "minecraft:spruce_checked", ("minecraft:birch_checked", 0.25), ("minecraft:fallen_spruce_tree", 0.05), ("minecraft:fallen_birch_tree", 0.03)))
    write_json(f"{MOD}/worldgen/placed_feature/trees_cold_swamp.json",
               transition.noise_trees(f"{MOD}:trees_cold_swamp", ratio=9, offset=0.05, max_water=2))


def tags():
    cold = f"{MOD}:cold_swamp"
    for tag in ["is_overworld", "has_structure/mineshaft", "has_structure/swamp_hut", "spawns_cold_variant_frogs",
                "allows_surface_slime_spawns", "has_closer_water_fog"]:
        transition.add_to_tag(f"minecraft/tags/worldgen/biome/{tag}.json", cold)


NAMES = {"en": {"biome.players-versus.cold_swamp": "Cold Swamp"}, "fr": {"biome.players-versus.cold_swamp": "Marais froid"}}


def main(vanilla):
    marsh()
    clearings(vanilla)
    swamp()
    cold_swamp(vanilla)
    tags()
    lemons.add_names(NAMES)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
