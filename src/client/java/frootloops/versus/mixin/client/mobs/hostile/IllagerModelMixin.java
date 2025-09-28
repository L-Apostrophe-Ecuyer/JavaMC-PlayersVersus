package frootloops.versus.mixin.client.mobs.hostile;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import frootloops.versus.VersusMod;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.render.entity.state.IllagerEntityRenderState;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IllagerEntityModel.class)
public abstract class IllagerModelMixin<S extends IllagerEntityRenderState> extends EntityModel<S> implements ModelWithArms, ModelWithHead {
    private ModelPart rightEar;
    private ModelPart leftEar;

    @Shadow private final ModelPart leftArm;
    @Shadow private final ModelPart rightArm;
    @Shadow private final ModelPart arms;
    @Shadow private final ModelPart head;

    protected IllagerModelMixin(ModelPart root, ModelPart root1, ModelPart leftArm, ModelPart rightArm, ModelPart arms, ModelPart head) {
        super(root);
        this.leftArm = leftArm;
        this.rightArm = rightArm;
        this.arms = arms;
        this.head = head;
    }


    @Inject(method = "<init>", at = @At("TAIL"))
    public void constructorHead(ModelPart modelPart, CallbackInfo ci) {
        this.rightEar = modelPart.getChild(EntityModelPartNames.HEAD).getChild(EntityModelPartNames.LEFT_EAR);
        this.leftEar = modelPart.getChild(EntityModelPartNames.HEAD).getChild(EntityModelPartNames.RIGHT_EAR);
    }

    @Overwrite
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData modelPartData2 = modelPartData.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F), ModelTransform.origin(0.0F, 0.0F, 0.0F));
        modelPartData2.addChild("hat", ModelPartBuilder.create().uv(32, 0).cuboid(-4.0F, -10.0F, -4.0F, 8.0F, 12.0F, 8.0F, new Dilation(0.45F)), ModelTransform.NONE);
        modelPartData2.addChild("nose", ModelPartBuilder.create().uv(24, 0).cuboid(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F), ModelTransform.origin(0.0F, -2.0F, 0.0F));

        modelPartData2.addChild(EntityModelPartNames.LEFT_EAR, ModelPartBuilder.create().uv(56, 0).cuboid(0.0f, 0.0f, -2.0f, 1.0f, 4.0f, 3.0f, Dilation.NONE), ModelTransform.of(3.9f, -6.0f, 0.0f, 0.0f, 0.0f, -0.5235988f));
        modelPartData2.addChild(EntityModelPartNames.RIGHT_EAR, ModelPartBuilder.create().uv(56, 0).cuboid(-1.0f, 0.0f, -2.0f, 1.0f, 4.0f, 3.0f, Dilation.NONE), ModelTransform.of(-3.9f, -6.0f, 0.0f, 0.0f, 0.0f, 0.5235988f));

        modelPartData.addChild("body", ModelPartBuilder.create().uv(16, 20).cuboid(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F).uv(0, 38).cuboid(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new Dilation(0.5F)), ModelTransform.origin(0.0F, 0.0F, 0.0F));
        ModelPartData modelPartData3 = modelPartData.addChild("arms", ModelPartBuilder.create().uv(44, 22).cuboid(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F).uv(40, 38).cuboid(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F), ModelTransform.of(0.0F, 3.0F, -1.0F, -0.75F, 0.0F, 0.0F));
        modelPartData3.addChild("left_shoulder", ModelPartBuilder.create().uv(44, 22).mirrored().cuboid(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F), ModelTransform.NONE);
        modelPartData.addChild("right_leg", ModelPartBuilder.create().uv(0, 22).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), ModelTransform.origin(-2.0F, 12.0F, 0.0F));
        modelPartData.addChild("left_leg", ModelPartBuilder.create().uv(0, 22).mirrored().cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), ModelTransform.origin(2.0F, 12.0F, 0.0F));
        modelPartData.addChild("right_arm", ModelPartBuilder.create().uv(40, 46).cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), ModelTransform.origin(-5.0F, 2.0F, 0.0F));
        modelPartData.addChild("left_arm", ModelPartBuilder.create().uv(40, 46).mirrored().cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), ModelTransform.origin(5.0F, 2.0F, 0.0F));
        return TexturedModelData.of(modelData, 64, 64);
    }


    @Inject(method = "setAngles", at = @At("HEAD"))
    public void setAngles(S renderState, CallbackInfo info) {
        float f = renderState.limbSwingAnimationProgress;
        float g = renderState.limbSwingAmplitude;
        float h = (float) (Math.PI / 6);
        float i = renderState.age * 0.1F + f * 0.5F;
        float j = 0.08F + g * 0.4F;
        this.leftEar.roll = (float) (-h ) - MathHelper.cos(i * 1.2F) * j;
        this.rightEar.roll = (float) (h) + MathHelper.cos(i) * j;
    }

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void setAnglesForTool(S renderState, CallbackInfo info) {
        if(renderState.pose == EntityPose.CROAKING) {
            this.arms.visible = true;
            this.leftArm.visible = true;
            this.rightArm.visible = true;

            this.rightArm.pitch = MathHelper.clamp((float)(this.head.pitch - 1.9198622f), (float)-2.4f, (float)3.3f);
            this.rightArm.yaw = this.head.yaw - 0.2617994f;

            this.leftArm.pitch = MathHelper.clamp((float)(this.head.pitch - 1.9198622f), (float)-2.4f, (float)3.3f);
            this.leftArm.yaw = this.head.yaw + 0.2617994f;
        }
        else if(renderState.pose == EntityPose.CROUCHING) {
            this.arms.visible = true;
            this.leftArm.visible = true;
            this.rightArm.visible = true;
            this.positionBlockingArm(leftArm, false);
        }
    }

    private void positionBlockingArm(ModelPart arm, boolean rightArm) {
        arm.pitch = arm.pitch * 0.5F - 0.9424779F + MathHelper.clamp(this.head.pitch, (float) (-Math.PI * 4.0 / 9.0), 0.43633232F);
        arm.yaw = (rightArm ? -30.0F : 30.0F) * (float) (Math.PI / 180.0) + MathHelper.clamp(this.head.yaw, (float) (-Math.PI / 6), (float) (Math.PI / 6));
    }
}
