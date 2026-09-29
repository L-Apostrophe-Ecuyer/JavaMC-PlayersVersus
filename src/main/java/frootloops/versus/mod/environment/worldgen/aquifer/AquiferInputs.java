package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mixin.environment.worldgen.NoiseConfigAccessor;
import frootloops.versus.mod.environment.worldgen.PvWorldgen;
import frootloops.versus.mod.environment.worldgen.density.AquiferFloodedness;
import frootloops.versus.mod.environment.worldgen.density.AquiferSpread;
import frootloops.versus.mod.environment.worldgen.density.PvFinalDensity;
import frootloops.versus.mod.environment.worldgen.density.PvHighRiver;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.noise.InterpolatedNoiseSampler;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The aquifer's input functions for one world: the generator settings' {@link AquiferFloodedness} and
 * {@link AquiferSpread}, and the high river's valley from their final density ({@link PvHighRiver}, whose inputs the
 * aquifer reads on lattices), seeded from the world's {@link NoiseConfig} the way NoiseConfig seeds its own router.
 * Built once per NoiseConfig and shared by every chunk; density functions hold no state.
 *
 * <p>It seeds the settings' functions itself instead of reading NoiseConfig's router, whose functions other mods may
 * replace: C2ME compiles them, and a compiled function is no longer an {@code AquiferFloodedness}.
 *
 * @param highRiver the high river's valley, or {@code null} if the settings' final density isn't
 *                  {@code players-versus:final_density} (a data pack replaced it), which leaves the aquifer without it
 */
public record AquiferInputs(AquiferFloodedness floodedness, AquiferSpread spread, @Nullable PvHighRiver highRiver) {

    /** Weak keys: an entry goes with its world. The values don't refer to their NoiseConfig, only to its noise samplers. */
    private static final Map<NoiseConfig, AquiferInputs> BY_CONFIG = Collections.synchronizedMap(new WeakHashMap<>());

    public static AquiferInputs of(NoiseConfig config, ChunkGeneratorSettings settings) {
        return BY_CONFIG.computeIfAbsent(config, key -> seed(key, settings));
    }

    private static AquiferInputs seed(NoiseConfig config, ChunkGeneratorSettings settings) {
        DensityFunction.DensityFunctionVisitor seeding = seeding(config);
        DensityFunction floodedness = PvWorldgen.unwrap(settings.noiseRouter().fluidLevelFloodednessNoise()).apply(seeding);
        DensityFunction spread = PvWorldgen.unwrap(settings.noiseRouter().fluidLevelSpreadNoise()).apply(seeding);
        if (!(floodedness instanceof AquiferFloodedness seededFloodedness) || !(spread instanceof AquiferSpread seededSpread)) {
            throw new IllegalStateException("A Players Versus noise router needs players-versus:aquifer_floodedness in fluid_level_floodedness"
                    + " and players-versus:aquifer_spread in fluid_level_spread, found " + floodedness + " and " + spread);
        }
        PvHighRiver highRiver = PvWorldgen.unwrap(settings.noiseRouter().finalDensity()) instanceof PvFinalDensity finalDensity
                && PvWorldgen.unwrap(finalDensity.highRiver()).apply(seeding) instanceof PvHighRiver seeded ? seeded : null;
        return new AquiferInputs(seededFloodedness, seededSpread, highRiver);
    }

    /**
     * Seeds functions as {@link NoiseConfig} seeds its router: every noise gets the world's sampler for that noise's key,
     * and the 3D base noise ({@code old_blended_noise}) a copy seeded from NoiseConfig's own splitter. NoiseConfig also
     * reseeds {@code end_islands} and, with the legacy random source, a few noises differently; none of that appears in
     * the Players Versus overworld, which doesn't use the legacy random source. {@code WorldgenDataTest} checks that
     * the two agree.
     */
    public static DensityFunction.DensityFunctionVisitor seeding(NoiseConfig config) {
        RandomSplitter splitter = ((NoiseConfigAccessor) (Object) config).playersVersus$randomDeriver();
        return new DensityFunction.DensityFunctionVisitor() {
            @Override
            public DensityFunction apply(DensityFunction densityFunction) {
                return densityFunction instanceof InterpolatedNoiseSampler sampler
                        ? sampler.copyWithRandom(splitter.split(Identifier.ofVanilla("terrain")))
                        : densityFunction;
            }

            @Override
            public DensityFunction.Noise apply(DensityFunction.Noise noise) {
                return new DensityFunction.Noise(noise.noiseData(), config.getOrCreateSampler(noise.noiseData().getKey().orElseThrow()));
            }
        };
    }
}
