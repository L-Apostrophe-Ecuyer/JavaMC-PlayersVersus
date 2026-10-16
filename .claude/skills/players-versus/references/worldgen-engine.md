# Worldgen Engine (Players Versus world type)

**This is the most actively-changing subsystem in the codebase — re-check it against source
before trusting any status claim below, including this doc's.** There is a living design
document, [`docs/worldgen-refactor-plan.md`](../../../docs/worldgen-refactor-plan.md) (revision 3),
that is far more detailed than this file: full vanilla API signatures (verified against Yarn
1.21.10), every density-function formula written out (Appendix A), a Mojang-mapping name table
for a future 26.x port (Appendix D), and the phased migration plan. **Read that file first for
anything beyond "what exists today and where."** This reference exists to (a) orient a maintainer
quickly, (b) record where the plan doc and the actual code have already diverged, and (c) give
day-to-day commands (build/test/bench).

## What this is, in one paragraph

Players Versus ships its own world type ("Improved", preset `players-versus:better_world`,
world-preset id registered as `CustomWorldgen.BETTER_WORLDGEN_PRESET`) that vanilla world types
never run any of this mod's worldgen code for. It replaces vanilla's water-table aquifer and ore
veins with mod-specific logic, replaces the overworld biome-parameter list with one that adds
transition biomes (mountainside/frozen/humid) and ~13 cave biomes, and is mid-refactor toward
replacing 18 hand-written density-function JSON files and a 41 KB surface-rule JSON with Java
code. The refactor is **in progress and partially reverted** (see Status below) — this is not
finished, stable code; treat any change here as needing a `/pvwg bench` or `runWorldgenSmoke`
comparison against the vanilla `Default` preset before trusting it.

## Current status vs. the plan doc (verify before relying on either)

The plan doc's Section 4/7 phase tables under-state what's actually live in code, and one Phase-2
experiment it describes has already been tried **and reverted**. As of this write-up:

| Plan phase | Plan doc says | Code actually shows |
|---|---|---|
| 0 — groundwork | "mostly done" | Done: single `ChunkNoiseSamplerMixin`, `PvAquifer`/`PvAquiferRules`/`PvAquiferDecision`, `PvOreVeins`, `/pvwg probe`+`/pvwg bench`, `remappedSrc/` and the four old mixins deleted. |
| 1 — biome layout as data | listed as not-yet-done ("1") | **Actually live**: `PvBiomeSource`/`PvBiomeLayout` exist, are registered (`Registries.BIOME_SOURCE`, id `players-versus:overworld`), and `better_world.json`'s `biome_source.type` is already `players-versus:overworld` — the old `VanillaBiomeParametersOverworldMixin` this replaced is deleted. **Update the plan doc's status table when you next touch this, it's stale.** |
| 2 — lattice-cached aquifer (F/S on a 4-block lattice) | described as the target | **Tried and reverted** (commit `373b78f` implemented it, `3f04892d` "Revert the lattice aquifer: interpolating F and S whole breaks ocean surfaces" undid it). `PvAquifer`'s own class javadoc now explains why: interpolating floodedness/spread wholesale changed 19% of decisions in y 48–63 and put stone barriers on ocean surfaces, because both functions step sharply inside their bands. **`Lattice.java` still exists and is unit-tested (`LatticeTest`, `AquiferLatticeTest`) but is currently unused by `PvAquifer`** — don't assume it's wired in just because it compiles and has tests. The plan's revised idea (lattice only the *smooth* inputs feeding F/S, not F/S themselves) is not yet implemented. |
| 3 — Java density-function kernels | not started | Not started — all 18 files under `data/players-versus/worldgen/density_function/**` are still hand-written JSON, still evaluated per-block through the vanilla noise router. |
| 4 — `PvChunkGenerator`/`PvSettings`/`PvSurfaceRules` in Java | not started | Not started — `noise_settings/overworld.json` (surface rules) is still JSON; the generator is still vanilla `NoiseChunkGenerator` fed PV settings, not a custom `ChunkGenerator` type. |

**Practical takeaway:** the aquifer and ore-vein *hook* mechanism (Section 6.1) is solid and
done; the aquifer *math* is presently identical in spirit to the pre-refactor `SimpleWaterAquifer`
(same JSON-driven per-block sampling), just re-plumbed through a cleaner, single-mixin gate; the
biome layout is the one piece that has actually moved to the new Java-data architecture.

## The one mixin: `ChunkNoiseSamplerMixin`

Target: `ChunkNoiseSampler` (constructor only). Two `@WrapOperation` hooks (MixinExtras, chains
with other mods instead of conflicting like `@Redirect`/`@Overwrite` would):
1. Wraps the vanilla call `AquiferSampler.aquifer(...)` — if `PvWorldgen.isPvGenerator(settings)`,
   returns `new PvAquifer(noiseRouter, chunkPos, fluidLevelSampler)` instead of calling vanilla.
