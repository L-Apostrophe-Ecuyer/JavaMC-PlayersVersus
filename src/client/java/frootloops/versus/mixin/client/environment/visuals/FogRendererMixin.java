package frootloops.versus.mixin.client.environment.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.LightType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Environment(EnvType.CLIENT)
@Mixin(BackgroundRenderer.class)
public abstract class FogRendererMixin {

    private static PlayerEntity cachedPlayer = null;
    private static float prevCaveFogAmount = 0.0f;

    @ModifyVariable(method = "applyFog", at = @At("HEAD"), ordinal = 0)
    private static BackgroundRenderer.FogType modifyFogType(BackgroundRenderer.FogType fogType) {
        return BackgroundRenderer.FogType.FOG_SKY;
    }

    @ModifyVariable(method = "applyFog", at = @At("HEAD"), ordinal = 0)
    private static float modifyViewDistanceOnRainOrCave(float viewDistance) {
        if(cachedPlayer != null) {
            float weatherFogAmount = (cachedPlayer.getWorld().getRainGradient(1.0f)/(1.5f + cachedPlayer.getWorld().getThunderGradient(1.0f))) - (float)Math.clamp((cachedPlayer.getPos().y - 194.0)/256.0, 0.0, 0.4);
            float caveFogAmount = (24f/(float)(32 + cachedPlayer.getWorld().getLightLevel(LightType.SKY, cachedPlayer.getBlockPos())) - 0.5f + prevCaveFogAmount + prevCaveFogAmount)/3.0f;
            prevCaveFogAmount = caveFogAmount;

            float fogAmount = Math.max(caveFogAmount, weatherFogAmount);
            return viewDistance * (0.95f - fogAmount);
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
