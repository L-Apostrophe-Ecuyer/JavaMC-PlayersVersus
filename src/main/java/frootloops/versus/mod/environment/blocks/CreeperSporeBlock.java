package frootloops.versus.mod.environment.blocks;


import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.CustomSpecialEffects;
import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class CreeperSporeBlock extends SporeBlossomBlock implements Fertilizable {
    private static final int MAX_AGE = 2;
    public static final IntProperty AGE = Properties.AGE_2;
    private static final double SEARCH_RANGE = 16.0;

    private static long lastUpdateTime = 0L;

    public CreeperSporeBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(AGE, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected boolean hasRandomTicks(BlockState state) {
        return state.getFluidState().isEmpty();
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int currentAge = state.get(AGE).intValue();
        if(currentAge >= MAX_AGE) {

            boolean canSpawnCreeper = world.getBlockState(pos.down()).isAir() && world.getBlockState(pos.down(2)).isAir();
            if(!canSpawnCreeper) return;

            if(world.getTime() == lastUpdateTime) return;
            lastUpdateTime = world.getTime(); // To avoid multiple raycasts per tick

            PlayerEntity nearestPlayer = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), SEARCH_RANGE, true);
            if(nearestPlayer != null) {
                Vec3d blockPosDouble = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
                boolean isPlayerWithinEyesight =  world.raycast(new RaycastContext(nearestPlayer.getEyePos(), blockPosDouble, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, nearestPlayer)).getType() == HitResult.Type.MISS;
                if(isPlayerWithinEyesight) {

                    // Summon 2-4 creepers!
                    for(int i = 0; i < 2 + random.nextInt(2); i++) {
                        CreeperEntity creeper = new CreeperEntity(EntityType.CREEPER, world);
                        creeper.setPos(pos.getX(), pos.getY() - 1.5, pos.getZ());
                        world.spawnEntity(creeper);
                    }
                    world.playSound((PlayerEntity)null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_CREEPER_HURT, SoundCategory.BLOCKS, 1.0F, 0.8f + random.nextFloat() * 0.4f);
                    world.spawnParticles(ParticleTypes.SNEEZE, pos.getX(), pos.getY(), pos.getZ(), 32, 0.5, 0.5, 0.5, 0.1);
                    world.spawnParticles(ParticleTypes.POOF, pos.getX(), pos.getY(), pos.getZ(), 6, 0.5, 0.5, 0.5, 0.1);

                    // Reset age:
                    world.setBlockState(pos, state.with(AGE, 0), Block.NOTIFY_LISTENERS);
                }
            }
        }
        else {
            world.setBlockState(pos, state.with(AGE,currentAge + 1), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        return state.getFluidState().isEmpty();
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        int currentAge = state.get(AGE, 0);
        if(currentAge == MAX_AGE) world.setBlockState(pos, Blocks.SPORE_BLOSSOM.getDefaultState(), Block.NOTIFY_LISTENERS); // Set to regular pink spore blossom
        else world.setBlockState(pos, state.with(AGE,currentAge + 1), Block.NOTIFY_LISTENERS);
    }
}
