# Data Overrides & Resource-Pack Integrity

This doc orients the large (2405-file) JSON layer and gives a validation strategy; it does not
enumerate individual recipe/loot-table/advancement changes (~679 recipes, ~150 advancements —
too many to usefully transcribe) or individual asset files.

## Layout
- `data/minecraft/**` — **vanilla-namespace overrides**: recipes (183 since 26.4 — only real recipe
  changes; see "Recipe rules" below), advancements (organized by vanilla category:
  `recipes/{brewing,building_blocks,combat,decorations,misc,redstone,tools,transportation}`, plus
  `story/`), loot tables (`blocks`, `entities`(+`sheep`), `chests`(+`trial_chambers`,`village`),
  `shearing/sheep`, `spawners`(+`ominous/trial_chamber`)), `archaeology` loot, `enchantment`,
  `enchantment_provider/trades`, `tags/**` (block/damage_type/enchantment/entity_type/item/
  worldgen), `trim_material`, and the worldgen tree covered in `worldgen-content-and-structures.md`.
  **Every file here changes vanilla behavior for every world type**, not just PV content — the same
  caveat as vanilla-namespace worldgen data.
- `data/players-versus/**` — mod-namespace new content: recipes (grouped:
  `breaking_down_armor`, `brewing/{concentrate_crafting,concentrate_smelting}`, `building_blocks`,
  `coloured_blocks/dye_combining`, `farming`, `inventory_management`, `smelting_additions`,
  `wood_slab_combining`, plus top-level), advancements (`recipes/{brewing,building_blocks,
  coloured_blocks}`, `story`), loot (`archaeology`, `blocks`, `chests/trial_tower`, `entities`,
  `spawners/pyramid`), `enchantment/**` (`enchanting-and-anvil.md`), `damage_type/**`, and the full
  `tags/**` set (`block`, `damage_type`, `enchantment/exclusive_set`, `item`(+`enchantable`),
  `worldgen/structure`).
- `assets/minecraft/**` — vanilla asset overrides (12 blockstates, 70 models, 243 textures, 8 lang,
  2 atlases, 3 items, 1 sounds.json) — same "affects everyone" caveat.
