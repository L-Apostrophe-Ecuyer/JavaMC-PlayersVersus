package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.block.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TorchBlock.class)
public abstract class TorchMixin extends Block {

    public TorchMixin(Settings settings) {
        super(settings);
    }

    public void tickTorchDegradation(BlockState state, ServerWorld world, BlockPos pos, boolean isCompletelyExtinguished) {
        if(state.isOf(Blocks.TORCH)) {
            if(isCompletelyExtinguished) world.setBlockState(pos, CustomBlocks.EXTINGUISHED_TORCH.getStateWithProperties(state));
            else world.setBlockState(pos, CustomBlocks.SMOLDERING_TORCH.getStateWithProperties(state));
        }
        else if(state.isOf(Blocks.WALL_TORCH)) {
            if(isCompletelyExtinguished) world.setBlockState(pos, CustomBlocks.EXTINGUISHED_WALL_TORCH.getStateWithProperties(state));
            else world.setBlockState(pos, CustomBlocks.SMOLDERING_WALL_TORCH.getStateWithProperties(state));
        }
        else return;

        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX(), pos.getY(), pos.getZ(), 4, 0.1, 0.2, 0.1, 0.05);
        world.playSoundAtBlockCenterClient(pos, SoundEvents.BLOCK_CANDLE_EXTINGUISH, SoundCategory.BLOCKS, 0.3f, 0.6f, true);
    }


    @Override
    public boolean hasRandomTicks(BlockState state) {
        return state.isOf(Blocks.TORCH) || state.isOf(Blocks.WALL_TORCH);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if(pos.getY() > 32 || world.getDimension().ultrawarm()) return;
        if(world instanceof ServerWorld && state.isOf(Blocks.TORCH) || state.isOf(Blocks.WALL_TORCH)) {
            int randomInteger = random.nextInt(512);
            int offset = 32 + (randomInteger - pos.getY() < 64 && world.getBlockState(pos.down()).isOf(Blocks.DEEPSLATE) ? 32 : 0);
            if (randomInteger > pos.getY() + 512 - offset) {
                this.tickTorchDegradation(state, world, pos, false);
            }
        }
    }

    @Override
    public void precipitationTick(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation) {
        if(world instanceof ServerWorld) {
            this.tickTorchDegradation(state, (ServerWorld)world, pos, true);
        }
    }
}
