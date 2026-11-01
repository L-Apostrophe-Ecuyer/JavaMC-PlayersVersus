# Staged mushrooms, pale shrooms and Conversion

Asked for on 2026-10-31, after the recipe book. Builds on the feedback batch's mushrooms (cave clusters, mushroom caves).
Revised by `2026-11-01-pale-warp-lore-and-watchling-design.md` (the lore fixes): where this says "below y 0" read "where
paleness holds" (below y 0 or a pale biome), the mushroom creeper's puff gives no Conversion, and what all-white things
turn into is that spec's.

## What the player sees

- **Where mushrooms grow wild.** Mushroom patches on the surface grow only on the mushroom fields, brown and red there;
  elsewhere the surface has none (caves keep their brown clusters, fallen trees their brown mushrooms). Red mushrooms,
  small or huge, grow wild only on the mushroom fields. The mushroom fields have no green vegetation: no sugar cane, no
  firefly bushes.
- **Stages.** A small brown or red mushroom grows into two (one slightly taller, light 2), then three (light 4):
  - It grows a stage, by chance, on a random tick where it's dark (light 5 or less) or on mycelium, on stone (the
    overworld's stones, deepslate and tuff among them), logs, mushroom blocks or mycelium, above y 0, beside another
    of its kind (any of the 26 blocks round it). To three, one beside it must be two or three already.
  - Two or three that tick where they couldn't grow shrink a stage. Three that tick where they could grow, by chance,
    spread a mushroom of their kind into an empty spot beside them, unless 8 of their kind are within 4 blocks.
  - Breaking them drops as many mushrooms; picking them picks a mushroom.
- **Pale shrooms.** Below y 0, a brown or red mushroom that would grow becomes two pale shrooms (grey) instead; pale
  shrooms grow by the same rules, below y 0 only, and shrink above it. Their item places one.
- **Conversion.** Staying in pale shrooms (5 seconds at a time) gives a dose of Conversion, a 2-minute effect; each dose
  refreshes it and adds its amplifier + 1 to the amplifier had. Its hearts turn white, amplifier + 1 of them, from the
  end of the health bar down. Food doesn't regenerate health into white hearts (other healing does). It wears off, milk
  cures it, a totem of undying cures it; then the hearts are red again.
- **What else converts** (asked for after the first round): every dose is one white heart more.
  - A pale zombie's hit, every one, armed or not, instead of its wither. A creaking's hit; and a creaking that
    vanishes when hit gives its attacker Conversion instead of blindness (the silverfish it otherwise lets out stays).
  - Any other zombie's hit (husks, drowned, zombie villagers, zombified piglins), 1 in 25, and then nothing else:
    a husk's converting hit gives no hunger.
  - The mushroom creeper never explodes, lit or after a player: its spore puff turns the creepers within 3 blocks.
    (It also gave everything else living there a dose until the lore fixes: a red mushroom host converts nothing.) The pale creeper still explodes, and its lingering cloud gives
    Conversion instead of darkness and wither.
  - Rotten flesh: besides its hunger, a 1-in-8 chance of a dose lasting 60 seconds.
  - A dose refreshes the effect's time but never shortens it (rotten flesh's minute on two minutes left leaves two).
- **Turning undead** (called shroomifying until the lore fixes). All of its hearts white, a living thing shakes for 5
  seconds (a player feels sick), then (as revised):
  - a player dies of it ("turned undead") and the zombie of the place rises where they fell, wearing their armour, which it
    always drops; a totem saves them, armour and all;
  - a villager turns into a zombie villager, piglins and piglin brutes into zombified piglins, a hoglin into a zoglin,
    illagers, witches and wandering traders into the zombie of the place; zombies turn pale, creepers into pale
    creepers, spiders into pale spiders, a horse into a zombie horse;
  - anything else dies of it.
  - The undead, mooshrooms, the mushroom creeper and spider, pale zombies, pale creepers, creakings, golems, the ender
    dragon and the warden never take Conversion.

