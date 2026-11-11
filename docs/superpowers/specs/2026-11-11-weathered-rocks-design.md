# Weathered rocks: the rock model rebuilt from the approved prototype

**Status: approved 2026-11-11, with the placement rules added from the review.** Part 1 of 4, in the order agreed on
2026-11-11:

1. The rocks rebuilt (this spec).
2. Terrain: the old 3D-noise terrain moved to humidity 0 to 0.3 and erosion 0.3 to 0.6, held to beta-like heights; the
   windswept biomes placed where their climate fits.
3. The rocky biomes (Rocky Plains, Rocky Jungle, a rare rocky birch and spruce forest) and where rocks appear (the
   rules are below, in "Where rocks go").
4. The earlier list: the warm dark forest and the other transitions, the wetlands.

It replaces the "Rocks" section of `2026-11-09-rocks-transitions-wetlands-design.md`.

## What was asked

After four rounds of a throwaway prototype rendered next to the references:

- "Rocks, in the real world, don't just form from noise ... references have smooth rock faces with intersecting big
  structures ... interlacing boulder formations eroded with time."
- "Rocks aren't just blobs, they're mostly angular; they have corners and edges."
- "The reference images don't use shaders, instead they use darker blocks to texture and make it look as if light was
  shining on the stone; a bit of texturing with clay as lightest rarely used block, andesite for highlights and edges,
  moss on top (and grass), and tuff for corners, undersides, and to draw cracks." Then: "Avoid using deepslate."
- "Some of your cliff faces are too straight/flat, with no supporting smaller block at the base and no slight curve
  down either."
- On the last round: "These are officially perfect."

The prototype's last renders are the reference for the look; the rebuilt model is checked against them.

## How a rock is made

| Question | Decision |
|---|---|
| What a rock is | A few big angular blocks that meet and overlap, never noise |
| A block | A box whose faces are a little off square (up to 0.06 to 0.15), its edges and corners cut off by 2 to 13 planes that cut in to 60 to 90% of the way out, deeper on its upper half where it should narrow upward; the planes' soft maximum (0.4 block) barely rounds the edges |
| Big faces | Bent: 1 to 3 faces of a big block lean back by 7 to 18° above a line 20 to 60% of the way up, so no big face is one flat plane and each comes out toward its foot |
| Where blocks meet | Blended a little (a smooth union over 0.5 to 0.8 blocks): a seam is a groove, never a slot |
| The foot | The rock widens where it meets the ground, curving out: by 1 block over the lowest 3 for boulders, 0.5 over 2 for piles, 3 over 9 for tall rocks |
| Props | Smaller blocks lean in against the foot of every block 6 or more tall: 1 to 3 of them, 2 to 4 for blocks 16 or more tall; 22 to 50% of its width, 14 to 40% of its height, tipped 6 to 17° toward it, set against one of its faces (the broader more often), never where rock already is |
| Clean-up | Single blocks standing out on five sides go; pits shut in on five sides fill |

The shapes keep their names (the data and `/pvwg rock` use them); each is a recipe of blocks:

| Shape | Recipe |
|---|---|
| boulder | A big block narrowing upward (radius about half its height), one or two smaller ones (55 to 85% as tall) leaning on it, up to two flat stones at its foot |
| pile | Layers of flat blocks, each layer narrower than the one under it (a cone about as tall as it is wide), lying over each other |
| dome | A broad boulder: the boulder's recipe, lower and wider |
| crag | A few upright columns close together, 8 to 13 tall |
| tower, needle, spire | Columns of blocks between upright joints (a jittered grid), each column its own height, lower outward, its top block cut at angles: tower 1 to 3 columns, needle one narrow one, spire a tall column with a needle beside it |
| massif | The same, wide, the columns' heights nearly level (a mesa) |
| monolith | A tall leaning mass and a lower shoulder against it, opposite its lean |

## How a rock is coloured

Light is painted into the stone from above. Minecraft has no sun direction, so nothing depends on one. For each block
of rock open to the air:

- what it sees of the sky (five rays: up, and tipped half a right angle four ways);
- how far it faces up;
- how open it stands (the share of open cells within two blocks: more on an edge, less in a corner);
- how high it is on the rock.

Undersides, inside corners and the drawn cracks are tuff. The rest goes by its light: the darkest 30% tuff, then 46%
stone, 21% andesite and the brightest 3% clay. The cracks drawn are half of the seams between blocks, each drawn whole,
never the joints inside one column. Moss (mostly) and grass grow on level tops, more often higher up, with moss hanging
a block or two over their edges and plants on the grass. No deepslate anywhere.

The palette gets one key, `bright` (clay by default); `shade` is tuff, `base` stone, `light` andesite.

## What stays, what goes

- Stays: the canvas and the world's canvas, the stamper (column by column, so a rock stamped a chunk at a time is the
  rock stamped whole; feet filled down to the ground), the rock feature, the rock formations and their saved pieces,
  `/pvwg rock`, the template JSON (`shape`, `count`, `distance`, `angle`, `height`, `radius`, `cover`), the preview
  and the gallery. The ground still rises against a rock's foot, all around it: up to 1 to 2.5 blocks, out to 3 to 10.
- Goes: the noise model (lobes, warp, Voronoi pieces and chunks, roughness, flutes, pinnacles, the sun-side shading),
  slabs, loose stones and vines. The template keys `vines`, `plants`, `scree`, `slabs` and `skirt` go with them; no
  data uses them.
- A big formation's model is built in 300 ms or less (how open a block stands is read from a summed-volume table,
  not counted cell by cell).

## Where rocks go (decided here, built in part 3)

From the review of this spec: "boulders ought to be in areas of mostly flat terrain, in middling ranges, and nowhere
near mountain peaks"; "at most they should start to appear erosion > -0.3, but ought to stay in flat stretches"; "they
appear in flat areas like jungles, plains, savannas, in the middle ranges of temperature"; "a case could be made to have
some appear in meadows in rare clusters".

- Flat ground only: erosion above -0.3, and there only where the ground is nearly level under the rock and around it.
  Never on mountainsides (where the layout's mountainside rule applies), on slopes or near peaks.
- Middle temperatures: in the plains, forests, savannas and jungles (temperature from about -0.15 to 0.55); not in the
  frozen and cold biomes, not in deserts or badlands.
- Never in the 3D-noise terrain (part 2).
- Meadows: a rare cluster here and there.
- The rocky biomes (part 3): many.

The layout rules in `docs/worldgen-refactor-plan.md` (mountainside, peak-warm and the rest) say which biome takes which
climate; they don't place rocks, and stay as they are.

## Checks

- Unit tests (`RockModelTest` rewritten, `RockTemplateTest` adjusted): the same seed gives the same rock; nothing
  floats; stamped a chunk at a time in any order is stamped whole; it stays within its reach, a feature's within 16
  blocks; it reaches the ground on a slope; no noise (single-block knobs and pits under 1% of its surface); a boulder is
  two or more blocks, a monolith has its shoulder, a tower's columns differ in height; a tall rock is wider at its
  foot than halfway up and has props there; only tuff, stone, andesite and clay under the cover; undersides are tuff;
  what faces up and stands high is lighter than what faces down and lies low; stone is the commonest tone and clay
  5% or less; cover only on level tops under the sky; nothing green under water.
- Game tests (`RocksGameTest`): as now.
- Looking: preview renders of every shape next to the references and the prototype's last renders, then the in-game
  gallery (`tools/gallery.sh @tools/gallery-rocks.txt`).

## Not in this part

Where rocks appear and the rocky biomes (part 3); the terrain (part 2).
