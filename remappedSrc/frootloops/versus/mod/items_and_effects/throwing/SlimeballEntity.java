package frootloops.versus.mod.items_and_effects.throwing;

import frootloops.versus.mod.mobs.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class SlimeballEntity extends ThrownItemEntity {

    private static final ItemStack slimeballStack = new ItemStack(Items.SLIME_BALL, 1);

    private int numBouncesLeft;

    public SlimeballEntity(World world, LivingEntity owner, ItemStack stack) {
        super((EntityType<SlimeballEntity>) ModEntities.SLIMEBALL, owner, world, stack);
        this.setItem(slimeballStack);
        this.numBouncesLeft = 2 + world.getRandom().nextInt(2);
    }

    public SlimeballEntity(World world, double x, double y, double z,  ItemStack stack) {
        super((EntityType<SlimeballEntity>) ModEntities.SLIMEBALL, x, y, z, world, stack);
        this.setItem(slimeballStack);
        this.numBouncesLeft = 2 + world.getRandom().nextInt(2);
    }

    public SlimeballEntity(EntityType<SlimeballEntity> entityType, World world) {
        super((EntityType<SlimeballEntity>) ModEntities.SLIMEBALL, world);
        this.setItem(slimeballStack);
        this.numBouncesLeft = 2 + world.getRandom().nextInt(2);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SLIME_BALL;
    }

    private ParticleEffect getParticleParameters() {
        return ParticleTypes.ITEM_SLIME;
    }

    @Override
    public ItemStack getStack() {
        return slimeballStack;
    }

    @Override
    public void handleStatus(byte status) {
        if (status == EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES) {
            ParticleEffect particleEffect = this.getParticleParameters();
            for (int i = 0; i < 8; ++i) {
                this.getWorld().addParticleClient(particleEffect, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        Entity entity = entityHitResult.getEntity();
        if(entity instanceof LivingEntity livingEntity) {
            double strength = 0.3 + 0.5 * Math.max(1.0, this.getVelocity().lengthSquared());
            livingEntity.setVelocity(livingEntity.getVelocity().add(0.0, 0.05 + 0.3 * this.getVelocity().y, 0.0));
            livingEntity.takeKnockback(strength, -this.getVelocity().x, -this.getVelocity().z);
        }
    }


    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.getWorld().isClient) {
            this.playSound(SoundEvents.ENTITY_SLIME_JUMP_SMALL, 1f, 0.9f + 0.3f * random.nextFloat());
            if(hitResult.getType() == HitResult.Type.ENTITY || this.numBouncesLeft < 1) {
                this.getWorld().sendEntityStatus(this, EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES);
                this.discard();
            }
            else {
                this.numBouncesLeft--;
                if (hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHitResult = (BlockHitResult)hitResult;
                    this.onBlockHit(blockHitResult);
                    BlockPos blockPos = blockHitResult.getBlockPos();
                    BlockState blockState = this.getWorld().getBlockState(blockPos);
                    this.getWorld().emitGameEvent(GameEvent.PROJECTILE_LAND, blockPos, GameEvent.Emitter.of(this, blockState));

                    // Bounce!
                    Direction direction = blockHitResult.getSide();
                    if(direction.getAxis() == Direction.Axis.X) this.setVelocity(this.getVelocity().multiply(-0.4, 0.6, 0.6));
                    else if(direction.getAxis() == Direction.Axis.Y) this.setVelocity(this.getVelocity().multiply(0.6, -0.4, 0.6));
                    else if(direction.getAxis() == Direction.Axis.Z) this.setVelocity(this.getVelocity().multiply(0.6, 0.6, -0.4));
                    this.velocityDirty = true;
                }
            }
        }
    }
}
