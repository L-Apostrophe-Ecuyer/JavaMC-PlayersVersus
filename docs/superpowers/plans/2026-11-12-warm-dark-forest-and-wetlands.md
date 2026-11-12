# The warm dark forest, denser forests, the Tuscan grove and the wetlands (Part 4) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Three new biomes (warm dark forest, grassy wetlands, mangrove marsh) placed by new layout rules, waterlogged leaves in the swamps' water, denser and more undergrown warm forests, a redone Tuscan grove, greener mangrove foliage.

**Architecture:** The biomes are written by the existing generators (`tools/transition-biomes.py` for the warm dark forest and the changed forests, `tools/wetlands.py` for the wetlands, the marsh configs and the waterlogged leaves), each made from its neighbours as the earlier transition biomes are. `PvBiomeLayout` gets three rules after the swamp rim. Data first (a biome no rule uses is harmless), then the layout, then the features that change existing biomes.

**Tech Stack:** Java 25, JUnit, the game tests (Fabric), Python generators, Minecraft 26.4 data JSON.

**Spec:** `docs/superpowers/specs/2026-11-12-warm-dark-forest-and-wetlands-design.md` (binding).

## Global Constraints

- Warm dark forest: dark forest slices at temperature 0.05 and up, after the humid band's rule. Grassy wetlands: swamp (not cold swamp) with humidity -0.15 and under, erosion above the rim 0.565. Mangrove marsh: mangrove swamp with erosion up to 0.565 or humidity -0.15 and under. The swamp rim (humid oak forest) is unchanged.
- Every new biome is in `is_overworld` and `has_structure/mineshaft`, named in `en_us` and `fr_fr` (as `lemons.add_names` writes), made from its neighbours (features in the same step order: `BiomeFeatureOrderTest` stays green); the mod's data only (`data/minecraft` overrides: `swamp.json`, `dark_forest.json` already; `mangrove_swamp.json` new, colours only, noted in the commit).
- Vanilla world types keep vanilla's layout (the layout is `PvBiomeSource`'s).
- Known failures on the base, not ours: `BlightTest`, `EndermanShulkerCarrierTest`, the blight game test.

## Review Focus

- A feature naming a biome or a placed feature that doesn't exist: the world fails to load (every biome is read at world creation): the data test and every smoke run check it.
- Feature order cycles between the new biomes and vanilla's: `BiomeFeatureOrderTest`.
- Leaves that decay, float in the air, fill a channel the marsh made, or stand on land: the game test.
- The vanilla `mangrove_swamp` override changing more than its colour.
- Rules overlapping or leaving holes: the partition test; each rule's probes.

---

### Task 1: The three new biomes' data

**Files:**
- Modify: `tools/transition-biomes.py` (warm dark forest; tags; names), `tools/wetlands.py` (grassy wetlands, mangrove marsh, their marsh configs), `CustomOverworldBiomes.java` (keys `WARM_DARK_FOREST`, `GRASSY_WETLANDS`, `MANGROVE_MARSH`)
- Create: the generated biome, feature and placed feature JSON, tag entries and language entries
- Test: `src/test/java/frootloops/versus/mod/environment/worldgen/biome/BiomeDataTest.java`

**Interfaces:**
- Produces: the biome keys above; biomes `players-versus:warm_dark_forest`, `grassy_wetlands`, `mangrove_marsh` and their trees (`trees_warm_dark_forest`, `trees_grassy_wetlands`, `trees_mangrove_marsh`) and marsh configs (`marsh_wetlands`, `marsh_mangrove`).

- [ ] **Step 1: Write the failing test** `BiomeDataTest` (reads `src/main/resources`, the game's jar for vanilla's): every biome file of the mod names features that exist (as files of the mod's or vanilla's data), a biome's step lists have no duplicates; the three new biomes exist, are in `is_overworld` and `has_structure/mineshaft`, and have names in `en_us.json` and `fr_fr.json`.
- [ ] **Step 2: Run it to see it fail** (no such biomes).
- [ ] **Step 3: Write the generators' entries and run them** (`python3 tools/transition-biomes.py <vanilla>`, `python3 tools/wetlands.py <vanilla>`); add the keys.
- [ ] **Step 4: Run `./gradlew test --tests '*BiomeDataTest' --tests '*BiomeFeatureOrderTest'`**: pass.
- [ ] **Step 5: Commit.**

