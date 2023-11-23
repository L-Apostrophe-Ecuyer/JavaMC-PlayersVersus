package frootloops.versus.mixin.items.equipment.elytra;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(LivingEntity.class)
public class ElytraGlideDurabilityMixin {

    @ModifyVariable(method = "tickFallFlying", at = @At("STORE"), ordinal = 1)
    private int noDurabilityWhileGliding(int j) {
        return 1;
    }
}
