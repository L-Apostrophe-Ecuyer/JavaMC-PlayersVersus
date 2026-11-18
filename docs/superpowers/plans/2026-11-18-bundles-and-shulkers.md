# Bundles and shulker boxes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A 5 × 4 bundle grid; a shulker box whose tooltip is its 27 slots, scrolled through, taken from and put into
in the inventory; an optional peek at placed shulker boxes, scroll-selected, taken from with a use.

**Architecture:** Common: `BundleGrid` (the count to show, read by a `BundleContents` mixin), `ShulkerItems` (a box's 27
slots: put in, take out), `ShulkerSelection` (the wheel's steps over non-empty slots), the selection kept on
`ItemContainerContents` (a mixin field, as `BundleContents` keeps its own), the clicks in an `Item` mixin, `ShulkerTooltip`
(the tooltip image), `ShulkerPeeking` (what the server shows and lets be taken), four payloads. Client: `WideBundleTooltip`,
`ClientShulkerTooltip` and `ShulkerGrid` (drawing), `ShulkerMouseActions`, `ShulkerPeek` (asking, the HUD, the key, the
option file), mixins on the container screen, the mouse wheel and the use of a block.

**Tech Stack:** Java 25, Fabric (Minecraft 26.4-snapshot-4, Fabric API rendering, key mapping, networking), JUnit 5,
Fabric game tests, the client game test gallery.

**Spec:** `docs/superpowers/specs/2026-11-18-bundles-and-shulkers-design.md`

## Global Constraints

- After a game run: `git checkout logs/latest.log`, `rm -f logs/*.log.gz`.
- Known failures on the base: `BlightTest.theShakeBeginsBeforeItsFullAndPeaksWhenItIs`,
  `EndermanShulkerCarrierTest.twoToFourInAHundredCarryAShulker`, the blight game test.
- New game test classes are listed in `src/gametest/resources/fabric.mod.json`.
- Names in `en_us.json` and `fr_fr.json`.

## Review Focus

- A click that used to swap or pick up now eating an item or doing nothing (a full box, a box in a stack, a result slot,
  creative mode).
- The client's and the server's views of a box parting (the selection's index after the contents change; a take whose
  slot now holds something else).
- The peek showing or giving what the player couldn't open (locked, a loot table, out of reach, the lid blocked).
- The wheel kept from the hotbar when it shouldn't be (a screen open, sneaking, the peek off, an empty box, a spectator).
- Duplication: anything taken that also stays (client prediction, a remainder put back, two takes in one tick).

---

### Task 1: The bundle, 5 × 4

**Files:** new `mod/items_and_effects/containers/BundleGrid.java` (`COLUMNS = 5`, `ROWS = 4`, `int itemsToShow(int size)`);
new mixin `mixin/items_and_effects/containers/BundleContentsMixin` (`getNumberOfItemsToShow` → `BundleGrid.itemsToShow(size())`);
client `mod/items_and_effects/containers/WideBundleTooltip` (vanilla's `ClientBundleTooltip` at 5 columns, `BundleGrid`'s
sizes, width 120), registered for `BundleTooltip` with `ClientTooltipComponentCallback`; `BundleGridTest`;
new `ShulkersGameTest` (listed).

- [ ] Failing `BundleGridTest.theGridShowsEverythingUpToTwentyAndNineteenPast`: `itemsToShow(n) == n` for 0..20,
  `== 19` for 21, 25, 64. Failing game test `aBundleShowsNineteenOfMany`: a bundle of 25 stacks (one item each, 25
  kinds) → `BundleItem.getNumberOfItemsToShow == 19`; of 20 → 20; of 13 → 13. Implement. Run both. Commit.

### Task 2: Into and out of a shulker box, in the inventory

**Files:** new `ShulkerItems` (`SIZE = 27`; `NonNullList<ItemStack> slots(ItemContainerContents)`;
`int insert(NonNullList<ItemStack> slots, ItemStack from)` → count moved, merging then first empty;
`boolean fits(ItemStack)`; `ItemStack take(NonNullList<ItemStack>, int slot)`); new `ShulkerSelection`
(`int next(List<ItemStack> slots, int selected, int wheel)` → the next non-empty slot, `-1` if none; `int selected(ItemStack box)`;
`void select(ItemStack box, int slot)`); new mixin `ItemContainerContentsMixin` (`@Unique int` selection, `SelectedSlot`
interface `playersVersus$selected()`, `playersVersus$withSelected(int)`); new mixin `ItemShulkerClicksMixin` on `Item`
(`overrideStackedOnOther`, `overrideOtherStackedOnMe` at HEAD for `ItemTags.SHULKER_BOXES`); new `SelectShulkerSlotPayload(int menuSlot, int boxSlot)`
(serverbound, registered in `VersusMod`, received in `VersusModServer` → `ShulkerSelection.select` on that menu slot's
box, if it is one); `ShulkerSelectionTest`, `ShulkerItemsTest`, `ShulkersGameTest`.

- [ ] Failing unit tests: `ShulkerSelectionTest` (down from -1 → first item; up from -1 → last; skips empties; wraps both
  ways; all empty → -1; a selected slot now empty steps from where it was). `ShulkerItemsTest` (onto same stacks first,
  then first empty; respects max stack size; a shulker box doesn't fit; a full box takes 0; take empties the slot).
- [ ] Failing game tests (`player.inventoryMenu.clicked`, a mock survival player): `aCarriedItemGoesIntoTheBox`,
  `aCarriedBoxTakesInTheClickedItem`, `theSelectedItemComesOutOnRightClick` (selection unselected after),
  `withNothingSelectedRightClickPicksUpTheBox`, `aBoxNeverGoesIntoABox` (swap), `aFullBoxSwaps`, `aStackOfBoxesIsLeftAlone`.
  Implement. Run. Commit.

### Task 3: The shulker box's tooltip and scrolling over it

**Files:** new common `ShulkerTooltip(List<ItemStack> slots, int selected) implements TooltipComponent`; `Item.getTooltipImage`
(the same `Item` mixin: a box with anything in it, its contents shown by `tooltip_display`); `ItemContainerContentsMixin`
(`addToTooltip` cancelled for a shulker box's contents); client `ShulkerGrid` (`CELL = 24`, `draw(graphics, font, x, y, slots, selected)`,
`width()`, `height()`), `ClientShulkerTooltip` (registered for `ShulkerTooltip`), `ShulkerMouseActions` (an
`ItemSlotMouseAction`: scroll → `ShulkerSelection.next` → select locally and send `SelectShulkerSlotPayload`; stop hovering
and quick-move/swap clicks unselect), mixin on `AbstractContainerScreen.init` adding it.

- [ ] Failing game tests: `aShulkerBoxsTooltipIsItsSlots` (27 slots in order, empties empty, the selection carried),
  `anEmptyBoxHasNoTooltipImage`, `aBoxsContentsAreNotAlsoListedAsText` (no "item.container.item_count" line), `aChestStillListsItsContents`.
  Implement. Run. Commit.

### Task 4: The peek

**Files:** new `PeekShulkerPayload(BlockPos)`, `TakeFromShulkerPayload(BlockPos, int slot, Item item)` (serverbound),
`ShulkerPeekPayload(BlockPos, ItemContainerContents)` (clientbound; empty contents = nothing to show); new `ShulkerPeeking`
(`Optional<ShulkerBoxBlockEntity> openable(ServerPlayer, BlockPos)`, `Optional<ItemContainerContents> contents(ServerPlayer, BlockPos)`,
`int take(ServerPlayer, BlockPos, int slot, Item item)` → count taken); invoker for `ShulkerBoxBlock.canOpen`; client
`ShulkerPeek` (the key, the option file, the tick that asks, the HUD element after the crosshair at 0.75 scale, the
selection), mixins `MouseHandlerPeekMixin` (the hotbar's `setSelectedSlot` in `onScroll`), `MultiPlayerGameModePeekMixin`
(`useItemOn` at HEAD: a selection on the box looked at → send the take, swing, `SUCCESS`); `VersusSettings.Shulkers`
(`PEEK_REFRESH_TICKS = 10`, `PEEK_SCALE = 0.75F`); names.

- [ ] Failing game tests: `aPeekShowsAnOpenableBox`, `aPeekShowsNothingLockedOutOfReachOrUnrolled` (locked to another key,
  9 blocks away, a loot table set), `aPeekShowsNothingWithTheLidBlocked`, `takingMovesTheStackIntoTheInventory`,
  `takingWithAFullInventoryLeavesTheRest`, `takingTheWrongItemTakesNothing`. Implement. Run. Commit.

### Task 5: In the world

- [ ] The gallery (`WorldgenGallery`): shots that hover an inventory slot, scroll, and look at a block; screenshots of a
  bundle of 25, a shulker box's tooltip with a selection, the peek. Suite, game tests, docs, a fresh final review,
  fixes, push.
