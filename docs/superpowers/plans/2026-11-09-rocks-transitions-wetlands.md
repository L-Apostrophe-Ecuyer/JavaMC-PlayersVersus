# Rocks, transitions and wetlands: plan for the todos

**Status: draft.** Nothing here is approved. The step-by-step TDD plan is written with the writing-plans skill after the
spec is approved (G1), so this file is the todo-level plan.

Spec: `docs/superpowers/specs/2026-11-09-rocks-transitions-wetlands-design.md`

## Where things stand

- Done and pushed earlier (PR #21): the swamp work (todos #1 to #11).
- Done, local only until this push: the screenshot gallery (`ff399cc0`, #16) and the merge of `26.4---Winter-Drop`
  (`a6da43ae`). Phase 0 runs the unit and game tests on the merged branch.
- Drafted before the spec was approved: the rock model core (RockNoise, RockMaterial, RockPalette, RockPlan, RockModel).
  Not compiled, no tests. It is kept out of the repository until G1, so unapproved code isn't in the PR; Phase 1 brings
  it in.
- Committed for review, not approved: the spec and this plan.
- Not started: rock stamping, presets, feature, formation, layout rows, biomes, wetlands, verification.

## Gates (what needs you)

| Gate | What you decide | Unlocks |
|---|---|---|
| G1 spec | Approve the spec; decide the open and to-derive rows of its layout table | Phases 1 and 3 |
| G2 execution | Native (recommended) or subagent-driven | the step-by-step plan |
| G3 rock looks | Approve the preview sheets (Phase 1) and the in-game gallery (Phase 2) | Phase 4, the rocky biomes |
| G4 final | Look at the final gallery | the push to PR #21 |

The rocks go into biomes once they are as good as possible, so G3 comes before the rocky biomes.

## Phases

### Phase 0: base and gate (#13, #19)

- Get G1 and G2. The spec and this plan are already committed for review; G1 is the approval.
- Run `./gradlew test` on the merged branch. Known failures on the base, not ours: BlightTest (shake),
  EndermanShulkerCarrierTest (chance), the blight game test (fully blighted player). Any other failure is new and is
  fixed before Phase 1.
- Exit: no new test failures.

### Phase 1: rock model in pure code (#14)

- Compile the five draft files and fix what breaks.
- Add `RockCanvas` (the interface the model writes through), `RockStamper` (stamps a model into a canvas over a box) and
  `RockTemplate` (JSON to a plan; the seed picks each value).
- Preset format, for G1: each numeric field is a number or a `[min, max]` range the seed picks within. A preset is a
  `palette` (gray or warm) and `bodies`, each with `lobes`, `cracks` and the body's numbers as in `RockPlan`. Presets are
  written once in `tools/rocks.py` and emitted inline in feature JSON, so the game's codec checks them when the data
  loads.
- Tests first. `RockModelTest`: sameSeedSameRock, nothingFloats, stampedByChunkEqualsStampedWhole, staysWithinItsBounds,
  reachesTheGroundOnASlope, crackIsOpenToTheSky, coverOnlyOnUpwardFaces, noPlantsUnderWater. `RockTemplateTest`: errors
  name the field.
- `RockPreview` (test source): ray-cast pictures of a stamped preset with the game's block textures, shading and fog.
  `tools/rock-preview.sh` runs it. It is for tuning shape without the game: a run takes seconds, a gallery view takes
  minutes here.
- Exit: tests green; a sheet of the four presets beside the four references, for G3.

### Phase 2: rocks in the world (#20)

- `RockFeature` (`players-versus:rock`) writes within 15 blocks of its origin only. `LevelRockCanvas` writes into the world.
- `RockFormation` structure (`players-versus:rock_formation`): one piece per body, placed by `random_spread`. The piece
  saves its seed and template; `postProcess` stamps only the chunk box it is given.
- `/pvwg rock <preset> [seed]` places a preset at your position, through the same code.
- `RocksGameTest`: a feature places rock and nothing beyond 15 blocks; a formation stamped in two halves equals one
  stamped whole; no plants under water.
- Gallery: every preset from three angles.
- Exit: game tests green, gallery shots reviewed, then G3.

### Phase 3: transitions that need no rocks (#15)

- Layout rows that need no rocks: Warm Dark Forest (once its band is derived), the jungle to forest transition, the
  Tuscan grove redone, warm oak and dark jungle forests denser.
- Tests first: `PvBiomeLayoutTest` gets a test per row. `BiomeFeatureOrderTest` keeps shared features in order.
- Census and gallery, compared with the baseline already taken: `/tmp/claude-0/-home-user-JavaMC-PlayersVersus/85c2ecdf-1848-5d72-a816-005aad6c3e41/scratchpad/shots/base1`
  (nine biomes, aerial and ground views).
- Exit: tests green, census shows every new biome, gallery shots reviewed.

### Phase 4: rocky biomes (#22, after G3)

- Rocky Hills and Rocky Savanna: layout rows, biome JSON written by a new entry in `tools/rocks.py`, the rock features in
  their feature lists, and the formation's biome tag.
- Exit: census finds formations in both biomes; gallery shots reviewed.

### Phase 5: wetlands (#17, after Phase 3, since both edit the layout)

- Grassy Wetlands on the swamp rim where G1 says (a climate box), waterlogged leaves in swamp water, Mangrove Marsh with a
  rim and greener mangrove foliage, marsh polish.
- Tests: a `MarshFeatureTest` case for the waterlogged leaves; the layout rows.
- Exit: census, and gallery shots compared with the baseline (swamp, cold swamp, mangrove swamp).

### Phase 6: verify, document, push (#18, after G4)

- `./gradlew test`; `./gradlew runGameTest -Ppv.acceptEula=true`; `runWorldgenSmoke` for `players-versus:better_world`,
  `minecraft:normal` and `server-default`.
- Check that `check` still doesn't run `runClientGameTest`. A dry run earlier showed no client task; re-check at the end.
- Update `docs/worldgen-refactor-plan.md` section 1.4 and mark the spec as built.
- Commit in pieces; push after G4.

## Dependencies

G1 → Phase 0 → Phase 1 → G3 → Phase 2 → Phase 4. Phase 3 needs only G1. Phase 5 follows Phase 3. Phase 6 needs Phases
2, 4 and 5.

## Review focus: where each line is tested

- A formation made in separate sessions, or chunk by chunk in any order: `stampedByChunkEqualsStampedWhole` (Phase 1), and
  a game test on a saved piece (Phase 2).
- A steep slope or a cliff edge: `reachesTheGroundOnASlope` (Phase 1), and a check that no fill is taller than 24 blocks.
- A rock in water: `noPlantsUnderWater` (Phase 1), and a game test with a pond (Phase 2).
- Bad data (an unknown block, a preset larger than its feature's reach): `RockTemplateTest` (Phase 1), a load-time error
  that names the field.
- Biome tags (overworld, mineshafts, frogs, villages, structures): a `BiomeTagsTest` for every new biome (Phases 3 to 5).

## Risks

- The spec's layout table had wrong rows, now corrected. Its open rows need climate boxes read from the parameter table
  before any layout test can be written.
- The rock model has never been compiled or run. Expect a first round of fixes.
- Shape tuning is the slowest loop. Phase 1's preview exists so most of it doesn't need the game.
- Overrides in `data/minecraft/worldgen` also change vanilla world types. Each commit that adds one says so.
