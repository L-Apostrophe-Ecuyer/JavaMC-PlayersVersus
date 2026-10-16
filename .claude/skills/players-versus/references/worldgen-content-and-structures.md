# Worldgen Content: Biomes, Features & Structures

Data-file layer sitting on top of the engine described in `worldgen-engine.md` — biomes, their
carvers/features/spawners, and jigsaw structures. This doc orients by directory structure and a
couple of spot-checked examples rather than exhaustively describing every JSON file; treat it as a
map, not a transcription.

## Biomes (`data/players-versus/worldgen/biome/`)
Two groups:
- **Surface transition biomes** (12 top-level files): `birch_taiga_forest`, `dark_taiga_forest`,
  `dark_birch_forest`, `cold_beach`, `cold_plains`, `cold_taiga`, `mountainside_jungle`,
  `mountainside_forest`(+`_warm`), `desert_oasis`, `pale_swamp`, `pale_taiga` — these are the
  targets referenced by `CustomOverworldBiomes`'s replacement maps and `PvBiomeLayout`'s transition
  rules (see `worldgen-engine.md` §"Biome layout"); each is a normal biome file (climate/effects/
  spawners/carvers/features) but is only ever *reached* through the PV biome-source layout logic,
  never placed directly by a parameter box of its own outside that Java-side derivation.
- **`caves/` (6 biomes)**: `badlands_cave`, `creeper_caves`(+`_desert`), `deep_caves`,
  `pale_hollow`, `regular_cave` (`frosted_caves` was removed in the 26.4 port: vanilla's Ice Caves
  take its places in the layout) — the README's "new cave biomes; frozen caves, deep caves, badland
  caves, desert caves, & creeper caves." Spot-checked `deep_caves.json` (above table shows the
  pattern all cave biomes likely follow): heavy monster-spawner weighting toward the mod's own mobs
  (`players-versus:pale_creeper` weight 80, `players-versus:pale_zombie` weight 100 vs.
  vanilla zombie weight 20 — **the custom mobs are the majority of what spawns in Deep Caves**, not
  a rare addition), a dedicated ambient music track per cave biome (`players-versus:music.overworld.*`,
  see `assets/players-versus/sounds/music/caves/`), and custom-namespace carvers/ore
  features (`players-versus:deep_caves_extra_underground`/`_canyons` carvers,
  `players-versus:caves/ore_terracotta_and_copper_gray` etc. features) layered alongside vanilla ore
  features at the same generation step.

## Configured/placed features (`data/players-versus/worldgen/{configured_feature,placed_feature}/`, `caves/` subdirs)
~30 configured features + ~30 caves-specific configured features; ~60 placed features overall (see
`architecture.md`'s file-count table from initial scouting). Naming pattern: features specific to a
cave biome's floor/ceiling decoration live under `caves/` (e.g. `badlands_cave_ceiling`,
`badlands_cave_floor`, `creeper_desert_patch(_ceiling)`, `ore_copper_{blue,green,orange}` — colored
copper-vein variants placed only in specific caves), while surface/general features
(`brown_mud_patch`, `patch_wheat_grass*`, `stone_stalagtite`, `stone_overhangs_*`,
`mud_lake`/`mud_pool_with_{dirt,wheat}`, `swamp_*`, `desert_*`) live at the top level. Two custom
Java `Feature` types (`MudPatchFeature`, `StoneStalagtiteFeature` — see `blocks-and-environment.md`/
`worldgen-engine.md`) back the mud-patch and stalactite/stalagmite features specifically; everything
else composes vanilla feature types (`ConfiguredFeatures.ORE`, `RandomPatchFeature`, etc.) with new
parameters.

## Vanilla-namespace worldgen overrides (`data/minecraft/worldgen/**`)
**~124 files** overriding vanilla biomes/configured_features/placed_features/carvers — per
`worldgen-engine.md`'s Open Question 1, these currently affect **vanilla world types too**, not just
the PV preset (carver probabilities specifically called out there: mod's `cave`/`cave_extra_underground`/
`canyon` rates are far below vanilla defaults). Treat any change here as **global**, not
PV-preset-scoped, until that open question resolves — this is the single biggest "surprising blast
radius" trap in the whole worldgen-content layer. `data/minecraft/tags/worldgen/{biome,structure}/**`
(22 files) carries the same caveat.

## Structures (`data/players-versus/{structure,worldgen/{structure,structure_set,template_pool,processor_list}}/`)
Three structure families, each jigsaw-based (`"type": "minecraft:jigsaw"`):

