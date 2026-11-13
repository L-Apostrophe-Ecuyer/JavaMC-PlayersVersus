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
- The grassy wetlands (PvBiomeLayout: the drier part of the warm swamp): an open wet meadow, the swamp's underground, its
  marsh drier and greener, few oaks, tall grass, reeds and flowers all over.
- The mangrove marsh (the mangrove swamp's rim and drier part): mud flats and grass, small mangroves and jungle bushes.
- The mangrove swamp's own file (data/minecraft, so every world type): vanilla's, its foliage a clear green like the marsh's.
- The waterlogged leaves: clumps of persistent, waterlogged leaves in the top layer of the swamps' pools (oak in the swamp and
  the grassy wetlands, spruce and now and then birch in the cold swamp), after the biome's trees and placed by their noise.
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
import animals
lemons = __import__("lemons")
write_json = transition.write_json


def vanilla_placed(vanilla, name):
    with open(os.path.join(vanilla, "data", "minecraft", "worldgen", "placed_feature", name + ".json")) as file:
        return json.load(file)


def marsh():
    swamps = {"marsh": {"biomes": ["minecraft:swamp"], "edge": {"id": "minecraft:rooted_dirt"}},
              "marsh_cold": {"biomes": [f"{MOD}:cold_swamp"], "edge": {"id": "minecraft:podzol", "properties": {"snowy": "false"}}},
              "marsh_rim": {"biomes": [f"{MOD}:humid_oak_forest"], "edge": {"id": "minecraft:rooted_dirt"}, "dryness": 0.25, "reshape": False},
              # The wetlands: more land than the swamp's, still with its pools and channels; the mangrove marsh's edge is mud
              "marsh_wetlands": {"biomes": [f"{MOD}:grassy_wetlands"], "edge": {"id": "minecraft:rooted_dirt"}, "dryness": 0.2},
              "marsh_mangrove": {"biomes": [f"{MOD}:mangrove_marsh"], "edge": {"id": "minecraft:mud"}, "dryness": 0.1},
              # The pale swamp: the swamp's, its edge pale moss
              "marsh_pale": {"biomes": [f"{MOD}:pale_swamp"], "edge": {"id": "minecraft:pale_moss_block"}}}
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
LEAVES = f"{MOD}:waterlogged_leaves"
LEAVES_COLD = f"{MOD}:waterlogged_leaves_cold"
LEAVES_WETLANDS = f"{MOD}:waterlogged_leaves_wetlands"
# A chunk's patches of leaves: this times the biome's trees' noise (so as many as before in the swamps, none where the trees have none)
LEAVES_RATIO = 20


def waterlogged_leaves():
    """
    Clumps of leaves in the top layer of the pools, among the trees: a chunk's patches by the noise its biome's trees are placed by
    (the same factor and offset: where they have none, the pools have none, so an open wetland has no clumps far from a tree),
    each twelve tries within four blocks, a try standing where the position is water with air above and water under it (two deep
    at least: over one deep water a clump would sit on the bottom), one block under the surface the height map finds, and in the
    biome still where it lands. Persistent, so they don't decay away from logs, and waterlogged, so the pool stays whole around
    them. Run after the biomes' trees are written.
    """
    spread = {"type": "minecraft:trapezoid", "min": -4, "max": 4, "plateau": 0}

    def patch(trees):
        noise = placed_file(trees)["placement"][0]
        assert noise["type"] == "minecraft:noise_based_count", trees + " isn't placed by noise"
        return [
            {"type": "minecraft:noise_based_count", "noise_to_count_ratio": LEAVES_RATIO, "noise_factor": noise["noise_factor"],
             "noise_offset": noise["noise_offset"]},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
            {"type": "minecraft:biome"},
            {"type": "minecraft:count", "count": 12},
            {"type": "minecraft:offset", "x": spread, "y": {"type": "minecraft:constant", "value": -1}, "z": spread},
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                {"type": "minecraft:matching_blocks", "blocks": "minecraft:water"},
                {"type": "minecraft:matching_block_tag", "tag": "minecraft:air", "offset": [0, 1, 0]},
                {"type": "minecraft:matching_blocks", "blocks": "minecraft:water", "offset": [0, -1, 0]}]}},
            {"type": "minecraft:biome"}]
    for name, wood in [("waterlogged_leaves", "oak"), ("waterlogged_spruce_leaves", "spruce"), ("waterlogged_birch_leaves", "birch")]:
        write_json(f"{MOD}/worldgen/feature/{name}.json", {"type": "minecraft:simple_block", "to_place": {
            "id": f"minecraft:{wood}_leaves", "properties": {"distance": "7", "persistent": "true", "waterlogged": "true"}}})
    write_json(f"{MOD}/worldgen/placed_feature/waterlogged_leaves.json", {"feature": LEAVES, "placement": patch("trees_swamp")})
    write_json(f"{MOD}/worldgen/placed_feature/waterlogged_leaves_wetlands.json", {"feature": LEAVES, "placement": patch("trees_grassy_wetlands")})
    # The cold swamp's: each leaf block spruce, a quarter of them birch
    for name in ("waterlogged_spruce_leaves", "waterlogged_birch_leaves"):
        write_json(f"{MOD}/worldgen/placed_feature/{name}.json", {"feature": f"{MOD}:{name}", "placement": []})
    write_json(f"{MOD}/worldgen/feature/waterlogged_leaves_cold.json",
               transition.selector(f"{MOD}:waterlogged_spruce_leaves", (f"{MOD}:waterlogged_birch_leaves", 0.25)))
    write_json(f"{MOD}/worldgen/placed_feature/waterlogged_leaves_cold.json", {"feature": LEAVES_COLD, "placement": patch("trees_cold_swamp")})