2. Wraps `OreVeinSampler.create(...)` similarly, substituting `PvOreVeins.create(...)`.

Both read the constructor's `ChunkGeneratorSettings` via `@Local(argsOnly = true)` — i.e. the
**settings object itself**, not a per-chunk copy — specifically so that C2ME's density-function
compiler (which can rewrite per-chunk density function instances) can't hide the PV marker from
the gate. This is a deliberate anti-fragility choice; if you ever refactor this hook, preserve
reading from the settings-level router, not a chunk-local one.

**The gate:** `PvWorldgen.isPvGenerator(ChunkGeneratorSettings)` returns true iff the settings'
noise router's `fluid_level_floodedness` slot unwraps (through any number of
`DensityFunctionTypes.RegistryEntryHolder` layers) to an instance of `AquiferFloodedness`
(`mod/environment/worldgen/density/AquiferFloodedness.java`, registered under
`players-versus:aquifer_floodedness`). Every vanilla world type's settings lack this marker, so
they always fall through to `original.call(...)` — **vanilla world types run zero PV worldgen
code**, which is the hard invariant this whole subsystem is built to preserve (see plan Section 3,
first bullet). If you ever add a new PV-only worldgen behavior, gate it the same way — don't add
a second, differently-implemented "is this a PV world" check.

## Aquifer: `PvAquifer` + `PvAquiferRules` + `PvAquiferDecision` + `PvWorldgenConstants`

One `PvAquifer` per `ChunkNoiseSampler` (i.e. per chunk). `apply(pos, density)`: density > 0 means
solid terrain (returns null, clears the fluid-tick flag); otherwise delegates to
`PvAquiferRules.decide(pos, density, lavaLevel, floodedness, spread)`, where `floodedness`/`spread`
are direct method references to `noiseRouter.fluidLevelFloodednessNoise()::sample` /
`...fluidLevelSpreadNoise()::sample` — **per-block evaluation of the JSON-defined density
functions**, not a cache (see Status table above re: the reverted lattice).

`PvAquiferRules.decide` — pure function, shared with `/pvwg probe` so the debug command and the
real aquifer can never disagree:

| Order | Condition | Result |
|---|---|---|
| 1 | `density > 0` | `SOLID` |
| 2 | below the generator's lava level | `LAVA` |
| 3 | `y >= SEA_LEVEL` (64) | `AIR_ABOVE_SEA` |
| 4a | `y > SEA_BAND_MIN_Y` (-32) and floodedness > `SEA_WATER_THRESHOLD` (0.34) | `SEA_WATER`, or `SEA_WATER_TICKING` if within `FLUID_TICK_MARGIN` (0.2) of the threshold |
| 4b | same band, floodedness > `seaBarrierThreshold(y)` (0.0001, ramping up by 0.015/block above y 60) | `SEA_BARRIER` (stone) |
| 5a | `BASIN_MIN_Y < y < BASIN_MAX_Y` (-4..32) and spread > `basinWaterThreshold(y)` (0.5, ramping down by 0.08/block below y 8) | `BASIN_WATER` / `BASIN_WATER_TICKING` |
| 5b | `y < BASIN_BARRIER_MAX_Y` (23) and spread > `basinBarrierThreshold(y)` (ramps 0.06/block above y 12, floor 0.0001) | `BASIN_BARRIER` (stone) |
| 6 | otherwise | `AIR` |

All named thresholds live in `PvWorldgenConstants` with a comment on *why* and which JSON files
still duplicate them (until Phase 3 moves the density functions to Java, `SEA_WATER_THRESHOLD`
etc. must stay numerically identical to the literals baked into
`data/players-versus/worldgen/density_function/overworld/aquifer_fluid_level_floodedness.json`
and siblings — **changing a constant here without updating the matching JSON literal silently
desyncs the Java rule-decision layer from the density functions that feed it**).

## Ore veins: `PvOreVeins`

Same copper/iron vein logic the old `OreVeinMixin` `@Overwrite` had (`CustomWorldgen.VeinType`
enum: copper 32–96 in terracotta, iron -8–36 in tuff), now built via
`OreVeinSampler.create(veinToggle, veinRidged, veinGap, randomDeriver)` inside the same gated hook,
so it's PV-only instead of a global overwrite.

## Biome layout: `PvBiomeSource` + `PvBiomeLayout` + `CustomOverworldBiomes`

