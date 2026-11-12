# The 3D-noise terrain's place (Part 2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The shattered, overhanging terrain (vanilla's bump in the terrain splines at erosion 0.40 to 0.58) exists only in a zone (humidity 0 to 0.3, erosion 0.3 to 0.6, inland), capped at Beta's heights (nothing over y 128), and the windswept biomes stand in that zone where their temperature and humidity fit; elsewhere the terrain at those erosions is plain.

**Architecture:** One smooth window of the climate (`players-versus:overworld/noise_zone`) mixes two forms of vanilla's offset and factor splines (`plain`: without the bump's points; `shattered`: the bump's plateau) into the mod's own `offset` and `factor`, which `depth`, `sloped_cheese` and the surface-level estimates use. In the zone the base noise fades out between y 100 and 128. `PvBiomeLayout` keeps vanilla's windswept slices only inside the zone's box. The numbers live in `PvWorldgenConstants`, read by the density-function generator and the Java side.

**Tech Stack:** Java 25, JUnit with the game's own density evaluation (`WorldgenTestData`), Python generators (`tools/port-26.3/pv_density.py`, new `tools/terrain.py`), Minecraft 26.4 data JSON.

**Spec:** `docs/superpowers/specs/2026-11-12-noise-terrain-zone-design.md` (binding).

## Global Constraints

