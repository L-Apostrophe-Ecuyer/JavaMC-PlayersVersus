package frootloops.versus.mod.environment.worldgen.ore;

import frootloops.versus.mod.environment.worldgen.CustomWorldgen;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;

/**
 * Large ore veins for the Players Versus world type: copper veins in terracotta, iron veins in tuff.
 *
 * <p>Same shape as vanilla's {@code OreVeinSampler}, with the vein types from {@link CustomWorldgen.VeinType}.
 * Only used for Players Versus generators ({@code ChunkNoiseSamplerMixin}); vanilla world types keep vanilla veins.
 *
 * <p>The router's {@code vein_toggle} is zero above y 50, so copper veins fade out there even though
 * {@link CustomWorldgen.VeinType#COPPER} allows up to y 96.
 */
public final class PvOreVeins {

    private static final float DENSITY_THRESHOLD = 0.3F;
    private static final int MAX_DENSITY_INTRUSION = 20;
    private static final double LIMINAL_DENSITY_REDUCTION = 0.2;
    private static final float BLOCK_GENERATION_CHANCE = 0.85F;
    private static final float MIN_ORE_CHANCE = 0.15F;
    private static final float DENSITY_FOR_MAX_ORE_CHANCE = 0.6F;
    private static final float VEIN_GAP_THRESHOLD = -0.3F;
    /** Toggle values above this pick copper, below pick iron (vanilla splits at 0). */
    private static final double COPPER_TOGGLE_THRESHOLD = -0.25;

    private PvOreVeins() {
    }

    public static ChunkNoiseSampler.BlockStateSampler create(DensityFunction veinToggle, DensityFunction veinRidged,
                                                             DensityFunction veinGap, RandomSplitter randomDeriver) {
        return pos -> {
            double toggle = veinToggle.sample(pos);
            int y = pos.blockY();
            CustomWorldgen.VeinType veinType = toggle > COPPER_TOGGLE_THRESHOLD ? CustomWorldgen.VeinType.COPPER : CustomWorldgen.VeinType.IRON;
            double toggleStrength = Math.abs(toggle);
            int yBelowMax = veinType.maxY - y;
            int yAboveMin = y - veinType.minY;
            if (yAboveMin < 0 || yBelowMax < 0) return null;

            int yDistanceToEdge = Math.min(yBelowMax, yAboveMin);
            double edgeFade = MathHelper.clampedMap(yDistanceToEdge, 0.0, MAX_DENSITY_INTRUSION, -LIMINAL_DENSITY_REDUCTION, 0.0);
            if (toggleStrength + edgeFade < DENSITY_THRESHOLD) return null;

            Random random = randomDeriver.split(pos.blockX(), y, pos.blockZ());
            if (random.nextFloat() > BLOCK_GENERATION_CHANCE) return null;
            if (veinRidged.sample(pos) >= 0.0) return null;

            double oreChance = MathHelper.clampedMap(toggleStrength, DENSITY_THRESHOLD, DENSITY_FOR_MAX_ORE_CHANCE, MIN_ORE_CHANCE, veinType.oreChance);
            if ((double) random.nextFloat() < oreChance && veinGap.sample(pos) > VEIN_GAP_THRESHOLD) {
                return random.nextFloat() < veinType.rawBlockChance ? veinType.rawOreBlock : veinType.ore;
            }
            return veinType.stone;
        };
    }
}
