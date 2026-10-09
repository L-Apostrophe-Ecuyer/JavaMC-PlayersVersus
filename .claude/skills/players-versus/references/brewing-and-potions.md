# Brewing, Potions, Concentrates & Biles

## Design intent
README: brewing needs only nether wart (much more common than blaze powder/etc.), nether wart
needs heat to grow, potions stack to 8, any potion can be "corrupted" with a fermented spider eye,
and lingering potions/placeable-edible concentrates are much easier to make. The whole subsystem is
built around **concentrates** — new craftable ingredient items, one per effect — that both brew
potions directly (`water + concentrate → potion`) and can be eaten/thrown-on-entities directly for
a punchier, riskier, no-brewing-required version of the same effect.

## Brewing graph (`BrewingSystem`, brewed in code by `RecipeManagerMixin`)

On 26.3 brewing is recipes (`minecraft:brewing`: a `PotionIngredient` input and reagent, an `ItemStackTemplate`
output) in the reloadable recipe registry; `PotionBrewing` is gone. The graph still lives in code:
`RecipeManagerMixin` (`@ModifyArg` on the `RecipeMap.create` call in `RecipeManager`'s constructor) hands the recipe
map `BrewingSystem.replaceVanillaBrewing(recipes)`: every data pack recipe except `minecraft`-namespace brewing ones,
plus the graph's recipes (`players-versus:brewing/<container>_<potion>_<reagent>`, ~1157 of them). Everything that asks
the recipe manager follows, including the `BREWING_INPUTS`/`BREWING_REAGENTS` property sets that the stand's slots and
hoppers check, and Fabric's own `RecipeMap.create` hook still runs. The recipe registry itself is untouched. Each mix
becomes one recipe per container; each container recipe one per potion in the registry (a brewing recipe's output is a
fixed potion). The first mix for a container, potion and reagent wins, as 1.21.x's registry did; a server log line gives
the counts. `describeRecipes` hands out reagents through `VanillaItems.getReplacementItem`, so blaze powder, magma cream
and the fermented spider eye brew as Concentrate of Strength, Concentrate of Fire and Corrupted Wart Powder (the
vanilla items are disabled and swapped on every stack).

- Registers ~30 `registerConcentrateRecipe(ingredient, potion)` calls, each of which:
  1. `water + ingredient → base potion`.
  2. `base potion + Concentrate of Decay → Decay potion` (every potion has a corrupted/decay path).
  3. If the potion has a "long" variant in the internal `brewablePotionTypes` map: `base + sugar →
     long`, **and** `thick + ingredient → long` (a shortcut skipping the base potion entirely),
     plus `long + Concentrate of Decay → Decay (long)`.
  4. If it has a "strong" variant: `base + glowstone dust → strong` and `awkward + ingredient → strong`
     (note the pairing: thick, made with glowstone, shortcuts to *long*; awkward, made with sugar, to
     *strong*), and a strong decay variant.
  5. If it has an **inverted** potion (its "opposite", e.g. Healing↔Harming, Haste↔Mining Fatigue,
     Leaping↔Slow Falling, Luck↔Unluck, Regeneration↔Decay): `base + fermented spider eye → inverted`,
     with the long/strong variants inverted the same way if both sides define them — **this is the
     "corrupt any potion with a fermented spider eye" README claim**, generalized data-driven via the
     `RelatedPotions` record rather than one-off vanilla-style overrides.
- The graph names `Items.BLAZE_POWDER` (Strength), `Items.MAGMA_CREAM` (Fire Resistance) and
  `Items.FERMENTED_SPIDER_EYE` (every inversion), but those vanilla items are replaced
  (`VanillaItems.ITEM_REPLACEMENT_MAP`: Concentrate of Strength, Concentrate of Fire, Corrupted Wart Powder) and
  disabled. `describeRecipes` resolves them to their replacements, so the recipes use the items players actually have.
  Recipes naming the vanilla items could never brew (the 26.3 JSON recipes did, and lost all three).
- Vanilla-untouched: Thick/Awkward from glowstone/sugar, splash/lingering conversion via
  gunpowder/redstone (unchanged from vanilla).
- **New potion effects not in vanilla combos**: Haste/Mining Fatigue/Vulnerability/Darkness/
  Buoyancy/Largeness/Smallness/Glowing/Decay all get full base/long/strong ladders where vanilla
  either doesn't have the potion at all (Haste, Mining Fatigue, Vulnerability, Darkness as a potion,
  Buoyancy, Largeness, Smallness, Glowing, Decay) or only has some tiers.

