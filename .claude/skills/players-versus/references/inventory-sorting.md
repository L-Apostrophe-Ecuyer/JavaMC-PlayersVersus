# Inventory System: Sorting, Hotbar Swapping, Merging & Dumping

README: "Complete inventory solution; Item merging, client-side inventory sorting, & smart hotbar
swapping." The sort buttons and clicks are client code
(`src/client/java/frootloops/versus/{mixin/client,mod}/items_and_effects/inventory/**`); the sorting
itself is item-only common code (`src/main/java/frootloops/versus/mod/items_and_effects/inventory/sorting/**`),
so the game tests can run it on a server.

## Sorting pipeline (`InventorySorter`, `sorting/{lists,groups}/**`)
`InventorySorter.sort(List<ItemSlot> slots, int numRows, Situation situation)` returns an
`Optional<ItemSlot[]>` laid out `row * 9 + column` (`null` = empty; row 0 is the player's hotbar), or
empty when the layout gives up — the client then leaves the inventory as it is. `Situation` is
`(playerInventory, inDeepDark, inNether, inWater)`, `Situation.CONTAINER` for chests. Each call makes
one `InventorySorter` with its own `SortingLists` and `SortingGroups`, so **nothing carries over
between sorts** (the pre-26.3 code kept static singletons, which leaked the Deep Dark block lock,
potion flags and leftover stacks into later sorts — duplicated and ghost stacks, "a hotbar of torches
and food"). Group sizes are always read from their lists; never add counters. The input is first put in a
fixed order (item, count, components), so the layout depends only on which stacks there are and sorting a
sorted inventory changes nothing. A sort that throws, or a layout that doesn't hold every slot exactly once,
returns empty.

- `ItemSlot(slotId, stack, itemType, name)` — `ItemSlot.of(slotId, stack)` works out the type
  (`ItemClassifier.typeOf`) and shown name once.
- `ItemOrdering.shouldGoBefore(...)` — which of two stacks goes first (rarity, tool/armour strength,
  food value, potion, then for blocks: axe blocks, the block maps, then map colour).
- `lists/*` — `SortedItemList` subclasses (by `ItemType`, item tag, block tag/sound/hardness, or a
  ranked map from `ItemSortingMaps`). `SortingLists` builds one of each per sort.
- `groups/*` — `SimpleSortingGroup`, `ToolSortingGroup` (tools first, then what they mine),
  `MainHotbarGroup` (one slot per role: main weapon, pickaxe, axe, extra weapon, food, potion, clutch,
  light, block, plus misc). `SortingGroups.all()` is the insertion order: combat, consumables,
  goodies, rare minerals, common minerals, redstone, pickaxes, axes, shovels, hoes, shears, brewing,
  world, containers, random.

Stages, in `InventorySorter.layOut`:
1. **Insert** each stack: hotbar roles first (player inventory), trash straight to the random group,
   shulker boxes / bundles / item containers straight to the containers group, else the first group
   that takes it, else the random group.
2. **Hotbar** (player inventory): `cleanUpHotbar`'s ladder, then `MainHotbarGroup.keepOnlyEssentials`
   (dropped stacks go back to their groups), then `avoidAWeakHotbar`. Rules:
   - The main weapon (best sword; a hoe in the Deep Dark) always stays, even without food or a second weapon.
   - The best pickaxe and axe join the hotbar unless their group makes whole rows (size a multiple of
     9, e.g. 1 axe + 8 wool): such rows stay together, for hotbar cycling to swap in.
   - A hotbar with no weapon and no tool takes a whole tool row (exactly 9: pickaxe, axe, shovel, hoe,
     then shears group) instead, its items going back to their groups; without one, the best tool of
     the first tool group that has one joins it.
   - The hotbar never takes another group once it holds 9 stacks (`tryCombiningTwoGroups` would read
     9 % 9 as empty), and gives back misc stacks past nine (`MainHotbarGroup.trimTo`).
   - Food, light, clutch item, potion, extra weapon and building block as before, with the Deep Dark
     (hoe/shears, vibration-blocking wool), Nether and water rules.
3. **Clean up groups** (`cleanUpGroups`, `cleanUpToolGroup`, `giveExtraToolsFromAndTo`, `cleanUpMisc`):
   hand-tuned pairwise merges, `tryCombiningTwoGroups` merging only when the sizes make a full row,
   then each group's `tryFormingRows` pairs lists whose remainders make 9 (tool groups may top up with
   tools).
4. **Order** (`getOrderedListOfGroups`): by leftover size, merging small complementary groups, then
   by item type, hotbar first.
5. **Lay out** (`SortedInventoryOutput`): one list per row or one group per row when they fit, else
   row filling; a 30-iteration cap gives up (empty Optional) rather than loop.

Debug tracing is off unless the game runs with `-Dpv.sorting.debug=true` (`SortingDebug`).

## Clicks (`InventorySorting`, `InventoryManagementHelper`, client)
`InventorySorting.sortInventory(menu, client, inventory, InventoryToSort)` with `PLAYER_INVENTORY`
(36 slots), `CREATIVE_INVENTORY` (sets the stacks directly, then `broadcastChanges`) or
`CONTAINER_INVENTORY` (`getContainerSize()` slots). It first puts the carried stack back and merges
partial stacks, clicking `menu.findSlot(inventory, i)` for each inventory index (never a raw index).
Then `MovePlanner.swapsWithSpare(current, target, spare)` plans SWAP clicks through one spare hotbar
slot: equal stacks (`ItemStack.matches`) are interchangeable, stacks already in place stay put, and a
cycle of k moves costs k + 1 clicks (k - 1 when the spare is one of the sorted slots, as in the
player's own inventory). For containers `MovePlanner.chooseSpare` picks an empty hotbar slot, else one
whose stack the container's slots accept, else the sort is skipped.

## Tests
- `src/test/.../sorting/MovePlannerTest` — plain JUnit (`./gradlew test`).
- `src/gametest/java/frootloops/versus/gametest/InventorySortingGameTest` — Fabric game tests on a
  headless server with the real items and tags, through `TestInventories.sortIds` (300 seeded
  inventories never lose or duplicate a stack, a sort doesn't change the next, the hotbar rules,
  containers, minecarts, block order). Loom's `fabricApi.configureTests` creates the `gametest`
  source set; `./gradlew runGameTest -Ppv.acceptEula=true` runs them (skipped without the property,
  since it starts a Minecraft server), and the `gametest` workflow does so on every push, printing
  the JUnit report.

Fixed on 26.3 (each pinned by a game test where it shows): leaked state between sorts; a null clutch
item in water; a null second choice in tool cleanup; `addSlots` skipping stacks; `tryFormingRows`
advancing the wrong index and losing tools; hand-kept group counters; the containers group sharing
the goodies' lists; minecarts ranked in the redstone map; the shears step giving shovels; the potion
comparison checking itself; the block comparison comparing a block with itself; the empty-slots-per-row
estimate dividing by `min(1, …)`; cursor placing and merging clicking raw indices.

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
(`InventoryManagementHelper.mergeStacksTogether(menu, client, inventory, numSlots)`), then quick-move (`SlotActionType.QUICK_MOVE`,
i.e. real shift-click packets) **only items whose type already exists in the destination inventory**
(`destinationItems` lookup — a stack must be an exact default-state match, `ItemStack.areItemsAndComponentsEqual`
against `getDefaultStack()`, so a uniquely-named/enchanted/damaged stack is never auto-dumped) — this
is a **restock-existing-stacks** action, not a "move everything" or "sort into chest" action; the
commented-out `InventorySorting.sortInventory(...)` calls at the end of both methods show a
sort-after-dump feature was considered/removed — verify intentional before re-enabling.

## Extension recipe: adding a new sorting category
1. Add a list field in `sorting/lists/SortingLists` (by tag if possible — tags need the least upkeep),
   and put it in the right group's list array.
2. For a new group, add a field in `groups/SortingGroups` and put it in `all()` where its priority
   belongs (insertion is first-match-wins, and the order breaks layout ties).
3. Add merge rules next to their closest sibling in `cleanUpGroups`/`cleanUpMisc` — merge order
   affects the layout.
4. Add a game test in `InventorySortingGameTest` for the layout you expect, and run
   `everySlotEndsUpExactlyOnce` (it covers every `ItemType` through `TestInventories`' pool; add the
   new items there).
