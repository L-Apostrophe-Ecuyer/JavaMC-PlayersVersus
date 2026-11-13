# Refinement pass: transitions, sub-biomes, features, mobs (part 5)

Asked: "Improvement and refinement pass on everything. Better transitions, sub biomes, features, mob selections, etc."
After the recon below: "All good points. Implement everything missing". It follows the four parts of
`2026-11-09-rocks-transitions-wetlands-design.md`.

## What the recon found

Measured over a real world's climate (seed 1234, 22.4 km a side, every 16 blocks; vanilla's and this mod's layout):

- Forest (12.5% of all columns) and plains (9.6%) are a third of the land, with no sub-biomes. Forest | plains is the
  most common land border (3.4% of all border length), forest | taiga 1.0%; river banks are about a third of it.
- The plain dark forest is 0.28% (vanilla's 1.96%): the dark birch forest (0.76%), the warm dark forest (0.79%) and
  the dark taiga forest (0.31%) took the rest.
- The warm swamp is small (0.35% of the land at erosion 0.55 and up, against the cold swamp's 0.80%), so the grassy
  wetlands (0.05%) and the humid oak forest (0.03%) are rare: the nearest of each is 3.5 km from the origin.
- Jungle meets plain forest (0.48% of borders) as often as it meets the warm oak forest: vanilla puts plain forest
  (and plains, where the weirdness is positive) between savanna and jungle at temperatures 0.2 to 0.55.
- The transition biomes' animals are their two neighbours' lists merged (cows and wolves in the mangrove marsh,
  llamas in the birch taiga), and the older biomes' lists were never chosen (the desert oasis has pigs and no husks).
- `pale_swamp` and `pale_taiga` are biome files that nothing places, with no tags.

## Decisions

### Transitions (`PvBiomeLayout` rules)

| Rule | Where | Becomes |
|---|---|---|
| forest-edge | plains and forest within 0.05 of their humidity border (-0.1), temperature under 0.2 | **Forest Edge** (new) |
| mixed-forest | forest and taiga within 0.05 of their humidity border (0.1), temperature -0.375 to -0.15 | **Mixed Forest** (new) |
| taiga-birch | taiga and birch forest within 0.05 of their temperature border (-0.15) | birch taiga forest where the weirdness is negative, dappled taiga where positive (the dappled taiga is already the birch forest's cold, weird side) |
| dark-taiga | old growth spruce and pine taiga and dark forest within 0.05 of their temperature border (-0.15) | dark taiga forest (dark oak among spruce): its place now, in place of the weird rules' |
| hot-forest | forest at temperature 0.2 and up (vanilla's forest between savanna and jungle) | warm oak forest, so a jungle never meets a plain forest |
| hot-plains | plains at temperature 0.2 and up, humidity 0.04 and up (next to the sparse jungle) | warm oak forest |
| pale-taiga | the pale garden's cold strip (temperature -0.1 and under), next to the old growth taigas | **Pale Taiga** (placed at last: spruces and pale oaks, pale moss) |
| pale-swamp | the wettest, weirdest swamps (humidity 0.35 and up, weirdness 0.3 and up), warm and cold | **Pale Swamp** (placed at last: pale oaks, pale moss, creaking hearts) |

River banks get their transition as features at the water's edge rather than a biome (a strip of biome by weirdness
would follow the river's middle, not its banks): reeds, tall grass and ferns, willows (swamp oaks with vines) and muddy
banks, in the temperate biomes beside water (willows only where trees are deciduous).

### Sub-biomes

| Sub-biome | Where (a pocket of its parent's climate) | What it is |
|---|---|---|
| **Old Oak Forest** | forest, temperature -0.1 to 0.1, humidity -0.04 to 0.06, weirdness 0 and up | great oaks (a tall fancy oak) and fancy oaks over moss and azaleas, fallen logs, ferns, mushrooms, vines on trunks |
| **Prairie** | plains, temperature -0.3 to -0.05, humidity -0.3 to -0.2, weirdness 0 and under | a sea of tall grass and wildflowers, a lone oak now and then; horses, donkeys and rabbits |

### Balance

- Dark forest: the warm dark forest from temperature 0.1 (was 0.05); the dark taiga forest only as the taiga
  transition (the weird rules no longer touch dark forests); the dark birch strip's dark side to humidity 0.32 (was
  0.35). The plain dark forest keeps 40% or more of the dark forests' area.
- Swamps: the cold swamp's drier part (humidity -0.15 and under) is grassy wetlands too, and its rim (erosion to 0.565)
  humid oak forest, as the warm swamp's. Grassy wetlands 0.15% or more of all columns, humid oak forest 0.06% or more.

### Mobs

Every mod biome's animals are chosen for the place, in the generators (no merged lists): farm animals where there is
grass and room, frogs in every wetland, parrots and pandas toward jungles, goats on mountainsides and rocky plains,
horses and donkeys on open grass, wolves and foxes in the woods and taigas, none of the cold or open land's animals in
warm wet places. Monsters by climate: the swamp's (bogged, slimes, witches) in the wetlands, ocelots in the jungle-side
biomes, husks and parched in the oasis, strays in the cold. Variants follow the climate: warm and cold farm animals and
frogs by the game's tags, and wolves' coats by the game's wolf variants (woods in the forests, rusty toward jungles,
ashen in the cold taiga, chestnut by the old taigas).

### Features

The older biomes get the undergrowth and ground the newer ones have (leaf litter under broad trees, bushes, ferns in
the cold woods, fallen logs, berries), the cold beach is made a beach (no flowers or badlands grass on it), and the
earlier parts' deferred items: the waterlogged leaves only where the water is two deep and in their own biome, the
mangrove marsh's bushes out of the water, the leaves' game test from the chunk's corner, the docs' counts and javadoc.

## Checks

- `PvBiomeBalanceTest` (new): the real climate (seed 1234) on a grid; the dark forest's share of its family, the
  wetlands' and humid oak forest's shares, no jungle beside plain forest, forest | plains and forest | taiga borders
  down by two thirds or more, every new biome present.
- `PvBiomeLayoutTest`: each rule slice by slice; the pieces still partition vanilla's slices.
- `BiomeDataTest`: the new biomes exist, are tagged and named; the animals fit (no cattle or wolves in the mangrove
  marsh, frogs in every wetland, camels and husks in the oasis, warm and cold variants by climate).
- Game tests: the riverbank features stand only by water; every tree feature grows tree blocks (the great oak with it).
- `BiomeFeatureOrderTest`, the census and the gallery (looked at).

## Not in this part

New blocks or fruits; terrain; the rocks; other biomes' sub-biomes (savanna, taiga, desert interiors).
