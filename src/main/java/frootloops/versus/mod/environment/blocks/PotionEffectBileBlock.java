package frootloops.versus.mod.environment.blocks;

import frootloops.versus.VersusMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;


public class PotionEffectBileBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 0.0, 16.0);

    private final ColorParticleOption PARTICLE;
    private final int MAX_DURATION, AMPLIFIER;
    private final float AMBIENT_OCCLUSION_AMOUNT;
    private final Holder<MobEffect> effect;

    public PotionEffectBileBlock(Properties settings, int color, Holder<MobEffect> statusEffectToGrant, int maxDuration, int amplifier, float ambientOcclusion) {
        super(settings.noCollision());
        this.effect = statusEffectToGrant;
        this.PARTICLE = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color);
        this.MAX_DURATION = Math.min(210, maxDuration);
        this.AMPLIFIER = amplifier;
        this.AMBIENT_OCCLUSION_AMOUNT = ambientOcclusion;
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier handler, boolean bl) {
        if (!world.isClientSide() && !entity.isSpectator() && entity instanceof LivingEntity livingEntity) {
            if(entity.fallDistance > 1.0 && (entity instanceof Player || (!world.isClientSide() && ((ServerLevel)world).getGameRules().get(GameRules.MOB_GRIEFING)) && entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight() > 0.512F)) {
                this.grantStatusEffect(livingEntity, true);
                super.entityInside(state, world, pos, entity, handler, bl);
                world.destroyBlock(pos, false);
            }
            else if(world.getGameTime() % 10L == 0 || entity.fallDistance > 0.0){
                this.grantStatusEffect(livingEntity, false);
            }
        }
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    private void grantStatusEffect(LivingEntity entity, boolean extraStrongEffect) {
        if(effect == null) {
            VersusMod.MOD_LOGGER.error("[ERROR] Status effect isn't registered properly for PotionEffectBileBlock: " + effect.getRegisteredName());
            return;
        }
        if (entity.isSteppingCarefully()) return;
        if(effect == MobEffects.INSTANT_DAMAGE && !entity.level().isClientSide()) entity.hurtServer((ServerLevel) entity.level(), entity.damageSources().magic(), extraStrongEffect ? 2 : 1);
        else if(effect == MobEffects.INSTANT_HEALTH) entity.heal(extraStrongEffect ? 2 : 1);
        else {
            int duration = 20 + (extraStrongEffect ? 40 : 0);
            if(entity.hasEffect(effect)) {
                int currentDuration = entity.getEffect(effect).getDuration();
                if(currentDuration > MAX_DURATION) return;
                else duration = 15 + currentDuration;
            }
            entity.addEffect(new MobEffectInstance(effect, duration, AMPLIFIER));
        }
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        double randomDouble = random.nextDouble();
        if(randomDouble < 0.1) {
            double d = (double) pos.getX() + 0.25 + randomDouble * 0.5;
            double e = (double) pos.getY() + 0.5;
            double f = (double) pos.getZ() + 0.25 + random.nextDouble() * 0.5;
            world.addParticle(PARTICLE, d, e, f, 0.0, 0.05, 0.0);
        }
    }

    @Override
    protected BlockState updateShape(
            BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random
    ) {
        return !state.canSurvive(world, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return world.getFluidState(pos).isEmpty() && (world.getBlockState(pos.below()).isFaceSturdy(world, pos, Direction.DOWN));
    }

    @Override
    public void playerDestroy(ServerLevel world, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        player.awardStat(Stats.BLOCK_MINED.get(this)); // Nothing dropped
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return AMBIENT_OCCLUSION_AMOUNT;
    }
}
