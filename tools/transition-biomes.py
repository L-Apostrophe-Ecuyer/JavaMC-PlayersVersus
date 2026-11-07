#!/usr/bin/env python3
"""Writes the transition biomes and their features: run from the repository root with vanilla's extracted data.

    python3 tools/transition-biomes.py <vanilla>     (<vanilla> holds data/minecraft, from the game's jar)

Each transition biome lies between two biomes (PvBiomeLayout's warm band and swamp rim) and is made from both:

- what lies under it (lakes, ores, springs, the carvers, the frost) is its main neighbour's, the mod's version where the
  mod overrides it, so every biome keeps the same order of features (BiomeFeatureOrderTest);
- its plants are its own: trees from both sides chosen by a random selector and placed by noise (noise_based_count, as
  the savanna's are), so they gather in groves and leave clearings instead of standing evenly; then the neighbours'
  flowers and grass in the order their own biomes place them;
- its grass and leaves take the colour halfway between the two (its temperature and downfall, or its colours, between
  theirs), so the game's colour blending fades one into the other across the border;
- its animals are both neighbours' (the more common of each kind's two weights), its monsters, sky, music and the rest
  its main neighbour's.
"""
import copy
import json
import os
import sys

MOD = "players-versus"
DATA = os.path.join("src", "main", "resources", "data")
ASSETS = os.path.join("src", "main", "resources", "assets")
CARVERS = ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"]
SPAWNS = "minecraft:gameplay/natural_mob_spawns"
VEGETATION = 9
UNDERGROUND_ORES = 6


def write_json(path, value):
    path = os.path.join(DATA, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as file:
        json.dump(value, file, indent=2)
        file.write("\n")


def biome(vanilla, name):
    """A biome as the game has it with the mod: the mod's override, else vanilla's."""
    namespace, path = name.split(":")
    for root in [DATA, os.path.join(vanilla, "data")]:
        file = os.path.join(root, namespace, "worldgen", "biome", path + ".json")
        if os.path.exists(file):
            with open(file) as handle:
                return json.load(handle)
    raise FileNotFoundError(name)


def noise_trees(feature, ratio, offset, factor=8, max_water=0, survives=None):
    """
    Placed by noise, as the savanna's trees are: a patch of ground gets up to `ratio` tries a chunk, another none. A
    feature that isn't a selector of checked trees says which sapling must survive where it goes.
    """
    placement = [
        {"type": "minecraft:noise_based_count", "noise_to_count_ratio": ratio, "noise_factor": factor, "noise_offset": offset},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:surface_water_depth_filter", "max_water_depth": max_water},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
        {"type": "minecraft:biome"}]
    if survives:
        placement.append({"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": survives}})
    return {"feature": feature, "placement": placement}


def selector(default, *chances):
    return {"type": "minecraft:random_selector", "default": default,
            "features": [{"chance": chance, "feature": feature} for feature, chance in chances]}


