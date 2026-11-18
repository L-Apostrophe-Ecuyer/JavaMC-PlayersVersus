# Rocks in the World (Part 3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Boulders, piles and crags appear in Players Versus worlds: sparsely on flat ground at middle temperatures, in rare clusters in meadows, and many, with the large formations, in three new rocky biomes.

**Architecture:** One pure rule says where rock may stand (`RockGround`: the climate, level ground). A placement filter (`players-versus:rock_ground`) and the formations' structure apply it in game. Placed features in the decoration step before trees put rocks in vanilla's and the mod's middle biomes (through Fabric's biome modifications) and in the rocky biomes (in their own JSON). New layout rules in `PvBiomeLayout` give the rocky biomes their climate.

**Tech Stack:** Java 25, Fabric API (`BiomeModifications`), Minecraft 26.4 data JSON written by `tools/rocks.py` and `tools/transition-biomes.py`.

**Spec:** `docs/superpowers/specs/2026-11-11-weathered-rocks-design.md`, section "Where rocks go" (its rules are binding); the climate boxes below are derived from vanilla's `OverworldBiomeBuilder` tables and recorded in that section by Task 3.

## Global Constraints

- Rock ground: temperature from -0.15 to 0.55; erosion above -0.3; not where humidity is 0 to 0.3 and erosion 0.3 to 0.6 (part 2's 3D-noise terrain); the ground level: the heights at the middle and at 8 points 4 and 8 points 8 blocks out within 2 of each other; no water at the middle.
- Only in the Players Versus world type (its biome source is `PvBiomeSource`); vanilla world types keep vanilla's worldgen.
- Rocks are placed in the `local_modifications` step (5), before trees: a tree placed first would be cut into the rock.
- Sparse rocks: a boulder (0.6), a pile (0.25) or a crag (0.15), in about one chunk of 10 tried. Meadows: a cluster of 2 to 4 in about one chunk of 48 tried.
- Rocky Plains: savannas' cool edge (temperature 0.2 to 0.3) where the erosion is 0.05 or more; the Tuscan Grove keeps the rest.
- Rocky Jungle: jungles (jungle, sparse, bamboo) at temperature 0.3 to 0.36, erosion 0.05 or more, out of the 3D-noise box.
- Rocky Birch Forest: old growth birch forests below temperature -0.075, erosion 0.05 to 0.3: birches and spruces.
- Formations only in the rocky biomes, and only on rock ground.
- Every new biome: in `is_overworld` and `has_structure/mineshaft`, named in `en_us` and `fr_fr`; `BiomeFeatureOrderTest` stays green.

## Review Focus

- A rock where the ground falls away just past the 8 blocks tested (a river bank, a cliff edge): the foot filling (at most 24 blocks) keeps it standing; `standsOnASlope` and `aFootIsNeverFilledMoreThanItsMost` cover the model, the census looks for rocks hanging over water.
- Two rocks of a cluster on top of each other: rock is stamped over rock; nothing floats (`nothingFloats` covers one body; Task 2's game test stamps two overlapping boulders).
- A rock feature at a chunk's corner reaching 16 blocks out: the feature stamps only within its 16 blocks (`RockFeature.MAX_REACH`), the region it may write.
- The vanilla world type: no rock anywhere (`runWorldgenSmoke` with `minecraft:normal`, the census counting none).
- A rocky biome's feature order against its neighbours': `BiomeFeatureOrderTest`.

---

### Task 1: Where rock may stand

**Files:**
- Create: `src/main/java/frootloops/versus/mod/environment/worldgen/rocks/RockGround.java` (pure), `.../rocks/RockGroundFilter.java`
- Modify: `CustomWorldgen.java` (register `players-versus:rock_ground` in `PLACEMENT_MODIFIER_TYPE`), `RockFormationStructure.java` (`findGenerationPoint` keeps only rock ground at the main body's anchor)
- Test: `src/test/java/.../rocks/RockGroundTest.java`; `RocksGameTest.java` (`theRockGroundIsLevelAndDry`)

**Interfaces:**
- Produces: `RockGround.climate(float temperature, float humidity, float erosion)`, `RockGround.level(int[] heights)`, `RockGround.AROUND` (the 17 offsets: the middle, 8 at 4 blocks, 8 at 8 blocks), `RockGround.LEVEL = 2`; `RockGroundFilter` (a `PlacementFilter`, no settings) with `boolean ground(WorldGenLevel level, int x, int z)` (level and dry) used by `shouldPlace` and by the game test.

- [ ] **Step 1: Write the failing tests**

```java
@Test void rocksStandAtMiddleTemperatures()     // climate(0.1, 0, 0) and (-0.1, 0, 0) and (0.5, 0, 0); not (-0.2, 0, 0) nor (0.6, 0, 0)
@Test void notOnMountainsides()                  // climate(0.1, 0, -0.25); not (0.1, 0, -0.35)
@Test void notInTheThreeDimensionalNoise()       // not climate(0.1, 0.1, 0.4); climate(0.1, -0.1, 0.4) and (0.1, 0.35, 0.4) and (0.1, 0.1, 0.25) and (0.1, 0.1, 0.65)
@Test void levelGroundOnly()                     // level(17 heights of 64) true; one 66 true; one 67 false; one 61 false
// Game test, skyAccess: a 21 by 21 grass plain is rock ground; a block of water at the middle is not; a 3-block step 6 out is not
@GameTest void theRockGroundIsLevelAndDry()
```

- [ ] **Step 2: Run them to see them fail**: `./gradlew test --tests '*RockGroundTest*'` (compilation fails: no `RockGround`).
- [ ] **Step 3: Implement** `RockGround`, `RockGroundFilter` (`shouldPlace`: the biome source is `PvBiomeSource`, the climate at the middle from the server's `RandomState` sampler, `ground`), its registration, and the formations' check (the climate from the context's sampler; heights from `getFirstOccupiedHeight(OCEAN_FLOOR_WG)` at the 17 points).
- [ ] **Step 4: Run** `.superpowers/sdd/2026-11-11-rocks-in-the-world/rocktests.sh` and the game tests; all pass (the blight test is the known failure).
- [ ] **Step 5: Commit** ("Rocks: where rock may stand: middle climates, level ground")

### Task 2: Sparse rocks and meadow clusters

**Files:**
- Modify: `tools/rocks.py` (writes `rock/sparse` and `rock/meadow_cluster`, their configured selectors), `CustomWorldgen.java` (adds them to the biomes below at `LOCAL_MODIFICATIONS`), `debug/WorldgenCensus.java` (counts rock features placed, by biome), `RocksGameTest.java`
- Biomes for sparse rocks: plains, sunflower plains, forest, flower forest, birch forest, dark forest, savanna, savanna plateau, jungle, sparse jungle, bamboo jungle; the mod's tuscan grove, warm oak forest, dark jungle forest, sparse dappled forest, dark birch forest. Meadow clusters: meadow.

- [ ] **Step 1: Write the failing game test** `twoBouldersStampedOverEachOtherStandTogether` (two boulder plans 4 blocks apart: every rock block joined face to face to rock at the ground).
- [ ] **Step 2: Implement** the data and the biome modification; the census line `[pvwg] census rocks <biome>: <n> in <chunks> chunks`.
- [ ] **Step 3: Verify**: `runWorldgenSmoke` for `players-versus:better_world` with the census of plains, savanna, forest, jungle, desert and snowy plains: rocks in the first four, none in the last two; `minecraft:normal`: none.
- [ ] **Step 4: Commit** ("Rocks: sparse on level ground in the middle biomes, clusters in meadows")

### Task 3: The rocky biomes

**Files:**
- Modify: `CustomOverworldBiomes.java` (`ROCKY_PLAINS`, `ROCKY_JUNGLE`, `ROCKY_BIRCH_FOREST`), `PvBiomeLayout.java` (rules `rocky-plains`, `rocky-jungle`, `rocky-birch`, before `warm` and `weird-cold`), `tools/transition-biomes.py` (the three biomes, their trees and rock features, tags, names), `tools/rocks.py` (the formations' biome tag: the rocky biomes), the spec's "Where rocks go" (the boxes)
- Test: `PvBiomeLayoutTest` (a test per biome: inside its box it is the biome; just outside, what it was), `warmBandTakesTheCoolEdgeOfWarmBiomes` (the Tuscan grove below erosion 0.05 only)

- [ ] **Step 1: Write the failing layout tests** `rockyPlainsTakeTheLevelCoolEdgeOfSavannas`, `rockyJungleLiesBetweenTheWarmBandAndTheJungle`, `rockyBirchForestTakesTheColdLevelOldGrowthBirch`.
- [ ] **Step 2: Run them to see them fail.**
- [ ] **Step 3: Implement** the keys, the rules and the biome data:
  - Rocky Plains: the plains' and the savanna's grass between (temperature 1.2, downfall 0.2), oaks and a few acacias by noise, tall grass, flowers; boulders 2 a chunk, a pile or a crag in 1 of 3.
  - Rocky Jungle: the jungle's (jungle trees and bushes, sparser; melons, vines); a boulder or a crag a chunk, piles.
  - Rocky Birch Forest: birches and spruces, ferns, moss; a boulder a chunk, crags.
- [ ] **Step 4: Run** the unit tests, the game tests and the census of the three biomes (each found, rocks counted, formations found); gallery shots of each.
- [ ] **Step 5: Commit** ("Rocks: the rocky plains, the rocky jungle and the rocky birch forest") and push; the user sees the shots.
