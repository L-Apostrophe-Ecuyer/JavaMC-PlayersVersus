# Trees: the tree block and felling

Minecraft 26.4-snapshot-3 (Mojang names), `26.4---Winter-Drop`. Builds on
[fallen leaves and instant decay](2026-10-19-fallen-leaves-design.md) and settles the open questions of
[the trees research](2026-10-19-trees-research.md). Ships in steps, each a working build: the tree block, then
felling, then the pass that makes both as good as they can be. Step 1 is built (`TreeBlock`, `TreeBlocks`,
`TreeFeatureMixin`, `tools/tree-blocks.py`, `TreesGameTest`). Step 2 is built too (`Felling`, `TreeFelling`,
`FelledTreeLandingMixin`, `FelledTreeDamageMixin`, `FellingGameTest`; plan
`docs/superpowers/plans/2026-10-20-tree-felling.md`). Step 3 is built (`Topple`, `TopplePayload`, `TreeRoots`,
`TreeDigMixin`, `FallenTreeFeatureMixin`, `ClientTopples` and its render mixins, `ToppleTest`, `TreeGrowthGameTest`);
where it changes step 2, step 3 is what holds.

## What it should feel like

Trees look as they always have until you cut one. Chop at the base and the cut takes longer the bigger the tree; then
it creaks and comes down away from you, crown and all, laying a fallen log along the ground while its leaves burst into
fallen leaves, saplings and sticks where they land. Whatever it lands on gets hurt, but a falling tree never kills a
player or a pet. Logs you build with never fall, and the trees of chunks generated before this stay as they were.

## Step 1: the tree block

**Blocks.** One per wood, `players-versus:<wood>_tree` ("Oak Tree"): oak, spruce, birch, jungle, acacia, dark oak,
mangrove, cherry, pale oak and poplar (26.4 gives poplars a log of their own, `poplar_log`, in `#poplar_logs`). Each is a log (axis,
strength, sound, flammable as logs are) with six boolean faces, `north` to `down`, true for bark and false for cut, as a
huge mushroom block's are true for skin and false for pores: 3 axes x 64 = 192 states a wood.

**Looks.** Vanilla's own textures, no new art: a bark face shows the log's (`oak_log` along the axis, `oak_log_top`
across it), a cut face the stripped log's (`stripped_oak_log`, `stripped_oak_log_top`), turned as a vanilla log of that
axis turns them, so a tree nobody has cut looks exactly as vanilla. One multipart blockstate per wood (a part for each
face, axis and look), generated rather than written by hand. The item looks like the log.

**Faces are set once.** When a tree grows (below), or when a tree block is placed in creative: a face against another
tree block, a log, or a block whose face toward it is sturdy (the ground, rock, mangrove roots, a bee nest, a creaking
heart) is cut; against anything else (air, leaves, water, vines, cocoa, snow) it is bark. Nothing changes a face after
that: placing a block next to a trunk leaves its bark, and chopping out a neighbour shows the cut that was there all
along. Rotating or mirroring one in a structure turns its faces with its axis, as a mushroom block's.

**Drops.** Breaking one drops its wood's vanilla log, with Silk Touch or without. Survival players never hold a tree
block, so everything a player builds is logs, and only trees are made of tree blocks.

**Creative.** Each sits in the natural blocks tab after its wood's log. Placed, it takes its axis from the face clicked,
as a log does, and sets its faces once from its neighbours.

**A log to the game.** Each tree block joins its wood's vanilla block tag (`oak_logs`, `spruce_logs`, `birch_logs`,
`jungle_logs`, `acacia_logs`, `dark_oak_logs`, `mangrove_logs`, `cherry_logs`, `pale_oak_logs`), and through them
`logs_that_burn` and `logs`: axes mine it at log speed, fire burns it, cocoa stays on a jungle trunk, a creaking heart
works in a pale oak trunk, a growing tree may grow through it as through a log, and leaves live by it, vanilla's rule
and `LeafReach`'s own-log rule alike. The plan checks each of these against 26.4's data.

**Growing trees.** Every tree the game's tree feature places (`TreeFeature`: in new chunks, from saplings, from bone
meal) puts each log down as its wood's tree block with the same axis (a mixin where the feature places its trunk), and
once the tree and its decorations are in place, sets the faces of all its tree blocks. A log with no tree block of its
own, such as another mod's, stays as it is.

**Only new trees.** Trees in chunks generated before this keep their vanilla logs: they never fall, and their leaves
keep today's rules. Logs that aren't living trees stay vanilla too: 26.4's fallen-tree features (already fallen), the
wooden buildings of structures, and whatever players place.

