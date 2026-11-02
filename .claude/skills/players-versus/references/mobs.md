# Mobs: Shared Combat AI, Spawning & Individual Redesigns

Covers every hostile/passive mob mixin and custom entity in one pass. Depth varies: the **shared
mechanics** (melee AI, spawn gating, moon/midnight helpers, entity registration) were read in full
and are documented in detail below; several **individual mob redesigns** are summarized from
direct inspection but not exhaustively — where a section says "see `<path>`", read that file
directly before making changes there; it wasn't fully re-verified line-by-line in this pass.

## Design intent
README: "Almost all mobs and PVE combat encounters sometimes tweaked (Skeletons), sometimes
redesigned (Phantoms). Mobs can use shields. Melee attacks from mobs have a short animation and can
now be dodged by strafing. Some new creatures found deep down in caves; blind creatures, or creepers
that only move when looking away." The shared thread: give hostile mobs the *same* attack-charge/
telegraph/shield-block vocabulary players have (see `combat-and-players.md`), so fights read as
readable exchanges instead of instant, unavoidable damage ticks.

## Shared hostile-mob AI (applies to every `MobEntity` using vanilla's `MeleeAttackGoal`)

### `MeleeAttackGoalMixin` (target `MeleeAttackGoal`) — the core redesign, `@Overwrite`-heavy
Replaces vanilla's "walk in, hit when close" melee goal with a full wind-up/cooldown/shield state
machine mirroring the player's own attack-charge system:
- **Cooldown length** (`getCooldownAmount`): axe/trident mainhand → 40 ticks + endlag; anything
  else in-hand → 20 ticks + endlag; empty mainhand → 20 ticks + endlag too (only the axe/trident
  branch differs). Endlag is 4 ticks for arthropods (spiders — faster recovery), 8 for everything
  else — **this is the literal implementation of "melee attacks now have a short animation and can
  be dodged by strafing"**: the mob telegraphs a swing, and a wind-up + recovery window exists for
  the player to react to.
- **Shield-blocking mobs** (`tick`, `canBlockWithShield`, `shouldPlayDefensively`): any mob with a
  blocking item (`getUseAction() == BLOCK`) in its off-hand can raise it — crouches, zeroes
  horizontal velocity, plays the block-use animation — for 16–32 ticks based on a fairly elaborate
  heuristic: won't block if recently hit (attack-of-opportunity punish window), won't block if the
  target is very close/very far and the mob itself isn't currently hurt, *will* try to block if the
  opponent is airborne/sprinting (anticipating a crit/sprint-attack) with a 1-in-3 tick check gated
  on actually looking at them, and won't bother blocking a slow-walking easy target. This is the
  Java-side half of "mobs can use shields" (README) — the item itself (illager/skeleton/zombie
  shields) is placed via loot tables/equipment, not this file.
- **Attack execution** (`attack`, `@Overwrite`): a swing only "lands" 4-8 ticks after it starts
  (`willTryLandingAnAttack` window relative to `numTicksEndlag`), and only if the mob can still see
  the target at that moment — a player who breaks line-of-sight mid-swing causes a "missed" sound
  instead of damage. Close-quarters (<1.5 blocks either eye-to-eye or center-to-center) always
  permits a swing/landing attempt; otherwise it needs the shared `Combat.getMobAttackBox`/
  `getEntityHitbox` intersection test (see `combat-and-players.md` — same hitbox math both players
  and mobs use), with a **jump-attack** fallback (mob leaps at 1.6× velocity + jump-boost modifier
  if grounded and the box only intersects when jumping). Getting hurt mid-wind-up
  (`hurtTime > 14`) interrupts the attack outright.
- `mobsNeedToBeAimingToLandHit` (`@Inject TAIL`): while mid-swing or mid-block, the mob's look
  control is locked to a slow 10°/10° turn rate — it can't snap-aim during its own animation, which
  is what actually makes strafing/dodging effective against it.

