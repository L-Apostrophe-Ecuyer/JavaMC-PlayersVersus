package frootloops.versus.mod.environment.worldgen;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.biome.PvBiomeSource;
import frootloops.versus.mod.environment.worldgen.debug.WorldgenBench;
import frootloops.versus.mod.environment.worldgen.debug.WorldgenDebugCommands;
import frootloops.versus.mod.environment.worldgen.density.AquiferFloodedness;
import frootloops.versus.mod.environment.worldgen.density.AquiferSpread;
import frootloops.versus.mod.environment.worldgen.density.PvAtHeight;
import frootloops.versus.mod.environment.worldgen.density.PvCorridorBias;
import frootloops.versus.mod.environment.worldgen.density.PvDepth;
import frootloops.versus.mod.environment.worldgen.density.PvEntrances;
import frootloops.versus.mod.environment.worldgen.density.PvFinalDensity;
import frootloops.versus.mod.environment.worldgen.density.PvHighRiver;
import frootloops.versus.mod.environment.worldgen.density.PvNoodle;
import frootloops.versus.mod.environment.worldgen.density.PvTerrain;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/**
 * Entry point for the Players Versus world type ("Improved", preset {@code players-versus:better_world}).
 *
 * <p>Code registered here only affects the Improved world type: the aquifer and ore veins apply to generators whose
 * settings opt in (see {@link #isPvGenerator}), the carvers to Improved's own generator ({@link PvChunkGenerator});
 * vanilla world types are left alone. The migration plan lives in {@code docs/worldgen-refactor-plan.md}.
 */
public final class PvWorldgen {

    public static final ResourceLocation AQUIFER_FLOODEDNESS_ID = ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "aquifer_floodedness");
    public static final ResourceLocation AQUIFER_SPREAD_ID = ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "aquifer_spread");
    public static final ResourceLocation DEPTH_ID = ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "depth");
    public static final ResourceLocation TERRAIN_ID = ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "terrain");
    public static final ResourceLocation ENTRANCES_ID = ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "entrances");
    public static final ResourceLocation NOODLE_ID = ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "noodle");
    public static final ResourceLocation FINAL_DENSITY_ID = ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "final_density");
    public static final ResourceLocation CORRIDOR_BIAS_ID = ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "corridor_bias");
    public static final ResourceLocation HIGH_RIVER_ID = ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "high_river");
    public static final ResourceLocation AT_HEIGHT_ID = ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "at_height");
    public static final ResourceLocation BIOME_SOURCE_ID = ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "overworld");
    public static final ResourceLocation CHUNK_GENERATOR_ID = ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "noise");

    private PvWorldgen() {
    }

    public static void initialize() {
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, AQUIFER_FLOODEDNESS_ID, AquiferFloodedness.CODEC);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, AQUIFER_SPREAD_ID, AquiferSpread.CODEC);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, DEPTH_ID, PvDepth.CODEC);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, TERRAIN_ID, PvTerrain.CODEC);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, ENTRANCES_ID, PvEntrances.CODEC);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, NOODLE_ID, PvNoodle.CODEC);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, FINAL_DENSITY_ID, PvFinalDensity.CODEC);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, CORRIDOR_BIAS_ID, PvCorridorBias.CODEC);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, HIGH_RIVER_ID, PvHighRiver.CODEC);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, AT_HEIGHT_ID, PvAtHeight.CODEC);
        Registry.register(BuiltInRegistries.BIOME_SOURCE, BIOME_SOURCE_ID, PvBiomeSource.CODEC);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, CHUNK_GENERATOR_ID, PvChunkGenerator.CODEC);
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
    public static boolean isPvGenerator(NoiseGeneratorSettings settings) {
        return unwrap(settings.noiseRouter().fluidLevelFloodednessNoise()) instanceof AquiferFloodedness;
    }

    /** The function a registry reference points to, following references to references. */
    public static DensityFunction unwrap(DensityFunction function) {
        while (function instanceof DensityFunctions.HolderHolder holder) {
            function = holder.function().value();
        }
        return function;
    }
}
