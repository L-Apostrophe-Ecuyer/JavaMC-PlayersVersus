# The cypress, the lemon bush's look, and lemons as food

Asked (2026-11-15), with a reference picture of a "Medium Cypress Tree":

- "Cypress trees overhaul: spruce as wood type, and mix of spruce leaves and lighter Cyprus leaves (same texture as spruce
  but with azalea colors) for highlights. The shape ought to be more pear-like and fuzzy, too."
- "Lemon bushes: the idea is there but texture ought to look like an actual tiny tree. And the 'leaf' part at the top
  ought to have a bushier model than just X cross."
- "Lemons: give one hunger, zero saturation, and take 40 ticks to eat. Gives 4 seconds of sourness effect (FOV), 4 seconds
  of slowness, and 24 seconds of haste 1."

## What the code does now

- **The cypress** (`players-versus:cypress`, the Tuscan grove's): a spruce trunk of 12 or 13 blocks under a column of
  spruce leaves, three blocks across (vanilla's spruce foliage placer at radius 1), from a block or two above the ground to
  a little over the trunk's top. The picture's tree is narrow at its foot, widest in its lower third, tapering to a
  one-leaf spire, with single leaves sticking out (fuzzy) and lighter, yellow-green leaves among the dark ones, mostly on
  its outside and up high.
- **The lemon bush** (`LemonBushBlock`): two halves, each a cross of the rose bush's textures recoloured: citrus green,
  with more leaves, blossoms or lemons where the roses were. The item is the top half's picture.
- **A lemon**: food as sweet berries (2 hunger, 0.1 saturation modifier), eaten in vanilla's 32 ticks; it makes one Sour
  (`SOUR_TICKS`, 30 ticks: the view widens and eases back, `SourFov`).

## Decisions

1. **Lemons**: 1 hunger, no saturation, 40 ticks to eat. Eating one gives Sour for 4 seconds (80 ticks), Slowness I for
   4 seconds and Haste I for 24 seconds (480 ticks). `VersusSettings.Lemons` holds the numbers. The Sour view widens at
   once, holds a second, and eases back over its last three seconds (its fall, 22 ticks for the old 30-tick Sour, would
   have held the widest view for nearly three seconds).
2. **Cypress leaves** (`players-versus:cypress_leaves`): a block of their own, spruce leaves' texture in azalea leaves'
   colours (untinted: the same yellow-green in every biome), otherwise spruce leaves: they live by spruce logs, decay away
   from them, drop as spruce leaves do (saplings, sticks; themselves to shears or silk touch), and are leaves to every tag
   that knows leaves.
3. **The cypress**: the spruce trunk stays. Its crown is placed by a foliage placer of the mod's
   (`players-versus:cypress_foliage_placer`): pear-shaped (narrow at its foot, widest in its lower third, then three blocks
   across, then a spire of single leaves over the trunk's top), fuzzy (now and then a single leaf one block further out,
   most in the wide part; now and then a corner missing), its leaves spruce leaves inside and cypress leaves for highlights
   on the outside, the more the higher (the spire nearly all cypress). No leaf more than two blocks from the trunk.
4. **The lemon bush's look**: a tiny tree. Its lower half a trunk (an oak-bark stem); its upper half a bushy crown (a
   rounded box of citrus-green leaves, wider than the stem, reaching down over the trunk's top) carrying its stage
   (bare, blossoms, lemons) on its faces, as the lemon leaves carry theirs. The item is a tiny tree's picture. The cross
   models go. Everything the bush does stays.

5. **The lemon trees go** (asked later the same day: "Remove lemon trees though, they aren't good enough as is"): by the
   owner's own words, "lemon trees" are the full trees of earlier versions (two or three stripped oak logs under lemon
   leaves) and "lemon bushes" the two-block bushes, so the trees go, the bushes stay. Nothing generated or grew the trees
   any more; they were kept for the worlds that had them. Now the lemon leaves (block, item, models, textures, loot,
   tags, names), the tree feature and its checked placement go; the fruit overlays the bush's crowns use are the bush's
   own (`lemon_bush_blossoms`, `lemon_bush_lemons`). A world that has lemon leaves loses them (unknown blocks become
   air).
6. **The lemon bushes go too** (the owner, next: "It's the two block lemon trees that are ugly."): the two-block bushes
   were what was meant. They go with all that grew or placed them: the lemon bush (block, item, models, textures, loot,
   picking table, tags, names), the lemon sapling and its pot, and the Tuscan grove's orchards (`lemon_bushes`,
   `lemon_bush_plant`, `lemon_orchards`). The full trees stay gone (they were replaced by the bushes at the owner's own
   request that morning). The lemon (item) and Sour stay as decision 1 has them, with nothing growing lemons for now.
   A world that has lemon bushes or saplings loses them.

