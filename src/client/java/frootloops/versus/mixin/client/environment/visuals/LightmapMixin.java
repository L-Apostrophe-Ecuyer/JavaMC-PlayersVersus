package frootloops.versus.mixin.client.environment.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Lightmap;
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
@Mixin(Lightmap.class)
public abstract class LightmapMixin {

    @Shadow private final Minecraft minecraft;

    protected LightmapMixin(Minecraft client, GameRenderer renderer) {
        this.minecraft = client;
    }
    private static final Vector3f NIGHT_VISION_SKY_COLOR_OVERWORLD = new Vector3f(0.52F, 0.43F, 0.35F);
    private static final Vector3f NIGHT_VISION_SKY_COLOR_NETHER = new Vector3f(0.9F, 1.0F, 0.9F);
    private static int nightVisionAmplifier = -1;

    @ModifyVariable(method = "updateLightTexture", at = @At("STORE"), ordinal = 0)
    private Vector3f modifySky(Vector3f skyColor) {
        if(nightVisionAmplifier > -1) {
            if(minecraft.level.dimensionType().ultraWarm()) return NIGHT_VISION_SKY_COLOR_NETHER;
            if(minecraft.level.dimensionType().hasRaids()) return NIGHT_VISION_SKY_COLOR_OVERWORLD;
            return skyColor.add(-0.2f, -0.2f, -0.4f);
        }
        return skyColor;
    }

    @ModifyVariable(method = "updateLightTexture", at = @At("STORE"), ordinal = 6)
    private float reducedNightVision(float l) {
        if (l > 0f) {
            MobEffectInstance effect = minecraft.player.getEffect(MobEffects.NIGHT_VISION);
            if(effect == null) {
                nightVisionAmplifier = -1;
                return l/2f;
            }
            nightVisionAmplifier = effect.getAmplifier();
            return nightVisionAmplifier * 0.15f + 0.125f + l/16f;

        } else {
            nightVisionAmplifier = -1;
            return 0f;
        }
    }

    @ModifyVariable(method = "updateLightTexture", at = @At("STORE"), ordinal = 8)
    private float reducedAmbientLight(float n) {
        if(nightVisionAmplifier > -1) return n - 0.05f + 0.015f * nightVisionAmplifier;
        return n - 0.03f;
    }

    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyDarken(F)F"))
    private float getSkyBrightness(ClientLevel world, float tickDelta) {
        float result = world.getSkyDarken(tickDelta);
        if(result < 0.99f && world.dimensionType().hasSkyLight()) {
            float moonPhaseDarkness = Math.abs(4.0f - (float)((world.dayTime() + 6000L + (24000L * 3L)) % (24000L * 8L))/24000.0f) - 0.1f;
            float moonHeight = 0.3f/(0.25f + Math.abs(((float)((world.getDayTime() - 750L)%12000L) - 6000f)/12000f)) - 0.4f;
            result -= moonHeight * (moonPhaseDarkness/24f + (moonPhaseDarkness * moonPhaseDarkness)/96f);
        }
        if(nightVisionAmplifier > -1) result += 0.1f + (0.3f * (float)nightVisionAmplifier);
        return result;
    }


    @ModifyVariable(method = "updateLightTexture", at = @At("STORE"), ordinal = 3)
    private float alwaysSomeDarkness(float h) {
        return (h + 2.0f)/3.0f;
    }

    @Overwrite
    private float calculateDarknessScale(LivingEntity entity, float factor, float delta) {
        if(factor > 0f) return Math.max(0.0f, Mth.cos(((float)entity.tickCount - delta) * (float)Math.PI * 0.025f) * factor);
        else if(nightVisionAmplifier > -1) return 0f;
        else if(entity.getY() < -32d) return 32f/384f;
        else if(entity.getY() < 0d) return -((float)entity.getY())/384f;
        else return 0f;
    }
}
