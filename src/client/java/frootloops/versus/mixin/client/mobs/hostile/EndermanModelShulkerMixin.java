package frootloops.versus.mixin.client.mobs.hostile;

import frootloops.versus.mod.mobs.hostile.end.ShulkerCarrierRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * An enderman carrying a shulker holds it as it holds a block, arms forward, but wider apart: the shulker is a whole
 * block between its hands (ShulkerRendererCarriedMixin draws it there).
 */
@Environment(EnvType.CLIENT)
@Mixin(EndermanModel.class)
public abstract class EndermanModelShulkerMixin {
    /** The arms forward (as for a block) and out (to hold a whole block between them), radians. */
    @Unique
    private static final float FORWARD = -0.5F, OUT = 0.3F;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/EndermanRenderState;)V", at = @At("TAIL"))
    private void playersVersus$holdShulker(EndermanRenderState state, CallbackInfo info) {
        if (!((ShulkerCarrierRenderState) state).playersVersus$carriesShulker()) return;
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        model.rightArm.xRot = FORWARD;
        model.leftArm.xRot = FORWARD;
        model.rightArm.zRot = OUT;
        model.leftArm.zRot = -OUT;
    }
}
