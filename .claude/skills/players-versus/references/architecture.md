# Architecture, Build & Conventions

## Vision (from README + `Ideas for Features.txt`)

> Mod that aims to streamline the design and experience of the vanilla game, mostly by improving
> the things already in the game and only rarely by adding new mobs, entities, items, blocks.

Inspiration: old-school Minecraft + Better Than Adventure. Goal: take vanilla's disparate,
sometimes-shallow systems (amethyst, mud, copper, archaeology, potion ingredients) and either
deepen their existing use or tie them into other systems, so nothing feels vestigial. fabric.mod.json
tagline: *"Improves the challenge; less frustration, more fear, more fun."* When adding anything
new, prefer deepening an existing vanilla mechanic over adding a new item/block/mob — new content
is explicitly the minority case per the README, not the default move.

`src/main/java/frootloops/versus/Ideas for Features.txt` is a live wishlist/changelog-style scratch
file (Quick Wins / Equipment / Environment / Creepies and Crawlies / Wishlist / Done sections) —
worth reading before starting new work, both to avoid duplicating an idea already in flight and to
match the tone of what the maintainer considers in-scope ("Trumpet (toot toot)" is a real entry).

## Module layout

Fabric Loom `splitEnvironmentSourceSets()`: **`src/main`** = common/server code (loaded on both
sides), **`src/client`** = client-only code (never reference client classes from `src/main` — Loom
will not catch this at compile time reliably for all cases, verify with `@Environment(EnvType.CLIENT)`
annotations and package placement). **`src/test`** is a real third source set (see below) added
recently for the worldgen refactor — JUnit 5 via `fabric-loader-junit`, run with `./gradlew test`,
`systemProperty "fabric.side","server"` (client classes are not on the test classpath).

Both `mixin/` and `mod/` trees under `frootloops.versus` mirror the same top-level taxonomy:
`players/`, `enchantments/`, `environment/` (+ `blocks/`, `worldgen/`, `sleeping/`, `archeology/`),
`items_and_effects/` (+ `brewing/`, `equipment/`, `throwing/`), `mobs/` (+ `hostile/`, `passive/`,
with `hostile/` further split into `end/`, `nether/`, `overworld/`, `illager/`). `mixin/` holds only
Mixin classes (targets, injectors, accessors); `mod/` holds ordinary registration/logic code the
mixins and entrypoints call into. **When adding a new area, follow this mirroring** — a new mixin
belongs under `mixin/<area>/`, its supporting logic under `mod/<area>/`, not colocated.

Entry points (`fabric.mod.json`):
- `frootloops.versus.VersusMod` (`ModInitializer`, main) — see boot sequence below.
- `frootloops.versus.VersusModClient` (`ClientModInitializer`, client) — entity renderers, client
  block init, particle factory registration only; deliberately thin.
