# Fallen Leaves, Instant Decay and Being in Leaves Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Green leaves shed a biome-tinted fallen leaves block that browns into leaf litter, leaves cut off from their tree fall at once, leaves underwater drag swimmers, a leafy frame shows while the camera is in leaves, and leaf litter no longer softens landings.

**Architecture:** One new block, `FallenLeavesBlock` (vanilla `LeafLitterBlock` plus `persistent` and a random-tick browning), reusing vanilla's litter models and texture with a foliage tint. Shedding (`LeafLitter`) picks what to drop by three block tags. Instant decay lives in `LeavesMixin`'s existing `tick` overwrite; swimming drag in `LeafMovement` through a `travelInWater` hook; the leafy frame is a client HUD element.

**Tech Stack:** Fabric, Minecraft 26.4-snapshot-3 (Mojang names), Java 25, Fabric API 0.162.2+26.4, MixinExtras, Fabric GameTest.

**Spec:** `docs/superpowers/specs/2026-10-19-fallen-leaves-design.md`

## Global Constraints

- No local build here (Java 25 and Mojang's maven are unreachable): each task is verified by pushing the branch
  `claude/leaf-block-overhaul-y76idt` and reading CI. `build` must pass; `gametest` must print `All N required tests
  passed`. Signatures not yet seen go in `.github/api-focus.txt` with `[api-diff]` in the commit message; the focus job
  prints them (call summaries, never decompiled code).
- Commit messages end with the two trailer lines `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` and
  `Claude-Session: https://claude.ai/code/session_018tbiBWYYFwejgzDUu6x5hG`.
- Mixin handlers are named `playersVersus$...`; overrides a mixin adds are `public`; tunables live in
  `VersusSettings.Leaves`.
- Values: `FALLEN_LEAVES_BROWNING_CHANCE = 0.057`, `DO_INSTANT_LEAF_DECAY = true`, `DO_LEAVES_VIEW = true`,
  `SWIM_DRAG = 0.3`; `DECAY_LITTER_CHANCE` stays 0.25.
- Block id `players-versus:fallen_leaves`, name "Fallen Leaves"; block tint `BlockTintSources.foliage()`; item tint
  constant `-12012264` (#48B518); map colour `MapColor.PLANT`; flammable 60/100; compostable 0.3; creative tab: after
  `Items.LEAF_LITTER` in `CreativeModeTabs.NATURAL_BLOCKS`.
- Tags: `players-versus:sheds_fallen_leaves` = oak, birch, jungle, acacia, dark oak, mangrove, azalea and flowering
  azalea leaves; `players-versus:sheds_leaf_litter` = red, orange and yellow poplar leaves; `players-versus:sheds_pink_petals`
  = cherry leaves. Spruce and pale oak leaves are in none.
- Drops: terms `minecraft:tool/can_shear` or `minecraft:tool/can_silk_touch` give `players-versus:fallen_leaves`, else
  `minecraft:leaf_litter`; count = `segment_amount`; then `minecraft:explosion_decay` (26.4 loot syntax: `"condition"`,
  `"modifier"`, `"type"`, as vanilla's `blocks/leaf_litter.json`).

## Review Focus

- A natural leaf the mod's reach rules reject decays the moment anything updates it, so a tree could go bald when a
  chunk loads or a block beside it changes: the tree test must cover the poplars, which the mod's own dappled trees
  (`players-versus:dappled_trees`) are made of (Task 4).
- A shed segment landing on a player's placed pile must add to it and keep it placed, not start it browning (Task 3).
- Breaking a placed pile by hand gives leaf litter, as a natural one does: persistence must not change drops (Task 2).
- A waterlogged leaf that decays at once must leave its water behind (Task 4).
- A full pile (four segments) takes no fifth from a shed leaf, of any kind (Task 3).

---

### Task 1: Remove the litter landing cushion; ask for the remaining signatures

**Files:**
- Modify: `src/main/java/frootloops/versus/mod/environment/blocks/leaves/LeafLitter.java` (delete `cushionLanding`, its imports, and the class javadoc's "a pile of it softens a landing")
- Modify: `src/main/java/frootloops/versus/mixin/environment/blocks/LeafMovementMixin.java:38-47` (drop the `LeafLitter.cushionLanding` call and its import; javadoc: "and landing in leaf litter takes some of it off" goes)
- Modify: `src/main/java/frootloops/versus/VersusSettings.java:89-90` (delete `LITTER_CUSHION_PER_SEGMENT` and its comment)
- Modify: `src/gametest/java/frootloops/versus/gametest/LeavesGameTest.java` (delete `aPileOfLitterSoftensALanding`)
- Modify: `.github/api-focus.txt`

**Interfaces:**
- Produces: nothing new.

- [ ] **Step 1: Delete the cushion code, setting and test listed above**
- [ ] **Step 2: Put these lines under the focus file's header**

```
member net.minecraft.world.item.component.Compostable (.)
member net.minecraft.core.component.DataComponents (COMPOST)
member net.minecraft.client.renderer.block.BlockStateModelSet (.)
member net.minecraft.client.renderer.block.dispatch.BlockStateModel (particle|sprite|Sprite)
member net.minecraft.client.resources.model.sprite.AtlasManager (get|Atlas)
member net.minecraft.client.renderer.texture.TextureAtlas (getSprite)
data tags/block/replaceable.json
data tags/block/replaceable_by_trees.json
data tags/block/inside_step_sound_blocks.json
data tags/block/combination_step_sound_blocks.json
data tags/block/sword_efficient.json
data tags/block/mineable/hoe.json
```

- [ ] **Step 3: Commit and push**

```bash
git add -A && git commit -m "Leaves: litter no longer softens a landing [api-diff]" && git push -u origin claude/leaf-block-overhaul-y76idt
```

- [ ] **Step 4: Verify on CI**

Expected: `build` passes; `gametest` prints `All 26 required tests passed`; the focus job prints the signatures and
tag files above. Note which vanilla block tags list `minecraft:leaf_litter`.

---

### Task 2: The fallen leaves block

**Files:**
- Create: `src/main/java/frootloops/versus/mod/environment/blocks/leaves/FallenLeavesBlock.java`
- Modify: `src/main/java/frootloops/versus/mod/environment/CustomBlocks.java` (field `FALLEN_LEAVES`; register in `onInitialize`; `FlammableBlockRegistry.getDefaultInstance().add(FALLEN_LEAVES, 60, 100)`)
- Modify: `src/main/java/frootloops/versus/mod/environment/CustomBlockItems.java` (field `FALLEN_LEAVES`)
- Modify: `src/main/java/frootloops/versus/mod/items_and_effects/RegisteringCustomItems.java` (register the item; `CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> output.insertAfter(Items.LEAF_LITTER, CustomBlockItems.FALLEN_LEAVES))`)
- Modify: `src/main/java/frootloops/versus/VersusSettings.java` (`FALLEN_LEAVES_BROWNING_CHANCE`)
- Modify: `src/client/java/frootloops/versus/mod/environment/CustomBlocksClient.java` (`BlockColorRegistry.register(List.of(BlockTintSources.foliage()), CustomBlocks.FALLEN_LEAVES)`)
- Create: `src/main/resources/assets/players-versus/blockstates/fallen_leaves.json` (vanilla `leaf_litter` multipart, every `"model"` pointing at `minecraft:block/leaf_litter_1`..`_4` unchanged)
- Create: `src/main/resources/assets/players-versus/items/fallen_leaves.json` (`minecraft:model` of `players-versus:item/fallen_leaves`, tints `[{"type": "minecraft:constant", "value": -12012264}]`)
- Create: `src/main/resources/assets/players-versus/models/item/fallen_leaves.json` (parent `minecraft:item/generated`, `layer0` `minecraft:block/leaf_litter`)
- Modify: `src/main/resources/assets/players-versus/lang/en_us.json` (`"block.players-versus.fallen_leaves": "Fallen Leaves"`)
- Create: `src/main/resources/data/players-versus/loot_table/blocks/fallen_leaves.json` (an `alternatives` entry per Global Constraints, `random_sequence` `players-versus:blocks/fallen_leaves`)
- Modify: the vanilla block tags Task 1 found listing `minecraft:leaf_litter`, as `data/minecraft/tags/block/<tag>.json` with `"replace": false` and `players-versus:fallen_leaves`
- Test: `src/gametest/java/frootloops/versus/gametest/LeavesGameTest.java`

**Interfaces:**
- Consumes: `VersusSettings.Leaves.FALLEN_LEAVES_BROWNING_CHANCE`.
- Produces: `CustomBlocks.FALLEN_LEAVES` (a `FallenLeavesBlock`), `CustomBlockItems.FALLEN_LEAVES` (a `BlockItem`),
  `FallenLeavesBlock.PERSISTENT` (= `BlockStateProperties.PERSISTENT`), and `FallenLeavesBlock` overriding
  `getStateForPlacement(BlockPlaceContext)` (persistent true), `isRandomlyTicking(BlockState)` (not persistent) and
  `randomTick(...)` (browning: `Blocks.LEAF_LITTER` with the same `SegmentableBlock.AMOUNT` and `LeafLitterBlock.FACING`).

- [ ] **Step 1: Write the game tests** (structure `TOWER`, grass floor at y 1 as in `decayingLeavesLeaveLitter`)

```java
/** Natural fallen leaves brown into leaf litter, keeping segments and facing; placed ones never do. */
public void fallenLeavesBrownWithTime(GameTestHelper helper)
// natural FALLEN_LEAVES (AMOUNT 3, FACING EAST, PERSISTENT false) at (2, 2, 2) and persistent at (4, 2, 2)
// 300 x helper.randomTick on each, then:
// assertBlockPresent(Blocks.LEAF_LITTER, (2, 2, 2)); AMOUNT == 3; FACING == EAST
// assertBlockPresent(CustomBlocks.FALLEN_LEAVES, (4, 2, 2)); AMOUNT == 3

/** Shears and Silk Touch keep fallen leaves, anything else gives leaf litter, a pile's worth either way. */
public void fallenLeavesDropThemselvesOnlyToShearsAndSilkTouch(GameTestHelper helper)
// state: FALLEN_LEAVES AMOUNT 3; drops = Block.getDrops(state, level, abs(2,2,2), null, null, tool)
// tool new ItemStack(Items.SHEARS)       -> one stack, CustomBlockItems.FALLEN_LEAVES x3
// tool diamond pickaxe + Silk Touch I    -> one stack, CustomBlockItems.FALLEN_LEAVES x3
//   (Silk Touch holder: EnchantRegistryHelper.getRegistryEntry(level, Enchantments.SILK_TOUCH))
// tool ItemStack.EMPTY                   -> one stack, Items.LEAF_LITTER x3
// same bare-hand check on the state with PERSISTENT true -> Items.LEAF_LITTER x3

/** Fallen leaves a player places are theirs: they don't brown. */
public void placedFallenLeavesArePersistent(GameTestHelper helper)
// player = helper.makeMockPlayer(GameType.SURVIVAL); ground = helper.absolutePos(new BlockPos(2, 1, 2))
// context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(CustomBlockItems.FALLEN_LEAVES),
//     new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false))
// CustomBlocks.FALLEN_LEAVES.getStateForPlacement(context) has PERSISTENT true and AMOUNT 1
```

- [ ] **Step 2: Implement the block, its registration, assets, loot table, tags and client tint as listed**

Properties: `BlockBehaviour.Properties.ofFullCopy(Blocks.LEAF_LITTER)` with `.mapColor(MapColor.PLANT)` and the
id `players-versus:fallen_leaves`. Item properties carry the compostable component Task 1 printed (chance 0.3).
Browning: `if (!state.getValue(PERSISTENT) && random.nextDouble() < FALLEN_LEAVES_BROWNING_CHANCE)` set the litter
state with `Block.UPDATE_ALL`.

- [ ] **Step 3: Commit, push, verify on CI**

Expected: `build` passes; `gametest` prints `All 29 required tests passed`, including the three above.

---

### Task 3: Shedding gives fallen leaves

**Files:**
- Modify: `src/main/java/frootloops/versus/mod/environment/blocks/leaves/LeafLitter.java`
- Create: `src/main/resources/data/players-versus/tags/block/sheds_fallen_leaves.json`
- Modify: `src/main/resources/data/players-versus/tags/block/sheds_leaf_litter.json` (the three poplars only)
- Test: `src/gametest/java/frootloops/versus/gametest/LeavesGameTest.java`

**Interfaces:**
- Consumes: `CustomBlocks.FALLEN_LEAVES`.
- Produces: `LeafLitter.SHEDS_FALLEN_LEAVES` (`TagKey<Block>`); `LeafLitter.shedState(BlockState leaves)` returning the
  fallen leaves, leaf litter or pink petals default state, or null; `LeafLitter.shed(...)` unchanged in signature, now
  joining any leaf pile.

- [ ] **Step 1: Rewrite `litterFallsThroughTheCanopyAndPilesUp` as `shedLeavesFallThroughTheCanopyAndPileUp`**

```java
// four oak sheds from (1, 7, 1) -> FALLEN_LEAVES at (1, 2, 1), AMOUNT 4; a fifth returns false
// red poplar shed from (3, 6, 1)  -> Blocks.LEAF_LITTER at (3, 2, 1)
// cherry shed from (5, 6, 1)      -> Blocks.PINK_PETALS at (5, 2, 1)
// spruce shed from (6, 6, 1)      -> returns false, (6, 2, 1) is air
// oak shed onto LEAF_LITTER AMOUNT 2 at (8, 2, 1)                -> LEAF_LITTER AMOUNT 3
// oak shed onto FALLEN_LEAVES AMOUNT 1, PERSISTENT true at (9, 2, 1) -> AMOUNT 2, PERSISTENT still true
// oak shed onto water at (7, 1, 3), and onto short grass at (9, 2, 3) -> false (as before)
// oak shed onto a full LEAF_LITTER (AMOUNT 4) at (10, 2, 1)     -> false
```

- [ ] **Step 2: Count fallen leaves where the tests counted oak litter**

`decayingLeavesLeaveLitter` and `litterAfterAWhileIn` (in `movingThroughLeavesKnocksLitterLoose`) count
`CustomBlocks.FALLEN_LEAVES` segments under oak leaves instead of `Blocks.LEAF_LITTER`.

- [ ] **Step 3: Implement the tag and the joining rule**

`shedState`: `SHEDS_FALLEN_LEAVES` -> `CustomBlocks.FALLEN_LEAVES.defaultBlockState()`, then the two existing tags. In
`shed`, a falling leaf (fallen leaves or leaf litter) adds a segment where the block below is a `FallenLeavesBlock` or
`Blocks.LEAF_LITTER`; petals only where it is `Blocks.PINK_PETALS`. `addSegment` keeps every other property.

- [ ] **Step 4: Commit, push, verify on CI**

Expected: `All 29 required tests passed`.

---

### Task 4: Instant leaf decay

**Files:**
- Modify: `src/main/java/frootloops/versus/mixin/environment/blocks/LeavesMixin.java` (the `tick` overwrite; the log
  matching in `getDistanceFromLog_OnUpdate` and `getUpdatedDistance`)
- Modify: `src/main/java/frootloops/versus/VersusSettings.java` (`DO_INSTANT_LEAF_DECAY`)
- Create: `src/gametest/resources/data/players-versus-gametest/gametest/structure/leaves_grove.snbt` (48 x 40 x 48, air, generated as the other two)
- Test: `src/gametest/java/frootloops/versus/gametest/LeavesGameTest.java`

**Interfaces:**
- Consumes: `LeafLitter.decay(ServerLevel, BlockPos, BlockState, RandomSource)`.
- Produces: `private boolean playersVersus$isOwnLog(BlockState log)` in `LeavesMixin` (own log type per `LOG_MAP`, plus
  `JUNGLE_LOG` and `JUNGLE_WOOD` for `OAK_LEAVES`; any `#logs` for leaves without a matching log), used by both
  distance methods.

- [ ] **Step 1: Write the game tests**

```java
/** Cutting a tree's trunk drops its natural crown within a few ticks; placed leaves stay; waterlogged ones leave water. */
public void aFelledTreeDropsItsCrownAtOnce(GameTestHelper helper)   // ROOM
// oak logs (3, 1..4, 3); natural oak leaves at y 3..5 around them with exact DISTANCE
//   (orthogonal to a log 1, diagonal 2; y 5: centre 1, edges 2, corners 3);
//   (4, 4, 4) PERSISTENT true; (2, 3, 3) WATERLOGGED true
// set the four logs to air; runAfterDelay(10):
//   no natural OAK_LEAVES left in x 2..4, y 3..5, z 2..4; OAK_LEAVES at (4, 4, 4); water at (2, 3, 3)

/** Every tree keeps all its leaves through an update of each: the reach rules never doom a natural tree's own leaves. */
public void everyTreeKeepsItsLeaves(GameTestHelper helper)          // leaves_grove
// grass floor y 0; place in a 3 x 3 grid 16 apart, origins at (8 + 16i, 1, 8 + 16j):
//   registryAccess().lookupOrThrow(Registries.FEATURE).getOrThrow(key).value()
//     .place(level, level.getChunkSource().getGenerator(), RandomSource.create(42), origin)
// keys, two tests of nine: OAK, FANCY_OAK, BIRCH, SPRUCE, PINE, ACACIA, DARK_OAK, SWAMP_OAK, JUNGLE_BUSH /
//   JUNGLE_TREE, MEGA_SPRUCE, AZALEA_TREE, MANGROVE, CHERRY, PALE_OAK, RED_POPLAR, ORANGE_POPLAR, YELLOW_POPLAR
//   (mud instead of grass under MANGROVE's spot); each place(...) must return true, or fail naming the tree
// count natural leaves per tree; scheduleTick(pos, block, 1) on each; runAfterDelay(5): counts unchanged,
//   failure names the tree and how many fell
```

- [ ] **Step 2: Implement**

In `tick`: when not persistent, `DO_INSTANT_LEAF_DECAY` and the new distance is 7, decay now:
`LeafLitter.decay(world, pos, state, random)`, `Block.dropResources(state, world, pos)`,
`world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x + 0.5, y + 0.5, z + 0.5, 6, 0.25, 0.25, 0.25, 0.0)`,
`world.removeBlock(pos, false)`; otherwise set the distance as now. Any tree the test catches gets its pairing or reach
fixed in `playersVersus$isOwnLog` or `LEAVES_WITH_MAX_DISTANCE`, and listed in the spec's decay section.

- [ ] **Step 3: Commit, push, verify on CI**

Expected: `All 32 required tests passed` (one felled tree, two groves of nine).

---

### Task 5: Leaves drag swimmers

**Files:**
- Modify: `src/main/java/frootloops/versus/mod/environment/blocks/leaves/LeafMovement.java`
- Modify: `src/main/java/frootloops/versus/mixin/environment/blocks/LeafMovementMixin.java`
- Modify: `src/main/java/frootloops/versus/VersusSettings.java` (`SWIM_DRAG`)
- Modify: `docs/superpowers/specs/2026-10-19-fallen-leaves-design.md` (Swimming: a drag, not the walking cap; see below)
- Test: `src/gametest/java/frootloops/versus/gametest/LeavesGameTest.java`

**Interfaces:**
- Produces: `public static Vec3 LeafMovement.swim(LivingEntity entity, Vec3 motion)`: `motion` with x and z times
  `1 - SWIM_DRAG * body` and y times `1 - SWIM_DRAG * swept`, under the same exclusions as `adjust`.

Why a drag: swimming speed (about 0.1 blocks a tick) is already below the walking cap fully inside leaves (0.12), so
the cap would never slow a swimmer. A 30% drag per tick fully inside, as gliders get, slows a swimmer to about half,
close to what leaves do to a walker.

- [ ] **Step 1: Write the game test**

```java
/** Leaves underwater hold back a swimmer as they hold back a walker. */
public void leavesUnderwaterHoldBackASwimmer(GameTestHelper helper)  // ROOM, 100 ticks
// stone floor; water source y 1..2 over x 1..13 in lanes z 1..2 and z 5..6; in the second lane x 4..12,
//   persistent oak leaves WATERLOGGED true instead of water
// husks at (1.5, 1, 1.5) and (1.5, 1, 5.5), driven east each tick like aHedgeSlowsAWalkButDoesntStopIt
// after 40 ticks: leaves lane < 0.85 x open lane, and > 0.2 x open lane; log both distances
```

- [ ] **Step 2: Implement `swim` and the hook**

`@WrapOperation(method = "travelInWater", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))`:
`Vec3 dragged = LeafMovement.swim(self, motion)`; if it differs, `self.setDeltaMovement(dragged)`; then
`original.call(self, type, dragged)`. Spec: replace the Swimming paragraph with the drag above.

- [ ] **Step 3: Commit, push, verify on CI**

Expected: `All 33 required tests passed`; the log shows the two distances.

---

### Task 6: The leafy view

**Files:**
- Create: `src/client/java/frootloops/versus/mod/environment/LeavesView.java`
- Modify: `src/client/java/frootloops/versus/VersusModClient.java` (`LeavesView.register()` after `LeavesRustling.register()`)
- Modify: `src/main/java/frootloops/versus/VersusSettings.java` (`DO_LEAVES_VIEW`)

**Interfaces:**
- Produces: `LeavesView.register()`; a `HudElement` attached with
  `HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Identifier.fromNamespaceAndPath(MOD_ID, "leaves_view"), ...)`.

- [ ] **Step 1: Implement**

Each client tick (`ClientTickEvents.END_CLIENT_TICK`): if `DO_LEAVES_VIEW`, not spectating, and the block at
`gameRenderer.getMainCamera().blockPosition()` is a `LeavesBlock`, remember that state and position and ease the
frame's strength toward 1, else toward 0, by 0.25 a tick. `extractRenderState(GuiGraphicsExtractor, DeltaTracker)`:
nothing at strength 0; else take the leaves' particle sprite (from `getModelManager().getBlockStateModelSet()`, per the
signatures Task 1 printed) and their colour `getBlockColors().getTintSource(state, 0).colorInWorld(state, level, pos)`
(white when there is no tint source), and draw tiles a quarter of the screen height, two rings deep along every edge,
with `blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, size, size, ARGB.color(alpha, colour))`, alpha
`0.85 * strength` on the outer ring and `0.45 * strength` on the inner.

- [ ] **Step 2: Commit, push, verify on CI**

Expected: `build` passes (the client source set compiles); tests unchanged. The frame itself can't be checked
headless: say so in the report.

---

### Task 7: Docs and cleanup

**Files:**
- Modify: `.claude/skills/players-versus/references/blocks-and-environment.md` (Leaves section: fallen leaves, the three
  tags, instant decay and `playersVersus$isOwnLog`, swimming drag, the leafy view; no cushion)
- Modify: `.github/api-focus.txt` (back to its header)

- [ ] **Step 1: Update the reference and clear the focus file**
- [ ] **Step 2: Commit, push, verify on CI**

Expected: `build` passes; `All 33 required tests passed`.