def placed_file(name):
    """One of this mod's placed features, as written."""
    with open(os.path.join(DATA, MOD, "worldgen", "placed_feature", name + ".json")) as file:
        return json.load(file)


def swamp():
    path = os.path.join(DATA, "minecraft", "worldgen", "biome", "swamp.json")
    with open(path) as file:
        biome = json.load(file)
    plants = [f for f in biome["features"][VEGETATION] if f not in [f"{MOD}:swamp_patch_vegetation", f"{MOD}:marsh", LEAVES] + MARSH_CLEARINGS]
    trees = plants.index(f"{MOD}:trees_swamp")
    biome["features"][VEGETATION] = [f"{MOD}:marsh"] + plants[:trees + 1] + [LEAVES] + MARSH_CLEARINGS + plants[trees + 1:]
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
        f"{MOD}:marsh_cold", "minecraft:glow_lichen", "minecraft:patch_large_fern", f"{MOD}:trees_cold_swamp", LEAVES_COLD, *MARSH_CLEARINGS,
        "minecraft:patch_grass_taiga_2", "minecraft:patch_waterlily", "minecraft:brown_mushroom_taiga", "minecraft:red_mushroom_taiga",
        "minecraft:patch_sugar_cane_swamp", "minecraft:patch_pumpkin", "minecraft:patch_firefly_bush_near_water_swamp",
        "minecraft:seagrass_swamp", "minecraft:patch_berry_common"]
    animals.set_spawns(made, "cold_swamp", lambda neighbour: transition.biome(vanilla, neighbour))
    write_json(f"{MOD}/worldgen/biome/cold_swamp.json", made)

    write_json(f"{MOD}/worldgen/feature/trees_cold_swamp.json", transition.selector(
        "minecraft:spruce_checked", ("minecraft:birch_checked", 0.25), ("minecraft:fallen_spruce_tree", 0.05), ("minecraft:fallen_birch_tree", 0.03)))
    write_json(f"{MOD}/worldgen/placed_feature/trees_cold_swamp.json",
               transition.noise_trees(f"{MOD}:trees_cold_swamp", ratio=9, offset=0.05, max_water=2))


def grassy_wetlands(vanilla):
    """An open wet meadow beside the swamp: its underground and animals, the plains' too; the marsh drier, few oaks."""
    main, plains = transition.biome(vanilla, "minecraft:swamp"), transition.biome(vanilla, "minecraft:plains")
    made = copy.deepcopy(main)
    made["temperature"] = 0.75
    made["downfall"] = 0.85
    made["has_precipitation"] = True
    # Lusher than the swamp's: clear water, bright grass and leaves
    made["effects"] = {"water_color": "#3e9ab0", "grass_color": "#7db356", "foliage_color": "#62ad4a", "dry_foliage_color": "#7b5334"}
    made["attributes"]["minecraft:visual/water_fog_color"] = "#274b48"
    made["features"][VEGETATION] = [
        f"{MOD}:marsh_wetlands", "minecraft:glow_lichen", f"{MOD}:trees_grassy_wetlands", LEAVES_WETLANDS, "minecraft:patch_tall_grass", f"{MOD}:marsh_reeds",
        f"{MOD}:marsh_flowers", "minecraft:flower_swamp", "minecraft:patch_grass_normal", "minecraft:patch_waterlily",
        "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal", "minecraft:patch_sugar_cane_swamp", "minecraft:patch_pumpkin",
        "minecraft:patch_firefly_bush_near_water_swamp", "minecraft:seagrass_swamp"]
    animals.set_spawns(made, "grassy_wetlands", lambda neighbour: transition.biome(vanilla, neighbour))
    write_json(f"{MOD}/worldgen/biome/grassy_wetlands.json", made)
    # A few oaks, in small groves: the noise's high ground
    write_json(f"{MOD}/worldgen/feature/trees_grassy_wetlands.json", transition.selector(
        "minecraft:oak_checked", (transition.SWAMP_OAK, 0.3), ("minecraft:fancy_oak_checked", 0.05)))
    write_json(f"{MOD}/worldgen/placed_feature/trees_grassy_wetlands.json",
               transition.noise_trees(f"{MOD}:trees_grassy_wetlands", ratio=4, offset=-0.2, max_water=2))


