# The pale, the warp and the red mushroom: lore fixes and the watchling

Asked for on 2026-11-01, after a review of the mod's lore against its code: fix where the code contradicted the lore
(but pale mobs don't turn back into regular ones), tie creakings, endermen and paleness together, and add the watchling.

## The rules a player can learn

1. **Warped things care about being seen** (the End), and **show only to a stare**: endermen, endermites,
   watchlings and phantoms are always see-through, a faint shimmer at the edge of sight that fills in smoothly as the
   middle of the view turns onto them, whole when stared at; their glowing eyes fade with them, and what an enderman
   carries shows once it's mostly seen. Endermen hate a stare; creakings, pale spiders and pale
   creepers move only unwatched; the watchling never moves, and kills those who watch it. Endermites and silverfish are
   the warp's bugs: silverfish come out of a hit creaking, and out of whoever looked at a watchling too long. A carved
   pumpkin over the eyes hides you from all of them that stare.
2. **Blight is the Wither, and a blighted wound makes pale.** The Wither effect is gone: every Wither given (the
   wither skeleton's and the Wither's hits, Decay potions, withering bile, the commands) is Blight instead, at the same
   strength (a dose of at most four white hearts). Blighted and wounded to below 8 health, zombies turn into pale
   zombies, creepers into pale creepers, spiders into pale spiders, villagers into pale villagers (rule 5), illagers,
   witches and wandering traders into pale zombies. Nether wart still withers in the dark.
3. **Paleness holds below y 0 and in the pale biomes** (the pale line: the pale garden, the pale grotto and hollow, the
   deep caves). Brown and red mushrooms there grow into pale shrooms; pale zombies, pale creepers, pale spiders and
   watchlings spawn there in the dark at any hour, and above it (to y 64) only at midnight under a new moon.
4. **Pale things blight the living; the blighted rise undead (zombies); the undead turn pale.** Blight's white
   hearts, all white for five seconds (the world shaking, a player's view too):
   - a player dies of it and rises as a **player zombie** (rule 10);
   - an illager, a witch or a wandering trader rises as **the zombie of the place**: a drowned under water, a
     husk in the desert, a zombie elsewhere. Pale zombies rise of a blighted wound (rule 2), not of turning;
   - a villager becomes a zombie villager, anywhere;
   - piglins become zombified piglins, a hoglin a zoglin;
   - **an animal with an undead kind of its own turns into it, and stays as calm as it was and its owner's**: a horse
     a zombie horse, a camel a camel husk, a nautilus a zombie nautilus, a chicken a zombie chicken;
   - zombies of every kind become pale zombies, a creeper a pale creeper, spiders pale spiders;
   - anything else dies of it (donkeys, mules, pigs, cows, wolves...: no undead kind of their own).
   - Never converted: skeletons (bone), the nether's undead, phantoms, the wither, the passive undead (undead horses,
     camel husks, zombie nautiluses, zombie chickens: kept beside living ones, they stay as they are); the pale
     themselves (pale zombies, creepers and spiders, creakings, watchlings); the red mushroom's hosts; endermen,
     endermites and silverfish; golems, the ender dragon, the warden.
5. **Pale villagers are villagers the paleness took**, half villager, half illager. A villager blighted and wounded (rule 2), or
   killed by a pale villager (on normal half the time, on hard always, as a zombie's kill), turns into a pale villager:
   a villager's body in its own clothes (its biome's, its trade's, its level's badge), with an illager's ashen skin and
   scowling face. It keeps its memories for when it's cured: its type, trade and level, its experience, its trades and
   what it thought of everyone. A wandering trader blighted and wounded, or killed so, turns into a pale wandering trader, in its
   blue robe, keeping the trades it carried; cured, it's a wandering trader again. It shies from players and hunts villagers; its hits blight them, so the villagers
   it hunts turn pale in turn, and villagers flee it as they flee zombie villagers. It doesn't burn, drown or freeze
   into anything, and stays where it turned. **Golden apples cure**: fed one, a pale villager (or a zombie villager)
   starts its cure, 1 to 3 minutes, and more apples hasten it; eaten, a golden apple cures Blight.
6. **Undead blight their own kind.** A pale zombie's, a pale villager's, a player zombie's and a creaking's hits blight anything living, every hit (a
   watchling's would, but it never strikes). Other zombies' hits convert only people (players, villagers, wandering traders, illagers, witches; zombified
   piglins: piglins), 1 in 25, and then give nothing else.
7. **The red mushroom takes flesh, never bone, and makes its hosts red and calm.** The mushroom creeper and spider are
   red and white with red mushrooms growing out of them, as the mooshroom's; they leave players alone, the creeper's
   spore puff turns creepers and touches nothing else, and they can't be converted.
8. **The undead are undead everywhere.** The pale zombie is a zombie (vanilla's `#zombies`), the pale creeper and
   spider undead (`#undead`): smite bites them, healing harms them, poison and regeneration don't touch them, the
   Blight spares them. Undead animals burn in daylight as vanilla's zombie horse does, except the desert's camel husk
   and any in body armour (horse armour, nautilus armour).
9. **The pale take colour from sight.** Blighted, the world greys for its sufferer, as much as their hearts are
   white, easing in and out smoothly, fading as Blight wears off; under a watchling's stare it breaks into ever
   bigger, drabber pixels beneath the static, and the static is heard, a hiss, crackle and low drone that swells and
   rises with it.

10. **A player the Blight takes rises as a player zombie.** Any death while blighted, of a hit, a fall or the Blight
   itself, leaves not a corpse's drops but a zombie in the player's skin (drawn greener and duller) and name, wearing
   the armour they wore, holding what they held and carrying the rest of what they had, which it drops, all of it,
   when it's killed (their armour and hands guaranteed). It blights what it hits, never turns into anything else, is
   neither sunburnt nor drowned, and stays where it rose. A totem of undying saves a player and cures them. With
   `keepInventory` the player keeps their things and the zombie wears copies and drops nothing. A creative player can't
   die of it: they live, the Blight lifts, and a copy of them rises beside them to be seen.

## Undead forms (second round, 2026-11-01)

Asked for: "zombie chickens, converted villagers, pale villagers = illagers? Make the internal logic sound and
consistent throughout all relevant entities and systems. Exceptions: avoid passive zombie mobs becoming hostile
(zombie horses, nautilus) that way they can be kept alongside healthy variants."

- **The zombie chicken**: a chicken Blight turned. Calm as a chicken, it follows seeds, but lays no eggs, eats
  nothing, never breeds and is never a chick; always the temperate kind, its feathers the zombie's green, its voice
  lower. Undead, burns in daylight, never converted. Drops 0-2 feathers and 0-1 rotten flesh. Baby zombies' chicken
  jockeys ride zombie chickens (a new one, or one of those about; never a living chicken).
- **Undead animals keep what they had**: tame, their owner, their saddle and armour, their leash and name; a
  chicken jockey's mount stays one.
- **Pale villagers**: replaced (third round, below) by the pale villager mob.

## The pale villager, withering, curing and sight (third round, 2026-11-01)

Asked for: undead animals in armour don't burn; withered villagers (those that became illagers) become their own mob,
keeping their trades, white skin and clothes, curable like zombie villagers, fleeing players and attacking villagers;
curing by golden apple; players converted become zombies, withered become pale zombies; grown brown mushrooms the
source of mining fatigue concentrates; a grey screen for Blight and growing static or pixelation for a watchling's
stare; the watchling as a haunting, never-moving horror that teleports when looked away from and leaves after 15-30 s
unseen; endermen, endermites and watchlings see-through by how much the crosshair is on them.

- Rules 1, 2, 4, 5, 8 and 9 above.
- **Brown mushrooms and mining fatigue**: three brown mushrooms grown together (stage 3) break into a brown mushroom
  cluster, which smelts into a concentrate of mining fatigue (as prismarine still does) or crafts back into three brown
  mushrooms. Smaller clusters still drop their mushrooms.

## The watchling

An enderman the paleness took, with a creaking's yellow eyes.
- **Spawning:** rare (weight 3-4, alone), in the dark where paleness holds: the pale garden, the deep caves, the pale
  grotto, regular caves below y 0.
- **Haunting:** it never moves and never strikes; it stands and watches whoever looked at it last. Looked at and then
  left, half a second to a second later it's gone: half the time 4 to 8 blocks behind its watcher, where they aren't
  looking, half the time 18 to 32 blocks off. Seen, then left unlooked at for 15 to 30 seconds, it vanishes for good,
  in a wisp of portal particles. Hit, it slips behind whoever hit it.
- **Seeing it:** it shows only to a stare (rule 1): fully within 1° of the middle of the view (from its edge), not at
  all 10° aside.
- **The stare:** a player looking at it fills their screen with grey static, and the world under it breaks into big
  drab pixels. Looking means in plain sight, within 48 blocks, at least half seen (about 5° from its edge), with no
  carved pumpkin on. The static is full after 6 seconds of
  looking and clears in 3 seconds of looking away. Full, the player dies ("looked at the Watchling too long") and 4 to 7
  silverfish crawl out where they stood. A totem saves them. Creative and spectating players are spared.
- It takes water damage and dodges arrows as an enderman, and drops 0-1 ender pearls.

## How

- `PaleLine` (tag `players-versus:pale` for the biomes). `MobSpawning.palePlace` puts the pale mobs' spawn rules on it;
  `StagedMushrooms` uses it for pale shrooms (`MushroomGrowth.Conditions.pale`).
- `Undeath` (was `Shroomify`): `turn` (what all-white things become), `zombieOfThePlace` (tag
  `players-versus:undead_rise_as_husks` for the desert), tag `players-versus:rise_as_zombies`. Damage type
  `players-versus:turned_undead` (was `shroomified`); `UndeathShakingPayload` (was `ShroomShakingPayload`).
- `BlightAttacks`: tag `players-versus:zombie_kin`; the watchling among the always-converting.
- Tag `players-versus:blight_immune` lists the never-converted (no longer all of `#minecraft:undead`).
- `Undeath#turnIllager` and `PaleVillagers` (pure, `PaleVillagersTest`); `ZombieInfectionMixin` puts both on
  `Zombie#convertVillagerToZombieVillager`'s head (Blight's villagers go through it on a stand-in zombie) and gives
  `Zombie#finalizeSpawn` zombie chickens. `Undeath#keepWhatWasKept` (tame and owner: `AbstractHorseAccessor`,
  `TamableAnimal#setOwnerReference`). `ZombieChickenEntity` / client `ZombieChickenRenderer`, textures from
  `tools/undead-animals.py`. Vanilla tags extended: `#zombies` (pale zombie, zombie chicken), `#undead` (pale creeper,
  pale spider), `#burn_in_daylight` (zombie chicken), `#arthropod` (mushroom spider). Game tests `UndeadFormsGameTest`.
- Third round: `PaleVillagerEntity` (extends `ZombieVillager`), client `PaleVillagerRenderer` (villager model and
  `VillagerProfessionLayer` over `tools/pale-villager.py`'s white skin), `Undeath#wither`/`turnPaleVillager`/`risePale`,
  `LivingEntityWitheringMixin`, `ServerPlayerWitheredMixin`, `VillagerHostilesSensorMixin`, `MobSunArmourMixin`,
  `CustomBrewingItems.BROWN_MUSHROOM_CLUSTER`, client `PaleSight` and `assets/players-versus/post_effect`
  (`tools/post-effects.py`). Game tests in `UndeadFormsGameTest`; `GazeTest`, `BlightTest`.
- `MushroomCreeperEntity#puff` doses nothing. `RedMushroomsLayer` (client) draws the red mushroom's block model on a
  model part, resolved each frame by `MushroomCreeperRenderer` and `MushroomSpiderRenderer`; their skins come from
  `tools/mushroom-mobs.py`, now from the red mushroom block.
- `WatchlingEntity` (extends `Enderman`, no goals; its haunting in `customServerAiStep`), `Gaze` (pure, `GazeTest`;
  `VersusSettings.Gaze`), client `GazeTranslucency` and `LivingEntityRendererGazeMixin` (a see-through render type and
  a faded tint for the warped, layers once mostly seen, shadows faded), `WatchlingStare` (server: who looks, the static, the death;
  `WatchedPayload`), client `WatchlingRenderer` (eyes layer swapped, as the pale spider's) and `WatchlingStatic` (a HUD
  element after the misc overlays: a grey wash and snow from `textures/gui/watchling_static.png`, a random part of it
  every frame). `VersusSettings.Watchlings`. Damage type `players-versus:watched`. Textures from `tools/watchling.py`.

## Rulings

- Pale mobs don't turn back into regular ones near the surface (the user's call).
- The undead form of the place goes by place only (depth, biome, water), not light: predictable at a glance.
- Zombies converted to pale zombies wherever they are (Blight is paleness), not by place.
- Spiders and cave spiders turn into pale spiders, horses into zombie horses: the pale and undead forms that exist.
- Pale spiders now also spawn in caves below y 0, as pale zombies and creepers do.
- The watchling's look cone is about 25°, wider than an enderman's stare: it's what's on your screen, as Slender's.
- The static is the server's count, sent to the player, so death and screen agree; the client only eases it.
- A villager's fate goes by place as the zombie of the place does: a pale place makes an illager, by the kill route
  too, so what a player sees in the deep caves doesn't depend on how the villager died.
- Illagers turned from villagers are armed by hand (no `finalizeSpawn`): no 6% patrol captains with ominous banners.
- Undead animals burn in daylight, as vanilla has its zombie horse and zombie nautilus; the zombie chicken follows.
  Keep them under a roof (the camel husk, of the desert, doesn't burn).
- The zombie chicken keeps the chicken's seed-following, so it can be led; it just won't eat or breed.
- Third round: the pale-place illager turning is replaced by the pale villager. Paleness of place still decides where
  mushrooms turn pale and the pale spawn; what a thing turns into decides by what took it: Blight makes zombies,
  withering makes the pale.
- The pale villager is built on the zombie villager (it keeps the villager's data and trades and inherits the mod's
  golden apple cure, ZombieVillagerMixin) but looks, sounds and acts a villager gone wrong, not a zombie.
- The zombie villager cure's check was inverted (no golden apple could start a cure); fixed, as the user asked for
  golden apple cures.
- Golden apples cure Blight when eaten; milk still clears it too, as it clears every effect.
- The brown mushroom cluster is an item of its own, so only a grown cluster gives one; prismarine still smelts into
  the concentrate.
- The watchling's screen effect uses vanilla's creeper-view shader (`post/bits`) at four strengths, drained of colour,
  under the HUD static; the Blight grey uses `post/color_convolve` at four strengths. Both are asked of the game
  renderer's requested post effects each tick (client `PaleSight`); `tools/post-effects.py` writes them.
- Translucency and screen effects are client-side only and couldn't be seen in CI (no client run): unverified in game.
- Fourth round: the grey eases (5 seconds end to end) through 20 post effects, the watchling's view through 16; the
  post effect's uniforms are fixed per file, so smoothness comes from small steps and easing rather than a live
  uniform. The see-through fade spans 0 to 40 degrees off the body's edge, smoothly, with a 12% floor so the warped
  are never wholly invisible; a watchling counts as watched from 70% seen (about 14 degrees off its edge). Eyes fade by
  dimming their glow; a carried block (which can't be see-through) shows from 60%. Phantoms join the warped.
- The pale villager's face is the illager's (brow, eyes, mouth) laid on the villager head, which shares the illager's
  layout; its skin the illager's grey, paler. Children and eggs are only greyed (other layouts).
- The pale wandering trader extends the pale villager; its cure (a mixin at ZombieVillager#finishConversion's head)
  makes a wandering trader with the trades it carried and two days' wandering, not a villager.

## Blight replaces the Wither (fifth round, 2026-11-01)

Asked for: lingering clouds of it should stack; it should shake the sufferer; all white should make a player zombie
keeping the player's items, equipment and skin (desaturated, greener); and Conversion should be renamed Blight and
replace the Wither.

- Renamed throughout: effect `players-versus:blight`, classes, tags (`blight_immune`), sprites, post effects
  (`blight_1..20`), lang. Saves lose the old `players-versus:conversion` effect (it didn't exist outside this branch).
- `LivingEntityBlightMixin` redirects every Wither given into Blight (`VersusSettings.Blight.REPLACE_WITHER`; at most
  `WITHER_MAX_HEARTS` white hearts a dose) before stacking it, and keeps Wither (like Blight) off the immune. The mod's
  own Wither sources name Blight directly (Decay potions, concentrates of death and decay, their biles, haunting's end,
  the recovery compass, which now gives a single white heart for the ritual). Bile blocks give a dose every two seconds
  of standing in them, as Blight stacks. The wither-damage hooks in `ZombieMixin` and `CreeperMixin`, the withered-player
  mixin and the pale zombie's own Wither are gone; the pale villager's hits are Blight like the rest
  (`BlightAttacks`).
- Stacking: the cloud test passed on CI before any change (the injector already caught the cloud's `addEffect`), so a
  stacking failure wasn't reproduced; what a player in survival would not have seen was the shaking and the turning in
  creative or spectator, which `whileConverting` skipped. Creative players now shake, turn (living) and leave a copy.
- Shaking: from `SHAKE_FROM` (40%) of the hearts white it builds to all white, for everyone: the body shakes as a
  frozen one's (derived on the client from the effect, which the server already sends: no packet any more), and the
  player's own camera sways up to `SHAKE_DEGREES`, never their aim (`BlightCameraShakeMixin`).
- `PlayerZombieEntity` (a `Zombie`; client `PlayerZombieRenderer`, a zombie model in the player's skin from the skin
  cache, multiplied by a grey-green tint in `LivingEntityRendererGazeMixin`; spawn egg, loot table, `#minecraft:zombies`,
  Blight-immune). `ServerPlayerBlightedDeathMixin` raises it at `ServerPlayer#die` HEAD, moving the player's things to it
  so the vanilla drops find none. Its skin on the zombie's classic model: slim arms (Alex-style) show a pixel of the
  neighbouring texture, and outer layers (jacket, sleeves) aren't drawn.
- `turned_undead` now bypasses invulnerability, so a freshly spawned player's turning isn't blocked.
- Not verified in a running client (no client in CI): the camera sway, the skin and tint, the effect icon and egg art
  (`tools/blight-art.py`).
