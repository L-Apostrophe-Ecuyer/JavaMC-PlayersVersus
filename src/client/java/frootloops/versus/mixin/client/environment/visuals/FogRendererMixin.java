package frootloops.versus.mixin.client.environment.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(BackgroundRenderer.class)
public abstract class FogRendererMixin {

    @ModifyVariable(method = "applyFog", at = @At("HEAD"), ordinal = 0)
    private static BackgroundRenderer.FogType modifyFogType(BackgroundRenderer.FogType fogType) {
        if(fogType == BackgroundRenderer.FogType.FOG_TERRAIN) return BackgroundRenderer.FogType.FOG_SKY;
        return fogType;
    }

    @Inject(method = "getFogModifier", at = @At("HEAD"), cancellable = true)
    private static void noMoreDarknessFog(Entity entity, float tickDelta, CallbackInfoReturnable cir) {
        if (entity instanceof PlayerEntity player) {
            if(player.hasStatusEffect(StatusEffects.DARKNESS))
                if(!player.hasStatusEffect(StatusEffects.BLINDNESS))
                    cir.cancel();
        }
    }

    @ModifyConstant(method = "applyFog", constant = @Constant(floatValue = 192.0f))
    private static float lessDenseNetherFog(float maxRenderDistance) {
        return 384.0f;
    }
}
