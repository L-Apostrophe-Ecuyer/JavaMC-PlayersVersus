package frootloops.versus.mixin.items;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class FirstPersonAnimationsMixin {

    @Shadow
    private final MinecraftClient client;

    protected FirstPersonAnimationsMixin(MinecraftClient client) {
        this.client = client;
    }

    @Shadow
    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate((float)i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    @Shadow
    private void applyBrushTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, float equipProgress) {}

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/util/UseAction;"), cancellable = true)
    private void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack itemStack, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo info) {
        if (itemStack.getItem() instanceof ToolItem) {
            Arm arm = (hand == Hand.MAIN_HAND) ? player.getMainArm() : player.getMainArm().getOpposite();
            boolean isRightArm = arm == Arm.RIGHT;
            this.applyEquipOffset(matrices, arm, equipProgress);

            UseAction action = itemStack.getUseAction();
            if (action == UseAction.BLOCK) this.applyToolBlockingTransformation(matrices, arm);
            else if(action == UseAction.BRUSH) this.applySweepingTransformation(matrices, tickDelta, arm, itemStack, equipProgress);

            ((HeldItemRenderer)((Object)this)).renderItem(player, itemStack, isRightArm ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !isRightArm, matrices, vertexConsumers, light);
            matrices.pop();
            info.cancel();
        }
    }


    private void applyToolBlockingTransformation(MatrixStack matrices, Arm arm) {
        int horizontal = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(horizontal * -0.14142136F, 0.08F, 0.14142136F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-102.25F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(horizontal * 13.365F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(horizontal * 78.05F));
    }

    private void applySweepingTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, float equipProgress) {
        if (arm == Arm.LEFT) {
            float f = (180f + (float)this.client.player.getItemUseTimeLeft()) - tickDelta + 1.0F;
            float g = 1.0F - f / (1000f);
            float m = -15.0F + 75.0F * MathHelper.cos(g * 45.0F * 3.1415927F);
            matrices.translate(0.1, 0.42, 0.35);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(m));
            matrices.translate(-0.3, 0.42, 0.35);
        } else {
            float f = (180f + (float)this.client.player.getItemUseTimeLeft()) - tickDelta + 1.0F;
            float g = 1.0F - f / (1000f);
            float m = -15.0F + 75.0F * MathHelper.cos(g * 45.0F * 3.1415927F);
            matrices.translate(-0.25, 0.22, 0.35);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(m));
        }
    }

}
