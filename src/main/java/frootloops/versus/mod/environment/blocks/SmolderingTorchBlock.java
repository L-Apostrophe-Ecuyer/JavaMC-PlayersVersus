package frootloops.versus.mod.environment.blocks;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.TorchBlock;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class SmolderingTorchBlock extends TorchBlock {

    public SmolderingTorchBlock(DefaultParticleType particle, Settings settings) {
        super(particle, settings);
    }

    public void tickSmolderingTorchDegradation(BlockState state, ServerWorld world, BlockPos pos) {
        if(world.getDimension().ultrawarm()) return;
        world.setBlockState(pos, CustomBlocks.EXTINGUISHED_TORCH.getStateWithProperties(state));
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX(), pos.getY(), pos.getZ(), 8, 0.1, 0.2, 0.1, 0.03);
    }


    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if(pos.getY() > -32) return;
        if(world instanceof ServerWorld) {
            if (random.nextInt(10) > 8) {
                this.tickSmolderingTorchDegradation(state, world, pos);
            }
        }
        world.playSoundAtBlockCenter(pos, SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 0.8f, 0.8f, true);
    }

    @Override
    public void precipitationTick(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation) {
        if(world instanceof ServerWorld) this.tickSmolderingTorchDegradation(state, (ServerWorld)world, pos);
        world.playSoundAtBlockCenter(pos, SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 0.8f, 0.8f, true);
    }
}