Registered biome-source type `players-versus:overworld` (`PvBiomeSource`, a small `BiomeSource`
wrapping a `MultiNoiseUtil.Entries` built once from `PvBiomeLayout.build()`). `PvBiomeLayout`:
1. Walks vanilla's `VanillaBiomeParameters().writeOverworldBiomeParameters(...)` callback.
2. For each **surface** slice (`depth == 0`), cuts it with `RULES` (mountainside, peak-warm,
   peak-cold, frozen, humid — matched in list order, first match per axis-box wins) using an
   explicit disjoint-box algorithm (`Box.intersect`/`Box.minus`) instead of overlapping ranges —
   this is the Q4 fix from the plan (mountain-transition overlap bug) already implemented.
3. Drops vanilla's `depth == 1` (underground) copy of every surface slice entirely — the PV
   underground belongs to cave biomes, not to a duplicate of the surface biome.
4. Replaces vanilla's Lush Caves and Dripstone Caves entries with PV variants (ice-cave splits —
   vanilla's Ice Caves since 26.4, where the frosted caves were — etc.) and appends
   `CustomOverworldBiomes.caveBiomesToPlaceInOverorld` (vanilla's ice caves, badlands,
   creeper caves regular/desert, regular cave, deep caves — climate-parameter boxes hand-tuned per
   entry) plus any `landBiomesToPlaceInOverorld` (currently empty — the extension point for adding
   a new *surface* biome).
5. Mountain/frozen/humid **target** biomes come from `CustomOverworldBiomes`'s three
   `Map<RegistryKey<Biome>, RegistryKey<Biome>>` tables (`MOUNTAIN_BIOME_REPLACEMENTS`,
   `FROZEN_BIOME_REPLACEMENTS`, `HUMID_BIOME_REPLACEMENTS`) plus a fourth for surface-cave biome
   selection (`SURFACE_CAVE_BIOME_REPLACEMENTS`) — **this is the file to edit to retarget which
   vanilla biome becomes which PV biome on a mountainside/in the cold/in the humid zone.**

`PvBiomeLayout.Entry` records which named `Rule` produced each entry, surfaced by `/pvwg probe`
and F3 — use it to debug "why is this the biome here" instead of re-deriving parameter math by
hand.

## Debug & benchmark tooling

- **`/pvwg probe`** (`WorldgenDebugCommands` → `WorldgenProbe`): prints, at your position, the
  biome (source vs. stored), the climate point, raw density values, floodedness/spread, and the
  `PvAquiferDecision` — the fastest way to sanity-check a change in-game.
- **`/pvwg bench <radius>`** (`WorldgenBench`): generates a square of chunks status-by-status,
  writes `report.txt` (per-status timings, correctness metrics, biome histograms) and PNGs
  (`surface`, `biomes-*`, `slice-y*`) to `<game dir>/pvwg/`. Tracks exactly the correctness metrics
  the plan's quirks (Q1/Q6/Q8) need: `basin_seam_ratio_x`/`z`, `stone_in_carved_space_below_y-8`,
  `fluid_ticks_queued_per_chunk`, `water_at_or_above_y64`.
- **`./gradlew runWorldgenSmoke -Ppv.acceptEula=true [-Ppv.levelType=... -Ppv.seed=... -Ppv.benchRadius=... -Ppv.benchCenter=x,z] [-Ppv.keepWorld=true]`**
  — headless dedicated-server run (`build.gradle`'s `worldgenSmoke` run config) that starts fresh
  in `run/worldgen-smoke/`, runs the bench, and stops itself. `-Ppv.levelType=server-default` tests
  what a server picks when `server.properties` doesn't name a level type (exercises
  `WorldPresetServerDefaultMixin`). **You must pass `-Ppv.acceptEula=true` yourself — this writes
  `eula.txt` accepting https://aka.ms/MinecraftEULA on your behalf, so only pass it if you actually
  accept.**
- **CI: `.github/workflows/worldgen-smoke.yml`** — runs vanilla `minecraft:normal` and
  `players-versus:better_world` side by side (matrix), each alone and paired with C2ME/Lithium,
  uploading timings/metrics/images/server logs. Triggers: `workflow_dispatch` (tick the EULA box in
  Actions → worldgen-smoke → Run workflow — only appears once this file is on the default branch)
  or any push whose commit message contains `[smoke]`. **A commit tagged `[smoke]` is a live
  network/CI action** (spins up a real Minecraft server under the EULA) — treat that tag as
  deliberate, not decorative, when writing commit messages here.
