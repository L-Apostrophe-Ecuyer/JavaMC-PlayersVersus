package frootloops.versus.mixin.client.environment.visuals;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.dimension.DimensionType;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
@Mixin(LightmapTextureManager.class)
public abstract class LightmapMixin {

    @Shadow private final MinecraftClient client;

    protected LightmapMixin(MinecraftClient client, GameRenderer renderer) {
        this.client = client;
    }
    private static final Vector3f NIGHT_VISION_SKY_COLOR = new Vector3f(1.0F, 0.1F, 0.0F);
    private static int nightVisionAmplifier = -1;

    @ModifyVariable(method = "update", at = @At("STORE"), ordinal = 0)
    private Vector3f modifySky(Vector3f skyColor) {
        if(MinecraftClient.getInstance().world.getDimensionEffects().hasAlternateSkyColor()) return skyColor; // End flashes
        if(nightVisionAmplifier > -1) return NIGHT_VISION_SKY_COLOR;
        return skyColor;
    }

    @ModifyVariable(method = "update", at = @At("STORE"), ordinal = 6)
    private float reducedNightVision(float l) {
        if (l > 0f) {
            StatusEffectInstance effect = client.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if(effect == null) {
                nightVisionAmplifier = -1;
                return l/2f;
            }
            nightVisionAmplifier = effect.getAmplifier();
            return nightVisionAmplifier * 0.2f + l/16f;

        } else {
            nightVisionAmplifier = -1;
            return 0f;
        }
    }

    @ModifyVariable(method = "update", at = @At("STORE"), ordinal = 8)
    private float reducedAmbientLight(float n) {
        if(nightVisionAmplifier > -1) return n - 0.05f;
        return n - 0.03f;
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getSkyBrightness(F)F"))
    private float getSkyBrightness(ClientWorld world, float tickDelta) {
        float result = world.getSkyBrightness(tickDelta);
        if(result < 0.99f && world.getDimension().hasSkyLight()) {
            float moonPhaseDarkness = Math.abs(4.0f - (float)((world.getLunarTime() + 6000L + (24000L * 3L)) % (24000L * 8L))/24000.0f) - 0.1f;
            float moonHeight = 0.3f/(0.25f + Math.abs(((float)((world.getTimeOfDay() - 750L)%12000L) - 6000f)/12000f)) - 0.4f;
            result -= moonHeight * (moonPhaseDarkness/24f + (moonPhaseDarkness * moonPhaseDarkness)/96f);
        }
        if(nightVisionAmplifier > -1) result += 0.1f + (0.3f * (float)nightVisionAmplifier);
        return result;
    }


    @ModifyVariable(method = "update", at = @At("STORE"), ordinal = 3)
    private float alwaysSomeDarkness(float h) {
        return (h + 2.0f)/3.0f;
    }

    @Overwrite
    private float getDarkness(LivingEntity entity, float factor, float delta) {
        if(factor > 0f) return Math.max(0.0f, MathHelper.cos(((float)entity.age - delta) * (float)Math.PI * 0.025f) * factor);
        else if(nightVisionAmplifier > -1) return 0f;
        else if(entity.getY() < -32d) return 32f/384f;
        else if(entity.getY() < 0d) return -((float)entity.getY())/384f;
        else return 0f;
    }
}
