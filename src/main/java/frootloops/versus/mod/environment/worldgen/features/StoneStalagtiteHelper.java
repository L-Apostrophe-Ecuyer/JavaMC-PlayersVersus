package frootloops.versus.mod.environment.worldgen.features;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;


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

    public static boolean canGenerateBase(WorldGenLevel world, BlockPos pos, int height) {
        float g = 6.0f / (float)height;
        for (float h = 0.0f; h < (float)Math.PI * 2; h += g) {
            int j;
            int i = (int)(Mth.cos(h) * (float)height);
            if (!StoneStalagtiteHelper.isAirOrWater(world, pos.offset(i, 0, j = (int)(Mth.sin(h) * (float)height)))) continue;
            return false;
        }
        return true;
    }

    public static boolean isAirOrWater(LevelAccessor world, BlockPos pos) {
        return world.isStateAtPosition(pos, StoneStalagtiteHelper::isAirOrWater);
    }

    public static boolean generateStoneBlock(LevelAccessor world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.is(BlockTags.CONVERTIBLE_TO_MUD)) {
            world.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            return true;
        }
        return false;
    }

    public static boolean canReplace(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.CONVERTIBLE_TO_MUD) || state.is(Blocks.SAND);
    }

    public static boolean isAirOrWater(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER);
    }
}
