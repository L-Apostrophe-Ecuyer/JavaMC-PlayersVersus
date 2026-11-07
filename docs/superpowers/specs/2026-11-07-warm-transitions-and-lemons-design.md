# Warm transitions, swamp rims, temperate replacements and lemons

What was asked, what was decided, and how each part is checked. The layout rules themselves are described in
`docs/worldgen-refactor-plan.md` section 1.4.

## Why no transition appeared

The `warm` rule with the `END_BARRENS` placeholder did work: a census of a real world (seed 8675309) found end barrens
420 blocks from spawn, the main biome there, and no savanna within 8,000 blocks. End barrens has no features, so in the
overworld it looked like bare grassland. Its region (temperature ≥ 0.15, humidity ≤ 0.375) also covered whole savannas
and jungles instead of their edge, and the version before it used the weirdness axis (`Box.W`) where temperature was
meant. `OldBiomeLayout`, edited alongside, is test-only: it is the reference the comparison test measures against.

## Decisions

| Question | Decision |
|---|---|
| Where the warm transitions go | The cool edge of the warm biomes: temperature 0.2–0.3 (vanilla's warm biomes start at 0.2) |
| Which transition where | By what lies beyond: savanna → Tuscan grove (plains beyond); jungle with humidity < 0.3 → warm oak forest (birch forest, forest); wetter jungle and bamboo jungle → dark jungle forest (dark forest) |
| Swamp transition | A rim on every swamp, erosion 0.55–0.6, humid oak forest; mangrove swamps unchanged |
| Windswept and old growth birch at temperature 0–1 | Replaced before the transitions: windswept hills → plains, windswept forest and old growth birch → forest, windswept savanna → plains below 0.2, savanna from it; the peak transitions that made windswept hills above freezing make meadows |
| How the transitions blend | Underground from the main neighbour; trees from both by noise (groves and clearings); colours halfway (the game's colour blending fades them); both neighbours' animals |
| Lemon trees | Bushes: 2–3 stripped oak logs under oak's round crown, which hangs nearly to the ground (inspired by Pam's HarvestCraft and Fruit's Delight fruit trees) |
| Lemon leaves | Bare → blossoms → lemons on random ticks (1 in 8) or bone meal; right-click picks 2–6 and leaves them bare; broken fruiting leaves drop their lemons; a lemon sapling grows the bush |
| Lemon | Sweet berries' food (2, 0.1), compostable, not plantable; eating one makes one Sour |
| Sour | 1.5 s; the view widens to 1.9× within 2 ticks, holds, eases back over the last 22; scaled by the FOV Effects slider; never past 165°. Applied at `Camera.calculateFov`, past vanilla's 1.5× clamp of the movement modifier |

## Found on the way

- Swamps' floating disks of grass and mud: the small swamp mangrove's `alter_ground` used a weighted provider, which
  in 26.4 is placed two blocks above the trunk's base whatever is there (found with a `setBlock` probe). It is rule
  based now, as vanilla's podzol is. Swamp trees are placed by noise, as the savanna's are.
- Two swamp patches were also listed in the swamp with no placement, so they ran once a chunk at the bottom of the world.
- 26.4's loot tables take `condition` and `modifier` where older ones took `conditions` and `functions`; the old names
  are silently ignored.
- The block tag audit asked for leaves in `#blocks_motion_no_leaves`; leaves block motion by `#leaves`.

## Checks

- Unit: `PvBiomeLayoutTest` (the band on every warm slice, the swamp rim, no windswept or old growth birch above
  freezing, the slices still partitioned), `BiomeFeatureOrderTest`, `SourFovTest`.
- Game: `LemonsGameTest` (the bush's shape, picking, fruiting by ticks and bone meal, oak logs keep the leaves, drops,
  eating), `TreeGrowthGameTest` (the bush's stripped trunk is expected).
- Census (`runWorldgenSmoke -Ppv.census=...`) on seed 8675309: all four biomes generate; the swamp's floating ground
  went from 257 blocks to none.

## Wetlands (asked after: grass islands ringed by mud and water, a cold swamp, gradients instead of random patches)

| Question | Decision |
|---|---|
| Microbiomes | A Cold Swamp biome for the cold half of swamps (temperature ≤ −0.15, next to taigas); marsh clearings in every swamp where the trees' noise leaves gaps |
| Mud, dirt and water | One smooth noise (dryness) per column: channels and islands near the water line, rings of gray mud, brown mud, an edge block and grass around every pool (`MarshFeature`). Replaces the patches of randomly mixed blocks |
| Why a feature, not a surface rule | PV's swamps are mostly flooded flats (about 70% water in a census); a surface rule only recolours ground that exists, so the islands and channels need blocks moved |
| Cold Swamp | Spruce and birch by noise, ferns, berries, the taiga's grass, podzol in its rings, cooler colours, cold frogs, witch huts |
| Swamp rim | Narrowed to erosion 0.565: at 0.6 it took most of the swamp |

Checked by `MarshFeatureTest` (rings in order, islands and channels against the water line, every ring over a stretch of
noise), `PvBiomeLayoutTest.theColdHalfOfSwampsIsColdSwamp`, and a census of seed 8675309 whose new ground map (the census
writes `pvwg/census-<biome>.png` from the finished chunks) shows the islands and rings.
