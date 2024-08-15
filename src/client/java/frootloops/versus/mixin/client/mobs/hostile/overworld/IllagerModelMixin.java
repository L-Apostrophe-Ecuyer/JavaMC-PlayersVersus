package frootloops.versus.mixin.client.mobs.hostile.overworld;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.IllagerEntityModel;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IllagerEntityModel.class)
public abstract class IllagerModelMixin<T extends IllagerEntity> extends SinglePartEntityModel<T>  {

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
    public void setAngles(T illagerEntity, float f, float g, float h, float i, float j, CallbackInfo info) {
        float k = 0.5235988f;
        float l = h * 0.1f + f * 0.5f;
        float m = 0.08f + g * 0.4f;
        this.leftEar.roll = k - MathHelper.cos((float)(l * 1.2f)) * m;
        this.rightEar.roll = -k + MathHelper.cos((float)l) * m;
    }

}
