package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.VersusMod;
import frootloops.versus.mixin.LivingEntityAccessor;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;

@Mixin(LeavesBlock.class)
public abstract class LeavesMixin extends Block implements SimpleWaterloggedBlock {

    public LeavesMixin(Properties settings) {
        super(settings);
    }
    private static final double FALL_DISTANCE_REDUCTION = 3.0;
    private static final double MIN_VELOCITY_TO_BE_SOLID = -0.4;
    private static Vec3 NORMAL_MULT = new Vec3(0.6, 0.8, 0.6);
    private static Vec3 SNEAKING_MULT = new Vec3(0.8, 0.8, 0.8);
    private static Vec3 JUMPING_MULT = new Vec3(0.9, 1.0, 0.9);

    private static final VoxelShape COLLISION_SHAPE_INSIDE = Block.box(4.0, 4.0, 4.0, 12.0, 12.0, 12.0);

    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter world, BlockPos pos, Entity entity) {
        if(entity instanceof LivingEntity && entity.getDeltaMovement().y < MIN_VELOCITY_TO_BE_SOLID) return Shapes.empty();
        return COLLISION_SHAPE_INSIDE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext && ((EntityCollisionContext) context).getEntity() instanceof LivingEntity livingEntity) {
            if(livingEntity.isAlwaysTicking() && livingEntity.isFallFlying() && livingEntity.getDeltaMovement().lengthSqr() > 0.5) {
                livingEntity.fallDistance = -(livingEntity.getDeltaMovement().y() + 0.1) * 8.0;
                livingEntity.setDeltaMovement(livingEntity.getDeltaMovement().scale(0.95));
                return Shapes.empty();
            }
            if(livingEntity.isVehicle() || livingEntity.isPassenger() || livingEntity.getDeltaMovement().y < MIN_VELOCITY_TO_BE_SOLID) {
                livingEntity.fallDistance = -(livingEntity.getDeltaMovement().y() + 0.1) * 8.0;
                livingEntity.setDeltaMovement(livingEntity.getDeltaMovement().multiply(1.0, 0.95, 1.0));
                return Shapes.empty();
            }
            if(livingEntity instanceof Spider || livingEntity instanceof FlyingAnimal || livingEntity instanceof Animal) return Shapes.block();
            if(!context.isAbove(Shapes.block(), pos, true)) return Shapes.empty();
        }
        return Shapes.block();
    }

    @Override
    public void fallOn(Level world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if(entity instanceof FallingBlockEntity fallingBlock) {
            if(fallingBlock.getBlockState().getSoundType() != SoundType.ANVIL) return;
            Block.dropResources(state, world, pos);
            world.removeBlock(pos, false);
        }
        else if(entity instanceof LivingEntity livingEntity) {
            fallDistance = Math.max(fallDistance, -(livingEntity.getDeltaMovement().y() + 0.1) * 8.0);
            if(livingEntity.isAlwaysTicking() && livingEntity.isFallFlying() && livingEntity.getDeltaMovement().lengthSqr() > 0.5) {
                livingEntity.fallDistance = fallDistance;
                livingEntity.setDeltaMovement(livingEntity.getDeltaMovement().scale(0.95));
            }
            else if(livingEntity.getDeltaMovement().y < MIN_VELOCITY_TO_BE_SOLID) {
                livingEntity.fallDistance = fallDistance;
                livingEntity.setDeltaMovement(livingEntity.getDeltaMovement().x, -0.15, livingEntity.getDeltaMovement().z);
            }
            else if(!state.getFluidState().isEmpty() && EnchantRegistryHelper.hasEnchantment(livingEntity.getItemBySlot(EquipmentSlot.FEET), Enchantments.FROST_WALKER)) {
                world.destroyBlock(pos, true);
                world.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, Blocks.FROSTED_ICE.defaultBlockState()));
                super.fallOn(world, state, pos, livingEntity, fallDistance);
            }
            else {
                fallDistance = fallDistance / FALL_DISTANCE_REDUCTION;
                super.fallOn(world, state, pos, livingEntity, fallDistance);
            }
        }
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier handler, boolean bl) {
        if (entity.isPassenger() || entity.isSpectator()) return;
        if (!entity.blockPosition().equals(pos) && !entity.blockPosition().above().equals(pos)) return;
        if (entity instanceof LivingEntity livingEntity) {
            if(livingEntity.isAlwaysTicking() && livingEntity.isFallFlying()) return;
            else if(((LivingEntityAccessor)livingEntity).isJumping()) {
                Vec3 v = livingEntity.getDeltaMovement();
                if(livingEntity.onGround() || (v.y < -0.07 && v.y > -0.08))
                    livingEntity.setDeltaMovement(v.x * 0.9, 0.33, v.z * 0.9);
            }
            else {
                if (livingEntity.isShiftKeyDown()) entity.makeStuckInBlock(state, SNEAKING_MULT);
                else entity.makeStuckInBlock(state, NORMAL_MULT);
            }
        }
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
