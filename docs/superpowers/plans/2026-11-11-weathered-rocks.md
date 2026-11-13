# Weathered Rocks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the rock model from the approved throwaway prototype: rocks made of a few big angular blocks, light painted in from above in tuff, stone, andesite and clay, moss and grass on the tops.

**Architecture:** Four new focused classes in `worldgen/rocks`: `RockDice` (numbers from a seed), `RockBlock` (one angular block: planes, distance), `RockVoxels` (the blocks voxelized: blending, the flared foot, seams, clean-up) and `RockLight` (the tones). `RockShape` becomes recipes of blocks, `RockPlan.Body` a list of blocks, and `RockModel` keeps its public surface (used by the stamper, the formation pieces and the gallery) over new insides. Everything outside the model keeps working throughout: each task leaves the build and the game tests green.

**Tech Stack:** Java 25, Fabric (Minecraft 26.4-snapshot-4), JUnit 5, Fabric game tests, Gradle.

**Spec:** `docs/superpowers/specs/2026-11-11-weathered-rocks-design.md`. The prototype it was approved from (throwaway, for reference only): `docs/superpowers/specs/weathered-rocks-prototype/Weathered.java.txt`.

## Global Constraints

- The rock model stays pure Java: no `net.minecraft` imports in `RockDice`, `RockBlock`, `RockVoxels`, `RockLight`, `RockShape`, `RockPlan`, `RockModel`, `RockStamper`, `RockTemplate`, `RockCanvas`, `RockMaterial`, `RockPalette`.
- Tones: `SHADE` tuff, `BASE` stone, `LIGHT` andesite, `BRIGHT` clay. No deepslate anywhere.
- A rock stamped a chunk at a time, in any order, is the rock stamped whole (the stamper reads each column's ground once, before writing it).
- A rock feature reaches at most `RockFeature.MAX_REACH` = 16 blocks from its origin.
- No noise on faces: the only noises left are the tone patches and the cover patches.
- Template keys: bodies `shape`, `count`, `distance`, `angle`, `height`, `radius`, `cover`; palette `shade`, `base`, `light`, `bright`, `soil`, `grass`, `moss`, `plants`.
- Commits end with `Co-Authored-By: Claude <noreply@anthropic.com>` and `Claude-Session: https://claude.ai/code/session_01WnvnLbPhXLdxHLdhxgmNXF`; no model names.
- Unit tests: `./gradlew test --tests '*Rock*'`. Game tests: `rm -f build/run/gameTest/report.xml && ./gradlew runGameTest -Ppv.acceptEula=true`, then read the fresh report (the blight test is a known failure).

## Review Focus

- The smallest size of every shape must still make a rock (a template may ask for the bottom of a range): Task 5, `theSmallestOfEveryShapeIsStillARock`.
- A template may ask for a size above the shape's range (`great_boulders` asks boulders 12 to 18 tall): the rock must stay within the reach the template reports, Task 5, `aRockLargerThanItsShapesRangeStaysWithinItsReach`.
- A rock at a cliff edge: its foot is never filled more than `RockStamper.MAX_FILL` (24) below its anchor, Task 5, `aFootIsNeverFilledMoreThanItsMost`.
- The worldgen threads read one formation's model at once: queries must not touch any state the build used as scratch, Task 5, `readingTheModelFromManyThreadsGivesTheSameRock`.
- A big formation is built on a worldgen thread: Task 5, `aBigFormationIsBuiltQuickly` (300 ms).

---

### Task 1: Angular blocks

**Files:**
- Create: `src/main/java/frootloops/versus/mod/environment/worldgen/rocks/RockDice.java`
- Create: `src/main/java/frootloops/versus/mod/environment/worldgen/rocks/RockBlock.java`
- Modify: `src/main/java/frootloops/versus/mod/environment/worldgen/rocks/RockShape.java` (its private `Dice` becomes `RockDice`)
- Test: `src/test/java/frootloops/versus/mod/environment/worldgen/rocks/RockBlockTest.java`

**Interfaces:**
- Produces: `final class RockDice` (package-private): `RockDice(long seed)`, `long next()`, `double unit()`, `double between(double from, double to)`, `int whole(int from, int to)` (both inclusive), `double angle()`, `boolean chance(double chance)`; each draw is `RockNoise.hash(seed, ++rolls)`, as `RockShape.Dice` does now.
- Produces: `public final class RockBlock`:
  - `static final double SOFT = 0.4` (how much the planes' soft maximum rounds an edge, in blocks)
  - `RockBlock(double cx, double cy, double cz, double[] planes, double[] orientation, int group)`: `planes` flat `{nx, ny, nz, d, ...}` in the block's own frame, unit normals; `orientation` a row-major 3x3 whose columns are the block's axes in the world; `group` -1 or a column's number
  - `static RockBlock standing(double x, double z, double tall, double sink, double[] planes, double yaw, double tipToward, double tip, int group)`: the block spans y from `-sink` to `tall` at (x, z), turned by `yaw`, then tipped by `tip` radians toward the level direction `tipToward` about its foot (x, -sink, z)
  - `double distance(double x, double y, double z)`: negative inside; the soft maximum `max + SOFT * ln(sum(exp((v_i - max) / SOFT)))` of the plane values `v_i = n_i . local - d_i`
  - `RockBlock turned(double[] turn, double px, double py, double pz)`
  - `double cx()`, `cy()`, `cz()`, `double reach()` (its half diagonal plus 2), `int group()`
  - `static double[] planes(RockDice dice, double a, double b, double c, int cuts, double depth, double crown, double skew, boolean upright, int bends)`
  - `static double[] level(double yaw)`: about the vertical, local x to (cos yaw, 0, sin yaw), local z to (-sin yaw, 0, cos yaw)
  - `static double[] tip(double toward, double angle)`: the turn about the level axis square to `toward` that tips +y toward (cos toward, 0, sin toward)
  - `static double[] multiply(double[] a, double[] b)`

- [ ] **Step 1: Write the failing tests** in `RockBlockTest`:

```java
@Test void aBoxIsItsHalfSizesAcross() {
    RockBlock box = new RockBlock(0, 0, 0, RockBlock.planes(new RockDice(1), 3, 2, 1, 0, 0.7, 0, 0, false, 0), RockBlock.level(0), -1);
    assertTrue(box.distance(0, 0, 0) < -0.5 && box.distance(0, 0, 0) > -1.1);
    assertTrue(box.distance(2.5, 0, 0) < 0 && box.distance(3.5, 0, 0) > 0);
    assertTrue(box.distance(0, 1.5, 0) < 0 && box.distance(0, 2.5, 0) > 0);
}
@Test void cutsTakeOffEdgesAndCorners()      // a = b = c = 4: 8 cuts at depth 0.6 leave the centre inside and at least 10% fewer of the 1-block grid's cells inside than no cuts
@Test void aBentFaceLeansBackAboveItsLine()  // a = c = 3, b = 6, 3 bends: the widest inside |x| or |z| at y = 5.5 is less than at y = -5
@Test void aStandingBlockStandsOnItsFoot()   // standing(0, 0, 6, 1.5, box 2 x 3.75 x 2, 0, 0, 0, -1): inside at (0, -1.2, 0) and (0, 5.8, 0), outside at (0, 6.5, 0); tipped 0.3 toward +x: (0.9 * 6 * sin 0.3, 0.9 * 6 * cos 0.3, 0) inside, foot (0, -1.2, 0) still inside
@Test void turningKeepsTheShape()            // for 100 points: turned(R, pivot).distance(R(p - pivot) + pivot) == distance(p) within 1e-9
@Test void theSameSeedGivesTheSameNumbers()  // RockDice(5) twice: equal draws; RockDice(6): different
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew test --tests '*RockBlockTest*'`
Expected: compilation fails, `RockBlock` and `RockDice` don't exist.

- [ ] **Step 3: Implement `RockDice` and `RockBlock`**, the planes as in the spec's "A block" and "Big faces" rows:
  - six faces, each normal tipped off its axis by up to `skew` on the two other axes, at `half * |n_axis|`;
  - `cuts` planes across an edge or a corner: signs random, `sy = +1` with chance `0.5 + 0.35 * crown`, components `between(0.35, 1) / half` on each axis, then one dropped: with `upright` the y component with chance 0.7, otherwise x (0.25), z (0.25) or y (0.15); offset `support * max(0.35, between(depth, depth + 0.15) - (ny > 0.3 ? 0.25 * crown : 0))` with `support = a|nx| + b|ny| + c|nz|`;
  - `bends` planes: on an x face with chance `c / (a + c)`, else a z face, sign random, `lean = between(0.12, 0.32)`, `at = between(0.2, 0.6)`, normal `(±cos lean on the face's axis, sin lean, 0)`, offset `cos(lean) * half + sin(lean) * (-b + 2 * b * at)`.
  Switch `RockShape` to `RockDice` (delete its nested `Dice`).

- [ ] **Step 4: Run them to see them pass**

Run: `./gradlew test --tests '*Rock*'`
Expected: all pass, the 44 earlier rock tests among them.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/frootloops/versus/mod/environment/worldgen/rocks/RockDice.java src/main/java/frootloops/versus/mod/environment/worldgen/rocks/RockBlock.java src/main/java/frootloops/versus/mod/environment/worldgen/rocks/RockShape.java src/test/java/frootloops/versus/mod/environment/worldgen/rocks/RockBlockTest.java
git commit   # "Rocks: angular blocks (planes cut across edges and corners, bent faces)" + trailers
```

### Task 2: Clay in, slabs, vines and loose stones out

**Files:**
- Modify: `rocks/RockMaterial.java` (`BRIGHT` after `LIGHT`; `isRock()` is `ordinal() <= BRIGHT.ordinal()`)
- Modify: `rocks/RockPalette.java` (`record RockPalette(Map<RockMaterial, String> blocks, List<String> plants)`; `GRAY` gains `BRIGHT` -> `minecraft:clay`; `slabs`, `vine` and `slab()` go)
- Modify: `rocks/RockCanvas.java` (`void rock(int x, int y, int z, RockMaterial material)`; `vine` goes)
- Modify: `rocks/LevelRockCanvas.java`, `rocks/RockStamper.java` (no slabs; only `PLANT` decorations are placed), `rocks/RockTemplate.java` (the keys in Global Constraints; `Body.with(...)` calls become `withCover`), `rocks/RockPlan.java` (`Body.with(...)` goes, `withCover` stays)
- Modify tests: `RockTestCanvas.java`, `RockPreview.java` (its `Scene`), `RockModelTest.java` (drop `aPileHasSlabsOnBareRock`; the allowed cells in `aRockIsThreeTonesOfRockUnderItsPlants` lose `slab:` and `vine:`), `RockTemplateTest.java`, `src/gametest/java/frootloops/versus/gametest/RocksGameTest.java` (`isRock`: tuff, stone, andesite, clay)

**Interfaces:**
- Produces: `RockMaterial.BRIGHT`; `RockPalette.block(RockMaterial)`; `RockCanvas.rock(int, int, int, RockMaterial)`.

- [ ] **Step 1: Write the failing tests** in `RockTemplateTest`:

```java
@Test void theOldKeysAreGone() {
    for (String key : List.of("vines", "plants", "scree", "slabs", "skirt")) {
        var mistake = assertThrows(IllegalArgumentException.class, () -> template("{\"bodies\": [{\"shape\": \"boulder\", \"" + key + "\": 1}]}"));
        assertTrue(mistake.getMessage().startsWith("bodies[0]." + key), mistake.getMessage());
    }
}
@Test void clayIsTheBrightestUnlessThePaletteSaysOtherwise() {
    assertEquals("minecraft:clay", template("{\"bodies\": [{\"shape\": \"boulder\"}]}").palette().block(RockMaterial.BRIGHT));
    assertEquals("minecraft:calcite", template("{\"bodies\": [{\"shape\": \"boulder\"}], \"palette\": {\"bright\": \"minecraft:calcite\"}}").palette().block(RockMaterial.BRIGHT));
}
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew test --tests '*RockTemplateTest*'`
Expected: compilation fails on `RockMaterial.BRIGHT`.

- [ ] **Step 3: Make the changes listed under Files.** The old model still computes slabs and vines; the stamper no longer places them. Its unknown-block check covers `blocks` and `plants` only.

- [ ] **Step 4: Run the rock tests and the game tests**

Run: `./gradlew test --tests '*Rock*'`, then the game tests (Global Constraints).
Expected: all rock tests pass; 7 of 7 rock game tests pass.

- [ ] **Step 5: Commit** (`"Rocks: clay as the brightest tone; slabs, vines and loose stones go"` + trailers)

### Task 3: The blocks voxelized

**Files:**
- Create: `rocks/RockVoxels.java`
- Test: `src/test/java/frootloops/versus/mod/environment/worldgen/rocks/RockVoxelsTest.java`

**Interfaces:**
- Consumes: `RockBlock` (Task 1).
- Produces: `final class RockVoxels`:
  - `static RockVoxels of(List<RockBlock> blocks, double blend, double flare, double flareHeight, long seed)`
  - `final int minX, minY, minZ, sizeX, sizeY, sizeZ`
  - `int index(int x, int y, int z)`: `((x - minX) * sizeZ + (z - minZ)) * sizeY + (y - minY)`
  - `boolean solid(int x, int y, int z)` (false outside the grid), `boolean filled(int x, int y, int z)` (solid, or `y < 0`: the ground), `boolean seam(int x, int y, int z)`, `int top()` (the highest solid y)

- [ ] **Step 1: Write the failing tests:**

```java
@Test void aBlockIsSolidInsideOnly()          // one standing box, half 3 x 3, 6 tall: solid(0, 3, 0), not solid(6, 3, 0) nor (0, 8, 0)
@Test void blocksMeetInAGrooveNotASlot()      // two boxes 6 wide, 8 tall, their faces 0.5 apart, blend 0.8: solid at the gap's middle, y = 3, z = 0
@Test void noKnobsNorPits()                    // 5 blocks tipped by RockDice(3): no solid cell at y >= 0 with 1 filled face neighbour or fewer, no empty one with 5 or more
@Test void theFootCurvesOut()                  // a box half 3 x 3, 20 tall: with flare 3 over 9 the width at y = 0 is 4 or more wider than without, at y = 12 the same within 1
@Test void seamsAreDrawnWholeAndNeverInsideAGroup() // two touching blocks of group 7: no seam cell; of groups 1 and 2, seeds 1 to 20: a seed draws all the cells of their meeting or none
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew test --tests '*RockVoxelsTest*'`
Expected: compilation fails, `RockVoxels` doesn't exist.

- [ ] **Step 3: Implement `RockVoxels`:**
  - The grid: the blocks' centres plus or minus their reach, plus `flare + 1`; `minY = max(-4, floor(lowest) - 1)`.
  - Each cell centre's value is the smooth minimum over the blocks within reach: `h = max(k - |a - b|, 0) / k`, `min(a, b) - h * h * k / 4`, `k = blend`.
  - A block's soft maximum is only needed where its hard maximum is within 2 blocks of 0. Elsewhere the hard maximum stands (this is what keeps the build within 300 ms).
  - Solid where `value - flare * r * r < 0`, with `r = max(0, 1 - (y + 0.5) / flareHeight)`.
  - Seams: track each cell's nearest two blocks by raw distance. A seam cell is one where `second - best < 1`, the two blocks' groups differ (or are -1), and `RockNoise.unit(RockNoise.mix(seed + 31 * min(i, j) + 7919 * max(i, j))) < 0.5`.
  - Then two clean-up passes over `y >= 0`, as in `noKnobsNorPits`.

- [ ] **Step 4: Run them to see them pass**

Run: `./gradlew test --tests '*Rock*'`
Expected: all pass.

- [ ] **Step 5: Commit** (`"Rocks: blocks voxelized, blended where they meet, the foot curving out"` + trailers)

### Task 4: Light painted from above

**Files:**
- Create: `rocks/RockLight.java`
- Test: `src/test/java/frootloops/versus/mod/environment/worldgen/rocks/RockLightTest.java`

**Interfaces:**
- Consumes: `RockVoxels` (Task 3), `RockMaterial.BRIGHT` (Task 2).
- Produces: `final class RockLight` with `static RockMaterial[] paint(RockVoxels voxels, long seed)`: the tone of each cell by `voxels.index`, `null` for air; `SHADE`, `BASE`, `LIGHT` or `BRIGHT`.

- [ ] **Step 1: Write the failing tests:**

```java
@Test void onlyTheFourTonesOfRock()      // a 3-block cluster: every solid cell one of SHADE, BASE, LIGHT, BRIGHT; air null
@Test void undersidesAreTuff()           // a box 8 wide resting on one 3 wide: every cell at y > 0 with air under it and facing down is SHADE
@Test void lightComesFromAbove()         // a 12-block column cluster: mean tone (SHADE 0 .. BRIGHT 3) of the exposed upward-facing cells in the top third >= that of the exposed cells in the bottom third + 0.5
@Test void stoneIsCommonestAndClayRare() // the same cluster's exposed cells: BASE the largest share, BRIGHT 0.05 or less
@Test void theSameRockTheSameTones()     // two paints of equal voxels and seed are equal
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew test --tests '*RockLightTest*'`
Expected: compilation fails, `RockLight` doesn't exist.

- [ ] **Step 3: Implement `RockLight.paint`.** Cells below the ground or shut in on all six sides are `BASE`. For each exposed cell, take:
  - `open`: the share of open cells within two blocks (124 around it), read from a summed-volume table of the grid;
  - `up`: the y share of the open cells' pull within two blocks, from box counts of the same table (above minus below, east minus west, south minus north);
  - `sky`: the share of five rays, `(0,1,0)`, `(±1,1,0)` and `(0,1,±1)`, that leave 16 steps unblocked.

  Tuff where the cell has air under it at `y > 0` and `up < -0.3`, or `seam && open < 0.55`, or `open < 0.36`. Otherwise:

  `light = 0.35 * sky + 0.25 * up + 0.8 * (open - 0.5) + 0.65 * y / top + 0.07 * RockNoise.noise(seed + 50, x / 4.0, y / 4.0, z / 4.0) + 0.05 * (RockNoise.unit(RockNoise.hash(seed + 51, x, y, z)) - 0.5)`

  The tones go by that light's quantiles over these cells: below the 0.30 quantile `SHADE`, below 0.76 `BASE`, below 0.97 `LIGHT`, the rest `BRIGHT`.

- [ ] **Step 4: Run them to see them pass**

Run: `./gradlew test --tests '*Rock*'`
Expected: all pass.

- [ ] **Step 5: Commit** (`"Rocks: light painted from above in tuff, stone, andesite and clay"` + trailers)

### Task 5: Rocks of blocks: the shapes' recipes and the model over them

**Files:**
- Modify: `rocks/RockPlan.java` (`Body`; `Lobe` and `Fracture` go)
- Rewrite: `rocks/RockShape.java`, `rocks/RockModel.java`
- Modify: `rocks/RockTemplate.java` (`reach()`, `plan()`), `src/gametest/java/frootloops/versus/gametest/client/WorldgenGallery.java` if `bounds` changed meaning
- Rewrite test: `RockModelTest.java`; adjust `RockTemplateTest.java`

**Interfaces:**
- Consumes: `RockDice`, `RockBlock` (Task 1); `RockVoxels` (Task 3); `RockLight` (Task 4).
- Produces:
  - `record Body(int dx, int dz, List<RockBlock> blocks, double blend, double flare, double flareHeight, double cover, double height, long seed)` with `Body withCover(double cover)` and `Body at(int dx, int dz)`
  - `RockShape.body(long seed, double height, double radius)` and `int RockShape.reach(double height, double radius)`, a bound on `RockModel.build(body).reach()` for any seed
  - `RockModel` keeps:
    - `build`, `body`, `bounds`, `reach`;
    - `material(x, y, z)`, `lowest(x, z)`, `highest(x, z)`, `rise(x, z)`;
    - `NONE`, the public `minX` to `sizeZ` and `groundMinX` to `groundSizeZ`;
    - `decorations()`, now `List<Decoration>` with `record Decoration(int x, int y, int z, int data)` (a plant).

    `slab()` goes.

- [ ] **Step 1: Write the failing tests** in the rewritten `RockModelTest`. The helpers stay: `stamp`, `rock`, `tone`, `GROUND`, `FLAT`, `everyShape`. These carry over unchanged:
  - `sameSeedSameRock`
  - `anotherSeedAnotherRock`
  - `nothingFloats`
  - `stampedChunkByChunkInAnyOrderIsTheRockStampedWhole`
  - `staysWithinItsReach` (against `RockShape.reach`)
  - `stampsOnlyInsideTheBoxGiven`
  - `standsOnASlope`
  - `nothingGrowsUnderWater`
  - `grassOnlyUnderTheSkyAndMossOnlyOnTheSurface`

  New tests:

```java
@Test void everyShapeIsAboutTheSizeAsked()        // mid-range height and radius, seeds 1 to 8: the highest rock within 0.7 and 1.15 of the height
@Test void theSmallestOfEveryShapeIsStillARock()  // each shape at its minimum height and radius, seeds 1 to 8: 20 or more rock cells
@Test void noNoiseOnTheFaces()                    // every shape, seeds 1 to 3: knobs and pits at most 1% of the surface cells
@Test void aBoulderIsTwoBlocksOrMore()            // BOULDER seeds 1 to 20: body.blocks().size() >= 2
@Test void aMonolithLeansAndHasItsShoulder()      // MONOLITH seeds 1 to 8: the top's centre 0.15 * height or more from the foot's, sideways; a second top between 0.45 and 0.75 of the height
@Test void aTowersColumnsDifferInHeight()         // TOWER seeds 1 to 8 with 2 or more columns (groups): their tops differ by 0.1 * height or more
@Test void aTallRockIsWiderAtItsFootAndPropped()  // MONOLITH and TOWER seeds 1 to 8: rock cells at y = 0 at least 1.2 times those halfway up; in 7 of 8 a block 0.4 of the height or less stands at the foot
@Test void onlyTheFourTonesUnderTheCover()        // every cell: rock:SHADE, rock:BASE, rock:LIGHT, rock:BRIGHT, rock:GRASS, rock:MOSS, rock:SOIL, ground or plant
@Test void aRockLargerThanItsShapesRangeStaysWithinItsReach() // BOULDER at height 18, radius 10, seeds 1 to 20: model.reach() <= RockShape.BOULDER.reach(18, 10)
@Test void aFootIsNeverFilledMoreThanItsMost()    // a body on a column whose ground is 40 lower: no rock written below anchor - MAX_FILL - 1
@Test void readingTheModelFromManyThreadsGivesTheSameRock() // 8 threads read every material of one built MASSIF model: all equal to a sequential read
@Test void aBigFormationIsBuiltQuickly()          // MASSIF at height 56, radius 26: the best of 3 builds 300 ms or less
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew test --tests '*RockModelTest*'`
Expected: compilation fails, `Body` has no `blocks()`.

- [ ] **Step 3: Rewrite `RockPlan.Body` and `RockShape`.** Keep the enum's `minHeight, maxHeight, minRadius, maxRadius`, now:

  | Shape | Height | Radius |
  |---|---|---|
  | BOULDER | 6 to 10 | 3 to 5 |
  | PILE | 5 to 9 | 5 to 9 |
  | DOME | 5 to 8 | 6 to 9 |
  | CRAG | 8 to 13 | 3.5 to 5 |
  | TOWER | 30 to 46 | 7 to 10 |
  | NEEDLE | 18 to 34 | 2.5 to 4.5 |
  | SPIRE | 55 to 85 | 10 to 14 |
  | MASSIF | 38 to 56 | 18 to 26 |
  | MONOLITH | 26 to 38 | 8 to 11 (its width) |

  The recipes are the prototype's (`Weathered.boulder`, `pile`, `castle`, `monolith`, `feet`); its numbers are the spec's.
  - **BOULDER, DOME:** `boulder`; blend 0.6, flare 1 over 3, props chance 0.55.
  - **PILE:** `pile`, layers 3 thick; blend 0.5, flare 0.5 over 2, no props.
  - **Columns, from `castle`:** joints every `width`, blocks every `joint`, lower outward by `flat`; blend 0.6, flare 3 over 9, props chance 0.85.
    - TOWER: width 0.9 times the radius, joint 14, flat 0.4.
    - NEEDLE: one column, width twice the radius, joint 12.
    - SPIRE: width 0.9 times the radius, joint 16, flat 0.55, and a needle column (half to 0.7 of the height) 1.2 times the radius away.
    - CRAG: width 0.9 times the radius, joint 6, flat 0.5.
    - MASSIF: width 10, joint 16, flat 0.8.
  - **MONOLITH:** `monolith`, its lean 0.16 to 0.26 toward its yaw; blend 0.8, flare 3 over 9, props chance 0.9.

  Default covers:

  | Shape | Cover |
  |---|---|
  | boulder, pile, dome, needle | 0.6 |
  | crag | 0.5 |
  | tower, spire | 0.75 |
  | massif | 0.8 |
  | monolith | 0.85 |

- [ ] **Step 4: Rewrite `RockModel`** over `RockVoxels.of(body.blocks(), body.blend(), body.flare(), body.flareHeight(), body.seed())`, then `RockLight.paint(...)`, then:
  - **Cover:** a level top (3 of 4 neighbours' tops within 1) is covered with chance `cover * (0.45 + 0.55 * min(1, top / max(4, 0.6 * height)))`. The patch test is `noise(seed + 40, x / 5, top / 5, z / 5) < 2 * chance - 1 + 0.15 * (unit(hash(seed + 41, x, top, z)) - 0.5)`. It's moss where `noise(seed + 42, x / 3.5, top / 3.5, z / 3.5) > -0.25`, grass elsewhere. Moss hangs 1 to 2 down an open side, each block with chance 0.55. A plant stands on 30% of the grass.
  - **Rise:** up to `clamp(0.06 * height + 0.5, 1, 2.5)`. Out to `clamp(0.3 * height + 2, 3, 10)` times `0.75 + 0.5 * unit(hash(seed + 90, x >> 2, 0, z >> 2))`. Its profile is `(1 - d / out)^1.4`, where `d` is the distance to the nearest column with rock at y 0.
  - **`bounds`:** the blocks' extents plus the flare and the rise's reach.
  - **Concurrency:** every array is filled in `build`; queries only read.

  In `RockTemplate`, `reach()` is `away + shape.reach(height.most, radius.most)`, and `plan()` calls `shape.body(...).withCover(cover or the shape's).at(...)`.

- [ ] **Step 5: Run the rock tests and the game tests**

Run: `./gradlew test --tests '*Rock*'`, then the game tests.
Expected: all rock tests pass; 7 of 7 rock game tests pass.

- [ ] **Step 6: Commit** (`"Rocks: rocks of angular blocks, the noise model gone"` + trailers)

### Task 6: The look, checked

**Files:**
- Modify (if the renders call for it): `tools/rocks.py` and the data it writes; `docs/superpowers/specs/2026-11-09-rocks-transitions-wetlands-design.md` (its Rocks section: superseded by the weathered-rocks spec)

- [ ] **Step 1: Render every shape with the preview** (`tools/rock-preview.sh`) from the prototype's angles, into a sheet to compare with the last prototype renders the user approved.
Expected: each shape reads as the prototype's: angular blocks, bent faces, a flared and propped foot, tuff low and under, andesite and a little clay high, moss on top.

- [ ] **Step 2: Run the in-game gallery**

Run: `tools/gallery.sh @tools/gallery-rocks.txt <scratchpad>/shots/weathered`
Expected: 30 screenshots; no rock on water or in trees; the user sees them.

- [ ] **Step 3: Mark the old spec's Rocks section superseded; commit; push** (`"Rocks: the weathered rocks checked in the preview and in game"` + trailers), after the user has looked at the screenshots.
