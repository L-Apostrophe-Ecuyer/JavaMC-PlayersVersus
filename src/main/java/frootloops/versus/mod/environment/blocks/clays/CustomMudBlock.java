package frootloops.versus.mod.environment.blocks.clays;

import frootloops.versus.VersusMod;
import frootloops.versus.mixin.LivingEntityAccessor;
import frootloops.versus.mod.environment.CustomDamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CustomMudBlock extends MoistBlock {

    protected static final VoxelShape ITEM_COLLISION_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0);

    public CustomMudBlock(Properties settings, Block dryVersion, Block cookedVersion) {
        super(settings, dryVersion, cookedVersion);
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier handler, boolean bl) {
        if (!entity.isSpectator() && entity instanceof LivingEntity livingEntity && entity.getInBlockState().is(this) && !canWalkOnWetMud(entity)) {

            // When a player goes inside mud, break a fragile block that was on top:
            double entityRelativeY = entity.getY() - Math.floor(entity.getY());
            Block blockOnTop = world.getBlockState(pos.above()).getBlock();
            if(entityRelativeY < 0.6) {
                if(blockOnTop.defaultDestroyTime() < 0.2f || blockOnTop instanceof VegetationBlock) world.destroyBlock(pos.above(), true);
            }

            // When an entity has their head inside of mud, they might take some damage
            boolean canEntityBeDamaged = shouldDamage(world, livingEntity);

            // When an entity is jumping, they should be able to get out of the block, or at least stop falling:
            if(((LivingEntityAccessor)livingEntity).isJumping())  {
                Vec3 velocity = entity.getDeltaMovement();
                if(entity.isInLiquid()) {
                    entity.makeStuckInBlock(state, new Vec3(1.1, 1.0, 1.1));
                    if(world.getGameTime() % 20L == 0) entity.playSound(this.soundType.getStepSound(), this.soundType.getVolume() * 0.5F, this.soundType.getPitch() * 0.75F);
                }
                else if(entityRelativeY < 0.95 || blockOnTop.defaultDestroyTime() > 0.0) {
                    entity.setDeltaMovement(velocity.x, 0.03, velocity.z);
                    if(world.getGameTime() % 20L == 0) entity.playSound(this.soundType.getStepSound(), this.soundType.getVolume() * 0.5F, this.soundType.getPitch() * 0.75F);
                }
                else if(velocity.y < 0.12){
                    entity.setDeltaMovement(velocity.x, 0.3, velocity.z);
                    entity.playSound(this.soundType.getFallSound(), this.soundType.getVolume() * 0.5F, this.soundType.getPitch() * 0.75F);
                }
            }
            else if(entityRelativeY > 0.95 && entity.getDeltaMovement().y >= 0.0) {
                // When still jumping dont interrupt
            }

            // Otherwise, when the entity moves, they'll get hurt:
            else if(hasEntityMoved(entity) || entity.isDiscrete()) {
                if(canEntityBeDamaged && world.getGameTime() % 20L == 0 && !world.isClientSide()) {
                    entity.hurtServer((ServerLevel) world, CustomDamageSources.getMudSuffocation(world), 1); // Damage every second while moving
                    entity.playSound(this.soundType.getHitSound(), this.soundType.getVolume() * 0.5F, this.soundType.getPitch() * 0.75F);
                }
                else if(world.getGameTime() % 10L == 0) entity.playSound(this.soundType.getHitSound(), this.soundType.getVolume() * 0.5F, this.soundType.getPitch() * 0.75F);
                entity.makeStuckInBlock(state, new Vec3(0.995, 0.45, 0.995));
                return; // To avoid dealing damage twice
            }
            else if(entityRelativeY < 0.95) {
                entity.makeStuckInBlock(state, new Vec3(0.98, 0.04, 0.98));
            }

            // Suffocation damage from waiting, every 4 seconds
            if(canEntityBeDamaged && !world.isClientSide() && world.getGameTime() % 80L == 0) {
                entity.hurtServer((ServerLevel) world, CustomDamageSources.getMudSuffocation(world), 1);
            }
        }
    }

    @Override
    public void fallOn(Level world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if(entity instanceof FallingBlockEntity fallingBlock) {
            if(fallDistance < MIN_FALL_DISTANCE_TO_DRY && fallingBlock.getBlockState().getSoundType() != SoundType.ANVIL) return;
            world.setBlockAndUpdate(pos, this.getDryVersion().defaultBlockState());
            world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, this.getDryVersion().defaultBlockState()));
            super.fallOn(world, state, pos, entity, fallDistance);
        }
        else if(entity instanceof LivingEntity && fallDistance > MIN_FALL_DISTANCE_TO_DRY) {
            if(fallDistance < 20f && entity.is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) return;
            entity.fallDistance = entity.fallDistance - MIN_FALL_DISTANCE_TO_DRY;
            if(fallDistance < MIN_FALL_DISTANCE_TO_DRY) return;
            world.setBlockAndUpdate(pos, this.getDryVersion().defaultBlockState());
            world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, this.getDryVersion().defaultBlockState()));
            super.fallOn(world, state, pos, entity, entity.fallDistance);
        }
    }


    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Entity entity;
        if (context instanceof EntityCollisionContext && (entity = ((EntityCollisionContext) context).getEntity()) != null) {
            if(!context.isAbove(Shapes.block(), pos, true)) Shapes.empty();
            if(entity instanceof LivingEntity livingEntity) return (livingEntity.fallDistance > MIN_FALL_DISTANCE_TO_DRY || CustomMudBlock.canWalkOnWetMud(livingEntity)) ? Shapes.block() : Shapes.empty();
        }
        return ITEM_COLLISION_SHAPE;
    }

    protected VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }


    private boolean shouldDamage(Level world, LivingEntity entity) {
        if(entity instanceof Animal) return false;
        return world.getBlockState(new BlockPos(entity.getBlockX(), (int) (entity.getEyeY() - 0.04), entity.getBlockZ())).is(this);
    }

    private boolean hasEntityMoved(Entity entity) {
        return entity.xOld != entity.getX() || entity.zOld != entity.getZ(); // || entity.lastYaw != entity.getYaw() || entity.lastPitch != entity.getPitch();
    }

    public static boolean canWalkOnWetMud(Entity entity) {
        if (entity.isInLiquid() && !entity.isAlwaysTicking()) return true; // Unrealistic but fairly useful!
        if (entity instanceof Pig || entity instanceof TamableAnimal || entity.is(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS)) return true;
        if (entity instanceof LivingEntity) return ((LivingEntity)entity).getItemBySlot(EquipmentSlot.FEET).is(Items.LEATHER_BOOTS);
        return false;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter world, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return 0.2f;
    }
}
