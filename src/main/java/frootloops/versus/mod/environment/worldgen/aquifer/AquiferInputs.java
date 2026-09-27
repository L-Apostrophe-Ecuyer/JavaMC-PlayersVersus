package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.PvWorldgen;
import frootloops.versus.mod.environment.worldgen.density.AquiferFloodedness;
import frootloops.versus.mod.environment.worldgen.density.AquiferSpread;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The aquifer's input functions for one world: the generator settings' {@link AquiferFloodedness} and
 * {@link AquiferSpread}, with their noises seeded from the world's {@link NoiseConfig} the way NoiseConfig seeds its
 * own router. Built once per NoiseConfig and shared by every chunk; density functions hold no state.
 *
 * <p>It seeds the settings' functions itself instead of reading NoiseConfig's router, whose functions other mods may
 * replace: C2ME compiles them, and a compiled function is no longer an {@code AquiferFloodedness}.
 */
public record AquiferInputs(AquiferFloodedness floodedness, AquiferSpread spread) {

    /** Weak keys: an entry goes with its world. The values don't refer to their NoiseConfig, only to its noise samplers. */
    private static final Map<NoiseConfig, AquiferInputs> BY_CONFIG = Collections.synchronizedMap(new WeakHashMap<>());

    public static AquiferInputs of(NoiseConfig config, ChunkGeneratorSettings settings) {
        return BY_CONFIG.computeIfAbsent(config, key -> seed(key, settings));
    }

    private static AquiferInputs seed(NoiseConfig config, ChunkGeneratorSettings settings) {
        DensityFunction floodedness = PvWorldgen.unwrap(settings.noiseRouter().fluidLevelFloodednessNoise()).apply(seeding(config));
        DensityFunction spread = PvWorldgen.unwrap(settings.noiseRouter().fluidLevelSpreadNoise()).apply(seeding(config));
        if (!(floodedness instanceof AquiferFloodedness seededFloodedness) || !(spread instanceof AquiferSpread seededSpread)) {
            throw new IllegalStateException("A Players Versus noise router needs players-versus:aquifer_floodedness in fluid_level_floodedness"
                    + " and players-versus:aquifer_spread in fluid_level_spread, found " + floodedness + " and " + spread);
        }
        return new AquiferInputs(seededFloodedness, seededSpread);
    }

    /**
     * Gives every noise the world's sampler for that noise's key, which is what NoiseConfig's own router gets.
     * NoiseConfig also reseeds {@code old_blended_noise} and {@code end_islands} and, with the legacy random source,
     * a few noises differently; none of that appears in the aquifer's inputs, and Players Versus settings don't use the
     * legacy random source. {@code WorldgenDataTest} checks that the two agree.
     */
    public static DensityFunction.DensityFunctionVisitor seeding(NoiseConfig config) {
        return new DensityFunction.DensityFunctionVisitor() {
            @Override
            public DensityFunction apply(DensityFunction densityFunction) {
                return densityFunction;
            }

            @Override
            public DensityFunction.Noise apply(DensityFunction.Noise noise) {
                return new DensityFunction.Noise(noise.noiseData(), config.getOrCreateSampler(noise.noiseData().getKey().orElseThrow()));
            }
        };
    }
}
