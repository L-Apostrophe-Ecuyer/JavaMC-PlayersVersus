# Data Overrides & Resource-Pack Integrity

This doc orients the large (2405-file) JSON layer and gives a validation strategy; it does not
enumerate individual recipe/loot-table/advancement changes (~679 recipes, ~150 advancements —
too many to usefully transcribe) or individual asset files.

## Layout
- `data/minecraft/**` — **vanilla-namespace overrides**: recipes (679, the largest single JSON
  folder in the repo), advancements (organized by vanilla category:
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

## Extension recipe: adding/changing a recipe, loot table, or advancement
- New content → `data/players-versus/**`, mirroring the existing subfolder taxonomy above (don't
  invent a new top-level category if an existing one fits).
- Changing vanilla behavior → `data/minecraft/**`, same path vanilla itself uses (e.g. a recipe
  override replaces `data/minecraft/recipe/<id>.json` byte-for-byte at the same path) — remember
  this affects **every world type**, including a player's vanilla-preset world.
- Always add the matching advancement-unlock recipe entry (`advancement/recipes/**`) when adding a
  new craftable recipe with a new unlock item, following vanilla's own recipe-advancement pairing
  convention, unless the recipe is deliberately always-unlocked.
