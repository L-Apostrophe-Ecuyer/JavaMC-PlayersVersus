package frootloops.versus.mixin.client.environment.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;


@Environment(EnvType.CLIENT)
@Mixin(DarknessFogEnvironment.class)
public abstract class DarknessFogRendererMixin {

    @ModifyConstant(method = "setupFog", constant = @Constant(floatValue = 15.0F))
    private float darknessEffectNoLongerCompletelyBlindsPlayers(float fogEnd) {
        MobEffectInstance darkness = Minecraft.getInstance().player.getEffect(MobEffects.DARKNESS);
        float blindnessAmount = darkness == null ? 0.0f : 8f + darkness.getAmplifier() * 12.0f;
        return 48.0f - blindnessAmount;
    }

    @ModifyConstant(method = "setupFog", constant = @Constant(floatValue = 0.75F))
    private float darknessEffectFogStart(float fogStartOffset) {
        return 0.0f;
    }
}
