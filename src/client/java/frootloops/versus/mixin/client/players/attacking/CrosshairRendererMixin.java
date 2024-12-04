package frootloops.versus.mixin.client.players.attacking;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import frootloops.versus.mod.Combat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.GameMode;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Environment(EnvType.CLIENT)
@Mixin(value = InGameHud.class, priority = 9999)
public class CrosshairRendererMixin {

    @Shadow
    private final MinecraftClient client;

    @Shadow
    private final DebugHud debugHud;

    @Shadow private static final Identifier CROSSHAIR_TEXTURE = Identifier.of("hud/crosshair");
    @Shadow private static final Identifier CROSSHAIR_ATTACK_INDICATOR_FULL_TEXTURE = Identifier.of("hud/crosshair_attack_indicator_full");
    @Shadow private static final Identifier CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_TEXTURE = Identifier.of("hud/crosshair_attack_indicator_background");
    @Shadow private static final Identifier CROSSHAIR_ATTACK_INDICATOR_PROGRESS_TEXTURE = Identifier.of("hud/crosshair_attack_indicator_progress");


    @Shadow
    private boolean shouldRenderSpectatorCrosshair(HitResult hitResult) {return false;}
    public CrosshairRendererMixin(MinecraftClient client, int scaledWidth, int scaledHeight, DebugHud debugHud) {
        this.client = client;
        this.debugHud = debugHud;
    }

    @Overwrite
    private void renderCrosshair(DrawContext context, RenderTickCounter tickCounter) {
        GameOptions gameOptions = this.client.options;
        if (gameOptions.getPerspective().isFirstPerson()) {
            if (this.client.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR || this.shouldRenderSpectatorCrosshair(this.client.crosshairTarget)) {

                if (this.debugHud.shouldShowDebugHud() && !gameOptions.hudHidden && !this.client.player.hasReducedDebugInfo() && !(Boolean)gameOptions.getReducedDebugInfo().getValue()) {

                    Camera camera = this.client.gameRenderer.getCamera();
                    Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
                    matrix4fStack.pushMatrix();
                    matrix4fStack.mul(context.getMatrices().peek().getPositionMatrix());
                    matrix4fStack.translate((float)(context.getScaledWindowWidth() / 2), (float)(context.getScaledWindowHeight() / 2), 0.0F);
                    matrix4fStack.rotateX(-camera.getPitch() * (float) (Math.PI / 180.0));
                    matrix4fStack.rotateY(camera.getYaw() * (float) (Math.PI / 180.0));
                    matrix4fStack.scale(-1.0F, -1.0F, -1.0F);
                    RenderSystem.renderCrosshair(10);
                    matrix4fStack.popMatrix();

                } else {
                    RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR, GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);

                    // Changes start here:
                    // This is the code that makes the crosshair's size depend on the attack cooldown:
                    float attackCooldownProgress = client.player.getAttackCooldownProgress(0.0f);
                    int crosshairSize = 1 + 2 * (int)(7d * attackCooldownProgress);
                    context.drawGuiTexture(RenderLayer::getCrosshair, CROSSHAIR_TEXTURE, (context.getScaledWindowWidth() - crosshairSize) / 2, (context.getScaledWindowHeight() - crosshairSize) / 2, crosshairSize, crosshairSize);

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
                            context.drawGuiTexture(RenderLayer::getCrosshair, CROSSHAIR_ATTACK_INDICATOR_FULL_TEXTURE, posX, posY, 16, 16);
                        } else {
                            int swordIconWhitePixels = (int)(attackCooldownProgress * 17.0D);
                            context.drawGuiTexture(RenderLayer::getCrosshair, CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_TEXTURE, posX, posY, 16, 4);
                            context.drawGuiTexture(RenderLayer::getCrosshair, CROSSHAIR_ATTACK_INDICATOR_PROGRESS_TEXTURE, 16, 4, 0, 0, posX, posY, swordIconWhitePixels, 4);
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
