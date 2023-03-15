
package frootloops.versus.mixin.passive_mobs;

import net.minecraft.entity.passive.SheepEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SheepEntity.class)
public class SheepMixin {

    @ModifyArg(method = "sheared", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/Random;nextInt(I)I"))
    private int doubleTheAmountOfWool(int three) {
        return 6;
    }
}