### `HostileEntityMixin.canSpawnInDark` (`@Overwrite`, target `HostileEntity`)
Adds explicit block-tag/moon-phase gates on top of vanilla's darkness check: Zombie/Skeleton natural
spawns need `#players-versus:undead_overworld_spawnable_on` underfoot (and are blocked above y64
under open sky) **unless it's midnight** (`MobSpawning.isMidnight`); Creeper natural spawns need
`#players-versus:creeper_spawnable_on` and sky-light ≤7 below y128 **unless it's a new moon**
(`MobSpawning.isNewMoon`). This is the concrete mechanism behind the README's "ambient darkness
increases during new moons" claim for spawning specifically (the visual/gameplay-difficulty side of
new-moon darkness is covered in `environment-systems.md`, not re-verified here — cross-check before
assuming full overlap).

### `MobSpawning` (spawn helpers + custom spawn table)
- `isMidnight`/`isNewMoon`/`isMidnightDuringNewMoon`: the overworld clock's day time `% 24000` between 18000
  and 20000 = midnight; moon phase `NEW_MOON_PHASE` (4, vanilla's `MoonPhase` new moon; 0 is full) = new moon. It
  was 7 (waxing gibbous) before the feedback batch of 2026-10; creepers' and pale zombies' new-moon rules use it.
- `addCustomSpawns()`: Deeper Creeper in Deep Caves/Regular Cave biomes (weight 100, group 1-1);
  Wither Skeleton (60) and Zombified Piglin (3, group 1-4) also added to Deep Caves; Breeze added to
  Frozen Peaks (100); vanilla's Frostbite wherever vanilla spawns snow foxes (140, group 2-4; it took
  over from the mod's Frosted Zombie in the 26.4 port); the Ice Cube in vanilla's Ice Caves (100,
  group 1-2); extra
  desert fauna (cave spider, husk, camel, cat); Phantoms as an ordinary overworld monster (80, group
  1-2) everywhere but the mushroom fields and mushroom caves, at the new moon's midnight only
  (`PhantomMixin.checkSpawnRules`); vanilla's insomnia spawner does nothing (`PhantomSpawnerMixin`);
  Blaze added to Nether Wastes (15). Mushroom creepers and spiders: `SpawnPlacements` with
  `Monster::checkMonsterSpawnRules`; they spawn by the mushroom caves' biome file (only they, at a spawn
  cost, charge 0.8).
  A commented-out Wither Skeleton overworld-wide line and a commented-out Pale Zombie
  BiomeModifications call remain in source — both custom undead mobs currently spawn **only** via
  their `SpawnRestriction` predicate (below), not via `BiomeModifications.addSpawn`; if you want
  Pale Zombies to actually spawn naturally, that line needs uncommenting (verify intentional
  before doing so — could be deliberately disabled pending balance work).