**Leaves.** No change in step 1: a tree block is its wood's log to `LeafReach`, so a new tree's leaves live by it as an
old tree's live by its logs.

## Step 2: felling

**How it moves.** Vanilla-style falling blocks, as FallingTree's fall-blocks mode: the game already draws and moves
them, they hurt what they land on as anvils do, and the game tests can check where everything lands. Two other ways
were weighed: one rigid entity swinging the whole tree around its stump, the most realistic fall but drawn, collided
and damaged by code of its own that no game test can see; and no animation at all (the logs laid down along the fall
line at once), the fallback if falling blocks misbehave.

**What falls.** A group of tree blocks connected to each other by faces, edges or corners (acacia and jungle branches
grow diagonally), a creaking heart in a trunk counting as part of it, falls when none of it rests on a sturdy top face
below it outside the group: the ground, roots, rock, a vanilla log, a block a player placed. Checked whenever a tree
block is removed, whatever removed it (a player, an explosion, fire, a piston), for the tree blocks around it, up to
`MOST_TREE_BLOCKS` (1024) a group; a bigger group counts as standing. So:
- cutting a trunk fells everything above the cut, and cutting a branch drops just that branch;
- a tree whose branches touch another tree's stays hung in it until that branch is cut too;
- a block placed under a branch props it up;
- sneaking doesn't stop a fall;
- vanilla logs never fall, and leaves hold nothing up.

**The cut.** Breaking a tree block takes its usual time x (1 + `CUT_TIME_SCALE` x sqrt(n)), with n the tree blocks
that would fall if it went (`CUT_TIME_SCALE` 0.5). An oak with an iron axe (n about 5): about a second instead of half
a second; by hand, about 6 seconds; the last of a mega spruce's four columns (n about 100), about 3 seconds. The other
three columns, a stump, or the tip of a branch take the usual time. The client works n out from the same blocks the
server has, so both agree on when the block breaks.

**Which way.** Along whichever of north, south, east and west is nearest to straight away from whoever made the cut.
A tree whose weight (1 for each of its tree blocks, 1/4 for each leaf going with it) sits more than `LEAN` (1.5)
blocks off the cut, horizontally, falls toward that side instead, whoever cut it. With nobody to fall away from (an
explosion, fire, a cutter straight above or below), it falls to its heavy side, or a random side when balanced.

**The warning.** On the cut the tree creaks; `FELLING_DELAY` (10 ticks) later it checks again that nothing holds it
up, then falls.

**The crown.** The leaves that go with a felled group: walking out from its blocks through natural leaves of its kind
(`LeafReach.isOwnKind`), up to vanilla's six steps, each leaf nearer to the group than to any log or tree block of its
kind left standing. A leaf as near another tree, or nearer, stays with that tree: tree A's leaves no longer hang on
tree B. A lone tree's whole crown goes with it.

**The fall.** Every block of the group and every leaf of its crown becomes a falling block where it stands, thrown out
in the fall direction faster the higher it is, so that a block h above the cut lands about h blocks out (tuned in a
game test). Tree blocks fly as their wood's vanilla log, turned as the tree turns (a standing trunk lands lying along
the fall); leaves fly as themselves. At most `MOST_FALLING_BLOCKS` (512) fly a tree, tree blocks first: the rest
(crown leaves, all but always) break where they stand, a leaf as if it decayed. What the tree carries: a creaking heart
in its trunk and a bee nest on it break as if mined, the nest's bees coming out angry at whoever cut; vines, cocoa and
hanging moss drop off as vanilla's do when their support goes.

**Landing.**
- The trunk, every column of tree blocks going straight up from the cut, lands as vanilla logs lying along the fall:
  a fallen log, as 26.4's fallen trees. A trunk block coming down on another fallen log, or with no room where it
  lands, breaks into a log item instead, so the fallen log lies one block thick.
- Every other tree block (branches, forks, the bend of an acacia) breaks into a log item where it lands.
- A leaf bursts where it lands as if it had decayed there: its shed (fallen leaves; litter for poplars, petals for
  cherry, nothing for spruce and pale oak) settles on the ground below, as `LeafLitter` sheds, and its drops (saplings,
  sticks, apples) fall there.
- The stump stays, showing its cut.

**Damage.** A falling log hurts what it lands on as an anvil: `LOG_DAMAGE_PER_BLOCK` (2) for each block it fell after
the first, at most `MOST_LOG_DAMAGE` (10, five hearts), counted as a falling block, so a helmet softens it. Leaves don't
hurt. It never takes the last half heart from a player or from an animal with an owner (a tamed wolf, cat, parrot or
horse); anything else can die.

## Step 3: as good as it can be

