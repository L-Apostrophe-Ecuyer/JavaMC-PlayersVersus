# The warm dark forest, denser warm forests, the Tuscan grove, the wetlands (part 4)

Part 4 of the rocks, terrain and transitions pass (1 rocks rebuilt, 2 the 3D-noise zone, 3 rocky biomes, 4 this). It
builds the rows of `2026-11-09-rocks-transitions-wetlands-design.md` that were left open ("band to derive") and the
earlier list of requests:

- a warmer variant of dark forests; swamp transitions; waterlogged leaves in the water of swamps; a grassy wetlands
  biome bordering swamps; mangrove swamp transitions and less brownish foliage; jungle transitions to regular forests
  (the warm band, built in the first pass: warm oak forest, dark jungle forest);
- from that spec's "Biomes" list: the Tuscan grove redone (cypresses, olive-grey oaks, fields of flowers and dry grass);
  the warm oak forest, dark jungle forest and humid oak forest denser and with undergrowth (the humid oak forest was
  nearly bare on slopes).

## What the climate tables say

Measured over random land (seed 1234, continentalness from -0.11): swamps (vanilla's: temperature -0.45 to 0.2,
erosion 0.55 and up, **every humidity**) are 1.8% of the land, 39% of them cold (the cold swamp, from -0.15 down);
mangrove swamps (the same, temperature 0.2 and up) 0.9%. Of a swamp's columns 10% have erosion under 0.565 (its rim, the
humid oak forest), 35% under 0.6; by humidity 24% are drier than -0.2, 53% drier than 0. Dark forests (humid slices:
humidity 0.3 and up, temperature -0.15 to 0.2) border the dark jungle forest at 0.2.

## Decisions

| Question | Decision |
|---|---|
| Warm dark forest | The dark forest's warm side: temperature 0.05 to 0.2, after the humid band (which keeps its strip of dark birch forest). Dark oaks, with vines, oaks and fancy oaks among them, jungle bushes, a third of the dark forest's giant mushrooms; temperature 0.8 (leaves warmer), the dark forest's grass; bushes, leaf litter, vines. Between the dark forest and the dark jungle forest |
| Grassy wetlands | The drier part of the warm swamp: humidity -0.15 and under (about 30% of its columns past the rim), erosion above the rim (0.565), temperature -0.15 to 0.2. Open wet meadow: the marsh drier (pools and channels, more land), tall grass, reeds, flowers, few oaks, lush green water and grass. Bordered by the swamp (wetter), the humid oak forest (the rim) and the land beyond |
| Mangrove marsh | The mangrove swamp's rim (erosion under 0.565) and its drier part (humidity -0.15 and under): mud flats and grass (the marsh with mud for its edge), small mangroves and jungle bushes |
| Waterlogged leaves | Clumps of waterlogged, persistent leaves at the water line of swamps (oak in the swamp, spruce and birch in the cold swamp, oak in the grassy wetlands), among the trees: `players-versus:waterlogged_leaves` |
| Mangrove foliage | `minecraft:mangrove_swamp`'s foliage colour `#8db127` (yellow-brown green) becomes a clear green; the override also changes vanilla world types, as the mod's swamp's does |
| Tuscan grove | Cypresses (a narrow dark column: `players-versus:cypress`, spruce leaves), oaks in olive-grey leaves (the biome's foliage colour), the lemon orchards, fields of wildflowers and dry grass |
| Denser | Tree noise ratios and offsets up by about half (warm oak forest 12 to 18, dark jungle forest 16 to 22, humid oak forest 12 to 17), undergrowth added: bushes, ferns, grass patches |

## Checks

- `PvBiomeLayoutTest`: each rule, slice by slice (the warm dark forest's band; the grassy wetlands' humidity; the mangrove
  marsh's rim and drier part), the pieces still partition vanilla's slices; `BiomeFeatureOrderTest` (the new biomes agree
  on the order of their features).
- Game test: waterlogged leaves float at the water line of a pond, persistent and waterlogged, on none of the land.
- Census (`runWorldgenSmoke -Ppv.census=...`): every new biome generates; the denser biomes' tree counts (before and after).
- Looking: the gallery's shots of each new biome and the changed ones.

## As built

Where the build differs from the table above (the plan's ledger holds each ruling and the census numbers):

- **Tree densities** went above the table's numbers: warm oak forest 12/0.45 to 22/0.55 (tree noise ratio/offset), dark jungle forest 16/0.55 to 34/0.7, humid oak forest 12/0.45 to 20/0.55, Tuscan grove 5/0.0 to 8/0.1. The table's own numbers raised the census's leaves only 19%, 11% and 78% (tries fail and canopies overlap); the built ones give +35%, +43%, +86% and +92% over the same patches.
- **Warm dark forest**: as dense as the dark forest by the census (tree noise 22/0.55 against its flat 16 tries a chunk), and a third of its giant mushrooms, brown and red, tried first in the selector (found by the final review: the mushroom had been tried last, at 0.4% a try).
- **Cypress**: a column three blocks across made with the spruce's foliage placer (radius 1). The pine's, first used, adds a random radius and made most cypresses cones (found by the final review; `TuscanGroveGameTest`).
- **Tuscan grove** fields: flowers where the biome noise is above 0.1 (three patches a chunk) and the desert's dry grass where it is below -0.05 (two), `tuscan_flower_field` and `tuscan_dry_grass`. The flowers are the mod's own mix (`tuscan_flowers`: poppies, cornflowers, oxeye daisies, dandelions, alliums, azure bluets, some grass): the meadow's provider, first used, picks one to three kinds for a thousand blocks at a time, and `TuscanGroveGameTest` found a field of one kind.
- **Waterlogged leaves**: three placed features (swamp, cold swamp, grassy wetlands), each placed by its biome's trees' noise (found by the final review: counted flat, the wetlands' pools were dotted with leaves far from any tree); the cold swamp's mix is a random selector of spruce and birch leaves.
- **Mangrove swamp**: the foliage colour `#62a83e` (the marsh's) in an override file, vanilla's in every other field (`BiomeDataTest` compares it with the game's own).
- **The census** counts the mod's `*_tree` blocks as trees (it had counted none of the trunks).
- The grassy wetlands' share of water follows the terrain under them: 56% in one low coastal patch, and a meadow with ponds where the ground is higher.

## Not in this part

Anything of the wetlands' ground beyond the marsh's existing rings; the swamp rim's width (0.565, as before).
