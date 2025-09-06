package frootloops.versus.mixin.client.players.attacking;

import frootloops.versus.mod.Combat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.GameMode;
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

    @Shadow public boolean shouldRenderCrosshair() {
        return this.debugHud.shouldShowDebugHud()
                && this.client.options.getPerspective() == Perspective.FIRST_PERSON
                && !this.client.player.hasReducedDebugInfo()
                && !this.client.options.getReducedDebugInfo().getValue();
    }


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
                if (!this.shouldRenderCrosshair()) {
                    context.createNewRootLayer();
                    int i = 15;
                    context.drawGuiTexture(
                            RenderPipelines.CROSSHAIR, CROSSHAIR_TEXTURE, (context.getScaledWindowWidth() - 15) / 2, (context.getScaledWindowHeight() - 15) / 2, 15, 15
                    );

                    // Changes start here:
                    // This is the code that makes the crosshair's size depend on the attack cooldown:
                    float attackCooldownProgress = this.client.player.getAttackCooldownProgress(0.0F);
                    int crosshairSize = 1 + 2 * (int)(7f * attackCooldownProgress);
                    context.drawGuiTexture(
                            RenderPipelines.CROSSHAIR, CROSSHAIR_TEXTURE,
                            (context.getScaledWindowWidth() - crosshairSize) / 2,
                            (context.getScaledWindowHeight() - crosshairSize) / 2,
                            crosshairSize,
                            crosshairSize
                    );

                    if (this.client.options.getAttackIndicator().getValue() == AttackIndicator.CROSSHAIR) {

                        // Second change:
                        // This is to make sure that the entity truly is targettable:
                        boolean isEntityTargettable = false;
                        if (this.client.targetedEntity != null && Combat.isInAttackRangeOf(client.player, client.targetedEntity, attackCooldownProgress)) {
                            isEntityTargettable = this.client.player.getAttackCooldownProgressPerTick() > 5.0F;
                            isEntityTargettable &= this.client.targetedEntity.isAlive();
                        }

                        // Back to vanilla here:
                        int j = context.getScaledWindowHeight() / 2 - 7 + 16;
                        int k = context.getScaledWindowWidth() / 2 - 8;
                        if (isEntityTargettable) {
                            context.drawGuiTexture(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_FULL_TEXTURE, k, j, 16, 16);
                        } else if (attackCooldownProgress < 1.0F) {
                            int l = (int)(attackCooldownProgress * 17.0F);
                            context.drawGuiTexture(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_TEXTURE, k, j, 16, 4);
                            context.drawGuiTexture(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_PROGRESS_TEXTURE, 16, 4, 0, 0, k, j, l, 4);
                        }
                    }
                }
            }
        }
    }
}
