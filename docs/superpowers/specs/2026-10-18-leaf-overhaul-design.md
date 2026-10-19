# Leaves: moving, climbing and falling through them, and their litter

Minecraft 26.4-snapshot-3 (Mojang names), `26.4---Winter-Drop`. Replaces the leaf movement in `LeavesMixin`, reworks
`LeavesRustling`, and ties leaf litter to it.

## What it should feel like

Leaves are like scaffolding you can push through. You can walk into a bush or a canopy, and it slows you a bit, more the
deeper you are. With leaves at your feet you climb up holding jump and sink down holding sneak, and you can stand on any
leaf you're above. Falling onto leaves slows you a bit per leaf, takes some of the fall away, and a thick enough canopy
catches you. Leaves rustle and shed as anything pushes through them; knocked-about and dying leaves drop litter to the
ground, and litter softens a landing.

## What was wrong (the code before this)

- **The collision shape changed the entity.** `getCollisionShape` scaled the entity's velocity by 0.95 and rewrote its
  fall distance every time it was asked. That happens many times a tick, on both sides, for every leaf near the entity,
  so the slowing was random. It also read the velocity, which the server doesn't know for a player (the client
  simulates players). The server could find the leaf solid where the client went through it, and pull the player back
  on top. That is the "bob on top".
- **`makeStuckInBlock` zeroed all momentum every tick** inside leaves (cobweb style). Falling into leaves stopped dead
  ("instantly cancel momentum") and reset the fall distance. Walking became ~1.2 blocks/s, not "a bit" slower. Holding
  jump skipped the slowdown entirely (the jump branch has none), which is how leaves are usually crossed. Only the
  block at the feet or the one above them counted, not how much of the body was in leaves.
- **Climbing worked from the block above the feet.** The jump branch counted a leaf at head height. It relaunched the
  entity at 0.33 whenever its vertical speed was about −0.078, exactly the speed right after the stuck multiplier zeroed
  it. Holding jump under or in leaves therefore bobbed up and down.
- **Any leaf below an entity was solid once it fell slower than 0.4 blocks/tick**, so every layer of a canopy became a
  floor mid-fall.
- **`#fall_damage_resetting` held `#minecraft:leaves`.** Crossing any leaf at 1 block/tick or more (a fall of about 8
  blocks) zeroed the fall distance. Faster falls through one leaf were cushioned more than slower ones.

## The rules

**Who goes through.** Leaf-passers are living things other than animals and spiders. For everything else (animals,
spiders, items, projectiles, falling blocks, vehicles) leaves stay solid blocks, as before. A passer that rides
something uses its mount's rule.

