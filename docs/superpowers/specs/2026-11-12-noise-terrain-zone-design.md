# The 3D-noise terrain's place, Beta-style heights, windswept biomes by climate (part 2)

Part 2 of the rocks, terrain and transitions pass (parts: 1 rocks rebuilt, 2 this, 3 rocky biomes and where rocks go,
4 the warm dark forest, the other transitions and the wetlands). It follows `2026-11-11-weathered-rocks-design.md`, whose
"Where rocks go" already keeps rocks out of this part's zone.

## What was asked

From the review of the rocks' placement ("Yes I trust your instincts! And not just very high erosion, but generally
flat areas. I would also add, right now very high erosion has 'windswept' biomes and areas that I have some problems
with"):

- "The old 3d noise is great and ought to have a place for it to exist, but this is the wrong place. Maybe a band of
  humidity 0-0.3 and erosion 0.3-0.6 would be better."
- "The old 3d noise areas should have beta style terrain gen rather than noise going up to y256."
- "The windswept biomes placed there are bad for a number of reasons, the main being that they don't fit the area in
  terms of temperature and humidity and are also ugly/have ugly grass and foliage colors. Keep the biomes, but change
  placement rules."
- "Boulders don't belong wherever we place the old 3d noise" (built in part 3: `RockGround`, the rocky jungle's rules).

## What it is today

The terrain is vanilla's: `sloped_cheese = 4 * quarter_negative((depth + jaggedness * ...) * factor) + base_3d_noise`,
`depth = gradient + offset`. Vanilla's `offset` and `factor` are splines of continentalness, erosion and the ridges.
The "old 3D noise" (`old_blended_noise`, the terrain of Beta and the releases up to 1.17) is in every column; how much it
shows depends on `factor`: the vertical slope of the density is 4 * factor / 128 per block, so a noise of 1 moves the
ground by 32 / factor blocks. Where factor is 4.7 to 6.3, the ground moves by about 5 blocks (plains); where it is
1.4 to 1.8, by 20 or more, with overhangs and floating pieces.

Vanilla's splines hold one bump at erosion 0.40 to 0.58 (the points at 0.45 and 0.55 are its plateau): factor falls
from 4.7 to 6.3 down to 1.4 to 1.8 and offset rises from about -0.50 to -0.33 (the ground about 22 blocks higher). It is
the same bump the biomes use: vanilla's windswept biomes fill erosion 0.45 to 0.55, every humidity, every temperature
up to 0.2. Measured (seed 1234, 1,500 inland columns): the bump's highest solid block has a median of y 69, p90 95, p99
121, and 7 columns over y 128; the noise is faded out only from y 240 to 256.

The zone asked for is 5.5% of the land (humidity 0 to 0.3, erosion 0.3 to 0.6); the bump is 3.65% of it, and 39% of
the bump lies inside the zone.

## How it is made

### The zone

One smooth window, `players-versus:overworld/noise_zone` (0 to 1), of the three climate values the biomes use
(`minecraft:overworld/vegetation` is the humidity): the product of smoothsteps, each rising over its fade at one edge of
the box and falling over its fade at the other.

| Climate value | Box | Fade (inside the box) |
|---|---|---|
| humidity | 0 to 0.3 | 0.05 |
| erosion | 0.3 to 0.6 | 0.06 |
| continentalness | from 0.03 | 0.09 (inland: where vanilla puts its windswept biomes) |

Its numbers are `PvWorldgenConstants`'s (`NOISE_ZONE_*`); the density-function generator
(`tools/port-26.3/pv_density.py`) and the Java side (`PvBiomeLayout`, `RockGround`) read the same ones.

### Terrain

`players-versus:overworld/offset` and `factor` (the mod's own; vanilla's stay as they are for the other world types) are
vanilla's splines in two forms, mixed by the window: `lerp(zone, plain, shattered)`.

- **plain**: vanilla's spline without the bump: the points at erosion 0.45 and 0.55 are taken out of each erosion
  node, so the spline runs from its point at 0.40 to the one at 0.58 (the far inland nodes, which is most of the zone;
  the factor's nodes at the coast, from 0.35 to 0.62), which are the plain terrain either side (factor 4.7 to 6.3,
  offset about -0.50). Everywhere outside erosion 0.35 to 0.62 it is vanilla's spline, point for point. Vanilla's low
  factor on peaks (ridges at the plateaus, 1.56 at erosion 0.4) isn't part of the bump: it fades out toward 4.7 at 0.58,
  so at erosion 0.5 on peaks the factor is still about 3.4 outside the zone.
- **shattered**: the bump's plateau, its sub-spline at erosion 0.45 in each node that has one (the nodes of the coast,
  which have none, read erosion 0.5), a function of continentalness and the ridges only. Inside the window the terrain
  doesn't depend on the erosion; the window's fades are the transition, over about 100 blocks at the climate's median
  gradient and 35 where it is steepest (a tenth of the ground): the ground rises 22 blocks over a fade, as steeply as
  vanilla's own bump does: measured over 1,100 columns in the fades and 1,300 on vanilla's bump edges, the 90th
  percentile of the offset's change in 8 blocks is 0.031 against 0.034 (the factor's 0.78 against 0.82); the test asks
  for at most 1.25 times vanilla's. (The humidity noise changes about a seventh faster per block than the erosion's;
  the zone's humidity edges are that much steeper than its erosion edges.)

