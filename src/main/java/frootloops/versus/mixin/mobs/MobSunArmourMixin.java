package frootloops.versus.mixin.mobs;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Undead animals in armour don't burn in daylight: a zombie horse in horse armour, a zombie nautilus in its own. (The
 * body slot is the animals' armour; a zombie's helmet already shields it, and wears for it, in vanilla.)
 */
@Mixin(Mob.class)
public abstract class MobSunArmourMixin {

    @Inject(method = "burnUndead", at = @At("HEAD"), cancellable = true)
    private void playersVersus$armourShades(CallbackInfo info) {
        if (!((Mob) (Object) this).getItemBySlot(EquipmentSlot.BODY).isEmpty()) info.cancel();
    }
}
