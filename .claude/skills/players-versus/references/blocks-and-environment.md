# Custom Blocks, Block Tweaks & Environment Systems

## Custom block registry (`CustomBlocks.onInitialize`)
A large, flat `Block` registration pass (~90 fields) grouped by family; each new block is built with
`getSettings(name, copyFrom)` (copies a vanilla block's base settings, e.g. hardness/sound, then lets
the specific constructor override collision/luminance/piston-behavior). Families present:
- **Torches**: `SmolderingTorchBlock`/`SmolderingWallTorchBlock` (custom classes, luminance 12,
  instant-break, wood sound) and `EXTINGUISHED_TORCH`/`EXTINGUISHED_WALL_TORCH` (plain vanilla
  `TorchBlock`/`WallTorchBlock` subclass reuse, luminance 6) — see `TorchMixin` below for the
  burn-out state machine linking these three states (lit → smoldering → extinguished).
- **Nether wart variants**: `CORRUPTED_WART_PLANT`/`WITHERED_WART_PLANT` (plain `NetherWartBlock`
  copies) — see `brewing-and-potions.md` for `NetherWartMixin`'s state-machine wiring these in.
  `CustomBlockItems.CORRUPTED_WART`/`WITHERED_WART` are their item forms (brewing-stand fuel, per
  `brewing-and-potions.md`).
  Corrupted Wart Powder replaces Fermented Spider Eye (item-replacement pattern, see
  `items-and-equipment.md`).
- **Decorative stone lines**: Cut Lapis, Polished Stone, Dripstone (full slab/stair/wall/pillar/
  brick/polished set), Terracotta (bricks/tiles/chiseled + slab/stair/wall each) — plain vanilla
  block-type reuse (`SlabBlock`/`StairsBlock`/`WallBlock`), no custom behavior; these exist purely
  to round out building-block sets vanilla left incomplete (README: "streamlined... terracotta").
- **Mud/clay moisture system** (`MoistBlock`/`MoistSlabBlock`/`MoistStairsBlock`/`MoistWallBlock`/
  `MoistureConvertableBlock`, `blocks/clays/*`): `CustomMudBlock extends MoistBlock` is Gray Mud/
  Brown Mud's actual behavior class (see below); Gray Clay, Brown Clay(+bricks/slab/stairs/wall),
  Brown Mud(+bricks/slab/stairs/wall) round out the family as plain `MoistBlock`/`MoistSlabBlock`
  variants sharing the wet↔dry conversion mechanic. Gray Clay/Mud and Brown counterparts replace
  vanilla Clay/Mud/Packed Mud/Mud Bricks(+slab/stairs)/Mud Brick Wall via the item-replacement
  pattern (`items-and-equipment.md`) — **the vanilla mud/clay block types themselves are hidden**,
  every gameplay path funnels through this mod's versions.
- **Farming/nature**: `WheatGrassBlock` (+ `WILD_WHEAT`, `CLOVERS` via `CloverBlock`) — README:
  "Crops and sugar cane have gotten a few quality of life improvements. Added wheat grass, too!"
- **`CreeperSporeBlock`** (`CREEPER_SPORE_BLOSSOM`) — tied to Creeper Caves biome per README
  ("creepers that only move when looking away... with new spores!"); not read this pass for its own
  mechanic, check the class directly.
- **Biles**: ~30 `PotionEffectBileBlock` instances, one per concentrate/potion effect — fully
  covered in `brewing-and-potions.md`, registered here.