- **Trial Towers** (`trial_tower`, `trial_tower_cold`, `trial_tower_snowy`, `trial_tower_desert`,
  `trial_tower_warm` — 5 structure JSONs sharing one `structure_set` with weight 1 each, i.e. equal
  chance per placement roll; `random_spread`, spacing 40 / separation 12, excludes a 6-chunk zone
  around villages). Spot-checked `trial_tower.json`: birch/old-growth-birch/forest biomes only,
  `underground_structures` step, `terrain_adaptation: bury`, `start_pool: trial_tower/tower`, size 3,
  fixed `start_height.absolute: -12` — **always generates at a fixed depth**, not surface-relative.
  Template pools live under `worldgen/template_pool/trial_tower/{tower,tower_cold,tower_desert,
  tower_snowy,tower_warm,chamber,chamber_cold,chamber_warm}.json`; NBT pieces under
  `structure/trial_tower/*.nbt` (towers 1-5 + cold/desert/snowy/warm/illager variants, chambers
  themed dripstone/frozen/minotaur/mud/ring_of_fire×2/slime×3/water×2 — README: "new Trial Towers").
- **Desert Pyramids** (`structure/pyramid`, `worldgen/template_pool/pyramid/**`): basement(×6)/main(×6,
  incl. a "small" variant)/side_challenges(24 pieces!)/support_beams(front+back)/small_pyramid_side —
  by far the most side-content of the three families (24 side-challenge NBTs alone) — README: "new
  desert pyramids." `worldgen/structure_set/desert_dungeons.json` and
  `worldgen/processor_list/desert_pyramid.json` complete the wiring.
- **Cold Trail Ruins** (`structure/trail_ruins_cold`, `worldgen/template_pool/trail_ruins_cold/**`):
  buildings(34 pieces, `buildings/grouped` subfolder), decor(7), roads(7), tower(35 pieces incl.
  `tower/additions` and `tower/tower_top` subfolders) — the largest single structure family by piece
  count; `worldgen/structure_set/trail_tower.json` name is shared/reused with the Trial Tower set
  (confusing naming — **`trail_tower.json` the structure_set is NOT about Trial Towers**, verify
  which structure_set file backs which structure before editing; check the `"structures"` list
  inside the file, not just its filename, given this naming collision). Processor lists:
  `trail_ruins_{houses,roads,tower_top}_archaeology.json` (2 dedicated processors each named for
  archaeology — likely seeding suspicious-block placement into these structures specifically, tying
  into `blocks-and-environment.md`'s archaeology section).
- **Zombie-infested vanilla villages** (`data/minecraft/structure/village/{plains,desert,savanna,
  snowy,taiga}/zombie/**` + matching `worldgen/template_pool/village/*/zombie/**` and
  `worldgen/processor_list/zombie_{plains,plains_street,desert,savanna,snowy,taiga}.json`) — a
  vanilla-namespace override adding zombie-village variants across every village biome type
  (README: "new... ruined villages" — this is that feature, despite the "ruined" wording sounding
  like Ruined Portals; it's actually zombie-infested regular villages, not the vanilla Ruined
  Village structure).

## Validating structure/feature references
No automated check was run in this pass (see `data-and-assets.md` for the validation-script
recommendation covering JSON parse + asset-reference integrity generally). Before large structure
edits, at minimum spot-check: every `template_pool` element's `"location"` resolves to an existing
`.nbt` (mod or vanilla), every `processor_list`/`structure`/`structure_set` id referenced actually
exists, and every biome list in a structure JSON matches real biome ids (mod or vanilla) — a typo
here fails silently at structure-placement time (the structure just never spawns), not at build time.

## Extension recipe: adding a new structure variant
1. Model the piece(s) in-game/BlockBench-exported → export as `.nbt` under a new
   `structure/<family>/<name>.nbt`.
2. Add/extend a `worldgen/template_pool/<family>/*.json` entry pointing at it (weight relative to
   siblings in the same pool).
3. If it's a wholly new structure (not just a new piece in an existing pool), add a
   `worldgen/structure/<name>.json` (jigsaw config: biomes, start_pool, size, terrain_adaptation,
   start_height) and register it in the family's `worldgen/structure_set/*.json` (or a new one).
4. Add a loot table under `loot_table/chests/<family>/**` if it has chests, and a processor_list if
   it needs archaeology/suspicious-block seeding or block substitution.
5. Sanity-check placement in-game with `/locate structure players-versus:<id>` and, for anything
   touching the PV world type's aquifer/biome interplay, cross-check with `/pvwg probe`
   (`worldgen-engine.md`).
