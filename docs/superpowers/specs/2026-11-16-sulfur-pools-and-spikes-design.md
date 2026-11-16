# Sulfur pools and sulfur spikes

Asked (2026-11-16), while the cypress work was finishing:

- "Sulfur pools always have many potent Sulfur+ magma block below at the pool floor?"
- "Sulfur spikes deal rand(1,(damage dealt)) seconds of poison and rand(5,(damage dealt)) seconds of nausea to entities
  they hurt"

## What the game does now (26.4-snapshot-4)

- **Sulfur pools** (`minecraft:sulfur_pool`, the sulfur caves' pools, vanilla's: the mod changes nothing of them): a
  lake of water with a sulfur barrier, then one potent sulfur block (wet) on the floor below the lake's origin, if the
  floor is within four blocks. Potent sulfur under water gives off noxious gas (Nausea for 4 seconds to whatever is near
  its gas at the surface). With a magma block under it, it is a geyser that erupts now and then (dormant, then erupting,
  launching what stands over it); with lava under it, one that erupts without end.
- **Sulfur spikes** (`minecraft:sulfur_spike`, a speleothem as pointed dripstone is): a spike hanging from a ceiling
  falls when its support goes and hurts what it lands on (damage type `falling_stalactite`, at least 6 damage a block
  fallen). Landing on a sulfur spike is an ordinary fall: unlike pointed dripstone and icicles, vanilla gives sulfur
  spikes no stalagmite damage of their own.

## Decisions

1. **Geysers on every pool's floor.** A share of each sulfur pool's floor, the solid blocks under its water, becomes
   potent sulfur with a magma block beneath it: periodic geysers (magma, not lava: the owner named magma). A third of the
   floor, rounded up, and at least six (all of it on a floor smaller than that), chosen at random: every pool has many.
   The pool's one vanilla potent sulfur block goes; these take its place. A floor block whose block beneath can't take
   magma (air, fluid, a block entity, what pools never replace) is passed over, so no magma hangs into a cave below.
2. **Where.** A feature of the mod's (`players-versus:pool_geysers`) as the second step of vanilla's `sulfur_pool`,
   overridden in the mod's data as its other vanilla features are (global, by the worldgen plan's decision S6).
3. **Sulfur spikes poison and nauseate.** A sulfur spike that hurts a living thing gives it Poison I for a whole number
   of seconds from 1 to the damage dealt, and Nausea for a whole number from 5 to the damage dealt (5 when it dealt less
   than 5), each uniformly at random. The damage dealt is what the thing took, after armour and enchantments, absorption
   included (Fabric's "damage taken"). Undead, immune to poison, get the nausea only.
4. **What counts as a sulfur spike hurting.** A falling sulfur spike landing on it (`falling_stalactite` from a falling
   sulfur spike), and landing on a sulfur spike: the fall damage taken coming down on one is the spike's, though vanilla
   adds none of its own. The numbers (1 and 5 seconds) are `VersusSettings.SulfurSpikes`.

## Checks

- Game tests: a sulfur pool grown on a slab of sulfur, over several seeds, has at least six (a third of its floor) of
  potent sulfur geysers on its floor, each over magma; a falling sulfur spike and a fall onto one each poison and
  nauseate a pig for times within the damage it took; a fall onto plain ground doesn't.
- Unit test: the durations' bounds, both ends reachable.
- The gallery: a sulfur pool, looked at.
