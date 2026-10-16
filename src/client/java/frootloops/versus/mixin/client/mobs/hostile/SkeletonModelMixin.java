package frootloops.versus.mixin.client.mobs.hostile;

import frootloops.versus.mod.mobs.melee.MeleeAnimation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
@Mixin(SkeletonModel.class)
public abstract class SkeletonModelMixin<S extends SkeletonRenderState> extends HumanoidModel<S> {


    public SkeletonModelMixin(ModelPart root, Function<Identifier, RenderType> renderLayerFactory) {
        super(root, renderLayerFactory);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/SkeletonRenderState;)V", at = @At("TAIL"))
    private void setAnglesForShield(S renderState, CallbackInfo info) {
        if(renderState.pose == Pose.CROUCHING) {
            this.poseBlockingArm(this.leftArm, false);
        }
        MeleeAnimation.poseHumanoid(this, renderState);
    }

    private void poseBlockingArm(ModelPart arm, boolean rightArm) {
        arm.xRot = arm.xRot * 0.5F - 0.9424779F + Mth.clamp(this.head.xRot, (float) (-Math.PI * 4.0 / 9.0), 0.43633232F);
        arm.yRot = (rightArm ? -30.0F : 30.0F) * (float) (Math.PI / 180.0) + Mth.clamp(this.head.yRot, (float) (-Math.PI / 6), (float) (Math.PI / 6));
    }
}