SWAMP_OAK = {"feature": "minecraft:swamp_oak", "placement": [
    {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": {"id": "minecraft:oak_sapling", "properties": {"stage": "0"}}}}]}

# Each biome: its two neighbours (the first one main), its climate for colours, its plants and its trees
BIOMES = {
    "tuscan_grove": {
        "neighbours": ["minecraft:savanna", "minecraft:plains"],
        # Between the plains' grass (0.8, 0.4) and the savanna's (2.0 counts as 1.0, 0.0): drier, more olive than the plains
        "temperature": 0.95, "downfall": 0.25, "has_precipitation": True,
        "trees": selector("minecraft:oak_checked", ("minecraft:fancy_oak_checked", 0.3), ("minecraft:acacia_checked", 0.12)),
        "tree_placement": dict(ratio=5, offset=0.0),
        # Lemon bushes in orchards of their own, on a coarser noise than the oaks', so the two don't share their patches
        "extra": [("lemon_orchards", noise_trees(f"{MOD}:lemon_bush", 10, -0.1, factor=14,
                                                     survives={"id": f"{MOD}:lemon_sapling", "properties": {"stage": "0"}}))],
        "vegetation": ["minecraft:glow_lichen", f"{MOD}:lemon_orchards", f"{MOD}:trees_tuscan_grove", "minecraft:flower_plains",
                       "minecraft:patch_grass_savanna", "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal",
                       "minecraft:patch_pumpkin", "minecraft:patch_sugar_cane", "minecraft:patch_firefly_bush_near_water"],
    },
    "warm_oak_forest": {
        "neighbours": ["minecraft:sparse_jungle", "minecraft:forest"],
        # Between the forest's (0.7, 0.8) and the jungle's (0.95, 0.9)
        "temperature": 0.825, "downfall": 0.85, "has_precipitation": True,
        "trees": selector("minecraft:oak_checked", ("minecraft:jungle_bush", 0.22), ("minecraft:jungle_tree", 0.18),
                          ("minecraft:fancy_oak_checked", 0.12), ("minecraft:birch_checked", 0.06)),
        "tree_placement": dict(ratio=12, offset=0.45),
        "vegetation": ["minecraft:glow_lichen", f"{MOD}:trees_warm_oak_forest", "minecraft:flower_warm", "minecraft:patch_grass_jungle",
                       "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal", "minecraft:patch_pumpkin",
                       "minecraft:patch_sugar_cane", "minecraft:patch_firefly_bush_near_water", "minecraft:vines", "minecraft:patch_melon_sparse"],
    },
    "dark_jungle_forest": {
        "neighbours": ["minecraft:dark_forest", "minecraft:jungle"],
        # Between the dark forest's (0.7, 0.8) and the jungle's (0.95, 0.9), darkened as the dark forest's grass is
        "temperature": 0.825, "downfall": 0.85, "has_precipitation": True, "grass_color_modifier": "dark_forest",
        "trees": selector("minecraft:dark_oak_checked", (f"{MOD}:dark_oak_with_vines_checked", 0.35), ("minecraft:jungle_tree", 0.18),
                          ("minecraft:jungle_bush", 0.15), ("minecraft:mega_jungle_tree_checked", 0.05)),
        "tree_placement": dict(ratio=16, offset=0.55),
        "vegetation": ["minecraft:glow_lichen", f"{MOD}:trees_dark_jungle_forest", "minecraft:flower_default", "minecraft:patch_grass_jungle",
                       "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal", "minecraft:patch_leaf_litter",
                       "minecraft:patch_pumpkin", "minecraft:patch_sugar_cane", "minecraft:patch_firefly_bush_near_water",
                       "minecraft:vines", "minecraft:patch_melon_sparse"],
    },
    "humid_oak_forest": {
        "neighbours": ["minecraft:swamp", "minecraft:forest"],
        # Between the forest's and the swamp's: its colours between theirs (the swamp's grass and leaves are its own)
        "temperature": 0.75, "downfall": 0.9, "has_precipitation": True,
        "colors": {"grass_color": "#5f8c42", "foliage_color": "#5e8a3a", "dry_foliage_color": "#7b5334", "water_color": "#507a9e"},
        "trees": selector("minecraft:oak_checked", (SWAMP_OAK, 0.35), ("minecraft:fancy_oak_checked", 0.08), ("minecraft:fallen_oak_tree", 0.06)),
        "tree_placement": dict(ratio=12, offset=0.45, max_water=2),
        # Under its water, brown mud on the sand and gravel: disks that only replace those, after the clay's
        "underground_extra": [("humid_mud_disks", {"type": "minecraft:disk", "half_height": 1,
                                                   "radius": {"type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 4},
                                                   "state_provider": {"type": "minecraft:rule_based", "fallback": {"id": f"{MOD}:brown_mud"}, "rules": []},
                                                   "target": {"type": "minecraft:matching_blocks", "blocks": ["minecraft:sand", "minecraft:gravel", "minecraft:dirt"]}},
                               {"feature": f"{MOD}:humid_mud_disks", "placement": [
                                   {"type": "minecraft:count", "count": 2},
                                   {"type": "minecraft:in_square"},
                                   {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"},
                                   {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_fluids", "fluids": "minecraft:water"}},
                                   {"type": "minecraft:biome"}]})],
        # Mud, clay and rooted dirt, and puddles: the swamp's own patches, a few a chunk on the ground
        "extra": [("humid_mud_patches", {"feature": f"{MOD}:swamp_patch_vegetation", "placement": [
            {"type": "minecraft:count", "count": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 3}},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"},
            {"type": "minecraft:biome"}]})],
        "vegetation": [f"{MOD}:humid_mud_patches", "minecraft:glow_lichen", f"{MOD}:trees_humid_oak_forest", "minecraft:flower_swamp",
                       "minecraft:patch_grass_normal", "minecraft:patch_waterlily", "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal",
                       "minecraft:patch_sugar_cane_swamp", "minecraft:patch_pumpkin", "minecraft:patch_firefly_bush_near_water_swamp",
                       "minecraft:seagrass_swamp"],
    },
}


def merged_creatures(neighbours):
    """Both neighbours' animals: each kind once, with the larger of its weights."""
    creatures = {}
    for neighbour in neighbours:
        for entry in neighbour["attributes"][SPAWNS]["argument"]["spawns_by_category"].get("creature", []):
            kept = creatures.get(entry["type"])
            if kept is None or entry["weight"] > kept["weight"]:
                creatures[entry["type"]] = copy.deepcopy(entry)
    return list(creatures.values())


def make(vanilla, name, spec):
    main, other = (biome(vanilla, neighbour) for neighbour in spec["neighbours"])
    made = copy.deepcopy(main)
    made["temperature"] = spec["temperature"]
    made["downfall"] = spec["downfall"]
    made["has_precipitation"] = spec["has_precipitation"]
    effects = {"water_color": main["effects"].get("water_color", "#3f76e4")}
    if "grass_color_modifier" in spec:
        effects["grass_color_modifier"] = spec["grass_color_modifier"]
    effects.update(spec.get("colors", {}))
    made["effects"] = effects
    made["carvers"] = CARVERS
    made["features"][VEGETATION] = spec["vegetation"]
    for extra_name, feature, placed in spec.get("underground_extra", []):
        write_json(f"{MOD}/worldgen/feature/{extra_name}.json", feature)
        write_json(f"{MOD}/worldgen/placed_feature/{extra_name}.json", placed)
        made["features"][UNDERGROUND_ORES].append(f"{MOD}:{extra_name}")
    spawns = made["attributes"][SPAWNS]["argument"]["spawns_by_category"]
    spawns["creature"] = merged_creatures([main, other])
    # Milder than a savanna: snow golems don't melt
    made["attributes"].pop("minecraft:gameplay/snow_golem_melts", None)
    write_json(f"{MOD}/worldgen/biome/{name}.json", made)

    write_json(f"{MOD}/worldgen/feature/trees_{name}.json", spec["trees"])
    write_json(f"{MOD}/worldgen/placed_feature/trees_{name}.json", noise_trees(f"{MOD}:trees_{name}", **spec["tree_placement"]))
    for extra_name, placed in spec.get("extra", []):
        write_json(f"{MOD}/worldgen/placed_feature/{extra_name}.json", placed)


def add_to_tag(path, *values):
    full = os.path.join(DATA, path)
    tag = {"replace": False, "values": []}
    if os.path.exists(full):
        with open(full) as file:
            tag = json.load(file)
    for value in values:
        if value not in tag["values"]:
            tag["values"].append(value)
    write_json(path, tag)


def tags():
    every = [f"{MOD}:{name}" for name in BIOMES]
    add_to_tag("minecraft/tags/worldgen/biome/is_overworld.json", *every)
    add_to_tag("minecraft/tags/worldgen/biome/has_structure/mineshaft.json", *every)
    add_to_tag("minecraft/tags/worldgen/biome/is_forest.json", f"{MOD}:warm_oak_forest", f"{MOD}:dark_jungle_forest", f"{MOD}:humid_oak_forest")
    # A Tuscan countryside has its villages
    add_to_tag("minecraft/tags/worldgen/biome/has_structure/village_plains.json", f"{MOD}:tuscan_grove")


NAMES = {
    "en": {"biome.players-versus.tuscan_grove": "Tuscan Grove", "biome.players-versus.warm_oak_forest": "Warm Oak Forest",
           "biome.players-versus.dark_jungle_forest": "Dark Jungle Forest", "biome.players-versus.humid_oak_forest": "Humid Oak Forest"},
    "fr": {"biome.players-versus.tuscan_grove": "Bosquet toscan", "biome.players-versus.warm_oak_forest": "Chênaie chaude",
           "biome.players-versus.dark_jungle_forest": "Forêt de jungle sombre", "biome.players-versus.humid_oak_forest": "Chênaie humide"},
}


def main(vanilla):
    for name, spec in BIOMES.items():
        make(vanilla, name, spec)
    tags()
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    lemons = __import__("lemons")
    lemons.add_names(NAMES)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
