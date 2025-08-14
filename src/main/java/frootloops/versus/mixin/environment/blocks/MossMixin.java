package frootloops.versus.mixin.environment.blocks;

import net.minecraft.block.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MossBlock.class)
public abstract class MossMixin extends Block implements Fertilizable {
    public MossMixin(Settings settings) {
        super(settings);
    }

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        BlockState stateUp = world.getBlockState(pos.up());
        return (!stateUp.isOpaque() && stateUp.getBlock().getHardness() == 0.0f) || stateUp.isOf(Blocks.MOSS_BLOCK);
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
