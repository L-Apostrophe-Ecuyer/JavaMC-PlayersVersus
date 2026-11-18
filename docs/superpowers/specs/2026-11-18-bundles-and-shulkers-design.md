# Bundles and shulker boxes: a wider bundle, a shulker box you can see into and reach into

Asked (2026-11-18):

- "Bundles have a UI overlay of items to extract but it only shows up to 11 items or something. I'd like to increase
  that to 19 items (5x4)"
- "Shulkers should have the same kind of overlay, but containing the shulker's items and having empty slots where there
  are empty slots in the shulker. When you hover over this UI and scroll, it scrolls between items rather than between
  slots. In the inventory, you can quickly pull out a selected item, or insert something into the shulker like a bundle."
- "This shulker UI should optionally be also available as a small overlay on shulkers in-game when targeting the block,
  where you have to actually scroll to select any items to avoid bad inputs."

## Now

- **The bundle's tooltip** is a grid 4 wide and 3 high: up to 12 items; past 12 it shows fewer (8 to 11, so that its top
  row is as full as the bundle's last row would be) and a "+N" cell for the rest. Scrolling over the bundle selects among
  the items shown; a right click takes out the selected one (or the first).
- **A shulker box's tooltip** is five lines of text ("Diamond x5"), then "and N more...". Nothing is selected, nothing
  taken out or put in without placing the box and opening it.

## Decisions

### The bundle: 5 × 4

1. The grid is **5 wide and 4 high**: up to **20 items**; past 20, **19 items and a "+N" cell** (bottom right, where
   vanilla puts it) for the rest. The grid is always full when the bundle overflows (vanilla's rule of matching the last
   row goes: it hid up to 4 items for the look of it).
2. Scrolling selects among those 19 or 20, as before; the server takes the same count (the shared rule,
   `BundleContents.getNumberOfItemsToShow`).
3. Everything else is vanilla's: the slot sprites, the highlight, the selected item's name above, the fullness bar (as
   wide as the grid, 120), the empty bundle's text.

### The shulker box's tooltip

4. A shulker box with anything in it shows a **compact grid of its 27 slots, 9 × 3, in the box's own order**: cells of
   9 pixels a pixel apart in a one-pixel frame (91 × 31 pixels), the items at half size (8 pixels), an item where the
   box has one and an empty cell where it hasn't. The grid is **in the box's own colour**: the dyes' usual colours, the
   undyed box's purple, in three shades (the frame, the cells, the lightened selected cell, outlined). An empty
   shulker box shows no grid (as vanilla shows nothing). The five lines of text go (the grid says it all); other
   containers' lines (a chest picked with its contents) stay.
5. Hidden as vanilla hides it: a box whose `tooltip_display` hides its contents shows no grid.

### The shulker box in the inventory

6. **Scrolling over a shulker box in a slot selects among its items, skipping the empty slots**, in the box's order,
   wrapping round (scrolling down from nothing selects the first item, up the last), as the bundle does. The selected
   item is highlighted and named above the grid. Leaving the slot, or clicking it, unselects (vanilla's bundle rule).
7. **Right click with an empty cursor takes the selected item** (its whole stack) onto the cursor. With nothing selected
   the right click is vanilla's (the box is picked up): nothing comes out of a box the player didn't choose from.
8. **Putting in, as a bundle does:** a left click with an item on the cursor onto a shulker box in a slot puts as much
   of it in as fits (onto stacks of the same item first, then the first empty slots); a left click with a shulker box on
   the cursor onto an item in a slot takes that item in. What can't go in a shulker box (another shulker box, anything
   `canFitInsideContainerItems` refuses), or a box with no room for any of it, leaves the click vanilla's (the two
   swap): the player is never stuck holding something they meant to swap.
9. Only a single box (a stack of one) does any of this; a slot that can't be taken from or put into (`mayPickup`,
   `mayPlace`, `allowModification`) is left alone. The sounds are the bundle's.
10. The selection is kept as the bundle keeps its own: on the box's contents, on each side, not saved and not sent
    (the client tells the server what it selected, as for bundles); creative mode works as for bundles.

### The shulker box in the world: a peek

