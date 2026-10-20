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
- **Block tags decide what blocks are (26.4)**: the game no longer works out "blocks motion" from a block's collision
  shape; `#minecraft:blocks_motion_no_leaves` (and the tags built on it) lists the blocks, and the heightmaps, suffocation,
  fluid flow, dolphin jumps, teleporting and lava fires ask it. **Every solid block of the mod goes in
  `data/minecraft/tags/block/blocks_motion_no_leaves.json`**; `BlockTagAudit` lists the ones missing (a warning when a
  server starts, and the `census` line). Gray Mud and the clays were missing: the ocean floor heightmap skipped the
  mangrove swamp's whole mud and clay layer, so a mangrove tree placed by it stood over the stone beneath and its
  `would_survive` check failed (the cause of "mangrove trees no longer spawn"). Also from 26.4: `#minecraft:dirt` is only
  dirt, coarse dirt and rooted dirt; grass blocks, mud and moss blocks have their own tags, gathered by
  `#substrate_overworld` → `#supports_vegetation` / `#supports_sugar_cane` / `#supports_mangrove_propagule`. Check those,
  not `#dirt`, when a block needs "ground". The mod's soils join them in data: Gray Mud in `#mud`, Brown Mud in `#dirt`,
  the clays in `#supports_mangrove_propagule` and `#supports_small_dripleaf` (the old `small_dripleaf_placeable`).
  The clays also sit in every tag vanilla clay is in (axolotl spawning, azalea, lush ground, sculk, enderman), the muds
  in `#frogs_spawnable_on`, and `needs_stone_tool.json` (it replaces vanilla's: iron, lapis and the crafter only, the
  copper family is mined from the start; the file was named `needs_stone_tools` and did nothing until 26.4's pass).
  Stairs, slabs and walls of the mod belong in `#stairs`/`#slabs`/`#walls` and every stone block in `mineable/pickaxe`
  (clays and muds in `mineable/shovel`); a block missing from one is easy to miss, so check when adding blocks.
  `-Ppv.census=<biomes>` (`WorldgenCensus`, CI job `census`) counts ground and trees in a world's nearest patches of those
  biomes, which plants survive on each floor block, what the heightmaps count, and writes some columns out as they are.
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
changing anything; the split is deliberate. **All of it is client-side except the quick undo**: it works on a
vanilla server, because the client sends only ordinary clicks.
- `mod/players/building/PlacementRules` (common, pure, unit-tested) — which state a placement should have:
  vanilla's unless a rule applies; only noisy-input properties (`AXIS`, facings, `HALF`, `SLAB_TYPE` top/bottom,
  `ROTATION_16`; `STAIRS_SHAPE`/`IN_WALL` follow) may change, never attachment, waterlogging, double slabs, door/bed
  halves. Order: sneaking → vanilla; stroke's first block (copied, for "turnable" blocks only — not wall-attached
  ones); **the last placement** (`follow`: same block, within 3 s — pillars/chains keep their axis next to it on any
  side; stairs keep facing+half along their row or one step up/down their slope, not back to back (a roof's ridge);
  slabs keep their half beside on the same level; trapdoors facing+half along their row); neighbours (pillar/stair
  rows — the old `BlockItemMixin` scoring, ported — guessed halves, fence gates across fences, furnace-like fronts,
  `ROTATION_16` within two steps); vanilla. Redstone parts (`isFunction`) only follow a stroke. `sameTurn()`
  compares turns ignoring neighbour-made shape.
- Client (`src/client/.../mod/players/building/ClientPlacement`) chooses the state, then **clicks so that vanilla
  places it**: tries ways of clicking (the same face at another height, then any face of the target spot itself —
  `replaceClicked` makes the block go there with that face — and fake yaw/pitch: cardinal, ±90 pitch, 16 sign
  steps) in a capped search (`MOST_TRIES`), checking each with the item's own `getPlacementState`
  (`BlockItemInvoker`) with the player's rotation set for it; if none gives the chosen state, the click is vanilla's.
  A fake rotation is sent as `ServerboundMovePlayerPacket.Rot` just before the use and the real one just after (and
  set on the client player around the predicted use, so both sides compute the same). Clicking the spot itself is
  only done when the block under the crosshair is "plain" (no `useWithoutItem`/`useItemOn` of its own, found by
  reflection and cached per class), so a click that would open a chest or flip a lever still does; and only within
  the reach less a margin (`REACH_MARGIN`; the server allows reach + 1). No server mixin turns anything any more
  (the earlier hint packet and `StandingAndWallBlockItemMixin` are gone).
- Strokes = right-click held since the stroke's first block. Within one: place-on-arrival
  (`MinecraftClientMixin.handleKeybinds` HEAD zeroes `rightClickDelay` when the crosshair reaches a new spot, after
  vanilla's first-repeat delay), face lock (only the face the stroke began on, the face the player aimed at, not the
  one clicked for the turn), no growth out of the stroke's own blocks unless the player moved ≥0.5 along that face,
  never the same spot twice; a refused repeat does nothing at all. Ladders and scaffolding aren't stroked.
  Reach-around (`DO_BEDROCK_BRIDGING`): on a MISS while looking down, eyes moved 0.5 back along the facing,
  `RayTraceHandler` ray must hit the block at about feet level; synthetic `BlockHitResult` on its facing side.
- Preview (`PlacementPreview`, Fabric `LevelRenderEvents.COLLECT_SUBMITS`): white outline of what a right-click
  would place, only when it's turned unlike vanilla (and a click can place it) or reached around to; full blocks
  get a mark of their turn (a rod along the axis, a plate on the facing side). Drawn as vanilla's
  `LevelRenderer.submitBlockOutline` does: translate by block pos − camera (`LevelRendererAccessor`), then
  `SubmitNodeCollector.submitShapeOutline` with vanilla's line width (`GameRendererInvoker`).
- Quick undo (the server-side part): `players/building/BlockItemMixin` (`placeBlock` RETURN →
  `RecentPlacements`) and `BlockBehaviourMixin` (`getDestroyProgress` RETURN): a block the same player placed ≤40
  ticks ago (client) / 60 (server) mines ×4, 5 ticks at slowest, 2 at fastest, only with the tool it drops with.
  The client does it only when the server runs the mod (`RecentPlacements.serverAgrees`, from
  `ClientPlayNetworking.canSend(RespawnNearbyPayload.ID)` on join).
- Settings: `VersusSettings.QOL.DO_SMARTER_BLOCK_PLACING`, `DO_BEDROCK_BRIDGING`, `DO_PLACING_STROKES`,
  `DO_PLACEMENT_PREVIEW`, `DO_FAST_UNDO_PLACING`.
- Tests: `src/test/.../mod/players/building/PlacementRulesTest` (rules on a map-backed `View`, mixin handlers
  present). The client logic has no tests — CI doesn't run a client; check in game.
`RayTraceHandler` (`mod/players/`, from CloudG360's BridgingMod) casts the reach-around's shifted ray.

## Leaves you push through (`LeafMovement`, `LeafLitter`, `LeafReach`, `FallenLeavesBlock`, `LeavesMixin`, `LeafMovementMixin`, `LeavesMoveControlMixin`, client `LeavesView`)
Design and numbers: `docs/superpowers/specs/2026-10-18-leaf-overhaul-design.md`, then
`docs/superpowers/specs/2026-10-19-fallen-leaves-design.md` (fallen leaves, instant decay, swimming, the view; it
supersedes the first spec's litter and soft landings). Tunables: `VersusSettings.Leaves`.
- **Who passes**: living things except `Animal`s and `Spider`s (`LeafMovement.passes`). Everything else, and every
  no-context query, sees a full block.
- **When a leaf is solid** (`LeafMovement.isSolid`, the collision shape): the passer is above it, not sneaking, not
  gliding, fall distance ≤ 4 (`BREAK_THROUGH_FALL_DISTANCE`), the same rule on the server as on the client (the server looks for the block holding a
  player up, and logs "standing on air" when it finds none). A placement context is open: 26.4 flags as placement both
  block placing and the server's check of each move a player's client already made (`getPreMoveCollisions`, through
  `CollisionContext.withPosition`), and a solid leaf there pulls the player back out of every leaf they step into. Never
  read velocity or change the entity there: shapes are asked many times a tick, on both sides.
- **Movement** goes through `handleOnClimbable`'s return (`LeafMovement.adjust`), the motion after input and before
  `move`. It applies a horizontal speed cap by weighted immersion (legs ×3), vertical drag on the swept box, and
  climbing and descending with a foothold (leaves at the feet). It runs only where the entity is simulated (the client
  for its player, the server for mobs).
- **Falls**: `checkFallDamage` at its head caps the fall distance at the free-fall height of the tick's speed while in
  leaves (`cushionFall`). The server counts a player's fall via `Entity.doCheckFallDamage` → `checkFallDamage`, so this
  covers players. Leaves are deliberately not in `#fall_damage_resetting`.
- **Mob jumping**: `MoveControl.tick` jumps a mob whose feet are inside a no-context-solid block, so
  `LeavesMoveControlMixin` gives it the mob's own leaf shape there. Without it, mobs hop through hedges and climb them.
- **Swimming**: a `@WrapOperation` on `travelInWater`'s `move` drags a swimmer in leaves (`LeafMovement.swim`,
  `SWIM_DRAG`) and keeps the drag in its speed. `handleOnClimbable` isn't on the water path, so walking's cap never
  reached swimmers.
- **Litter**: `entityInside` (server) sheds by speed², `randomTick` sheds while decaying. A segment lands straight
  down through leaves on ground it can survive on, joining any leaf pile (fallen leaves or leaf litter, up to four;
  petals only petals). Tags `players-versus:sheds_fallen_leaves` (the green broad-leaved trees),
  `sheds_leaf_litter` (the poplars) and `sheds_pink_petals` (cherry); spruce and pale oak shed nothing. Litter doesn't
  soften a landing.
- **Fallen leaves** (`players-versus:fallen_leaves`, `FallenLeavesBlock extends LeafLitterBlock` + `PERSISTENT`):
  vanilla's leaf litter models tinted with `BlockTintSources.foliage()`. Natural piles brown into `LEAF_LITTER` on a
  random tick (`FALLEN_LEAVES_BROWNING_CHANCE`); placed ones are persistent. Shears or Silk Touch drop them, anything
  else leaf litter (loot table). The item's `COMPOSTABLE` is built directly (`minecraft:compostable/low`): item
  components bind late, so copying leaf litter's at class load reads nothing.
  Speed is measured as vanilla's sweet berry bush does: `getKnownMovement()` for a client-authoritative player (zeroed
  by `handleClientTickEnd` on a client tick without movement), `position() - oldPosition()` for anything else. Never
  the old position for a player: `commonTick` catches it up in the level tick, and a player's block effects run later,
  in its connection tick (`tickPlayer` → `doTick`), so it always reads nothing.
- **Decay** (`LeavesMixin.tick`, an `@Overwrite`): a natural leaf whose neighbours say its hold weakened searches for
  an own log (`LeafReach.searchDistance`, six steps through natural leaves of its kind) and, finding none, decays at
  once (drops, a litter chance, a particle puff); a felled crown falls a block a tick. Don't trust remembered
  distances for instant decay: vanilla grows some leaves a step too far, and a leaf would die before its neighbour
  corrects. `LeafReach` holds the rules (`LeavesMixin.playersVersus$isOwnLog` and both distance methods ask it) — own log tags (built by id: 26.4's `BlockTags` names only `JUNGLE_LOGS`,
  `PALE_OAK_LOGS` and `LOGS`), oak leaves also take jungle logs (jungle bushes), azalea's two leaves are one kind,
  vanilla's six-block reach. **Never key static maps by `Blocks.*` in a mixin of a block class**: its static init runs
  while `Blocks` is still being built, so the keys are null (the old reach maps silently never worked).
- **Tests**: `LeafMovementTest` (math); `LeavesGameTest` (husks walking, climbing, falling, sneaking, swimming;
  litter and fallen leaves; decay, with two groves growing all 18 vanilla trees and checking none loses a leaf), in
  `leaves_room`, `leaves_tower` and `leaves_grove` (48 x 40 x 48, its grass floor only) as SNBT in
  `src/gametest/resources/.../gametest/structure/`.
- **Client**: `LeavesRustling` uses `LeafMovement.measure` for every passer within 32 blocks; `LeavesView` (a HUD
  element after `MISC_OVERLAYS`) frames the screen while the camera is in leaves, as powder snow's frost does: our own
  `textures/misc/leaves_outline.png` (grey, drawn by `tools/leaves-outline.py`) stretched over the screen, in the leaf's
  tint, or the average colour of its texture for untinted leaves.

## Tree blocks (`TreeBlock`, `TreeBlocks`, `TreeFeatureMixin`; spec `docs/superpowers/specs/2026-10-19-trees-design.md`)

- **What**: `players-versus:<wood>_tree` for oak, spruce, birch, jungle, acacia, cherry, dark oak, pale oak, mangrove and
  poplar (`CustomBlocks.TREE_BLOCKS`, keyed by the vanilla log). A `RotatedPillarBlock` copied from its log, with six
  booleans `north`..`down` (`PipeBlock.PROPERTY_BY_DIRECTION`): true shows bark, false the cut (the stripped log).
- **Faces are set once**, never in `updateShape`: as a tree grows (`TreeBlocks.setFaces` at the end of
  `TreeFeature.place`) or in creative placement (`getStateForPlacement`). Cut against a `TreeBlock`, `#logs` or a sturdy
  face, bark otherwise (`TreeBlock.withFaces`). `rotate`/`mirror` turn the faces as a huge mushroom block's.
- **Growing trees**: `TreeFeatureMixin` wraps `doPlace`'s trunk setter and foliage setter (a poplar's crown,
  `PoplarFoliagePlacer`, grows a ring of logs round its trunk) with `TreeBlocks.growing`, so each log with a tree block
  of its own goes down as it, axis kept, and `@Share`s the positions with a `RETURN` inject that sets the faces.
  `VersusSettings.Trees.DO_TREE_BLOCKS` turns it off. Fallen-tree features, structures and placed logs stay vanilla.
