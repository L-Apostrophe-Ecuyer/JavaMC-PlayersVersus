package frootloops.versus.mixin.client.mobs.hostile;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.model.monster.illager.IllagerModel;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.mobs.melee.MeleeAnimation;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IllagerModel.class)
public abstract class IllagerModelMixin<S extends IllagerRenderState> extends EntityModel<S> implements ArmedModel, HeadedModel {
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
        this.rightEar = modelPart.getChild(PartNames.HEAD).getChild(PartNames.LEFT_EAR);
        this.leftEar = modelPart.getChild(PartNames.HEAD).getChild(PartNames.RIGHT_EAR);
    }

    @Overwrite
    public static LayerDefinition createBodyLayer() {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition modelPartData = modelData.getRoot();
        PartDefinition modelPartData2 = modelPartData.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        modelPartData2.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 12.0F, 8.0F, new CubeDeformation(0.45F)), PartPose.ZERO);
        modelPartData2.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F), PartPose.offset(0.0F, -2.0F, 0.0F));

        modelPartData2.addOrReplaceChild(PartNames.LEFT_EAR, CubeListBuilder.create().texOffs(56, 0).addBox(0.0f, 0.0f, -2.0f, 1.0f, 4.0f, 3.0f, CubeDeformation.NONE), PartPose.offsetAndRotation(3.9f, -6.0f, 0.0f, 0.0f, 0.0f, -0.5235988f));
        modelPartData2.addOrReplaceChild(PartNames.RIGHT_EAR, CubeListBuilder.create().texOffs(56, 0).addBox(-1.0f, 0.0f, -2.0f, 1.0f, 4.0f, 3.0f, CubeDeformation.NONE), PartPose.offsetAndRotation(-3.9f, -6.0f, 0.0f, 0.0f, 0.0f, 0.5235988f));

        modelPartData.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F).texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition modelPartData3 = modelPartData.addOrReplaceChild("arms", CubeListBuilder.create().texOffs(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F).texOffs(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 3.0F, -1.0F, -0.75F, 0.0F, 0.0F));
        modelPartData3.addOrReplaceChild("left_shoulder", CubeListBuilder.create().texOffs(44, 22).mirror().addBox(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F), PartPose.ZERO);
        modelPartData.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-2.0F, 12.0F, 0.0F));
        modelPartData.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(2.0F, 12.0F, 0.0F));
        modelPartData.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 46).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        modelPartData.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 46).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        return LayerDefinition.create(modelData, 64, 64);
    }


    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/IllagerRenderState;)V", at = @At("HEAD"))
    public void setAngles(S renderState, CallbackInfo info) {
        float f = renderState.walkAnimationPos;
        float g = renderState.walkAnimationSpeed;
        float h = (float) (Math.PI / 6);
        float i = renderState.ageInTicks * 0.1F + f * 0.5F;
        float j = 0.08F + g * 0.4F;
        this.leftEar.zRot = (float) (-h ) - Mth.cos(i * 1.2F) * j;
        this.rightEar.zRot = (float) (h) + Mth.cos(i) * j;
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/IllagerRenderState;)V", at = @At("TAIL"))
    private void setAnglesForTool(S renderState, CallbackInfo info) {
        if(renderState.pose == Pose.CROAKING) {
            this.arms.visible = true;
            this.leftArm.visible = true;
            this.rightArm.visible = true;

            this.rightArm.xRot = Mth.clamp((float)(this.head.xRot - 1.9198622f), (float)-2.4f, (float)3.3f);
            this.rightArm.yRot = this.head.yRot - 0.2617994f;

            this.leftArm.xRot = Mth.clamp((float)(this.head.xRot - 1.9198622f), (float)-2.4f, (float)3.3f);
            this.leftArm.yRot = this.head.yRot + 0.2617994f;
        }
        else if(renderState.pose == Pose.CROUCHING) {
            this.arms.visible = true;
            this.leftArm.visible = true;
            this.rightArm.visible = true;
            this.positionBlockingArm(leftArm, false);
        }
        MeleeAnimation.poseIllager(this.head, this.rightArm, this.leftArm, renderState);
    }

    private void positionBlockingArm(ModelPart arm, boolean rightArm) {
        arm.xRot = arm.xRot * 0.5F - 0.9424779F + Mth.clamp(this.head.xRot, (float) (-Math.PI * 4.0 / 9.0), 0.43633232F);
        arm.yRot = (rightArm ? -30.0F : 30.0F) * (float) (Math.PI / 180.0) + Mth.clamp(this.head.yRot, (float) (-Math.PI / 6), (float) (Math.PI / 6));
    }
}
