# Version History & Porting Playbook

## Branch model
One branch per targeted Minecraft version: `origin/1.19.4`, `1.20.2`, `1.20.4`, `1.20.5`, `1.21.2`,
`1.21.7`, `1.21.9` (this repo's default/main branch — `origin/HEAD` points here), plus in-progress
work branches like `26.3---Wilderness-Bound` (observed as the active branch during this audit,
19 commits ahead of `1.21.9` at time of writing, entirely worldgen-refactor work — see
`worldgen-engine.md`). **When porting to a new Minecraft version, branch from the most recent
version branch, not from an in-progress feature branch**, unless the feature branch's work is
meant to carry forward into the port too.

## What actually breaks on a version bump (inferred from this repo's own structure, not tested)
Based on the density of `@Overwrite` usage (`architecture.md`'s census) and the areas this audit
found richest in vanilla-internals assumptions, expect the most porting work in, roughly, this order:
1. **Worldgen** — `NoiseRouter`/`DensityFunction`/`ChunkGeneratorSettings` constructor shapes,
   `MultiNoiseUtil`/`VanillaBiomeParameters` internals, surface-rule JSON schema. This mod already
   tracks this explicitly: `docs/worldgen-refactor-plan.md` Appendix D is a pre-built Yarn→Mojang
   name-mapping table for a future 26.x (unobfuscated) port — **read it before starting a 26.x
   port**, it was written specifically for this purpose.
2. **Mob AI internals** (`@Overwrite`-heavy: Zombie, Warden×2, Slime, Enderman, HostileEntity,
   MeleeAttackGoal, MobSpawnGroups) — Mojang changes hostile-mob AI/spawn logic almost every
   version; every `@Overwrite` here needs a fresh diff against the new decompiled source.
2. **Enchanting/anvil internals** (`AnvilCostMixin`, `EnchantingTableMixin`, `EnchantmentHelperMixin`
   — all full-method `@Overwrite`s) — the enchantment-effects-component system itself is relatively
   new (1.21+) and has been actively evolving; expect signature drift.
3. **Client rendering mixins** (`GameRendererMixin`, `CrosshairRendererMixin`, `WeatherRendererMixin`,
   `LightmapMixin`, model mixins) — rendering pipeline internals (render layers, `DrawContext` API)
   change most cosmetically-disruptively but are usually mechanically simple to re-port (signature
   fixes, not logic rewrites).
4. **Item component system consumers** (`VanillaItems`, `CustomEquipment`, `ConcentrateItem`, etc.)
   — Mojang's data-component API (`DataComponentTypes`) is still evolving version to version;
   expect new/renamed/restructured components more than removed ones.

## Version-bump checklist (derived from `gradle.properties`/`fabric.mod.json`/`architecture.md`)
1. `gradle.properties`: `minecraft_version`, `yarn_mappings`, `loader_version`, `loom_version`,
   `fabric_version` — check https://fabricmc.net/develop for current compatible values.
2. `fabric.mod.json`: `depends.minecraft`/`depends.fabricloader`/`depends.fabric-api` ranges.
3. Run `./gradlew build` — fix compile errors from renamed/removed vanilla APIs first (mechanical).
4. Re-verify every `@Overwrite` body against the new decompiled vanilla source
   (`./gradlew genSources`, see `architecture.md`) — this is not caught by the compiler; an
   `@Overwrite` with a matching signature but stale internal logic **compiles and loads fine while
   silently dropping new vanilla behavior**.
5. Recompute the unregistered-mixin list (`architecture.md`'s grep snippet) — a mixin whose target
   method's signature changed will fail loudly at mixin-apply time with `defaultRequire: 1`; fix or
   deliberately unregister it, don't leave it broken.
6. Re-run `./gradlew test` (worldgen regression suite, see `worldgen-engine.md`) and, if touching
   worldgen at all, `./gradlew runWorldgenSmoke -Ppv.acceptEula=true` against both `minecraft:normal`
   and `players-versus:better_world` to compare timings/correctness metrics.
7. Spot-check `docs/worldgen-refactor-plan.md`'s Appendix B (verified API signatures) against the
   new Yarn mappings if continuing worldgen-refactor work mid-port.

## Repo hygiene notes
- `remappedSrc/` (a stale Loom `migrateMappings` output directory used during earlier ports) has
  been **fully deleted** as of the current worldgen-refactor commit range — do not resurrect it as
  a reference; if you need old-version source for comparison, check out the relevant version branch
  instead (`git show origin/1.21.7:src/main/java/...`).
- `bin/` and `build/` are compiler/IDE output, gitignored, never a source of truth.
- Commit message conventions observed: short imperative summaries ("Fixed and tweaked worldgen",
  "Rebalanced shields", "Update to 1.21.10"); the active worldgen-refactor branch additionally uses
  a `[smoke]` tag suffix on commits that should trigger the `worldgen-smoke` CI benchmark (see
  `worldgen-engine.md`) — **only add `[smoke]` when you actually want that CI job to spin up a real
  Minecraft server**, it's a functional trigger, not decoration.
- `docs/worldgen-refactor-plan.md` is a living design document, not a historical record — if you
  advance a phase (e.g. finish Phase 2's lattice aquifer properly, or implement Phase 3), update its
  status tables; this audit found the doc already lagging behind the actual code state once (Phase 1
  biome layout shown as pending in the doc while already live in code) — don't let that drift grow.

## Minecraft's future versioning (unverified — external knowledge, not derived from this repo)
Minecraft has been moving toward year-based versioning (26.x) and away from obfuscated/Yarn-mapped
jars toward Mojang's own official mappings for some builds. The `docs/worldgen-refactor-plan.md`
Appendix D name-mapping table is this repo's own preparation for that transition. Treat any other
claim about "26.x" or "unobfuscated jars" behavior as **external knowledge Claude brings, not
something this codebase itself has verified** — confirm against Mojang/Fabric's actual current
documentation before acting on it, since this area moves fast and this audit did not check
current external sources.
