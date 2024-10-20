package frootloops.versus.mixin.client.environment.visuals;

import frootloops.versus.VersusMod;
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

    private static PlayerEntity cachedPlayer = null;

    @ModifyVariable(method = "applyFog", at = @At("HEAD"), ordinal = 0)
    private static BackgroundRenderer.FogType modifyFogType(BackgroundRenderer.FogType fogType) {
        return BackgroundRenderer.FogType.FOG_SKY;
    }

    @ModifyVariable(method = "applyFog", at = @At("HEAD"), ordinal = 0)
    private static float modifyViewDistanceOnRain(float viewDistance) {
        if(cachedPlayer != null) {
            if(cachedPlayer.getPos().y < 48.0d) viewDistance *= 0.95f - (cachedPlayer.getPos().y - 48.0)/(128.0);
            return viewDistance * (0.95f - (cachedPlayer.getWorld().getRainGradient(1.0f)/(1.5f + cachedPlayer.getWorld().getThunderGradient(1.0f))));
        }
        return viewDistance * 0.95f;
    }

    @Inject(method = "getFogModifier", at = @At("HEAD"), cancellable = true)
    private static void noMoreDarknessFog(Entity entity, float tickDelta, CallbackInfoReturnable cir) {
        if (entity instanceof PlayerEntity player) {
            if(player.hasStatusEffect(StatusEffects.DARKNESS) && !player.hasStatusEffect(StatusEffects.BLINDNESS)) cir.cancel();
            else cachedPlayer = player;
        }
    }

    @ModifyConstant(method = "applyFog", constant = @Constant(floatValue = 192.0f))
    private static float lessDenseNetherFog(float maxRenderDistance) {
        return 384.0f;
    }
}
