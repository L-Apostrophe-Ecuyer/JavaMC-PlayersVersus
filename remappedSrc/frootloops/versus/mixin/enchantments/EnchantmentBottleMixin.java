package frootloops.versus.mixin.enchantments;

import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ExperienceBottleEntity.class)
public class EnchantmentBottleMixin {

    @ModifyVariable(method = "onCollision", ordinal = 0, at = @At("STORE"))
    private int moreExperiencePerBottle(int amount) {
        return amount * 4;
    }
}