**What holds a tree up.** A block holds up a tree resting on it when it's part of a tree (a tree block, a creaking
heart) or has a sturdy top face that isn't leaves (`Felling.holds`). A cut is any block a tree needs going: one of its
tree blocks, or the block its foot rests on. A tree block notices the block under it going, or turning into something
that doesn't hold it (`TreeBlock.updateShape`, in a running world only), whatever did it: dug out, blown up, burnt,
pushed by a piston, turned to falling sand. Breaking the block a tree rests on takes as long as cutting the trunk there
would (`TreeDigMixin`, on every block state's break time, for parts of trees and the blocks under them), so digging a
tree out from under is no shortcut; it comes down the same way, away from whoever dug. A player's break of either is
recorded through Fabric's `PlayerBlockBreakEvents` for which way the tree falls. Two checks at one spot (a trunk block
cut is also what the block above it rested on) are one cut; when the tree comes down `FELLING_DELAY` later, what falls
is what nothing holds up then, as the world stands, so a cut filled in meanwhile (stone, a log) holds the tree up. What
a felled tree held up (another tree's branch resting on it) is checked once it's down.

**Rooted dirt.** Once a tree has grown, the soil under the foot of its trunk (dirt, grass, podzol, mycelium or moss; not
mud or sand) turns to rooted dirt (`ROOTED_DIRT_UNDER_TREES`), as azaleas already stand on: dug into, the ground under a
tree shows it holds one. (26.4 leaves grass under a trunk, so this is done after the tree grows, not through vanilla's
below-trunk block.)

**Toppling.** The rigid swing step 2 set aside, built: a group standing over its cut (the block above the cut is in it
and the block under the cut holds) turns over as one body on a hinge, the level line across the fall at the front edge
of its lowest layer, level with that layer's bottom (`Topple`). Gravity (0.1, more than a falling block's 0.04, so it
comes down briskly) pulls its weight round the hinge as a rigid body's: angle'' = g (up sin angle + along cos angle) /
inertia, along and up the weighted sums of where each block's centre sits from the hinge (1 a tree block or a hive,
1/4 a leaf), inertia their moment round it. The cut pushes it at 0.06 radians a tick at least, or enough to lift weight
sitting behind the hinge over it with a quarter to spare. A six-block trunk comes down in under a second (18 ticks),
its top hitting the ground at about 1.3 blocks a tick; a 25-block one in about a second. Cut through its trunk with
room in front of the cut, a tree also drops onto what's under the cut as it starts to tip (by the cut's height, falling
at the same gravity, in 4 ticks), turning on the front edge of the cut's floor, so it ends lying on the ground beside
its stump, never a block over it; dug out from under (the ground all round the hole) it tips over the hole's edge
instead. Its blocks' paths are checked half a block at a time, so a fast treetop can't pass through a wall. It's let go lying down (a right angle), on any solid block that isn't leaves getting in the way of its wood
(the step undone), or if it ever tips back; each block then flies on as a vanilla falling block with the speed it had,
so it lands where its momentum takes it. Anything not standing over its cut (a branch cut off) just drops.
- Tree blocks fly as themselves, their faces as they were; leaves as themselves; hives with their bees inside.
- A turning log hurts each creature it sweeps through once, as hard as a falling log landing from as high as it would
  fall to reach its speed (`LOG_DAMAGE_PER_BLOCK` a block after the first, at most `MOST_LOG_DAMAGE`), sparing the last
  half heart of a player or a pet as before. A turning leaf about to go into the ground bursts there.
- The server and its clients step the same topple alike: the server sends the hinge, the way, the three sums, the
  angle, the speed and the falling blocks with where they started (`TopplePayload`) when it starts, and the angle when
  it lets go; in between each client turns it itself (`ClientTopples`), moving each block round the hinge each tick and
  drawing it between ticks where the tree has turned it, turned over with it about its own centre (render mixins on the
  falling block renderer), so the tree swings smoothly and looks one body. Let go, a block keeps its turn until it
  lands.
- Landing: a trunk block lands as its wood's log, turned as its tree turned over when that was half way or more
  (`TURNED_X`/`TURNED_Z` tags), unless there's a log under it or a creature where it lands; anything else as before.

**Hives.** A bee nest or beehive next to a felled tree's blocks or crown falls with it, its bees and honey inside, and
lands whole; its bees then come out angry at whoever cut the tree (the nearest player within 16 blocks when nobody
did). A hive that can't land whole breaks, its bees out angry, dropping what its loot table gives.