### Task 2: The layout rules

**Files:**
- Modify: `PvBiomeLayout.java` (rules `warm-dark`, `grassy-wetlands`, `mangrove-marsh`)
- Test: `PvBiomeLayoutTest.java`

**Interfaces:**
- Consumes: the keys of Task 1.

- [ ] **Step 1: Write the failing tests:** `theDarkForestsWarmSideIsTheWarmDarkForest` (each dark forest slice: temperature 0.1 to 0.19 the warm dark forest, under 0.0 not, humid strip excepted), `theDrierWarmSwampIsGrassyWetlands` (swamp slices: humidity under -0.2 and erosion above 0.58 and temperature above -0.1: wetlands; humidity over 0: swamp; the rim: humid oak forest; the cold half: cold swamp), `theMangroveSwampsRimAndDrierPartAreMarsh`, and the existing `piecesPartitionEachSurfaceSlice`, `swampsHaveAHumidOakRim`, `theColdHalfOfSwampsIsColdSwamp` stay green.
- [ ] **Step 2: Run them to see them fail.**
- [ ] **Step 3: Implement the rules** after `swamp-rim` and `humid`.
- [ ] **Step 4: Run `./gradlew test --tests '*PvBiomeLayoutTest' --tests '*BiomeDataTest' --tests '*Rock*'`**: pass.
- [ ] **Step 5: Commit.**

### Task 3: Waterlogged leaves

**Files:**
- Modify: `tools/wetlands.py` (the feature, its placed feature; the swamp's, the cold swamp's and the wetlands' lists), the generated `data/minecraft/worldgen/biome/swamp.json`
- Test: `src/gametest/.../WetlandsGameTest.java`

- [ ] **Step 1: Write the failing game test** `waterloggedLeavesFloatAtTheWaterLine`: a pond (water three deep over mud, ringed by grass) in a test area; the placed feature run 40 times over it; every leaf block it made is `waterlogged` and `persistent`, its top at the water's surface, over water, and none stands on land or in the air; and at least 3 stand.
- [ ] **Step 2: Run it to see it fail** (no such feature).
- [ ] **Step 3: Write the feature** (`random_patch` of `simple_block`: oak leaves, `waterlogged`, `persistent`; placement: a rarity filter, the water's surface, a block predicate: water at the position and water or solid below, air above), add it to the biomes' vegetation after their trees.
- [ ] **Step 4: Run `./gradlew runGameTest -Ppv.acceptEula=true`** (only the known failure) and `BiomeDataTest`, `BiomeFeatureOrderTest`.
- [ ] **Step 5: Commit.**

### Task 4: Denser forests, the Tuscan grove, greener mangroves

**Files:**
- Modify: `tools/transition-biomes.py` (ratios and undergrowth for the warm oak, dark jungle and humid oak forests; the Tuscan grove: cypresses, colours, flowers), `tools/wetlands.py` (the mangrove swamp's override)
- Create: `players-versus:cypress` and its placed feature, `data/minecraft/worldgen/biome/mangrove_swamp.json`
- Test: `BiomeDataTest` (the cypress exists and is in the Tuscan grove's trees; the mangrove swamp's override changes only its foliage colour against vanilla's file)

- [ ] **Step 1: Baseline census** of the four biomes' trees (before), kept in the ledger.
- [ ] **Step 2: Write the failing tests** (the cypress; the mangrove override differs from vanilla's in the foliage colour only).
- [ ] **Step 3: Implement** and regenerate.
- [ ] **Step 4: Census again:** the denser biomes' tree counts up by a third or more; run `BiomeDataTest`, `BiomeFeatureOrderTest`.
- [ ] **Step 5: Commit.**

### Task 5: In the world

**Files:** `docs/worldgen-refactor-plan.md` (section 1.4, a section for part 4), the spec (as built), the ledger.

- [ ] **Step 1: The whole suite and the game tests** (only the known failures).
- [ ] **Step 2: Census** of the new biomes and of the swamps; the gallery's shot of each new and changed biome (`tools/gallery.sh`), looked at.
- [ ] **Step 3: Final review** by a fresh reviewer; fixes.
- [ ] **Step 4: Docs; commit; push.**
