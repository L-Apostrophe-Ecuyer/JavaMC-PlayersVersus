# The spawn check's sky light, and the tags Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The monster spawn check reads the sky light once; the tags are pruned, correct and kept so by a test.

**Architecture:** `MobSpawning.darkEnoughToSpawn` (vanilla's darkness check from one read) used by `HostileEntityMixin`; a shared generator helper `tools/tagfiles.py` (add, own, prune by resolving vanilla's and the mod's tags); a one-time prune; `TagAuditTest`.

**Tech Stack:** Java 25, Fabric (Minecraft 26.4-snapshot-4), JUnit 5, Fabric game tests, Python 3.

**Spec:** `docs/superpowers/specs/2026-11-18-spawn-light-and-tags-design.md`

## Global Constraints

- After a game run: `git checkout logs/latest.log`, `rm -f logs/*.log.gz`.
- Known failures on the base: `BlightTest.theShakeBeginsBeforeItsFullAndPeaksWhenItIs`, `EndermanShulkerCarrierTest.twoToFourInAHundredCarryAShulker`, the blight game test.

## Review Focus

- A darkness check that differs from vanilla's in thunder, at the world border, or in a dimension without sky light.
- A pruned entry that changed what a tag resolves to (a nested tag that holds it only in vanilla, or only with the mod's additions).
- A generator rerun that puts a pruned or stale entry back.
- Natural ground taking in something built (bricks, polished stone) or something a mob shouldn't stand on.

---

### Task 1: The sky light once

**Files:** `MobSpawning` (`darkEnoughToSpawn(ServerLevelAccessor, BlockPos, RandomSource, int skyLight)`, `overworldRules` without `seesSky`), `HostileEntityMixin`; `MobSpawningTest`; `SpawnRulesGameTest`.

- [ ] Failing game test `theDarknessCheckIsVanillas`: open sky, under a roof, beside a torch, at a roof's edge; 200 seeds each; `darkEnoughToSpawn(level, pos, random(seed), sky)` equals `Monster.isDarkEnoughToSpawn(level, pos, random(seed))`. Implement, refactor the rules and the mixin. Run `MobSpawningTest`, `*:spawn_rules_game_test*`. Commit.

### Task 2: The tags

**Files:** new `tools/tagfiles.py`; `tools/lemons.py`, `tools/cypress.py`, `tools/transition-biomes.py`, `tools/wetlands.py`, `tools/refine-biomes.py`, `tools/animals.py`, `tools/tree-blocks.py`; the tag files; `data/minecraft/wolf_variant/rusty.json` (deleted); the lang files; new `TagAuditTest`.

- [ ] Failing `TagAuditTest` (duplicates, dangling mod ids, re-listed and nested-covered entries, variant tags vs `tools/animals.py`, natural ground, item tag names). Helper, generators, prune, natural ground, names. Resolved contents before and after compared. Run the suite. Commit.

### Task 3: In the world

- [ ] Suite, game tests, docs, a fresh final review, fixes, push.
