package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import frootloops.versus.mod.environment.blocks.BlockSounds;
import frootloops.versus.mod.environment.blocks.leaves.LeafLitter;
import frootloops.versus.mod.environment.blocks.leaves.LeafMovement;
import frootloops.versus.mod.environment.blocks.leaves.LeafReach;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.sounds.BlockSoundSets;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leaves you can push through, like scaffolding ({@link LeafMovement}): this decides when a leaf is solid, what landing on
 * one does, and when leaves shed litter ({@link LeafLitter}); the movement through them is {@code LeafMovementMixin}'s.
 * Leaves also only stay alive near their own tree's logs ({@link LeafReach}), and a natural leaf cut off from them decays
 * as soon as it knows.
 */
@Mixin(LeavesBlock.class)
public abstract class LeavesMixin extends Block implements SimpleWaterloggedBlock {

    public LeavesMixin(Properties settings) {
        super(settings);
    }

    @Shadow
    protected abstract boolean decaying(BlockState state);

    /** Solid to stand on, open to push through, from what both sides know of the entity; asking changes nothing. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return LeafMovement.isSolid(pos, context) ? Shapes.block() : Shapes.empty();
    }

    /**
     * Landing on top of leaves (a fall of at most 2.5 blocks for what passes through them; any fall for animals and
     * spiders) divides the fall. A falling anvil breaks the leaves; other falling blocks land on them.
     */
    @Override
    public void fallOn(Level world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (entity instanceof FallingBlockEntity fallingBlock) {
            if (!BlockSounds.is(fallingBlock.getBlockState(), BlockSoundSets.ANVIL)) return;
            Block.dropResources(state, world, pos);
            world.removeBlock(pos, false);
        }
        else if (entity instanceof LivingEntity livingEntity) {
            if (!state.getFluidState().isEmpty() && EnchantRegistryHelper.hasEnchantment(livingEntity.getItemBySlot(EquipmentSlot.FEET), Enchantments.FROST_WALKER)) {
                world.destroyBlock(pos, true);
                world.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, Blocks.FROSTED_ICE.defaultBlockState()));
                super.fallOn(world, state, pos, livingEntity, fallDistance);
            }
            else {
                super.fallOn(world, state, pos, livingEntity, fallDistance / VersusSettings.Leaves.LANDING_FALL_DIVISOR);
            }
        }
    }

    /** On the server, natural leaves something pushes through shed litter now and then, the more the faster it goes. */
    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier handler, boolean bl) {
        if (world instanceof ServerLevel serverLevel && !entity.isSpectator() && LeafMovement.passes(entity)) {
            LeafLitter.disturb(serverLevel, pos, state, entity);
        }
    }

    /** A decaying leaf may leave a segment of litter on the ground below as it goes. */
    @Inject(method = "randomTick", at = @At("HEAD"))
    private void playersVersus$shedAsItDecays(BlockState state, ServerLevel world, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (this.decaying(state)) LeafLitter.decay(world, pos, state, random);
    }

    @Redirect(method = "updateShape", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/LeavesBlock;getDistanceAt(Lnet/minecraft/world/level/block/state/BlockState;)I"))
    private int getDistanceFromLog_OnUpdate(BlockState state) {
        if (playersVersus$isOwnLog(state)) return 0;
        if (LeafReach.isOwnKind(this, state)) return state.getValue(LeavesBlock.DISTANCE);
        return 7;
    }

    @Redirect(method = "getStateForPlacement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/LeavesBlock;updateDistance(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState updateDistanceFromLogs_OnPlace(BlockState state, LevelAccessor world, BlockPos pos) {
        if(state.getValue(LeavesBlock.PERSISTENT)) return state;
        return state.setValue(LeavesBlock.DISTANCE, getUpdatedDistance(state, world, pos));
    }

    private int getUpdatedDistance(BlockState state, LevelAccessor world, BlockPos pos) {
        int i = 7;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            mutable.setWithOffset(pos, direction);
            BlockState neighborState = world.getBlockState(mutable);
            if (playersVersus$isOwnLog(neighborState)) i = 1;
            else if (LeafReach.isOwnKind(this, neighborState)) i = Math.min(i, neighborState.getValue(LeavesBlock.DISTANCE) + 1);
            if (i == 1) break;
        }
        return i;
    }

    /** Whether this block is a log that keeps these leaves alive ({@link LeafReach}). */
    private boolean playersVersus$isOwnLog(BlockState log) {
        return LeafReach.isOwnLog(this, log);
    }

    /**
     * A natural leaf works out how far it is from its tree's logs, from what its neighbours remember. When that says its
     * hold on the tree got weaker, it searches for the tree itself ({@link LeafReach#searchDistance}) and, cut off, decays
     * at once; a felled tree's crown falls a block a tick. Without instant decay, a leaf that was by a log and no longer
     * is goes straight to 7, to decay on a random tick.
     */
    @Overwrite
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (state.getValue(LeavesBlock.PERSISTENT)) return;
        int currentDistance = state.getValue(LeavesBlock.DISTANCE);
        int newDistance = getUpdatedDistance(state, world, pos);
        if (VersusSettings.Leaves.DO_INSTANT_LEAF_DECAY) {
            if (newDistance > currentDistance || newDistance == 7) {
                int searched = LeafReach.searchDistance(world, pos, this);
                if (searched == 7) {
                    playersVersus$decayNow(state, world, pos, random);
                    return;
                }
                if (searched != LeafReach.UNKNOWN) newDistance = searched;
            }
        } else if (currentDistance == 1 && newDistance > 1) newDistance = 7;
        if (currentDistance != newDistance) world.setBlock(pos, state.setValue(LeavesBlock.DISTANCE, newDistance), Block.UPDATE_ALL);
    }

    /** A leaf cut off from its tree goes: vanilla's drops, by chance a segment of litter, and a puff of leaf, no sound. */
    private void playersVersus$decayNow(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        LeafLitter.decay(world, pos, state, random);
        Block.dropResources(state, world, pos);
        world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.0);
        world.removeBlock(pos, false);
    }
}
