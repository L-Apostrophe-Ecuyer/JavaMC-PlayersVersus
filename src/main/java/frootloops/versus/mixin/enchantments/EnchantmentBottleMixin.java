package frootloops.versus.mixin.enchantments;

import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ThrownExperienceBottle.class)
public class EnchantmentBottleMixin {

    @ModifyVariable(method = "onHit", ordinal = 0, at = @At("STORE"))
    private int moreExperiencePerBottle(int amount) {
        return amount * 4;
    }
}
