package frootloops.versus.mod.environment.worldgen;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.debug.WorldgenBench;
import frootloops.versus.mod.environment.worldgen.debug.WorldgenDebugCommands;
import frootloops.versus.mod.environment.worldgen.density.AquiferFloodedness;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

/**
 * Entry point for the Players Versus world type ("Improved", preset {@code players-versus:better_world}).
 *
 * <p>Code registered here only affects generators whose settings opt in (see {@link #isPvGenerator}); vanilla world
 * types are left alone. The migration plan lives in {@code docs/worldgen-refactor-plan.md}.
 */
public final class PvWorldgen {

    public static final Identifier AQUIFER_FLOODEDNESS_ID = Identifier.of(VersusMod.MOD_ID, "aquifer_floodedness");

    private PvWorldgen() {
    }

    public static void initialize() {
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, AQUIFER_FLOODEDNESS_ID, AquiferFloodedness.CODEC);
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

    private static DensityFunction unwrap(DensityFunction function) {
        while (function instanceof DensityFunctionTypes.RegistryEntryHolder holder) {
            function = holder.function().value();
        }
        return function;
    }
}