- The zone: humidity 0 to 0.3 (fade 0.05), erosion 0.3 to 0.6 (fade 0.06), continentalness from 0.03 (fade 0.09). Smoothsteps, product of the five edges.
- Vanilla's `offset`, `factor` and every `minecraft:` data file stay as they are: vanilla world types are untouched. Only `players-versus:overworld/*` density functions change.
- Outside the window and outside erosion 0.34 to 0.63 the mod's offset and factor equal vanilla's.
- Heights: in the zone the base noise is `noise * (1 - zone * ramp)`, the ramp 0 at y 100 and 1 at y 128, clamped; the zone's tops: median y 65 to 90 with a spread of 25 blocks or more, none over 128.
- Windswept: only where humidity is 0.025 to 0.275, continentalness 0.03 or more, temperature below 0; hills / gravelly hills / forest by the spec's table; elsewhere what lies beside; the temperate rule (`TEMPERATE_MIN` 0) stays.
- `BiomeFeatureOrderTest`, `PvBiomeLayoutTest` (pieces partition vanilla's slices) and the rock tests stay green. Known failures on the base, not ours: `BlightTest`, `EndermanShulkerCarrierTest`, the blight game test.

## Review Focus

- The border of the zone (the window's fades): terrain steps or cliffs along a contour; the census and the gallery's edge shot look at it; the tests check the window is continuous and monotone across a fade.
- Coasts and rivers inside the zone: the continentalness fade keeps the zone off the shore; the mod's high river and river carver terms are untouched (`sloped_cheese` keeps its `min` with them).
- The aquifer and surface rules reading the surface-level estimate: it follows the new offset and factor (Task 2), so water and surface blocks stand where the ground is.
- Vanilla's data changing under the copies (a snapshot moving its splines): `theTerrainOutsideTheBumpIsVanillas` fails when the copies drift from vanilla.
- Old worlds: a world keeps its generator and its data from when it was made; chunks already generated don't change; new chunks next to old ones meet at the zone's old edge (the window is smooth, the old terrain isn't): noted, not fixed.

---

### Task 1: The zone's numbers and its window

**Files:**
- Modify: `src/main/java/frootloops/versus/mod/environment/worldgen/PvWorldgenConstants.java`, `.../rocks/RockGround.java`, `.../biome/PvBiomeLayout.java`, `tools/port-26.3/pv_density.py`
- Create: `src/main/resources/data/players-versus/worldgen/density_function/overworld/noise_zone.json` (written by the generator)
- Test: `src/test/java/frootloops/versus/mod/environment/worldgen/terrain/NoiseZoneTest.java` (uses `WorldgenTestData`, already in the tree)

**Interfaces:**
- Produces: `PvWorldgenConstants.NOISE_ZONE_HUMIDITY_MIN/MAX`, `NOISE_ZONE_EROSION_MIN/MAX`, `NOISE_ZONE_CONTINENTALNESS_MIN` (doubles, 0.0 / 0.3 / 0.3 / 0.6 / 0.03) and `NOISE_ZONE_HUMIDITY_FADE`, `NOISE_ZONE_EROSION_FADE`, `NOISE_ZONE_CONTINENTALNESS_FADE` (0.05 / 0.06 / 0.09), `NOISE_CAP_FROM_Y`, `NOISE_CAP_TO_Y` (100.0 / 128.0); the density function `players-versus:overworld/noise_zone` (2D, 0 to 1); `PvBiomeLayout` and `RockGround` read the box from the constants (their own constants removed).

- [ ] **Step 1: Write the failing test** `NoiseZoneTest`: over 40,000 random positions within 30,000 blocks, with the router's humidity, erosion and continentalness (`router().vegetation()`, `erosion()`, `continents()`) and the registry's `players-versus:overworld/noise_zone`, evaluated with `RandomState#sampleBlockValueUncached(function, x, 0, z)`:
  - `theWindowIsZeroOutsideTheBox`: 0 where humidity < 0 or >= 0.3, erosion < 0.3 or >= 0.6, or continentalness < 0.03; between 0 and 1 everywhere.
  - `theWindowIsOneWellInside`: 1 (within 1e-5) where humidity is 0.05 to 0.25, erosion 0.36 to 0.54 and continentalness 0.12 or more.
  - `theWindowMatchesItsFormula`: within 1e-3 of a reference computed in the test from the constants (product of `smoothstep` edges), at every sample, with at least 100 samples in the fades (0 < window < 1) and 100 inside (window 1).
- [ ] **Step 2: Run it to see it fail** (`./gradlew test --tests '*NoiseZoneTest'`): the function `players-versus:overworld/noise_zone` doesn't exist.
- [ ] **Step 3: Add the constants to `PvWorldgenConstants` (plain `double`/`int` literals, so the generator reads them); in `pv_density.py` add `noise_zone()` (a `cache` of the product of five smoothsteps `t * t * (3 - 2 * t)`, `t` the clamped, scaled distance into a fade, on `minecraft:overworld/vegetation`, `erosion`, `continents`) to `FUNCTIONS` as `overworld/noise_zone`; run `python3 tools/port-26.3/pv_density.py`; point `RockGround` and `PvBiomeLayout`'s zone constants at `PvWorldgenConstants`.
- [ ] **Step 4: Run `./gradlew test --tests '*NoiseZoneTest' --tests '*RockGroundTest' --tests '*PvBiomeLayoutTest'`**: pass.
- [ ] **Step 5: Commit** ("Terrain: the 3D-noise zone's window and its numbers in one place").

### Task 2: Terrain shapes: the bump moves into the zone

**Files:**
- Create: `tools/terrain.py` (reads vanilla's `offset.json` and `factor.json` from the game's jar in Gradle's cache, or from a folder given as an argument; writes `overworld/offset.json` and `overworld/factor.json`), the generated JSON, `overworld/preliminary_surface_level.json`, `overworld/chunk_surface_level.json` (from `pv_density.py`)
- Modify: `tools/port-26.3/pv_density.py` (`depth` uses `pv("overworld/offset")`, `sloped_cheese` `pv("overworld/factor")`; the surface-level functions; `noise_settings` points `aquifers.surface_level` and `noise_router.chunk_surface_level` at them; `GONE` loses the preliminary surface level)
- Test: `src/test/java/.../terrain/TerrainShapeTest.java`

**Interfaces:**
- Consumes: `players-versus:overworld/noise_zone` (Task 1).
- Produces: `players-versus:overworld/offset` and `factor` (cache of vanilla's wrapper around `lerp(zone, plain, shattered)`), `players-versus:overworld/preliminary_surface_level`, `chunk_surface_level`.

- [ ] **Step 1: Write the failing tests** `TerrainShapeTest`, positions as in Task 1 (inland, continentalness 0.3 or more where the test says so), the mod's functions and vanilla's `minecraft:overworld/offset` and `factor` from the registry:
  - `theTerrainOutsideTheBumpIsVanillas`: where the window is 0 and erosion < 0.3 or > 0.63 (any continentalness): the mod's offset and factor equal vanilla's within 1e-4, at 3,000 or more samples.
  - `theBumpIsGoneOutsideTheZone`: where the window is 0, continentalness 0.3 or more and erosion 0.47 to 0.53 (200 or more samples): factor 4 or more (vanilla's bump: 1.4 to 1.8 off the valleys) and offset under -0.4 (vanilla's -0.33); and the mean over them is within 0.1 of the mean at erosion 0.62 to 0.66.
  - `theBumpHoldsInsideTheZone`: where the window is 1 and continentalness 0.3 or more: at erosion 0.47 to 0.53 the mod's equal vanilla's (the plateau); at erosion 0.37 to 0.43 the offset averages over -0.4 (vanilla's plain -0.5) and factor under 3.
  - `theTransitionIsSmooth`: along a line of 400 positions across a fade (found by walking in x from a position inside the zone out), consecutive offsets differ by under 0.03 and factors by under 0.5.
- [ ] **Step 2: Run them to see them fail**: no mod offset or factor.
- [ ] **Step 3: Write `tools/terrain.py`**: `plain(spline)` removes the points at erosion 0.45 and 0.55 from every erosion node; `shattered(spline)` replaces each erosion node that has them by its 0.45 point's sub-tree (equal to the 0.55's, asserted) and each other erosion node's coordinate by the constant 0.5; both keep vanilla's other points and derivatives; the files wrap them as vanilla's do (`cache(lerp(blend_alpha, blend_offset | 10.0, ...))`, the offset adding -0.50375). In `pv_density.py`: the references, `preliminary_surface_level` (vanilla's `find_top_surface` tree with the mod's offset and factor) and `chunk_surface_level` (its `interpolated`, 16 by 1), `noise_settings`, `GONE`. Run both generators.
- [ ] **Step 4: Run `./gradlew test --tests '*TerrainShapeTest' --tests '*NoiseZoneTest'`**: pass; `python3 tools/port-26.3/pv_density.py --check` reports only the three files stale before this plan (`material_rule/overworld.json`, `noise/sand_beach.json`, `noise/gravel_beach.json`).
- [ ] **Step 5: Commit** ("Terrain: the shattered terrain lives in the zone, plain elsewhere").

### Task 3: Beta-style heights

**Files:**
- Modify: `tools/port-26.3/pv_density.py` (`sloped_cheese`), the generated `overworld/sloped_cheese.json`
- Test: `src/test/java/.../terrain/TerrainCapTest.java`

**Interfaces:**
- Consumes: `noise_zone` (Task 1), `offset` and `factor` (Task 2), `NOISE_CAP_FROM_Y`, `NOISE_CAP_TO_Y`.

- [ ] **Step 1: Write the failing tests** `TerrainCapTest`, over 3,000 columns where the window is 1 and continentalness 0.3 or more:
  - `nothingStandsOverTheCapInTheZone`: `final_density` is negative at every y in 129 to 255 (step 6), and the highest y with positive density (scanning down from 255 in steps of 2) is 128 or less.
  - `theZonesTopsAreBetaLike`: the tops' median is 65 to 90, their spread (tenth to ninetieth percentile) 25 blocks or more, the 90th percentile 115 or less and none over 128.
  - `theCapLeavesOtherTerrainAlone`: where the window is 0 (1,000 columns), the tops' distribution equals what the same columns give with the sloped cheese's reference formula (`4 * quarter_negative((depth + jaggedness * half_negative(jagged)) * factor) + base_3d_noise`, composed in the test from the registry's functions), column by column, where `|ridges| >= 0.22` (off the rivers' carver).
- [ ] **Step 2: Run them to see the first two fail** (the zone's terrain, with no cap yet, has ground over 128).
- [ ] **Step 3: In `sloped_cheese`, the base noise becomes `mul(base_3d_noise, sub(1.0, mul(noise_zone, gradient(y, 100 -> 128, 0 -> 1))))`**; run the generator; note the zone's heights; if the median is out of range, the shattered form's offset gets a ruling (a lift or a drop), recorded in the ledger.
- [ ] **Step 4: Run `./gradlew test --tests '*Terrain*' --tests '*NoiseZoneTest'`**: pass.
- [ ] **Step 5: Commit** ("Terrain: nothing of the 3D noise's ground over y 128 in the zone").

### Task 4: Windswept biomes by climate

**Files:**
- Modify: `src/main/java/frootloops/versus/mod/environment/worldgen/biome/PvBiomeLayout.java` (a replacement rule `windswept`, before `temperate`)
- Test: `src/test/java/.../biome/PvBiomeLayoutTest.java`

**Interfaces:**
- Consumes: the zone's constants (Task 1).
- Produces: `PvBiomeLayout` with the replacement `windswept`: the part of a windswept slice (hills, gravelly hills, forest) inside the box (humidity `NOISE_ZONE_HUMIDITY_MIN + fade/2` to `MAX - fade/2`, continentalness from `NOISE_ZONE_CONTINENTALNESS_MIN`, temperature under `TEMPERATE_MIN`) is the biome of the spec's table; the rest is what lies beside it (the spec's list).

- [ ] **Step 1: Write the failing tests** (random points through `layout.findValue`, as `noWindsweptNorOldGrowthBirchAboveFreezing` does):
  - `windsweptBiomesStandOnlyInTheZone`: over 400,000 points, every windswept biome found (off the mountainsides) has humidity 0.025 to 0.275, continentalness 0.03 or more, temperature below 0 and erosion 0.45 to 0.55 (the slice's own).
  - `theZoneHasTheRightWindsweptBiome`: inside that box, the table: temperature under -0.15: gravelly hills at humidity under 0.1, forest from it; -0.15 to 0: hills under 0.1, forest from it; and 1,000 or more points found.
  - `windsweptSlicesOutsideTheZoneBecomeTheirNeighbours`: at erosion 0.5, temperature -0.6 / -0.3 / -0.1, humidity -0.5 / 0 / 0.2 / 0.5, continentalness 0.5, weirdness -0.9: the biome is the one the spec lists (snowy plains, snowy taiga, taiga, plains, forest, old growth spruce taiga), none windswept.
  - the existing `noWindsweptNorOldGrowthBirchAboveFreezing` and `piecesPartitionEachSurfaceSlice` stay.
- [ ] **Step 2: Run them to see them fail.**
- [ ] **Step 3: Implement the `windswept` replacement** (region: the slice's temperature under `TEMPERATE_MIN`; the target function cuts the piece by the box through a second replacement for the inside).
- [ ] **Step 4: Run `./gradlew test --tests '*PvBiomeLayoutTest' --tests '*BiomeFeatureOrderTest' --tests '*Rock*'`**: pass.
- [ ] **Step 5: Commit** ("Biomes: the windswept biomes stand in the 3D-noise zone, where they fit").

### Task 5: In the world

**Files:**
- Modify: `docs/worldgen-refactor-plan.md` (section 1.4's windswept rule, a section for the zone), `docs/superpowers/specs/2026-11-12-noise-terrain-zone-design.md` (as built)
- Test: `src/gametest/.../RocksGameTest.java` unchanged; a census of the windswept biomes

- [ ] **Step 1: Run the whole suite** (`./gradlew test`) and the game tests (`rm -f build/run/gameTest/report.xml && ./gradlew runGameTest -Ppv.acceptEula=true`): only the known failures.
- [ ] **Step 2: Census** (`./gradlew runWorldgenSmoke -Ppv.acceptEula=true -Ppv.census=minecraft:windswept_hills,minecraft:windswept_forest,minecraft:windswept_gravelly_hills`): the biomes exist, the zone's tops by the census's ground heights.
- [ ] **Step 3: Gallery** (`tools/gallery.sh`): the windswept biomes and an `at:` shot at the zone's edge, beside the earlier shots in the scratchpad.
- [ ] **Step 4: Update the docs; run `python3 tools/port-26.3/pv_density.py --check`.**
- [ ] **Step 5: Commit and push.**
