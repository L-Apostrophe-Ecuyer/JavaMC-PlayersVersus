package frootloops.versus.mod.environment.blocks.clays;

import frootloops.versus.mixin.LivingEntityAccessor;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.CustomDamageSources;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;

public class BrownMudBlock extends MoistBlock {


    public BrownMudBlock(Settings settings) {
        super(settings, CustomBlocks.MUDSTONE);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!entity.isSpectator() && entity instanceof LivingEntity livingEntity && entity.getBlockStateAtPos().isOf(this) && !canWalkOnWetMud(entity)) {

            // When a player goes inside mud, break a fragile block that was on top:
            double entityRelativeY = entity.getY() - Math.floor(entity.getY());
            if(entityRelativeY < 0.6) {
                Block blockOnTop = world.getBlockState(pos.up()).getBlock();
                if(blockOnTop.getHardness() < 0.2f || blockOnTop instanceof PlantBlock) world.breakBlock(pos.up(), true);
            }

            // When an entity has their head inside of mud, make them drown:
            boolean canEntityBeDamaged = shouldDamage(world, livingEntity);
            if(canEntityBeDamaged && !world.isClient && world.getTime() % 30L == 0) {
                entity.damage((ServerWorld) world, CustomDamageSources.getMudSuffocation(world), 1);
            }

            // When an entity is jumping, they should be able to get out of the block, or at least stop falling:
            if(((LivingEntityAccessor)livingEntity).isJumping())  {
                if(entity.isInFluid()) {
                    Vec3d velocity = entity.getVelocity();
                    entity.slowMovement(state, new Vec3d(1.1, 1.0, 1.1));
                    entity.setVelocity(velocity.add(0.0, 0.03, 0.0));
                }
                else {
                    entity.slowMovement(state, new Vec3d(1.1, entityRelativeY < 0.9 ? 0.15 : 0.0, 1.1));
                }
            }

            // Otherwise, when the entity moves, they'll get hurt:
            else if(hasEntityMoved(entity)) {
                if(canEntityBeDamaged && world.getTime() % 5L == 0 && !world.isClient) entity.damage((ServerWorld) world, CustomDamageSources.getMudSuffocation(world), 1);
                entity.slowMovement(state, new Vec3d(0.98, 0.95, 0.98));
            }
            else {
                entity.slowMovement(state, new Vec3d(0.95, 0.6, 0.95));
            }

        }
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if(entity instanceof FallingBlockEntity fallingBlock) {
            BlockState result = (fallDistance > MIN_FALL_DISTANCE_TO_DRY + 2.0f || fallingBlock.getBlockState().getSoundGroup() == BlockSoundGroup.ANVIL) ? Blocks.PACKED_MUD.getDefaultState() : Blocks.DIRT.getDefaultState();
            world.setBlockState(pos, result);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, result));
            super.onLandedUpon(world, state, pos, entity, fallDistance);
        }
        else if(entity instanceof LivingEntity && fallDistance > MIN_FALL_DISTANCE_TO_DRY) {
            if(fallDistance < 20f && entity.getType().isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE)) return;
            entity.fallDistance = entity.fallDistance - MIN_FALL_DISTANCE_TO_DRY;

            BlockState blockState = fallDistance > MIN_FALL_DISTANCE_TO_DRY + 3f ? Blocks.PACKED_MUD.getDefaultState() :  Blocks.DIRT.getDefaultState();
            world.setBlockState(pos, blockState);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, blockState));
            super.onLandedUpon(world, state, pos, entity, fallDistance * 0.5F);
        }
    }


    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Entity entity;
        if (context instanceof EntityShapeContext && (entity = ((EntityShapeContext)context).getEntity()) != null && entity instanceof LivingEntity livingEntity) {
            return livingEntity.fallDistance > 5f || BrownMudBlock.canWalkOnWetMud(livingEntity) ? VoxelShapes.fullCube() : VoxelShapes.empty();
        }
        return VoxelShapes.fullCube();
    }

    protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }


    public boolean shouldDamage(World world, LivingEntity entity) {
        if(entity instanceof PigEntity) return false;
        return world.getBlockState(new BlockPos(entity.getBlockX(), (int) (entity.getEyeY() - 0.04), entity.getBlockZ())).isOf(this);
    }

    public boolean hasEntityMoved(Entity entity) {
        return entity.lastRenderX != entity.getX() || entity.lastRenderZ != entity.getZ() || entity.prevYaw != entity.getYaw() || entity.prevPitch != entity.getPitch();
    }

    public static boolean canWalkOnWetMud(Entity entity) {
        if (entity instanceof ItemEntity || entity instanceof WaterCreatureEntity || entity instanceof PigEntity || entity.getType().isIn(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS)) return true;
        if (entity instanceof LivingEntity) return ((LivingEntity)entity).getEquippedStack(EquipmentSlot.FEET).isOf(Items.LEATHER_BOOTS);
        return false;
    }

    @Override
    protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 0.2f;
    }
}