def mangrove_marsh(vanilla):
    """The mangrove swamp's rim and drier part: mud flats and grass, small mangroves, jungle bushes."""
    main, jungle = transition.biome(vanilla, "minecraft:mangrove_swamp"), transition.biome(vanilla, "minecraft:sparse_jungle")
    made = copy.deepcopy(main)
    made["temperature"] = 0.85
    made["downfall"] = 0.85
    made["has_precipitation"] = True
    # The mangrove swamp's water, clear greens in place of its murky grass
    made["effects"] = {"water_color": "#3a8a78", "grass_color": "#78a84c", "foliage_color": MANGROVE_FOLIAGE, "dry_foliage_color": "#7b5334"}
    made["features"][VEGETATION] = [
        f"{MOD}:marsh_mangrove", "minecraft:glow_lichen", f"{MOD}:trees_mangrove_marsh", "minecraft:patch_grass_normal", "minecraft:patch_dead_bush",
        "minecraft:patch_waterlily", "minecraft:seagrass_swamp", "minecraft:patch_firefly_bush_near_water"]
    animals.set_spawns(made, "mangrove_marsh", lambda neighbour: transition.biome(vanilla, neighbour))
    write_json(f"{MOD}/worldgen/biome/mangrove_marsh.json", made)
    # Mangroves may stand in the water, the jungle bushes on dry land only
    bush = {"feature": "minecraft:jungle_bush", "placement": [
        {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": "minecraft:oak_sapling"}}]}
    write_json(f"{MOD}/worldgen/feature/trees_mangrove_marsh.json", transition.selector(
        bush, ("minecraft:mangrove_checked", 0.45), ("minecraft:tall_mangrove_checked", 0.1)))
    write_json(f"{MOD}/worldgen/placed_feature/trees_mangrove_marsh.json",
               transition.noise_trees(f"{MOD}:trees_mangrove_marsh", ratio=8, offset=0.1, max_water=3))


# The mangrove swamp's leaves, and the marsh beside it: a clear green in place of vanilla's yellow-brown #8db127
MANGROVE_FOLIAGE = "#62a83e"


def mangrove_swamp(vanilla):
    """Vanilla's mangrove swamp, its foliage colour the marsh's (the only change; it applies to every world type)."""
    biome = transition.biome(vanilla, "minecraft:mangrove_swamp")
    biome["effects"]["foliage_color"] = MANGROVE_FOLIAGE
    write_json("minecraft/worldgen/biome/mangrove_swamp.json", biome)


def tags(vanilla):
    cold = f"{MOD}:cold_swamp"
    for tag in ["is_overworld", "has_structure/mineshaft", "has_structure/swamp_hut", "spawns_cold_variant_frogs",
                "allows_surface_slime_spawns", "has_closer_water_fog"]:
        transition.add_to_tag(f"minecraft/tags/worldgen/biome/{tag}.json", cold)
    # Each new wetland is what it comes from, in the tags of structures, mobs and the map
    for child, parent in [("grassy_wetlands", "minecraft:swamp"), ("mangrove_marsh", "minecraft:mangrove_swamp")]:
        for tag in ["is_overworld", "has_structure/mineshaft"]:
            transition.add_to_tag(f"minecraft/tags/worldgen/biome/{tag}.json", f"{MOD}:{child}")
        transition.inherit_tags(vanilla, parent, f"{MOD}:{child}")


NAMES = {"en": {"biome.players-versus.cold_swamp": "Cold Swamp", "biome.players-versus.grassy_wetlands": "Grassy Wetlands",
                "biome.players-versus.mangrove_marsh": "Mangrove Marsh"},
         "fr": {"biome.players-versus.cold_swamp": "Marais froid", "biome.players-versus.grassy_wetlands": "Zones humides herbeuses",
                "biome.players-versus.mangrove_marsh": "Marécage de mangrove"}}


def main(vanilla):
    marsh()
    clearings(vanilla)
    swamp()
    cold_swamp(vanilla)
    grassy_wetlands(vanilla)
    mangrove_marsh(vanilla)
    mangrove_swamp(vanilla)
    waterlogged_leaves()
    tags(vanilla)
    lemons.add_names(NAMES)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
