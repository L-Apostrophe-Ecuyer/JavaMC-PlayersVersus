package frootloops.versus.mod.environment.blocks;

import frootloops.versus.VersusMod;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

import java.util.function.ToIntFunction;


public class PotionEffectBileBlock extends Block {

    private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);

    private final EntityEffectParticleEffect PARTICLE;
    private final int DURATION, AMPLIFIER;
    private final float AMBIENT_OCCLUSION_AMOUNT;
    private RegistryEntry<StatusEffect> effect;

    public PotionEffectBileBlock(Settings settings, int color, RegistryEntry<StatusEffect> statusEffectToGrant, int duration, int amplifier, float ambientOcclusion) {
        super(settings);
        this.effect = statusEffectToGrant;
        this.PARTICLE = EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, color);
        this.DURATION = duration;
        this.AMPLIFIER = amplifier;
        this.AMBIENT_OCCLUSION_AMOUNT = ambientOcclusion;
    }

    public static ToIntFunction<BlockState> getLuminanceSupplier(int luminance) {
        return (state) -> luminance;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && !entity.isSpectator() && entity.getBlockStateAtPos().isOf(this) && entity instanceof LivingEntity livingEntity) {
            if(entity.fallDistance > 0.65 && (entity instanceof PlayerEntity || (!world.isClient && ((ServerWorld)world).getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) && entity.getWidth() * entity.getWidth() * entity.getHeight() > 0.512F)) {
                this.grantStatusEffect(livingEntity, true);
                world.breakBlock(pos, false);
            }
            else if(world.getTime() % 20L == 0 || entity.fallDistance > 0.0){
                this.grantStatusEffect(livingEntity, false);
            }
        }
        super.onEntityCollision(state, world, pos, entity);
    }

    private void grantStatusEffect(LivingEntity entity, boolean extraStrongEffect) {
        if(effect == null) {
            VersusMod.MOD_LOGGER.error("[ERROR] Status effect isn't registered properly for PotionEffectBileBlock: " + effect.getIdAsString());
            return;
        }
        if (entity.bypassesSteppingEffects() || entity.hasStatusEffect(effect)) return;
        if(effect == StatusEffects.INSTANT_DAMAGE && !entity.getWorld().isClient) entity.damage((ServerWorld) entity.getWorld(), entity.getDamageSources().magic(), extraStrongEffect ? 2 : 1);
        else if(effect == StatusEffects.INSTANT_HEALTH) entity.heal(extraStrongEffect ? 2 : 1);
        else entity.addStatusEffect(new StatusEffectInstance(effect, extraStrongEffect ? DURATION + 20: DURATION, AMPLIFIER));
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        double randomDouble = random.nextDouble();
        if(randomDouble < 0.1) {
            double d = (double) pos.getX() + 0.25 + randomDouble * 0.5;
            double e = (double) pos.getY() + 0.5;
            double f = (double) pos.getZ() + 0.25 + random.nextDouble() * 0.5;
            world.addParticle(PARTICLE, true, d, e, f, 0.0, 0.05, 0.0);
        }
    }

    @Override
    protected BlockState getStateForNeighborUpdate(
            BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random
    ) {
        return !state.canPlaceAt(world, pos)
                ? Blocks.AIR.getDefaultState()
                : super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return world.getFluidState(pos).isEmpty() && (world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos, Direction.DOWN));
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        player.incrementStat(Stats.MINED.getOrCreateStat(this)); // Nothing dropped
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return AMBIENT_OCCLUSION_AMOUNT;
    }
}
