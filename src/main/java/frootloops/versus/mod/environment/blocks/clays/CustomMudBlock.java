package frootloops.versus.mod.environment.blocks.clays;

import frootloops.versus.VersusMod;
import frootloops.versus.mixin.LivingEntityAccessor;
import frootloops.versus.mod.environment.CustomDamageSources;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.passive.*;
import net.minecraft.fluid.WaterFluid;
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

public class CustomMudBlock extends MoistBlock {

    protected static final VoxelShape ITEM_COLLISION_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 14.0, 16.0);

    public CustomMudBlock(Settings settings, Block dryVersion, Block cookedVersion) {
        super(settings, dryVersion, cookedVersion);
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity, EntityCollisionHandler handler, boolean bl) {
        if (!entity.isSpectator() && entity instanceof LivingEntity livingEntity && entity.getBlockStateAtPos().isOf(this) && !canWalkOnWetMud(entity)) {

            // When a player goes inside mud, break a fragile block that was on top:
            double entityRelativeY = entity.getY() - Math.floor(entity.getY());
            Block blockOnTop = world.getBlockState(pos.up()).getBlock();
            if(entityRelativeY < 0.6) {
                if(blockOnTop.getHardness() < 0.2f || blockOnTop instanceof PlantBlock) world.breakBlock(pos.up(), true);
            }

            // When an entity has their head inside of mud, they might take some damage
            boolean canEntityBeDamaged = shouldDamage(world, livingEntity);

            // When an entity is jumping, they should be able to get out of the block, or at least stop falling:
            if(((LivingEntityAccessor)livingEntity).isJumping())  {
                Vec3d velocity = entity.getVelocity();
                if(entity.isInFluid()) {
                    entity.slowMovement(state, new Vec3d(1.1, 1.0, 1.1));
                    if(world.getTime() % 20L == 0) entity.playSound(this.soundGroup.getStepSound(), this.soundGroup.getVolume() * 0.5F, this.soundGroup.getPitch() * 0.75F);
                }
                else if(entityRelativeY < 0.95 || blockOnTop.getHardness() > 0.0) {
                    entity.setVelocity(velocity.x, 0.03, velocity.z);
                    if(world.getTime() % 20L == 0) entity.playSound(this.soundGroup.getStepSound(), this.soundGroup.getVolume() * 0.5F, this.soundGroup.getPitch() * 0.75F);
                }
                else if(velocity.y < 0.12){
                    entity.setVelocity(velocity.x, 0.3, velocity.z);
                    entity.playSound(this.soundGroup.getFallSound(), this.soundGroup.getVolume() * 0.5F, this.soundGroup.getPitch() * 0.75F);
                }
            }
            else if(entityRelativeY > 0.95 && entity.getVelocity().y >= 0.0) {
                // When still jumping dont interrupt
            }

            // Otherwise, when the entity moves, they'll get hurt:
            else if(hasEntityMoved(entity) || entity.isSneaky()) {
                if(canEntityBeDamaged && world.getTime() % 20L == 0 && !world.isClient()) {
                    entity.damage((ServerWorld) world, CustomDamageSources.getMudSuffocation(world), 1); // Damage every second while moving
                    entity.playSound(this.soundGroup.getHitSound(), this.soundGroup.getVolume() * 0.5F, this.soundGroup.getPitch() * 0.75F);
                }
                else if(world.getTime() % 10L == 0) entity.playSound(this.soundGroup.getHitSound(), this.soundGroup.getVolume() * 0.5F, this.soundGroup.getPitch() * 0.75F);
                entity.slowMovement(state, new Vec3d(0.995, 0.45, 0.995));
                return; // To avoid dealing damage twice
            }
            else if(entityRelativeY < 0.95) {
                entity.slowMovement(state, new Vec3d(0.98, 0.04, 0.98));
            }

            // Suffocation damage from waiting, every 4 seconds
            if(canEntityBeDamaged && !world.isClient() && world.getTime() % 80L == 0) {
                entity.damage((ServerWorld) world, CustomDamageSources.getMudSuffocation(world), 1);
            }
        }
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if(entity instanceof FallingBlockEntity fallingBlock) {
            if(fallDistance < MIN_FALL_DISTANCE_TO_DRY && fallingBlock.getBlockState().getSoundGroup() != BlockSoundGroup.ANVIL) return;
            world.setBlockState(pos, this.getDryVersion().getDefaultState());
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, this.getDryVersion().getDefaultState()));
            super.onLandedUpon(world, state, pos, entity, fallDistance);
        }
        else if(entity instanceof LivingEntity && fallDistance > MIN_FALL_DISTANCE_TO_DRY) {
            if(fallDistance < 20f && entity.getType().isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE)) return;
            entity.fallDistance = entity.fallDistance - MIN_FALL_DISTANCE_TO_DRY;
            if(fallDistance < MIN_FALL_DISTANCE_TO_DRY) return;
            world.setBlockState(pos, this.getDryVersion().getDefaultState());
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, this.getDryVersion().getDefaultState()));
            super.onLandedUpon(world, state, pos, entity, entity.fallDistance);
        }
    }


    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Entity entity;
        if (context instanceof EntityShapeContext && (entity = ((EntityShapeContext) context).getEntity()) != null) {
            if(!context.isAbove(VoxelShapes.fullCube(), pos, true)) VoxelShapes.empty();
            if(entity instanceof LivingEntity livingEntity) return (livingEntity.fallDistance > MIN_FALL_DISTANCE_TO_DRY || CustomMudBlock.canWalkOnWetMud(livingEntity)) ? VoxelShapes.fullCube() : VoxelShapes.empty();
        }
        return ITEM_COLLISION_SHAPE;
    }

    protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }


    private boolean shouldDamage(World world, LivingEntity entity) {
        if(entity instanceof AnimalEntity) return false;
        return world.getBlockState(new BlockPos(entity.getBlockX(), (int) (entity.getEyeY() - 0.04), entity.getBlockZ())).isOf(this);
    }

    private boolean hasEntityMoved(Entity entity) {
        return entity.lastRenderX != entity.getX() || entity.lastRenderZ != entity.getZ(); // || entity.lastYaw != entity.getYaw() || entity.lastPitch != entity.getPitch();
    }

    public static boolean canWalkOnWetMud(Entity entity) {
        if (entity.isInFluid() && !entity.isPlayer()) return true; // Unrealistic but fairly useful!
        if (entity instanceof PigEntity || entity instanceof TameableEntity || entity.getType().isIn(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS)) return true;
        if (entity instanceof LivingEntity) return ((LivingEntity)entity).getEquippedStack(EquipmentSlot.FEET).isOf(Items.LEATHER_BOOTS);
        return false;
    }

    @Override
    protected VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.fullCube();
    }

    @Override
    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return false;
    }

    @Override
    protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 0.2f;
    }
}