- Per-mob `SpawnRestriction` predicates add fine-grained placement logic beyond the biome-modification
  weight: Deeper Creeper needs light ≤1, `#pale_creeper_spawnable_on`, and y<32; Pale Zombie needs
  light 0, `#undead_overworld_spawnable_on`, auto-allowed in Deep Caves biome, otherwise y-gated
  (never above y64, midnight-during-new-moon only between y24-64, unconditional below y24) — a
  three-tier depth/rarity curve; the Ice Cube (`IceCubeEntity.checkIceCubeSpawnRules`) needs a dark
  enough spot (vanilla's monster light check) unless a spawner made it.

## Custom hostile entities

- **`PaleCreeperEntity`** (extends `CreeperEntity`, spawn egg registered): faster (0.36 speed),
  longer follow range (40), armored (10/3.0 armor/toughness), immune to Wither damage (explicit
  `hurtServer` override), explodes with an area-effect cloud of Blight on death (5-block radius,
  300-tick duration, shrinking; each reach of the cloud is a dose, and doses stack)
  after vanilla's plain explosion, and has bespoke footstep/hurt
  sounds (stone + mangrove-roots layered) plus a cave-ambience idle sound when it has no target —
  "blind creatures... that only move when looking away" per the README maps to `CreeperFollowTargetThroughWallsGoal`
  + `CreepingAndExplodingGoal` (not re-read this pass — check those two goal classes directly for the
  exact "only moves unobserved" state machine). A blighted
  regular creeper wounded below 8 HP turns **into** a pale creeper (`Undeath#turnPale`, from
  `LivingEntityBlightMixin`) — an in-fiction mutation path, not just a separate spawn pool.
- **Mushroom creeper and mushroom spider** (`MushroomCreeperEntity`, `MushroomSpiderEntity`; renderers and spawn
  eggs; textures from `tools/mushroom-mobs.py`) — of the mushroom caves. They ignore players (no player target
  goal) and hunt the plain kind (`EntityTypes.CREEPER`/`SPIDER` exactly); `HurtByTargetGoal` makes them fight back.
  The creeper's swelling ends in a spore puff, always (`CreeperMixin` at `explodeCreeper`'s head, server side; it
  also puts out a lit one, which would puff for ever): no damage, every plain creeper within 3 blocks `convertTo` a
  mushroom creeper, nothing else touched, swell reset, 100-tick cooldown on its swell goal. It never explodes, after a
  player or lit. Both are the red mushroom's hosts (lore spec `2026-11-01-pale-warp-lore-and-watchling-design.md`):
  red and white skins (`tools/mushroom-mobs.py`, from the red mushroom block) and red mushrooms growing out of the
  creeper's head and the spider's abdomen (client `RedMushroomsLayer`, the red mushroom's block model as the
  mooshroom's, resolved each frame in their renderers' `extractRenderState`). Never converted. The spider's `doHurtTarget` on a plain spider converts it, unharmed. Game tests
  `MushroomMobsGameTest`, assets `MushroomMobsTest`.
- **Watchling** (`mod/mobs/hostile/end/WatchlingEntity`, extends `Enderman`; client `WatchlingRenderer`,
  `WatchlingStatic`; spec `2026-11-01-pale-warp-lore-and-watchling-design.md`): an enderman with a creaking's yellow
  eyes (three of them, on a head 12 tall: `WatchlingModel`, built from `EndermanModel.createBodyLayer().apply(...)`,
  `tools/watchling-head.py`, big arms and a jaw from a 64x64 texture; `WatchlingModel` is a `HumanoidModel`, animated per
  `WatchlingAnimation`: maw opening while it fights, head cocked while it haunts, a two-armed strike) that haunts like Slender. Left alone it never moves or
  strikes (its enderman-style goals are gated on `isEnraged()`). `customServerAiStep`: watched (`WatchlingStare.watcher`) it notes its watcher and `DATA_HUNTING` (synced,
  `isHunting`); looked away from, it teleports after `TELEPORT_DELAY`-2x ticks to nearer
  (`WatchlingStare.approach`: `APPROACH_MIN`-`MAX` of the distance, not nearer than `CLOSEST`) at a bearing more than
  `ASIDE_DEGREES` off the watcher's facing; unlooked at it creeps nearer the same way every `STALK_MIN`-`MAX` ticks;
  unlooked at for `VANISH_MIN`-`MAX` ticks (90 s to 3 min) after being seen it discards itself. Hit by any non-watchling
  living attacker (or shot at) `hurtServer` -> `enrage`: `DATA_ENRAGED`, it targets the attacker, melee-fights with an
  enderman's stats, `fight()` teleports up to a target beyond `FIGHT_TELEPORT_RANGE` and `calm()` returns it to haunting
  after `CALM_TICKS` with no live target; `isHunting()` is false while enraged (no pull), and `WatchlingStare.rate(
  fighting, looking, distance)` ignores nearness for a fighting one (a stare still builds static). `WatchlingStare` (server, each level tick): the pressure on a player is the fastest rate among
  the watchlings about them: 1 for looking at one (48 blocks, line of sight, at least half seen by `Gaze`, no
  `#gaze_disguise_equipment` on the head) plus `proximity(distance)` (0 beyond `NEAR_RANGE`, up to `PROXIMITY_RATE` at
  `KILL_RANGE`, looking or not); the static (`WatchedPayload` to the HUD) fills at rate/`STARE_TICKS` and clears over
  `CLEAR_TICKS`; full, they die of `players-versus:watched` and 4-7 silverfish spawn. Client `WatchlingPull` turns the
  local player's view towards a hunting one within `PULL_RANGE` now and then (`Entity#turn`, as the mouse does).
  Spawns rarely in the dark by the pale line.
