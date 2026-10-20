package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mixin.client.players.LevelRendererAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * With the camera inside a leaf block, that leaf's texture, tinted as it is there, frames the view: tiles a quarter of
 * the screen high, two rings deep along every edge, the outer ring the thicker. It eases in and out over a few ticks and
 * isn't drawn for spectators. A HUD element after vanilla's camera overlays (a carved pumpkin, powder snow).
 */
@Environment(EnvType.CLIENT)
public final class LeavesView implements HudElement {

    /** How far the frame eases toward shown or hidden each tick. */
    private static final float EASE_PER_TICK = 0.25f;
    private static final float OUTER_ALPHA = 0.85f;
    private static final float INNER_ALPHA = 0.45f;

    private float strength;
    private BlockState leaves;
    private BlockPos at = BlockPos.ZERO;

    private LeavesView() {
    }

    public static void register() {
        LeavesView view = new LeavesView();
        ClientTickEvents.END_CLIENT_TICK.register(view::tick);
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "leaves_view"), view);
    }

    private void tick(Minecraft minecraft) {
        BlockState inside = null;
        if (VersusSettings.Leaves.DO_LEAVES_VIEW && minecraft.level != null && minecraft.player != null && !minecraft.player.isSpectator()) {
            BlockPos camera = BlockPos.containing(((LevelRendererAccessor) minecraft.levelRenderer).playersVersus$levelRenderState().cameraRenderState.pos);
            BlockState state = minecraft.level.getBlockState(camera);
            if (state.getBlock() instanceof LeavesBlock) {
                inside = state;
                this.at = camera;
            }
        }
        if (inside != null) this.leaves = inside;
        this.strength = Mth.approach(this.strength, inside != null ? 1.0f : 0.0f, EASE_PER_TICK);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (this.strength <= 0.0f || this.leaves == null || minecraft.level == null) return;
        TextureAtlasSprite sprite = minecraft.getModelManager().getBlockStateModelSet().getParticleMaterial(this.leaves).sprite();
        BlockTintSource tint = minecraft.getBlockColors().getTintSource(this.leaves, 0);
        int colour = tint == null ? 0xFFFFFF : tint.colorInWorld(this.leaves, minecraft.level, this.at);
        int width = graphics.guiWidth(), height = graphics.guiHeight(), size = Math.max(1, height / 4);
        ring(graphics, sprite, width, height, size, 0, ARGB.color(OUTER_ALPHA * this.strength, colour));
        ring(graphics, sprite, width, height, size, size, ARGB.color(INNER_ALPHA * this.strength, colour));
    }

    /** One ring of tiles along the edges, this far in from them. */
    private static void ring(GuiGraphicsExtractor graphics, TextureAtlasSprite sprite, int width, int height, int size, int inset, int colour) {
        for (int x = inset; x < width - inset; x += size) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, inset, size, size, colour);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, height - inset - size, size, size, colour);
        }
        for (int y = inset + size; y < height - inset - size; y += size) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, inset, y, size, size, colour);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, width - inset - size, y, size, size, colour);
        }
    }
}
