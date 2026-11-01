package frootloops.versus.mod.mobs.hostile.overworld;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.level.Level;

/**
 * A wandering trader the withering took: a pale villager in a trader's blue robe. It keeps the trades it carried; cured
 * with golden apples (ZombieVillagerMixin), it's a wandering trader again, those trades and all, with a fresh while to
 * wander before it leaves.
 */
public class PaleWanderingTraderEntity extends PaleVillagerEntity {
    public PaleWanderingTraderEntity(EntityType<? extends ZombieVillager> type, Level level) {
        super(type, level);
    }

    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.WANDERING_TRADER_AMBIENT;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WANDERING_TRADER_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.WANDERING_TRADER_DEATH;
    }
}
