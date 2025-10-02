package frootloops.versus.mixin.client.players;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.gui.hud.debug.DebugHudLines;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(DebugHud.class)
public abstract class DebugHudMixin {

    @Shadow private final MinecraftClient client;

    protected DebugHudMixin(MinecraftClient client) {
        this.client = client;
    }

    @Shadow
    private void drawText(DrawContext context, List<String> text, boolean left) {}

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void limitDebugWhenInSurvival(DrawContext context, CallbackInfo info) {
        if (this.client.getCameraEntity() != null && this.client.world != null) {
            boolean shouldRestrictDebug = this.client.player.getGameMode().isSurvivalLike();
            if (shouldRestrictDebug) {

                context.createNewRootLayer();
                Profiler profiler = Profilers.get();
                profiler.push("debug");

                final List<String> list = new ArrayList();
                boolean isDebugReduced = this.client.hasReducedDebugInfo();
                boolean isF3Enabled = this.client.debugHudEntryList.isF3Enabled();

                // Show player position if enabled:
                if(!isDebugReduced && (isF3Enabled || client.debugHudEntryList.isEntryVisible(DebugHudEntries.PLAYER_POSITION))) {
                    BlockPos blockPos = client.player.getBlockPos();
                    list.add(String.format(Locale.ROOT, " %d %d %d", blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                }

                // Show player FPS if enabled:
                if(client.debugHudEntryList.isEntryVisible(DebugHudEntries.FPS)) {
                    list.add( String.format(Locale.ROOT, "%d FPS", client.getCurrentFps()));
                }

                // And that's it!
                this.drawText(context, list, true);
                info.cancel();
            }
        }
    }
}
