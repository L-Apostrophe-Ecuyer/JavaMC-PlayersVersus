package frootloops.versus.mod.items_and_effects.blight;

import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * How the blighted shake, worked out here from the Blight they're seen to have (the server tells every client who is
 * affected, so no packet is needed): from SHAKE_FROM of their hearts white, building to all of them, they shake as
 * frozen ones do, and the player themselves feels it in their own view (BlightCameraShakeMixin).
 */
@Environment(EnvType.CLIENT)
public final class UndeathShaking {
    private UndeathShaking() {}

    /** How hard the entity shakes, 0 to 1. */
    public static float strength(LivingEntity entity) {
        MobEffectInstance blight = entity.getEffect(CustomStatusEffects.BLIGHT);
        if (blight == null) return 0.0F;
        if (entity instanceof Player player && player.isSpectator()) return 0.0F;
        return Blight.shakeStrength(blight.getAmplifier(), entity.getMaxHealth());
    }

    /** Fills a render state: the entity shakes if it's blighted enough. */
    public static void extract(LivingEntity entity, LivingEntityRenderState state) {
        if (strength(entity) > 0.0F) state.isFullyFrozen = true;
    }

    /**
     * How far the view is turned, in degrees, on one axis (0 the yaw, 1 the pitch) for a shake of this strength: smooth
     * wavering, not jitter, off to the sides more than up and down.
     */
    public static float viewOffset(float strength, int axis, long millis) {
        if (strength <= 0.0F) return 0.0F;
        double t = millis / 1000.0;
        double wave = axis == 0
                ? Math.sin(t * 17.0) * 0.6 + Math.sin(t * 7.3 + 1.0) * 0.4
                : Math.cos(t * 19.0) * 0.6 + Math.sin(t * 5.1 + 2.0) * 0.4;
        float degrees = frootloops.versus.VersusSettings.Blight.SHAKE_DEGREES * (axis == 0 ? 1.0F : 0.7F);
        return (float) (wave * degrees * strength * strength);
    }

    public static float viewOffset(float strength, int axis) {
        return viewOffset(strength, axis, Util.getMillis());
    }
}