11. **Looking at a placed shulker box with an empty main hand** (within reach, nothing open) shows the same compact grid
    below the crosshair, with the selected item's name. With something in the main hand there is no peek: the wheel is
    the hotbar's and the use is that item's (or the box's opening, as ever). The server shows it only to an empty main
    hand too.
12. **Nothing is selected until the player scrolls**: while the peek shows, the mouse wheel selects among the box's
    items (as in the inventory) instead of the hotbar. Looking away unselects.
13. **Using the box with an item selected takes that stack into the player's inventory** (as much as fits; the rest
    stays) instead of opening it, and unselects. With nothing selected, using the box opens it, as ever.
14. **Sneaking is the way out:** while sneaking the peek doesn't take the wheel (the hotbar scrolls) and using the box
    does what vanilla does.
15. **Optional:** a key ("Shulker box peek", unbound by default, under the mod's controls) turns the peek on and off; it
    starts on, and the choice is kept between games (`config/players-versus-client.properties`).
16. **The server decides what may be seen and taken**: only a box the player could open now (in reach, not locked to
    them, its lid free to open, no loot table still to roll, not a spectator). The client asks for the box's contents
    when it starts looking and every 10 ticks while it looks; the server answers with them (or with nothing). Taking
    names the slot and the item the player saw there; the server takes it only if that item is still there, then sends
    the contents again.

## Checks

- Unit: the bundle's count to show (`BundleGridTest`); the shulker selection's steps (`ShulkerSelectionTest`); putting
  in and taking out of a box's 27 slots (`ShulkerItemsTest`).
- Game tests (`ShulkersGameTest`, a survival player and their inventory menu, the real clicks): the clicks of 7–9; the
  tooltip image and its text; the peek's answers and refusals; taking from a placed box (all, part, the wrong item).
- In game: the gallery's screenshots of the bundle's and a shulker box's tooltips and of the peek.

## As built

- **The bundle** (`BundleGrid`, `BundleContentsMixin`, client `WideBundleTooltip` registered with Fabric's
  `ClientTooltipComponentCallback`): as decided. Seen in game (the gallery, 25 stacks: 19 and "+12").
- **Into and out of a box** (`ShulkerItems`, `ShulkerSelection`, `ShulkerClicks` through `ItemShulkerClicksMixin` on
  `Item`'s two click overrides, the selection on `ItemContainerContents` by `ItemContainerContentsMixin`,
  `SelectShulkerSlotPayload`): as decided. With the mixin off, the three new clicks' tests fail and the five that keep
  vanilla's clicks pass.
- **The tooltip** (`ShulkerTooltip`, `ItemShulkerTooltipMixin`; the text lines dropped in `ItemStack.addToTooltip` by
  `ItemStackShulkerTooltipMixin`, since vanilla hands the contents' own tooltip the component map, not the stack; client
  `ShulkerGrid`, `ClientShulkerTooltip`, `ShulkerMouseActions` added to every container screen): as decided. Seen in
  game, the wheel skipping an empty slot to the second item.
- **The peek** (`ShulkerPeeking` and three payloads on the server's side; client `ShulkerPeek`, `MouseHandlerPeekMixin`,
  `MultiPlayerGameModePeekMixin`; `VersusSettings.Shulkers`): as decided, an empty box showing nothing (as its tooltip).
  Taking puts in only what the inventory has room for (vanilla's `Inventory.add` deletes the rest for a player with
  infinite materials). The use key held on after a take doesn't open the box until it is let go. With each of
  `ShulkerPeeking`'s guards taken out, a test fails (reach, the lock, the loot table, the lid, the item). Seen in game.
- **Checks:** `BundleGridTest`, `ShulkerSelectionTest` (the wheel's steps over a slot predicate: item stacks can't be
  made in this project's unit tests, so the put-in and take-out cases are game tests); `ShulkersGameTest` (25). The
  gallery's new shots (`gui`, `hover`, `scroll`, `shot`, `close`, `peek`) and `tools/gallery-containers.txt`.
- **After the final review:** a menu closing unselects every box in it (`AbstractContainerMenuShulkerMixin`), and a click
  left vanilla's (a pick-up, a swap) unselects the box first, so no selection outlives its hover and a right click
  never takes from a box the player didn't just choose from; the client unselects on a throw too. The peek keeps to
  where the player may use blocks (`mayInteract`: spawn protection, the world border) and to the living. With the HUD
  hidden (F1) the peek takes neither the wheel nor the use (`PeekRules`, unit-tested). `ShulkersGameTest` (28),
  `PeekRulesTest` (3).
- **The grid and the empty hand** (`ShulkerGrid` redrawn with fills and half-size items; `ShulkerColors`, a table of the
  dyes' colours since 26.4's `DyeColor` has none, in three shades with a floor so that a black box's cells still show;
  `ShulkerTooltip` carries the box's colour, the peek reads it from the block; `PeekRules.takesInput` and
  `ShulkerPeeking.openable` need an empty main hand; `VersusSettings.Shulkers.PEEK_SCALE` is gone, the grid being small
  already). `ShulkerColorsTest` (5), `PeekRulesTest` (4), `ShulkersGameTest` (30). Seen in game: the peek in blue and
  lime, the tooltip in red and black. With an empty main hand there is always a free slot, so a peek take never lacks
  room (`takingFillsTheSlotTheEmptyHandLeavesFree`); `ShulkerPeeking.take`'s room check stays as a guard.

## The peek, refined (2026-11-18)

Asked: "Refine the shulker in world ui a bit more; user experience, expected behavior & intent vs result, possible
conflicting inputs". What playing it showed, and what changes:

1. **A take filled the hand and the peek went away.** The game puts a new stack in the first empty slot, often the
   one held, so after one take the hand held it and the peek (empty hand only) vanished. **A take keeps the held slot
   free**: onto stacks of the same item first, then the first free slot other than the held one; the held slot only if
   nothing else has room.
2. **The wheel stuck on an empty hotbar slot.** Scrolling through the hotbar while looking at a box, the first empty
   slot made the peek appear and the next notch went into the box. **The hotbar keeps a scroll it began**: the peek
   takes the wheel only once the wheel has rested from the hotbar for half a second (10 ticks).
3. **After a take, the place was lost.** The selection went back to nothing, and the next turn of the wheel started
   from the first item again. **The peek remembers where it was**: after a take nothing is selected (a second take
   still needs a turn of the wheel), and the next turn goes on from the slot just emptied.
4. **No way back to opening the box.** With an item selected, a use always took it. **The wheel passes through
   nothing** between the last item and the first: one more notch unselects, and a use opens the box again.
5. **The item could change under the selection.** Another player's move could leave the highlight on something else,
   and a take named only the item's kind. **A changed slot unselects**, and a take names the whole stack seen (its
   components too, so not another enchanted sword); the server takes it only if that is still what is there.
6. **A use held from before took.** A use key already held when the wheel selected would take at its next repeat.
   **A take needs a fresh press** of the use key after the selection.
7. **The grid flickered at a box's edge.** A one-tick slip of the crosshair reset the selection and asked the server
   again. **The peek outlasts a slip of 4 ticks**: still drawn, the selection kept, but the wheel and the use the game's
   until the crosshair is back on the box.
8. **Nothing told a new player what to do.** **A dim "Scroll to choose" line** shows under the grid while the wheel
   would choose and nothing is chosen; a selection shows its name and count ("Golden Apple ×3").
9. **The grid could reach the action bar** on a small screen. It moves up as needed to keep above it, never onto the
   crosshair.

**As built:** all nine. `ShulkerPeeking.take` names the stack seen (`TakeFromShulkerPayload` carries it) and gives it past
the held slot; `PeekCursor` (the selection through nothing, the place kept, unselecting a changed slot);
`PeekRules.takesWheel` (`VersusSettings.Shulkers.PEEK_WHEEL_REST_TICKS = 10`); `ShulkerPeek` with the slip
(`PEEK_SLIP_TICKS = 4`), the fresh press, the hint and the layout. `PeekCursorTest` (8), `PeekRulesTest` (5),
`ShulkersGameTest` (32: `aTakeKeepsTheHeldSlotFree` and `takingAnotherOfTheSameKindTakesNothing` failed first, against
vanilla's placement and a take by kind). Seen in game: the hint, then "Diamond ×12" on an orange box.
