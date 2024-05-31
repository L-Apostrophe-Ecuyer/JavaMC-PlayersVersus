package frootloops.versus.mixin.mobs.hostile.shields;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PiglinEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Environment(EnvType.CLIENT)
@Mixin(PiglinEntityModel.class)
public abstract class PiglinModelMixin<T extends MobEntity> extends PlayerEntityModel<T> {

    public PiglinModelMixin(ModelPart root, boolean thinArms) {
        super(root, thinArms);
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
