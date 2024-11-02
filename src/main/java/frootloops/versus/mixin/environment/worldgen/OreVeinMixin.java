package frootloops.versus.mixin.environment.worldgen;

import frootloops.versus.mod.environment.worldgen.CustomWorldgen;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.gen.OreVeinSampler;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(OreVeinSampler.class)
public class OreVeinMixin {

    private static final double DENSITY_THRESHOLD = 0.4;
    private static final int MAX_DENSITY_INTRUSION = 20;
    private static final double LIMINAL_DENSITY_REDUCTION = 0.2;
    private static final float BLOCK_GENERATION_CHANCE = 0.7F;
    private static final double MIN_ORE_CHANCE = 0.15;
    private static final double MAX_ORE_CHANCE = 0.3;
    private static final double DENSITY_FOR_MAX_ORE_CHANCE = 0.6;
    private static final float RAW_ORE_BLOCK_CHANCE = 0.035F;
    private static final float VEIN_GAP_THRESHOLD = -0.3F;

    @Overwrite
    public static ChunkNoiseSampler.BlockStateSampler create(DensityFunction veinToggle, DensityFunction veinRidged, DensityFunction veinGap, RandomSplitter randomDeriver) {
        BlockState blockState = null;
        return (pos) -> {
            double veinToggleNoiseValue = veinToggle.sample(pos);
            int y = pos.blockY();
            CustomWorldgen.VeinType veinType = veinToggleNoiseValue > 0.0 ? CustomWorldgen.VeinType.COPPER : CustomWorldgen.VeinType.IRON;
            double veinToggleNoiseValueAbs = Math.abs(veinToggleNoiseValue);
            int yBelowMax = veinType.maxY - y;
            int yAboveMin = y - veinType.minY;
            if (yAboveMin >= 0 && yBelowMax >= 0) {
                int l = Math.min(yBelowMax, yAboveMin);
                double f = MathHelper.clampedMap((double)l, 0.0, MAX_DENSITY_INTRUSION, -LIMINAL_DENSITY_REDUCTION, 0.0);
                if (veinToggleNoiseValueAbs + f < DENSITY_THRESHOLD) {
                    return blockState;
                } else {
                    Random random = randomDeriver.split(pos.blockX(), y, pos.blockZ());
                    if (random.nextFloat() > BLOCK_GENERATION_CHANCE) {
                        return blockState;
                    } else if (veinRidged.sample(pos) >= 0.0) {
                        return blockState;
                    } else {
                        double g = MathHelper.clampedMap(veinToggleNoiseValueAbs, DENSITY_THRESHOLD, DENSITY_FOR_MAX_ORE_CHANCE, MIN_ORE_CHANCE, MAX_ORE_CHANCE);
                        if ((double)random.nextFloat() < g && veinGap.sample(pos) > -VEIN_GAP_THRESHOLD) {
                            return random.nextFloat() < RAW_ORE_BLOCK_CHANCE ? veinType.rawOreBlock : veinType.ore;
                        } else {
                            return veinType.stone;
                        }
                    }
                }
            } else {
                return blockState;
            }
        };
    }
    
}
