package frootloops.versus.mixin.client.items_and_effects.blight;

import frootloops.versus.mod.items_and_effects.blight.UndeathShaking;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The blighted player's own view shakes as their hearts are blighted (UndeathShaking): the camera is turned a little off the
 * way they face, and back, never their aim. (Others see their body shake instead.)
 */
@Environment(EnvType.CLIENT)
@Mixin(Camera.class)
public abstract class BlightCameraShakeMixin {

    @ModifyVariable(method = "setRotation(FF)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float playersVersus$shakeYaw(float yRot) {
        return yRot + UndeathShaking.viewOffset(playersVersus$strength(), 0);
    }

    @ModifyVariable(method = "setRotation(FF)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float playersVersus$shakePitch(float xRot) {
        return xRot + UndeathShaking.viewOffset(playersVersus$strength(), 1);
    }

    private static float playersVersus$strength() {
        Entity viewed = Minecraft.getInstance().getCameraEntity();
        return viewed instanceof LivingEntity living ? UndeathShaking.strength(living) : 0.0F;
    }
}
