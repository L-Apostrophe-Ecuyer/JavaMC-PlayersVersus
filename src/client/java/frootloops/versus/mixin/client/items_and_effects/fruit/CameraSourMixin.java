package frootloops.versus.mixin.client.items_and_effects.fruit;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.items_and_effects.fruit.SourFov;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sour widens the view (SourFov): applied to the field of view the camera works out each frame, after vanilla's own
 * modifier and its 1.5 clamp, so it can go as wide as Sour says; the hand's field of view is left alone. The camera
 * remembers Sour from frame to frame (SourFov.Tracker), so a Sour renewed by another lemon keeps the view wide.
 */
@Mixin(Camera.class)
public abstract class CameraSourMixin {

    @Unique
    private static final SourFov.Tracker playersVersus$sourSeen = new SourFov.Tracker();

    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float playersVersus$sour(float fov, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        MobEffectInstance sour = minecraft.player == null || CustomStatusEffects.SOUR == null ? null : minecraft.player.getEffect(CustomStatusEffects.SOUR);
        float remaining = sour == null ? 0.0F : sour.getDuration() - partialTicks;
        float multiplier = playersVersus$sourSeen.multiplier(remaining, VersusSettings.Lemons.SOUR_TICKS);
        return SourFov.widen(fov, multiplier, minecraft.options.fovEffectScale().get().floatValue());
    }
}
