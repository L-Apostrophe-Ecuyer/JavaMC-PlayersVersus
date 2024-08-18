package frootloops.versus.mixin.client.mobs.hostile;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.IllagerEntityModel;
import net.minecraft.client.render.entity.state.IllagerEntityRenderState;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IllagerEntityModel.class)
public abstract class IllagerModelMixin<S extends IllagerEntityRenderState> extends EntityModel<S> {

    @Shadow private final ModelPart root;

    private ModelPart rightEar;
    private ModelPart leftEar;

    @Shadow private final ModelPart leftArm;
    @Shadow private final ModelPart rightArm;
    @Shadow private final ModelPart arms;
    @Shadow private final ModelPart head;

    public IllagerModelMixin(ModelPart root, ModelPart rightEar, ModelPart leftEar, ModelPart leftArm, ModelPart rightArm, ModelPart arms, ModelPart head) {
        this.root = root;
        this.rightEar = rightEar;
        this.leftEar = leftEar;
        this.leftArm = leftArm;
        this.rightArm = rightArm;
        this.arms = arms;
        this.head = head;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void constructorHead(ModelPart root, CallbackInfo ci) {
        this.rightEar = root.getChild(EntityModelPartNames.HEAD).getChild(EntityModelPartNames.LEFT_EAR);
        this.leftEar = root.getChild(EntityModelPartNames.HEAD).getChild(EntityModelPartNames.RIGHT_EAR);
    }

    @Overwrite
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData modelPartData2 = modelPartData.addChild(EntityModelPartNames.HEAD, ModelPartBuilder.create().uv(0, 0).cuboid(-4.0f, -10.0f, -4.0f, 8.0f, 10.0f, 8.0f), ModelTransform.pivot(0.0f, 0.0f, 0.0f));
        modelPartData2.addChild(EntityModelPartNames.HAT, ModelPartBuilder.create().uv(32, 0).cuboid(-4.0f, -10.0f, -4.0f, 8.0f, 12.0f, 8.0f, new Dilation(0.45f)), ModelTransform.NONE);
        modelPartData2.addChild(EntityModelPartNames.NOSE, ModelPartBuilder.create().uv(24, 0).cuboid(-1.0f, -1.0f, -6.0f, 2.0f, 4.0f, 2.0f), ModelTransform.pivot(0.0f, -2.0f, 0.0f));

        modelPartData2.addChild(EntityModelPartNames.LEFT_EAR, ModelPartBuilder.create().uv(56, 0).cuboid(0.0f, 0.0f, -2.0f, 1.0f, 4.0f, 3.0f, Dilation.NONE), ModelTransform.of(3.9f, -6.0f, 0.0f, 0.0f, 0.0f, -0.5235988f));
        modelPartData2.addChild(EntityModelPartNames.RIGHT_EAR, ModelPartBuilder.create().uv(56, 0).cuboid(-1.0f, 0.0f, -2.0f, 1.0f, 4.0f, 3.0f, Dilation.NONE), ModelTransform.of(-3.9f, -6.0f, 0.0f, 0.0f, 0.0f, 0.5235988f));

        modelPartData.addChild(EntityModelPartNames.BODY, ModelPartBuilder.create().uv(16, 20).cuboid(-4.0f, 0.0f, -3.0f, 8.0f, 12.0f, 6.0f).uv(0, 38).cuboid(-4.0f, 0.0f, -3.0f, 8.0f, 20.0f, 6.0f, new Dilation(0.5f)), ModelTransform.pivot(0.0f, 0.0f, 0.0f));
        ModelPartData modelPartData3 = modelPartData.addChild(EntityModelPartNames.ARMS, ModelPartBuilder.create().uv(44, 22).cuboid(-8.0f, -2.0f, -2.0f, 4.0f, 8.0f, 4.0f).uv(40, 38).cuboid(-4.0f, 2.0f, -2.0f, 8.0f, 4.0f, 4.0f), ModelTransform.of(0.0f, 3.0f, -1.0f, -0.75f, 0.0f, 0.0f));
        modelPartData3.addChild("left_shoulder", ModelPartBuilder.create().uv(44, 22).mirrored().cuboid(4.0f, -2.0f, -2.0f, 4.0f, 8.0f, 4.0f), ModelTransform.NONE);
        modelPartData.addChild(EntityModelPartNames.RIGHT_LEG, ModelPartBuilder.create().uv(0, 22).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f), ModelTransform.pivot(-2.0f, 12.0f, 0.0f));
        modelPartData.addChild(EntityModelPartNames.LEFT_LEG, ModelPartBuilder.create().uv(0, 22).mirrored().cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f), ModelTransform.pivot(2.0f, 12.0f, 0.0f));
        modelPartData.addChild(EntityModelPartNames.RIGHT_ARM, ModelPartBuilder.create().uv(40, 46).cuboid(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f), ModelTransform.pivot(-5.0f, 2.0f, 0.0f));
        modelPartData.addChild(EntityModelPartNames.LEFT_ARM, ModelPartBuilder.create().uv(40, 46).mirrored().cuboid(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f), ModelTransform.pivot(5.0f, 2.0f, 0.0f));
        return TexturedModelData.of(modelData, 64, 64);
    }


    @Inject(method = "setAngles", at = @At("HEAD"))
    public void setAngles(S renderState, CallbackInfo info) {
        float f = renderState.limbFrequency;
        float g = renderState.limbAmplitudeMultiplier;
        float i = renderState.age * 0.1F + f * 0.5F;
        float j = 0.08F + g * 0.4F;
        this.leftEar.roll = (float) (-Math.PI / 6) - MathHelper.cos(i * 1.2F) * j;
        this.rightEar.roll = (float) (Math.PI / 6) + MathHelper.cos(i) * j;
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
