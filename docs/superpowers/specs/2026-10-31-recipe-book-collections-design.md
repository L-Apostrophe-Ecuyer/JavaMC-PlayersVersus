# Recipe book: groups, collections and their popups

Asked for on 2026-10-31, after the feedback batch. Supersedes the recipe book's pins and the family recipe groups that
`RecipeRules` wrote into recipe files.

## What the player sees

- **No pins.** Right-click no longer pins anything; nothing is listed first for having been pinned.
- **Groups** are a single item's recipes (several ways to make the same thing). A group's button shows the item; its
  popup is vanilla's: the recipes' shapes, selectable. Left click fills the crafting grid with the shown recipe (shift
  or a double click for the most crafts, as before) and, when the item has several recipes, also opens the group's
  popup and keeps it open. Hovering opens it until the mouse leaves the button and the popup. This popup doesn't move.
- **Collections** are similar items made by different recipes, gathered automatically from what the recipes make
  (`RecipeFamilies`): a block's material family (all oak: planks, stairs, slabs, fences, doors, boats...; all deepslate;
  all copper), tools and armor by material, dyed things by kind (all sixteen wools, beds, carpets, dyes...). Anything
  else stands alone as its group. A collection's button shows its items in turn with the stacked look.
  - Hovering it opens its popup, the items' icons (not recipe shapes), until the mouse leaves the button and the popup.
  - Left click keeps it open as a window: dragged by its title bar anywhere over the screen, closed with its x or Esc.
    One window at a time: clicking another collection moves the window to it.
  - Its icons are smaller than the inventory's (12 pixels in 14-pixel cells), uncraftable ones on red as in the book.
  - In the popup or window, an item is used as a group's button on the page: left click fills the grid, and an item
    with several recipes opens its recipe shapes there, kept open.
- Recipe files keep vanilla's groups: the book makes its groups and collections itself, so a data pack's recipes join
  them as vanilla's do.

## How

- `RecipeFamilies` (common, pure, tested): the collection an item's id belongs to, from its path and the set of item
  paths in the registry: tools and armor by material; dyed kinds where all sixteen colours of the kind exist (the plain
  undyed item joins); else the material family, by `RecipeRules`' old family words. `RecipeRules` keeps the stairs,
  slab and pressure plate shapes only.
- `RecipeBookCollections` (client), at `RecipeBookPage.updateCollections`'s head: the book's collections (vanilla's,
  filtered by tab, search and "craftable only", their recipes selected for the grid) are split by result item into
  `ItemRecipes` (a `RecipeCollection` over one item's recipes, answering craftable and selected from the vanilla
  collections it came from), then gathered by `RecipeFamilies` into `FamilyRecipes` (a `RecipeCollection` over its
  items' recipes, keeping the items), when two or more share a family. Sorted craftable first, then by the inventory
  sort, as before.
- `RecipeBookPageMixin`: clicks and hover as above; the group popup's hover/pin state in `RecipeGroupHover` (with a
  pin: while pinned, an open popup stays whatever the mouse does).
- `FamilyWindow` (client): the collection popup and window, drawn at the end of the book in a stratum of its own (the
  group popup is drawn above it when open); input at the head of the book's `mouseClicked`, `keyPressed` (Esc); its
  drag follows the mouse while the left button is held. `RecipeWindowLayout` (common, pure, tested): cells, title bar,
  close box and size. A click on the window never reaches the slots or drops the carried item
  (`hasClickedOutside`, `isHovering`).
- Removed: `RecipePins`, `RecipePinStore`, the pin sprite, `DO_RECIPE_BOOK_PINS`, `DO_FAMILY_RECIPE_GROUPS`.

## Rulings

- A collection's button doesn't fill the grid on a click (it opens the window): the user's answer.
- Items with one recipe in a collection fill the grid on a click, those with several also open their shapes.
- Dyed kinds group by kind (all wools), material families by material (all oak), as asked; an item belongs to the first
  that applies: tools/armor, dyed kind, material family.
