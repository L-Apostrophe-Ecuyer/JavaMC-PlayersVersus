# The cypress, the lemon bush's look, and lemons Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Lemons as asked (1 hunger, no saturation, 40 ticks, Sour/Slowness 4 s, Haste I 24 s); cypress leaves; a pear-shaped, fuzzy cypress of spruce and cypress leaves; a lemon bush that looks like a tiny tree.

**Architecture:** `Fruits.LEMON`'s food and consume effects from `VersusSettings.Lemons`; a cypress leaves block (`CustomBlocks`, `LeafReach`, assets and data from `tools/lemons.py`'s sibling generator `tools/cypress.py`); a foliage placer type of the mod's (`CypressFoliagePlacer`) used by `tools/transition-biomes.py`'s cypress; the lemon bush's models and textures from `tools/lemons.py`.

**Tech Stack:** Java 25, Fabric (Minecraft 26.4-snapshot-4), JUnit 5, Fabric game tests, Python 3 generators (Pillow).

**Spec:** `docs/superpowers/specs/2026-11-15-cypress-lemon-look-and-lemons-design.md`

## Global Constraints

- Existing worlds keep their cypresses (spruce leaves stay spruce leaves) and lemon bushes (the block and its states don't change).
- After a game run: `git checkout logs/latest.log`, `rm -f logs/*.log.gz`.
- Known failures on the base, not ours: `BlightTest.theShakeBeginsBeforeItsFullAndPeaksWhenItIs`, `EndermanShulkerCarrierTest.twoToFourInAHundredCarryAShulker`, the blight game test; the shulker bullet game test is flaky under load.

## Review Focus

- Cypress leaves that decay on a cypress (their own log's tag) or never decay.
- A cypress crown that leaves holes in itself or floats off its trunk (the fuzz).
- The crown's leaves and the tree's decay distances (leaves the tree feature sets as it grows).
- The lemon bush's crown z-fighting with its fruit overlay, or its trunk drawn through the crown.
- The Sour view's curve at the new length.

---

### Task 1: Lemons as food

**Files:** `Fruits`, `VersusSettings.Lemons`; `LemonsGameTest`.

- [ ] Failing test: eating a lemon takes 40 ticks, fills 1 hunger and 0 saturation, and gives Sour 80 ticks, Slowness I 80 ticks, Haste I 480 ticks. Implement. Run `*:lemons_game_test*`, `SourFovTest`. Commit.

### Task 2: Cypress leaves

**Files:** `CustomBlocks`, `CustomBlockItems`, `RegisteringCustomItems`, `LeafReach`; new `tools/cypress.py` (texture: spruce leaves' in azalea leaves' colours; blockstate, models, item, loot as spruce leaves', tags, names); `TuscanGroveGameTest`.

- [ ] Failing test: cypress leaves live by spruce logs (not oak), are leaves, drop as spruce leaves. Implement, generate. Commit.

### Task 3: The cypress's crown

**Files:** new `CypressFoliagePlacer` (registered with the mod's worldgen types), `tools/transition-biomes.py` (the cypress's feature); `TuscanGroveGameTest`.

- [ ] Failing test: over several seeds, the grown cypress is wider in its lower half than its upper, has a spire of single leaves on top, nothing beyond two blocks from the trunk, both leaves, cypress leaves more common in its upper half. Implement, generate. Commit.

### Task 4: The lemon bush's look

**Files:** `tools/lemons.py` (crown textures and models, trunk model, blockstate, item icon); `BiomeDataTest` or a new data test.

- [ ] Failing test: the bush's blockstate maps every lower state to the trunk and each upper stage to its crown; the crown models are boxes with the stage's overlay. Implement, generate. Client compiles. Commit.

### Task 5: In the world

- [ ] Suite, game tests, the gallery (a cypress and lemon bushes up close; the Tuscan grove), docs, a fresh final review, fixes, push.

### Task 6: Remove the lemon trees (asked during execution)

**Files:** delete `LemonLeavesBlock` (its stages move to `LemonBushBlock`); `CustomBlocks`, `CustomBlockItems`, `RegisteringCustomItems`, `LeafReach`, `CustomBlocksClient`, `VersusSettings`; `tools/lemons.py` (no leaves, no tree; the overlays the bush's own); the lemon leaves' and the tree's assets and data, their tag entries and names; `LemonsGameTest`, `TreeGrowthGameTest`, `BiomeDataTest`, `LemonBushModelsTest`.

- [ ] Failing test: nothing of the lemon trees is left in the assets or data; the crowns use the bush's own overlays. Remove, regenerate. Run the lemon and tree growth game tests. Commit.

