#!/usr/bin/env python3
"""The refinement pass's data for the older biomes and vanilla's: run from the repository root, with the game's data
extracted where `vanilla` points (its data/minecraft/...).

    python3 tools/refine-biomes.py <vanilla>

- River banks: what grows and lies at the water's edge in the temperate biomes. CustomWorldgen's biome modifications put
  them in the biomes of two tags, in the Players Versus world type only (the players-versus:players_versus_world filter):
  - riverbank_mud: muddy banks, the mod's gray mud right at the water and brown mud behind it, before anything grows;
  - riverbank_reeds: sugar cane along the water's edge;
  - riverbank_plants: tall grass, large ferns and bushes within two blocks of the water;
  - riverbank_willows: now and then a willow (a swamp oak, vines hanging from its leaves) on the bank, where the trees are
    deciduous.
  players-versus:has_riverbanks holds the temperate biomes beside water (and the river: its own banks, whatever its
  climate; reeds grow by a desert's river, and nothing that needs grass grows on its sand);
  players-versus:has_riverbank_willows those of them whose trees are deciduous.
- Animals (tools/animals.py chooses them): the older, hand-made biomes' (their files rewritten with their animals and, for
  some, a vanilla biome's monsters), the game's tags for warm and cold farm animals and frogs, and its wolf variants (each
  file vanilla's, with a second spawn condition for the mod's biomes of that coat).
"""
import json
import os
import sys

import animals

MOD = "players-versus"
DATA = os.path.join("src", "main", "resources", "data")

SIDES = ((1, 0), (-1, 0), (0, 1), (0, -1))
AIR = {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}
SOLID_BELOW = {"type": "minecraft:solid", "offset": [0, -1, 0]}
PV_WORLD = {"type": f"{MOD}:players_versus_world"}
# How far from the water (along x and z, level with the ground or a block lower) the plants and willows stand at most
REACH = 2


def water_at(dx, dy, dz):
    return {"type": "minecraft:matching_fluids", "fluids": "minecraft:water", "offset": [dx, dy, dz]}


def beside_water(dy):
    """Water beside the block `dy` above the position (below, if negative), on one of its four sides."""
    return {"type": "minecraft:any_of", "predicates": [water_at(dx, dy, dz) for dx, dz in SIDES]}


def near_water():
    """Water within REACH blocks along x and z, level with the ground under the position or a block lower."""
    return {"type": "minecraft:any_of", "predicates": [water_at(dx, dy, dz) for dy in (-1, -2) for dx in range(-REACH, REACH + 1)
                                                       for dz in range(-REACH, REACH + 1) if (dx, dz) != (0, 0)]}


def all_of(*predicates):
    return {"type": "minecraft:all_of", "predicates": list(predicates)}


def keep(predicate):
    return {"type": "minecraft:block_predicate_filter", "predicate": predicate}


def trapezoid(spread):
    return {"type": "minecraft:trapezoid", "min": -spread, "max": spread, "plateau": 0}


def at_the_waters_edge(tries, heightmap="MOTION_BLOCKING_NO_LEAVES"):
    """`tries` places a chunk, kept where the ground under them meets water: the water's edge, on land."""
    return [{"type": "minecraft:count", "count": tries}, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": heightmap}, PV_WORLD, {"type": "minecraft:biome"},
            keep(all_of(AIR, SOLID_BELOW, beside_water(-1)))]


def around(count, spread, *predicates):
    """`count` tries around each place, up to `spread` blocks out along x and z and a block up or down, kept in the air
    where the predicates hold."""
    return [{"type": "minecraft:count", "count": count},
            {"type": "minecraft:offset", "x": trapezoid(spread), "y": trapezoid(1), "z": trapezoid(spread)},
            keep(all_of(AIR, *predicates))]


def bare(feature):
    return {"feature": feature, "placement": []}


SUGAR_CANE = {"id": "minecraft:sugar_cane", "properties": {"age": "0"}}
OAK_SAPLING = {"id": "minecraft:oak_sapling", "properties": {"stage": "0"}}
BANK_GROUND = ["minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:podzol", "minecraft:rooted_dirt"]

FEATURES = {
    # Gray mud where the bank meets the water, brown mud behind it, two deep (the grass and the dirt under it)
    "riverbank_mud": {"type": "minecraft:disk", "half_height": 1, "radius": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 2},
                      "state_provider": {"type": "minecraft:rule_based", "fallback": {"id": f"{MOD}:brown_mud"},
                                         "rules": [{"if_true": beside_water(0), "then": {"id": f"{MOD}:gray_mud"}}]},
                      "target": {"type": "minecraft:matching_blocks", "blocks": BANK_GROUND}},
    # Tall grass mostly, large ferns and bushes among it. A random selector tries its choices in turn: a large fern in 0.3,
    # a bush in 0.7 x 0.3 = 0.21, tall grass in the rest
    "riverbank_plants": {"type": "minecraft:random_selector", "default": bare("minecraft:tall_grass"),
                         "features": [{"chance": 0.3, "feature": bare("minecraft:large_fern")}, {"chance": 0.3, "feature": bare("minecraft:bush")}]},
}

