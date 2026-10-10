# Items, Equipment & General Item Behavior

## The vanilla→modded item replacement pattern (core mechanism, reused everywhere)

Several vanilla items (copper tools/armor, fermented spider eye, blaze powder, magma cream,
recovery compass, ladder, clay, mud, packed mud, mud bricks/slab/stairs, mud brick wall) are
**silently swapped for a modded replacement** via a small, elegant three-part mechanism in
`VanillaItems` + two mixins — understand this before touching any of the affected items, and reuse
it (rather than reinventing per-item hacks) if you need to replace another vanilla item wholesale:

1. `VanillaItems.ITEM_REPLACEMENT_MAP` (`Map<Item, Item>`) is the single source of truth: vanilla
   item → mod item.
2. `VanillaItemsMixin` (target `Item`, implements `ToggleableFeature`) makes every vanilla item in
   the map report `isEnabled(FeatureSet) = false` — this is the same mechanism vanilla uses to
   disable experimental items, repurposed to hide the vanilla item from crafting-book/creative
   listings etc.
3. `ItemStackMixin`'s two `@ModifyVariable`s on `ItemStack`'s constructors redirect **every**
   `new ItemStack(vanillaItem, count)` call (both the `RegistryEntry<Item>` and `ItemConvertible`
   constructor overloads) to construct with the replacement item instead — so recipes, loot tables,
   commands, and other mods that reference the vanilla item by id still work, but the actual
   `Item`/`ItemStack` instance that comes back is always the mod's replacement.

**Consequence:** grep for `Items.CLAY`/`Items.MUD`/etc. directly in this codebase and you'll rarely
find the modded item referenced explicitly outside `CustomBlockItems`/`CustomEquipment`/`CustomBrewingItems`
— it's the vanilla id that flows through data files, and this pattern is what makes that resolve to
the mod's version at runtime. **When adding a new "replace vanilla X with modded X"**, add one line
to `ITEM_REPLACEMENT_MAP` and nothing else needs to change on the data side.

## Tool & weapon rebalance (`VanillaItems.modifyVanillaToolsAndWeapons`)

