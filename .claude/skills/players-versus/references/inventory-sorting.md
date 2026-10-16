# Client Inventory System: Sorting, Hotbar Swapping, Merging & Dumping

All client-only (`src/client/java/frootloops/versus/{mixin/client,mod}/items_and_effects/inventory/**`).
README: "Complete inventory solution; Item merging, client-side inventory sorting, & smart hotbar
swapping."

## Sorting architecture (`InventorySorting` → `SortingHelper` → `sorting/{groups,lists}/**`)
Three-layer pipeline, entirely client-side heuristics operating on `ItemSlot` copies before issuing
real `clickSlot` packets to actually move items:

1. **`SortedItemLists`** (`sorting/lists/*`: `SortedItemList`, `SortedBlockItemList`,
   `SortedMappedItemList`, `SortedMiscItemList`, `SortedTaggedItemList`, `SortedTypedItemList`) —
   the leaf classification layer. Each list owns a *category* (pickaxes, pickaxe-mineable blocks,
   weapons, redstone items, minerals, brewing ingredients, fishing/exploration items, etc.) and
   knows how to test whether an `ItemStack` belongs to it (by `ItemType` enum, by item tag, by an
   explicit item set, or by block-mineable-tool relationship). `ItemComparaisonHelper` supplies the
   actual per-item ordering/typing logic these lists lean on.
2. **`SortingGroups`** (`groups/*`: `SortingGroup` base, `SimpleSortingGroup`, `ToolSortingGroup`,
   `MainHotbarGroup`) — mid-level buckets, each wrapping one or two `SortedItemList`s. Fifteen
   groups exist beyond the hotbar: Combat, Consumables, Goodies, Rare/Common Minerals, Redstone,
   Pickaxe/Axe/Shovel/Hoe/Shears (each a `ToolSortingGroup` pairing the tool type with the blocks it
   mines), Brewing, World (fishing+exploration), Containers, and a catch-all Random/Misc group —
   plus `MAIN_HOTBAR`, a `MainHotbarGroup` with its own "essential loadout" rules. `ToolSortingGroup`
   tracks tool-count vs. block-count separately (`getNumTools`/`hasOnlyTools`/`canGiveawayTools`) so
   the merge logic below can distinguish "this group is all tools, safe to redistribute" from
   "this group has mined blocks mixed in, keep them together."
3. **`SortingHelper.getOptimalInventoryRows`** — the actual algorithm, run once per sort action:
   - **Insert**: every slot goes into the best-matching group (`insertItemIntoGroup`), hotbar first
     if sorting the player's own inventory, falling back to the catch-all `RANDOM_GROUP` if nothing
     claims it. Trash-typed items go straight to the back of `RANDOM_GROUP`.
   - **Hotbar curation** (`cleanUpHotbar`, player inventory only): a long heuristic ladder that
     tries to guarantee a "sensible loadout" — always want an axe and pickaxe if available (giving
     away from a mixed tool+block group only if it *can* give away without breaking block clusters),
     top up with hoe/shears specifically **when in the Deep Dark** (a context-aware rule — see
     below), then fills remaining hotbar space with building material, then combat items, then
     ensures at least one consumable is present if the hotbar isn't nearly full/empty already.
   - **Group cleanup & merging** (`cleanUpGroups`): a large, hand-tuned set of pairwise merge rules
     (`tryCombiningTwoGroups` — merges only when the two groups' sizes sum to exactly a multiple of
     9, i.e. they'd tile rows cleanly) between related groups (redstone+raw-redstone-ore, rare+common
     minerals, brewing+consumables when there are enough potions+concentrates combined, world+shears,
     etc.), plus symmetric tool-group cleanup (`cleanUpToolGroup`/`giveExtraToolsFromAndTo`) that
     redistributes near-empty tool groups into their most related neighbor rather than leaving
     fragmented half-rows.
   - **Row assembly** (`getOrderedListOfGroups` + the main placement loop): sorts non-empty groups
     by leftover-size, greedily merges complementary-sized groups into shared rows, then falls back
     to a slower per-group row-splitting pass for groups that don't fit a `numRows`-groups-total
     trivial case. A 30-iteration safety cap on the row-filling loop logs an error and aborts
     (returns `null`) rather than looping forever if the group-size bookkeeping ever goes
     inconsistent — **a `null` return means "sorting failed, leave inventory as-is"**, not a crash;
     any consumer of this method must handle that.
   - **Context parameters** (`isInDeepDark`, `isInNether`, `isInWater`) thread all the way down to
     `MAIN_HOTBAR.tryInsertingSlot` — the sort result genuinely depends on where the player is
     standing, not just inventory contents (e.g. Deep Dark biases toward hoe/shears — presumably for
     sculk-sensor/wool-muffling utility — being kept in the hotbar).
   - Heavy `DEBUG_SORTING_*` boolean flags (in `InventorySorting`) gate very verbose per-step
     `MOD_LOGGER.warn` tracing through the whole pipeline — flip these on when debugging a bad sort
     result rather than adding new print statements.