- `frootloops.versus.VersusModServer` (`DedicatedServerModInitializer`) — dedicated-server-only
  hook; currently just re-registers the same C2S packet receiver `VersusMod.onInitialize()` already
  registers on the integrated-server path (both calls are idempotent/safe, see
  `combat-and-players.md`'s respawn-near-death section).

## `VersusMod.onInitialize()` boot order (order is deliberately meaningful — see inline comments in source)

1. `DefaultItemComponentEvents.MODIFY.addPhaseOrdering(DEFAULT_PHASE, "late")` — reserves a named
   late phase for item-component modification events (consumed in `mod/items_and_effects/*`) so
   this mod's component tweaks apply after other mods' `DEFAULT_PHASE` modifications.
2. `Combat.onInitialize()` — resolves the two custom attack-damage attributes into `RegistryEntry`
   handles; must run before anything reads `Combat.CRITICAL_ATTACK_DAMAGE`/`SPRINT_ATTACK_DAMAGE`.
3. `CustomStatusEffects.registerCustomStatusEffects()`
4. `CustomBlocks.onInitialize()`, `CustomSpecialEffects.onInitialize()`
5. `CustomPotions.registerCustomPotions()`
6. `RegisteringCustomItems.registerAllCustomItems()`, `VanillaItems.onInitialize()`
7. `ModEntities.onInitialize()`
8. `CustomWorldgen.onInitialize()` (registers the two custom features, then calls
   `PvWorldgen.initialize()` — see `worldgen-engine.md`)
9. Network: `PayloadTypeRegistry.playC2S().register(RespawnNearbyPayload.ID, ...)`, then
   `VersusModServer.addPacketRecievers()`.

If you add a new registration step with an ordering dependency (e.g. something that reads a
`RegistryEntry` resolved by an earlier step), insert it in the matching position and leave a
comment — this method has none of Fabric's automatic dependency ordering, it's manual and fragile.

## `VersusSettings` — the mod's central feature-flag/tuning namespace

Static nested classes, static (mostly non-final) fields, read directly by mixins/mod code —
**not** a config file, not hot-reloadable, not exposed to players; changing a value means editing
Java and rebuilding. Four groups: `QOL` (hotbar-swap-on-pick, smarter block placing, bedrock
bridging), `Combat` (aim assist, hold-to-attack, food overhaul flags + starvation toggle +
sweeping-level constant), `Gameplay` (sleep overhaul + tick speed, zombie sieges/raids outside
villages, brushing speed, `isFastForwardingTime` — a *mutable* runtime flag, not a static toggle,
set by the sleep system itself), `Items` (stack sizes and eat-time constants per food category).
`VersusMod.DEBUG_MODE` (top-level, currently `false`) gates verbose nanosecond-timing logs in a few
client attack-path mixins — grep `DEBUG_MODE` before assuming it's dead; it's a real (if coarse)
perf-diagnostic switch, not vestigial.

**When adding a new tunable:** put it in the matching `VersusSettings` inner class next to its
siblings, not as a bare constant buried in the consuming mixin — this file is the single place a
maintainer checks for "what can I flip without touching logic."

## Mixin registration

Two configs: `src/main/resources/players-versus.mixins.json` (package `frootloops.versus.mixin`,
`compatibilityLevel: JAVA_21`, `injectors.defaultRequire: 1`, applies on both sides) and
`src/client/resources/players-versus.client.mixins.json` (package `frootloops.versus.mixin.client`,
same settings, `environment: client` in `fabric.mod.json`). `defaultRequire: 1` means an injector
that fails to match its target throws at mixin-apply time (loud failure) — **any mixin you add is
loud-broken by default if its target signature is wrong**, which is the intended safety net; don't
add `require = 0` to silence a specific injector unless you have a real reason (e.g. optional
compat with another mod's changes) and comment why.

**131 total mixin classes exist in source (as of this writing — recount with the shell snippet
below, this number drifts).** Four are present in source but registered in **neither** config, so
they never apply at runtime — verify each one is *intentionally* dormant (e.g. superseded, kept for
reference) before assuming it's simply forgotten, and don't copy their pattern as "how mixins here
work" since they're unexercised:

| Class | Likely status |
|---|---|
| `mixin.enchantments.EnchantmentProtectionMixin` | Unregistered — check `enchanting.md` notes before reviving; may target something later replaced by `EnchantmentHelperMixin`. |
| `mixin.environment.blocks.ShortPlantMixin` | Unregistered. |
| `mixin.mobs.hostile.nether.ZombifiedPiglin` | Unregistered (also note: filename doesn't end in `Mixin`, unlike every sibling — if reviving, consider renaming for consistency, but check nothing external references the literal class name first). |
| `mixin.client.items_and_effects.inventory.DebugInteractionManagerMixin` | Unregistered (client). |

Recompute this list after any mixin add/remove — it silently drifts:
```bash
for f in $(git ls-files 'src/main/java/frootloops/versus/mixin/*.java'); do
  n=${f#src/main/java/frootloops/versus/mixin/}; n=${n%.java}; n=${n//\//.}
  grep -q "\"$n\"" src/main/resources/players-versus.mixins.json || echo "$n"
done
```
(swap the client path/package/config for the client-side equivalent.)

## Access widener

`src/main/resources/players-versus.accesswidener` (`accessWidener v2 named`, wired via
`fabric.mod.json`'s `"accessWidener"` key and `loom.accessWidenerPath` in `build.gradle` — added
recently alongside the worldgen refactor). Currently widens exactly one method:
`net/minecraft/world/biome/source/util/VanillaBiomeParameters#writeOverworldBiomeParameters` to
`accessible`, consumed by `PvBiomeLayout.build()` (see `worldgen-engine.md`). If you need to call
another vanilla method that's narrower than `public`, add a line here rather than reaching for
reflection or a throwaway `@Accessor` mixin interface — this project already has the plumbing.

## Build & verify (Windows, this checkout)

JDK 21 required; `java`/`gradle` are not on PATH by default here — set `JAVA_HOME` explicitly:
```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"   # or your JDK 21 install
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew build          # full build; confirmed passing (~2 min) on this checkout
./gradlew test           # JUnit 5 worldgen regression tests (fast, no game client needed)
./gradlew genSources     # decompiles Minecraft/Fabric API into .gradle/loom-cache/**-sources.jar — do this once if you need to read vanilla source
./gradlew runWorldgenSmoke -Ppv.acceptEula=true   # see worldgen-engine.md — starts a real server, only pass acceptEula if you mean it
```
`./gradlew build` currently produces ~39 javac warnings, almost entirely `@Overwrite is missing
javadoc comment` on the codebase's many `@Overwrite` mixin methods — cosmetic, not a regression
signal; don't chase these down as bugs. CI (`.github/workflows/build.yml`) runs `./gradlew build`
on `ubuntu-22.04` / Java 21 (Microsoft distribution) for every push and PR; a second workflow
(`.github/workflows/worldgen-smoke.yml`) runs a real headless server benchmark, gated behind either
a manual EULA-accepting dispatch or a `[smoke]` tag in the commit message (see `worldgen-engine.md`).

To read vanilla Minecraft/Fabric API source while working: `./gradlew genSources` then look under
`.gradle/loom-cache/minecraftMaven/**/*-sources.jar` (common) and the matching `clientOnly` jar
(client-only classes) — `jar xf <sources.jar>` extracts them if your editor can't browse a jar
directly. Fabric API module sources live under
`.gradle/loom-cache/remapped_mods/remapped/net/fabricmc/fabric-api/**-sources.jar`.

## `@Overwrite` usage — the highest-risk injector in this codebase

`@Overwrite` fully replaces a vanilla method body; unlike `@Inject`/`@ModifyVariable`/etc. it gives
**zero warning if vanilla's original logic changes** on a Minecraft version bump — the mixin still
"matches" (same signature) but silently drops whatever new vanilla behavior was added inside. As of
this writing, `@Overwrite` appears in ~27 files (recount: `grep -rl '@Overwrite' src`), concentrated
in: `PlayerEntityMixin` (reach), several `mobs/hostile/**` mixins (Zombie, Warden×2, Slime,
Enderman, HostileEntity, MeleeAttackGoal, MobSpawnGroups — AI/spawn-cap logic that vanilla changes
almost every version), `enchantments/**` (AnvilCost, EnchantingTable, EnchantmentHelper), a few
`environment/**` block/weather/minecart mixins, `items_and_effects/brewing/BrewingRecipeRegistryMixin`,
`equipment/LivingEntityBlockingMixin`, and several client rendering mixins (`GameRendererMixin`,
`CrosshairRendererMixin`, `WeatherRendererMixin`, `LightmapMixin`, `IllagerModelMixin`,
`ItemMergingMixin`, `AnimatedResultButtonMixin`). **Before a Minecraft version bump, diff every
`@Overwrite` target method's decompiled body against the version you're porting from** — this is
the #1 source of silent behavior loss in a Fabric mod port, more than any other mixin type. Prefer
`@Inject`/`@ModifyVariable`/`@WrapOperation`/`@ModifyReturnValue` over a new `@Overwrite` when the
change is expressible that way; the worldgen refactor's `ChunkNoiseSamplerMixin` (a `@WrapOperation`
replacing what used to be an `@Overwrite`-heavy pair of mixins) is the template to follow.

## Version history & branching (see `porting-and-history.md` for the full playbook)

Branch-per-Minecraft-version model: `origin/1.19.4`, `1.20.2`, `1.20.4`, `1.20.5`, `1.21.2`,
`1.21.7`, `1.21.9` exist as remote branches; `1.21.9` is this repo's default/main branch;
`26.3---Wilderness-Bound` (current branch at last check) is in-progress work ahead of `1.21.9`.
`remappedSrc/` (a stale mirror produced by Loom's `migrateMappings`, used during past ports) has
been **fully deleted** as of the current worldgen-refactor commits — if you see it referenced in
old notes or an older branch, know that it's gone here and shouldn't be recreated; regenerate
mapped sources with `genSources` instead when porting.

## Known small issues (repo-wide, not otherwise called out)

- 39 `@Overwrite is missing javadoc` warnings at build time — cosmetic (see above), skip.
- Grep `DEBUG_MODE` before removing anything that looks unused in the attack-path client mixins —
  it's a real switch consumers still read, not dead flag.
