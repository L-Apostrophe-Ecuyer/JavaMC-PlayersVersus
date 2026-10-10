---
name: players-versus
description: Total-recall expertise over the Players Versus Minecraft Fabric mod (frootloops.versus) — every mixin, every custom system, the design vision, build/verify commands, and known gotchas. Use for ANY task touching this repository: understanding, maintaining, refactoring, extending, porting, or debugging any part of the mod.
---

# Players Versus — Mod Expertise

Fabric mod (mod id `players-versus`, Java package `frootloops.versus`) for Minecraft
**1.21.10** (Yarn `1.21.10+build.2`, Fabric Loader `0.17.3`, Fabric API `0.136.0+1.21.10`,
Loom `1.11-SNAPSHOT`, Java 21). Streamlines and deepens vanilla systems rather than bolting on new
content — tagline: *"Improves the challenge; less frustration, more fear, more fun."* Full vision
and feature list: [`README.md`](../../../README.md); live wishlist/scratchpad:
[`src/main/java/frootloops/versus/Ideas for Features.txt`](../../../src/main/java/frootloops/versus/Ideas%20for%20Features.txt).

**This repo changes actively and fast** — before trusting anything below about a *specific* file's
current behavior, prefer reading that file over trusting a stale summary. This skill was last
verified against commit `3f04892d` on branch `26.3---Wilderness-Bound` (branched from `1.21.9`,
19 commits ahead, currently mid worldgen-refactor — see `references/worldgen-engine.md`).

## How to use this skill
Load `references/architecture.md` first for any nontrivial task — it covers the module layout,
boot sequence, `VersusSettings` (the central tuning namespace), mixin registration mechanics, build
commands, and the `@Overwrite` version-bump risk pattern that recurs everywhere in this codebase.
Then load whichever subsystem reference(s) below match the task. Each reference doc is self-
contained with a component inventory, an extension recipe, and flagged known issues; none of them
require re-reading the others first.

| Reference | Covers |
|---|---|
| [`architecture.md`](references/architecture.md) | Boot sequence, `VersusSettings`, mixin registration (incl. 4 currently-dormant mixins), access widener, build/test/genSources commands, `@Overwrite` risk census, version history pointer |
| [`combat-and-players.md`](references/combat-and-players.md) | Melee reach/charge/crit/sprint, aim assist, hold-to-attack, food/regen overhaul, jump enchant hook, respawn-near-death, full client attacking pipeline |
| [`enchanting-and-anvil.md`](references/enchanting-and-anvil.md) | 10 custom enchantments (incl. one confirmed dead — Cleaving), anvil cost rework, enchant-upgrading enchanting table, client anvil/enchanting screens |
| [`items-and-equipment.md`](references/items-and-equipment.md) | Vanilla→modded item replacement mechanism, tool/weapon rebalance, shields-on-swords, elytra/fireworks, trident, flint & steel, item burning/decay, copper equipment, Recovery Compass |
| [`brewing-and-potions.md`](references/brewing-and-potions.md) | Brewing recipe graph generator, ~35 potions, 5 new status effects, ~30 concentrates (incl. one confirmed bug), biles, nether wart mutation states |
| [`mobs.md`](references/mobs.md) | Shared mob melee AI (wind-up/shield-block/strafe-dodge), spawn gating, custom hostile mobs (Deeper Creeper, Warden rebalance in depth), a spawn-less mob gap (Wildfire), passive/end/nether/illager pointers |
| [`villagers-and-trading.md`](references/villagers-and-trading.md) | Trade factory DSL, full trade-table replacement, profession-affinity gossip/restock/leveling rework, Wandering Trader buy-offers |
| [`blocks-and-environment.md`](references/blocks-and-environment.md) | Custom block registry, mud/clay moisture hazard system, torch burn-out, sleep/lunar fast-forward mechanic, weather timing rework, archaeology speed-up |
| [`worldgen-engine.md`](references/worldgen-engine.md) | **The most volatile subsystem.** The Players Versus world type, gated aquifer/ore-vein hook, biome layout, `/pvwg` debug tools, worldgen-smoke CI, doc-vs-code status table — points to `docs/worldgen-refactor-plan.md` for full design detail |
| [`worldgen-content-and-structures.md`](references/worldgen-content-and-structures.md) | Custom biomes/features directory map, vanilla-namespace worldgen override blast-radius warning, Trial Towers / Desert Pyramids / Cold Trail Ruins / zombie villages |
| [`inventory-sorting.md`](references/inventory-sorting.md) | Inventory sorting (common-code `InventorySorter`: lists→groups→hotbar rules→row layout; client SWAP clicks via `MovePlanner`), its game tests, hotbar swap/cycle, container quick-dump |
| [`data-and-assets.md`](references/data-and-assets.md) | Data/asset directory map (2405 JSON files), vanilla-vs-mod-namespace override distinction, recommended (not-yet-run) validation checks |
| [`porting-and-history.md`](references/porting-and-history.md) | Branch-per-version model, version-bump checklist, what breaks first on a port, repo hygiene |

