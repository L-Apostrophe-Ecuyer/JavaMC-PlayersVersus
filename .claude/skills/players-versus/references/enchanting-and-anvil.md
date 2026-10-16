# Enchanting Table, Anvil & Custom Enchantments

## Design intent

Two vanilla frustrations get targeted directly: (1) the anvil's level cost scales unpredictably
and gets punishing on already-enchanted gear; (2) re-enchanting a used item means losing its
current enchantments. Here the anvil's cost curve is flattened/rebalanced (see `AnvilCostMixin`)
and the **enchanting table can accept an already-enchanted item and improve it** rather than only
accepting bare tools (README: "Enchanting table can take in already enchanted items and improve
their enchantments"). Ten new enchantments round out combat/utility niches vanilla leaves thin
(critical/sprint attack scaling, shield-parry counterattacks, frost weapons, armor sub-resistances,
knockback shovels, jump-boosting leggings, a curse that punishes killing the wearer).

## Custom enchantments (`CustomEnchants`, `data/players-versus/enchantment/*.json`)

| Id | Items | Max lvl | Implementation | Effect |
|---|---|---|---|---|
| `critical_strike` | `#players-versus:critical_weapons` | 5 | Data (`minecraft:damage` + `minecraft:attributes` on the mod's `critical_attack_damage` attribute) | +1/+0.5 per lvl bonus damage; feeds `Combat.CRITICAL_ATTACK_DAMAGE`, consumed in `PlayerEntityMixin.modifyAttackDamage` (see `combat-and-players.md`) |
| `sprinting_strike` | `#players-versus:sprint_weapons` | 5 | Data, same pattern on `sprint_attack_damage` attribute | +1/+1.0 per lvl; consumed the same way for sprint attacks |
| `tossing` | shovels | 2 | Data (`minecraft:knockback`) + Java (`CustomEnchants.performTossAttack`, called from `LivingEntityMixin.attackEnchantmentEffects` on a grounded, non-sneaking shovel hit) | +0.25/lvl knockback effect value, **plus** a separate Java-side vertical launch (`0.2 + 0.1*level` upward velocity) — two independent knockback mechanisms stack |
| `riposte` | swords, shield | 3 | **Pure Java** — no `effects` in JSON at all | Extends the shield-parry window: `LivingEntityBlockingMixin.getDamageBlockedAmount` computes `ticksToParry = PARRY_TIME_TICKS + level`; a hit landing within that window counts as parried (bypasses shield-piercing effects, blocks even sonic boom) |
| `cleaving` | axes | 1 | **Registered but has empty `minecraft:damage`/`minecraft:attributes`/`minecraft:post_attack` arrays in JSON, and zero Java references anywhere in `src/`** | **No effect whatsoever — dead enchantment.** Obtainable (weight 6, anvil cost 1, cost 12–32) but does nothing when applied. This is either an unfinished feature or should be removed/hidden; flag to the maintainer rather than silently "fixing" it, since the intended effect (presumably bonus axe damage or a cleave/AoE hit) isn't specified anywhere. |
| `frost_aspect` | axes (primary), sharp weapons (supported) | 2 | Data (`minecraft:hit_block`: extinguishes lit fire-aspect-lightable blocks, freezes adjacent water into frosted ice) + Java (`CustomEnchants.performFrostAttack`, called from `LivingEntityMixin.attackEnchantmentEffects`) | On-hit: extinguishes target, adds `180 + 60*level` frozen ticks (comment: "Minimum ticks to get damaged is 140 for most; ModEntities remove 2 frozen ticks/tick") |
| `ender_curse` | any equippable | 1 | Pure Java (`CustomEnchants.onCurseOfEnderUserDamaged`, called from `LivingEntityMixin.modifyInvincibilityFrames` when the *wearer* takes damage) | On taking damage while worn: 2.0 magic damage to the wearer, then up to 16 random-teleport attempts within a 16×16×16-ish volume (Enderman-style, with sound/particle) and 8 ticks of i-frames — a literal curse that punishes the wearer, not the attacker |
| `magic_protection` | armor | 4 | Data only (`minecraft:damage_protection` gated on damage-source tag `players-versus:is_protected_by_magic_enchant`) | +3/+2 per lvl flat reduction against tagged "magic" damage sources |
| `impact_protection` | armor | 4 | Data only | Layered: knockback-resistance attribute (+0.01/lvl), +2/+1 reduction vs mace-mainhand melee hits specifically, +1/+1 vs generically-tagged impact damage, +2/+1 extra vs head-impact damage **only if the helmet itself has this enchantment**, +2/+1 extra vs fall damage **only if the boots have it** — i.e. slot-specific bonus stacking is intentional, not per-armor-piece-additive by accident |
| `piercing_protection` | armor | 4 | Data only | +2/+1 vs tagged piercing damage sources; +1/+1 extra specifically vs direct melee hits from `#players-versus:piercing_weapons` |
| `bounding_strides` | leggings | 3 | Data (`minecraft:attributes` on vanilla `minecraft:step_height`) + Java (`SpecialMovementMixin.jump`, see `combat-and-players.md`) | Step-height +0.15/lvl (data) plus the sprint-jump distance/height/particle mechanic (Java, entirely separate from the step-height attribute) |

**Custom damage-source tags** these enchants key off (`data/players-versus/tags/damage_type/*.json`):
`is_head_impact`, `is_melee_attack`, `is_protected_by_impact_enchant`, `is_protected_by_magic_enchant`,
`is_protected_by_piercing_enchant`. **Custom item tags**: `critical_weapons`, `piercing_weapons`,
`sprint_weapons`, `sweep_weapons`, `copper_tool_materials`, and `enchantable/{knockback,riposte,
sweeping,thorns}_applicable`. **Custom exclusive sets**: `exclusive_set/knockback` (vanilla
Knockback + `tossing`), `exclusive_set/riposte` (vanilla Sweeping Edge + `riposte` — a sword can't
have both). **When adding a new "protection-flavor" enchant, tag the relevant damage sources first**
(a new `data/players-versus/damage_type/*.json` or a tag addition), then write the enchant JSON
against that tag — don't invent per-enchant one-off conditions when an existing protection tag fits.

## Vanilla enchantment mechanics changes

### `EnchantRegistryHelper` (`mod/enchantments/`)
Thin wrapper caching a `RegistryEntryLookup<Enchantment>` (lazily bound to whichever `World` calls
it first — **not per-world-instance safe if you juggle multiple `World`s with different registry
managers in the same JVM session**, though in normal single-server operation this never matters).
`getLevel`/`hasEnchantment`/`getEquipmentLevel` are the standard lookup path every mixin in this
codebase uses instead of calling `EnchantmentHelper` directly with a raw `RegistryKey` — **use
this helper for any new enchant-level check**, don't hand-roll the `RegistryEntryLookup` dance.
`getMostImportantEnchant`/`getValueOfEnchantment` rank enchantments by
`avgPower + anvilCost + exclusiveSetSize*8` — used by the enchanting-table slot-preview UI (see
below) to pick which of several candidate enchantments to *display*, not which to actually apply.

### `EnchantmentHelperMixin` (`@Overwrite EnchantmentHelper.getPossibleEntries`)
Reworks enchant-power scaling by item enchantability: enchantability 1 items get power divided by
3 (clamped 1–10); enchantability ≤4 gets `power - 4 + enchantability`; higher enchantability adds
`enchantability/3` on top of the table's power — a smoother curve than vanilla's, favoring
high-enchantability items less linearly. Books/fishing rods/tridents are exempt (use raw table
power). Otherwise mirrors vanilla's per-level min/max power window logic.

### `EnchantingTableMixin` (target `EnchantmentScreenHandler`)
- `onButtonClick` (`@Overwrite`): the core "improve existing enchantments" feature — vanilla only
  ever adds new enchantments; this iterates the *item's own current enchantments* first (raising
  a level if the newly-rolled level is higher; books can bump a matching-level enchant by +1 even
  without a higher roll) before adding any brand-new enchantment from the roll. Everything else
  (lapis cost, XP cost gating, seed reroll, stat/advancement triggers) matches vanilla structure.
- `updateUnavailableEnchantments` (`@Inject TAIL` on `onContentChanged`): recomputes the three
  slot previews after this rework, using `EnchantRegistryHelper.getMostImportantEnchant` to decide
  which enchant name/level to *show* per slot (vanilla shows whichever the RNG happened to pick).
- `generateEnchantments`: unchanged reroll mechanics, but books with >1 candidate now collapse to
  just the single most-important one (consistent with the "important enchant" ranking above).

### `AnvilCostMixin` (`@Overwrite AnvilScreenHandler.updateResult`, `priority = 999`)
The flattened cost model (README: "level cost has been flattened for enchanted tools and removed
for unenchanted tools"):
- **Repair cost formula changed**: `durabilityRecoveryChunk = maxDamage * 3 / (10 + levelRequiredToSmith/2)`
  — repairing already-enchanted (higher repair-cost) items recovers *less* durability per material,
  a deliberate anti-exploit curve (previously flat regardless of enchant load).
- **Enchant-merge cost** (`getLevelForApplying`): Mending costs a flat 12, Protection 6, any cursed
  enchant **contributes -12** (cheaper to combine curses), otherwise
  `max(levelRequired + minPower(level), anvilCost)`, clamped to `[3, 30]`.
- **Enchantability rebate**: `getEnchantabilityRebate` subtracts up to `enchantability/2` (capped at
  `levelRequired-1`) from the enchant-merge cost — high-enchantability tools (e.g. diamond, or
  future custom copper — see `items-and-equipment.md`) get a real, tangible cost discount, which is
  the "removed for unenchanted, flattened for enchanted" README claim's concrete mechanism.
- **Books only grant XP levels back** (`levelsOnlyUsedWhenUsingBooks`, `@Redirect` on
  `PlayerEntity.addExperienceLevels`) — combining two non-book items no longer refunds the player's
  own levels the way merging with a book does, matching vanilla's book-specific XP-back behavior
  but made an explicit, single-purpose redirect here rather than relying on incidental vanilla flow.

### `EnchantmentBottleMixin` (`@ModifyVariable` on `ExperienceBottleEntity.onCollision`)
XP bottles give **4×** vanilla XP per bottle — a deliberate multiplier, not a bug; consider this
before "fixing" bottle XP if it looks high compared to vanilla wiki numbers.

### `EnchantmentProtectionMixin` — **registered in neither mixin config, dormant**
`@ModifyConstant`s on `DamageUtil.getInflictedDamage` (20.0f→18.0f ceiling, 25.0f→24.0f
denominator) — would tweak vanilla Protection's damage-reduction curve slightly. Since it's
unregistered, **Protection currently behaves 100% vanilla**; if reviving this, register it in
`players-versus.mixins.json` under the `enchantments.*` block and verify against current vanilla
`DamageUtil` (these two constants may have moved across MC versions — check the decompiled source
before assuming they're still at 20.0f/25.0f).

## Client screens

- **`AnvilScreenMixin`** (`AnvilScreen`): removes the vanilla 40-level display cap (shows real
  costs up to 999), adds a large `showBookErrorMessage` rewrite covering every failure/success
  state (incompatible items, conflicting enchants, nothing-to-repair, book-combine prompt, level
  cost display), and swaps in cycling cosmetic slot-icon hints (`CyclingSlotIcon`) showing which
  tool/armor/repair-material types the current input accepts.
- **`EnchantingScreenMixin`** (`EnchantmentScreen`): `@Redirect`s the random glyph-phrase generator
  to instead cycle through and display the **real enchantment name** for whichever of the 3 slots
  has nonzero power — a UX change (real names instead of the vanilla galactic-alphabet flavor text)
  with defensive `MOD_LOGGER.error` calls if the handler's slot-power/level/id arrays are ever
  inconsistent (never observed in practice, but present).

## Extension recipe: adding a new custom enchantment

1. Add a `RegistryKey<Enchantment>` constant to `CustomEnchants` via the `of(String)` helper.
2. Write `data/players-versus/enchantment/<id>.json` (copy the closest existing sibling above for
   `supported_items`/`primary_items`/cost-curve shape). If the effect needs anything beyond what
   vanilla's data-driven enchantment-effect components (`minecraft:damage`, `minecraft:attributes`,
   `minecraft:damage_protection`, `minecraft:hit_block`, `minecraft:knockback`, `minecraft:post_attack`,
   etc.) express, leave `effects` minimal/absent and implement the rest in Java — **hook it from
   `LivingEntityMixin.attackEnchantmentEffects`** (attacker-side, on-hit) or
   `LivingEntityMixin.modifyInvincibilityFrames`/`onStatusEffectsRemoved` (victim-side) rather than
   adding a new mixin injection point, unless the timing genuinely doesn't fit those existing hooks.
3. If it needs a new damage-source or item tag, add it under
   `data/players-versus/tags/{damage_type,item}/**` and reference the tag, not a hardcoded list.
4. Add a lang key `enchantment.players-versus.<id>` to
   `assets/players-versus/lang/en_us.json` (and ideally the other 7 lang files this project ships).
5. **Do not repeat the Cleaving mistake** — if you register an enchant, either give it a working
   `effects` block or a genuine Java hook before merging; an anvil-costed, obtainable enchantment
   that silently does nothing is a real player-facing bug, not a placeholder.
