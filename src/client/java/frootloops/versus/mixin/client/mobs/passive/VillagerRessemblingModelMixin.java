package frootloops.versus.mixin.client.mobs.passive;


import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.client.render.entity.state.VillagerEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerResemblingModel.class)
public abstract class VillagerRessemblingModelMixin extends EntityModel<VillagerEntityRenderState> {

    @Shadow private final ModelPart root;

    private ModelPart rightEar;
    private ModelPart leftEar;

    public VillagerRessemblingModelMixin(ModelPart root, ModelPart rightEar, ModelPart leftEar) {
        this.root = root;
        this.rightEar = rightEar;
        this.leftEar = leftEar;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void constructorHead(ModelPart root, CallbackInfo ci) {
        this.rightEar = root.getChild(EntityModelPartNames.HEAD).getChild(EntityModelPartNames.LEFT_EAR);
        this.leftEar = root.getChild(EntityModelPartNames.HEAD).getChild(EntityModelPartNames.RIGHT_EAR);
    }


    @Inject(method = "getModelData", at = @At("RETURN"), cancellable = true)
    private static void constructorHead(CallbackInfoReturnable<ModelData> cir) {
        ModelData modelData = cir.getReturnValue();
        ModelPartData modelPartData = modelData.getRoot().getChild(EntityModelPartNames.HEAD);
        modelPartData.addChild(EntityModelPartNames.LEFT_EAR, ModelPartBuilder.create().uv(56, 0).cuboid(0.0f, 0.0f, -2.0f, 1.0f, 4.0f, 3.0f, Dilation.NONE), ModelTransform.of(3.9f, -6.0f, 0.0f, 0.0f, 0.0f, -0.5235988f));
        modelPartData.addChild(EntityModelPartNames.RIGHT_EAR, ModelPartBuilder.create().uv(56, 0).cuboid(-1.0f, 0.0f, -2.0f, 1.0f, 4.0f, 3.0f, Dilation.NONE), ModelTransform.of(-3.9f, -6.0f, 0.0f, 0.0f, 0.0f, 0.5235988f));
        cir.setReturnValue(modelData);
    }


    @Inject(method = "setAngles", at = @At("HEAD"))
    public void setAngles(VillagerEntityRenderState renderState, CallbackInfo info) {
        float f = renderState.limbFrequency;
        float g = renderState.limbAmplitudeMultiplier;
        float i = renderState.age * 0.1F + f * 0.5F;
        float j = 0.08F + g * 0.4F;
        this.leftEar.roll = (float) (-Math.PI / 6) - MathHelper.cos(i * 1.2F) * j;
        this.rightEar.roll = (float) (Math.PI / 6) + MathHelper.cos(i) * j;
    }

}