`tools/terrain.py` writes the two functions (and the surface-level copies) from vanilla's files in the game's jar.

`players-versus:overworld/depth` takes the new offset and `sloped_cheese` the new factor; jaggedness stays vanilla's (its
splines end at erosion -0.375). The surface-level estimates the aquifer and the material rules read
(`preliminary_surface_level`, `chunk_surface_level`) are copied with the new offset and factor, so they follow.

### Beta-style heights

In the zone the base noise fades out with height: `base_3d_noise * (1 - zone * ramp(y))`, the ramp rising from 0 at y 100
to 1 at y 128. Above y 128 there is no noise in the zone, so the density is the smooth depth's, negative: no ground. Beta's
world was 128 tall, its hills between 64 and 110: the zone's ground (offset about -0.33, factor 1.4 to 1.8, valleys
lower) is rugged, the noise moving it by 15 to 25 blocks, and none of it stands over 128. Measured over 336 columns well
inside the zone: the highest solid block has a median of y 69, a tenth percentile of 53, a ninetieth of 89, a 99th of
107 and a highest of 111 (the same columns without the cap: highest 149, and 6 columns in 1,200 over y 128). Outside
the zone nothing changes (a test compares the sloped cheese with the old formula).

### Windswept biomes

They are the biomes of the rugged terrain, so they go where it is: vanilla's windswept slices (erosion 0.45 to 0.55,
which the window holds at full strength from 0.36 to 0.54) stay windswept only where the window is at half strength or
more: the humidity 0.025 to 0.275, the continentalness 0.075 or more, and the temperature below 0 (the rule that took them
from temperate climates, `TEMPERATE_MIN`, stays), as the biome that fits:

| Temperature | Humidity 0.025 to 0.1 | Humidity 0.1 to 0.275 |
|---|---|---|
| below -0.15 | windswept gravelly hills | windswept forest |
| -0.15 to 0 | windswept hills | windswept forest |

(Vanilla puts gravelly hills at humidities under -0.1, outside the zone; they stay, as the cold, dry biome of it.)
Windswept slices anywhere else become what lies beside them, by vanilla's middle biomes: snowy plains, snowy taiga,
taiga, forest, plains and old growth spruce taiga, by temperature and humidity (plains and forests from -0.15 up, as the
temperate rule does). The rest of the zone keeps the biomes of its climate, on the rugged ground: forests, plains,
taigas, and above freezing the temperate and warm ones.

The windswept savanna stays out: the earlier request replaced the warm windswept biomes with their warm variants.

## Checks

- Unit tests with the game's own evaluation (`WorldgenTestData`: vanilla's pack and this mod's, the router built as a
  world does, `RandomState#sampleBlockValueUncached`): the window is 0 outside the box and 1 well inside; outside the
  window and outside erosion 0.35 to 0.63 the mod's offset and factor equal vanilla's; in the band outside the window
  the offset is under -0.4 (vanilla's -0.33) and the mean factor 4 or more (4.8; vanilla's 3.1, and not every column:
  peaks keep some of their low factor, see above); inside the window they are the plateau's (equal to vanilla's where it
  is the plateau); over the fades the ground and the noise's weight change no faster than along vanilla's bump edges
  (the 90th percentile of the steps in 8 blocks, at most 1.25 times); in the zone the base noise is gone over y 128 and
  nothing is solid over it, the tops' median lies between y 65 and 90 and their spread (tenth to ninetieth percentile)
  is 25 blocks or more.
- `PvBiomeLayoutTest`: windswept biomes only inside the zone where the window is at half strength or more, and below 0
  (400,000 random climates); the table above, slice by slice, and what lies beside them elsewhere; the pieces still
  partition vanilla's slices.
- Looking: the gallery's windswept biomes, before and after, and the zone's edge.
- Running: `runWorldgenSmoke` for the Players Versus world type (census of the windswept biomes), the game tests.

## Not in this part

The other places vanilla's splines have a low factor (the plateaus at low erosion and some ridges) stay: they aren't
the very high erosion the request is about. The warm dark forest and the wetlands (part 4).
