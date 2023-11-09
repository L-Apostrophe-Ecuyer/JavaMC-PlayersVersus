package frootloops.versus.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class WaterMovementMixin {

    @ModifyVariable(method = "travel", at = @At("STORE"), ordinal = 2)
    private float fasterWaterMovement(float h) {
        if(((LivingEntity)((Object)this)).isSprinting()) return h;
        return h + 0.5f;
    }
}
