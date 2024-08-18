package frootloops.versus.mixin.client.mobs.hostile;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.SkeletonEntityModel;
import net.minecraft.client.render.entity.state.SkeletonEntityRenderState;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
@Mixin(SkeletonEntityModel.class)
public abstract class SkeletonModelMixin<S extends SkeletonEntityRenderState> extends BipedEntityModel<S> {


    public SkeletonModelMixin(ModelPart root, Function<Identifier, RenderLayer> renderLayerFactory) {
        super(root, renderLayerFactory);
    }

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void setAnglesForShield(S renderState, CallbackInfo info) {
        if(renderState.pose == EntityPose.CROUCHING) {
            this.positionBlockingArm(this.leftArm, false);
        }
    }

    private void positionBlockingArm(ModelPart arm, boolean rightArm) {
        arm.pitch = arm.pitch * 0.5F - 0.9424779F + MathHelper.clamp(this.head.pitch, (float) (-Math.PI * 4.0 / 9.0), 0.43633232F);
        arm.yaw = (rightArm ? -30.0F : 30.0F) * (float) (Math.PI / 180.0) + MathHelper.clamp(this.head.yaw, (float) (-Math.PI / 6), (float) (Math.PI / 6));
    }
}
