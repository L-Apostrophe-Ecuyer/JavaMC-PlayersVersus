package frootloops.versus.mixin.mobs.hostile.overworld.shields;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.AbstractZombieModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractZombieModel.class)
public abstract class ZombieModelMixin<T extends HostileEntity> extends BipedEntityModel<T> {
    public ZombieModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void setAnglesForShield(T hostileEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        if(hostileEntity.getPose() == EntityPose.CROUCHING) {
            if(hostileEntity.isBlocking()) {
                this.leftArm.pitch = MathHelper.cos(f * 0.6662f) * 2.0f * g * 0.5f;
                this.leftArm.pitch = this.leftArm.pitch * 0.5f - 0.9424779f;
                this.leftArm.yaw = 0.5235988f;
            }
        }
    }
}
