package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A watchling's static on the screen of whoever looks at it, as thick as the server says (WatchedPayload): a grey
 * wash and snow that changes every frame, until nothing else shows.
 */
@Environment(EnvType.CLIENT)
public final class WatchlingStatic implements HudElement {
    private static final Identifier SNOW = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/gui/watchling_static.png");
    /** The snow texture's size, and how much of it covers the screen (so each of its pixels is a big grain). */
    private static final int SNOW_SIZE = 128, SAMPLE = 64;
    private static final float EASE_PER_TICK = 0.15F;

    private final RandomSource random = RandomSource.create();
    private float stare, shown;

    private WatchlingStatic() {
    }

    public static void register() {
        WatchlingStatic view = new WatchlingStatic();
        ClientPlayNetworking.registerGlobalReceiver(WatchedPayload.TYPE, (payload, context) -> view.stare = payload.stare());
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if (minecraft.level == null) view.stare = 0.0F;
            view.shown = Mth.approach(view.shown, view.stare, EASE_PER_TICK);
        });
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "watchling_static"), view);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (this.shown <= 0.01F || Minecraft.getInstance().level == null) return;
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        float strength = Math.min(1.0F, this.shown);
        graphics.fill(0, 0, width, height, ARGB.color(0.45F * strength, 0x7A7A7A));
        int u = this.random.nextInt(SNOW_SIZE - SAMPLE), v = this.random.nextInt(SNOW_SIZE - SAMPLE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, SNOW, 0, 0, u, v, width, height, SAMPLE, SAMPLE, SNOW_SIZE, SNOW_SIZE, ARGB.color(strength, 0xFFFFFF));
    }
}