## Quick facts
- **Build**: `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"; export PATH="$JAVA_HOME/bin:$PATH"; ./gradlew build` — confirmed passing (~2 min), ~39 cosmetic `@Overwrite`-javadoc warnings expected.
- **Tests**: `./gradlew test` (JUnit 5, server-side classpath). Game tests: `./gradlew runGameTest -Ppv.acceptEula=true` (Fabric game test server; skipped without the property); CI runs them in the `gametest` workflow.
- **Read vanilla/Fabric API source**: `./gradlew genSources`, then browse `.gradle/loom-cache/**/*-sources.jar`.
- Source sets: `src/main` (common/server), `src/client` (client-only), `src/test` (JUnit, worldgen-only, added recently).
- `mixin/` and `mod/` both mirror the same taxonomy: `players/`, `enchantments/`, `environment/{blocks,worldgen,sleeping,archeology}/`, `items_and_effects/{brewing,equipment,throwing}/`, `mobs/{hostile/{end,nether,overworld,illager},passive}/`.
- `remappedSrc/` no longer exists (deleted during the current worldgen refactor) — don't resurrect it.

## Cross-cutting patterns worth knowing before touching anything
1. **The vanilla→modded item replacement pattern** (`VanillaItems.ITEM_REPLACEMENT_MAP` +
   `VanillaItemsMixin` + `ItemStackMixin`'s constructor redirects) — reuse this instead of ad-hoc
   swap logic; see `items-and-equipment.md`.
2. **`@Overwrite` is the highest version-bump risk in this codebase** — it silently drops new
   vanilla behavior on a Minecraft update with zero compiler warning. Prefer `@Inject`/
   `@ModifyVariable`/`@WrapOperation`/`@ModifyReturnValue` for new mixins. See `architecture.md`.
3. **`VersusSettings`** is the single place every feature flag/tuning constant lives — check it
   before assuming a behavior is unconditional, and add new tunables there, not as bare mixin
   constants.
4. **Mixin registration drifts silently** — 4 mixin classes currently exist in source but are
   registered in neither `players-versus.mixins.json` nor the client config (see `architecture.md`
   for the exact list and the recompute-it shell snippet).
5. **Worldgen vanilla-namespace overrides affect vanilla world types too** — not just the PV preset.
   This is flagged as an open, undecided question in the mod's own design doc. Don't assume "it's
   in `data/minecraft/worldgen/`" means "PV-only."

## Confirmed bugs found during this audit (fix candidates, not yet fixed)
- `mod/items_and_effects/brewing/ConcentrateItem.useOnEntity`: instant-effect branch checks
  `StatusEffects.INSTANT_DAMAGE` twice (the intended-heal branch is unreachable dead code) — a
  Concentrate of Health used on an entity currently does nothing on success. See
  `brewing-and-potions.md`.
- `mod/enchantments/` `cleaving.json` (the Cleaving enchantment): registered, obtainable, costs
  anvil levels, has empty effect arrays and zero Java hooks anywhere in `src/` — completely
  non-functional. See `enchanting-and-anvil.md`.
- `mod/items_and_effects/VanillaItems.modifyVanillaStackSizes`'s catch-all loop: a `return` where a
  `continue` was almost certainly meant, inside the `Registries.ITEM` scan for maxCount-1 items —
  aborts the whole remaining scan on the first durability-having item encountered. See
  `items-and-equipment.md`.
- `mod/players/death/RespawnNearLastDeath.respawnPlayerNearTheirDeath`: calls `.get()` on an
  `Optional<GlobalPos>` with no `isPresent()` guard — a client can trigger a `NoSuchElementException`
  by sending the respawn-nearby packet without ever having died. See `combat-and-players.md`.
- `mod/mobs/hostile/nether/WildfireEntity`: fully registered/modeled/rendered custom mob with **no
  natural spawn path** wired anywhere (`MobSpawning.addCustomSpawns()` never places it, no
  `SpawnRestriction`) — summon/spawn-egg only. May be intentional (unreleased mob); flag to
  maintainer either way. See `mobs.md`.

None of these were fixed as part of this audit (read-only pass); each reference doc gives the exact
file/line and a suggested fix where applicable.
