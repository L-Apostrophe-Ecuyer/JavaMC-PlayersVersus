package frootloops.versus.mixin.client.mobs.hostile;

import frootloops.versus.mod.mobs.melee.MeleeAnimation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.monster.piglin.PiglinModel;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Poses a humanoid mob's melee swing once its animation is done. Skeletons and piglins pose their arms after this
 * method, so they're posed at the end of their own (SkeletonModelMixin, PiglinModelMixin).
 */
@Environment(EnvType.CLIENT)
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMeleeMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void playersVersus$poseSwing(HumanoidRenderState state, CallbackInfo info) {
        Object model = this;
        if (model instanceof SkeletonModel || model instanceof PiglinModel) return;
        MeleeAnimation.poseHumanoid((HumanoidModel<?>) model, state);
    }
}
