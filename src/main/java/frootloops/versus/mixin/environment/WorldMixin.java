package frootloops.versus.mixin.environment;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin {
    @Shadow
    protected float thunderGradient;

    @Inject(method = "hasRain", at = @At("HEAD"), cancellable = true)
    private void darknessWhenAngered(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if(thunderGradient < 0.1f) cir.setReturnValue(false);
    }
}
