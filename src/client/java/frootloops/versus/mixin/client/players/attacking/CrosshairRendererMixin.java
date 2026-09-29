package frootloops.versus.mixin.client.players.attacking;

import frootloops.versus.mod.Combat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Environment(EnvType.CLIENT)
@Mixin(value = Gui.class, priority = 9999)
public class CrosshairRendererMixin {

    @Shadow
    private final Minecraft minecraft;

    @Shadow
    private final DebugScreenOverlay debugOverlay;

    @Shadow private static final Identifier CROSSHAIR_SPRITE = Identifier.parse("hud/crosshair");
    @Shadow private static final Identifier CROSSHAIR_ATTACK_INDICATOR_FULL_SPRITE = Identifier.parse("hud/crosshair_attack_indicator_full");
    @Shadow private static final Identifier CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_SPRITE = Identifier.parse("hud/crosshair_attack_indicator_background");
    @Shadow private static final Identifier CROSSHAIR_ATTACK_INDICATOR_PROGRESS_SPRITE = Identifier.parse("hud/crosshair_attack_indicator_progress");


    @Shadow
    private boolean canRenderCrosshairForSpectator(HitResult hitResult) {return false;}
    public CrosshairRendererMixin(Minecraft client, int scaledWidth, int scaledHeight, DebugScreenOverlay debugHud) {
        this.minecraft = client;
        this.debugOverlay = debugHud;
    }

    @Overwrite
    private void renderCrosshair(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Options gameOptions = this.minecraft.options;
        if (gameOptions.getCameraType().isFirstPerson()) {
            if (this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR || this.canRenderCrosshairForSpectator(this.minecraft.hitResult)) {
                if (!this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR)) {
                    context.nextStratum();

                    // Changes start here:
                    // This is the code that makes the crosshair's size depend on the attack cooldown:
                    float attackCooldownProgress = this.minecraft.player.getAttackStrengthScale(0.0F);
                    int crosshairSize = 1 + 2 * (int)(7f * attackCooldownProgress);
                    context.blitSprite(
                            RenderPipelines.CROSSHAIR, CROSSHAIR_SPRITE,
                            (context.guiWidth() - crosshairSize) / 2,
                            (context.guiHeight() - crosshairSize) / 2,
                            crosshairSize,
                            crosshairSize
                    );

                    if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR) {

                        // Second change:
                        // This is to make sure that the entity truly is targettable:
                        boolean isEntityTargettable = false;
                        if (this.minecraft.crosshairPickEntity != null && Combat.isInAttackRangeOf(minecraft.player, minecraft.crosshairPickEntity, attackCooldownProgress)) {
                            isEntityTargettable = this.minecraft.player.getCurrentItemAttackStrengthDelay() > 5.0F;
                            isEntityTargettable &= this.minecraft.crosshairPickEntity.isAlive();
                        }

                        // Back to vanilla here:
                        int j = context.guiHeight() / 2 - 7 + 16;
                        int k = context.guiWidth() / 2 - 8;
                        if (isEntityTargettable) {
                            context.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_FULL_SPRITE, k, j, 16, 16);
                        } else if (attackCooldownProgress < 1.0F) {
                            int l = (int)(attackCooldownProgress * 17.0F);
                            context.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_SPRITE, k, j, 16, 4);
                            context.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_PROGRESS_SPRITE, 16, 4, 0, 0, k, j, l, 4);
                        }
                    }
                }
            }
        }
    }
}