## `CustomPotions` — registered potions
Registers ~35 `Potion`s (durations/amplifiers explicit per call — e.g. Largeness base 2400 ticks
amp 0, long 4200 amp 0, strong 1200 amp 1) wrapping either a brand-new `CustomStatusEffects` entry
(Largeness, Smallness, Vulnerability, Buoyancy, Haunting) or an **overhauled vanilla** status effect
registered under the vanilla namespace (see `CustomStatusEffects.registerOverhauledVanillaEffect` —
Haste/Mining Fatigue/Darkness/Glowing/Decay(=Wither)/Unluck/Levitation potions all reuse vanilla
`StatusEffects.*` constants, just packaged into new `Potion`s vanilla never defined). Two potions
(`DARKNESS_STRONG`, `GLOWING_STRONG`) are hand-built multi-effect `Potion`s (Darkness+Blindness,
Glowing+Night Vision) rather than the single-effect helper — a nice small "strong tier gives a bonus
secondary effect" pattern worth reusing for future strong-tier potions that want to feel distinct
rather than just longer/bigger-amplifier.

## Custom status effects (`CustomStatusEffects` + `mod/items_and_effects/brewing/effects/*`)
Five real custom `StatusEffect` subclasses: `VulnerabilityStatusEffect`, `LargenessStatusEffect`,
`SmallnessStatusEffect`, `BuoyancyStatusEffect`, `HauntingStatusEffect`. `BuoyancyStatusEffect`'s
actual per-tick physics live in `LivingEntityMixin.applyBuoyancyEffect` (see `combat-and-players.md`
if that doc covers it, otherwise: swims/sneaks dampen the push, base upward velocity scales with
amplifier). `HauntingStatusEffect.removeEffect` is invoked from `LivingEntityMixin.onStatusEffectsRemoved`
when it wears off — check that class directly for its on-expire behavior before assuming Haunting is
purely cosmetic; also check `FireResistanceEffect.java` (present in `brewing/effects/` despite not
being a `CustomStatusEffects` registration — likely helper logic for the *vanilla* Fire Resistance
overhaul rather than a new effect; verify its role before assuming it's dead code).

## Concentrates (`CustomBrewingItems`, `ConcentrateItem`)
~30 `ConcentrateItem`s (`concentrate_of_<effect>`), each a placeable `BlockItem` (see
`blocks-and-environment.md` for the block side — biles/concentrate blocks are the placed form) that
is **also directly edible** with real risk attached:
- `finishUsing` (eating it): 70% chance to actually grant the wrapped effect (plus a short Poison
  I as a "this wasn't food" tax); 30% chance of just Poison II with no benefit. Every attempt costs
  2 seconds of eating with a Hunger+Nausea side-consume-effect (`CONCENTRATE_COMPONENT`) and a
  nausea pulse every 8 ticks of the 60-tick `finishUsing` window — deliberately unpleasant/risky
  compared to a clean potion, matching "concentrate = a rough, direct form" flavor.
- `useOnEntity` (right-click an entity with it): same 70% roll, but on success applies the effect at
  **2/3 duration, amplifier 0** to the *target* entity (not the user) — a way to buff an ally or
  (for harmful effects) debuff a hostile mob without brewing a splash potion, at the cost of item
  consumption and a cooldown (`4 + 2/3 duration` ticks). Spawns 20 tinted `ENTITY_EFFECT` particles
  colored to the wrapped effect's own color.
  - **Bug:** the instant-effect branch checks `if (item.effect == StatusEffects.INSTANT_DAMAGE) ...
    else if (item.effect == StatusEffects.INSTANT_DAMAGE) entity.heal(1.0F);` — **both branches test
    the same constant**, so the "heal" branch is unreachable dead code. A Concentrate of Health
    (wrapping `INSTANT_HEALTH`, if that's what it wraps — verify via `CustomBrewingItems.CONCENTRATE_OF_HEALTH`'s
    registration) used on an entity currently **does nothing on success** (no damage, no heal, no
    status effect since instant effects skip the `addStatusEffect` branch entirely) — only the
    particles/sound/cooldown/item-consumption fire. Compare against `PotionEffectBileBlock.grantStatusEffect`
    (same file, correct: `if (effect == INSTANT_DAMAGE) ... else if (effect == INSTANT_HEALTH) ...`)
    for the intended pattern. **Fix:** change the second condition in `ConcentrateItem.useOnEntity`
    to `StatusEffects.INSTANT_HEALTH`.

## Biles (`PotionEffectBileBlock`, `assets/players-versus/{blockstates,models/block,textures/block}/bile/**`)
The placed, walkable form of a concentrate — a thin (0-height), no-collision hazard/utility puddle:
- Falling onto one with `fallDistance > 1.0` (players always; other mobs only if `DO_MOB_GRIEFING`
  and big enough) triggers the block's "extra strong" effect grant and **destroys the bile**
  (single-use, like a landmine); otherwise just standing/moving through it re-applies the normal
  (non-extra-strong) effect every 10 ticks or on any fall movement.
- Instant-damage/instant-health effects apply directly as damage/heal (1 or 2 depending on
  extra-strong); duration-based effects extend an existing application by `15 + currentDuration`
  ticks (capped at `MAX_DURATION`, itself capped at 210 ticks in the constructor) rather than
  stacking independently — walking back and forth across the same bile refreshes it, doesn't stack
  amplifiers.
- Entities that `bypassesSteppingEffects()` (boots with the right enchant, etc.) are immune.
- Mining a bile drops nothing (`afterBreak` only increments the mined-stat) — bile blocks are
  meant to be consumed by walking into them or simply broken away, not harvested for the concentrate
  item back.

## Brewing stand & nether wart

- **Fuel** is the 26.3 `BREWING_FUEL` item component (uses + speed multiplier): `VanillaItems` gives it to
  Nether Wart (`VersusSettings.Items.BREWS_PER_NETHER_WART` brews each, speed 1) and removes it from blaze powder.
  Vanilla's stand, fuel slot, quick-move, hoppers and fuel bar (`fuel`/`totalFuel`) then handle nether wart
  themselves. This replaced 1.21.x's brewing-stand and menu mixins: a `tick` HEAD inject that refilled 21 "charges"
  from nether wart and drained one a second (on 26.3 it never set `totalFuel`, which the fuel bar divides by), a
  custom fuel slot, a quick-move redirect, and a `canPlaceItem` override. Those mixins also let Withered and
  Corrupted Wart into the fuel slot, but only Nether Wart ever burned; neither is fuel now.
- **Slots**: vanilla's own rules, driven by the recipes above: the reagent slot takes `BREWING_REAGENTS`, the
  potion slots the `brewing_potion_inputs` tag (potions and glass bottles) or `BREWING_INPUTS`.
- **`NetherWartMixin`** (target `NetherWartBlock`, covers Nether Wart's growth stage state machine):
  outside ultrawarm dimensions, wart below light level 10 turns into **Withered Wart** (a stalled,
  presumably lower-value state — see `blocks-and-environment.md` for the block itself); a mature
  vanilla Nether Wart block has a 50% chance per random tick to instead become **Corrupted Wart**
  and cancels its own further random ticking that tick — this is the "nether wart needs heat" README
  mechanic: darkness withers it, but there's also a *chance-based* mutation path into Corrupted Wart
  that doesn't obviously depend on heat/light in this snippet — cross-check
  `mixin/environment/blocks/BlockMixin`/light-related worldgen doc entries if "needs heat to grow"
  turns out to be implemented via ambient light/biome temperature checks elsewhere, since this
  mixin alone shows *darkness withering* and a *separate* random corruption chance, not a
  heat-accelerates-growth mechanic — don't assume this file is the whole "needs heat" story.
- **`VanillaStatusEffectsMixin`** / **`BadOmenEffectInstanceMixin`**
  exist alongside these (not read in this pass — see file paths) for
  vanilla status-effect tweaks, and Bad Omen/raid interaction (`VersusSettings.Gameplay.DO_RAIDS_OUTSIDE_VILLAGES`
  gate lives in `BadOmenEffectInstanceMixin`, per `architecture.md`'s settings grep) — read those
  directly before changing raid or vanilla-status-effect behavior; not re-verified in this pass.

## Potion bottle / splash / lingering interactions

- **`PotionBottleUsageMixin`** (target `PotionItem`, `useOnBlock` override): right-clicking a water
  bottle on a mud-convertible block or on Clay turns it into Brown Mud / vanilla Mud respectively —
  ties the potion-bottle item into the mud/clay moisture system (see `blocks-and-environment.md`)
  as an explicit "wet this block" tool, on top of whatever passive wetting mechanics that system has.
- **Throwing mixins** (`SplashPotionMixin`, `LingeringPotionMixin`, `ThrowablePotionItemMixin`,
  `FireballEntityMixin`, `SnowballMixin`/`SnowballEntityMixin`) exist under
  `mixin/items_and_effects/throwing/` — not re-read in this pass; check them directly if changing
  splash/lingering potion behavior specifically (radius, dilution, lingering-cloud duration).

## Extension recipe: adding a new brewable effect

1. If it needs a new status effect, add a class under `brewing/effects/` and register it in
   `CustomStatusEffects` (or reuse a vanilla `StatusEffects.*` constant directly).
2. Register base/long/strong `Potion`s in `CustomPotions.registerCustomPotions()`.
3. Add a `ConcentrateItem` instance + registration in `CustomBrewingItems`/`RegisteringCustomItems`
   (see `items-and-equipment.md`'s item-registration recipe).
4. Add the ingredient→potion + optional long/strong/inverted wiring via one
   `registerConcentrateRecipe` call (and an entry in `BrewingSystem`'s `brewablePotionTypes` map if
   it has an inverted counterpart) — don't hand-write the sugar/glowstone/spider-eye recipe
   permutations yourself, the helper generates them.
5. Add the bile block + assets if it should be placeable (`PotionEffectBileBlock`, matching
   `blockstates`/`models`/`textures` under `bile/`, and a recipe if it's craftable as a block).
6. Add lang keys for the potion name (`item.minecraft.potion.effect.players-versus.<name>` or
   similar — check an existing entry's exact key shape in `assets/players-versus/lang/en_us.json`
   before guessing the format) and the concentrate item.
