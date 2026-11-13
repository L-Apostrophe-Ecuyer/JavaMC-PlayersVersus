"""The animals of the mod's biomes, chosen for each place (imported by the biome generators: tools/transition-biomes.py,
tools/wetlands.py, tools/refine-biomes.py; not run on its own).

- CREATURES: each mod biome's animals (the creature category of its natural spawns) and their weights. Farm animals where
  there is grass and room, frogs in every wetland, parrots and pandas toward the jungles, goats on mountainsides and rocky
  ground, horses and donkeys on open grass, wolves and foxes in the woods and taigas; none of the cold or open land's
  animals in warm, wet places, no farm animals in the pale biomes.
- MONSTERS: the biomes whose monsters are a vanilla biome's for their climate (the jungle's ocelots toward the jungles, the
  desert's husks and parched in the oasis); the others keep their own (a wetland's are the swamp's, the cold plains' have
  strays).
- Variants follow the climate: WARM and COLD go to the game's tags for warm and cold farm animals and frogs (COLD_FROGS
  where frogs are cold), and WOLF_COATS to the game's wolf variants, as a second spawn condition beside vanilla's.
"""

# How many come together, as vanilla has them: a number, or the least and the most
COUNTS = {"sheep": 4, "pig": 4, "chicken": 4, "cow": 4, "wolf": 4, "llama": 4, "camel": 1, "donkey": 1, "horse": (2, 6),
          "rabbit": (2, 3), "fox": (2, 4), "frog": (2, 5), "goat": (1, 3), "parrot": (1, 2), "panda": (1, 2), "polar_bear": (1, 2),
          "armadillo": (2, 3)}

FARM = [("sheep", 12), ("pig", 10), ("chicken", 10), ("cow", 8)]
TAIGA = FARM + [("wolf", 8), ("rabbit", 4), ("fox", 8)]

CREATURES = {
    # Plains and forests, and between them
    "forest_edge": FARM + [("horse", 5), ("donkey", 1), ("wolf", 4)],
    "mixed_forest": TAIGA,
    "old_oak_forest": FARM + [("wolf", 5)],
    "prairie": [("horse", 10), ("donkey", 3), ("rabbit", 6), ("sheep", 12), ("cow", 8), ("pig", 6), ("chicken", 6)],
    "warm_dark_forest": FARM,
    "dark_birch_forest": FARM,
    "dark_taiga_forest": TAIGA,
    # Toward the jungles: parrots (and the jungle's ocelots, MONSTERS)
    "warm_oak_forest": FARM + [("parrot", 10)],
    "dark_jungle_forest": FARM + [("parrot", 40), ("panda", 1)],
    "rocky_jungle": FARM + [("parrot", 40), ("panda", 1), ("wolf", 4)],
    "mountainside_jungle": [("parrot", 20), ("sheep", 6), ("goat", 4), ("wolf", 2)],
    # Savannas' edges: armadillos, horses and donkeys; goats on the rocky plains
    "tuscan_grove": FARM + [("horse", 5), ("donkey", 3), ("armadillo", 4)],
    "rocky_plains": FARM + [("horse", 5), ("donkey", 1), ("armadillo", 10), ("goat", 5)],
    "desert_oasis": [("camel", 8), ("rabbit", 6)],
    # The wetlands: frogs everywhere; on the grassy ones farm animals and horses, in the cold swamp the taiga's animals
    "grassy_wetlands": FARM + [("frog", 10), ("horse", 5), ("donkey", 1)],
    "humid_oak_forest": FARM + [("frog", 10), ("wolf", 4)],
    "cold_swamp": [("frog", 10), ("wolf", 8), ("fox", 8), ("rabbit", 4), ("sheep", 6)],
    "mangrove_marsh": [("frog", 10), ("parrot", 8), ("chicken", 5)],
    "pale_swamp": [("frog", 10)],
    # Taigas and the woods between birch forests and taigas (no llamas: they came from the windswept hills)
    "birch_taiga_forest": [("sheep", 12), ("pig", 6), ("chicken", 6), ("cow", 6), ("wolf", 8), ("rabbit", 4), ("fox", 8), ("goat", 2)],
    "dappled_taiga": TAIGA,
    "sparse_dappled_forest": TAIGA,
    "rocky_birch_forest": TAIGA,
    "cold_taiga": TAIGA,
    "pale_taiga": [("wolf", 6), ("fox", 6), ("rabbit", 4)],
    # The cold, open land
    "cold_plains": [("rabbit", 12), ("sheep", 8), ("fox", 4), ("wolf", 2), ("polar_bear", 1)],
    "rocky_snowy_plains": [("rabbit", 10), ("fox", 6), ("goat", 4), ("wolf", 3), ("polar_bear", 1)],
    "cold_beach": [],
    # Mountainsides: goats, with the meadow's few
    "mountainside_forest": [("goat", 5), ("sheep", 4), ("rabbit", 2), ("donkey", 1)],
    "mountainside_forest_warm": [("goat", 5), ("llama", 4), ("sheep", 4), ("donkey", 1)],
}

MONSTERS = {
    "warm_oak_forest": "minecraft:jungle", "dark_jungle_forest": "minecraft:jungle", "rocky_jungle": "minecraft:jungle",
    "mountainside_jungle": "minecraft:jungle", "desert_oasis": "minecraft:desert",
}

WARM = ["desert_oasis", "tuscan_grove", "rocky_plains", "warm_oak_forest", "dark_jungle_forest", "mangrove_marsh", "mountainside_jungle",
        "mountainside_forest_warm"]
COLD = ["cold_plains", "cold_taiga", "cold_beach", "rocky_snowy_plains", "birch_taiga_forest", "dappled_taiga", "sparse_dappled_forest",
        "dark_taiga_forest", "pale_taiga", "mixed_forest", "rocky_birch_forest", "cold_swamp"]
COLD_FROGS = ["cold_swamp", "cold_plains", "cold_taiga", "cold_beach", "rocky_snowy_plains"]
# Wolves' coats: woods in the forests, rusty toward the jungles (the rocky jungle is a jungle already), ashen in the cold,
# chestnut by the old taigas; the others' wolves are pale (the game's default)
WOLF_COATS = {"woods": ["forest_edge", "old_oak_forest", "humid_oak_forest", "mixed_forest"], "rusty": ["mountainside_jungle"],
              "ashen": ["cold_taiga", "cold_plains", "rocky_snowy_plains"], "chestnut": ["pale_taiga", "dark_taiga_forest"]}


def spawn(kind, weight):
    count = COUNTS[kind]
    if isinstance(count, tuple):
        count = {"type": "minecraft:uniform", "max_inclusive": count[1], "min_inclusive": count[0]}
    return {"type": f"minecraft:{kind}", "count": count, "weight": weight}


def creatures(name):
    """The biome's animals, as its natural spawns list them."""
    return [spawn(kind, weight) for kind, weight in CREATURES[name]]


def set_spawns(biome, name, vanilla_biome=None):
    """Gives the biome (its JSON) its animals, and the monsters of MONSTERS' vanilla biome if it has one there
    (`vanilla_biome` reads a vanilla biome's JSON by its id)."""
    spawns = biome["attributes"]["minecraft:gameplay/natural_mob_spawns"]["argument"]["spawns_by_category"]
    spawns["creature"] = creatures(name)
    if name in MONSTERS:
        source = vanilla_biome(MONSTERS[name])
        spawns["monster"] = source["attributes"]["minecraft:gameplay/natural_mob_spawns"]["argument"]["spawns_by_category"]["monster"]
