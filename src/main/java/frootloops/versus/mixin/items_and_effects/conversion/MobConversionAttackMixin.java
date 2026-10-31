package frootloops.versus.mixin.items_and_effects.conversion;

import frootloops.versus.mod.items_and_effects.conversion.ConversionAttacks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every mob's melee hit that lands goes by ConversionAttacks: pale zombies, creakings and (rarely) zombies convert. */
@Mixin(Mob.class)
public abstract class MobConversionAttackMixin {

    @Inject(method = "doHurtTarget", at = @At("RETURN"))
    private void playersVersus$convertingHit(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> info) {
        if (info.getReturnValueZ() && target instanceof LivingEntity living) ConversionAttacks.afterHit(level, (Mob) (Object) this, living);
    }
}
