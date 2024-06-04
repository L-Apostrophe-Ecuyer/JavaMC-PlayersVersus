package frootloops.versus.mod.items.throwing;

import frootloops.versus.mod.mobs.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class SlimeballEntity extends ThrownItemEntity {

    private static final ItemStack slimeballStack = new ItemStack(Items.SLIME_BALL, 1);

    public SlimeballEntity(World world, LivingEntity owner) {
        super((EntityType<SlimeballEntity>) ModEntities.SLIMEBALL, owner, world);
        //super(EntityType.SNOWBALL, owner, world);
        this.setItem(slimeballStack);
    }

    public SlimeballEntity(World world, double x, double y, double z) {
        super((EntityType<SlimeballEntity>) ModEntities.SLIMEBALL, x, y, z, world);
        //super(EntityType.SNOWBALL, x, y, z, world);
        this.setItem(slimeballStack);
    }

    public SlimeballEntity(EntityType<SlimeballEntity> entityType, World world) {
        super((EntityType<SlimeballEntity>) ModEntities.SLIMEBALL, world);
        //super(EntityType.SNOWBALL, world);
        this.setItem(slimeballStack);
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
                this.method_48926().addParticle(particleEffect, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        Entity entity = entityHitResult.getEntity();
        if(entity instanceof LivingEntity livingEntity) {
            livingEntity.takeKnockback(1, -this.getVelocity().x, -this.getVelocity().z);
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.method_48926().isClient) {
            this.method_48926().sendEntityStatus(this, EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES);
            this.playSound(SoundEvents.ENTITY_SLIME_JUMP_SMALL, 1f, 1f);
            this.discard();
        }
    }
}
