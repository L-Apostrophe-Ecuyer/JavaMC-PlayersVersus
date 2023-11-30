package frootloops.versus.mixin.hostile_mobs.illager;

import frootloops.versus.mod.hostile_mobs.ai.FleeAttackerAndHealGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitchEntity.class)
public abstract class WitchMixin extends RaiderEntity {

    protected WitchMixin(EntityType<? extends RaiderEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("HEAD"))
    private void witchesCanHeal(CallbackInfo ci) {
        this.goalSelector.add(1, new FleeAttackerAndHealGoal<>(this, 1));
    }
}