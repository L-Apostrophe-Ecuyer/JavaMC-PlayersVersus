package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.VersusMod;
import frootloops.versus.mixin.LivingEntityAccessor;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import frootloops.versus.mod.environment.blocks.clays.CustomMudBlock;
import net.minecraft.block.*;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.FlyingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.passive.TameableShoulderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.OptionalInt;

@Mixin(LeavesBlock.class)
public abstract class LeavesMixin extends Block implements Waterloggable {

    public LeavesMixin(Settings settings) {
        super(settings);
    }

    private static final float FALL_DISTANCE_TO_FALL_THROUGH = 6.0F;
    private static final float FALL_DISTANCE_REDUCTION = 4.0F;
    private static Vec3d MOVE_TOWARDS_CENTER_MULT = new Vec3d(0.5, 1.1, 0.5);
    private static Vec3d SNEAKING_MULT = new Vec3d(0.8, 0.5, 0.8);

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (context instanceof EntityShapeContext && ((EntityShapeContext) context).getEntity() instanceof LivingEntity livingEntity) {
            if(livingEntity.hasVehicle() || livingEntity.isSneaking() || livingEntity instanceof SpiderEntity || livingEntity instanceof FlyingEntity || livingEntity instanceof TameableShoulderEntity) return VoxelShapes.fullCube();
            if(!context.isAbove(VoxelShapes.fullCube(), pos, true)) return VoxelShapes.empty();
        }
        return VoxelShapes.fullCube();
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if(entity instanceof FallingBlockEntity fallingBlock) {
            if(fallingBlock.getBlockState().getSoundGroup() != BlockSoundGroup.ANVIL) return;
            Block.dropStacks(state, world, pos);
            world.removeBlock(pos, false);
        }
        else if(entity instanceof LivingEntity livingEntity) {
            if(!state.getFluidState().isEmpty() && EnchantRegistryHelper.hasEnchantment(livingEntity.getEquippedStack(EquipmentSlot.FEET), Enchantments.FROST_WALKER)) {
                Block.dropStacks(state, world, pos);
                world.setBlockState(pos, Blocks.FROSTED_ICE.getDefaultState());
                world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, Blocks.FROSTED_ICE.getDefaultState()));
                super.onLandedUpon(world, state, pos, livingEntity, livingEntity.fallDistance);
            }
            else if(fallDistance > 4) {
                livingEntity.fallDistance = livingEntity.fallDistance / FALL_DISTANCE_REDUCTION;
                super.onLandedUpon(world, state, pos, livingEntity, livingEntity.fallDistance);
            }
        }
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (entity.hasVehicle()) return;
        if (!entity.getBlockPos().equals(pos) && !entity.getBlockPos().up().equals(pos)) return;
        if (!entity.isSpectator() && entity instanceof LivingEntity livingEntity && entity.getBlockPos().equals(pos) && !((LivingEntityAccessor)livingEntity).isJumping()) {
            if(livingEntity.isSneaking()) entity.slowMovement(state, SNEAKING_MULT);
            else if(Combat.isLookingTowards(livingEntity, pos.toCenterPos(), -0.25)) entity.slowMovement(state, MOVE_TOWARDS_CENTER_MULT);
        }
    }

    @Redirect(method = "getStateForNeighborUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/LeavesBlock;getDistanceFromLog(Lnet/minecraft/block/BlockState;)I"))
    private int getDistanceFromLog_OnUpdate(BlockState state) {
        boolean hasMatchingLog = LEAVES_WITH_MATCHING_LOG.getOrDefault(this, false);
        if(hasMatchingLog && LOG_MAP.get(state.getBlock()) == this) return 0;
        else if(!hasMatchingLog && state.isIn(BlockTags.LOGS)) return 0;
        else if(state.getBlock() == this) return state.get(LeavesBlock.DISTANCE);
        return 7;
    }

    @Redirect(method = "getPlacementState", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/LeavesBlock;updateDistanceFromLogs(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private BlockState updateDistanceFromLogs_OnPlace(BlockState state, WorldAccess world, BlockPos pos) {
        if(state.get(LeavesBlock.PERSISTENT)) return state;
        return state.with(LeavesBlock.DISTANCE, getUpdatedDistance(state, world, pos));
    }

    private int getUpdatedDistance(BlockState state, WorldAccess world, BlockPos pos) {
        int i = 7;
        boolean hasMatchingLog = LEAVES_WITH_MATCHING_LOG.getOrDefault(this, false);
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (Direction direction : Direction.values()) {
            mutable.set(pos, direction);
            BlockState neighborState = world.getBlockState(mutable);
            if(hasMatchingLog && LOG_MAP.get(neighborState.getBlock()) == this) i = 1;
            else if(!hasMatchingLog && neighborState.isIn(BlockTags.LOGS)) i = 1;
            else if(neighborState.isOf(state.getBlock())){
                int d = getDistanceFromLog_OnUpdate(world.getBlockState(mutable));
                i = Math.min(i, d + 1);
            }
            if (i == 1) break;
        }
        if(i > LEAVES_WITH_MAX_DISTANCE.getOrDefault(this, 7)) return 7;
        return i;
    }

    @Overwrite
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if(!(Boolean)state.get(LeavesBlock.PERSISTENT)) {
            int currentDistance = state.get(LeavesBlock.DISTANCE);
            int newDistance = getUpdatedDistance(state, world, pos);
            if(currentDistance == 1 && newDistance > 1) newDistance = 7;
            if(currentDistance != newDistance) world.setBlockState(pos, state.with(LeavesBlock.DISTANCE, newDistance), Block.NOTIFY_ALL);
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
