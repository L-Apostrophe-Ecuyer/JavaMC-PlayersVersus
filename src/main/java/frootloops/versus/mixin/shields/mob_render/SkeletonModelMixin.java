package frootloops.versus.mixin.shields.mob_render;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.SkeletonEntityModel;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(SkeletonEntityModel.class)
public abstract class SkeletonModelMixin<T extends MobEntity> extends BipedEntityModel<T> {


    public SkeletonModelMixin(ModelPart root, Function<Identifier, RenderLayer> renderLayerFactory) {
        super(root, renderLayerFactory);
    }

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void setAnglesForShield(T hostileEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        if(hostileEntity.getPose() == EntityPose.CROUCHING && hostileEntity.isBlocking()) {
            this.leftArm.pitch = MathHelper.cos(f * 0.6662f) * 2.0f * g * 0.5f;
            this.leftArm.pitch = this.leftArm.pitch * 0.5f - 0.9424779f;
            this.leftArm.yaw = 0.5235988f;
        }
    }
}
