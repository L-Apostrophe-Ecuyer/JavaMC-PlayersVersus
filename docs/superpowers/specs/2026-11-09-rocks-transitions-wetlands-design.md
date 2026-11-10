# Rock formations, warm transitions and wetlands (second pass)

What was asked, what was decided and how each part is checked. It follows
`2026-11-07-warm-transitions-and-lemons-design.md`; the layout rules are described in
`docs/worldgen-refactor-plan.md` section 1.4.

## What was asked

1. More transition biomes: a warmer dark forest, swamp transitions, waterlogged leaves in swamp water, a grassy
   wetlands biome bordering swamps, warm variants in place of the windswept biomes of warm areas, mangrove swamp
   transitions with less brown foliage, jungle transitions toward regular forests.
2. "The real task": boulders and rock formations made with noise, as good-looking as possible, compared with four
   reference screenshots (tall mossy monoliths with terraced grass skirts, a cluster of spires, low jagged boulder
   piles). As rock features and, for the bigger spires, boulders and cracked monoliths, as formations on a world grid.
   They go into new biomes.
3. Then: "improve this all substantially": generation, the savanna replacements and warm transitions, the wetlands, the
   rocks.

## Seeing it: the gallery

Tuning biomes and rocks from census numbers and top-down maps is blind work. `WorldgenGallery` (a client game test)
runs the real client, creates a world from the Create World screen, flies a camera to each biome's nearest patch or to
given coordinates, runs commands, and saves screenshots; `tools/gallery.sh` runs it without a display. It does nothing
without `-Ppv.gallery`, and `check` doesn't run it. Found on the way: Mesa's software Vulkan takes minutes a frame with
26.4's renderer (OpenGL through EGL is used instead; Xvfb's GLX has none of the sRGB framebuffers the game asks for),
and the test API's own chunk wait never ends from a render distance of 7 (it asks for corners the server never sends).

## Rocks

| Question | Decision |
|---|---|
| How a rock is made | A field over space, positive inside: lobes fused together (columns with a rounded top, or boulders: ellipsoids cut flat by planes), pushed in and out by noise (bulges, grooves down the face, benches around it, roughness), split by cracks. Cells inside are rock (`RockModel`) |
| Why a field of lobes, not raw 3D noise | The references are simple masses (an egg, a bullet, a heap) with detail on them. Noise alone gives blobs with holes; a fixed outline gives a silhouette one can aim at, and noise only has to roughen it |
| Cracks | Upright planes that wander, open from a height up and widening to the top, closed in places (bridges). A crack can drop the far side of the rock by a share of its height: twin peaks |
| Materials | By role (`RockMaterial`), blocks from a palette in the JSON: the rock, two kinds of layers along a tilted, waving height, streaks down the face, weathered and mossy patches (more near the ground and on boulders) |
| Plant cover | On upward faces by how level they are (the field's gradient), how high, and a noise: grass on top under the sky, moss on ledges and hanging from the edges, vines below, plants on the grass |
| Fine shape | Slabs where the surface passes through the lower half of a cell (boulders and bare tops) |
| At the ground | Each body stands on the ground at its own middle and goes on into it; columns that reach the model's bottom are filled down to the ground. Around a big rock the ground rises toward its foot with the ground's own blocks (a skirt), with scree and loose stones |
| Pure code | The model, the plan and the stamping (`RockNoise`, `RockPlan`, `RockTemplate`, `RockModel`, `RockStamper`, `RockCanvas`) use no game classes: tests build rocks, and `RockPreview` draws them with the game's textures in seconds |
| Small rocks | Feature type `players-versus:rock` (boulders, piles, crags, small spires): everything within 15 blocks of its origin, as a feature may only write in the chunks around its own |
| Big rocks | Structure type `players-versus:rock_formation`, one piece a body, placed by `random_spread` (a world grid): spires, monoliths, massifs and giant boulders 25 to 60 blocks across. A piece draws only the columns of the chunk being made, from the same model: no seams, any order |
| Where | The new rocky biomes (below). `/pvwg rock <id> [seed]` places any of them where one stands |

## Layout (PvBiomeLayout)

| Area (climate) | Now, in `PvBiomeLayout` | Proposed |
|---|---|---|
| Windswept hills and gravelly hills, temperature ≥ 0 | plains | Rocky Hills (new): grass hills with boulders, crags and a few oaks |
| Windswept savanna, temperature ≥ 0.2 | savanna | Rocky Savanna (new): dry grass, acacias, boulders and spires |
| Windswept savanna, temperature < 0.2 | plains | Rocky Hills (new) |
| Windswept forest and old growth birch forest, temperature ≥ 0 | forest | open: keep forest, or a rocky forest |
| Savanna and savanna plateau, temperature ≤ 0.3 (the warm band) | Tuscan Grove | Tuscan Grove, redone: cypresses, colours |
| Jungle and sparse jungle, humidity < 0.3, temperature ≤ 0.3 | Warm Oak Forest | Warm Oak Forest, denser |
| Jungle and sparse jungle, humidity ≥ 0.3; bamboo jungle, temperature ≤ 0.3 | Dark Jungle Forest | Dark Jungle Forest, denser |
| Humidity 0.275–0.35 (the humid band, outside the warm band) | jungle → sparse jungle; birch and dark forest → dark birch forest; desert → oasis; badlands → desert | unchanged; the jungle to forest transition is open (to derive) |
| Dark forest, weirdness ≥ 0, temperature ≤ 0 | dark taiga forest | unchanged |
| Dark forest on its warm side (band to derive) | dark forest | Warm Dark Forest (new) |
| Swamp, erosion ≤ 0.565 (the rim) | humid oak forest | open: Grassy Wetlands where the rim is drier (to derive: swamps are humid, so the split needs the parameter table) |
| Swamp, temperature ≤ -0.15 | cold swamp | cold swamp, unchanged |
| Mangrove swamp (no rim now) | mangrove swamp | Mangrove Marsh (new): its rim band to derive |

Rows marked open or to derive need a climate box read from the parameter table before the layout tests are written.

## Biomes

- Tuscan Grove: cypresses (a narrow dark column), olive-grey foliage on its oaks, lemon orchards, fields of flowers and
  dry grass.
- Warm Oak Forest, Dark Jungle Forest, Humid Oak Forest: denser and with undergrowth; the humid oak forest was nearly
  bare on slopes.
- Warm Dark Forest: the dark forest's canopy in warmer colours, oaks among the dark oaks, bushes, vines, fewer giant
  mushrooms.
- Grassy Wetlands: open wet meadow: pools and channels (the marsh, drier), reeds, tall grass, flowers, few trees.
- Mangrove Marsh: mud flats and grass with small mangroves and jungle bushes.
- Mangrove Swamp: greener foliage. Swamps: clumps of waterlogged leaves at the water line among the trees.
- Rocky Hills, Rocky Savanna: the rocks' homes.

## Checks

- Unit: `RockModelTest` (the same seed gives the same rock; nothing floats; a rock stamped chunk by chunk in any order
  is the rock stamped whole; it stays in its bounds, a feature's within 15 blocks; it reaches the ground on a slope;
  cracks are open to the sky; cover only on upward faces), `RockTemplateTest` (the JSON's defaults and errors),
  `PvBiomeLayoutTest` (each row of the layout table), `BiomeFeatureOrderTest`.
- Looking: `RockPreview` renders next to the four references; the gallery's screenshots of every biome above and of
  every rock, before and after.
- Running: `runWorldgenSmoke` for the Players Versus and vanilla world types, the game tests, a census of the new biomes.
