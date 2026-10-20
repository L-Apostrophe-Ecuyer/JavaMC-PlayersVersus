# Fallen leaves, instant leaf decay, and being in leaves

Minecraft 26.4-snapshot-3 (Mojang names), `26.4---Winter-Drop`. Builds on
[the leaf overhaul](2026-10-18-leaf-overhaul-design.md): its litter now comes as fallen leaves, its landing cushion goes,
leaves cut off from their tree fall at once, and being inside leaves looks and swims like it.

## What it should feel like

Green leaves fall green under the trees that have them, and brown into vanilla leaf litter over the next day; autumn
poplars drop brown litter straight away, cherries petals. Fell a tree and its crown comes down at once, carpeting
the ground. With your head in a bush you see leaves around the edge of your view, and leaves underwater hold back a
swimmer as they hold back a walker.

## Fallen leaves

**One block**, `players-versus:fallen_leaves`: green leaf litter, for the trees whose leaves take the biome's colour
or something close to it: oak, birch, jungle, acacia, dark oak, mangrove, azalea and flowering azalea. The others keep
what they had: the poplars, whose leaves are already autumn red, orange and yellow, shed vanilla leaf litter; cherry
sheds pink petals, which never brown; spruce and pale oak shed nothing. `FallenLeavesBlock` extends vanilla's
`LeafLitterBlock` (segments 1 to 4, facing, no collision, survives on a sturdy top face) and adds `persistent`, as
leaves have.

**Looks.** Vanilla's leaf litter models and texture, unchanged: the texture is grayscale and every face takes tint 0,
where vanilla's litter takes the biome's dry foliage colour and fallen leaves the biome's foliage colour, as oak leaves
do. The item is vanilla's litter texture as a flat icon (`item/generated` over `minecraft:block/leaf_litter`), tinted
with oak leaves' item colour (#48B518).

**Browning.** Natural fallen leaves tick randomly; each random tick turns them into vanilla `leaf_litter` with the same
segments and facing at a chance of `FALLEN_LEAVES_BROWNING_CHANCE` (0.057: a block gets a random tick about every 68
seconds at the default tick speed, so they last about an in-game day on average). Fallen leaves a player places are
persistent and never brown, as placed leaves never decay; adding a segment to a natural pile by hand makes it yours.

**Drops.** Silk Touch or shears: fallen leaves, one per segment. Anything else: vanilla leaf litter, one per segment.
Explosions decay the drop, as vanilla's litter.

**Like vanilla litter.** Same sound and strength, a plant-green map colour, flammable (60, 100), compostable (0.3),
listed after leaf litter in the natural blocks tab.

## Shedding

Everything that shed litter sheds by these tags: `players-versus:sheds_fallen_leaves` (the eight above),
`players-versus:sheds_leaf_litter` (now the poplars only) and `players-versus:sheds_pink_petals` (cherry). Natural
leaves something pushes through shed, and so do decaying leaves.

A segment falls straight down through any leaves to the first air with sturdy ground under it, within 24 blocks, as
before. There it joins a pile: a leaf joins fallen leaves or brown leaf litter alike (one more segment, up to 4; the
pile stays what it is), petals join only petals. Anything else stops it, as before (plants, water, other blocks).

## Leaf decay

