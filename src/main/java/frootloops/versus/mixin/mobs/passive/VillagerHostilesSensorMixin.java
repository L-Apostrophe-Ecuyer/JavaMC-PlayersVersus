package frootloops.versus.mixin.mobs.passive;

import frootloops.versus.mod.mobs.hostile.overworld.PaleVillagerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.VillagerHostilesSensor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Villagers flee pale villagers as they flee zombie villagers: from 8 blocks off. */
@Mixin(VillagerHostilesSensor.class)
public abstract class VillagerHostilesSensorMixin {

    @Inject(method = "isHostile", at = @At("HEAD"), cancellable = true)
    private void playersVersus$paleVillagersAreHostile(LivingEntity entity, CallbackInfoReturnable<Boolean> info) {
        if (entity instanceof PaleVillagerEntity) info.setReturnValue(true);
    }

    /** The sensor's table of distances doesn't know them (and would fail on them). */
    @Inject(method = "isClose", at = @At("HEAD"), cancellable = true)
    private void playersVersus$paleVillagersAreClose(LivingEntity villager, LivingEntity hostile, CallbackInfoReturnable<Boolean> info) {
        if (hostile instanceof PaleVillagerEntity || villager instanceof PaleVillagerEntity) info.setReturnValue(hostile.distanceToSqr(villager) <= 64.0);
    }
}
