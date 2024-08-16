package frootloops.versus.mixin.client.items;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class Deprecated_ThirdPersonAnimationsMixin<T extends LivingEntity> {

    @Shadow public final ModelPart rightArm;
    @Shadow public final ModelPart leftArm;
    @Shadow public BipedEntityModel.ArmPose leftArmPose = BipedEntityModel.ArmPose.EMPTY;
    @Shadow public BipedEntityModel.ArmPose rightArmPose = BipedEntityModel.ArmPose.EMPTY;

    protected Deprecated_ThirdPersonAnimationsMixin(ModelPart rightArm, ModelPart leftArm) {
        this.rightArm = rightArm;
        this.leftArm = leftArm;
    }

    @Inject(method = "positionRightArm", at = @At("HEAD"), cancellable = true)
    private void positionRightArm(T entity, CallbackInfo info) {
        if(this.rightArmPose == BipedEntityModel.ArmPose.BRUSH && entity.getMainHandStack().hasEnchantments() && entity.getMainHandStack().getItem() instanceof ToolItem toolItem) {
            if(!(toolItem instanceof SwordItem || toolItem instanceof HoeItem)) return;

            this.rightArm.pitch = -1.5f;
            this.rightArm.yaw = 0.0f;
            info.cancel();

        }
    }
}
