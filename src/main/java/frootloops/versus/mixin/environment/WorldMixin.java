package frootloops.versus.mixin.environment;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin {
    @Shadow
    protected float thunderGradient, rainGradient, rainGradientPrev;

    @Inject(method = "hasRain", at = @At("HEAD"), cancellable = true)
    private void noRainUnlessThunder(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if(thunderGradient < 0.05f) cir.setReturnValue(false);
    }

    @Overwrite
    public void setRainGradient(float newRainGradient) {
        if(newRainGradient > rainGradientPrev) newRainGradient = Math.min(1f, (rainGradient + rainGradientPrev)/3f);
        else newRainGradient = Math.max(0.0f, newRainGradient);
        this.rainGradientPrev = newRainGradient;
        this.rainGradient = newRainGradient;
    }
}
