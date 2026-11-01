# The pale, the warp and the red mushroom: lore fixes and the watchling

Asked for on 2026-11-01, after a review of the mod's lore against its code: fix where the code contradicted the lore
(but pale mobs don't turn back into regular ones), tie creakings, endermen and paleness together, and add the watchling.

## The rules a player can learn

1. **Warped things care about being seen** (the End). Endermen hate a stare; creakings, pale spiders and pale creepers
   move only unwatched; the watchling freezes while watched and kills those who watch it. Endermites and silverfish are
   the warp's bugs: silverfish come out of a hit creaking, and out of whoever looked at a watchling too long. A carved
   pumpkin over the eyes hides you from all of them that stare.
2. **Darkness withers.** Wither damage turns zombies and creepers pale (below 8 health); nether wart withers in the dark.
3. **Paleness holds below y 0 and in the pale biomes** (the pale line: the pale garden, the pale grotto and hollow, the
   deep caves). Brown and red mushrooms there grow into pale shrooms; pale zombies, pale creepers, pale spiders and
   watchlings spawn there in the dark at any hour, and above it (to y 64) only at midnight under a new moon.
4. **Pale things convert the living; the converted rise undead; the undead turn pale.** Conversion's white hearts, all
   white:
   - a player, an illager, a witch or a wandering trader rises as **the zombie of the place**: a drowned under water, a
     pale zombie where paleness holds, a husk in the desert, a zombie elsewhere (a player dies first; it rises in their
     armour);
   - a villager becomes a zombie villager, piglins zombified piglins, a hoglin a zoglin, a horse a zombie horse;
   - zombies of every kind become pale zombies, a creeper a pale creeper, spiders pale spiders;
   - anything else dies of it.
   - Never converted: skeletons (bone), the nether's undead, phantoms, the wither, undead horses; the pale themselves
     (pale zombies, creepers and spiders, creakings, watchlings); the red mushroom's hosts; endermen, endermites and
     silverfish; golems, the ender dragon, the warden.
5. **Undead convert their own kind.** A pale zombie's, a creaking's and a watchling's hits convert anything living, every
   hit. Other zombies' hits convert only people (players, villagers, wandering traders, illagers, witches; zombified
   piglins: piglins), 1 in 25, and then give nothing else.
6. **The red mushroom takes flesh, never bone, and makes its hosts red and calm.** The mushroom creeper and spider are
   red and white with red mushrooms growing out of them, as the mooshroom's; they leave players alone, the creeper's
   spore puff turns creepers and touches nothing else, and they can't be converted.

## The watchling

An enderman the paleness took, with a creaking's yellow eyes.
- **Spawning:** rare (weight 3-4, alone), in the dark where paleness holds: the pale garden, the deep caves, the pale
  grotto, regular caves below y 0.
- **Moving:** it hunts players. While any player looks at it, it freezes, as a pale spider does. Unwatched and more
  than 10 blocks off, it teleports up behind its prey, 6 to 10 blocks away where they aren't looking, every 4 to 8
  seconds. Close, it hits as an enderman, and its hits convert.
- **The stare:** a player looking at it fills their screen with grey static. Looking means in plain sight, within 48
  blocks, within about 25° of the middle of the view, with no carved pumpkin on. The static is full after 6 seconds of
  looking and clears in 3 seconds of looking away. Full, the player dies ("looked at the Watchling too long") and 4 to 7
  silverfish crawl out where they stood. A totem saves them. Creative and spectating players are spared.
- It takes water damage and dodges arrows as an enderman, and drops 0-1 ender pearls.

## How

- `PaleLine` (tag `players-versus:pale` for the biomes). `MobSpawning.palePlace` puts the pale mobs' spawn rules on it;
  `StagedMushrooms` uses it for pale shrooms (`MushroomGrowth.Conditions.pale`).
- `Undeath` (was `Shroomify`): `turn` (what all-white things become), `zombieOfThePlace` (tag
  `players-versus:undead_rise_as_husks` for the desert), tag `players-versus:rise_as_zombies`. Damage type
  `players-versus:turned_undead` (was `shroomified`); `UndeathShakingPayload` (was `ShroomShakingPayload`).
- `ConversionAttacks`: tag `players-versus:zombie_kin`; the watchling among the always-converting.
- Tag `players-versus:conversion_immune` lists the never-converted (no longer all of `#minecraft:undead`).
- `MushroomCreeperEntity#puff` doses nothing. `RedMushroomsLayer` (client) draws the red mushroom's block model on a
  model part, resolved each frame by `MushroomCreeperRenderer` and `MushroomSpiderRenderer`; their skins come from
  `tools/mushroom-mobs.py`, now from the red mushroom block.
- `WatchlingEntity` (extends `Enderman`, its goals replaced), `WatchlingStare` (server: who looks, the static, the death;
  `WatchedPayload`), client `WatchlingRenderer` (eyes layer swapped, as the pale spider's) and `WatchlingStatic` (a HUD
  element after the misc overlays: a grey wash and snow from `textures/gui/watchling_static.png`, a random part of it
  every frame). `VersusSettings.Watchlings`. Damage type `players-versus:watched`. Textures from `tools/watchling.py`.

## Rulings

- Pale mobs don't turn back into regular ones near the surface (the user's call).
- The undead form of the place goes by place only (depth, biome, water), not light: predictable at a glance.
- Zombies converted to pale zombies wherever they are (Conversion is paleness), not by place.
- Spiders and cave spiders turn into pale spiders, horses into zombie horses: the pale and undead forms that exist.
- Pale spiders now also spawn in caves below y 0, as pale zombies and creepers do.
- The watchling's look cone is about 25°, wider than an enderman's stare: it's what's on your screen, as Slender's.
- The static is the server's count, sent to the player, so death and screen agree; the client only eases it.