## How

- `MushroomGrowth` (pure, tested): what a mushroom of a kind and stage does on a random tick, from what's around it.
  `StagedMushrooms`: what's around one in the world, and doing it. `StagedMushroomBlock` (`brown_mushrooms`,
  `red_mushrooms`, `pale_shroom`, property `mushrooms` 1-3): brown and red ones at 1 are vanilla's mushroom (their
  stage-1 state isn't used and looks like it). `MushroomBlockMixin` hands vanilla mushrooms' random tick to
  `StagedMushrooms` (`VersusSettings.Mushrooms.DO_STAGED_MUSHROOMS`). Tag `players-versus:mushrooms_grow_on`.
- Worldgen: the placed features `brown_mushroom_{normal,taiga,old_growth,dappled_forest,swamp}` are count 0 (the red
  ones already were); `mushroom_fields`' biome file is vanilla's with its own patches
  (`players-versus:mushroom_fields_{brown,red}_mushrooms`) instead of the taiga ones, sugar cane and firefly bushes.
- `ConversionStatusEffect`; `LivingEntityConversionMixin` stacks doses (`addEffect`) and keeps the immune
  (`players-versus:conversion_immune`) from taking it (`canBeAffected`). `Conversion` (pure, tested): white hearts,
  stacking, the regeneration cap, which hearts are white. `ConversionExposure`: the time spent in pale shrooms.
- `HungerManagerMixin`: the food overhaul's regeneration stops at the cap, and so does vanilla's (`FoodData.tick`'s
  `heal`). `ConversionHeartsMixin` (client) draws white hearts over the bar's last ones (sprites
  `players-versus:hud/heart/conversion_*`), counting the containers vanilla draws to know each heart.
- `Undeath` (was `Shroomify`): the effect's tick notes an all-white entity; at the end of the level's tick it shakes (an
  `UndeathShakingPayload` to the clients watching, which make its render state "fully frozen"), then turns undead.
  Damage type `players-versus:turned_undead` (no armour, shields, resistance or knockback; a totem still works).
  `players-versus:rise_as_zombies` lists the mobs that rise as the zombie of the place.
- Assets from `tools/staged-mushrooms.py` (tall mushrooms, pale shrooms, white hearts, the cluster models).

- `ConversionAttacks`: the dose, and what a mob's landed melee hit does (`MobConversionAttackMixin` at
  `Mob#doHurtTarget`'s return); `LivingEntityConversionMixin` drops a converting hit's other effects on its target
  that tick. `CreakingScares`, `MushroomCreeperEntity#puff` (with `CreeperMixin` putting out a lit one),
  `PaleCreeperEntity`'s cloud and rotten flesh's consumable (`VanillaItems`) give the other doses.

## Rulings

- "Stone" is the overworld's stones (`#minecraft:base_stone_overworld`), deepslate among them: below y 0 is mostly
  deepslate, where pale shrooms grow.
- Light is the block's raw light, sky light at its brightest: surface mushrooms in daylight's reach don't grow, by
  night or day, so they don't grow at night and shrink by day.
- Growth, turning pale and spreading happen on a random tick by a 1-in-4 chance; shrinking always.
- Below y 0, pale shrooms beside a brown or red mushroom count as its kind, so a cluster turning pale doesn't strand
  its last ones.
- Stage 1 doesn't spread as vanilla's mushrooms do: only three spread.
- A player's armour moves to their pale zombie (never copied, whatever keepInventory says).
- "So goes the Absorption effect" read as a comparison: Absorption is unchanged.
- "Spore creepers" are both the mushroom creeper and the pale creeper (the user's answer).
- A pale zombie's every hit converts, armed or not (its wither came only from bare hands); the creaking's silverfish
  stays: it isn't something inflicted.
- "Very rarely" for other zombies: 1 in 25 hits (`ZOMBIE_CONVERSION_CHANCE`).
- A lit mushroom creeper puffs and goes out, rather than puffing again every second and a half.