Every vanilla tool/weapon across all 6 material tiers (wood/stone/iron/gold/diamond/netherite) gets
its `ATTRIBUTE_MODIFIERS` component rebuilt from the shared base stats in `Combat` (see
`combat-and-players.md`: `SWORD_DAMAGE=3.0/SPEED=1.6`, `AXE=6.0/1.0`, `PICKAXE=2.0/1.2`,
`SHOVEL=3.0/1.4`, `HOE=1.0/2.0`) plus a flat per-tier `extra` damage bonus (wood +0, stone +1,
iron/gold +2, diamond +3, netherite +4 — **iron and gold share the same damage tier**, unlike
vanilla where gold tools deal iron-tier damage but netherite/diamond differ; this is intentional
parity, not a copy-paste bug, confirmed by both being assigned `extra = 2.0` in the same block).
Shovels and hoes get `+1.0` attack-knockback baked in (a small "shovel/hoe as backup weapon" buff).
Swords additionally get a `BLocksAttacksComponent` (**swords can block!** — see Shields below) whose
blocking amount scales with material (`material.attackDamageBonus() - 1F`), using a **material-tiered
blocking sound** (`CustomSpecialEffects.SWORD_BLOCKING_{WOOD,STONE,METAL,DIAMOND}` — iron/gold/
netherite share the "metal" sound). Shields are made **instant-block** (`blockDelaySeconds = 0.0625`
vs. vanilla's longer wind-up) via `createDamageBlockingComponent`.

`createToolAttributeModifiers`/`createArmorAttributes`/`createDamageBlockingComponent` are the shared
builder functions — **reuse these for any new weapon/armor/blocking item** rather than hand-building
`AttributeModifiersComponent`/`BlocksAttacksComponent` from scratch; `CustomEquipment`'s copper line
already does exactly this.

Other `DefaultItemComponentEvents.MODIFY` tweaks bundled in the same pass: Glistering Melon Slice
becomes edible (nutrition 4, Regeneration II for 10 ticks); leather armor gains small knockback
resistance; chainmail gains real armor toughness (previously near-useless in vanilla); turtle
helmets get a bigger buff (`createTurtleArmorAttributes`: armor + 0.5 toughness + 0.5 water-movement
efficiency + **+1.0 attack damage** — the extra attack damage on a *helmet* is unusual, verify it's
intentional flavor ("turtle headbutt") before assuming it's a copy-paste error from the sword path).

## Stack sizes & food eat-times (`VanillaItems.modifyVanillaStackSizes`/`modifyVanillaFoods`)

Driven entirely by `VersusSettings.Items` constants (`combat-and-players.md`'s sibling doc has the
`VersusSettings` overview) — throwables, minecarts, boats(+chest variants), potions, buckets, cake,
and ~38 explicitly-listed vanilla foods each get a category-based `MAX_STACK_SIZE` and/or
`CONSUMABLE` (eat-time) override. A final catch-all loop re-scans `Registries.ITEM` for any
maxCount-1 item that's a saddle/body-equipment, boat, minecart, armor stand, or end crystal and
stacks those too — **this loop has a latent bug**: its damageable-item check
(`if (item.getDefaultStack().getComponents().contains(DataComponentTypes.MAX_DAMAGE)) return;`)
uses `return` instead of `continue` inside a `for` loop, so **the very first durability-having item
encountered in `Registries.ITEM` iteration order silently aborts the entire remaining scan** for
saddle/boat/minecart/armor-stand/end-crystal stack-size bumps. In practice this is likely masked
because all the boat/minecart/potion/bucket cases are already handled by the explicit calls above
this loop, and only the saddle/body-equipment branch depends on the loop actually completing — worth
a real fix (`return` → `continue`) since it's a genuine logic bug, low real-world impact given the
explicit list above covers the common cases.

`modifyVanillaFoodItem` classifies each food as bottled/stew/bucket/other by its `USE_REMAINDER`
component (converts to bottle/bowl/bucket) and as "drink" vs "eat" by remainder + consume sound,
then rebuilds its `ConsumableComponent` with the right `VersusSettings.Items.EAT_TIME_*` and
`MAX_COUNT_*`. **Adding a new food:** call `modifyVanillaFoodItem(Items.X, VersusSettings.Items.EAT_TIME_*)`
in the list, don't hand-build the component.

## Shields, swords-as-shields & parry (`LivingEntityBlockingMixin`, target `LivingEntity`)
`getDamageBlockedAmount` is a full `@Overwrite` of vanilla's blocking-damage-reduction logic, layering
in: Riposte's extended parry window (see `enchanting-and-anvil.md`), shield-piercing-arrow bypass
unless parried, and (implicitly, via the sword `BLOCKS_ATTACKS` components added above) the same
logic applying whenever *any* sword is the "blocking item" — swords block like shields once this
component is present, at a lower base amount than a dedicated shield. Re-verify this method against
decompiled vanilla on any MC bump — `@Overwrite` on core combat math is exactly the highest-risk
pattern flagged in `architecture.md`.

## Elytra & fireworks
- **`ElytraGlidingMixin`** (target `LivingEntity`): durability loss while gliding is slowed at low
  speed (360 ticks between losses below `velocity² < 2.0`, vs. 80 ticks otherwise — vanilla is a
  flat 10) — **and** getting attacked within 2 game-ticks of an attack instantly cancels gliding
  (`canGlide` forced false) — a deliberate "can't tank hits mid-flight" balance choice.
- **`ElytraFireworksMixin`** (target `FireworkRocketItem`): while gliding (and not creative), using
  a firework instead damages the **elytra itself** by up to 8 durability per use (capped at
  durability-left−1, i.e. never destroys it outright from one firework) — "elytra durability is
  mainly spent on fireworks" per the inline comment, de-emphasizing elytra's own natural wear.
- **`ElytraFireworksSpeedMixin`** (target `FireworkRocketEntity`): initial/extra speed boost cut
  from vanilla 0.5 to 0.2/0.1, acceleration constants roughly halved, and **firework lifetime is
  shortened when the shooter is gliding**, with an explicit per-flight-tier comment table (Flight 1
  average 16→8 ticks, Flight 2 26→21, Flight 3 36→32) — deliberately widens the gap between low- and
  high-tier firework rockets for elytra boosting. A rocket auto-detonates if the shooter stops
  holding a firework in either hand or hits a wall, and loses an extra tick of life per tick spent
  in water/rain.

## Trident (`TridentEntityMixin`, target `TridentEntity`)
Always behaves as if Loyalty is one level higher than actually enchanted (`alwaysLoyal`) — a
baseline "it comes back eventually" QoL even on an unenchanted trident, once it returns from a
throw. Falling out of the world (`y < bottomY`) marks it as having dealt damage so it despawns
rather than falling forever. **Drowned-specific:** a drowned's own thrown trident tracks back to
its thrower's hand (clears the drowned's held-item slot while the trident is in flight, then
re-equips it once the trident gets within 3 blocks on return) — makes drowned-thrown tridents behave
like a boomerang the mob itself "holds" during flight, rather than vanishing/duplicating.

## Flint & steel (`FlintAndSteelItemMixin`)
`useOnBlock`: right-clicking any smoldering/extinguished torch (regular or wall — see
`blocks-and-environment.md`) with flint & steel relights it back to a normal vanilla torch (state
copied via `getStateWithProperties`), consuming 1 durability. `useOnEntity` (override, not
`@Inject`): igniting a living entity directly always succeeds and **sets the user as the entity's
attacker** — makes flint-and-steel arson properly count as a PvP/aggro action instead of an
untracked ignition.

## Throwables & the slimeball entity
- **`SlimeballsAndFireChargesMixin`** (target `Item`, `@Inject use HEAD`): makes plain Slimeballs
  and Fire Charges throwable by hand (vanilla: Slimeballs aren't throwable at all; Fire Charges only
  via dispenser/off-hand-with-crossbow context) — Slimeball spawns a custom `SlimeballEntity`
  projectile, Fire Charge spawns a real `FireballEntity` (a small, weak one, per `new FireballEntity(world, user, dir, 1)` explosion power). Both go on the standard consume-item/cooldown/stat path.
- **`SlimeballEntity`** (`mod/items_and_effects/throwing`, registered in `ModEntities`): a bouncy
  projectile — 2–3 random bounces off blocks (velocity reflected and damped per-axis on the hit
  face, `×-0.4` on the hit axis / `×0.6` on the others) before it expires; on hitting a living entity
  it does knockback + a small upward "boop" nudge, no damage. Purely a fun/traversal-denial toy, not
  a combat item.

## Item entities: burning, fire-immunity, despawn ("Note: this code is bad" — author's own comment)
`ItemEntityMixin` is explicitly flagged by its own inline comment as rough — treat any change here
with extra caution and prefer a rewrite-with-tests over an incremental patch if you touch it heavily.
- `setHealth` (`@Inject` on `initDataTracker`): item entities get 60 "health" (a burn-survival
  counter, not display HP) and, if their owner is a *dead* player, an artificially negative
  `itemAge` (`-12000`) — makes items dropped by a dead player's death-drop behave as freshly-aged
  for despawn purposes (works with the "important items despawn slower" logic in
  `PlayerEntityMixin.dropInventory`, see `combat-and-players.md`).
- `isFireImmune` (override): items are briefly fire-immune for their first 10 ticks of existence
  (protects freshly-dropped items from immediately catching fire from the block/lava that killed
  their owner); obsidian/crying obsidian/ender chest/enchanting table/enchanted golden apple item
  entities, and anything with `DAMAGE_RESISTANT`, are permanently fire-immune.
- `damage` (`@Inject` before the vanilla `emitGameEvent` call, cancellable): when `health == 0`
  (i.e. this item has now "died" once to fire/lava and is being re-evaluated), routes to
  `doFireDamageTransformation` for lava/fire damage or `doRegularDamageTransformation` for
  anything else. **`doRegularDamageTransformation` is an empty, unimplemented stub** — non-fire
  damage to a "dead" item entity currently does nothing; if this is meant to handle e.g.
  explosion/cactus destruction of items, it needs an actual body, or the method (and its call site)
  should be removed if truly unneeded.
- `doFireDamageTransformation`: buckets empty themselves (placing their fluid, or turning back into
  lava bucket if burned extra-hot in soul fire); a burning stack of Totems of Undying makes all
  *other* item entities within 4 blocks glowing/invulnerable/never-despawning (a "totem saves your
  loot too" easter egg); everything else consults `BurningConversion.ITEM_BURNING_CONVERSION_MAP`
  (~150 entries — ores/ingots/blocks/tools/nuggets across gold/iron/copper/lapis/emerald/redstone/
  diamond, plus clay/mud/ice/snow/paper/stick special-cases) for a health budget (`itemExtraHealth`)
  and a result item (normal vs. "very hot"/soul-fire result) once the item's accumulated `itemAge`
  exceeds that budget. Ordinary logs-that-burn become coal if not extra-hot. **This table is the
  single place to add fire-transformation behavior for a new item** — don't special-case it in the
  mixin body; add a `BurningConversion.ITEM_BURNING_CONVERSION_MAP.put(...)` entry instead.

## `ItemStackMixin` / `ItemUsageMixin` / `VanillaItemsMixin` grab-bag
- `isEnchantable` (`@Inject RETURN`): items with an `ENCHANTABLE` component but zero current
  enchantments are always enchantable regardless of vanilla's normal gate; items with existing
  enchantments become **un**-enchantable once their summed min-power exceeds `4 × enchantability` —
  a soft cap preventing infinite enchant-table stacking on high-enchantability gear.
- `getMiningSpeedMultiplier` (`@Inject RETURN`): deepslate-specific multiplier table — netherite
  ×1.3, diamond ×1.1, iron unchanged (early-return), everything else (wood/stone/gold/non-pickaxes)
  ×0.6 — reinforces "you need at least iron for deepslate" from `PlayerEntityMixin.getBlockBreakingSpeed`
  (see `combat-and-players.md`) at the `ItemStack` level too; **keep these two in sync** if either changes.
- `ItemUsageMixin.getUseAction`: makes Recovery Compass report `UseAction.BOW` (drives the
  drawn-bow-style use animation/sound for its channeling use). `dontUseOffhandItemAfterExhausted`:
  after finishing a use action, proactively cooldowns the *other* hand's item briefly if it can't
  currently be used — specifically to stop "accidentally reuse the offhand item right after eating"
  misclicks; a dead commented-out block above it (`canOnlyBlockIfAttackIsCharged`) documents an
  abandoned idea (block-only-while-attack-charged) — safe to delete if you're cleaning up, or revive
  deliberately if that idea is wanted after all (currently blocking has no such gate).
- `VanillaItemsMixin`/`ItemStackMixin.modifyItemRegistry`/`modifyItemConvertible`: see the
  replacement-item mechanism at the top of this doc.

## Equipment: Recovery Compass, Decayable (copper) armor

- **`RecoveryCompassItem`**: a channeled-use (`UseAction.BOW`, 160-tick) item, max stack 1, that on
  finish teleports the player to their last-death location (using the vanilla respawn-anchor-style
  `TeleportTarget`/portal-particle path) and **consumes the stored death position**
  (`setLastDeathPos(Optional.empty())`) — one-shot, unlike vanilla's Recovery Compass which just
  points at it indefinitely. Getting attacked partway through channeling cancels and applies a
  240-tick cooldown; every 40 ticks of channeling briefly blinds/nauseates/withers the user (a
  "reaching into the void" flavor cost) and, past that mark, if the last-death dimension check has
  since become invalid, applies a shorter 60-tick cooldown and effectively no-ops. On success,
  nearby (8-block) Endermen aggro onto the player — a small risk/reward tax on using it. Damages
  itself by 1 durability per tick while channeling (`maxDamage = USE_TIME_TICKS*2` = 320).
- **`DecayableArmorItem`** (copper armor's class): every 1200 ticks (1 real-world minute), once
  damage ≥ a per-piece threshold (`MIN_DAMAGE_TO_DECAY`: 24/36/28/24 for helmet/chest/legs/boots —
  see `CustomEquipment`), a damage-weighted random roll can swap the equipped piece for its
  "exposed" copper variant (`DURABILITY_CAP = maxDamage - minDamageToDecay`; higher current damage
  → higher decay chance) — copper armor visually/functionally oxidizes with wear, mirroring copper
  blocks' weathering, and **only while actually worn** (`inventoryTick` with a non-null slot).
  Exposed copper armor is a separate, weaker `ArmorMaterial` (see `CustomEquipment`'s defense maps:
  base copper 2/3/5/2/7 boots/legs/chest/helm/body defense vs. exposed 1/2/4/1/6) — there is
  currently **no code path shown here that turns exposed copper back into fresh copper** (no
  "un-oxidize" item interaction found for armor, unlike copper blocks' axe-scraping) — verify this
  is intentional (copper armor decay is one-way) before assuming a scrape-to-restore feature exists.
- **Copper tools/armor registry** (`CustomEquipment`): `ToolMaterial`s for regular (durability 128)
  and pre-waxed (196, `+0.5` mining speed) copper tiers, enchantability 1 (very low — matches gold's
  vanilla enchantability-lore of "soft metal, easy to enchant, doesn't last"), `INCORRECT_FOR_STONE_TOOL`
  mining-level tag (copper tools mine like wood/gold tier, not stone-tier). Every tool/armor piece
  has a `_waxed` twin registered separately (waxed pieces don't decay/oxidize further — mirrors
  vanilla copper block waxing) — **when adding a new copper item, add both the base and `_waxed`
  variant**, plus a `BurningConversion` entry (see above; copper pieces convert to `COPPER_INGOT` at
  60 or 20 (exposed/waxed) health-budget ticks) and an entry in `RegisteringCustomItems` for both.

## Extension recipe: adding a new custom item

1. Add the `Item` instance as a `public static final` in the relevant `mod/items_and_effects/**`
   holder class (`CustomEquipment` for weapons/armor/tools, `CustomBrewingItems` for
   ingredients/concentrates — see `brewing-and-potions.md`).
2. Register it in `RegisteringCustomItems.registerAllCustomItems()` via `registerCustomItem(name,
   item, itemGroup...)` — pick the closest matching `ItemGroups.*` constant(s); items with no
   natural creative-tab home (e.g. `corrupted_wart_powder`, replacing Fermented Spider Eye) are
   registered with `null` groups on purpose, matching their vanilla-replacement counterpart's own
   tab placement (handled by the replaced item's own group entry, not duplicated).
3. If it replaces a vanilla item, add the pair to `VanillaItems.ITEM_REPLACEMENT_MAP` instead of
   writing custom swap logic.
4. Add assets (`assets/players-versus/{items,models/item,textures/item}/<name>.*`) and a lang key.
5. If it can burn as an item entity, add a `BurningConversion` entry; if it's a tool/weapon, build
   its attribute component via `VanillaItems.createToolAttributeModifiers`/`createArmorAttributes`.
