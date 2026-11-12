#!/usr/bin/env python3
"""Writes the transition biomes and their features: run from the repository root with vanilla's extracted data.

    python3 tools/transition-biomes.py <vanilla>     (<vanilla> holds data/minecraft, from the game's jar)

Each transition biome lies between two biomes (PvBiomeLayout's warm band and swamp rim) and is made from both:

- what lies under it (lakes, ores, springs, the carvers, the frost) is its main neighbour's, the mod's version where the
  mod overrides it, so every biome keeps the same order of features (BiomeFeatureOrderTest);
- the humid oak forest's ground is the marsh's, drier (tools/wetlands.py);
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
LOCAL_MODIFICATIONS = 2
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
        # The oaks' leaves olive-grey (the cypresses' spruce leaves keep their own dark green)
        "colors": {"foliage_color": "#7a874c"},
        # Cypresses among oaks and fancy oaks
        "trees": selector("minecraft:oak_checked", (f"{MOD}:cypress_checked", 0.4), ("minecraft:fancy_oak_checked", 0.2)),
        "tree_placement": dict(ratio=8, offset=0.1),
        # Lemon bushes in orchards of their own, on a coarser noise than the oaks', so the two don't share their patches
        "extra": [("lemon_orchards", noise_trees(f"{MOD}:lemon_bush", 10, -0.1, factor=14,
                                                     survives={"id": f"{MOD}:lemon_sapling", "properties": {"stage": "0"}}))],
        # Fields of flowers and of dry grass (tuscan_fields), the plains' flowers and the savanna's grass between them
        "vegetation": ["minecraft:glow_lichen", f"{MOD}:lemon_orchards", f"{MOD}:trees_tuscan_grove", f"{MOD}:tuscan_flower_field",
                       f"{MOD}:tuscan_dry_grass", "minecraft:flower_plains", "minecraft:patch_grass_savanna", "minecraft:brown_mushroom_normal",
                       "minecraft:red_mushroom_normal", "minecraft:patch_pumpkin", "minecraft:patch_sugar_cane", "minecraft:patch_firefly_bush_near_water"],
    },
    "warm_oak_forest": {
        "neighbours": ["minecraft:sparse_jungle", "minecraft:forest"],
        # Between the forest's (0.7, 0.8) and the jungle's (0.95, 0.9)
        "temperature": 0.825, "downfall": 0.85, "has_precipitation": True,
        "trees": selector("minecraft:oak_checked", ("minecraft:jungle_bush", 0.22), ("minecraft:jungle_tree", 0.18),
                          ("minecraft:fancy_oak_checked", 0.12), ("minecraft:birch_checked", 0.06)),
        "tree_placement": dict(ratio=22, offset=0.55),
        "vegetation": ["minecraft:glow_lichen", f"{MOD}:trees_warm_oak_forest", "minecraft:patch_large_fern", "minecraft:patch_bush",
                       "minecraft:flower_warm", "minecraft:patch_grass_jungle",
                       "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal", "minecraft:patch_pumpkin",
                       "minecraft:patch_sugar_cane", "minecraft:patch_firefly_bush_near_water", "minecraft:vines", "minecraft:patch_melon_sparse"],
    },
    "dark_jungle_forest": {
        "neighbours": ["minecraft:dark_forest", "minecraft:jungle"],
        # Between the dark forest's (0.7, 0.8) and the jungle's (0.95, 0.9), darkened as the dark forest's grass is
        "temperature": 0.825, "downfall": 0.85, "has_precipitation": True, "grass_color_modifier": "dark_forest",
        "trees": selector("minecraft:dark_oak_checked", (f"{MOD}:dark_oak_with_vines_checked", 0.35), ("minecraft:jungle_tree", 0.18),
                          ("minecraft:jungle_bush", 0.15), ("minecraft:mega_jungle_tree_checked", 0.05)),
        "tree_placement": dict(ratio=34, offset=0.7),
        "vegetation": ["minecraft:glow_lichen", f"{MOD}:trees_dark_jungle_forest", "minecraft:patch_large_fern", "minecraft:patch_bush",
                       "minecraft:flower_default", "minecraft:patch_grass_jungle",
                       "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal", "minecraft:patch_leaf_litter",
                       "minecraft:patch_pumpkin", "minecraft:patch_sugar_cane", "minecraft:patch_firefly_bush_near_water",
                       "minecraft:vines", "minecraft:patch_melon_sparse"],
    },
    "warm_dark_forest": {
        "neighbours": ["minecraft:dark_forest", "minecraft:forest"],
        # The dark forest's (0.7, 0.8) warmed toward the dark jungle forest's (0.825, 0.85): the same dark canopy in warmer greens
        "temperature": 0.8, "downfall": 0.8, "has_precipitation": True,
        "colors": {"grass_color": "#6a9a52", "foliage_color": "#7a9c4c", "dry_foliage_color": "#7b5334"},
        # Dark oaks, with vines and with oaks, fancy oaks and jungle bushes among them. A third of the dark forest's giant mushrooms, tried
        # first as its own are (an entry tried last takes only what the entries before it leave): brown 0.008, red 0.017 a try, 2.5%
        # together against its 7.4%
        "trees": selector("minecraft:dark_oak_checked", ({"feature": "minecraft:huge_brown_mushroom", "placement": []}, 0.008),
                          ({"feature": "minecraft:huge_red_mushroom", "placement": []}, 0.017), (f"{MOD}:dark_oak_with_vines_checked", 0.25),
                          ("minecraft:fancy_oak_checked", 0.14), ("minecraft:oak_checked", 0.12), ("minecraft:jungle_bush", 0.1),
                          ("minecraft:birch_checked", 0.04)),
        # As dense as the dark forest's (its flat 16 a chunk): the census over the same ground
        "tree_placement": dict(ratio=22, offset=0.55),
        "vegetation": ["minecraft:glow_lichen", f"{MOD}:trees_warm_dark_forest", "minecraft:forest_flowers", "minecraft:patch_bush", "minecraft:flower_default",
                       "minecraft:patch_grass_forest", "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal", "minecraft:patch_leaf_litter",
                       "minecraft:patch_pumpkin", "minecraft:patch_sugar_cane_dark", "minecraft:patch_firefly_bush_near_water", "minecraft:vines"],
    },
    "humid_oak_forest": {
        "neighbours": ["minecraft:swamp", "minecraft:forest"],
        # Between the forest's and the swamp's: its colours between theirs (the swamp's grass and leaves are its own)
        "temperature": 0.75, "downfall": 0.9, "has_precipitation": True,
        "colors": {"grass_color": "#5f8c42", "foliage_color": "#5e8a3a", "dry_foliage_color": "#7b5334", "water_color": "#507a9e"},
        "trees": selector("minecraft:oak_checked", (SWAMP_OAK, 0.35), ("minecraft:fancy_oak_checked", 0.08), ("minecraft:fallen_oak_tree", 0.06)),
        "tree_placement": dict(ratio=20, offset=0.55, max_water=2),
        # Its ground is the marsh's, drier: gray and brown mud by the water, rooted dirt, grass (tools/wetlands.py)
        "vegetation": [f"{MOD}:marsh_rim", "minecraft:glow_lichen", f"{MOD}:trees_humid_oak_forest", "minecraft:patch_large_fern", "minecraft:patch_bush",
                       "minecraft:patch_tall_grass", "minecraft:flower_swamp",
                       "minecraft:patch_grass_normal", "minecraft:patch_waterlily", "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal",
                       "minecraft:patch_sugar_cane_swamp", "minecraft:patch_pumpkin", "minecraft:patch_firefly_bush_near_water_swamp",
                       "minecraft:seagrass_swamp"],
    },
}


# The rocky biomes: each between two others, its rocks placed in the step before trees (tools/rocks.py writes them)
BIOMES.update({
    "rocky_plains": {
        "neighbours": ["minecraft:savanna", "minecraft:plains"],
        # Drier than the Tuscan grove (0.95, 0.25): the savanna's gold through the plains' green
        "temperature": 1.2, "downfall": 0.2, "has_precipitation": True,
        "trees": selector("minecraft:oak_checked", ("minecraft:acacia_checked", 0.35), ("minecraft:fancy_oak_checked", 0.1)),
        "tree_placement": dict(ratio=2, offset=-0.2),
        "rocks": [f"{MOD}:rock/rocky_plains"],
        "vegetation": ["minecraft:glow_lichen", "minecraft:patch_tall_grass", f"{MOD}:trees_rocky_plains", "minecraft:flower_plains",
                       "minecraft:patch_grass_savanna", "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal",
                       "minecraft:patch_pumpkin", "minecraft:patch_sugar_cane", "minecraft:patch_firefly_bush_near_water"],
    },
    "rocky_jungle": {
        "neighbours": ["minecraft:sparse_jungle", "minecraft:jungle"],
        # The sparse jungle's (0.95, 0.8) and the jungle's (0.95, 0.9)
        "temperature": 0.95, "downfall": 0.85, "has_precipitation": True,
        "trees": selector("minecraft:jungle_bush", ("minecraft:jungle_tree", 0.35), ("minecraft:oak_checked", 0.1), ("minecraft:mega_jungle_tree_checked", 0.05)),
        "tree_placement": dict(ratio=8, offset=0.3),
        "rocks": [f"{MOD}:rock/rocky_jungle"],
        "vegetation": ["minecraft:glow_lichen", f"{MOD}:trees_rocky_jungle", "minecraft:flower_warm", "minecraft:patch_grass_jungle",
                       "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal", "minecraft:patch_pumpkin", "minecraft:patch_sugar_cane",
                       "minecraft:patch_firefly_bush_near_water", "minecraft:vines", "minecraft:patch_melon_sparse"],
    },
    "rocky_birch_forest": {
        "neighbours": ["minecraft:old_growth_birch_forest", "minecraft:taiga"],
        # Between the old growth birch forest's (0.6, 0.6) and the taiga's (0.25, 0.8)
        "temperature": 0.45, "downfall": 0.7, "has_precipitation": True,
        "trees": selector("minecraft:super_birch_bees_0002", ("minecraft:spruce_checked", 0.35), ("minecraft:birch_checked", 0.15), ("minecraft:pine_checked", 0.08)),
        "tree_placement": dict(ratio=10, offset=0.4),
        "rocks": [f"{MOD}:rock/rocky_birch_forest"],
        "vegetation": ["minecraft:glow_lichen", "minecraft:patch_large_fern", "minecraft:wildflowers_birch_forest", f"{MOD}:trees_rocky_birch_forest",
                       "minecraft:flower_default", "minecraft:patch_grass_taiga_2", "minecraft:brown_mushroom_taiga", "minecraft:red_mushroom_taiga",
                       "minecraft:patch_pumpkin", "minecraft:patch_sugar_cane", "minecraft:patch_firefly_bush_near_water", "minecraft:patch_berry_common"],
    },
})


# A cypress: a tall, narrow column of spruce leaves (a plus, three blocks across, tapering at its top) on a straight trunk of
# twelve or thirteen blocks, bare for one or two blocks at its foot. The spruce's foliage placer, not the pine's: the pine's adds
# a random radius (the trunk's bare length), which makes most of its trees cones as wide as spruces.
CYPRESS = {
    "type": "minecraft:tree",
    "below_trunk_provider": "minecraft:soil_beneath_tree",
    "decorators": [],
    "foliage_placer": {"type": "minecraft:spruce_foliage_placer", "offset": 1, "radius": 1,
                       "trunk_height": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 2}},
    "foliage_provider": {"id": "minecraft:spruce_leaves", "properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}},
    "ignore_vines": True,
    "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "upper_size": 1},
    "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": 12, "height_rand_a": 1, "height_rand_b": 0},
    "trunk_provider": {"id": "minecraft:spruce_log", "properties": {"axis": "y"}},
}


def vanilla_placed(vanilla, name):
    with open(os.path.join(vanilla, "data", "minecraft", "worldgen", "placed_feature", name + ".json")) as file:
        return json.load(file)


def cypress():
    write_json(f"{MOD}/worldgen/feature/cypress.json", CYPRESS)
    write_json(f"{MOD}/worldgen/placed_feature/cypress_checked.json", {"feature": f"{MOD}:cypress", "placement": [
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": "minecraft:spruce_sapling"}}]})


def tuscan_fields(vanilla):
    """
    Fields of flowers where the biome noise (a 200-block scale, the plains' flowers use it too) is high, of dry grass where
    it is low, a patch each chunk: the meadow's flowers (and tall grass) and the desert's dry grass, in fields of their own.
    """
    flowers = vanilla_placed(vanilla, "flower_meadow")
    flowers["placement"].insert(0, {"type": "minecraft:noise_threshold_count", "noise_level": 0.1, "below_noise": 0, "above_noise": 1})
    write_json(f"{MOD}/worldgen/placed_feature/tuscan_flower_field.json", flowers)
    dry = vanilla_placed(vanilla, "patch_dry_grass_desert")
    dry["placement"] = [step for step in dry["placement"] if step["type"] != "minecraft:rarity_filter"]
    dry["placement"].insert(0, {"type": "minecraft:noise_threshold_count", "noise_level": -0.05, "below_noise": 1, "above_noise": 0})
    write_json(f"{MOD}/worldgen/placed_feature/tuscan_dry_grass.json", dry)


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
    # Rocks after what the main neighbour has in their step (an amethyst geode), before the trees
    made["features"][LOCAL_MODIFICATIONS] = main["features"][LOCAL_MODIFICATIONS] + spec.get("rocks", [])
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


def inherit_tags(vanilla, parent, child):
    """Adds `child` to every biome tag (the game's and the mod's, by file) that holds `parent`: the structures, mobs and
    the rest of what makes the parent what it is."""
    found = []
    for root in [os.path.join(vanilla, "data"), DATA]:
        base = os.path.join(root, "minecraft", "tags", "worldgen", "biome")
        for folder, _, files in os.walk(base):
            for file in files:
                path = os.path.join(folder, file)
                with open(path) as handle:
                    if parent in json.load(handle).get("values", []):
                        found.append(os.path.relpath(path, os.path.join(root, "minecraft", "tags", "worldgen", "biome")))
    for relative in sorted(set(found)):
        add_to_tag(f"minecraft/tags/worldgen/biome/{relative}", child)


def tags(vanilla=None):
    every = [f"{MOD}:{name}" for name in BIOMES]
    add_to_tag("minecraft/tags/worldgen/biome/is_overworld.json", *every)
    add_to_tag("minecraft/tags/worldgen/biome/has_structure/mineshaft.json", *every)
    add_to_tag("minecraft/tags/worldgen/biome/is_forest.json", f"{MOD}:warm_oak_forest", f"{MOD}:dark_jungle_forest", f"{MOD}:humid_oak_forest",
               f"{MOD}:rocky_birch_forest", f"{MOD}:warm_dark_forest")
    # A warm dark forest is a dark forest still: its woodland mansions, trial chambers and strongholds
    if vanilla:
        inherit_tags(vanilla, "minecraft:dark_forest", f"{MOD}:warm_dark_forest")
    add_to_tag("minecraft/tags/worldgen/biome/is_jungle.json", f"{MOD}:rocky_jungle")
    # The large rock formations stand in the rocky biomes only
    add_to_tag(f"{MOD}/tags/worldgen/biome/has_structure/rock_formation.json", f"{MOD}:rocky_plains", f"{MOD}:rocky_jungle", f"{MOD}:rocky_birch_forest")
    # A Tuscan countryside has its villages
    add_to_tag("minecraft/tags/worldgen/biome/has_structure/village_plains.json", f"{MOD}:tuscan_grove")


NAMES = {
    "en": {"biome.players-versus.tuscan_grove": "Tuscan Grove", "biome.players-versus.warm_oak_forest": "Warm Oak Forest",
           "biome.players-versus.warm_dark_forest": "Warm Dark Forest",
           "biome.players-versus.dark_jungle_forest": "Dark Jungle Forest", "biome.players-versus.humid_oak_forest": "Humid Oak Forest",
           "biome.players-versus.rocky_plains": "Rocky Plains", "biome.players-versus.rocky_jungle": "Rocky Jungle",
           "biome.players-versus.rocky_birch_forest": "Rocky Birch Forest"},
    "fr": {"biome.players-versus.tuscan_grove": "Bosquet toscan", "biome.players-versus.warm_oak_forest": "Chênaie chaude",
           "biome.players-versus.warm_dark_forest": "Forêt sombre chaude",
           "biome.players-versus.dark_jungle_forest": "Forêt de jungle sombre", "biome.players-versus.humid_oak_forest": "Chênaie humide",
           "biome.players-versus.rocky_plains": "Plaines rocheuses", "biome.players-versus.rocky_jungle": "Jungle rocheuse",
           "biome.players-versus.rocky_birch_forest": "Forêt de bouleaux rocheuse"},
}


def main(vanilla):
    cypress()
    tuscan_fields(vanilla)
    for name, spec in BIOMES.items():
        make(vanilla, name, spec)
    tags(vanilla)
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    lemons = __import__("lemons")
    lemons.add_names(NAMES)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
