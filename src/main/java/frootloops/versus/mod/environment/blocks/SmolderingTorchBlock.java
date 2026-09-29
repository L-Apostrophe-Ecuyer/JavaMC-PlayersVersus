package frootloops.versus.mod.environment.blocks;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SmolderingTorchBlock extends TorchBlock {

    public SmolderingTorchBlock(SimpleParticleType particle, Properties settings) {
        super(particle, settings);
    }

    public void tickSmolderingTorchDegradation(BlockState state, ServerLevel world, BlockPos pos) {
        if(world.dimensionType().ultraWarm()) return;
        world.setBlockAndUpdate(pos, CustomBlocks.EXTINGUISHED_TORCH.withPropertiesOf(state));
        world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX(), pos.getY(), pos.getZ(), 8, 0.1, 0.2, 0.1, 0.03);
        world.playLocalSound(pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 0.8f, true);
    }


    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        int y = pos.getY();
        if(y > 8) return;
        if(!world.isClientSide()) {
            if (random.nextInt(200) > y + 165) {
                this.tickSmolderingTorchDegradation(state, world, pos);
            }
        }
    }

    @Override
    public void handlePrecipitation(BlockState state, Level world, BlockPos pos, Biome.Precipitation precipitation) {
        if(!world.isClientSide()) this.tickSmolderingTorchDegradation(state, (ServerLevel)world, pos);
        world.playLocalSound(pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 0.8f, true);
    }
}
