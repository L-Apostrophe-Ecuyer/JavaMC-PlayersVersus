package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TorchBlock.class)
public abstract class TorchMixin extends Block {

    public TorchMixin(Properties settings) {
        super(settings);
    }

    public void tickTorchDegradation(BlockState state, ServerLevel world, BlockPos pos, boolean isCompletelyExtinguished) {
        if(state.is(Blocks.TORCH)) {
            if(isCompletelyExtinguished) world.setBlockAndUpdate(pos, CustomBlocks.EXTINGUISHED_TORCH.withPropertiesOf(state));
            else world.setBlockAndUpdate(pos, CustomBlocks.SMOLDERING_TORCH.withPropertiesOf(state));
        }
        else if(state.is(Blocks.WALL_TORCH)) {
            if(isCompletelyExtinguished) world.setBlockAndUpdate(pos, CustomBlocks.EXTINGUISHED_WALL_TORCH.withPropertiesOf(state));
            else world.setBlockAndUpdate(pos, CustomBlocks.SMOLDERING_WALL_TORCH.withPropertiesOf(state));
        }
        else return;

        world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX(), pos.getY(), pos.getZ(), 4, 0.1, 0.2, 0.1, 0.05);
        world.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 0.6f);
    }


    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if(world.dimensionType().ultraWarm()) return;
        if(world instanceof ServerLevel && state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) {
            int randomInteger = random.nextInt(512);
            int offset = 8 + (world.getBlockState(pos.below()).is(Blocks.DEEPSLATE) || world.isRainingAt(pos) ? 48 : 0);
            if (randomInteger > 512 + Math.min(pos.getY() + 16, 0) - offset) {
                this.tickTorchDegradation(state, world, pos, false);
            }
        }
    }

    @Override
    public void handlePrecipitation(BlockState state, Level world, BlockPos pos, Biome.Precipitation precipitation) {
        if(world instanceof ServerLevel) {
            this.tickTorchDegradation(state, (ServerLevel)world, pos, true);
        }
    }
}
