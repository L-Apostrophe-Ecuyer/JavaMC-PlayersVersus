# Combat & Players

Player-facing systems: melee combat (reach/charge/crit/sprint), aim assist, hold-to-attack,
food & regeneration overhaul, sprinting gate, jump enchant hook, death/respawn-near-death,
and the client HUD/attacking pipeline that drives all of it.

## Design intent

Vanilla 1.9+ "combat update" mechanics (attack cooldown, reach attribute, sweeping) are kept
but reinterpreted: reach now *scales with charge* (`Combat.getAttackRange`), critical hits
require an actual fall instead of just cooldown, and sprint-attacks get their own damage
attribute instead of relying on movement state at the moment of the swing. The food system
is overhauled into a slower, curve-based regen system (see `VersusSettings.Combat.DO_FOOD_OVERHAUL`)
rather than vanilla's saturation/exhaustion thresholds — deliberately reminiscent of older
Minecraft ("less frustration" per README) while keeping starvation optional and off by default.
Respawn-near-death and hold-to-attack are pure QoL additions gated by `VersusSettings`.

## Component inventory

### `mod/Combat.java` (common) — the shared math library
Static, stateless. Everything else in this subsystem calls into it.
- Tool base stats as constants (`SWORD_SPEED/DAMAGE`, `AXE_*`, `PICKAXE_*`, `SHOVEL_*`, `HOE_*`, `TRIDENT_*`) — **currently unused for actual attribute assignment**; they document intended values but nothing in this repo wires them to item components (verify before assuming tools use these — grep turned up no consumer). Likely aspirational/leftover from an older item-attribute approach.
- Registers two custom `EntityAttribute`s at class-init time via a static block: `critical_attack_damage` and `sprint_attack_damage` (both `ClampedEntityAttribute`, default 2.0, range 0–2048, tracked/synced). `Combat.onInitialize()` (called from `VersusMod.onInitialize`) resolves them into `RegistryEntry` handles (`CRITICAL_ATTACK_DAMAGE`, `SPRINT_ATTACK_DAMAGE`) used everywhere else — **must run after the static registration but the registration happens in a static initializer that fires on class-load, so ordering is naturally safe** even though `Combat.onInitialize()` is called explicitly first in `VersusMod`.
- `getCappedAttackSpeedOf` / `getTicksPerAttackOf`: attack speed is capped at `PLAYER_MAX_ATTACK_SPEED = 2.5`, converted to ticks-per-attack for hold-to-attack timing.
- `getAttackChargeProgress` = `player.getAttackCooldownProgress(0f)`.
- `getAttackRangeBonusOf(ItemStack)`: reads `DataComponentTypes.ATTRIBUTE_MODIFIERS` on mainhand item, looks for an `ENTITY_INTERACTION_RANGE` modifier scoped to `MAINHAND`, returns its value (used for weapon-specific reach bonuses, e.g. tridents).
- `getAttackRange(player, chargeProgress)`: **the core mechanic** — `reachAttribute * min(1, chargeProgress) + (0.5 if riding a live vehicle else 0)`. Reach is zero until charge builds up, unlike vanilla's flat reach.
- `isInAttackRangeOf` / `isLookingTowards*`: shared geometry helpers (dot-product cone check, default threshold -0.5, "strict" -0.75) used both for player aim-assist and for mob melee (see `mobs-hostile-core.md`).
- `getMobAttackBox` / `getEntityHitbox`: shared with mob melee hitbox logic — enderman gets a taller box, airborne mobs get a raised box, vehicle passengers get combined boxes.
- `canPlayerSprint`: **the sprint gate**. If `DO_FOOD_OVERHAUL` is off, vanilla-like (`food > 6`). If on: hunger effect blocks sprinting outright; otherwise sprint is allowed unless food is exactly 0 *and* saturation is exactly 0.
- `AttackType` enum (`NORMAL`, `SPRINT`, `CRITICAL`) and `getAttackType`: charge must be ≥ 0.9 to qualify as anything but NORMAL; sprint requires on-ground+sprinting; critical requires falling and not on ground/climbing/swimming/blind/mounted.

