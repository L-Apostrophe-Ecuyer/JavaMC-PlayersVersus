package frootloops.versus.mixin.environment;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapMixin {

    @Shadow private final MinecraftClient client;

    protected LightmapMixin(MinecraftClient client, GameRenderer renderer) {
        this.client = client;
    }

    @ModifyVariable(method = "update", at = @At("STORE"), ordinal = 3)
    private float alwaysSomeDarkness(float h) {
        return (h + 2.0f)/3.0f;
    }

    @ModifyVariable(method = "update", at = @At("STORE"), ordinal = 6)
    private float reducedNightVision(float l) {return l > 0f && this.client.player.hasStatusEffect(StatusEffects.CONDUIT_POWER) ? l : 0f;}

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/LightmapTextureManager;getBrightness(Lnet/minecraft/world/dimension/DimensionType;I)F"))
    private float getBrightness(DimensionType type, int lightLevel) {
        float lightPercent = (float)lightLevel/32f + (float)(lightLevel + 6f)/ 32.0f;
        float ambientLight = this.client.player.hasStatusEffect(StatusEffects.NIGHT_VISION) ? type.ambientLight() + 0.065f : type.ambientLight() - 0.05f;
        return MathHelper.lerp(ambientLight, lightPercent / (4.0f - 3.0f * lightPercent), 1.5f);
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getSkyBrightness(F)F"))
    private float getSkyBrightness(ClientWorld world, float tickDelta) {
        float result = world.getSkyBrightness(tickDelta);
        if(result < 0.99f && world.getDimension().hasSkyLight()) {
            float moonPhaseDarkness = Math.abs(4.0f - (float)((world.getLunarTime() + 6000L + (24000L * 3L)) % (24000L * 8L))/24000.0f);
            float moonHeight = 0.3f/(0.25f + Math.abs(((float)(world.getTimeOfDay()%12000L) - 6000f)/12000f));
            result -= 0.08f + moonHeight * (moonPhaseDarkness/64f + (moonPhaseDarkness * moonPhaseDarkness)/96f);
        }
        if(this.client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) result += 0.2f;
        return result;
    }


    @Overwrite
    private float getDarkness(LivingEntity entity, float factor, float delta) {
        if(factor > 0f) return Math.max(0.0f, MathHelper.cos(((float)entity.age - delta) * (float)Math.PI * 0.025f) * 0.45f * factor);
        else if(entity.getY() < -32d) return 48f/384f;
        else if(entity.getY() < 16d) return -((float)entity.getY() - 16f)/384f;
        else return 0f;
    }
}
