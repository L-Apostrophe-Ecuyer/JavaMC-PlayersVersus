# Inventory Sorting Restructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the client inventory sort clean, cheap and dependable (no state between sorts, no lost items, a hotbar that keeps its tools) while keeping its category lists, comparisons and row layout.

**Architecture:** The item-only sorting logic moves to common code and runs as one `InventorySorter` object per sort (fresh lists and groups, stages as methods: insert, curate hotbar, clean up groups, order groups, lay out). The client keeps the screens and turns the result into the fewest SWAP clicks through a pure `MovePlanner`. A Fabric game test sorts real inventories on a headless server in CI.

**Tech Stack:** Minecraft 26.3 (Mojang names), Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 (`fabric-gametest-api-v1`), Loom 1.17.1, Java 25, JUnit 5 (fabric-loader-junit).

**Spec:** the design approved in the session on 2026-10-10, restated here as Design.

## Design (approved)

Hotbar rules, for the player's inventory:
1. The main weapon (best sword; a hoe in the Deep Dark, as today) always stays in the hotbar. It is no longer dropped for lacking food or a second weapon.
2. The best pickaxe and the best axe go to the hotbar, unless their tool group fills whole rows exactly (group size a multiple of 9, e.g. 1 axe + 8 wool). Such rows stay together, since the player can swap them in with hotbar cycling.
3. The rest of the loadout works as today: food, light, clutch item, potion, extra weapon and building block, plus the Deep Dark, Nether and water rules.
4. No weak hotbar. If the hotbar would hold no weapon and no tool, the best whole tool row (pickaxe, then axe, shovel, hoe, shears) becomes the hotbar, and the hotbar's items go back to their groups. (4b) If there is no whole tool row, the best tool of the first tool group that has one joins the hotbar.

Refactor: the sort keeps no state between sorts and has no static mutable singletons. ArrayLists replace the LinkedLists, and debug logging is off unless `-Dpv.sorting.debug=true`. Moves cost one SWAP click per moved stack plus one per cycle, and stacks already in place stay put. Every category list, comparison rule and row-layout heuristic is kept; only the hotbar rules and the listed bug fixes change behaviour.

Bug fixes (each changes behaviour on purpose):
- B1. The flags no longer leak between sorts: `isBlockSlotLocked`, `hasGoodPotion`, `hasExcellentPotion`, leftover group items and misc lists.
- B2. The Containers group uses its own lists (bundles, shulker boxes) instead of sharing Goodies'.
- B3. Minecarts go into `ITEMS_MINECARTS`, not into `ITEMS_REDSTONE_COMPONENTS`.
- B4. In water, the first clutch item no longer dereferences a null `clutchItem`.
- B5. `cleanUpToolGroup` no longer dereferences a null second choice.
- B6. A sort that gives up leaves the inventory untouched instead of throwing on `null`.
- B7. `SortingGroup.addSlots` now tries every slot (the pop/re-add loop skipped some).
- B8. `tryFormingRows` pairs lists `i < j` (its inner loop advanced `i`), and sizes are derived, so tools can no longer be lost.
- B9. Group sizes are computed from the lists instead of hand-kept counters.
- B10. The cleanup ladder's shears step gives shears, not shovels.
- B11. The potion comparison checks the other potion's effects.
- B12. The block comparison compares against the other block, not itself.
- B13. The empty-slots-per-row estimate divides by `max(1, rows left)`, not `min`.
- B14. Cursor placing and stack merging click the menu slot of each inventory index (`findSlot`), not the raw index.

## Global Constraints

- Minecraft 26.3, Fabric API 0.161.0+26.3, Loom 1.17.1, Java 25; there's no local build. Verify on push in CI: `build` (compile + JUnit) and the new `gametest` workflow.
- Packages stay `frootloops.versus.mod.items_and_effects.inventory.sorting[.lists|.groups]`; client code stays under `src/client/java/frootloops/versus/{mod,mixin/client}/items_and_effects/inventory/`.
- Match the surrounding style: sparse comments, class Javadoc, existing names where the class survives.
- Commit trailers: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` and `Claude-Session: https://claude.ai/code/session_01FWtD6gxbErQxWgXqpnxhD8`.
- The game test accepts the Minecraft EULA only through `-Ppv.acceptEula=true`, which the workflow passes, as the worldgen smoke test does.