**Instant.** When a natural leaf learns in its scheduled tick that its hold on its tree got weaker (its neighbours
remember a longer way to a log than it did, or it's at 7), it searches out through natural leaves of its own kind for
one of its tree's logs, six steps at most. Finding none, it decays right then: vanilla's drops, a quarter of the time a
segment of fallen leaves, and a small puff of its leaf particles (no sound). A felled crown falls in a ripple, a block a
tick: a small oak's is gone in three. Leaves that never get a scheduled tick still decay on a random tick, as vanilla,
and so does a leaf whose search would reach unloaded chunks.

**The rules.** A leaf lives within six blocks of one of its own tree's logs, stripped or not, counting through natural
leaves of its own kind; leaves whose tree has no logs of its own here (mangrove, azalea, pale oak, poplars) take any
log. Placed leaves remember no distance and hold up no tree's reach. The mod had these rules as static maps on the
leaves block, filled while the game was still making its blocks, so every leaves key was null and they never ran;
`LeafReach` holds them now, built on first use.

Going at once makes the rules show: a natural tree must keep its own leaves after any update. A game test grows every
vanilla tree, updates every one of its leaves, and checks none falls. What it caught:
- jungle bushes are oak leaves around a jungle log: oak leaves also take jungle logs;
- azalea trees mix azalea and flowering azalea leaves: the two are one kind;
- pines, swamp oaks, mega spruces, cherries, acacias and dark and pale oaks grow leaves six blocks out: no tree reaches
  less than vanilla's six (the mod's five for most trees is gone);
- vanilla grows some leaves remembering a step more than they are, so a leaf six out could see no neighbour at five
  for a tick: a weakened hold searches before it decays.

**Not solved here.** A leaf is kept by any log of its type within its reach, so a felled tree's leaves can stay on a
neighbour's log of the same type. Knowing which tree a leaf belongs to is for the tree log block
(`2026-10-19-trees-research.md`).

## Being in leaves

**The view.** While the camera is inside a leaf block, leaves frame the screen as frost does in powder snow: a
pixel-art frame of leaves of our own (`textures/misc/leaves_outline.png`, grey, drawn by `tools/leaves-outline.py`),
thick in the corners and along the edges and clear in the middle, stretched over the screen in the colour of the leaf at
that spot (its tint, or the average colour of its texture for leaves with no tint). It eases in and out over a few ticks, and isn't
drawn in spectator mode. Client only (`LeavesView`, a HUD element drawn before vanilla's camera overlays).

**Swimming.** Leaves underwater drag a swimmer (`LeafMovement.swim`, with the same immersion as walking), in vanilla's
water travel, before the move: `SWIM_DRAG` (0.15) of its speed a tick fully inside, across by how much of its body is in
them, up and down by how much of its path. The drag stays in its speed for water to slow further, so a swimmer fully in
leaves keeps about half to two thirds of its speed (a third sprint-swimming), as a hedge leaves a walker two thirds. Not
walking's speed cap: a swimmer goes about 0.1 to 0.2 blocks a tick, around the cap, so it would barely slow one. No
climbing or sinking help: swimming already goes up and down.

## Removed

The litter landing cushion: `LeafLitter.cushionLanding`, its call in `LeafMovementMixin`,
`VersusSettings.Leaves.LITTER_CUSHION_PER_SEGMENT`, the game test `aPileOfLitterSoftensALanding`, and its lines in the
docs.

## Settings (`VersusSettings.Leaves`)

`FALLEN_LEAVES_BROWNING_CHANCE` (0.057), `DO_INSTANT_LEAF_DECAY` (true), `SWIM_DRAG` (0.15), `DO_LEAVES_VIEW` (true,
client), `DECAY_LITTER_CHANCE` (0.25, as before).

## Testing

Game tests (server):
- shedding gives fallen leaves under oak, brown litter under a poplar, petals under cherry and nothing under spruce,
  and a shed onto a brown pile adds a segment to it;
- natural fallen leaves brown after enough random ticks, keeping segments and facing; placed ones never do;
- drops: Silk Touch and shears give fallen leaves per segment, a bare hand gives leaf litter per segment;
- cutting a tree's log drops its crown within a few ticks; persistent leaves stay, waterlogged ones leave water;
- a leaf lives only by its own tree's logs (an oak leaf by a birch log falls; by a stripped oak log or a jungle log it
  stays); a placed leaf holds up no natural one;
- every vanilla tree keeps all its leaves through an update of each;
- a husk crossing waterlogged leaves goes slower than one crossing open water.

Not testable headless: the tint and the leafy view (client).