PLACED = {
    # A few patches of mud along the water's edge a chunk: the ground itself, under the place
    "riverbank_mud": {"feature": f"{MOD}:riverbank_mud",
                      "placement": at_the_waters_edge(8, "OCEAN_FLOOR") + [{"type": "minecraft:offset", "x": 0, "y": -1, "z": 0}]},
    # Clumps of sugar cane right at the water (sugar cane needs it beside its ground)
    "riverbank_reeds": {"feature": "minecraft:sugar_cane",
                        "placement": at_the_waters_edge(10) + around(8, 2, {"type": "minecraft:would_survive", "state": SUGAR_CANE})},
    # Clumps of tall grass, ferns and bushes, near the water only
    "riverbank_plants": {"feature": f"{MOD}:riverbank_plants", "placement": at_the_waters_edge(10) + around(10, 3, near_water())},
    # Now and then a willow on the bank, near the water, on dry ground
    "riverbank_willows": {"feature": "minecraft:swamp_oak", "placement": [
        {"type": "minecraft:count", "count": 4}, {"type": "minecraft:in_square"},
        {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}, PV_WORLD, {"type": "minecraft:biome"},
        keep(all_of(near_water(), {"type": "minecraft:would_survive", "state": OAK_SAPLING}))]},
}

# The temperate biomes beside water, vanilla's and the mod's; the river's own banks
RIVERBANKS = ["minecraft:river", "minecraft:plains", "minecraft:sunflower_plains", "minecraft:forest", "minecraft:flower_forest",
              "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "minecraft:dark_forest", "minecraft:dappled_forest",
              "minecraft:meadow", "minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
              f"{MOD}:forest_edge", f"{MOD}:mixed_forest", f"{MOD}:old_oak_forest", f"{MOD}:prairie", f"{MOD}:warm_oak_forest",
              f"{MOD}:dark_birch_forest", f"{MOD}:warm_dark_forest", f"{MOD}:dark_taiga_forest", f"{MOD}:birch_taiga_forest",
              f"{MOD}:dappled_taiga", f"{MOD}:sparse_dappled_forest", f"{MOD}:tuscan_grove", f"{MOD}:rocky_birch_forest"]
# Where the trees are deciduous (not the river: it runs through every climate)
WILLOWS = ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest",
           "minecraft:old_growth_birch_forest", "minecraft:dark_forest", "minecraft:dappled_forest", f"{MOD}:forest_edge",
           f"{MOD}:old_oak_forest", f"{MOD}:prairie", f"{MOD}:warm_oak_forest", f"{MOD}:dark_birch_forest", f"{MOD}:warm_dark_forest"]


def write_json(path, value):
    path = os.path.join(DATA, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as file:
        json.dump(value, file, indent=2)
        file.write("\n")


def riverbanks():
    for name, feature in FEATURES.items():
        write_json(f"{MOD}/worldgen/feature/{name}.json", feature)
    for name, placed in PLACED.items():
        write_json(f"{MOD}/worldgen/placed_feature/{name}.json", placed)
    write_json(f"{MOD}/tags/worldgen/biome/has_riverbanks.json", {"replace": False, "values": RIVERBANKS})
    write_json(f"{MOD}/tags/worldgen/biome/has_riverbank_willows.json", {"replace": False, "values": WILLOWS})


# The older biomes, made by hand before the generators (the generated ones get their animals from their generator)
HAND_MADE = ["birch_taiga_forest", "cold_beach", "cold_plains", "cold_taiga", "dappled_taiga", "dark_birch_forest", "dark_taiga_forest",
             "desert_oasis", "mountainside_forest", "mountainside_forest_warm", "mountainside_jungle", "sparse_dappled_forest"]


def read_json(path):
    with open(path) as file:
        return json.load(file)


def vanilla_biome(vanilla, name):
    return read_json(os.path.join(vanilla, "data", "minecraft", "worldgen", "biome", name.split(":")[1] + ".json"))


def add_to_tag(path, *values):
    full = os.path.join(DATA, path)
    tag = read_json(full) if os.path.exists(full) else {"replace": False, "values": []}
    for value in values:
        if value not in tag["values"]:
            tag["values"].append(value)
    write_json(path, tag)


def mobs(vanilla):
    for name in HAND_MADE:
        path = os.path.join(DATA, MOD, "worldgen", "biome", name + ".json")
        biome = read_json(path)
        animals.set_spawns(biome, name, lambda other: vanilla_biome(vanilla, other))
        write_json(f"{MOD}/worldgen/biome/{name}.json", biome)
    tags = "minecraft/tags/worldgen/biome"
    warm, cold = [f"{MOD}:{name}" for name in animals.WARM], [f"{MOD}:{name}" for name in animals.COLD]
    add_to_tag(f"{tags}/spawns_warm_variant_farm_animals.json", *warm)
    add_to_tag(f"{tags}/spawns_warm_variant_frogs.json", *warm)
    add_to_tag(f"{tags}/spawns_cold_variant_farm_animals.json", *cold)
    add_to_tag(f"{tags}/spawns_cold_variant_frogs.json", *[f"{MOD}:{name}" for name in animals.COLD_FROGS])
    for coat, biomes in animals.WOLF_COATS.items():
        variant = read_json(os.path.join(vanilla, "data", "minecraft", "wolf_variant", coat + ".json"))
        variant["spawn_conditions"].append({"condition": {"type": "minecraft:biome", "biomes": [f"{MOD}:{name}" for name in biomes]}, "priority": 1})
        write_json(f"minecraft/wolf_variant/{coat}.json", variant)


def main(vanilla):
    riverbanks()
    mobs(vanilla)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