## Review Focus

1. An item is lost or duplicated by a sort. Expected: every stack ends up in exactly one slot (Task 3 fuzz test).
2. A sort depends on an earlier one (Deep Dark, potions). Expected: the same inventory always sorts the same way (Task 3 repeatability test).
3. A shulker box is sorted while the spare hotbar slot holds a shulker box, which the box's slots refuse. Expected: an empty or placeable spare is picked, or the sort is skipped (Task 2 `chooseSparePrefersEmptyThenPlaceable`; Task 6 applies it).
4. An already-sorted inventory is sorted again. Expected: no clicks (Task 2 `alreadySortedNeedsNoSwaps`).
5. Many identical full stacks (20 × 64 cobblestone). Expected: interchangeable stacks don't move needlessly (Task 2 `equalStacksAreInterchangeable`).

---

### Task 1: Sorting core in common code, game test harness in CI

**Files:**
- Move: `src/client/java/frootloops/versus/mod/items_and_effects/inventory/sorting/**` → `src/main/java/frootloops/versus/mod/items_and_effects/inventory/sorting/**` (`git mv`, same packages)
- Create: `src/main/java/frootloops/versus/mod/items_and_effects/inventory/sorting/SortingDebug.java`
- Modify: `ItemSlot.java`, `ItemComparaisonHelper.java` (map colour: `Minecraft.getInstance().level` → `EmptyBlockGetter.INSTANCE`; the probe showed `getMapColor` only reads the state's `mapColor`)
- Modify: every sorting class importing `InventorySorting.DEBUG_SORTING_*` → `SortingDebug.ENABLED`
- Modify: `build.gradle` (add `fabricApi { configureTests { … } }`)
- Create: `src/gametest/resources/fabric.mod.json`, `src/gametest/java/frootloops/versus/gametest/InventorySortingGameTest.java`, `src/gametest/java/frootloops/versus/gametest/TestInventories.java`
- Create: `.github/workflows/gametest.yml`

**Interfaces:**
- Produces: `SortingDebug.ENABLED` (`Boolean.getBoolean("pv.sorting.debug")`) and `SortingDebug.log(Supplier<String>)` (MOD_LOGGER.info when enabled); the `gametest` source set with mod id `players-versus-gametest` and entrypoint `"fabric-gametest": ["frootloops.versus.gametest.InventorySortingGameTest"]`; the Gradle task the run config creates (expected `runGameTest`; confirm from the first CI log).
- Produces (gametest): `static Optional<int[]> TestInventories.sortIds(List<ItemStack> stacks, int numRows, boolean playerInventory, boolean inDeepDark, boolean inNether, boolean inWater)`, where `result[i]` is the index in `stacks` of the stack laid at position `i` (`row * 9 + column`), or -1 for empty, and an empty Optional means the sort gave up. Every game test sorts through it, so the tests survive the restructure unchanged.

- [ ] **Step 1:** Move the package, replace the client references (the debug flags, `Minecraft.getInstance().level`), and add `configureTests` with `createSourceSet = true`, `modId = "players-versus-gametest"`, `enableGameTests = true`, `enableClientGameTests = false`, `eula = providers.gradleProperty("pv.acceptEula").getOrElse("false") == "true"`.
- [ ] **Step 2:** Write `TestInventories.sortIds` on today's API (`new ItemSlot(i, stack, ItemComparaisonHelper.getItemTypeOf(stack))` in a `LinkedList`, `SortingHelper.getOptimalInventoryRows`, `null` → empty Optional). Write the game test `aMixedInventoryKeepsEveryItem(GameTestHelper)`, annotated `@net.fabricmc.fabric.api.gametest.v1.GameTest`: it sorts a fixed 20-stack player inventory (4 rows, no situation), asserts with `helper.assertTrue` that the result is present and every input index appears exactly once, then calls `helper.succeed()`. Add a second test, `harnessFailsLoudly`, that calls `helper.fail("harness check")`.
- [ ] **Step 3:** Write `gametest.yml`: on push; checkout, JDK 25 (microsoft), `chmod +x gradlew`, `./gradlew runGameTest -Ppv.acceptEula=true`; on failure, upload the run directory's `logs/` and the JUnit report.
- [ ] **Step 4:** Push. Expected: `build` green, `gametest` red, naming `harnessFailsLoudly`. This proves a failing test fails the job.
- [ ] **Step 5:** Remove `harnessFailsLoudly`, push. Expected: `build` and `gametest` green, and the log lists `aMixedInventoryKeepsEveryItem` as passed.
- [ ] **Step 6:** Commit message: `Inventory sorting: core in common code, Fabric game test in CI`.

### Task 2: Move planner

**Files:**
- Create: `src/main/java/frootloops/versus/mod/items_and_effects/inventory/sorting/MovePlanner.java`
- Test: `src/test/java/frootloops/versus/mod/items_and_effects/inventory/sorting/MovePlannerTest.java` (plain JUnit, no `TestGame`)

**Interfaces:**
- Produces: `static int[] MovePlanner.swapsWithSpare(int[] currentClass, int[] targetClass, int spare)` returns, in order, the positions to SWAP with the spare slot. Equal class means interchangeable stacks, and class 0 means empty. `spare` is the spare's own position when it is one of the positions, else -1. Throws `IllegalArgumentException` when the two arrays aren't the same multiset.
- Produces: `static int MovePlanner.chooseSpare(boolean[] emptyHotbar, boolean[] placeableHotbar)` returns the first empty hotbar index, else the first placeable one, else -1.
- Algorithm: positions whose current class equals their target class stay fixed. Within each class, pair the remaining targets and sources in ascending order. Then walk the cycles of the resulting permutation: the spare's own cycle costs `k - 1` swaps starting after the spare, and every other cycle `a, π(a), …, π^(k-1)(a), a` costs `k + 1`.

- [ ] **Step 1:** Write the tests:
  - `alreadySortedNeedsNoSwaps`: `swapsWithSpare(c, c, -1).length == 0`.
  - `swapsRealiseTheTarget`: 1000 seeded random cases, n ∈ {27, 36, 54}, classes 0..8 with duplicates. Simulate an exchange of `content[p]` with the spare for each p (the spare being `content[spare]` when inside, else a separate cell holding 99), then assert `content == target` and, when the spare is outside, that it holds 99 again.
  - `twoCycleCostsThreeOutsideOneInside`: `[1,2] → [2,1]` gives 3 swaps with spare -1, and 1 swap with spare 0.
  - `equalStacksAreInterchangeable`: `[5,5,5,0] → [0,5,5,5]` gives at most 3 swaps.
  - `unequalContentsAreRejected`.
  - `chooseSparePrefersEmptyThenPlaceable`.
- [ ] **Step 2:** Push. Expected: `build` red (no `MovePlanner`).
- [ ] **Step 3:** Implement `MovePlanner` (pure Java, no Minecraft imports).
- [ ] **Step 4:** Push. Expected: `build` green, `MovePlannerTest` all passed.
- [ ] **Step 5:** Commit message: `Inventory sorting: plan moves as swap cycles through one spare slot`.

### Task 3: One sorter per sort

**Files:**
- Modify/rename in `sorting/`: `ItemSlot` (record `ItemSlot(int slotId, ItemStack stack, ItemType itemType, String name)`, `static ItemSlot of(int slotId, ItemStack stack)`, `toString()` returns `name`, computed once as `hoverName + (count != max ? "(" + count + ")" : "")`). `ItemComparaisonHelper` becomes `ItemClassifier` (`static ItemType typeOf(ItemStack)`) plus `ItemOrdering` (the `shouldGoBefore` overloads and preference helpers). `SortingHelper` becomes `InventorySorter`. `SortedInventoryOutput` stays (ArrayList API).
- Modify: `lists/*` (ArrayList storage; `take`/`takeAll` return `List<ItemSlot>`). `lists/SortedItemLists` becomes `lists/SortingLists` with instance fields of the same names, one instance per sort.
- Modify: `groups/*` (instance per sort; `size()` derived; `ToolSortingGroup.getNumTools()` = tool list size). `groups/SortingGroups` becomes an instance class with fields `hotbar, pickaxes, axes, shovels, shears, hoes, consumables, combat, redstone, goodies, containers, rareMinerals, commonMinerals, brewing, world, random` and `SortingGroup[] all()` in today's `SORTING_GROUPS` order.
- Test: `src/gametest/java/frootloops/versus/gametest/InventorySortingGameTest.java`, `TestInventories.java`.

**Interfaces:**
- Produces: `record InventorySorter.Situation(boolean playerInventory, boolean inDeepDark, boolean inNether, boolean inWater)` with `Situation.CONTAINER`.
- Produces: `static Optional<ItemSlot[]> InventorySorter.sort(List<ItemSlot> slots, int numRows, Situation situation)`; index = `row * 9 + column`, `null` = empty; empty Optional when the layout gives up.
- Produces (gametest): `TestInventories.random(RandomSource random, int maxStacks)` returning `List<ItemStack>` from a fixed pool of about 120 stacks covering every `ItemType`; `sortIds` keeps its signature and switches to `InventorySorter.sort` in Step 3.
- Fixes folded in here because the structure removes them: B1, B4, B5, B6, B7, B8, B9.

- [ ] **Step 1:** Write the game tests:
  - `everySlotEndsUpExactlyOnce`: 300 seeded inventories, a third each for player (4 rows; situation drawn from none, Deep Dark, Nether, water), chest (3 rows) and double chest (6 rows). Each result must be present, every input index must appear exactly once, and the -1s must number `rows * 9 - n`.
  - `aSortDoesNotChangeTheNext`: sort X, then a Deep Dark sort with wool and a long fire resistance potion, then X again. The two X results must be equal arrays.
  - `inWaterWithAPearlAndNoTotem`: present.
- [ ] **Step 2:** Push. Expected: `gametest` red, with the old code losing or duplicating items or leaking state (record which tests fail in the commit message).
- [ ] **Step 3:** Restructure as listed. Each old static method becomes a private stage method on the `InventorySorter` instance, with its body ported as is apart from B1, B4–B9. All debug output goes through `SortingDebug.log(() -> …)`. Point `TestInventories.sortIds` at `ItemSlot.of` and `InventorySorter.sort`.
- [ ] **Step 4:** Push. Expected: `build` and `gametest` green.
- [ ] **Step 5:** Commit message: `Inventory sorting: one sorter per sort, no lost items or leaked state`.

### Task 4: Classification and comparison fixes

**Files:**
- Modify: `groups/SortingGroups` (containers uses `lists.MISC_CONTAINERS`, renamed from `MISC_GONTAINERS`), `ItemSortingMaps` (B3), `InventorySorter` (B10), `ItemOrdering` (B11, B12), `SortedInventoryOutput` (B13)
- Test: `InventorySortingGameTest`

- [ ] **Step 1:** Write the game test `shulkerBoxesSortTogether`: a chest (3 rows) holding 3 shulker boxes, 10 assorted misc stacks and 5 stone, in mixed order. The output positions of the 3 boxes must be consecutive.
- [ ] **Step 2:** Push. Expected: `gametest` red on `shulkerBoxesSortTogether`.
- [ ] **Step 3:** Apply B2, B3, B10–B13.
- [ ] **Step 4:** Push. Expected: green, with Task 3's tests still passing.
- [ ] **Step 5:** Commit message: `Inventory sorting: containers group, minecarts, shears, potion and block comparisons, empty slots per row`.

### Task 5: Hotbar rules

**Files:**
- Modify: `InventorySorter` (hotbar stage: `cleanUpHotbar`, `keepOnlyEssentials` and the new weak-hotbar step), `groups/MainHotbarGroup` (`hasWeaponOrTool()`), and the layout's first group (a whole tool row can lead in place of the hotbar group).
- Test: `InventorySortingGameTest`

**Interfaces:**
- Produces: `boolean MainHotbarGroup.hasWeaponOrTool()` is true when the main weapon, extra weapon, pickaxe or axe slot is set, or any misc item `isToolOrWeapon()`.
- Rule 2 in the ladder: wherever the hotbar lacks a pickaxe (axe) and the group's `getNumTools() > 0`, take `takeBestTool()` when `canGiveawayTools() || size() % 9 != 0`, and leave the group whole otherwise.

- [ ] **Step 1:** Write the game tests, all with a player inventory and no situation unless noted. Here "row r" means indices `9r..9r+8`, and "a whole row of X" means some row r ≥ 1 holds exactly X.
  - `wholeToolRowsStayWhole`: diamond sword, 16 bread, 32 torches, iron axe + 8 × 64 oak planks, iron pickaxe + 8 × 64 cobblestone. Row 0 must hold the sword, bread and torches; there must be a whole row of pickaxe + 8 cobblestone and a whole row of axe + 8 planks.
  - `aWeakHotbarTakesTheBestWholeToolRow`: 16 bread, 32 torches, iron pickaxe + 8 cobblestone, iron axe + 8 planks. Row 0 must be exactly pickaxe + 8 cobblestone, there must be a whole row of axe + 8 planks, and the bread and torches must not be in row 0.
  - `aLonePickaxeJoinsTheHotbar`: diamond sword, 16 bread, iron pickaxe + 12 × 64 cobblestone. Row 0 must contain the sword, the pickaxe and the bread.
  - `theSwordStaysWithoutFood`: diamond sword plus 20 assorted block stacks, with no food and no other weapon. Row 0 must contain the sword.
  - `aShovelAvoidsAWeakHotbar` (4b): 16 bread, 32 torches, iron shovel + 3 × 64 dirt. Row 0 must contain the shovel.
- [ ] **Step 2:** Push. Expected: `gametest` red on these.
- [ ] **Step 3:** Implement rules 1, 2, 4 and 4b.
- [ ] **Step 4:** Push. Expected: green, with every earlier game test still passing.
- [ ] **Step 5:** Commit message: `Inventory sorting: the hotbar keeps its best tools, whole tool rows stay whole`.

### Task 6: Client: clicks from the plan

**Files:**
- Modify: `src/client/java/frootloops/versus/mod/items_and_effects/inventory/InventorySorting.java`, `InventoryManagementHelper.java`, `ContainerDumping.java`
- Modify: `src/client/java/frootloops/versus/mixin/client/items_and_effects/inventory/InventoryScreen{Survival,Chest,Shulker,Creative}Mixin.java` (new calls)

**Interfaces:**
- Consumes: `InventorySorter.sort`, `MovePlanner.swapsWithSpare`, `MovePlanner.chooseSpare`.
- Produces: `enum InventorySorting.InventoryToSort { PLAYER_INVENTORY, CREATIVE_INVENTORY, CONTAINER_INVENTORY }` and `static void InventorySorting.sortInventory(AbstractContainerMenu menu, Minecraft client, Container inventory, InventoryToSort type)`, sorting 36 slots for the player and `getContainerSize()` slots for a container.
- Produces: `InventoryManagementHelper.placeOrDropCursorStack(AbstractContainerMenu, Minecraft, Container, int numSlots)` and `mergeStacksTogether(AbstractContainerMenu, Minecraft, Container, int numSlots)`, both clicking `menu.findSlot(inventory, i)` (B14).
- Apply: menu slots come from `findSlot` for every index; the sort is skipped if any is missing. Stack classes come from `ItemStack.matches`, with 0 for empty. The spare for player sorts is a hotbar index (prefer one whose target is empty, else 8), with position = index. For containers it comes from `chooseSpare` over the hotbar, where placeable means `menu.getSlot(menuSlot[0]).mayPlace(stack)`; when it returns -1, log and skip. Each planned p becomes `handleContainerInput(menu.containerId, menuSlot[p], spare, ContainerInput.SWAP, player)`. Creative keeps `setItem` plus `inventoryMenu.broadcastChanges()`.

- [ ] **Step 1:** Implement, and update the four screen mixins and `ContainerDumping` to the new signatures.
- [ ] **Step 2:** Push. Expected: `build` green (client compiles; JUnit and the mixin tests pass) and `gametest` green.
- [ ] **Step 3:** Commit message: `Inventory sorting: sort with the fewest swaps, click the right menu slots`.

### Task 7: Documentation

**Files:**
- Modify: `.claude/skills/players-versus/references/inventory-sorting.md` (new structure, hotbar rules, game test, debug property)
- Modify: `.claude/skills/players-versus/SKILL.md`, if its build/verify section lists the CI checks (add `gametest`)

- [ ] **Step 1:** Rewrite the reference to the new structure and rules. List B1–B14 under "Fixed on 26.3".
- [ ] **Step 2:** Push. Expected: everything green.
- [ ] **Step 3:** Commit message: `Inventory sorting: document the new pipeline and hotbar rules`.
