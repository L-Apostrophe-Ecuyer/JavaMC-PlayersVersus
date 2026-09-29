package frootloops.versus.mod.items_and_effects.throwing;

import frootloops.versus.mod.mobs.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class SlimeballEntity extends ThrowableItemProjectile {

    private static final ItemStack slimeballStack = new ItemStack(Items.SLIME_BALL, 1);

    private int numBouncesLeft;

    public SlimeballEntity(Level world, LivingEntity owner, ItemStack stack) {
        super((EntityType<SlimeballEntity>) ModEntities.SLIMEBALL, owner, world, stack);
        this.setItem(slimeballStack);
        this.numBouncesLeft = 2 + world.getRandom().nextInt(2);
    }

    public SlimeballEntity(Level world, double x, double y, double z,  ItemStack stack) {
        super((EntityType<SlimeballEntity>) ModEntities.SLIMEBALL, x, y, z, world, stack);
        this.setItem(slimeballStack);
        this.numBouncesLeft = 2 + world.getRandom().nextInt(2);
    }

    public SlimeballEntity(EntityType<SlimeballEntity> entityType, Level world) {
        super((EntityType<SlimeballEntity>) ModEntities.SLIMEBALL, world);
        this.setItem(slimeballStack);
        this.numBouncesLeft = 2 + world.getRandom().nextInt(2);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SLIME_BALL;
    }

    private ParticleOptions getParticleParameters() {
        return ParticleTypes.ITEM_SLIME;
    }

    @Override
    public ItemStack getItem() {
        return slimeballStack;
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == EntityEvent.DEATH) {
            ParticleOptions particleEffect = this.getParticleParameters();
            for (int i = 0; i < 8; ++i) {
                this.level().addParticle(particleEffect, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        Entity entity = entityHitResult.getEntity();
        if(entity instanceof LivingEntity livingEntity) {
            double strength = 0.3 + 0.5 * Math.max(1.0, this.getDeltaMovement().lengthSqr());
            livingEntity.setDeltaMovement(livingEntity.getDeltaMovement().add(0.0, 0.05 + 0.3 * this.getDeltaMovement().y, 0.0));
            livingEntity.knockback(strength, -this.getDeltaMovement().x, -this.getDeltaMovement().z);
        }
    }


    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide()) {
            this.playSound(SoundEvents.SLIME_JUMP_SMALL, 1f, 0.9f + 0.3f * random.nextFloat());
            if(hitResult.getType() == HitResult.Type.ENTITY || this.numBouncesLeft < 1) {
                this.level().broadcastEntityEvent(this, EntityEvent.DEATH);
                this.discard();
            }
            else {
                this.numBouncesLeft--;
                if (hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHitResult = (BlockHitResult)hitResult;
                    this.onHitBlock(blockHitResult);
                    BlockPos blockPos = blockHitResult.getBlockPos();
                    BlockState blockState = this.level().getBlockState(blockPos);
                    this.level().gameEvent(GameEvent.PROJECTILE_LAND, blockPos, GameEvent.Context.of(this, blockState));

                    // Bounce!
                    Direction direction = blockHitResult.getDirection();
                    if(direction.getAxis() == Direction.Axis.X) this.setDeltaMovement(this.getDeltaMovement().multiply(-0.4, 0.6, 0.6));
                    else if(direction.getAxis() == Direction.Axis.Y) this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, -0.4, 0.6));
                    else if(direction.getAxis() == Direction.Axis.Z) this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.6, -0.4));
                    this.hasImpulse = true;
                }
            }
        }
    }
}
