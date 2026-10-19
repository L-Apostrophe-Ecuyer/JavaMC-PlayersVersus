# Trees: a tree block and felling (research)

A spike, not a design: what other games and mods do, what a mushroom-style "tree" block would take in 26.4, how it
could settle which leaves belong to which tree, and a recommendation with the questions it leaves open.

## The ask

- Trees made of their own block, one per kind of tree, like a huge mushroom block: a face shows stripped wood where it
  touches other logs or the ground, and bark everywhere else.
- Some kind of fancy tree chopping.
- From the leaf decay work: leaves go by distance to the nearest log, so the leaves of tree A can live on tree B's log.
  How could the tree block make that smarter?

## What others do

**Whole tree at once.** [FallingTree](https://modrinth.com/project/Qkzu4Sjj) breaks every connected log when one is
broken (modes: instantaneous, shift down, fall as items, fall as blocks; sneak to break one block). Hytale fells the
whole tree from its bottom log, and Vintage Story's axe takes the whole tree in one long action, costing the axe the
same durability as each block would and capped by what durability is left; clearing the leaves first makes it faster
and drops more seeds and sticks ([Vintage Story wiki: Axe](https://wiki.vintagestory.at/index.php/Axe/en)).

**Chops that add up.** [HT's TreeChop](https://www.curseforge.com/minecraft/mc-mods/treechop) counts chops on any log
of a tree; once there are enough, scaled to the tree's size (logarithmic by default: 10 logs take 7 chops, 100 take
25), the whole tree comes down. Leaves then decay as vanilla. [Treeliable](https://modrinth.com/mod/treeliable), a fork,
adds a Hytale-like mode: chopping through one layer drops everything above it.
[ChopChopChop](https://www.modpackindex.com/mod/160057/chopchopchop) asks for logs + 1 chops, up to 25.

**Trees that fall over.** [Dynamic Trees](https://modrinth.com/mod/dynamictrees) builds trees from branch blocks of
varying thickness that grow from rooty dirt; thicker branches take longer to cut, cutting the base fells the tree,
which tips over with an animation, breaks leaves as it lands and can hurt what it lands on.
[PeterWolf's Realistic Tree Felling](https://www.curseforge.com/minecraft/mc-mods/peterwolfs-realistic-tree-felling)
turns the tree into one server-driven falling entity that clients only draw, falling away from the player.
[Dynamic Falling Tree](https://www.curseforge.com/minecraft/mc-mods/dynamic-falling-tree) flood-fills logs, leaves and
attached blocks from the broken log, checks it's a tree (a root on dirt), swaps the blocks out without a flicker and
hands them to a rigid-body physics library. The [Physical Falling Trees](https://www.planetminecraft.com/data-pack/physical-falling-trees/)
data pack tips a tree toward its heavier side, or toward the player if balanced, and hurts by falling speed.

**Telling trees from houses.** Felling a log cabin is the classic failure. Mods track placed logs
([TreeChopper](https://modrinth.com/project/PrzrL2lp), [TreeHarvester](https://modrinth.com/plugin/treeharvester)),
which misses anything placed before the mod or by other mods, or check the shape ([TreeQoL](https://modrinth.com/mod/treeqol):
log count, horizontal spread, at least 20% leaves), which misfires on leafy builds.
[Knots & Rings](https://modrinth.com/mod/knots-and-rings) keeps placed logs apart from natural trees and treats a trunk
as alive while it keeps its leaves.

## The tree block

A huge mushroom block has six boolean faces; a face turns to pores when the same block is placed against it and never
turns back, so breaking one of two mushroom blocks shows the pores of the other. A tree block works the same way with
the faces inverted to the ask:

- Six faces plus vanilla's log axis: a face against a tree block, log or the ground is cut wood; any other face is
  bark. Cut wood is the stripped side along the axis and the rings across it, from vanilla's own four log textures
  (`oak_log`, `oak_log_top`, `stripped_oak_log`, `stripped_oak_log_top`), so no new art. 3 axes x 64 faces = 192
  states per wood, a multipart model as the mushroom's.
- Faces only ever turn to cut wood, as the mushroom's turn to pores: chop out a block and the blocks around it show
  the wound, the pale stripped wood or the rings of a cut trunk. That's the look the ask describes, for free.
- Worldgen and saplings grow it: every tree configured feature takes a trunk provider, so either a data override per
  tree or a mixin where the tree feature sets its trunk blocks swaps each log for its tree block. The mixin survives
  version bumps better; this codebase already knows what vanilla-namespace data overrides cost.
- Breaking it drops the vanilla log. Players never hold a tree block, so **anything a player builds is logs, and
  anything made of tree blocks is a tree**: the cabin problem goes away without tracking or guessing.
- One block per wood: oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry, pale oak, and whatever 26.4's
  poplars grow from. Huge fungi's stems could follow later.

## Which tree a leaf belongs to

Leaves today live within reach of any log of their kind (LeafReach). With tree blocks:

1. **Leaves live by tree blocks, not logs.** Leaves left on a cabin's log beams decay; a leaf placed by a player is
   persistent anyway.
2. **A tree is a connected group of tree blocks standing on the ground.** Cut through its trunk and the group above
   the cut is a felled tree. Its blocks go (falling, see below), and our instant decay drops every leaf they kept alive
   in the same moment.
3. **A leaf goes with the tree it's closest to.** At the moment a tree is felled, walk out from its blocks through the
   leaves within reach and take each leaf that is nearer this tree than any other tree block: those fall with it, the
   rest stay with the neighbour they actually touch. It's one bounded flood fill per felling, not a per-leaf owner
   stored in the world.

That settles tree A and tree B for any two trees; what stays shared is a leaf truly between two trunks, which may as
well stay.

## Felling

Players Versus wants "less frustration, more fear, more fun". The recommendation is **notch and fall**:

- **Notching.** An axe on a tree's trunk takes a few chops per trunk block of the cut, more for thick trunks, as
  TreeChop scales chops. Each chop shows a deeper notch facing the chopper (a property on the tree block, its model
  and collision thinning from that side). Sneaking breaks a single block as now.
- **The fall.** When the cut goes through, everything above it tips away from the chopper, or toward its heavy side,
  with a creak first as a warning. It falls as one entity drawing the tree's blocks (as PeterWolf's), or as block
  display entities with interpolated transforms, which vanilla clients animate smoothly with no custom renderer.
  Whatever it lands on gets hurt.
- **Where it lands.** The trunk lies down as a fallen tree (26.4 already has fallen-tree features: a log on its side
  with a stump, `FALLEN_OAK_TREE` and the others), to be cut into logs; the crown shatters into fallen leaves, sticks
  and saplings. The stump stays.
- **Instant felling, Vintage Story style,** is the fallback if animation proves too costly: the same cut, the same
  crown capture, the logs dropped at the stump.

## Open questions

1. Should a cut face stay cut once uncovered (the mushroom's way, showing the wound), or go back to bark?
2. Only new trees (new chunks and saplings), or also convert old ones (a natural log with natural leaves becomes a tree
   block when touched)? Old worlds would otherwise keep vanilla logs.
3. Should anyone ever hold a tree block (creative, Silk Touch on an axe)? Holding one lets builders make "trees" that
   fell.
4. Notch and fall, chops that add up, or instant?
5. Does a felled trunk stay in the world as a fallen log, or drop straight to items?
6. How hard should a falling tree hit?

## Next

A spec for the tree block alone (faces, worldgen swap, drops, leaves bound to tree blocks), then felling as its own spec
once the questions above are answered.
