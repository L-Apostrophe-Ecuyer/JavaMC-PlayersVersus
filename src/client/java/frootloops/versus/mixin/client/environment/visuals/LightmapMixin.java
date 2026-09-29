package frootloops.versus.mixin.client.environment.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
// 26.x computes the lightmap on the GPU from a LightmapRenderState that LightmapRenderStateExtractor fills; its
// calculateDarknessScale is 1.21.10's (Lightmap/LightTexture), unchanged in signature and role.
// Not ported yet (no one-to-one place in 26.x): the ordinal-based tweaks to 1.21.10's updateLightTexture locals:
// night-vision sky tints, weaker night vision, lower ambient light, moon-phase night darkening through getSkyDarken,
// and "always some darkness". They belong on LightmapRenderState's fields (skyFactor, nightVisionEffectIntensity,
// ambientColor, brightness) after extract, to be tuned in game.
@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapMixin {

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
