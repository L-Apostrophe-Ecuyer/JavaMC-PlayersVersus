package frootloops.versus.mod.environment.worldgen;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.biome.PvBiomeSource;
import frootloops.versus.mod.environment.worldgen.debug.WorldgenBench;
import frootloops.versus.mod.environment.worldgen.debug.WorldgenDebugCommands;
import frootloops.versus.mod.environment.worldgen.density.AquiferFloodedness;
import frootloops.versus.mod.environment.worldgen.density.AquiferSpread;
import frootloops.versus.mod.environment.worldgen.density.PvDepth;
import frootloops.versus.mod.environment.worldgen.density.PvEntrances;
import frootloops.versus.mod.environment.worldgen.density.PvFinalDensity;
import frootloops.versus.mod.environment.worldgen.density.PvNoodle;
import frootloops.versus.mod.environment.worldgen.density.PvTerrain;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

/**
 * Entry point for the Players Versus world type ("Improved", preset {@code players-versus:better_world}).
 *
 * <p>Code registered here only affects the Improved world type: the aquifer and ore veins apply to generators whose
 * settings opt in (see {@link #isPvGenerator}), the carvers to Improved's own generator ({@link PvChunkGenerator});
 * vanilla world types are left alone. The migration plan lives in {@code docs/worldgen-refactor-plan.md}.
 */
public final class PvWorldgen {

    public static final Identifier AQUIFER_FLOODEDNESS_ID = Identifier.of(VersusMod.MOD_ID, "aquifer_floodedness");
    public static final Identifier AQUIFER_SPREAD_ID = Identifier.of(VersusMod.MOD_ID, "aquifer_spread");
    public static final Identifier DEPTH_ID = Identifier.of(VersusMod.MOD_ID, "depth");
    public static final Identifier TERRAIN_ID = Identifier.of(VersusMod.MOD_ID, "terrain");
    public static final Identifier ENTRANCES_ID = Identifier.of(VersusMod.MOD_ID, "entrances");
    public static final Identifier NOODLE_ID = Identifier.of(VersusMod.MOD_ID, "noodle");
    public static final Identifier FINAL_DENSITY_ID = Identifier.of(VersusMod.MOD_ID, "final_density");
    public static final Identifier BIOME_SOURCE_ID = Identifier.of(VersusMod.MOD_ID, "overworld");
    public static final Identifier CHUNK_GENERATOR_ID = Identifier.of(VersusMod.MOD_ID, "noise");

    private PvWorldgen() {
    }

    public static void initialize() {
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, AQUIFER_FLOODEDNESS_ID, AquiferFloodedness.CODEC);
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, AQUIFER_SPREAD_ID, AquiferSpread.CODEC);
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, DEPTH_ID, PvDepth.CODEC);
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, TERRAIN_ID, PvTerrain.CODEC);
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, ENTRANCES_ID, PvEntrances.CODEC);
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, NOODLE_ID, PvNoodle.CODEC);
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, FINAL_DENSITY_ID, PvFinalDensity.CODEC);
        Registry.register(Registries.BIOME_SOURCE, BIOME_SOURCE_ID, PvBiomeSource.CODEC);
        Registry.register(Registries.CHUNK_GENERATOR, CHUNK_GENERATOR_ID, PvChunkGenerator.CODEC);
        WorldgenDebugCommands.register();
        WorldgenBench.registerHeadlessRun();
    }

    /**
     * Whether a generator uses the Players Versus aquifer and ore veins: its noise router carries an
     * {@link AquiferFloodedness} in the {@code fluid_level_floodedness} slot.
     *
     * <p>Checks the settings' own router, not the per-chunk copy, so optimizers that rewrite the per-chunk density
     * functions (for example C2ME's density-function compiler) can't hide the marker.
     */
    public static boolean isPvGenerator(ChunkGeneratorSettings settings) {
        return unwrap(settings.noiseRouter().fluidLevelFloodednessNoise()) instanceof AquiferFloodedness;
    }

    /** The function a registry reference points to, following references to references. */
    public static DensityFunction unwrap(DensityFunction function) {
        while (function instanceof DensityFunctionTypes.RegistryEntryHolder holder) {
            function = holder.function().value();
        }
        return function;
    }
}