**Creaking heart trees go deep.** A pale oak's trunk goes on down into the ground as roots (`TreeRoots`): each of its
columns `ROOT_DEPTH_MIN` (3) to `ROOT_DEPTH_MAX` (5) blocks, through dirt, rock, gravel or clay, as far as the ground
lets it; a root branches out sideways from a column, away from the trunk, one or two blocks down and one or two long,
six times in ten, some of them showing at the surface. The creaking heart its decorator set in the trunk moves down
into a root `HEART_IN_ROOTS` (3/4) of the time, standing up, with a root above and below it and a knot of roots round
it on every other side, so it has pale oak logs all round (it keeps its creaking, and its idle sound); a tree block
takes its old place. Felling the tree leaves its roots, and the heart in them, until someone digs it out. Roots show
bark where they meet the ground (they're set by a root's rule: cut only against the tree), as they would dug out.

**Every tree.** Every tree feature (`minecraft:tree`, vanilla's and the mod's, through `TreeFeature`) and every fallen
tree (`minecraft:fallen_tree`: a stump and a log lying beside it, through `FallenTreeFeature`) grows its wood's tree
blocks; a game test grows each one the game has in turn and finds no vanilla log of a wood that has a tree block. Huge
fungi and huge mushrooms are not trees here.

## Settings (`VersusSettings.Trees`)

`DO_TREE_BLOCKS` (true), `DO_FELLING` (true), `CUT_TIME_SCALE` (0.5), `LEAN` (1.5), `FELLING_DELAY` (10 ticks),
`LOG_DAMAGE_PER_BLOCK` (2), `MOST_LOG_DAMAGE` (10), `MOST_FALLING_BLOCKS` (512), `MOST_TREE_BLOCKS` (1024),
`ROOTED_DIRT_UNDER_TREES` (true), `DO_ROOTS` (true), `ROOT_DEPTH_MIN` (3), `ROOT_DEPTH_MAX` (5), `HEART_IN_ROOTS` (0.75).

## Testing

Game tests (server), step 1:
- every vanilla tree in the groves grows its wood's tree blocks and no vanilla logs, with faces as the rule says (a
  trunk block's up and down faces cut and its sides bark, the lowest one's down face cut against the grass), and keeps
  all its leaves;
- a block placed against a tree block, or a neighbouring tree block broken, leaves its faces as they were;
- breaking a tree block drops its wood's log, with Silk Touch and without;
- a tree block is a log to the game: in `minecraft:logs` and its wood's tag, a cocoa pod stays on a jungle tree block.

Step 2:
- an oak cut at its base by a test player standing west of it falls east: no tree blocks left above the cut, logs
  lying along the x axis east of the stump, its crown gone, fallen leaves and saplings out past the log's end; cut from
  the north, it falls south;
- a fancy oak's branch cut where it joins falls alone, and a branch with a block placed under it stays;
- two oaks whose crowns touch: felling one leaves the other every leaf;
- the cut: a tree block with a tree above it breaks slower than a stump, by 1 + 0.5 sqrt(n);
- a column of vanilla logs with its bottom broken stays up;
- a husk where the tree lands is hurt; a test player and a tamed wolf at 1 health where it lands live;
- a tree with a bee nest, felled: the nest is gone and its bees are out (step 3: the nest lands whole beyond the stump
  and its bees come out angry).

Step 3:
- the topple (JUnit, `ToppleTest`): it starts where the tree stood; a six-block trunk comes down in 8 to 30 ticks,
  never stopping, and lies along its way on the ground beyond its foot, its top coming down fast; a tree cut through its
  trunk has settled onto its stump within 5 ticks and lies on the ground; a block moves each tick as far as its speed
  says; a tree leaning back a little still goes over; a client's topple turns and settles exactly as the server's; a
  sweep hits as an anvil falling to its speed would;
- digging out the dirt under a six-block trunk takes 1 + 0.5 sqrt(6) times as long as bare dirt, and fells it east of
  the hole, lying along x on the ground, when dug from the west;
- a cut filled with cobblestone before the tree comes down holds it up;
- partway down, a felled trunk's falling blocks are as far apart as they stood, its top leaning out the way it falls;
- a felled tree's heart breaks, and its bee nest with two bees lands whole beyond the stump, both bees out and angry;
- a grown oak stands on rooted dirt (grown on grass);
- a grown pale oak's four trunk columns go at least `ROOT_DEPTH_MIN` deep; a creaking heart in a pale oak's trunk goes
  down into a knot of roots with pale oak logs on all six sides, a tree block back in its place, the roots showing bark
  against the ground;
- every tree and fallen tree feature the game has grows tree blocks and no vanilla log of their woods.

Not testable headless: the faces' textures and how the fall looks (the client's turning and drawing).

## Later

Converting old trees; huge fungi stems and huge mushrooms as trees; stripping a tree block in place with an axe (26.4
moved stripping into axes' item components, `BlockTransformers`).