- **The warped show only to a stare**: `Gaze` (pure, `GazeTest`, `VersusSettings.Gaze`) rates how well a view sees a
  body (1 on it, smoothly down to 0 at 30° off its edge) and `Gaze.opacity` floors it at 3% and rises with the square. Client
  `LivingEntityRendererGazeMixin` + `LivingEntityRenderStateGazeMixin` (`GazeRenderState`) draw endermen, endermites,
  watchlings and phantoms with `RenderTypes.entityTranslucent` at that alpha and fade their shadows;
  `EyesLayerGazeMixin` dims their eyes (the 9-argument `submitModel` with a tint); `CarriedBlockLayerGazeMixin` hides a
  carried block under 60% (`GazeTranslucency`). A watchling is watched from 70% seen.
- **Watchling stare, heard and seen**: client `WatchlingStaticSound` (a looping `AbstractTickableSoundInstance` of
  `players-versus:entity.watchling.static`, `tools/watchling-static-sound.py`) swells and rises with the HUD static;
  `PaleSight` asks for `watchling_1..16` (`tools/post-effects.py`).
- **Pale villager** (`mod/mobs/hostile/overworld/PaleVillagerEntity`, extends `ZombieVillager`; client
  `PaleVillagerRenderer`, the villager model and `VillagerProfessionLayer` "villager" over a villager-illager skin from
  `tools/pale-villager.py`: ashen skin, the illager's brow, eyes and mouth): a blighted, wounded villager
  (`Undeath#turnPale`/`turnPaleVillager`, or killed by one). Keeps villager data (type, profession, level: its clothes),
  gossip, offers, xp, all restored on its cure (game test `aBlightedPaleAndCuredVillagerKeepsItsMemories`); inherits the
  golden apple cure (`ZombieVillagerMixin`). Flees players (`AvoidEntityGoal`), hunts `AbstractVillager`s, its hits blight
  (`BlightAttacks`), its kills turn pale (villagers and wandering traders); no reinforcements (`handleAttributes` empty, chance 0),
  no drowned/frostbite conversion, never despawns, villager sounds pitched low. `VillagerHostilesSensorMixin` makes
  villagers flee it (8 blocks). Blight-immune.
- **Pale wandering trader** (`PaleWanderingTraderEntity`, extends `PaleVillagerEntity`; client
  `PaleWanderingTraderRenderer`, the wandering trader's model and the same skin treatment): a blighted, wounded wandering trader
  (`Undeath#turnPaleTrader`), keeps its offers; cured (`ZombieVillagerMixin` at `finishConversion` HEAD) it's a
  wandering trader again with them (`overrideOffers`) and 48000 ticks to wander.
- **Undead animals in body armour don't burn** (`MobSunArmourMixin` at `Mob#burnUndead` HEAD).
- **Zombie chicken** (`mod/mobs/passive/ZombieChickenEntity`, extends `Chicken`; client `ZombieChickenRenderer`;
  textures from `tools/undead-animals.py`): what Blight makes of a chicken, a passive undead animal as the zombie
  horse. No eggs (`eggTime` held up), no food, no offspring, never a chick (`canBeABaby`, `setAge` clamped), always the
  temperate variant (`setVariant` ignored), lower voice. In `#minecraft:zombies` (so undead) and
  `#minecraft:burn_in_daylight` (vanilla's tag-driven `Mob#burnUndead`), never converted. Drops 0-2 feathers and 0-1
  rotten flesh. Baby zombies' chicken jockeys ride them (`ZombieInfectionMixin` wraps `Zombie#finalizeSpawn`'s
  `EntityType#create` and keeps only zombie chickens among the nearby ones). Game tests `UndeadFormsGameTest`.
- **Zombie wolf** (`mod/mobs/passive/ZombieWolfEntity`, extends `Wolf`; drawn by vanilla's `WolfRenderer`, its skins
  from `getTexture` (wild, tame, angry; `tools/wolves.py`, the zombie's green)): what Blight makes of a wolf
  (`Undeath#undeadKind`), keeping tame/owner (`setTame(.., true)`: a tame one's 40 health), its collar (`WolfAccessor`
  invoker) and its armour (the conversion's equipment). A wolf in all but: never a pup, never in love or breeding, eats
  only rotten flesh (a tame one heals on it), lower voice. In `#minecraft:zombies` and `#minecraft:burn_in_daylight`
  (wolf armour shades it, `MobSunArmourMixin`); not Blight-immune: dying fully blighted it turns pale (below). Drops
  0-2 rotten flesh.
- **Pale wolf** (`mod/mobs/hostile/overworld/PaleWolfEntity`, extends `Wolf`, implements `Enemy`; client
  `PaleWolfRenderer`, `WolfRenderer` plus an `EyesLayer` of `pale_wolf_eyes.png`): a wolf's skeleton. Its skins (wild,
  angry; `tools/wolves.py`) are bone in the pale oak's palest tones with transparent gaps (the model draws cutout, no
  cull): the body's and mane's sides every other row (ribs) around a spine and breastbone, the tail's vertebrae, thin
  legs; a skull with dark sockets, orange eyes, a nose hole and teeth. A zombie wolf dying fully blighted
  (`Undeath#paleKind`; wild, its owner forgotten), or spawned in packs (`MobCategory.MONSTER`): Pale Garden 40 x 2-4
  at any hour, and `#is_forest`/`#is_taiga` (not the Pale Garden) 8 x 2-4 only at night (`MobSpawning#canSpawnPaleWolf`:
  `isNight` 13000-23000, `Monster.isDarkEnoughToSpawn`, on `#wolves_spawnable_on`); never in caves. Wolf goals minus a
  pet's (beg, breed, follow owner, sit), plus a skeleton's `AvoidEntityGoal` of living wolves (`EntityTypes.WOLF` only:
  6 blocks, 1.0/1.2); targets only `HurtByTargetGoal` (alerting its pack) and players. Living wolves hunt it as they
  hunt skeletons (`WolfHuntsPaleWolvesMixin`, Wolf `registerGoals` TAIL, priority 6). Sounds (`CustomSpecialEffects`,
  `sounds.json`, subtitled): ambient every ~200 ticks, a howl (`entity.pale_wolf.howl`: its own `sounds/pale_wolf/howl1-3.ogg` from
  `tools/pale-wolf-howl.py`, 26.4 shipping no wolf howls; volume 1.5) or one time in three a bone rattle (`entity.pale_wolf.rattle`, the skeleton's); a wolf's growl
  when hunting; the skeleton's hurt, death and steps. Its bite blights (`BlightAttacks`), it's Blight-immune and in
  `#minecraft:undead`; `mobInteract` passes (no taming, feeding, dyeing), never a pup, despawns far off as monsters do.
  Drops 0-2 bones. Tests: `UndeadFormsGameTest`, `PaleMobsTest` (skin, sounds), `MobSpawningTest` (night).
- The pale zombie is in `#minecraft:zombies`, the pale creeper, spider and wolf in `#minecraft:undead` (smite, inverted
  healing, the wither spares them); the mushroom spider joins the pale one in `#minecraft:arthropod`.
- **Warden (`WardenMixin`)** — extensively rebalanced, all deliberate per inline doc-comments in the
  source itself (read them for full designer intent): anger toward a target **decays** over time if
  they're sneaking or far away (README-adjacent: rewards "run and hide" play over "just outrun it");
  spawns via trigger (shrieker) get a suspicion boost toward the nearest player instead of starting
  neutral, but cheat/command-spawned Wardens get reduced health (300) and never despawn; hearing/
  sniffing anger gain is heavily distance-scaled (÷5 beyond 24 blocks, ÷2 beyond 16, boosted under 2
  blocks) instead of vanilla's flat amount, and sprinting always maxes it out; sonic booms only fire
  within an 8-block horizontal / 10-block vertical box (vs. vanilla's much larger range) and are on
  a 240-tick internal cooldown after any attack; **ranged non-trident projectiles falloff-damage the
  Warden and are ignored entirely past 16 blocks or below 3 damage** — the "arrow invulnerability"
  mentioned in the class's own comment, meant to force closer, riskier engagement rather than
  peppering it with a bow from max range.
- **`WardenVibrationListenerMixin`** — not re-read this pass; likely tunes the sniff/vibration
  detection radius feeding into the anger system above. Check directly before assuming it's unrelated.
- **Player Zombie** (`mod/mobs/hostile/overworld/PlayerZombieEntity`, extends `Zombie`; client
  `PlayerZombieRenderer`/`PlayerZombieRenderState`, the zombie model wearing the player's skin from the skin cache,
  tinted grey-green in `LivingEntityRendererGazeMixin`): what a player who dies of the Wither while blighted leaves
  (`ServerPlayerBlightedDeathMixin`). Synced `ResolvableProfile`, custom name, worn items (guaranteed drops), held
  items, and a `stash` of the rest of the inventory (saved as `Stash`, dropped by `dropCustomDeathLoot` with an extended
  lifetime). No sun, no drowning, never despawns, its hits blight. Blight-immune; in `#minecraft:zombies`. Game tests in
  `BlightGameTest`.
- **Pale Zombie** (`mod/mobs/hostile/overworld/PaleZombieEntity.java`, renderer under
  `src/client/.../mobs/hostile/overworld/`) — the deep-cave zombie variant (spawn conditions above).
  Its hits convert: a dose of Blight each (`BlightAttacks`, through `MobBlightAttackMixin` on
  `Mob#doHurtTarget`), (it no longer withers). Creakings' hits do too, and their vanish-on-hit gives the
  attacker Blight instead of blindness (`CreakingScares`); other zombies' hits, 1 in 25
  (`VersusSettings.Mushrooms.ZOMBIE_BLIGHT_CHANCE`), in place of their own effect (a husk's hunger is dropped). The snow-biome Frosted
  Zombie was removed in the 26.4 port: vanilla's Frostbite takes its spawns, and vanilla turns
  freezing zombies into frostbites (`Zombie#convertsToWhenFreezing`), which `ZombieMixin` did before.
- **Ice Cube** (`mod/mobs/hostile/overworld/IceCubeEntity.java`, `IceCubeRenderer` in the client set)
  — a `Slime` subclass in blue (the slime model with its own texture) that spawns in vanilla's Ice
  Caves. Its hit freezes (`SlimeMixin.dealDamage` calls `IceCubeEntity.freeze`: vanilla's Freezing
  effect, 30 ticks per size); it is freeze-immune (`freeze_immune_entity_types`, `canFreeze`,
  `canBeAffected`). Its loot is the magma cube's: Frigid Concentrate from cubes of size 2 or more
  (`loot_table/entities/ice_cube.json`), as magma cubes drop magma cream (→ Concentrate of Fire).
- **Skeleton/Spider/Slime/Silverfish/Drowned/Phantom/Enderman/Zombie(+Horde/EventListener/Villager)**
  (`mixin/mobs/hostile/overworld/{SkeletonMixin,SpiderMixin,SlimeMixin,SilverfishMixin,DrownedMixin,
  PhantomMixin,PhantomAccessor,ZombieMixin,ZombieHordeMixin,ZombieEventListenerMixin,ZombieVillagerMixin}.java`,
  `mixin/mobs/hostile/end/EndermanMixin.java`, plus client model mixins
  `mixin/client/mobs/hostile/{SkeletonModelMixin,ZombieModelMixin}.java`) — **not read this pass**.
  README explicitly calls out Skeletons ("tweaked") and Phantoms ("redesigned",
  `PhantomMoveControlRevamp` in `mod/mobs/hostile/overworld/`) as noteworthy; `ZombieHordeMixin`
  is where `VersusSettings.Gameplay.DO_ZOMBIE_SEIGES_OUTSIDE_VILLAGES` is consumed (confirmed via
  grep in `architecture.md`'s settings table) — read it directly for the siege mechanic itself.
  `ZombieMixin` has both `@Overwrite`s (per `architecture.md`'s `@Overwrite` census) — high
  version-bump risk, diff against decompiled vanilla `ZombieEntity` before any MC upgrade.
- **End**: `mixin/mobs/hostile/end/dragon/{ChargingPlayerPhaseMixin,HoldingPatternPhaseMixin,
  StrafePlayerPhaseMixin}.java` + `mod/mobs/hostile/end/DragonManager.java` (dragon phase/behavior
  tweaks) and `EndermanHideAndWaitGoal.java` (a new AI goal, presumably the "hide and wait" behavior
  variant referenced nowhere else in this pass) — not read; check directly for dragon fight changes.
- **Nether**: `mixin/mobs/hostile/nether/{WitherSkeletonMixin,ZombifiedPiglin}.java` (the latter is
  unregistered — see `architecture.md`) and the new `WildfireEntity`/`WildfireShootFireBallsGoal`
  (registered in `ModEntities`, fire-immune, 0.6×1.8 hitbox, spawn egg present but **not** added to
  any biome in `MobSpawning.addCustomSpawns()`, and no `WildfireEntity`-specific `SpawnRestriction`
  seen either) — **Wildfire currently has no natural spawn path**, obtainable only via spawn egg or
  commands; verify this is intentional (a "not yet released"/summon-only mob) before assuming it's
  a bug, but flag it to the maintainer either way since it's a fully-modeled, renderer-equipped mob
  with zero spawn wiring.
- **Illagers/raids**: `mixin/mobs/hostile/illager/{MoveToRaidCenterGoalMixin,PillagerMixin,
  PillagerPatrolMixin,VindicatorMixin,VexMixin,WitchMixin,RaidMixin}.java` +
  `mod/mobs/hostile/overworld/PillagerCaptainBlowHornGoal.java` — not read; `RaidMixin`/
  `PillagerPatrolMixin` are the likely home of `VersusSettings.Gameplay.DO_RAIDS_OUTSIDE_VILLAGES`
  consumption alongside `BadOmenEffectInstanceMixin` (see `brewing-and-potions.md`) — check both
  before changing raid-trigger behavior. Client illager/piglin model mixins exist too
  (`mixin/client/mobs/hostile/{IllagerModelMixin,PiglinModelMixin}.java`).

## Passive mobs
- `mixin/mobs/passive/{PigMixin,PolarBearMixin,SheepMixin}.java` + `mod/mobs/passive/PiggingAroundGoal.java`
  — not read this pass. `PolarBearMixin` almost certainly relates to the README's future/wishlist
  "Black Bears... territorial" idea's nearest-shipped cousin, or an unrelated tweak — verify before
  assuming a connection. Check these directly for current passive-mob behavior changes.
- **Villagers/Wandering Trader** are covered in `villagers-and-trading.md`, not here.

### Copper golem transport (`TransportItemsBetweenContainersMixin`, setting `Gameplay.DO_SMARTER_COPPER_GOLEMS`)
Two `@WrapOperation`s on vanilla's `TransportItemsBetweenContainers` (only the copper golem runs it):
- `getTransportTarget` keeps the chest with the smallest `distToCenterSqr`. While the golem carries something, that value
  is replaced with `GolemChestRanking.cost` (`mod/mobs/passive/`): chest the item fits in because it already holds some
  (most of it first, then nearest), else the nearest empty chest, else the last one looked into (the *furthest* chest,
  full or not). Empty-handed (picking up) stays vanilla's nearest. Nothing is refused here: whatever is chosen, vanilla's
  arrival check decides ("holds the item, or empty? no: mark visited, search again"), so there is no
  `matchesLeavingItemsRequirement` override. Reach, locks, queuing and the visited/unreachable memories are vanilla's.
- `pickupItemFromContainer` takes `min(stack, 16)`; the 16 is dropped, so a golem lifts the whole stack. A chest that
  can't take all of it fills first, then the golem goes on to the next best (the filled one is now "visited").
Pure ranking is unit-tested (`GolemChestRankingTest`); the mixin itself only gets exercised by the `[smoke]` server boot
(applying) and in-game.

## Land pathfinding & mob spawn caps
- **`LandPathfindingMixin`** (`mixin/mobs/`) — not read; likely affects path costs/water-avoidance
  for land mobs. Check directly before touching pathing behavior.
- **`MobSpawnGroupsMixin`** (`@Overwrite` per the census in `architecture.md`) — pairs with
  `MobSpawning.MOB_CAP_MONSTERS = 36` / `MOB_CAP_AMBIENT = 4` (raised from vanilla's defaults,
  presumably consumed here) — re-verify the exact vanilla defaults being replaced before assuming
  the delta; this constant pair alone confirms the caps exist, not their vanilla baseline.

## Entity registration (`ModEntities`)
Central registry for every custom entity type: `SlimeballEntity` (misc group, tiny 0.25×0.25 hitbox
— see `items-and-equipment.md`), `PaleCreeperEntity`, `IceCubeEntity`, `PaleZombieEntity`,
`WildfireEntity` (all monster group, tracking ranges 4-8 blocks — short compared to vanilla hostiles,
worth checking if intentional for a "you need to be close to notice these" cave-mob design or just
unconsidered). Each gets a `FabricDefaultAttributeRegistry.register` call plus (except Slimeball) a
`SpawnEggItem` registered through `RegisteringCustomItems`. **When adding a new custom entity:**
follow this exact four-step pattern (`EntityType.Builder` here, attributes in `ModEntities.onInitialize`,
spawn egg here + registration in `RegisteringCustomItems`, spawn placement in `MobSpawning`) — and
don't forget the `MobSpawning` step, or you'll reproduce the Wildfire gap above.

## Extension recipe: adding a new hostile mob with the shared combat AI
If it uses vanilla's `MeleeAttackGoal` (most `HostileEntity`/`MobEntity` subclasses do by default),
it automatically inherits the wind-up/shield-block/strafe-dodge system above — no extra wiring
needed. Give it a mainhand item if you want it to telegraph the 40-tick "heavy" cooldown instead of
20-tick "quick", and an off-hand `BLOCK`-use item (shield) if you want it to defend. Add its spawn
placement via `MobSpawning.addCustomSpawns()` (a `BiomeModifications.addSpawn` and, if it needs
placement logic beyond biome (light level, depth, block tag), a `SpawnRestriction.register` predicate)
— and register it in `ModEntities` first, per the pattern above.
