package frootloops.versus.mixin.client.environment.visuals;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.LunarDarkness;
import frootloops.versus.mod.environment.WorldTime;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
// 26.x computes the lightmap on the GPU from a LightmapRenderState that LightmapRenderStateExtractor fills once a tick:
// the sky's share of it is skyFactor, vanilla's visual/sky_light_factor (the minecraft:day timeline: 1 by day, 0.24 by
// night). Nights darken with the moon on it (LunarDarkness), as 1.21.10's darkened getSkyDarken.
// Not ported (no one-to-one place in 26.x): 1.21.10's night-vision sky tints, weaker night vision, lower ambient light
// and "always some darkness". They belong on LightmapRenderState's other fields (nightVisionEffectIntensity,
// nightVisionColor, ambientColor, brightness), to be tuned in game.
@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapMixin {

    @Shadow @Final private Minecraft minecraft;

    /** The moon darkens the night: the sky factor vanilla just took from the timeline, less the moon's darkening. */
    @Inject(method = "extract", at = @At("TAIL"))
    private void playersVersus$moonlessNights(LightmapRenderState state, float partialTick, CallbackInfo info) {
        ClientLevel level = this.minecraft.level;
        // Only when the lightmap is redone this tick: the state is kept between, and would darken again and again
        if (!state.needsUpdate || level == null || !VersusSettings.Moon.DARKER_MOONLESS_NIGHTS) return;
        if (!level.dimensionType().hasSkyLight()) return;
        state.skyFactor = LunarDarkness.skyFactor(state.skyFactor, WorldTime.dayTime(level));
    }

    @Overwrite
    private float calculateDarknessScale(LivingEntity entity, float factor, float delta) {
        if(factor > 0f) return Math.max(0.0f, Mth.cos(((float)entity.tickCount - delta) * (float)Math.PI * 0.025f) * factor);
        // Night vision lifts the depth darkness (1.21.10 learnt of it from the night vision factor it had just modified).
        else if(entity.hasEffect(MobEffects.NIGHT_VISION)) return 0f;
        else if(entity.getY() < -32d) return 32f/384f;
        else if(entity.getY() < 0d) return -((float)entity.getY())/384f;
        else return 0f;
    }
}
