package frootloops.versus.mod.environment.blocks;


import frootloops.versus.VersusMod;
import net.minecraft.world.level.block.BonemealSource;
import frootloops.versus.mod.environment.CustomSpecialEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SporeBlossomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class CreeperSporeBlock extends SporeBlossomBlock implements BonemealableBlock {
    private static final int MAX_AGE = 2;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_2;
    private static final double SEARCH_RANGE = 16.0;

    private static long lastUpdateTime = 0L;

    public CreeperSporeBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getFluidState().isEmpty();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        int currentAge = state.getValue(AGE).intValue();
        if(currentAge >= MAX_AGE) {

            boolean canSpawnCreeper = world.getBlockState(pos.below()).isAir() && world.getBlockState(pos.below(2)).isAir();
            if(!canSpawnCreeper) return;

            if(world.getGameTime() == lastUpdateTime) return;
            lastUpdateTime = world.getGameTime(); // To avoid multiple raycasts per tick

            Player nearestPlayer = world.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), SEARCH_RANGE, true);
            if(nearestPlayer != null) {
                Vec3 blockPosDouble = new Vec3(pos.getX(), pos.getY(), pos.getZ());
                boolean isPlayerWithinEyesight =  world.clip(new ClipContext(nearestPlayer.getEyePosition(), blockPosDouble, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, nearestPlayer)).getType() == HitResult.Type.MISS;
                if(isPlayerWithinEyesight) {

                    // Summon 2-4 creepers!
                    for(int i = 0; i < 2 + random.nextInt(2); i++) {
                        Creeper creeper = new Creeper(EntityTypes.CREEPER, world);
                        creeper.setPosRaw(pos.getX(), pos.getY() - 1.5, pos.getZ());
                        world.addFreshEntity(creeper);
                    }
                    world.playSound((Player)null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.CREEPER_HURT, SoundSource.BLOCKS, 1.0F, 0.8f + random.nextFloat() * 0.4f);
                    world.sendParticles(ParticleTypes.SNEEZE, pos.getX(), pos.getY(), pos.getZ(), 32, 0.5, 0.5, 0.5, 0.1);
                    world.sendParticles(ParticleTypes.POOF, pos.getX(), pos.getY(), pos.getZ(), 6, 0.5, 0.5, 0.5, 0.1);

                    // Reset age:
                    world.setBlock(pos, state.setValue(AGE, 0), Block.UPDATE_CLIENTS);
                }
            }
        }
        else {
            world.setBlock(pos, state.setValue(AGE,currentAge + 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, BonemealSource source) {
        return state.getFluidState().isEmpty();
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        int currentAge = state.getValueOrElse(AGE, 0);
        if(currentAge == MAX_AGE) world.setBlock(pos, Blocks.SPORE_BLOSSOM.defaultBlockState(), Block.UPDATE_CLIENTS); // Set to regular pink spore blossom
        else world.setBlock(pos, state.setValue(AGE,currentAge + 1), Block.UPDATE_CLIENTS);
    }
}
