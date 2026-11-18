# The monster spawn check's sky light, and a pass over the tags

Asked (2026-11-18), of the deferred minors:

- "Skylight being read just once if possible"
- "tags do need a pass"
- (Also: "Pale mobs should probably spawn from biome jsons instead of mixin replacements for performance reasons" — held
  for the owner's choice: 26.4's biome spawn lists can't say "below y 0", see the end.)

## The sky light

**Now:** the mod's monster spawn check (`HostileEntityMixin`, which takes over vanilla's `checkMonsterSpawnRules`) reads
the sky light for its own rules, then again for "sees the sky" (`canSeeSky` is the sky light being 15), then vanilla's
darkness check (`Monster.isDarkEnoughToSpawn`) reads it a third time, and a fourth inside its brightness test.

**Decision:** one read of the sky light (and one of the block light) a check. The rules take "sees the sky" from it
(`overworldRules` loses its `seesSky` argument). The darkness check is the mod's copy of vanilla's
(`MobSpawning.darkEnoughToSpawn`), given that read: the same tests in the same order, the random drawn as vanilla draws
it, the brightness worked out from the two reads as the light engine does. A game test holds the copy to vanilla's
answer, place by place and seed by seed, so that a version that changes vanilla's check fails it.

## The tags

**Now:** 181 tag files. None fails to load. But several generators append to the same biome tags and never take an
entry out, so the tags keep what earlier passes put there; some files re-list what vanilla's own tag already has, or
what a tag nested in them already holds; one entry is listed twice; Fabric warns that the mod's item tags have no names.

**Decisions:**

1. **The climate variants are the animals table's.** The warm and cold farm animal and frog tags hold exactly what
   `tools/animals.py` says (`WARM`, `COLD`, `COLD_FROGS`), less what vanilla's own tags already cover;
   `tools/refine-biomes.py` writes them whole, and the transition biomes no longer inherit them from their parents. So
   the five biomes an old hand edit made cold-frog biomes (the birch taiga, dappled taiga, sparse dappled and dark taiga
   forests, the dark birch forest) get the temperate frog.
2. **The rusty wolf coat's second condition goes:** its one biome, the mountainside jungle, is a jungle already.
3. **No entry is said twice:** not within a file, not when vanilla's own file lists it, not when a tag the file or
   vanilla's file nests already holds it. The generators skip such entries as they add them; the files are pruned once.
   What each tag resolves to doesn't change, but for decisions 1 and 4.
4. **Natural ground** (`players-versus:natural_ground`: where zombies, skeletons, creepers and the pale mobs may spawn)
   gains what the overworld's own ground also is: the ores (their tags), infested stone and deepslate, raw iron and
   copper blocks (ore veins), suspicious gravel, blue ice, and the sulfur caves' sulfur and cinnabar (26.4's: without them
   no undead could spawn on a sulfur cave's floor).
5. **The item tags have names**, English and French, as Fabric asks.
6. **A test keeps them so** (`TagAuditTest`): no entry twice, no entry naming something the mod doesn't have, the
   variant tags the animals table's, natural ground as decided, every item tag named.

## Checks

- Game test: the darkness check matches vanilla's over places of every light and 200 seeds each.
- Unit: `MobSpawningTest` (the rules, without `seesSky`), `TagAuditTest`.
- Before and after the prune, every tag's resolved contents compared: unchanged but for decisions 1 and 4.

## The pale mobs (held)

In 26.4 a biome's spawns are its `minecraft:gameplay/natural_mob_spawns` attribute, layered by dimension, biome, timeline
and weather: nothing by height. In a Players Versus world (seed 1234, the climate sampled) no surface biome reaches below
y 0; underground, the regular cave is 68% of the caves above y 0 and 50% of those below, the deep caves 2% and 36%. So
for "everything below y 0 is pale" the regular cave would have to list both kinds, each refused at the wrong height, and
the spawner keeps the kind it picks for a whole group: picks of the wrong kind for the height are lost, about a third of
them in the regular caves. The swap costs a comparison a check and makes nothing it throws away.
