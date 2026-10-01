package frootloops.versus.mod.environment.worldgen;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.biome.PvBiomeSource;
import frootloops.versus.mod.environment.worldgen.debug.WorldgenBench;
import frootloops.versus.mod.environment.worldgen.debug.WorldgenDebugCommands;
import frootloops.versus.mod.environment.worldgen.density.AquiferFloodedness;
import frootloops.versus.mod.environment.worldgen.density.AquiferSpread;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;

/**
 * Entry point for the Players Versus world type ("Improved", preset {@code players-versus:better_world}).
 *
 * <p>Code registered here only affects the Improved world type: the aquifer applies to generators whose settings opt
 * in (see {@link #isPvGenerator}), the carvers to Improved's own generator ({@link PvChunkGenerator}); vanilla world
 * types are left alone. The terrain itself is data ({@code worldgen/density_function/overworld}), which 26.3 compiles.
 * The migration plan lives in {@code docs/worldgen-refactor-plan.md}.
 */
public final class PvWorldgen {

    public static final Identifier AQUIFER_FLOODEDNESS_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "aquifer_floodedness");
    public static final Identifier AQUIFER_SPREAD_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "aquifer_spread");
    public static final Identifier BIOME_SOURCE_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "overworld");
    public static final Identifier CHUNK_GENERATOR_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "noise");

    private PvWorldgen() {
    }

    public static void initialize() {
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, AQUIFER_FLOODEDNESS_ID, AquiferFloodedness.CODEC);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, AQUIFER_SPREAD_ID, AquiferSpread.CODEC);
        Registry.register(BuiltInRegistries.BIOME_SOURCE, BIOME_SOURCE_ID, PvBiomeSource.CODEC);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, CHUNK_GENERATOR_ID, PvChunkGenerator.CODEC);
        WorldgenDebugCommands.register();
        WorldgenBench.registerHeadlessRun();
    }

    /**
     * Whether a generator uses the Players Versus aquifer: its settings' aquifer config carries an
     * {@link AquiferFloodedness} as its {@code fluid_level_floodedness}.
     *
     * <p>Checks the settings themselves, not what the world compiled from them, which other mods may change.
     */
    public static boolean isPvGenerator(NoiseGeneratorSettings settings) {
        return settings.aquifers().map(config -> unwrap(config.fluidLevelFloodednessNoise()) instanceof AquiferFloodedness).orElse(false);
    }

    /** The function a registry reference points to, following references to references. */
    public static DensityFunction unwrap(DensityFunction function) {
        while (function instanceof DensityFunctions.HolderHolder holder) {
            function = holder.function().value();
        }
        return function;
    }
}
