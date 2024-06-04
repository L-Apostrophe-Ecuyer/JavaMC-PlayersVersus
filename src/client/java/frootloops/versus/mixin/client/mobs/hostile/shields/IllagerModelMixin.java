package frootloops.versus.mixin.client.mobs.hostile.shields;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.IllagerEntityModel;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Environment(EnvType.CLIENT)
@Mixin(IllagerEntityModel.class)
public abstract class IllagerModelMixin<T extends IllagerEntity> extends SinglePartEntityModel<T> implements ModelWithArms, ModelWithHead {

    @Shadow
    private final ModelPart leftArm;

    @Shadow
    private final ModelPart rightArm;

    @Shadow
    private final ModelPart arms;

    protected IllagerModelMixin(ModelPart leftArm, ModelPart rightArm, ModelPart arms) {
        this.leftArm = leftArm;
        this.rightArm = rightArm;
        this.arms = arms;
    }

    @Inject(method = "setAngles", at = @At("HEAD"))
    private void showArmsWhileBlocking(T hostileEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        if(hostileEntity.getPose() == EntityPose.CROUCHING && hostileEntity.isBlocking()) {
            hostileEntity.setAttacking(true);
        }
    }

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void setAnglesForShield(T hostileEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        if(hostileEntity.getPose() == EntityPose.CROUCHING && hostileEntity.isBlocking()) {
            this.arms.visible = true;
            this.leftArm.visible = true;
            this.rightArm.visible = true;
            this.leftArm.pitch = MathHelper.cos(f * 0.6662f) * 2.0f * g * 0.5f;
            this.leftArm.pitch = this.leftArm.pitch * 0.5f - 0.9424779f;
            this.leftArm.yaw = 0.5235988f;
        }
    }
}
