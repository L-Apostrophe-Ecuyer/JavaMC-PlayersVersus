package frootloops.versus.mixin.client.players;

import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.gui.hud.debug.DebugHudEntryVisibility;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(DebugHud.class)
public abstract class DebugHudMixin {

    @Shadow private final MinecraftClient client;

    @Shadow private boolean renderingChartVisible, renderingAndTickChartsVisible, packetSizeAndPingChartsVisible;

    protected DebugHudMixin(MinecraftClient client) {
        this.client = client;
    }

    @Shadow
    private void drawText(DrawContext context, List<String> text, boolean left) {}


    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void limitDebugWhenInSurvival(DrawContext context, CallbackInfo info) {
        if (this.client.getCameraEntity() != null && this.client.world != null) {
            boolean shouldRestrictDebug = this.client.player.getGameMode() == null || this.client.player.getGameMode().isSurvivalLike();
            if (shouldRestrictDebug) {

                renderingChartVisible = false;
                renderingAndTickChartsVisible = false;
                packetSizeAndPingChartsVisible = false;

                context.createNewRootLayer();
                Profiler profiler = Profilers.get();
                profiler.push("debug");

                final List<String> list = new ArrayList();
                boolean isDebugReduced = this.client.hasReducedDebugInfo();
                boolean isF3Enabled = this.client.debugHudEntryList.isF3Enabled();

                // Show player position if enabled:
                BlockPos blockPos = client.player.getBlockPos();
                if(!isDebugReduced && (isF3Enabled || client.debugHudEntryList.getVisibility(DebugHudEntries.PLAYER_POSITION) != DebugHudEntryVisibility.NEVER)) {
                    list.add(String.format(Locale.ROOT, " %d %d %d ", blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                }

                // Show player FPS if enabled:
                if(client.debugHudEntryList.isEntryVisible(DebugHudEntries.FPS)) {
                    list.add( String.format(Locale.ROOT, " %d FPS ", client.getCurrentFps()));
                }

                // Show light level if enabled:
                if(client.debugHudEntryList.isEntryVisible(DebugHudEntries.LIGHT_LEVELS)) {
                    list.add( String.format(Locale.ROOT, " %d Block Light ", client.world.getLightLevel(LightType.BLOCK, blockPos)));
                    list.add( String.format(Locale.ROOT, " %d Sky Light ", client.world.getLightLevel(LightType.SKY, blockPos)));
                }

                // Show biome if enabled:
                if(client.debugHudEntryList.isEntryVisible(DebugHudEntries.BIOME)) {
                    list.add(" Biome: " + getBiomeAsString(client.world.getBiome(blockPos)) + " ");
                }

                // If F3 is currently enabled, then show how to configure the screen;
                if(isF3Enabled) {
                    list.add("");
                    list.add(" Use F3 + F6 to configure the debug screen ");
                }

                // And that's it!
                this.drawText(context, list, true);
                info.cancel();
            }
        }
    }

    private static String getBiomeAsString(RegistryEntry<Biome> biome) {
        String biomeName = biome.getKeyOrValue().map(key -> key.getValue().toString(), value -> "[unregistered " + value + "]");
        return biomeName.substring(biomeName.indexOf(':') + 1);
    }
}
