package frootloops.versus.mixin.environment.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BonemealableFeaturePlacerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BonemealableFeaturePlacerBlock.class)
public abstract class MossMixin extends Block implements BonemealableBlock {
    public MossMixin(Properties settings) {
        super(settings);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, BonemealSource source) {
        BlockState stateUp = world.getBlockState(pos.above());
        return (!stateUp.canOcclude() || stateUp.getBlock().defaultDestroyTime() < 1.0f) || stateUp.is(Blocks.MOSS_BLOCK);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if(placer.isShiftKeyDown()) return;
        if(world.getBlockState(pos.above()).isAir()) {
            MossMixin.mossifyNeighborBlock(world, pos.north());
            MossMixin.mossifyNeighborBlock(world, pos.south());
            MossMixin.mossifyNeighborBlock(world, pos.west());
            MossMixin.mossifyNeighborBlock(world, pos.east());
            MossMixin.mossifyNeighborBlock(world, pos.below());
        }
    }

    @Inject(method = "performBonemeal", at = @At("TAIL"), cancellable = false)
    private void addBlocksBelow(ServerLevel world, RandomSource random, BlockPos pos, BlockState state, BonemealSource source, CallbackInfo info) {
        pos = pos.below();
        BlockState stateDown = world.getBlockState(pos);
        if(stateDown.is(BlockTags.MOSS_REPLACEABLE)) {
            BlockState neighborState;
            BlockPos[] neighborsPos = new BlockPos[] {pos.north(), pos.south(), pos.west(), pos.east()};
            int numAirBlocks = 0;
            boolean hasMoss = false;
            for (BlockPos blockPos : neighborsPos) {
                neighborState = world.getBlockState(blockPos);
                if(neighborState.is(Blocks.MOSS_BLOCK)) {
                    hasMoss = true;
                    break;
                }
                if(neighborState.isAir()) numAirBlocks++;
            }
            if(numAirBlocks >= 2 || hasMoss) {
                world.setBlockAndUpdate(pos, Blocks.MOSS_BLOCK.defaultBlockState());
            }
        }
    }

    private static void mossifyNeighborBlock(Level world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();

        if(block == Blocks.COBBLESTONE) world.setBlock(pos, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        else if(block == Blocks.COBBLESTONE_SLAB) world.setBlock(pos, Blocks.MOSSY_COBBLESTONE_SLAB.withPropertiesOf(blockState), Block.UPDATE_CLIENTS);
        else if(block == Blocks.COBBLESTONE_STAIRS) world.setBlock(pos, Blocks.MOSSY_COBBLESTONE_STAIRS.withPropertiesOf(blockState), Block.UPDATE_CLIENTS);

        else if(block == Blocks.STONE_BRICKS) world.setBlock(pos, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), Block.UPDATE_CLIENTS);
        else if(block == Blocks.STONE_BRICK_SLAB) world.setBlock(pos, Blocks.MOSSY_STONE_BRICK_SLAB.withPropertiesOf(blockState), Block.UPDATE_CLIENTS);
        else if(block == Blocks.STONE_BRICK_STAIRS) world.setBlock(pos, Blocks.MOSSY_STONE_BRICK_STAIRS.withPropertiesOf(blockState), Block.UPDATE_CLIENTS);
    }
}
