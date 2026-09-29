package frootloops.versus.mixin.client.players;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugHudMixin {

    @Shadow private final Minecraft minecraft;

    @Shadow private boolean renderProfilerChart, renderFpsCharts, renderNetworkCharts;

    protected DebugHudMixin(Minecraft client) {
        this.minecraft = client;
    }

    @Shadow
    private void renderLines(GuiGraphics context, List<String> text, boolean left) {}


    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void limitDebugWhenInSurvival(GuiGraphics context, CallbackInfo info) {
        if (this.minecraft.getCameraEntity() != null && this.minecraft.level != null) {
            boolean shouldRestrictDebug = this.minecraft.player.gameMode() == null || this.minecraft.player.gameMode().isSurvival();
            if (shouldRestrictDebug) {

                renderProfilerChart = false;
                renderFpsCharts = false;
                renderNetworkCharts = false;

                context.nextStratum();
                ProfilerFiller profiler = Profiler.get();
                profiler.push("debug");

                final List<String> list = new ArrayList();
                boolean isDebugReduced = this.minecraft.showOnlyReducedInfo();
                boolean isF3Enabled = this.minecraft.debugEntries.isF3Visible();

                // Show player position if enabled:
                BlockPos blockPos = minecraft.player.blockPosition();
                if(!isDebugReduced && (isF3Enabled || minecraft.debugEntries.getStatus(DebugScreenEntries.PLAYER_POSITION) != DebugScreenEntryStatus.NEVER)) {
                    list.add(String.format(Locale.ROOT, " %d %d %d ", blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                }

                // Show player FPS if enabled:
                if(minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.FPS)) {
                    list.add( String.format(Locale.ROOT, " %d FPS ", minecraft.getFps()));
                }

                // Show light level if enabled:
                if(minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.LIGHT_LEVELS)) {
                    list.add( String.format(Locale.ROOT, " %d Block Light ", minecraft.level.getBrightness(LightLayer.BLOCK, blockPos)));
                    list.add( String.format(Locale.ROOT, " %d Sky Light ", minecraft.level.getBrightness(LightLayer.SKY, blockPos)));
                }

                // Show biome if enabled:
                if(minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.BIOME)) {
                    MutableComponent text = Component.literal("Biome: ").append("subtitles.players-versus.sword_blocking").append(" ");
                    list.add(" Biome: " + getBiomeName(minecraft.level.getBiome(blockPos)) + " ");
                }

                // If F3 is currently enabled, then show how to configure the screen;
                if(isF3Enabled) {
                    list.add("");
                    list.add(" Use F3 + F6 to configure ");
                    list.add(" the debug screen. ");
                }

                // And that's it!
                this.renderLines(context, list, true);
                info.cancel();
            }
        }
    }

    private static String getBiomeName(Holder<Biome> biome) {
        Optional<ResourceKey<Biome>> biomeKey = biome.unwrapKey();
        if(biomeKey.isEmpty()) return "[Unregistered]";
        return Language.getInstance().getOrDefault(biomeKey.get().location().toLanguageKey("biome"));
    }
}
