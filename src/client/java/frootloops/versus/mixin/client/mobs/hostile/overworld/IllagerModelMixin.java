package frootloops.versus.mixin.client.mobs.hostile.overworld;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.IllagerEntityModel;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IllagerEntityModel.class)
public abstract class IllagerModelMixin<T extends Entity> extends SinglePartEntityModel<T>  {

    @Shadow private final ModelPart root;

    private ModelPart rightEar;
    private ModelPart leftEar;

    public IllagerModelMixin(ModelPart root, ModelPart rightEar, ModelPart leftEar) {
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
        modelPartData.addChild(EntityModelPartNames.LEFT_EAR, ModelPartBuilder.create().uv(56, 0).cuboid(0.0f, 0.0f, -2.0f, 1.0f, 5.0f, 3.0f, Dilation.NONE), ModelTransform.of(4.5f, -6.0f, 0.0f, 0.0f, 0.0f, -0.5235988f));
        modelPartData.addChild(EntityModelPartNames.RIGHT_EAR, ModelPartBuilder.create().uv(56, 0).cuboid(-1.0f, 0.0f, -2.0f, 1.0f, 5.0f, 3.0f, Dilation.NONE), ModelTransform.of(-4.5f, -6.0f, 0.0f, 0.0f, 0.0f, 0.5235988f));
        cir.setReturnValue(modelData);
    }


    @Inject(method = "setAngles", at = @At("HEAD"))
    public void setAngles(T mobEntity, float f, float g, float h, float i, float j, CallbackInfo info) {
        float k = 0.5235988f;
        float l = h * 0.1f + f * 0.5f;
        float m = 0.08f + g * 0.4f;
        this.leftEar.roll = k - MathHelper.cos((float)(l * 1.2f)) * m;
        this.rightEar.roll = -k + MathHelper.cos((float)l) * m;
    }

}
