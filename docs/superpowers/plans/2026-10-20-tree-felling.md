# Tree Felling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cut a tree at its base and it comes down away from you, crown and all, as step 2 of the trees spec describes.

**Architecture:** Every decision is made by `Felling` from the blocks alone (Task 1, built): what a cut leaves with
nothing holding it up, which way each group falls, which leaves go with it, how much longer the cut takes. The rest
acts on its answers: the cut's time on both sides, a creak and a delayed check on the server, then vanilla falling blocks
that land as fallen logs, log items and burst leaves, hurting what they land on as an anvil does.

**Tech Stack:** Fabric, Minecraft 26.4-snapshot-3 (Mojang names), Java 25, MixinExtras, Fabric game tests.

**Spec:** `docs/superpowers/specs/2026-10-19-trees-design.md` (Step 2: felling)

## Global Constraints

- Settings in `VersusSettings.Trees`: `DO_FELLING` (true), `CUT_TIME_SCALE` (0.5), `LEAN` (1.5), `FELLING_DELAY` (10
  ticks), `LOG_DAMAGE_PER_BLOCK` (2), `MOST_LOG_DAMAGE` (10), `MOST_FALLING_BLOCKS` (512), `MOST_TREE_BLOCKS` (1024).
- Only tree blocks fall: vanilla logs never do, so trees of old chunks and everything players build stay put.
- A falling tree never takes the last half heart from a player or from an animal with an owner.
- Verification is CI's: `build` (JUnit) and `gametest` (Fabric game tests); no local Minecraft.

## Review Focus

- A cut that splits a tree in two (a fork cut at the join): each part falls, or stands, on its own.
- A tree whose branch rests on another tree's: it stays hung until that branch is cut.
- Trees at chunk edges: a group reaching an unloaded chunk counts as standing, on both sides.
- Explosions, fire and pistons removing a tree block: the same check, with no cutter (heavy side, else random).
- A player standing on the tree they cut: nothing to fall away from straight below; they ride or drop with it.

---

### Task 1: The felling model (done)

**Files:** `mod/environment/blocks/trees/Felling.java`; test `FellingGameTest`.

**Produces:** `Felling.afterCut(LevelReader, BlockPos cut, @Nullable Vec3 cutter, RandomSource) -> List<Fall>`,
`Fall(Set<BlockPos> blocks, Set<BlockPos> crown, Direction direction)`, `Felling.cutTimeFactor(LevelReader, BlockPos)`.

Groups are tree blocks (and creaking hearts) joined by faces, edges or corners; a group stands while one of its blocks
rests on a sturdy top face outside it (never leaves). The crown is each leaf nearer the group than any log left
standing, through leaves of its kind, within six steps. Tests: a trunk cut fells everything above it, corner-joined
branches and heart included; vanilla logs never fall; a branch cut at its join falls alone, and stays with a block
under it; a felled tree takes only the leaves nearer to it; the cut factor is 1 + 0.5 sqrt(n); a balanced tree falls
away from its cutter, a leaning one to its heavy side; a group past MOST_TREE_BLOCKS stands.

### Task 2: The cut takes longer

**Files:** `TreeBlock.java` (override `getDestroyProgress`); test in `FellingGameTest`.

- [ ] Test `cuttingUnderATreeIsSlowerThanAStump`: a mock survival player with an iron axe; `getDestroyProgress` at the
  base of a six-high trunk is the stump's divided by 1 + 0.5 sqrt(5), within 1e-6.
- [ ] Implement: `super.getDestroyProgress(...) / Felling.cutTimeFactor(level, pos)` when `DO_FELLING` and the getter is a
  `LevelReader` (the client's level and the server's both are), so both sides break the block on the same tick.

### Task 3: The creak and the check

**Files:** `TreeBlock.java` (`affectNeighborsAfterRemoval`, or whatever 26.4 calls on removal: probe first),
`TreeFelling.java` (server: pending checks per level), `VersusSettings.Trees` (`DO_FELLING`, `FELLING_DELAY`).

- [ ] Test `aCutTreeCreaksThenFalls`: break a trunk's foot with a mock player; the tree blocks above are still there
  after 5 ticks and gone after `FELLING_DELAY` + 2.
- [ ] Implement: on removal, when `Felling.afterCut` finds anything, play a creak (`SoundEvents.CREAKING_HEART_HURT`-ish,
  pitch low) and schedule a check `FELLING_DELAY` ticks later that calls `afterCut` again (the world may have changed)
  and hands each `Fall` to Task 4. The cutter is the breaking player, else null.

### Task 4: The flight

**Files:** `TreeFelling.java`; test in `FellingGameTest`.

- [ ] Test `anOakCutFromTheWestFallsEast`: a grown oak (`TreeFeatures.OAK`, seed 42) cut at its foot by a player west
  of it: after 60 ticks no tree block is left above the stump, and every log lies east of it, along x.
- [ ] Implement: each block of the fall, tree blocks first, then crown leaves, up to `MOST_FALLING_BLOCKS`, becomes a
  `FallingBlockEntity` where it stands: tree blocks as their vanilla log turned to lie along the fall, leaves as
  themselves; given horizontal speed in the fall's direction growing with height so a block h above the cut lands
  about h out (tune in the test). The rest break where they stand, a leaf as if it decayed (`LeafLitter.decay`).

### Task 5: The landing

**Files:** `TreeFelling.java`, a mixin on `FallingBlockEntity` landing (probe the 26.4 method), `LeafLitter`.

- [ ] Tests: `aFallenTrunkLiesOneBlockThick` (the oak's trunk lands as one row of logs along x; its other blocks are log
  items), `aFelledCrownBurstsIntoFallenLeaves` (fallen leaves and saplings or sticks lie past the log's end).
- [ ] Implement: falling blocks from a felled tree carry a mark (an entity tag or attachment). A trunk block lands as its
  log; on another fallen log or with no room, it drops as an item. Every other tree block drops as an item. A leaf
  bursts as if it decayed there: its shed settles below (`LeafLitter`), its drops fall.

### Task 6: The damage

**Files:** the landing mixin; test in `FellingGameTest`.

- [ ] Tests: `aFallingTreeHurtsWhatItLandsOn` (a husk where the oak lands loses health), `aFallingTreeNeverKillsAPlayerOrAPet`
  (a mock player and a tamed wolf at 1 health where it lands live).
- [ ] Implement: a falling log hurts as an anvil, `LOG_DAMAGE_PER_BLOCK` per block fallen after the first, at most
  `MOST_LOG_DAMAGE`, as falling-block damage (helmets soften it); clamp so players and owned animals keep half a heart.

### Task 7: What a tree carries

**Files:** `TreeFelling.java`; test in `FellingGameTest`.

- [ ] Test `aFelledTreesBeeNestBreaksAndItsBeesComeOut`.
- [ ] Implement: a creaking heart in the trunk and a bee nest on it break as if mined, the bees angry at the cutter; vines,
  cocoa and hanging moss drop as vanilla's do when their support goes.
