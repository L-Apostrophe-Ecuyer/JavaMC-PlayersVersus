package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import frootloops.versus.mod.environment.blocks.BlockSounds;
import frootloops.versus.mod.environment.blocks.leaves.LeafLitter;
import frootloops.versus.mod.environment.blocks.leaves.LeafMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
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

import java.util.HashMap;

/**
 * Leaves you can push through, like scaffolding ({@link LeafMovement}): this decides when a leaf is solid, what landing on
 * one does, and when leaves shed litter ({@link LeafLitter}); the movement through them is {@code LeafMovementMixin}'s.
 * Leaves also only stay alive near their own tree's logs, and some trees' leaves reach less far from them.
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
            else super.fallOn(world, state, pos, livingEntity, fallDistance / VersusSettings.Leaves.LANDING_FALL_DIVISOR);
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
        boolean hasMatchingLog = LEAVES_WITH_MATCHING_LOG.getOrDefault(this, false);
        if(hasMatchingLog && LOG_MAP.get(state.getBlock()) == this) return 0;
        else if(!hasMatchingLog && state.is(BlockTags.LOGS)) return 0;
        else if(state.getBlock() == this) return state.getValue(LeavesBlock.DISTANCE);
        return 7;
    }

    @Redirect(method = "getStateForPlacement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/LeavesBlock;updateDistance(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState updateDistanceFromLogs_OnPlace(BlockState state, LevelAccessor world, BlockPos pos) {
        if(state.getValue(LeavesBlock.PERSISTENT)) return state;
        return state.setValue(LeavesBlock.DISTANCE, getUpdatedDistance(state, world, pos));
    }

    private int getUpdatedDistance(BlockState state, LevelAccessor world, BlockPos pos) {
        int i = 7;
        boolean hasMatchingLog = LEAVES_WITH_MATCHING_LOG.getOrDefault(this, false);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            mutable.setWithOffset(pos, direction);
            BlockState neighborState = world.getBlockState(mutable);
            if(hasMatchingLog && LOG_MAP.get(neighborState.getBlock()) == this) i = 1;
            else if(!hasMatchingLog && neighborState.is(BlockTags.LOGS)) i = 1;
            else if(neighborState.is(state.getBlock())){
                int d = getDistanceFromLog_OnUpdate(world.getBlockState(mutable));
                i = Math.min(i, d + 1);
            }
            if (i == 1) break;
        }
        if(i > LEAVES_WITH_MAX_DISTANCE.getOrDefault(this, 7)) return 7;
        return i;
    }

    @Overwrite
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if(!(Boolean)state.getValue(LeavesBlock.PERSISTENT)) {
            int currentDistance = state.getValue(LeavesBlock.DISTANCE);
            int newDistance = getUpdatedDistance(state, world, pos);
            if(currentDistance == 1 && newDistance > 1) newDistance = 7;
            if(currentDistance != newDistance) world.setBlock(pos, state.setValue(LeavesBlock.DISTANCE, newDistance), Block.UPDATE_ALL);
        }
    }

    private static final HashMap<Block, Integer> LEAVES_WITH_MAX_DISTANCE = new HashMap<>();
    static {
        LEAVES_WITH_MAX_DISTANCE.put(Blocks.OAK_LEAVES, 5);
        LEAVES_WITH_MAX_DISTANCE.put(Blocks.SPRUCE_LEAVES, 5);
        LEAVES_WITH_MAX_DISTANCE.put(Blocks.BIRCH_LEAVES, 5);
        LEAVES_WITH_MAX_DISTANCE.put(Blocks.DARK_OAK_LEAVES, 5);
        LEAVES_WITH_MAX_DISTANCE.put(Blocks.PALE_OAK_LEAVES, 5);
        LEAVES_WITH_MAX_DISTANCE.put(Blocks.ACACIA_LEAVES, 5);
        LEAVES_WITH_MAX_DISTANCE.put(Blocks.CHERRY_LEAVES, 5);
    }

    private static final HashMap<Block, Boolean> LEAVES_WITH_MATCHING_LOG = new HashMap<>();
    static {
        LEAVES_WITH_MATCHING_LOG.put(Blocks.OAK_LEAVES, true);
        LEAVES_WITH_MATCHING_LOG.put(Blocks.SPRUCE_LEAVES, true);
        LEAVES_WITH_MATCHING_LOG.put(Blocks.BIRCH_LEAVES, true);
        LEAVES_WITH_MATCHING_LOG.put(Blocks.DARK_OAK_LEAVES, true);
        LEAVES_WITH_MATCHING_LOG.put(Blocks.JUNGLE_LEAVES, true);
        LEAVES_WITH_MATCHING_LOG.put(Blocks.ACACIA_LEAVES, true);
        LEAVES_WITH_MATCHING_LOG.put(Blocks.CHERRY_LEAVES, true);
    }

    private static final HashMap<Block, Block> LOG_MAP = new HashMap<>();
    static {
        LOG_MAP.put(Blocks.OAK_LOG, Blocks.OAK_LEAVES);
        LOG_MAP.put(Blocks.OAK_WOOD, Blocks.OAK_LEAVES);
        LOG_MAP.put(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES);
        LOG_MAP.put(Blocks.SPRUCE_WOOD, Blocks.SPRUCE_LEAVES);
        LOG_MAP.put(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
        LOG_MAP.put(Blocks.BIRCH_WOOD, Blocks.BIRCH_LEAVES);
        LOG_MAP.put(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES);
        LOG_MAP.put(Blocks.DARK_OAK_WOOD, Blocks.DARK_OAK_LEAVES);
        LOG_MAP.put(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES);
        LOG_MAP.put(Blocks.JUNGLE_WOOD, Blocks.JUNGLE_LEAVES);
        LOG_MAP.put(Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES);
        LOG_MAP.put(Blocks.ACACIA_WOOD, Blocks.ACACIA_LEAVES);
        LOG_MAP.put(Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES);
        LOG_MAP.put(Blocks.CHERRY_WOOD, Blocks.CHERRY_LEAVES);
    }
}