## Mud/clay moisture mechanic (`MoistBlock`/`MoistureConvertableBlock`/`CustomMudBlock`)
`MoistBlock` (base class, not read line-by-line) pairs a "wet" block with a `dryVersion` and
`cookedVersion` (e.g. Brown Mud → Brown Clay when dried by fire/campfire-adjacent heat, presumably —
verify exact trigger in `MoistBlock`/`MoistureConvertableBlock` directly) and defines
`MIN_FALL_DISTANCE_TO_DRY`. `CustomMudBlock` (Gray/Brown Mud's concrete behavior) layers on:
- **Suffocation/slow-sink hazard**: entities standing in it slow dramatically (`slowMovement`
  multipliers as low as 0.04 vertical), and if their *head* is also inside mud (eye height check),
  moving or sneaking deals 1 damage per second (`CustomDamageSources.getMudSuffocation`) plus a
  flat 1-damage tax every 4 seconds even while still. Jumping lets an entity fight free of it
  (velocity nudges up to 0.3, with layered checks for whether there's a solid/fragile block above).
- **Fragile-block collapse**: standing in mud with a low-hardness block or `PlantBlock` directly
  above (and low enough in the block) breaks that block — mud "swallows" flowers/etc. placed on top.
- **Exemptions** (`canWalkOnWetMud`): any non-player entity fully in fluid, pigs, tameable mobs,
  powder-snow-walkable mobs, or **anyone wearing leather boots** — leather boots are a direct,
  concrete counter-item to the mud hazard, not just flavor.
- **Drying on impact** (`onLandedUpon`): a falling block or a living entity falling far enough
  (`> MIN_FALL_DISTANCE_TO_DRY`, fall-damage-immune entity types exempted below 20 blocks) dries the
  mud to its `dryVersion` on landing and reduces the entity's remaining fall distance by the
  threshold — a "hard impact packs the mud down" mechanic that also softens the landing slightly.
- Cannot be pathfound through by mobs (`canPathfindThrough` false) — AI treats it as a real obstacle
  even though its hitbox is otherwise mostly passable.

## Torch burn-out (`SmolderingTorchBlock`/`SmolderingWallTorchBlock`, `TorchMixin`, `FlintAndSteelItemMixin`)
`TorchMixin` (not read this pass) presumably drives the lit→smoldering transition under the
README's stated conditions ("In deep caves only, or while exposed to rain, regular torches can burn
out"); `FlintAndSteelItemMixin.useOnBlock` (see `items-and-equipment.md`) is the confirmed *relight*
path (smoldering/extinguished → normal torch, 1 durability, `state.getStateWithProperties` preserves
facing/etc.). Check `TorchMixin` directly for the exact rain/depth trigger and the smoldering→
extinguished timing before changing burn-out balance.

## Sleep & lunar cycle (`ServerSleepingMixin`, target `ServerWorld`)
The README's "beds speed up time" mechanic, in full: `checkOnEepyPlayers` (`@Redirect` on
`SleepManager.canSkipNight`) forces the sleep-percentage requirement to 100% when
`DO_SLEEP_OVERHAUL` is on, then **cancels vanilla's own instant time-skip** and instead flips a
mutable flag (`VersusSettings.Gameplay.isFastForwardingTime`) that drives the server's
`TickManager` tick-rate up to `SLEEP_TICK_SPEED` (1020, i.e. ~51× normal — the world ticks much
faster while everyone sleeps, rather than jumping straight to morning). While fast-forwarding
starts, nearby (24×12×24 box) hostile mobs get a chance to notice and path toward each sleeping
player — a deliberate "your shelter gets tested while you fast-forward" risk, not a bug. Waking
anyone (or losing the ability to skip) resets the tick rate to 20 and clears sleep state. **This is
a server-wide tick-rate change** — be aware it affects *everything* ticking (redstone, hoppers,
mob AI elsewhere) while active, not just the sleeping players' immediate area; if you see reports
of "weird timing" bugs specifically during the night-skip, check here first.

## Weather (`ServerWorldWeatherMixin`, target `ServerWorld`)
`tickWeather` (`@Overwrite`) mirrors vanilla's rain/thunder timer state machine almost exactly but
with **retuned duration ranges**: clear weather 24000–72000 ticks (vanilla ~12000–180000, i.e.
narrower and generally shorter-clear), rain 3000–9000, clear-before-thunder 36000–120000, thunder
1000–6000 — re-verify against current vanilla defaults before assuming the exact delta, but the
ranges are deliberately hand-tuned constants here, not vanilla passthrough. **`hasRain(BlockPos)`
is overridden to return `false` whenever the dimension isn't currently thundering** — i.e., ordinary
"raining" no longer wets blocks/extinguishes fire/etc. *unless* it's also thundering; plain rain
(mechanically) only shows through visuals. This is very likely the seam where "fog as a weather
event" (README) replaces ordinary rain — **not confirmed**: the client-side fog renderer
(`mixin/client/environment/visuals/WeatherRendererMixin`) wasn't read in this pass; read it before
relying on this theory, but the `hasRain` override alone is a striking, deliberate behavior change
worth flagging to anyone touching weather.

## Client visuals (not read this pass — check directly before changing)
`mixin/client/environment/visuals/{ClientWorldSkyMixin,DarknessFogRendererMixin,LightmapMixin,
NetherFogRendererMixin,WeatherRendererMixin}.java` — sky color, darkness-effect fog, light-map
(night-vision-adjacent — README: "Improved night vision a bit more" appears many times in git log),
nether fog, and the weather/fog renderer referenced above. `mixin/client/players/HungerSprintingMixin`-adjacent
lightmap tuning is covered in `combat-and-players.md` only for hunger; the *visual* darkness/night
mechanics live here instead.

## Minecarts (`mixin/environment/{MinecartMixin,MinecartExperimentalMixin}`, `SparksParticle`)
Not read this pass. README: "Minecarts can be much faster and throw sparks when making high speed
turns." `SparksParticle` (client) is the visual; `MinecartExperimentalMixin` likely gates behind a
vanilla experimental-rules toggle (name suggests "experimental minecart physics" ruleset). Check
both directly before touching minecart speed/turning.

## Archaeology (`BrushItemMixin`, `SuspiciousBlockEntityMixin`, `BrushesNotRequiredMixin`)
README: "Archeology and the brush made into an actual game mechanic... lots more structures have
suspicious blocks." Confirmed from the settings grep in `architecture.md`:
`VersusSettings.Gameplay.BRUSHING_TICKS_PER_STAGE = 2` (vanilla is 5 — brushing is **2.5× faster**
per stage) is read in both `BrushItemMixin` and `SuspiciousBlockEntityMixin`. `BrushesNotRequiredMixin`
(name suggests suspicious blocks can still be manually dug/looted without a brush, just worse/slower
— verify directly) and the large `data/players-versus/loot_table/archaeology/**` +
`data/minecraft/loot_table/archaeology/**` trees (13 mod-namespace tables) back the "lots more
suspicious blocks" content claim — cross-check against `data-and-assets.md` for the structure-level
placement (`data/*/worldgen/placed_feature/suspicious_sand.json`, structure processor lists).

## Building QoL (placement rules, strokes, reach-around, quick undo)
Rewritten on 26.4 (branch `claude/26-4-worldgen-client-fixes-vz8an5`). Read the classes' javadoc before
changing anything; the split is deliberate:
- `mod/players/building/PlacementRules` (common) — pure rules that turn a placed block: vanilla's state unless a
  rule applies; only noisy-input properties (`AXIS`, facings, `HALF`, `SLAB_TYPE` top/bottom, `ROTATION_16`; plus
  `STAIRS_SHAPE`/`IN_WALL` recomputed) may change, never attachment, waterlogging, double slabs, door/bed halves.
  Order: sneaking → vanilla; stroke's first block (copied, for "turnable" blocks only — not wall-attached ones);
  last placement settles a *guessed* half (slab/stair/trapdoor clicked on the side of a full block); neighbours
  (pillars/stairs rows — the old `BlockItemMixin` scoring, ported — guessed halves, fence gates across fences,
  furnace-like fronts in rows, `ROTATION_16` within two steps); vanilla. Redstone parts (`isFunction`) only follow
  a stroke. `adjust()` is the single entry both mixins call; `acceptable()` is the server's validation.
- `PlacementHintPayload`/`PlacementHints` — the client sends its chosen state (only when not vanilla's) just before
  the use-on packet; the server stores it at once in the receiver (no `execute`, so it precedes the use) and every
  player placement consumes it. A server without the mod → client's `choose()` returns vanilla (`canSend` check).
- Mixins: `players/building/BlockItemMixin` (`getPlacementState` RETURN → `adjust`; `placeBlock` RETURN → recent
  placements + client stroke memory), `StandingAndWallBlockItemMixin` (signs/banners/heads/torches override
  `getPlacementState` without super), `BlockBehaviourMixin` (`getDestroyProgress` RETURN: `RecentPlacements`, a
  block the same player placed ≤40 ticks ago (client) / 60 (server) mines ×4, 5 ticks at slowest, 2 at fastest,
  only with the tool it drops with). Player-null placements (dispensed shulker boxes) stay vanilla — the old mixin
  crashed there.
- Client (`src/client/.../mod/players/building/ClientPlacement`): strokes = right-click held since the stroke's
  first block. Within one: place-on-arrival (`MinecraftClientMixin.handleKeybinds` HEAD zeroes `rightClickDelay`
  when the crosshair reaches a new spot), face lock (only the face the stroke began on), no growth out of the
  stroke's own blocks unless the player moved ≥0.5 along that face since, never the same spot twice; a refused
  repeat does nothing at all (`playersVersus$useOnBlock` in `players/MinecraftClientMixin`). Ladders and
  scaffolding aren't stroked. Reach-around (`DO_BEDROCK_BRIDGING`): on a MISS while looking down, eyes moved 0.5
  back along the facing, ray must hit the block at about feet level; synthetic `BlockHitResult` on its facing side.
  Arrival waits for vanilla's first repeat delay (4 ticks) after the stroke's first block, so a click held a
  moment while the mouse moves still places one block.
- Preview (`PlacementPreview`, client): Fabric `LevelRenderEvents.COLLECT_SUBMITS`; outlines the block a
  right-click would place (white, `RenderTypes.linesTranslucent()`), only when it's turned unlike vanilla or
  reached around to. Draws like vanilla's `LevelRenderer.submitBlockOutline`: translate by block pos − camera
  (`LevelRenderState.cameraRenderState.pos`, via `LevelRendererAccessor`), then
  `SubmitNodeCollector.submitShapeOutline(pose, shape, type, color, width, false)` with vanilla's width
  (`GameRenderState.windowRenderState.appropriateLineWidth`, via `GameRendererInvoker`). What would be placed
  comes from the item's own `getPlacementState` (`BlockItemInvoker`) with `ClientPlacement.previewing` set, so
  nothing is sent or remembered.
- Settings: `VersusSettings.QOL.DO_SMARTER_BLOCK_PLACING`, `DO_BEDROCK_BRIDGING`, `DO_PLACING_STROKES`,
  `DO_PLACEMENT_PREVIEW`, `DO_FAST_UNDO_PLACING`.
- Tests: `src/test/.../mod/players/building/PlacementRulesTest` (rules on a map-backed `View`, mixin handlers
  present). The client logic has no tests — CI doesn't run a client; check in game.
`RayTraceHandler` (`mod/players/`, from CloudG360's BridgingMod) casts the reach-around's shifted ray.

## Other blocks not read this pass
`LadderBlockItem`/`LadderMixin` (README: implies ladder placement changes — replaces vanilla Ladder
per the item-replacement pattern), `ScaffoldingItemMixin`, `CropMixin`/`SugarCaneMixin`/`ShortPlantMixin`
(unregistered — see `architecture.md`)/`TallFlowerMixin`/`MossMixin`/`LeavesMixin` (leaf-and-flying
behavior, README-called-out), `PointedDripstoneMixin` (rarity tweak per git log "Tweaked dripstone
to be a biiiit more rare"), `TntMixin`, `BlockMixin` (likely a grab-bag of small generic block
tweaks — check first if a change doesn't obviously belong elsewhere). All under
`mixin/environment/blocks/` — read the specific file before changing any one of these; this doc
does not re-verify their current behavior.

## Extension recipe: adding a new custom block
1. Register the `Block` instance in `CustomBlocks` (`getSettings(name[, copyFromBlock])`, then the
   specific `Block` subclass constructor) and its `BlockItem` in `CustomBlockItems`.
2. Register in `RegisteringCustomItems` with an appropriate `ItemGroups.*`.
3. Add `assets/players-versus/{blockstates,models/block,textures/block}/<name>.*` and a loot table
   under `data/players-versus/loot_table/blocks/<name>.json` (even a simple self-drop) — see
   `data-and-assets.md` for the validation scripts that catch a missing one.
4. If it participates in the moisture system, extend `MoistBlock`/one of its slab/stair/wall
   siblings rather than reinventing wet/dry state.
5. Add a recipe if craftable, and a lang key.