**When a leaf is solid for a passer.** When its feet are above the leaf's top, it isn't sneaking, it isn't gliding, and
its fall distance is at most 2.5 (vanilla powder snow's threshold). Otherwise the leaf is empty. Block placement always
sees leaves as solid, so leaves can't be placed into anyone. On the server, players see every leaf as empty: their own
client simulates their movement, so the server never rejects or pulls back a move through leaves. All of this depends
only on state both sides share (position, sneaking, gliding, fall distance), never on velocity, and asking it changes
nothing.

**Immersion** is how much of the passer is inside leaves that are empty for it, from 0 to 1. Several leaf blocks
don't add up separately: it is the share of the body in leaves, however many blocks that takes. It is measured two
ways. For the horizontal slowdown, the lower half of the bounding box counts three times the upper half, so brushing a
canopy with your head is mild and wading through a waist-high bush is not. For vertical drag it is the plain share of
the box swept through the tick's motion, so a fast fall can't skip a thin layer between two ticks.

**A foothold** is a leaf, empty for the passer, at its feet (its block position). Climbing and controlled descent need
one, so leaves at head height or higher can't be climbed.

**Slowdown (horizontal).** Horizontal speed is capped, and the cap tightens with immersion: 0.32 blocks/tick when
barely touching (above sprinting, 0.28, so no effect), down to 0.12 fully inside. Above the cap, at most 35% of the
speed goes per tick, so pushing in slows you over two or three ticks instead of stopping you. Momentum is kept, and
anything under the cap (sneaking) isn't slowed at all.

| where | immersion | cap | walking (0.216) | sprinting (0.28) |
|---|---|---|---|---|
| head brushing a canopy | 0.22 | 0.276 | unchanged | −1% |
| waist-high bush | 0.78 | 0.164 | −24% | −41% |
| fully inside | 1.0 | 0.12 | −44% | −57% |

**Vertical.** Vertical speed, up or down, loses 30% × immersion per tick. Then, with a foothold and a fall no faster
than 0.3 blocks/tick (the *catch speed*):
- *Climb*: jump held. You rise at least 0.1 blocks/tick (a ladder is about 0.12); faster upward motion, like the jump
  that started it, is only dragged. Fall distance resets while climbing.
- *Descend*: sneak held. The leaves under you open (you're descending), and you sink at no more than 0.15 blocks/tick
  (scaffolding's speed).
- A faster fall isn't grabbed at once, holding jump or sneak or not: the drag slows it to the catch speed first.
- *Resting*: with no input you settle onto the leaf top below you (it's solid, you're above it), so you can stand on any
  layer of a canopy, as in scaffolding.
- *Walking mobs* don't hop through leaves. Vanilla's `MoveControl` jumps a mob whose feet are inside a block shorter
  than its step, asking the block's shape with no entity, which for leaves is a full block. It asks for the mob's own
  shape instead. No-context queries otherwise still see leaves as full blocks (block state caches, falling-leaf
  particles, pathfinding).

**Falling.**
- From 2.5 blocks or less, you land on top of leaves. Leaves take the fall distance down to a third, as before.
- From higher you break in. The drag bleeds your speed, and while you're in leaves and still falling, your fall
  distance can't exceed the free-fall height that gives your current speed, h(v) for v_n = 3.92 (1 − 0.98^n). Landing
  damage then follows how fast you were going, not where you started. Once you're slow enough (about 0.5 blocks/tick,
  2.5 blocks' worth), the next leaf top below you holds and catches you.
- The cap runs where vanilla counts the fall, `LivingEntity.checkFallDamage`, with the tick's vertical movement. It
  runs on both sides: the server checks a player's fall with the movement its client reports, so the damage it deals
  matches what the client simulated. The landing tick itself isn't capped, since that movement is cut short by the
  ground, so a bush can't erase a fast fall.
- Leaves leave `#fall_damage_resetting`.
- Gliders pass through leaves and lose 30% × immersion of their speed on every axis per tick.

Simulated with vanilla gravity and drag, 30% drag, canopy bottom 4 blocks above stone. Each cell is damage through the
canopy / damage from the same height onto bare stone, where 0* means the canopy caught the fall (it landed on an inner
leaf). Columns are canopy thickness in leaf layers:

| drop above the canopy | 1 layer | 2 layers | 3 layers | 4 layers |
|---|---|---|---|---|
| 5 | 3/7 | 1/8 | 0/9 | 0*/10 |
| 8 | 5/10 | 2/11 | 0/12 | 0*/13 |
| 12 | 9/14 | 3/15 | 1/16 | 0/17 |
| 16 | 11/18 | 6/19 | 2/20 | 0/21 |
| 24 | 17/26 | 9/27 | 4/28 | 1/29 |
| 32 | 27/34 | 14/35 | 6/36 | 2/37 |
| 48 | 39/50 | 22/51 | 12/52 | 6/53 |

**Rustling (client).** It runs for every leaf-passer within 32 blocks of you, not only players, and uses the same
immersion as the movement. Entering leaves gives a burst scaled by speed: a crash from a fall bursts and rustles
loudest. Moving inside sheds the leaves' own falling leaves (biome-tinted where vanilla tints them), more the faster
and deeper you go, and rustles every 0.9 blocks moved. Sneaking halves it all.

## Leaf litter

Since [fallen leaves](2026-10-19-fallen-leaves-design.md): leaves shed fallen leaves of their own colour (cherry still
sheds petals), which brown into this litter; spruce and pale oak shed too; leaves cut off from their tree fall at once;
and litter no longer softens a landing. The rest of this section still holds.

- **Knocked-about leaves shed.** On the server, every natural (not player-placed) leaf a passer moves through has a
  chance per tick of dropping a segment of litter below it, `0.1 × (speed − 0.1)²` and at most 25%. Walking through a
  hedge drops one every 15 seconds or so; crashing through a canopy drops a handful. Speed is measured as vanilla's
  sweet berry bush measures it: a player's is the movement its client sent, anything else's how far it moved that tick.
  (The server applies a player's block effects during the player's own tick, after its old position has caught up with
  it, so how far it moved would always be nothing.)
- **Dying leaves leave litter.** A decaying leaf has a 25% chance to drop a segment as it goes, so a felled tree leaves
  a carpet under it.
- **Where it lands.** Straight down through the canopy, to the first air with ground the litter can stay on (vanilla's
  survival check), within 24 blocks. It adds a segment to litter already there (up to 4), and never replaces a plant,
  lands in water or rests on leaves.
- **Which leaves.** (Superseded: every leaf type, see fallen leaves.) Tag `players-versus:sheds_leaf_litter`: oak,
  birch, dark oak, jungle, acacia, mangrove, azaleas, the poplars. Cherry leaves drop pink petals instead
  (`players-versus:sheds_pink_petals`). Spruce and pale oak shed nothing.
- **Softer landings.** (Removed.) Landing in litter took 10% off the fall distance per segment.

## Code

- `mod/environment/blocks/leaves/LeafMovement`: who passes, when a leaf is solid, immersion and foothold, the movement
  adjustment, the fall cushion. The math (speed cap, drag, equivalent fall height) is static and pure.
- `mod/environment/blocks/leaves/LeafLitter`: shedding, landing the litter, the cushion of a pile.
- `mixin/environment/blocks/LeavesMixin`: collision shape (no side effects), landing on top, shedding in
  `entityInside`, decay litter in `randomTick`. Distance and decay rules unchanged.
- `mixin/environment/blocks/LeafMovementMixin` (on `LivingEntity`): `handleOnClimbable`'s result (the motion vanilla
  is about to move with, after input) through `LeafMovement.adjust`; `updateFallFlyingMovement`'s result through the
  glide drag; `checkFallDamage` at its head through the fall cushion. The server counts a player's fall through
  `Entity.doCheckFallDamage` → `checkFallDamage` with the movement its client sent, so the cushion covers players there.
- `mixin/environment/blocks/LeavesMoveControlMixin`: the mob's own leaf shape in `MoveControl.tick`'s jump check.
- `client/.../LeavesRustling`: on top of `LeafMovement`.
- `VersusSettings.Leaves`: the tunables. Data: the two tags; leaves out of `#fall_damage_resetting`.

## Testing

- JUnit `LeafMovementTest`: the equivalent fall height against a step-by-step vanilla fall, the speed cap, the body
  weighting.
- Game tests `LeavesGameTest` (server, husks so nothing burns): a hedge slows a walk but doesn't stop it; leaves at the
  feet climb, leaves just above the head don't; a fall onto a canopy hurts much less than onto stone, and one layer
  only a bit less; a small drop lands on top; a cow stands on leaves; a sneaking husk sinks into them; litter falls
  through the canopy to the ground and stacks; decaying leaves shed litter; moving fast through natural leaves knocks
  litter loose and keeping still doesn't, for a mob and for a player (a Fabric fake player with the movement its client
  would have sent); landing on a pile of litter hurts less.
- Not testable headless: the rustling (client) and the player's own client-side feel. These follow the same rules as
  the server-simulated mobs the game tests cover.