## Checks

- Game tests: eating a lemon gives the three effects for their times, takes 40 ticks, fills one hunger and no
  saturation; cypress leaves live by spruce logs and drop as spruce leaves; grown cypresses are pears (wider below than
  above, a spire on top, nothing beyond two blocks from the trunk) of both leaves (cypress ones more up high).
- Data: the bush's models and blockstates (trunk below, crowns above, every stage); after decision 6, nothing of the
  lemon plants left, the lemon and Sour kept.
- The gallery: a cypress and lemon bushes up close, looked at.

## As built

- **Lemons** (`Fruits`, `VersusSettings.Lemons`): as decided; the Sour view widens in 2 ticks, holds about a second and
  eases back over its last 60 ticks. A lemon eaten while sour renews Sour: the camera (`SourFov.Tracker`) sees its time
  left go up and holds the view at its peak, rather than drop it to normal and widen it again (the final review's).
- **Cypress leaves** (`tools/cypress.py`, `CustomBlocks.CYPRESS_LEAVES`): vanilla's `LeavesBlock` as spruce's, untinted
  through a `cube_all` model as azalea's leaves are, each pixel of spruce leaves taking azalea's green of the same rank of
  brightness, mipmapped as leaves are (spruce leaves' `dark_cutout` metadata, the final review's); alive by spruce logs
  (`LeafReach`), one kind with spruce leaves, as azalea's two leaves are, so each reaches the trunk through the other
  (the final review's: by their own block only, a cypress lost one leaf in eight the first time its leaves ticked);
  spruce leaves' loot.
- **The cypress** (`CypressFoliagePlacer`, registered as `players-versus:cypress_foliage_placer` in `CustomWorldgen`):
  rows down the trunk from its top to a block or two above the ground, by height in the crown (0 its foot, 1 its top
  row): a plus below 0.1, the square with fuzz (a leaf one block out from a side, 35%) to 0.45, the square with its
  corners kept 60% and 10% fuzz to 0.75, a plus above, then a spire of two or three single leaves over the trunk.
  Each row's square first, then its fuzz, only where the leaf inside it grew (so none hangs beside a ragged corner).
  Highlights 15% + 55% of the height (20% more on fuzz, 90% on the spire).
- **The lemon bush** (`tools/lemons.py`): the lower half an oak stem two pixels wide, flared to three at its foot; the
  upper half four boxes of `lemon_bush_leaves` (oak leaves in a fixed citrus green): a 12-pixel box reaching three pixels
  down over the stem, a 16-pixel band each way round its middle and a cap, the blossoms' or lemons' overlay on copies of
  the boxes 0.05 pixel larger. The item is a tiny tree. The gallery's scene is `tools/gallery-cypress-lemons.txt`.
- **Checks**: `LemonsGameTest.aLemonIsALittleFoodAndAQuickSour`, `TuscanGroveGameTest.cypressLeavesAreSpruceLeavesInLighterColours`
  and `aCypressIsAFuzzyPear` (24 seeds), `LemonBushModelsTest` (2); each watched failing first. The gallery looked at:
  the cypresses and the three stages of bush up close, and the Tuscan grove. After the final review:
  `TuscanGroveGameTest.aCypressKeepsEveryLeafWhenItsLeavesTick` (24 trees on seeds giving both trunk heights, a tick on
  every leaf, none lost), `SourFovTest.aSourRenewedWhileItLastsKeepsTheViewWide`, `CypressLeavesDataTest`; each watched
  failing first. The fuzzy pear's "more cypress leaves up high" is over its 24 trees (at least 1.3 times): a highlight is
  a draw, and about one tree in ninety comes out even on its own.
- **The lemon trees** (decision 5): gone, as decided. `LemonBushBlock` holds the stages (`FRUIT`, `BARE`, `BLOSSOMS`,
  `LEMONS`) the lemon leaves held; `LemonBushModelsTest.theLemonTreesAreGone` checks nothing of them is left in the
  assets or data (watched failing first).
- **The lemon bushes** (decision 6): gone, as decided, with `LemonBushBlock`, the sapling, its pot and the orchards;
  `tools/lemons.py` writes only the lemon, Sour's icon and their names; the gallery's scene is `tools/gallery-cypress.txt`.
  `LemonsDataTest` checks nothing of the lemon plants is left in the resources and that the lemon and Sour are (watched
  failing first); `LemonBushModelsTest`, the bush game tests and `BiomeDataTest`'s orchard test went with the bushes.
