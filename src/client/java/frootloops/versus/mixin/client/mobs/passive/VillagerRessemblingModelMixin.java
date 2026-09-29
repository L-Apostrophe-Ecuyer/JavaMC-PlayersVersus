package frootloops.versus.mixin.client.mobs.passive;


import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(VillagerModel.class)
public abstract class VillagerRessemblingModelMixin extends EntityModel<VillagerRenderState> implements HeadedModel, VillagerLikeModel {

    private ModelPart rightEar;
    private ModelPart leftEar;

    protected VillagerRessemblingModelMixin(ModelPart modelPart, Function<ResourceLocation, RenderType> function) {
        super(modelPart, function);
    }


    @Inject(method = "<init>", at = @At("TAIL"))
    public void constructorHead(ModelPart root, CallbackInfo ci) {
        this.rightEar = root.getChild(PartNames.HEAD).getChild(PartNames.LEFT_EAR);
        this.leftEar = root.getChild(PartNames.HEAD).getChild(PartNames.RIGHT_EAR);
    }


    @Inject(method = "createBodyModel", at = @At("RETURN"), cancellable = true)
    private static void constructorHead(CallbackInfoReturnable<MeshDefinition> cir) {
        MeshDefinition modelData = cir.getReturnValue();
        PartDefinition modelPartData = modelData.getRoot().getChild(PartNames.HEAD);
        modelPartData.addOrReplaceChild(PartNames.LEFT_EAR, CubeListBuilder.create().texOffs(56, 0).addBox(0.0f, 0.0f, -2.0f, 1.0f, 4.0f, 3.0f, CubeDeformation.NONE), PartPose.offsetAndRotation(3.9f, -6.0f, 0.0f, 0.0f, 0.0f, -0.5235988f));
        modelPartData.addOrReplaceChild(PartNames.RIGHT_EAR, CubeListBuilder.create().texOffs(56, 0).addBox(-1.0f, 0.0f, -2.0f, 1.0f, 4.0f, 3.0f, CubeDeformation.NONE), PartPose.offsetAndRotation(-3.9f, -6.0f, 0.0f, 0.0f, 0.0f, 0.5235988f));
        cir.setReturnValue(modelData);
    }


    @Inject(method = "setupAnim", at = @At("HEAD"))
    public void setAngles(VillagerRenderState renderState, CallbackInfo info) {
        float f = renderState.walkAnimationPos;
        float g = renderState.walkAnimationSpeed;
        float h = (float) (Math.PI / 6);
        float i = renderState.ageInTicks * 0.1F + f * 0.5F;
        float j = 0.08F + g * 0.4F;
        this.leftEar.zRot = (float) (-h) - Mth.cos(i * 1.2F) * j;
        this.rightEar.zRot = (float) (h) + Mth.cos(i) * j;
    }

}
