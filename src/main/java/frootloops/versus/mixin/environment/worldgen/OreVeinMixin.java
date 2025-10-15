package frootloops.versus.mixin.environment.worldgen;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.CustomWorldgen;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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

    private static final float DENSITY_THRESHOLD = 0.14F;
    private static final int MAX_DENSITY_INTRUSION = 20;
    private static final double LIMINAL_DENSITY_REDUCTION = 0.2;
    private static final float BLOCK_GENERATION_CHANCE = 0.85F;
    private static final float MIN_ORE_CHANCE = 0.3F;
    private static final float MAX_ORE_CHANCE = 0.5F;
    private static final float DENSITY_FOR_MAX_ORE_CHANCE = 0.6F;
    private static final float RAW_ORE_BLOCK_CHANCE = 0.1F;
    private static final float VEIN_GAP_THRESHOLD = -0.45F;

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
                int yDistanceToCenter = Math.min(yBelowMax, yAboveMin);
                double densityFromCenter = MathHelper.clampedMap(yDistanceToCenter, 0.0, MAX_DENSITY_INTRUSION, -LIMINAL_DENSITY_REDUCTION, 0.0);
                if (veinToggleNoiseValueAbs + densityFromCenter < DENSITY_THRESHOLD) {
                    return null;
                } else {
                    Random random = randomDeriver.split(pos.blockX(), y, pos.blockZ());
                    if (random.nextFloat() > BLOCK_GENERATION_CHANCE) {
                        return null;
                    } else if (veinRidged.sample(pos) >= 0.0) {
                        return null;
                    } else {
                        double oreChance = MathHelper.clampedMap(veinToggleNoiseValueAbs, DENSITY_THRESHOLD, DENSITY_FOR_MAX_ORE_CHANCE, MIN_ORE_CHANCE, MAX_ORE_CHANCE);
                        if ((double)random.nextFloat() < oreChance && veinGap.sample(pos) > VEIN_GAP_THRESHOLD) {
                            return random.nextFloat() < RAW_ORE_BLOCK_CHANCE + (veinType == CustomWorldgen.VeinType.COPPER ? 0.2F : 0.0F) ? veinType.rawOreBlock : veinType.ore;
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
