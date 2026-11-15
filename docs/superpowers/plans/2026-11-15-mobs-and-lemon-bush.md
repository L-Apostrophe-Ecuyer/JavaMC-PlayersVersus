# Mobs and the lemon bush Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Watchlings now and then, carried shulkers gone with their enderman, eyes and carried blocks always whole, spawn rules as asked (creepers under a roof, undead on natural ground, spiders back, the pale kin below the pale line), and the lemon tree a two-block-tall bush.

**Architecture:** Spawn rules in `HostileEntityMixin`, `SpiderMixin` and `MobSpawning` (with the block tags they read); the pale kin by a mixin on the natural spawner's choice of mob; the shulker's despawn in `EndermanMixin`; the two fading mixins removed; the lemon bush a `DoublePlantBlock` with the lemon leaves' fruiting, its assets and data from `tools/lemons.py`.

**Tech Stack:** Java 25, Fabric (Minecraft 26.4-snapshot-4), JUnit 5, Fabric game tests, Python 3 generators.

**Spec:** `docs/superpowers/specs/2026-11-15-mobs-and-lemon-bush-design.md`

## Global Constraints

- Natural spawns only: spawn eggs, spawners and commands keep their current rules (`EntitySpawnReason.NATURAL` gates every change).
- Peaceful stays peaceful.
- Existing worlds keep their lemon leaves and lemon trees working; nothing new generates them.
- Known failures on the base, not ours: `BlightTest.theShakeBeginsBeforeItsFullAndPeaksWhenItIs`, `EndermanShulkerCarrierTest.twoToFourInAHundredCarryAShulker`, the blight game test; the shulker bullet game test is flaky under load.
- After a game run: `git checkout logs/latest.log`, `rm -f logs/*.log.gz`.

## Review Focus

- A spawn rule that now refuses a mob everywhere (the spiders' fate): each rule is run where the mob should spawn.
- The pale kin replacing a spawn above the pale line, or in the Nether or the End: the mapping is by height and dimension.
- A shulker discarded when its enderman is only unloaded or killed: only a despawn takes it.
- The lemon bush's halves coming apart (one half left when the other is broken, picked or grown).
- Feature order: the orchards' feature changes in the Tuscan grove only.

---

### Task 1: Spawn rules

**Files:** `HostileEntityMixin`, `SpiderMixin`, `MobSpawning`, block tags `undead_overworld_spawnable_on`, `creeper_spawnable_on`, `pale_mobs_spawnable_on` (one natural-ground list), a new mixin on `NaturalSpawner` for the pale kin, `PaleLine` (the kin by height); tests: new `SpawnRulesGameTest`, unit test for the pale kin.

- [ ] Failing tests: a creeper may spawn in a dark room under a roof, not where it sees the sky; a zombie and a skeleton on stone, not on planks (at midnight too); a spider in a dark room; below the pale line a zombie/creeper/spider spawn is its pale kin, above it and in other dimensions it isn't.
- [ ] Implement; run them, `*Spawn*`, the mob game tests. Commit.

### Task 2: Watchlings

**Files:** `MobSpawning` (weights, the pale tag, one within 128 blocks).

- [ ] Failing test: a watchling may spawn in total darkness below the pale line, not within 128 blocks of another. Implement. Commit.

### Task 3: A carried shulker despawns with its enderman

**Files:** `EndermanMixin`; `EndermanShulkerGameTest`.

- [ ] Failing test: an enderman carrying a shulker, despawned (no player within reach), takes the shulker; killed, it drops it as before. Implement. Commit.

### Task 4: Eyes and carried blocks whole

**Files:** remove `EyesLayerGazeMixin`, `CarriedBlockLayerGazeMixin` (and their registration), `GazeTranslucency` (docs, the carried block's threshold); the gallery gets a shot that runs a command (to summon an enderman holding a block at the edge of the view).

- [ ] The client compiles; the gallery's shot shows the eyes and the block whole by a see-through body. Commit.

### Task 5: The lemon bush

**Files:** new `LemonBushBlock` (a `DoublePlantBlock` with the lemon leaves' fruit stages), `CustomBlocks`, `CustomBlockItems`, `tools/lemons.py` (textures from vanilla's rose bush: leaves, blossoms, lemons; blockstates, models, item, loot, names, tags), the Tuscan grove's orchards (a patch of lemon bushes), the lemon sapling placing a bush; `LemonsGameTest`, `BiomeDataTest`.

- [ ] Failing tests: the bush is two blocks tall and stays whole (breaking either half takes both); it blossoms and fruits on random ticks and bone meal; picked, it gives 2 to 6 lemons and goes bare; the orchards place lemon bushes. Implement and generate. Commit.

### Task 6: In the world

- [ ] The suite and the game tests; a smoke run; the gallery (the enderman's shot, a lemon orchard), looked at; docs; a fresh final review; fixes; push.
