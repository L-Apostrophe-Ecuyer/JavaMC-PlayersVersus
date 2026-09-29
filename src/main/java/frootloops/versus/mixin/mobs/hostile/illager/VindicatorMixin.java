package frootloops.versus.mixin.mobs.hostile.illager;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Vindicator.class)
public abstract class VindicatorMixin extends AbstractIllager {
    protected VindicatorMixin(EntityType<? extends AbstractIllager> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void increaseAttributes(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason spawnReason, SpawnGroupData entityData, CallbackInfoReturnable<SpawnGroupData> cir) {
        AttributeInstance instanceDmg = this.getAttributes().getInstance(Attributes.ATTACK_DAMAGE);
        if (instanceDmg != null) instanceDmg.setBaseValue(3.0);

        AttributeInstance instanceHP = this.getAttributes().getInstance(Attributes.MAX_HEALTH);
        if (instanceHP != null) {
            instanceHP.setBaseValue(30.0D);
            this.setHealth(this.getMaxHealth());
        }
    }

    /*
    @Inject(method = "initGoals", at = @At("HEAD"))
    private void vindicatorsCanHeal(CallbackInfo ci) {
        this.goalSelector.add(1, new FleeAttackerAndHealGoal<>(this, 1));
    }*/
}