## Hotbar swapping/cycling (`HotbarCycling`, `HotbarSwappingClientMixin`)
`doHotbarSwap`/`doInverseHotbarSwap`/`doHotbarSwap(inventory, numTypesToSwap)`: swaps the 9 hotbar
slots with one of the three main-inventory rows via real `clickSlot(..., SWAP, ...)` packets (so the
server sees legitimate slot-swap actions, not a suspicious teleporting-item pattern) — the
mechanism behind "smart hotbar swapping" (README) and gated by
`VersusSettings.QOL.DO_HOTBAR_SWAPPING_ON_PICK_KEY` (per `architecture.md`'s settings grep) in
`HotbarSwappingClientMixin` (not read this pass — check it for the actual keybind/trigger).

## Item merging (`ItemMergingMixin`, client)
Not read this pass — an `@Overwrite`-based mixin per `architecture.md`'s census. README: "Item
merging" — almost certainly auto-stacking same-item drops on pickup or within a container, reducing
clutter from partial stacks. Check directly before changing pickup/stacking behavior.

## Container dumping (`ContainerDumping`, `InventoryManagementHelper`)
Two client-only quick-transfer actions (bound to on-screen buttons —
`TEXTURE_DUMP_TO_PLAYER_BUTTON`/`TEXTURE_DUMP_TO_STORAGE_BUTTON`, `assets/players-versus/textures/gui/sprites/container/**`):
"dump to storage" and "dump to player" each first merge same-item stacks within their own inventory
(`InventoryManagementHelper.mergeStacksTogether`), then quick-move (`SlotActionType.QUICK_MOVE`,
i.e. real shift-click packets) **only items whose type already exists in the destination inventory**
(`destinationItems` lookup — a stack must be an exact default-state match, `ItemStack.areItemsAndComponentsEqual`
against `getDefaultStack()`, so a uniquely-named/enchanted/damaged stack is never auto-dumped) — this
is a **restock-existing-stacks** action, not a "move everything" or "sort into chest" action; the
commented-out `InventorySorting.sortInventory(...)` calls at the end of both methods show a
sort-after-dump feature was considered/removed — verify intentional before re-enabling.

## Extension recipe: adding a new sorting category
1. Add a `SortedItemList` (or reuse an existing one) in `sorting/lists/SortedItemLists` classifying
   the new item set (by tag if possible — tags are the most maintenance-free classifier here).
2. Add a `SortingGroup` (Simple or Tool) in `SortingGroups`, and add it to the `SORTING_GROUPS` array
   in the position that reflects its priority relative to neighbors (order matters for
   `insertItemIntoGroup`'s first-match-wins loop and for merge-adjacency heuristics).
3. If it should interact with existing merge rules (e.g. "combine with Brewing when both are small"),
   add the pairwise `tryCombiningTwoGroups`/`mergeWithOtherGroup` call in `cleanUpGroups`/`cleanUpMisc`
   next to its closest conceptual sibling, rather than at the end of the method — merge order affects
   the final layout.
4. Don't touch `getOptimalInventoryRows`'s row-assembly logic (steps 3-4) unless you're fixing a
   genuine bug in the general algorithm — it's context-free with respect to specific groups.
