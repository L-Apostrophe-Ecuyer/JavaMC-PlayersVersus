package frootloops.versus.mixin.environment.blocks;

import net.minecraft.block.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MossBlock.class)
public abstract class MossMixin extends Block implements Fertilizable {
    public MossMixin(Settings settings) {
        super(settings);
    }

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        BlockState stateUp = world.getBlockState(pos.up());
        return (!stateUp.isOpaque() || stateUp.getBlock().getHardness() < 1.0f) || stateUp.isOf(Blocks.MOSS_BLOCK);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if(placer.isSneaking()) return;
        if(world.getBlockState(pos.up()).isAir()) {
            MossMixin.mossifyNeighborBlock(world, pos.north());
            MossMixin.mossifyNeighborBlock(world, pos.south());
            MossMixin.mossifyNeighborBlock(world, pos.west());
            MossMixin.mossifyNeighborBlock(world, pos.east());
            MossMixin.mossifyNeighborBlock(world, pos.down());
        }
    }

    @Inject(method = "grow", at = @At("TAIL"), cancellable = false)
    private void addBlocksBelow(ServerWorld world, Random random, BlockPos pos, BlockState state, CallbackInfo info) {
        pos = pos.down();
        BlockState stateDown = world.getBlockState(pos);
        if(stateDown.isIn(BlockTags.MOSS_REPLACEABLE)) {
            BlockState neighborState;
            BlockPos[] neighborsPos = new BlockPos[] {pos.north(), pos.south(), pos.west(), pos.east()};
            int numAirBlocks = 0;
            boolean hasMoss = false;
            for (BlockPos blockPos : neighborsPos) {
                neighborState = world.getBlockState(blockPos);
                if(neighborState.isOf(Blocks.MOSS_BLOCK)) {
                    hasMoss = true;
                    break;
                }
                if(neighborState.isAir()) numAirBlocks++;
            }
            if(numAirBlocks >= 2 || hasMoss) {
                world.setBlockState(pos, Blocks.MOSS_BLOCK.getDefaultState());
            }
        }
    }

    private static void mossifyNeighborBlock(World world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();

        if(block == Blocks.COBBLESTONE) world.setBlockState(pos, Blocks.MOSSY_COBBLESTONE.getDefaultState(), Block.NOTIFY_LISTENERS);
        else if(block == Blocks.COBBLESTONE_SLAB) world.setBlockState(pos, Blocks.MOSSY_COBBLESTONE_SLAB.getStateWithProperties(blockState), Block.NOTIFY_LISTENERS);
        else if(block == Blocks.COBBLESTONE_STAIRS) world.setBlockState(pos, Blocks.MOSSY_COBBLESTONE_STAIRS.getStateWithProperties(blockState), Block.NOTIFY_LISTENERS);

        else if(block == Blocks.STONE_BRICKS) world.setBlockState(pos, Blocks.MOSSY_STONE_BRICKS.getDefaultState(), Block.NOTIFY_LISTENERS);
        else if(block == Blocks.STONE_BRICK_SLAB) world.setBlockState(pos, Blocks.MOSSY_STONE_BRICK_SLAB.getStateWithProperties(blockState), Block.NOTIFY_LISTENERS);
        else if(block == Blocks.STONE_BRICK_STAIRS) world.setBlockState(pos, Blocks.MOSSY_STONE_BRICK_STAIRS.getStateWithProperties(blockState), Block.NOTIFY_LISTENERS);
    }
}