### `mixin/players/PlayerEntityMixin.java` (target: `PlayerEntity`, extends `LivingEntity`)
| Handler | Injector | Target | What it changes |
|---|---|---|---|
| `getEntityInteractionRange` | `@Overwrite` | `getEntityInteractionRange` | Replaces vanilla reach with `Combat.getAttackRange(this)` — **brittle**: any vanilla change to this method's signature/behavior silently drops (no compile error possible to catch semantic drift, only signature drift). |
| `createPlayerAttributes` | `@Inject(HEAD, cancellable)` static | `createPlayerAttributes` | Fully replaces the attribute container: adds the two custom attributes, sets base attack damage to **0.0** (!) and speed to 4.0, reach to `PLAYER_BASE_ATTACK_REACH=3.0`, block range 5.0, plus waypoint ranges. Because base damage is 0, **all player damage must come from item attribute modifiers or the mixins below** — a bare-fist punch deals 0 unless `attack()`'s min-1.0 clamp kicks in (see `attackTypes`). |
| `damage` (override, not mixin injector) | full override of `LivingEntity.damage` | — | Adds difficulty scaling parity, wakes sleeping players when hit (and looks at attacker), and (if `DO_FOOD_EATING_INTERRUPTION`) shortens active eat/drink time when hit hard. |
| `canMineCopperWithWood` | `@Inject(RETURN, cancellable)` on `canHarvest` | — | Wooden pickaxe can harvest copper-sound blocks/ore/raw block — QoL for early game. |
| `getBlockBreakingSpeed` | `@Inject(RETURN, cancellable)` | — | Creative = instant; airborne mining penalty (×4 rising / ×3 falling); cobweb speed buff; deepslate-ore tool-tier gating (gold pickaxe gets flat bonus, wrong tool is 3× slower, else bonus scales with mining speed ≥9); wood catches fire faster; shulker boxes faster; brushable blocks slower. |
| `modifyAppliedDamage` (override) | — | Sonic-boom protection enchant support (vanilla doesn't apply protection to sonic boom). |
| `modifyAttackDamage` | `@ModifyVariable(STORE, ordinal 0)` on `attack` | — | Sprint attacks add `SPRINT_ATTACK_DAMAGE` (floor 1.0); critical hits reverse-engineer vanilla's `×1.5` crit multiplier so the *final* damage equals `max(amount,1)*1.5 + CRITICAL_ATTACK_DAMAGE` after vanilla re-applies its own ×1.5 — **fragile**: assumes vanilla's `attack()` still multiplies crits by 1.5 downstream; if that constant changes upstream this formula silently mis-balances crits. |
| `attackTypes` | `@Inject(HEAD, cancellable)` on `attack` | — | Server-side only (`isClient()` early-return). Breaks the offhand shield on attack (6-tick cooldown). If attack damage attribute < 1.0 (i.e., **no weapon/attribute giving real damage**, since base is 0), attacking a vehicle/armor stand still deals a flat 2.0 damage; attacking a living entity does knockback-only with a "no damage" sound and **cancels the attack** (no damage, no cooldown reset elsewhere unless charge was ≥0.9 critical, which returns early above this check). |
| `doSweepingAttacksOnRegularSwings` | `@ModifyVariable(STORE, ordinal 3)` on `attack` | — | Sweeping Edge triggers sweep on *any* qualifying swing (not just specific conditions) once level ≥ `VersusSettings.Combat.MIN_SWEEPING_LEVEL_FOR_SWEEPING_ATTACKS` (1). |
| `attackKnockbackKitingNerf` | `@Inject(TAIL)` on `attack` | — | Standing still or walking backwards while attacking reduces the *target's* existing velocity (anti-kiting); otherwise low-velocity targets get a small guaranteed knockback nudge. |
| `setUuid` (override) | — | If `DO_FOOD_REDUCED_ON_SPAWN`, sets food to 6 whenever UUID is (re)assigned — i.e. on entity construction, which happens for every player login. |
| `canConsume` | `@Inject(HEAD, cancellable)` | — | Overhauled eating gate: can always eat if hunger not full AND `food < 6 + missingHealth` — lets players "overeat" to heal even near-full food, unlike vanilla's flat `foodLevel<20` check. |
| `setSprinting` (override) | — | Delegates the sprint gate to `Combat.canPlayerSprint`; silently no-ops (doesn't call super) if disallowed. |
| `dropInventory` (override) | — | On death: Vanishing-Curse items vanish; large stacks / non-common-rarity / custom-named / stored-enchant items get "coveted" (slower despawn); epic-rarity / container / damage-resistant items **never despawn**; everything else gets the usual 20-tick pickup delay. If a player killed you, dropped items are `setOwner`'d to the killer (loot-priority PvP QoL). |
| `getTargetingMargin` (override) | — | Gliding players get a fatter hitbox margin (2.0) for mob targeting. |

### `mixin/players/HungerManagerMixin.java` (target: `HungerManager`)
Only active when `VersusSettings.Combat.DO_FOOD_OVERHAUL` is true; otherwise `update` returns
immediately without cancelling, so **vanilla hunger runs unmodified when the flag is off**
(the `eat` injector still always runs, though — see below, this is asymmetric and worth noting).
- `eat` (`@Inject(HEAD)`, always active regardless of flag): resets exhaustion, floors food at 0, floors saturation at 0.1, and floors `foodTickTimer` at 16 — makes eating always give at least a sliver of saturation/regen headroom even under the overhaul.
- `update` (`@Inject(HEAD, cancellable)`): if overhaul enabled, fully replaces vanilla's per-tick hunger/regen logic and cancels vanilla's own `update` body.
  - Hunger-effect exhaustion accrues faster (`0.03125`/tick vs `0.00390625`/tick).
  - `doHungerExhaustion`: at 0 food, either starves (1 damage per exhaustion-cycle once saturation is also 0, gated by `IS_STARVATION_ENABLED` or the Hunger effect) or, if starvation disabled, decays saturation slowly toward a 0.25 floor. Above 0 food, exhaustion above 4.0 consumes a food point (or 0.5 saturation) — deliberately faster than vanilla's flat accounting, to disincentivize "topping off" food unnecessarily.
  - `doHealthRegeneration`: fast heal (24-tick interval) requires food ≥2 and one of {wither/fire (no fire-res)/poison-above-1hp} — i.e. fast healing is reserved for recovering from DoT effects. Slow heal (48-tick interval) is the default passive regen at food ≥1. Both consume food/saturation on tick-over. Getting hit resets the timer to a negative "recovery" window (`-20`/`-80` ticks) before regen can resume.

### `mixin/players/SpecialMovementMixin.java` (target: `PlayerEntity`)
Overrides `jump()` entirely to add the **Bounding Strides** custom enchantment (leggings-only,
see `enchanting-and-anvil.md` for registration): while sprinting, jump distance and (if colliding
or slow) jump height scale with level, costs extra hunger exhaustion, and adds particles/sound/a
short Speed burst. With no leggings or level 0, falls through to vanilla `super.jump()` untouched.

### `mod/players/RayTraceHandler.java`
Standalone raycast helper (credited to CloudG360/Team Abnormals, ported from an old bridging mod)
used by building/placement QoL (see `blocks-and-building.md` for bedrock-bridging/smarter-placing
consumers) — not attack-related despite living in `mod/players`. Interpolates position/rotation
between last-tick and current-tick for smooth raycasts.

### `mod/players/death/RespawnNearLastDeath.java` + `RespawnNearbyPayload.java` + `VersusModServer`
Implements the "Respawn Nearby" death-screen button: client sends `RespawnNearbyPayload` (a
`CustomPayload` C2S record carrying the requester's UUID) → `VersusModServer.addPacketRecievers`
registers a global receiver that runs `RespawnNearLastDeath.respawnPlayerNearTheirDeath` on the
server thread. That method only proceeds if the last-death dimension **and** current dimension
are both the Overworld (De Morgan's on the `||` guard — this reads oddly but is correct), then
does a vanilla-style spiral search (`spawnRadius=128`, offset multiplier 17) for a safe surface
block near the death position and teleports the player there.
- **Crash-risk (unverified against a live server, flagged for verification):** `lastDeathPos.get()` is called immediately after `Optional<GlobalPos> lastDeathPos = player.getLastDeathPos();` with no `isPresent()`/`isEmpty()` guard. A client can send `RespawnNearbyPayload` at any time (it's a plain C2S packet with no server-side precondition), including from a player who has never died this life (empty Optional) — this would throw `NoSuchElementException` inside the network-thread callback. Minecraft's networking layer generally logs-and-disconnects rather than crashing the whole server on a handler exception, but it is still an easy way for any connected client to force an exception/disconnect. **Suggested fix:** add `if (lastDeathPos.isEmpty()) return;` before line 35 of `RespawnNearLastDeath.java`.
- Networking registration happens twice defensively: once in `VersusMod.onInitialize` (common/client integrated-server path) and once in `VersusModServer.onInitializeServer` (dedicated server) — both call the same idempotent `addPacketRecievers()`, so no double-registration risk on dedicated servers (integrated server only runs `VersusMod`, dedicated only runs `VersusModServer`, but if both ever ran together `ServerPlayNetworking.registerGlobalReceiver` on an already-registered ID would throw — verify with `grep -rn addPacketRecievers` if changing this).

### Client death UI — `mixin/client/players/death/DeathScreenMixin.java` (target: `DeathScreen`)
Relabels the respawn/spectate buttons ("...Home") and adds a third disabled-until-clicked
"Respawn/Spectate Nearby" button (Overworld deaths only) that sends `RespawnNearbyPayload` and
requests a normal respawn simultaneously — the normal respawn happens first (vanilla flow),
then the nearby-teleport packet arrives and moves the player again shortly after spawning at
the world spawn. Button height constants are nudged down 12px only in the Overworld branch to
make room for the third button.

### Client attacking pipeline
1. **`mixin/client/players/attacking/GameRendererMixin`** (`@Overwrite findCrosshairTarget`): re-implements vanilla's crosshair raycast; entity-vs-block priority logic is unchanged from vanilla except it explicitly re-clamps the block hit to `blockInteractionRange`.
2. **`CrosshairTargetMixin`** (`@ModifyVariable` on `findCrosshairTarget`'s local `entityInteractionRange`): overrides the value actually used for entity raycasting to `Combat.getAttackRange(client.player)` — i.e., the *crosshair itself* only reaches out to charge-scaled range, matching the actual attack range.
3. **`CrosshairRendererMixin`** (`@Overwrite renderCrosshair`, `priority=9999`): crosshair size shrinks/grows with charge progress (`1 + 2*(int)(7*progress)` px), and the attack-indicator (sword icon) only shows "ready" when charge-per-tick exceeds 5.0 and the target is actually in range/alive — otherwise shows the vanilla partial-charge bar.
4. **`mixin/client/players/MinecraftClientMixin`** (`priority=999`, `doItemUse` `@Inject(HEAD, cancellable)`): fully reimplements right-click-use hand ordering. New behavior: clicking an `ItemFrameEntity` while not sneaking, if there's a chest immediately behind the frame's attachment block, interacts with the **chest** instead of the frame (prevents accidental frame-clicking near chests — see `mixin/environment/blocks` sibling doc? no — this is player-only). Off-hand-first priority (`shouldPrioritizeOffhand`) is a large heuristic favoring shields/blocking items and food, considering current target type and sneaking.
5. **`mixin/client/players/attacking/MinecraftClientMixin`** (`priority=100000`, a **different** mixin class than #4, same target — see cross-cutting note below): implements hold-to-attack (`handleBlockBreaking` intercepted before vanilla mining-key handling) and fully reimplements `doAttack` to add aim-assist (targets the previous target or your last attacker within a tight -0.9 dot-product cone and line-of-sight, when not already aiming at an entity) plus `DEBUG_MODE`-gated nanosecond timing logs.
6. **`mixin/client/players/HungerBarRendererMixin`** (`@Overwrite? no — @Inject(HEAD, cancellable) renderFood`): redraws the food HUD to show "damage debt" (missing health padded onto the food bar) and swaps in disabled/hunger-effect icon variants (`VersusModClient.HUD_TEXTURE_DISABLED_FOOD*`).
7. **`mixin/client/players/HungerSprintingMixin`** (`@ModifyConstant` on `ClientPlayerEntity.canSprint()`'s `6.0f` literal): client-side prediction mirror of `Combat.canPlayerSprint` so the client doesn't locally block sprint-start when the server would allow it (returns `-1.0f`/`128.0f` to always-pass/always-fail the vanilla comparison rather than duplicating the comparison operator).
8. **`DebugHudMixin`** / **`DebugOptionsScreenMixin`**: hide most F3 debug info (position, FPS, light, biome) from non-op players in survival-like game modes, and hide the corresponding config checkboxes in the debug options screen (op-level-2 override still shows everything).

**Two same-target, same-side mixin classes on `MinecraftClient`** exist in this subsystem
(`mixin.client.players.MinecraftClientMixin` @999 and `mixin.client.players.attacking.MinecraftClientMixin`
@100000) plus a third in `client/environment/worldgen` — check `players-versus.client.mixins.json`
priorities before adding a fourth; Mixin applies them in ascending priority order, and this repo
already relies on that ordering (attacking's `doItemUse`... wait, `doAttack`/`handleBlockBreaking`
must run before/after specific vanilla internals — if you add a new `MinecraftClient` mixin,
explicitly set `priority` and confirm against these two).

## Known small issue

`DebugHudMixin.limitDebugWhenInSurvival`, biome branch: builds an unused local
`MutableText text = Text.literal("Biome: ").append("subtitles.players-versus.sword_blocking").append(" ");`
that is never read — dead code, and the string being appended (`subtitles.players-versus.sword_blocking`,
a translation *key*, not translated) looks like a copy-paste leftover. The actual displayed line
one below it (`list.add(" Biome: " + getBiomeName(...) + " ")`) is correct and unaffected. Safe,
zero-risk cleanup: delete the dead `text` line.

## Tuning knobs

| Name | Value | File:line | Effect |
|---|---|---|---|
| `VersusSettings.Combat.CAN_AIM_ASSIST` | `true` | VersusSettings.java | Gates aim-assist in `doAttack` |
| `VersusSettings.Combat.CAN_HOLD_TO_ATTACK` | `true` | VersusSettings.java | Gates hold-to-attack in `handleBlockBreaking` |
| `VersusSettings.Combat.DO_FOOD_OVERHAUL` | `true` | VersusSettings.java | Switches hunger/regen/sprint-gate/eat-gate logic |
| `VersusSettings.Combat.DO_FOOD_EATING_INTERRUPTION` | `true` | VersusSettings.java | Hits interrupt long eat/drink actions |
| `VersusSettings.Combat.DO_FOOD_REDUCED_ON_SPAWN` | `true` | VersusSettings.java | New players spawn at 6 food, not 20 |
| `VersusSettings.Combat.IS_STARVATION_ENABLED` | `false` | VersusSettings.java | Starvation damage off by default |
| `VersusSettings.Combat.MIN_SWEEPING_LEVEL_FOR_SWEEPING_ATTACKS` | `1` (final) | VersusSettings.java | Sweeping Edge level needed for sweep-on-any-swing |
| `Combat.PLAYER_BASE_ATTACK_REACH` | `3.0` | Combat.java:63 | Base reach before charge scaling |
| `Combat.PLAYER_MAX_ATTACK_SPEED` | `2.5` | Combat.java:64 | Attack-speed attribute cap for timing math |
| `Combat.MIN_COOLDOWN_TO_SWING` | `0.6f` | Combat.java:59 | Charge threshold to permit hold-to-attack swing |
| `HungerManagerMixin.REGEN_TIME_FAST/SLOW` | 24/48 ticks | HungerManagerMixin.java:30 | Regen tick interval |
| `HungerManagerMixin.FOOD_REQUIRED_FOR_FAST_REGEN` | 2 | :32 | Fast-heal food floor |

## Extension recipes

**Add a new custom attack-damage attribute (like crit/sprint):** register it in a static block
similar to `Combat`'s (pick a unique `Identifier`), resolve it in `Combat.onInitialize()`, add it
to `createPlayerAttributes` in `PlayerEntityMixin`, and consume it in `modifyAttackDamage`. Remember
`VersusMod.onInitialize()` calls `Combat.onInitialize()` early — any new attribute lookup added there
runs before items/entities register, so it's safe to reference from registration code afterward.

**Add a new food/sprint gating rule:** touch only `Combat.canPlayerSprint` (server authority) and
mirror the exact same boolean into `HungerSprintingMixin.foodRequiedToSprint` (client prediction) —
if these two diverge, the client will locally reject sprint starts the server would have allowed
(or vice versa), causing rubber-banding.

**Add a new hold-to-attack condition:** edit `attacking/MinecraftClientMixin.holdToAttack`; keep
`Combat.getTicksPerAttackOf` as the single source of truth for timing so it stays consistent with
the crosshair/attack-indicator rendering in `CrosshairRendererMixin`.