- **Drops** the vanilla log (loot tables). Creative only: an item per wood after its log in the natural blocks tab.
- **Tags**: in its wood's `#minecraft:<wood>_logs` (so `logs_that_burn`, `logs`, mineable with axe, creaking hearts),
  and listed by name in `overworld_natural_logs`, `prevents_nearby_leaf_decay`, `supports_cocoa` (jungle),
  `blocks_motion_no_leaves` and `mangrove_logs_can_grow_through`.
- **Assets and data are generated** by `tools/tree-blocks.py` (run from the repository root): per wood a 36-part
  multipart blockstate of one-face models (`models/block/tree_face/*`, vanilla's `cube_column` faces plus the lying
  log's half-turned up face), turned as vanilla's log blockstate turns them, so an uncut tree looks exactly vanilla;
  the item model (the vanilla log's), the loot table and the tags. Edit the script, not its output.
- **Tests**: `TreesGameTest` (both groves grow tree blocks and no vanilla logs, every face by the rule; faces stay as
  set; creative placement; drops; tags and cocoa). `LeavesGameTest`'s groves check the leaves still live by them.
- **Felling (in progress, plan `docs/superpowers/plans/2026-10-20-tree-felling.md`)**: `Felling` decides from the blocks
  alone, changing nothing: `afterCut` gives each group of tree blocks (joined by faces, edges or corners, creaking hearts
  included) a cut leaves standing on nothing but leaves or air, with its crown (each leaf nearer it than any log left
  standing, through leaves of its kind, six steps) and which way it falls (its heavy side past `LEAN`, else away from
  the cutter); `cutTimeFactor` is 1 + `CUT_TIME_SCALE` x sqrt(what would fall). Nothing calls it in game yet: the cut's
  time, the creak, the falling blocks, landing and damage are the plan's next tasks. Tests: `FellingGameTest`.

## Other blocks not read this pass
`LadderBlockItem`/`LadderMixin` (README: implies ladder placement changes — replaces vanilla Ladder
per the item-replacement pattern), `ScaffoldingItemMixin`, `CropMixin`/`SugarCaneMixin`/`ShortPlantMixin`
(unregistered — see `architecture.md`)/`TallFlowerMixin`/`MossMixin`, `PointedDripstoneMixin` (rarity tweak per git log "Tweaked dripstone
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
