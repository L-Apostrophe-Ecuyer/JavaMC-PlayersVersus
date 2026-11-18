# Sulfur pools and spikes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every sulfur pool has many potent sulfur geysers (over magma) on its floor; sulfur spikes poison and nauseate what they hurt, for times drawn up to the damage dealt.

**Architecture:** A feature record `PoolGeysersFeature` (`players-versus:pool_geysers`, registered in `CustomWorldgen`) as the second step of vanilla's `sulfur_pool`, overridden in `data/minecraft/worldgen/feature/`. `SulfurSpikes` (the durations, the afflicting, a Fabric `AFTER_DAMAGE` listener for falling spikes) and `SulfurSpikeBlockMixin` (landing on a spike).

**Tech Stack:** Java 25, Fabric (Minecraft 26.4-snapshot-4), JUnit 5, Fabric game tests.

**Spec:** `docs/superpowers/specs/2026-11-16-sulfur-pools-and-spikes-design.md`

## Global Constraints

- After a game run: `git checkout logs/latest.log`, `rm -f logs/*.log.gz`.
- Known failures on the base, not ours: `BlightTest.theShakeBeginsBeforeItsFullAndPeaksWhenItIs`, `EndermanShulkerCarrierTest.twoToFourInAHundredCarryAShulker`, the blight game test; the shulker bullet game test is flaky under load.

## Review Focus

- A geyser whose magma replaced something it shouldn't (a block entity, bedrock, a cave's ceiling left hanging).
- Potent sulfur placed where it can't be a geyser (no water source over it, more than four blocks of water).
- A pool that gets none (a floor the scan misses: the lake's box, its offset).
- Effects on the client, on a dead entity, or for damage a spike didn't deal (another fall, a falling block that isn't a spike).
- Fractional damage after armour, and damage under 1 or under 5.

---

### Task 1: Geysers on the pool floor

**Files:** new `PoolGeysersFeature` (fields `vent`, `heat` block state providers, `share`, `minimum`, `can_replace` block predicate), `CustomWorldgen` (registration), `data/minecraft/worldgen/feature/sulfur_pool.json` (vanilla's lake, then `players-versus:pool_geysers`: potent sulfur `dormant`, magma, 1/3, 6, not `#lava_pool_stone_cannot_replace`); new `SulfurGameTest`.

- [ ] Failing test `SulfurGameTest.everySulfurPoolHasManyGeysersOnItsFloor`: over 8 seeds, grow `minecraft:sulfur_pool` on a slab of sulfur; its floor (solid under its water) has at least min(6, floor) and at least a third of it in potent sulfur, each over a magma block and not dry. Implement, run `*:sulfur_game_test*`. Commit.

### Task 2: Sulfur spikes poison and nauseate

**Files:** new `SulfurSpikes` (`poisonTicks(float damage, RandomSource)`, `nauseaTicks(...)`, `afflict(LivingEntity, float)`, `register()` for `AFTER_DAMAGE`), `VersusSettings.SulfurSpikes`, new `SulfurSpikeBlockMixin` (fallOn), `players-versus.mixins.json`, `VersusMod` (register); new `SulfurSpikesTest`; `SulfurGameTest`.

- [ ] Failing unit test `SulfurSpikesTest`: poison 20..20*floor(d) ticks in whole seconds (20 for d < 2), nausea 100..20*max(5, floor(d)); both ends reached over many draws.
- [ ] Failing game tests: a falling sulfur spike hurts a pig and leaves it poisoned and nauseous within the damage it took; a pig dropped onto a sulfur spike the same; a pig dropped onto grass from as high, neither. Implement, run. Commit.

### Task 3: In the world

- [ ] Suite, game tests, the gallery (a sulfur pool), docs, a fresh final review, fixes, push.