- `assets/players-versus/**` — mod assets: 84 blockstates, ~300 models (block+item, incl.
  `copper_equipment`(+`_exposed`), `concentrates`, `bile`), ~230 textures (block+item+entity+gui+
  particle+map), 40 sounds (incl. `music/caves/**`, `ambience/**`), 8 lang files (`en_{au,ca,gb,ud,
  us,za}`, `fr_{ca,fr}` — **not a full localization set**, only English variants + French; a
  contributor adding a new string must touch at minimum `en_us.json` and should consider whether the
  other 7 need the same key), `equipment/**` (copper armor's `EquipmentAsset` definitions), `particles/**`.
- `assets/unused/**` — a deliberately-parked "maybe later" bucket (17 unused blockstates, 56 unused
  models, 14 unused textures, incl. a whole `dripstone_{clay,vanilla,muddy}_style` texture set) —
  **not dead weight to clean up casually**; treat as the maintainer's own idea-parking-lot unless
  told otherwise.

## Known integrity risks (not verified in this pass — recommended checks before a release)
No automated validation script was written or run during this pass (time-constrained solo audit,
not the original 18-agent plan). Before trusting the data layer, a maintainer should run (or ask an
agent to run) at minimum:
1. **JSON parse check** — every file under `src/*/resources/**/*.json` parses (`./gradlew build`
   already exercises the ones Loom/Minecraft's own data loading touches, but a dedicated parse-all
   pass catches orphaned/malformed files data loading never reaches).
2. **Model/texture reference resolution** — every `blockstates/*.json` → `models/**` → `textures/**`
   chain resolves, checking both `assets/players-versus/**` and vanilla `assets/minecraft/**` (the
   latter extractable via `./gradlew genSources`-adjacent Loom caches — see `architecture.md` for
   the jar paths, or unzip the `minecraft-clientOnly`/`minecraft-common` non-sources jars directly
   for raw asset files, since `genSources` only decompiles `.java`).
3. **Registry-id cross-reference** — every custom id referenced in a recipe/loot table/advancement/
   worldgen JSON (`players-versus:*`) has a real Java-side registration (grep the id string against
   `mod/**` registration calls) — a typo here fails silently at runtime (missing recipe/feature),
   not at build time.
4. **Lang key coverage** — every registered block/item/entity/effect/enchantment has at least an
   `en_us.json` translation key; a missing one falls back to the raw id string in-game.
5. **Loot table presence** — every custom block has a loot table (even a trivial self-drop) unless
   deliberately drop-nothing (compare against `blocks-and-environment.md`'s bile-block "drops
   nothing" case, which is intentional and confirmed in Java, vs. a block that simply never got one).

None of these were run in this pass; treat this section as a **recommended follow-up task**, not a
completed audit. If asked to "validate the data layer," implement these checks (a small Java program
using the project's own `Gson` dependency, or a set of shell+`jq`-equivalent scripts — note this
Windows dev machine has no `python`/`node`/`jq` on PATH per `architecture.md`, so lean on Java or
PowerShell's native JSON cmdlets if scripting outside the Gradle build) rather than assuming they
already exist.

## Recipe rules (no file per stairs/slab/group)
`mod/items_and_effects/recipes/RecipeRules` changes every recipe file by rule as it loads
(`mixin/items_and_effects/recipes/RecipeFileMixin`, a `@WrapOperation` on the decode in 26.4's
`RegistryLoadTask$PendingRegistration.loadFromResource`, limited to `Registries.RECIPE`), so the rules reach
mods' and data packs' recipes too:
- groups are left as the files have them: the recipe book gathers its own collections (below; until 2026-10-31
  the rules rewrote the files' groups into families).
- stairs (`StairBlock` result, vanilla's 6-cell staircase) → `# `/`##`, same count (4); slabs (`SlabBlock`,
  row of 3) → `##`, two thirds of the count (6 → 4); pressure plates (`BasePressurePlateBlock`, row of 2) →
  `###`, twice the count (1 → 2). Any other shape is left alone.
- So **write stairs and slab recipes in vanilla's shape** (the mod's own and kept overrides are), and don't
  add overrides just to regroup or reshape. Kept `data/minecraft/recipe` files are real changes (doors 2,
  trapdoors 6, cut copper 8, nuggets, the mod's copper tools/armor and clay, terracotta…), with vanilla's
  category kept. Setting: `VersusSettings.Items.DO_CHEAPER_STAIRS_AND_SLABS`.

Recipe book screen (client; `mixin/client/items_and_effects/inventory/recipe_book/`, design in
`docs/superpowers/specs/2026-10-31-recipe-book-collections-design.md`):
- **Groups and collections.** At `RecipeBookPage.updateCollections`'s head, `RecipeBookCollections` splits vanilla's
  collections (already filtered by tab, search, "craftable only", and selected for the grid) by result item into
  groups (`ItemRecipes`: one item's recipes) and gathers groups into collections (`FamilyRecipes`) by
  `RecipeFamilies.collection` (common, tested: tools/armor by material, dyed kinds where all 16 colours exist, else
  the material family by shape/finish words) when two or more share one. Both extend `SelectedRecipes`, a
  `RecipeCollection` answering selected/craftable from snapshots of vanilla's, so vanilla's buttons, popup and
  `tryPlaceRecipe` take them. Search: vanilla kept a collection when any recipe matched, so split items are filtered
  again by name or id.
- **Group popup** (vanilla's overlay of recipe shapes): opens on a 150 ms dwell (`RecipeGroupHover.DWELL_MILLIS`),
  closes when the mouse is outside button and popup; a left click places the shown recipe *and* opens it pinned
  (`RecipeGroupHover.pin`), as does a right click or a click in it; a click outside closes it and goes through
  (`DO_RECIPE_BOOK_CLICK_THROUGH`); Esc closes it. Refreshed in place after the book refreshes.
- **Collection popup/window** (`FamilyWindow`, layout `RecipeWindowLayout`, common, tested): icons at 12 px in 14 px
  cells, six to a row, under a title bar with a close box; peeks on hover beside the button, a click pins it; the
  title bar drags it (the book's `mouseDragged`, ended by the next press); one at a time. An icon acts as a group
  button (left click places, several recipes open their shapes beside it). Drawn by `RecipeBookComponentMixin` at the
  end of the book's render in its own stratum, the group popup after it (the page's own overlay draw is skipped);
  the book takes clicks on it first, `hasClickedOutside` and `AbstractRecipeBookScreenMixin.isHovering` keep the
  slots under it from being clicked, hovered or tooltipped. Hooks: `RecipeBookPopups` (implemented by the page
  mixin), `RecipeBookComponentAccessor`.
- Order: craftable first, then the inventory sort (`RecipeBookSorting` → `ItemOrdering`, through
  `RecipeBookOrdering`'s merge sort, which never throws on the sort's not-quite-consistent "goes before" test);
  `OverlayRecipeComponentMixin` orders the group popup's sections and says where it is (vanilla's `isMouseOver` is
  always false). `RecipeButtonMixin` lists a group's recipes craftable first (icon and click cycle through them,
  `DO_RECIPE_ICONS_CYCLE_CRAFTABLE`). `AnimatedResultButtonMixin` draws collections stacked, groups on vanilla's
  many-recipes slot.
- `RecipeBookComponentMixin` also wraps `tryPlaceRecipe` in the book's `mouseClicked`: the second click of a
  double-click places the most crafts, or one again if already placed for the most (`DO_RECIPE_DOUBLE_CLICK_MAX`).
- Settings: `VersusSettings.QOL.DO_RECIPE_BOOK_COLLECTIONS` (off: vanilla's groups, sorted), `DO_RECIPE_BOOK_HOVER_GROUPS`,
  `DO_RECIPE_BOOK_CLICK_THROUGH`, `DO_SMART_RECIPE_ORDER`. Pins were removed on 2026-10-31.
- Tests: `RecipeFamiliesTest`, `RecipeWindowLayoutTest`, `RecipeGroupHoverTest`, `RecipeBookOrderingTest`; the mixins
  only a look in game.
- Tests: `src/test/.../mod/items_and_effects/recipes/RecipeRulesTest`.

## Extension recipe: adding/changing a recipe, loot table, or advancement
- New content → `data/players-versus/**`, mirroring the existing subfolder taxonomy above (don't
  invent a new top-level category if an existing one fits).
- Changing vanilla behavior → `data/minecraft/**`, same path vanilla itself uses (e.g. a recipe
  override replaces `data/minecraft/recipe/<id>.json` byte-for-byte at the same path) — remember
  this affects **every world type**, including a player's vanilla-preset world.
- Always add the matching advancement-unlock recipe entry (`advancement/recipes/**`) when adding a
  new craftable recipe with a new unlock item, following vanilla's own recipe-advancement pairing
  convention, unless the recipe is deliberately always-unlocked.
