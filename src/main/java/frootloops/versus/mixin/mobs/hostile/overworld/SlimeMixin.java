package frootloops.versus.mixin.mobs.hostile.overworld;


import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.mobs.hostile.overworld.IceCubeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

// Since 26.2 slimes and magma cubes are both AbstractCubeMobs (a magma cube was a slime before), which holds the
// attack. The class checks below keep each change to the mob it was for (ice cubes are slimes, but their own kind);
// sulfur cubes deal no damage.
@Mixin(AbstractCubeMob.class)
public abstract class SlimeMixin extends Mob {
    protected SlimeMixin(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
    }

    @Overwrite
    protected float getAttackDamage() {
        return (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE)/2.0f;
    }

    @Overwrite
    protected void dealDamage(LivingEntity target) {
        if (this.level() instanceof ServerLevel serverWorld && this.isAlive() && this.isWithinMeleeAttackRange(target) && this.hasLineOfSight(target)) {
            DamageSource damageSource = this.damageSources().mobAttack(this);
            if (target.hurtServer(serverWorld, damageSource, this.getAttackDamage())) {
                this.playSound(SoundEvents.SLIME_ATTACK, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                EnchantmentHelper.doPostAttackEffects(serverWorld, target, damageSource);

                // SLIMES: Extra knockback:
                if(this.getClass().equals(Slime.class)) {
                    target.knockback(0.2f + 0.2f * this.getAttackDamage(), this.getX() - target.getX(), this.getZ() - target.getZ(), damageSource, this.getAttackDamage());
                }

                // MAGMA: Some fire damage:
                else if(this.getClass().equals(MagmaCube.class)) {
                    target.igniteForTicks(10);
                }

                // ICE CUBES: Their touch freezes:
                else if((Object) this instanceof IceCubeEntity iceCube) {
                    iceCube.freeze(target);
                }
            }
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        if(damageSource.is(DamageTypes.LAVA) && this.getClass().equals(Slime.class) && ((AbstractCubeMob)((LivingEntity)(this))).getSize() == 1) {
            this.convertTo(EntityTypes.MAGMA_CUBE, ConversionParams.single(this, true, true), magmaCube -> {
                this.level().levelEvent(null, LevelEvent.SOUND_EXTINGUISH_FIRE, this.blockPosition(), 0);
            });
        }
        else super.die(damageSource);
    }
}
