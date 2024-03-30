package frootloops.versus.mixin.players.attacking;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import frootloops.versus.mod.Combat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = InGameHud.class, priority = 9999)
public class CrosshairRendererMixin {

    @Shadow
    private final MinecraftClient client;

    @Shadow
    private final DebugHud debugHud;

    @Shadow private static final Identifier CROSSHAIR_TEXTURE = new Identifier("hud/crosshair");
    @Shadow private static final Identifier CROSSHAIR_ATTACK_INDICATOR_FULL_TEXTURE = new Identifier("hud/crosshair_attack_indicator_full");
    @Shadow private static final Identifier CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_TEXTURE = new Identifier("hud/crosshair_attack_indicator_background");
    @Shadow private static final Identifier CROSSHAIR_ATTACK_INDICATOR_PROGRESS_TEXTURE = new Identifier("hud/crosshair_attack_indicator_progress");


    @Shadow
    private boolean shouldRenderSpectatorCrosshair(HitResult hitResult) {return false;}
    public CrosshairRendererMixin(MinecraftClient client, int scaledWidth, int scaledHeight, DebugHud debugHud) {
        this.client = client;
        this.debugHud = debugHud;
    }

    @Overwrite
    private void renderCrosshair(DrawContext context, float tickDelta) {
        GameOptions gameOptions = this.client.options;
        if (gameOptions.getPerspective().isFirstPerson()) {
            if (this.client.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR || this.shouldRenderSpectatorCrosshair(this.client.crosshairTarget)) {

                if (this.debugHud.shouldShowDebugHud() && !gameOptions.hudHidden && !this.client.player.hasReducedDebugInfo() && !(Boolean)gameOptions.getReducedDebugInfo().getValue()) {

                    Camera camera = this.client.gameRenderer.getCamera();
                    MatrixStack matrixStack = RenderSystem.getModelViewStack();
                    matrixStack.push();
                    matrixStack.multiplyPositionMatrix(context.getMatrices().peek().getPositionMatrix());
                    matrixStack.translate((float)(context.getScaledWindowWidth() / 2), (float)(context.getScaledWindowHeight() / 2), 0.0F);
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
                    double attackCooldownProgress = Combat.getAttackChargeProgress(client.player);
                    double attackCooldownProgressCapped = attackCooldownProgress > 1.0d ? 1.0d : attackCooldownProgress;
                    int crosshairSize = 1 + 2 * (int)(7d * attackCooldownProgressCapped);
                    context.drawGuiTexture(CROSSHAIR_TEXTURE, (context.getScaledWindowWidth() - crosshairSize) / 2, (context.getScaledWindowHeight() - crosshairSize) / 2, crosshairSize, crosshairSize);

                    // This is the code to make sure the attack indicator only shows when a target can be hit:
                    boolean isEntityTargettable = false;
                    if (this.client.targetedEntity != null && Combat.isInAttackRangeOf(client.player, client.targetedEntity, attackCooldownProgress)) {
                        isEntityTargettable = this.client.player.getAttackCooldownProgressPerTick() > 5.0f;
                        isEntityTargettable &= this.client.targetedEntity.isAlive();
                    }

                    if(isEntityTargettable) {
                        int posY = context.getScaledWindowHeight() / 2 - 7 + 16;
                        int posX = context.getScaledWindowWidth() / 2 - 8;
                        if (attackCooldownProgress >= 1.0F) {
                            context.drawGuiTexture(CROSSHAIR_ATTACK_INDICATOR_FULL_TEXTURE, posX, posY, 16, 16);
                        } else {
                            int swordIconWhitePixels = (int)(attackCooldownProgressCapped * 17.0D);
                            context.drawGuiTexture(CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_TEXTURE, posX, posY, 16, 4);
                            context.drawGuiTexture(CROSSHAIR_ATTACK_INDICATOR_PROGRESS_TEXTURE, 16, 4, 0, 0, posX, posY, swordIconWhitePixels, 4);
                        }
                    }
                    //if(ReacharoundTracker.currentTarget != null) this.drawExtraCrosshairIcon(matrices);
                }
            }
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.disableBlend();
    }
}
