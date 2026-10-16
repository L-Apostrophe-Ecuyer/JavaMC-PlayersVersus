# Tests written for the 1.21.10 world generator

These tests drove code that 26.3 replaced (the Java terrain kernels, the `NoiseChunk` cell loop, `DensityFunction.compute`,
the noise router's vanilla layout) or read data whose layout changed (`configured_carver`, biome carver lists), so they no
longer compile. They are kept as the reference for redoing them against 26.3's own evaluation (step 6 of the port in
`docs/worldgen-refactor-plan.md`, Section 13); they are not part of any source set.

What each checked, and what carries it now:

| Old test | Checked | Now |
|---|---|---|
| `TerrainPortTest`, `PvHighRiverTest`, `PvNoodleTest` | the Java kernels against the 1.21.10 JSON, point by point | the terrain is data; `tools/port-26.3/pv_density.py --check` compares the generated files with the 1.21.10 ones |
| `AquiferPortTest`, `WorldgenDataTest` | the aquifer's own seeding and lattice against vanilla's router and cell loop | `PvAquifer` samples volumes on 26.3's code path; redo with `RandomState.sampleBlockValueUncached` and a `NoiseChunk` |
| `AquiferTerrainPassTest`, `AquiferSurveyTest`, `DeepWaterSurveyTest`, `FloodedNoodleSurveyTest`, `HighRiverSurveyTest`, `TerrainPass`, `WorldgenTestData` | water, wall and cave statistics over a real terrain pass | `/pvwg bench` and the smoke workflow measure them on a real world |
| `PvCarversTest`, `VanillaOverridesTest` | the carver lists per biome, and what the `data/minecraft` overrides still change | redo with the data step (carvers are records in `worldgen/carver`; biome files are converted with 26.3's format) |
