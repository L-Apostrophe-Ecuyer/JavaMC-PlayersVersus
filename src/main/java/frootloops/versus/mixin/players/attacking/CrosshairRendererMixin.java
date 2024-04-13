package frootloops.versus.mixin.players.attacking;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;


import static net.minecraft.client.gui.DrawableHelper.drawTexture;

@Environment(EnvType.CLIENT)
@Mixin(value = InGameHud.class, priority = 9999)
public class CrosshairRendererMixin {

    private static final int ICON_SIZE = 31;

    @Shadow
    private final MinecraftClient client;

    @Shadow
    private int scaledWidth,scaledHeight;

    @Shadow
    private boolean shouldRenderSpectatorCrosshair(HitResult hitResult) {return false;}
    public CrosshairRendererMixin(MinecraftClient client) {
        this.client = client;
    }

    @Overwrite
    private void renderCrosshair(MatrixStack matrices) {
        GameOptions gameOptions = this.client.options;
        if (gameOptions.getPerspective().isFirstPerson()) {
            if (this.client.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR || this.shouldRenderSpectatorCrosshair(this.client.crosshairTarget)) {

                if (gameOptions.debugEnabled && !gameOptions.hudHidden && !this.client.player.hasReducedDebugInfo() && !(Boolean)gameOptions.getReducedDebugInfo().getValue()) {

                    Camera camera = this.client.gameRenderer.getCamera();
                    MatrixStack matrixStack = RenderSystem.getModelViewStack();
                    matrixStack.push();
                    matrixStack.multiplyPositionMatrix(matrices.peek().getPositionMatrix());
                    matrixStack.translate((float)(this.scaledWidth / 2), (float)(this.scaledHeight / 2), 0.0F);
                    matrixStack.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(camera.getPitch()));
                    matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw()));
                    matrixStack.scale(-1.0F, -1.0F, -1.0F);
                    RenderSystem.applyModelViewMatrix();
                    RenderSystem.renderCrosshair(10);
                    matrixStack.pop();
                    RenderSystem.applyModelViewMatrix();

                } else {
                    RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR, GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);

                    // Changes start here:
                    // This is the code that makes the crosshair's size depend on the attack cooldown:
                    float attackCooldownProgress = this.client.player.getAttackCooldownProgress(-1.0F);
                    int crosshairSize = 1 + 2 * (int)(7f * attackCooldownProgress);
                    int uv = 7 - crosshairSize/2;
                    drawTexture(matrices, (this.scaledWidth - crosshairSize) / 2, (this.scaledHeight - crosshairSize) / 2, 0, uv, uv, crosshairSize, crosshairSize, 256, 256);

                    // This is the code to make sure the attack indicator only shows when a target can be hit:
                    if (this.client.options.getAttackIndicator().getValue() == AttackIndicator.CROSSHAIR && this.client.targetedEntity != null && this.client.targetedEntity.isAlive()) {
                        int j = this.scaledHeight / 2 - 7 + 16;
                        int k = this.scaledWidth / 2 - 8;
                        if (attackCooldownProgress >= 1.0F) {
                            drawTexture(matrices, k, j, 68, 94, 16, 16);
                        } else if (attackCooldownProgress < 1.0F) {
                            int l = (int)(attackCooldownProgress * 17.0F);
                            drawTexture(matrices, k, j, 36, 94, 16, 4);
                            drawTexture(matrices, k, j, 52, 94, l, 4);
                        }
                    }
                    RenderSystem.defaultBlendFunc();
                }
            }
        }
    }
}