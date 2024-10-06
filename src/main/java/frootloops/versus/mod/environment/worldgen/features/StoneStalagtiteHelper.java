package frootloops.versus.mod.environment.worldgen.features;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;


public class StoneStalagtiteHelper {
    public static double scaleHeightFromRadius(double radius, double scale, double heightScale, double bluntness) {
        //if (radius < bluntness ) radius = bluntness;

        double d = 0.384;
        double e = radius / scale * d;
        double f = 0.75 * Math.pow(e, 1.3333333333333333);
        double g = Math.pow(e, 0.6666666666666666);
        double h = 0.3333333333333333 * Math.log(e);
        double i = heightScale * (f - g - h);
        i = Math.max(i, 0.0);
        return i / d * scale;
    }

    public static boolean canGenerateBase(StructureWorldAccess world, BlockPos pos, int height) {
        float g = 6.0f / (float)height;
        for (float h = 0.0f; h < (float)Math.PI * 2; h += g) {
            int j;
            int i = (int)(MathHelper.cos(h) * (float)height);
            if (!StoneStalagtiteHelper.isAirOrWater(world, pos.add(i, 0, j = (int)(MathHelper.sin(h) * (float)height)))) continue;
            return false;
        }
        return true;
    }

    public static boolean isAirOrWater(WorldAccess world, BlockPos pos) {
        return world.testBlockState(pos, StoneStalagtiteHelper::isAirOrWater);
    }

    public static boolean generateStoneBlock(WorldAccess world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.isIn(BlockTags.CONVERTABLE_TO_MUD)) {
            world.setBlockState(pos, Blocks.STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
            return true;
        }
        return false;
    }

    public static boolean canReplace(BlockState state) {
        return state.isIn(BlockTags.BASE_STONE_OVERWORLD) || state.isIn(BlockTags.CONVERTABLE_TO_MUD) || state.isOf(Blocks.SAND);
    }

    public static boolean isAirOrWater(BlockState state) {
        return state.isAir() || state.isOf(Blocks.WATER);
    }
}
