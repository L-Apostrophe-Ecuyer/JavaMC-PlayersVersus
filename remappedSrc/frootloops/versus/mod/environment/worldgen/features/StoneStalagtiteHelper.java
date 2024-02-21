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
        if (radius < bluntness) {
            radius = bluntness;
        }
        double d = 0.384;
        double e = radius / scale * 0.384;
        double f = 0.75 * Math.pow(e, 1.3333333333333333);
        double g = Math.pow(e, 0.6666666666666666);
        double h = 0.3333333333333333 * Math.log(e);
        double i = heightScale * (f - g - h);
        i = Math.max(i, 0.0);
        return i / 0.384 * scale;
    }

    public static boolean canGenerateBase(StructureWorldAccess world, BlockPos pos, int height) {
        if (StoneStalagtiteHelper.canGenerateOrLava(world, pos)) {
            return false;
        }
        float f = 6.0f;
        float g = 6.0f / (float)height;
        for (float h = 0.0f; h < (float)Math.PI * 2; h += g) {
            int j;
            int i = (int)(MathHelper.cos(h) * (float)height);
            if (!StoneStalagtiteHelper.canGenerateOrLava(world, pos.add(i, 0, j = (int)(MathHelper.sin(h) * (float)height)))) continue;
            return false;
        }
        return true;
    }

    public static boolean canGenerate(WorldAccess world, BlockPos pos) {
        return world.testBlockState(pos, StoneStalagtiteHelper::canGenerate);
    }

    public static boolean canGenerateOrLava(WorldAccess world, BlockPos pos) {
        return world.testBlockState(pos, StoneStalagtiteHelper::canGenerateOrLava);
    }

    public static boolean generateStoneBlock(WorldAccess world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.isIn(BlockTags.CONVERTABLE_TO_MUD)) {
            world.setBlockState(pos, Blocks.STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
            return true;
        }
        return false;
    }

    public static boolean canReplaceOrLava(BlockState state) {
        return StoneStalagtiteHelper.canReplace(state) || state.isOf(Blocks.LAVA);
    }

    public static boolean canReplace(BlockState state) {
        return state.isIn(BlockTags.CONVERTABLE_TO_MUD) || state.isIn(BlockTags.BASE_STONE_OVERWORLD);
    }

    public static boolean canGenerate(BlockState state) {
        return state.isAir() || state.isOf(Blocks.WATER);
    }

    public static boolean cannotGenerate(BlockState state) {
        return !state.isAir() && !state.isOf(Blocks.WATER);
    }

    public static boolean canGenerateOrLava(BlockState state) {
        return state.isAir() || state.isOf(Blocks.WATER) || state.isIn(BlockTags.DIRT) || state.isIn(BlockTags.RABBITS_SPAWNABLE_ON);
    }
}
