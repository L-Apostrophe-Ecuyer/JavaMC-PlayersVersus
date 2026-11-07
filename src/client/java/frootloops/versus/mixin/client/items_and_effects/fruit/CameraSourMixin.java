package frootloops.versus.mixin.client.items_and_effects.fruit;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.items_and_effects.fruit.SourFov;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sour widens the view (SourFov): applied to the field of view the camera works out each frame, after vanilla's own
 * modifier and its 1.5 clamp, so it can go as wide as Sour says; the hand's field of view is left alone.
 */
@Mixin(Camera.class)
public abstract class CameraSourMixin {

    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float playersVersus$sour(float fov, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || CustomStatusEffects.SOUR == null) return fov;
        MobEffectInstance sour = minecraft.player.getEffect(CustomStatusEffects.SOUR);
        if (sour == null) return fov;
        float remaining = sour.getDuration() - partialTicks;
        return SourFov.apply(fov, remaining, VersusSettings.Lemons.SOUR_TICKS, minecraft.options.fovEffectScale().get().floatValue());
    }
}