- **`src/test/java/frootloops/versus/mod/environment/worldgen/**`** — a real JUnit 5 test source
  set (`fabric-loader-junit`, `test { useJUnitPlatform(); systemProperty "fabric.side","server" }`
  in `build.gradle`; run with `./gradlew test`). `TestGame` bootstraps the game/registries before
  tests run (needed since these tests touch real registry-backed worldgen types).
  `VanillaOverridesTest`/`WorldgenDataTest`/`WorldgenTestData` check the mod's `data/minecraft/**`
  worldgen overrides against vanilla; `aquifer/LatticeTest` + `aquifer/AquiferLatticeTest` test the
  (currently-unused-in-production) `Lattice` class directly; `biome/PvBiomeLayoutTest` (plus
  `biome/OldBiomeLayout`, a frozen copy of the pre-refactor mixin logic kept as a regression
  oracle) checks the new layout against the old one's output. **Run `./gradlew test` before and
  after any biome/aquifer change** — this is the one subsystem in the repo with real automated
  regression coverage; use it.

## Open questions the plan doc has NOT resolved (Section 10 — check before assuming an answer)

1. Should the ~146 vanilla-namespace (`data/minecraft/worldgen/**` + tags) overrides that also
   silently change carver rates etc. in *vanilla* world types become PV-only, or is that
   cross-contamination accepted/intended? Not decided as of this writing.
2. Should "Improved" stay the pre-selected world type in Create World (current behavior, via
   `WorldPresetsMixin`/`WorldPresetServerDefaultMixin`), or should the mod stop overriding the
   default and let players opt in explicitly?
3. Do pre-refactor worlds (using the `players-versus:overworld` noise-settings key from before
   this rewrite) need to keep loading, or is breaking old PV worlds acceptable?

Don't answer these unilaterally in a refactor PR — they're explicitly flagged as undecided; surface
them to the maintainer instead.

## Extension recipes

**Tune an aquifer threshold:** edit the constant in `PvWorldgenConstants`, update the matching
literal in the density-function JSON it documents as duplicating it, then `./gradlew test` and
`/pvwg bench` before/after on the same seed.

**Add a new cave biome:** add a `PlacedBiome` entry to `CustomOverworldBiomes.caveBiomesToPlaceInOverorld`
(pick a `PlacedBiomeType`, climate-parameter boxes, rarity), register the biome's own `data/players-versus/worldgen/biome/caves/*.json`
+ `assets` (colors/mood sound/etc.) as usual, then check `/pvwg probe` and the bench's biome
histogram at a few y-slices.

**Add a new surface transition (mountainside/frozen/humid):** add an entry to the relevant map in
`CustomOverworldBiomes` (`MOUNTAIN_BIOME_REPLACEMENTS` etc.) — no code changes needed in
`PvBiomeLayout` itself unless you need a genuinely new *rule* (new climate axis condition), in
which case add a `Rule` to `PvBiomeLayout.RULES`, mindful that **earlier rules in the list win**
where regions overlap.

**Anything touching Phase 2/3/4 (aquifer lattice, Java density-function kernels, Java surface
rules):** re-read the plan doc's relevant section (6.2/6.5/6.6/6.7) in full first — it has verified
vanilla method signatures and worked-through formulas; guessing at vanilla `DensityFunctionTypes`
factory signatures instead of checking Appendix B is exactly how the reverted lattice attempt's
class of bug (steps inside a smooth-looking band) tends to happen.

## Non-refactor worldgen pieces (stable, not part of the PV-generator rewrite)

- **`mixin/environment/worldgen/structures/{DungeonsMixin,StructurePieceMixin}`** — unrelated to
  the aquifer/biome rewrite above; these tweak vanilla dungeon spawner/structure-piece placement
  and are unaffected by the PV-generator gate (they run for all world types). Verify current
  behavior by reading them directly before changing — not covered by the plan doc.
- **Custom features** — `mod/environment/worldgen/features/{MudPatchFeature,StoneStalagtiteFeature}`
  (+ their `*Config` records and `StoneStalagtiteHelper`), registered in `CustomWorldgen.onInitialize()`
  (`STONE_STALAGTITE_ID`/`MUD_PATCH_ID` under the mod namespace) alongside the call to
  `PvWorldgen.initialize()`. These are ordinary vanilla-style `Feature`s referenced from
  `data/*/worldgen/configured_feature/**` JSON like any other feature — **not** gated by
  `isPvGenerator`, so they can be (and are) placed in vanilla world types too if a
  `configured_feature`/`placed_feature` under `data/minecraft/**` references them.
- **`data/players-versus/worldgen/{biome,configured_feature,placed_feature,structure*,template_pool,processor_list}/**`**
  and their `data/minecraft/**` counterparts (vanilla-namespace overrides — see Open Question 1
  above for why these are contentious) are unchanged by this refactor and not re-verified in this
  pass; treat their contents as a separate, more static layer documented by ordinary JSON reading
  (structure NBTs, jigsaw pools, etc.) rather than by this file.
