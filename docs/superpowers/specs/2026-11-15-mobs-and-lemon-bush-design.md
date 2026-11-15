# Mobs and the lemon bush

Asked (2026-11-15), alongside finishing the refinement pass:

- "Making Watchlings spawns sometimes"
- "Shulkers being carried should despawn when their enderman despawns"
- "Endermen, phantom, etc eyes (and carried blocks) ought to stay visible always, with only the bodies being translucent"
- "Mobs spawn rates need adjusting. Creepers should only spawn if sky light is zero, spiders are not found anywhere atm,
  zombies abs skeletons should only spawn on natural blocks, and deep mobs (pale creeper, pale zombie, etc) aren't
  spawning enough right now"
- "Lemon trees should be like tall rose bushes; a 2x1 bush block instead of a full tree"

## What the code does now

- **Watchlings** spawn by `MobSpawning`: weight 4 in the Pale Garden, 3 in the deep caves, the pale grotto and the
  regular cave (whose monster lists weigh 500 to 600 in all), only in total darkness off the pale biomes, on deepslate,
  tuff or pale moss, below y 0 (up to y 64 at a new moon's midnight). One spawn attempt in a few hundred picks one, and
  most of those are refused: they are as good as never seen.
- **A carried shulker** rides its enderman. When the enderman despawns, the game dismounts its passengers: the shulker
  stays behind, and shulkers don't despawn.
- **See-through mobs** (endermen, endermites, phantoms; watchlings by their own renderer) fade with the player's gaze
  (`GazeTranslucency`). Their eyes fade with them (`EyesLayerGazeMixin`), and a carried block shows only once the
  enderman is 60% seen (`CarriedBlockLayerGazeMixin`). A carried shulker isn't faded.
- **Spawn rules** (`HostileEntityMixin.checkMonsterSpawnRules`): zombies and skeletons need `undead_overworld_spawnable_on`
  ground (grass, snow, podzol, coarse dirt, sand, stone, deepslate, nether stone), not below y 0, not above y 64 under
  open sky; creepers need `creeper_spawnable_on` ground and sky light 7 or less, below y 128. All of it is skipped at
  midnight (zombies and skeletons then spawn on any block, planks included), the creepers' at a new moon too. Spiders
  (`SpiderMixin`) don't spawn below y -8, nor above y 64 (96 in warm biomes) where sky light is over 2.
- **Deep mobs**: pale creepers, spiders and zombies spawn by the pale line (`MobSpawning.palePlace`): light 0 or 1, on
  deepslate, tuff or pale moss, below y 0. Below y 0 the caves' own lists are mostly zombies, skeletons and spiders,
  which are refused there (zombies and skeletons below y 0, spiders below y -8): most spawn attempts down there come to
  nothing, and the pale mobs' own entries are few (pale zombies weigh 10 in the regular cave).
- **Lemons** grow on lemon leaves (`LemonLeavesBlock`) of a small tree (`lemon_bush`: two or three stripped oak logs
  under the leaves), in the Tuscan grove's orchards; the leaves blossom and fruit, and are picked for 2 to 6 lemons.

## Decisions

1. **Watchlings**: weight 15 in every pale biome (the Pale Garden, the pale taiga, the pale swamp: the mod's `pale`
   tag) and 12 in the three cave biomes; still in total darkness off the pale biomes, by the pale line; and never
   within 128 blocks of another watchling (a watchling haunts one player at a time).
2. **Carried shulkers**: an enderman that despawns takes its shulker with it (the shulker is discarded too). Killed,
   it still drops it.
3. **Eyes and carried blocks**: always drawn whole; only the bodies fade with the gaze (the two fading mixins go).
4. **Spawn rules**:
   - Creepers spawn only where sky light is 0 (under a roof of blocks: caves, overhangs, dense cover), on natural
     ground, in the dark, at every hour and moon.
   - Zombies and skeletons spawn only on natural ground, at every hour (midnight still lifts the rule against open sky
     above y 64).
   - Natural ground is one list for them all: the overworld's stones, dirts (grass, podzol, mycelium, moss, mud...),
     sands, gravel, terracotta, snow, clay, calcite, dripstone and the mod's muds and clays (and the nether's stones
     for the undead).
   - Spiders: what keeps them from spawning is found and fixed (a game test runs their spawn rules where they should
     spawn).
   - Below the pale line (y 0) a zombie, creeper or spider about to spawn naturally is its pale kin instead (pale
     zombie, pale creeper, pale spider), by the pale kin's own rules: everything below the pale line is pale. Pale
     mobs may stand on any natural ground there, not only deepslate, tuff and pale moss.
5. **Lemon bush**: a two-block-tall plant, as a rose bush is (lower and upper halves), leafy, that blossoms and fruits
   as the lemon leaves do (bare, in blossom, hung with lemons; on random ticks and bone meal), picked for 2 to 6 lemons
   (it goes bare), dropping itself when broken (and its lemons if fruiting). The Tuscan grove's orchards are patches of
   lemon bushes. The lemon sapling places a lemon bush. The lemon leaves and the lemon tree feature stay registered for
   the worlds that have them; nothing generates them any more.

## Checks

- Game tests: the spawn rules run where each mob should and shouldn't spawn (creepers under a roof and under the sky,
  zombies and skeletons on stone and on planks, spiders in a dark room, a watchling with and without another nearby);
  an enderman's despawn takes its shulker; the lemon bush grows, fruits and is picked.
- Unit tests where the logic is plain (the pale kin of a spawn, by height).
- The client: the gallery shows an enderman with its block at the edge of the view, its eyes and block whole.
