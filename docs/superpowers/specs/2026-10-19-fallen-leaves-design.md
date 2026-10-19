# Fallen leaves, instant leaf decay, and being in leaves

Minecraft 26.4-snapshot-3 (Mojang names), `26.4---Winter-Drop`. Builds on
[the leaf overhaul](2026-10-18-leaf-overhaul-design.md): its litter now comes as fallen leaves, its landing cushion goes,
leaves cut off from their tree fall at once, and being inside leaves looks and swims like it.

## What it should feel like

Leaves fall the colour they were on the tree: green oak leaves under an oak, red under a red poplar, needles under a
spruce. Over the next day they brown into vanilla leaf litter. Fell a tree and its crown comes down at once, carpeting
the ground. With your head in a bush you see leaves around the edge of your view, and leaves underwater hold back a
swimmer as they hold back a walker.

## Fallen leaves

**Blocks.** One per leaf type, `players-versus:fallen_<leaves>`: `fallen_oak_leaves`, `fallen_birch_leaves`,
`fallen_spruce_leaves`, `fallen_jungle_leaves`, `fallen_acacia_leaves`, `fallen_dark_oak_leaves`,
`fallen_mangrove_leaves`, `fallen_azalea_leaves`, `fallen_flowering_azalea_leaves`, `fallen_pale_oak_leaves`,
`fallen_red_poplar_leaves`, `fallen_orange_poplar_leaves`, `fallen_yellow_poplar_leaves`. Cherry leaves keep shedding
vanilla pink petals, which never brown. `FallenLeavesBlock` extends vanilla's `LeafLitterBlock` (segments 1 to 4,
facing, no collision, survives on a sturdy top face) and adds `persistent`, as leaves have.

**Looks.** Vanilla's leaf litter models and texture, unchanged: the texture is grayscale (it averages #AAAAAA, so a
tint shows at 0.67 of its value), and every face takes tint 0. Each block gets the tint its leaves have:

| fallen leaves of | tint (block) | tint (item) |
|---|---|---|
| oak, jungle, acacia, dark oak, mangrove | the biome's foliage colour, as the leaves | the leaves' item colour (#48B518; mangrove #92C648) |
| birch | #80A755, as the leaves | same |
| spruce | #619961, as the leaves | same |
| azalea | #87AB42 | same |
| flowering azalea | #95A65B | same |
| pale oak | #AEB6AB | same |
| red poplar | #E6423B | same |
| orange poplar | #FF7520 | same |
| yellow poplar | #FF9E30 | same |

The leaves without a biome tint draw their colour from their texture, so their tints are the texture's average colour
over 0.67: the pile then averages the leaves' own colour. Orange and yellow poplar would need more than full red, so
theirs keep the hue at full red and their piles show a little darker than their leaves.

The items use vanilla's litter texture as a flat icon with the item tint above (one shared item model,
`item/generated` over `minecraft:block/leaf_litter`).

**Browning.** Natural fallen leaves tick randomly; each random tick turns them into vanilla `leaf_litter` with the same
segments and facing at a chance of `FALLEN_LEAVES_BROWNING_CHANCE` (0.057: a block gets a random tick about every 68
seconds at the default tick speed, so they last about an in-game day on average). Fallen leaves a player places are
persistent and never brown, as placed leaves never decay; adding a segment to a natural pile by hand makes it yours.

**Drops.** Silk Touch or shears: the fallen leaves, one per segment. Anything else: vanilla leaf litter, one per segment.
Explosions decay the drop, as vanilla's litter.

**Like vanilla litter.** Same sound, strength, map colour of its leaves, flammable (60, 100), compostable (0.3), listed
after leaf litter in the natural blocks tab.

## Shedding

Everything that shed litter now sheds fallen leaves: natural leaves something pushes through, and decaying leaves. Which
fallen leaves come from which leaves is a map in code (`FallenLeaves`), not the two tags, which go: every leaf type
has its own block. Leaves of other mods shed nothing.

A segment falls straight down through any leaves to the first air with sturdy ground under it, within 24 blocks, as
before. There it joins a pile: fallen leaves of any tree or brown leaf litter get one more segment (up to 4) and stay
what they are, petals join only petals. Anything else stops it, as before (plants, water, other blocks).

## Leaf decay

**Instant.** When a natural leaf's distance becomes 7 in its scheduled tick (cut off from its tree's logs, by the
mod's rules: its own log type, 5 blocks for most trees), it decays right then: vanilla's drops, a quarter of the time a
segment of fallen leaves, and a small puff of its leaf particles (no sound; a crown falls in a ripple, a block a tick).
Leaves that never get a scheduled tick still decay on a random tick, as vanilla.

Going at once makes the reach rules show: a natural tree must keep its own leaves after any update. A game test grows
every vanilla tree, updates every one of its leaves, and checks none falls. Vanilla's jungle bushes are oak leaves
around a jungle log, so oak leaves also count jungle logs as theirs; any other tree the test catches gets its pairing
or reach fixed the same way, and listed here.

**Not solved here.** A leaf is kept by any log of its type within its reach, so a felled tree's leaves can stay on a
neighbour's log of the same type. Knowing which tree a leaf belongs to is for the tree log block (research, separate).

## Being in leaves

**The view.** While the camera is inside a leaf block, its leaf texture, tinted as the leaf at that spot, frames the
screen: large tiles along the edges, fading to clear toward the centre. It eases in and out over a few ticks, and isn't
drawn in spectator mode. Client only (`LeavesView`, a HUD element drawn before vanilla's camera overlays).

**Swimming.** Swimming through leaves caps horizontal speed and drags vertical speed as walking through them does
(`LeafMovement` with the same immersion), in vanilla's fluid travel, before the move. No climbing or sinking help:
swimming already goes up and down.

## Removed

The litter landing cushion: `LeafLitter.cushionLanding`, its call in `LeafMovementMixin`,
`VersusSettings.Leaves.LITTER_CUSHION_PER_SEGMENT`, the game test `aPileOfLitterSoftensALanding`, and its lines in the
docs.

## Settings (`VersusSettings.Leaves`)

`FALLEN_LEAVES_BROWNING_CHANCE` (0.057), `DO_INSTANT_LEAF_DECAY` (true), `DO_LEAVES_VIEW` (true, client),
`DECAY_LITTER_CHANCE` (0.25, as before).

## Testing

Game tests (server):
- shedding gives the right fallen leaves (oak, spruce, a poplar), cherry gives petals, and a shed onto a brown pile adds
  a segment to it;
- natural fallen leaves brown after enough random ticks, keeping segments and facing; placed ones never do;
- drops: Silk Touch and shears give fallen leaves per segment, a bare hand gives leaf litter per segment;
- cutting a tree's log drops its crown within a few ticks; persistent leaves stay;
- every vanilla tree keeps all its leaves through an update of each;
- a husk crossing waterlogged leaves goes slower than one crossing open water.

Not testable headless: the tints and the leafy view (client).
