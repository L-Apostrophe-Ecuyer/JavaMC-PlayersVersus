package frootloops.versus.mixin.mobs.hostile.overworld;


import frootloops.versus.mod.mobs.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Slime.class)
public abstract class SlimeMixin extends Mob {
    protected SlimeMixin(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
    }

    @Overwrite
    public float getAttackDamage() {
        return (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE)/2.0f;
    }

    @Overwrite
    public void dealDamage(LivingEntity target) {
        if (this.level() instanceof ServerLevel serverWorld && this.isAlive() && this.isWithinMeleeAttackRange(target) && this.hasLineOfSight(target)) {
            DamageSource damageSource = this.damageSources().mobAttack(this);
            if (target.hurtServer(serverWorld, damageSource, this.getAttackDamage())) {
                this.playSound(SoundEvents.SLIME_ATTACK, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                EnchantmentHelper.doPostAttackEffects(serverWorld, target, damageSource);

                // SLIMES: Extra knockback:
                if(this.getClass().equals(Slime.class)) {
                    target.knockback(0.2f + 0.2f * this.getAttackDamage(), this.getX() - target.getX(), this.getZ() - target.getZ());
                }

                // MAGMA: Some fire damage:
                else if(this.getClass().equals(MagmaCube.class)) {
                    target.igniteForTicks(10);
                }
            }
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        if(damageSource.is(DamageTypes.LAVA) && this.getClass().equals(Slime.class) && ((Slime)((LivingEntity)(this))).getSize() == 1) {
            this.convertTo(EntityType.MAGMA_CUBE, ConversionParams.single(this, true, true), magmaCube -> {
                this.level().levelEvent(null, LevelEvent.SOUND_EXTINGUISH_FIRE, this.blockPosition(), 0);
            });
        }
        else super.die(damageSource);
    }
}
