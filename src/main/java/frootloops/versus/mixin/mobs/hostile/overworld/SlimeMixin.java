package frootloops.versus.mixin.mobs.hostile.overworld;


import frootloops.versus.mod.mobs.ModEntities;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SlimeEntity.class)
public abstract class SlimeMixin extends MobEntity {
    protected SlimeMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Overwrite
    public float getDamageAmount() {
        return (float)this.getAttributeValue(EntityAttributes.ATTACK_DAMAGE)/2.0f;
    }

    @Overwrite
    public void damage(LivingEntity target) {
        if (this.getEntityWorld() instanceof ServerWorld serverWorld && this.isAlive() && this.isInAttackRange(target) && this.canSee(target)) {
            DamageSource damageSource = this.getDamageSources().mobAttack(this);
            if (target.damage(serverWorld, damageSource, this.getDamageAmount())) {
                this.playSound(SoundEvents.ENTITY_SLIME_ATTACK, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                EnchantmentHelper.onTargetDamaged(serverWorld, target, damageSource);

                // SLIMES: Extra knockback:
                if(this.getClass().equals(SlimeEntity.class)) {
                    target.takeKnockback(0.2f + 0.2f * this.getDamageAmount(), this.getX() - target.getX(), this.getZ() - target.getZ());
                }

                // MAGMA: Some fire damage:
                else if(this.getClass().equals(MagmaCubeEntity.class)) {
                    target.setOnFireForTicks(10);
                }
            }
        }
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        if(damageSource.isOf(DamageTypes.LAVA) && this.getClass().equals(SlimeEntity.class) && ((SlimeEntity)((LivingEntity)(this))).getSize() == 1) {
            this.convertTo(EntityType.MAGMA_CUBE, EntityConversionContext.create(this, true, true), magmaCube -> {
                this.getEntityWorld().syncWorldEvent(null, WorldEvents.FIRE_EXTINGUISHED, this.getBlockPos(), 0);
            });
        }
        else super.onDeath(damageSource);
    }
}
