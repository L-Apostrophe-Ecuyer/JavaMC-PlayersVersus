package frootloops.versus.mod.environment;

import com.mojang.blaze3d.platform.NativeImage;
import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mixin.client.players.LevelRendererAccessor;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.WeakHashMap;
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
 * With the camera inside a leaf block, leaves fill the corners of the view, as frost does in powder snow: in each corner
 * of the screen the matching quarter of vanilla's leaf litter texture (the quarters its litter models are made of), so
 * big its pixels show, in the colour of the leaves the camera is in: their tint there, or for leaves with none (cherry,
 * azalea, pale oak) the average colour of their texture. It eases in and out over a few ticks and isn't drawn for
 * spectators. A HUD element after vanilla's camera overlays (a carved pumpkin, powder snow).
 */
@Environment(EnvType.CLIENT)
public final class LeavesView implements HudElement {

    /** Vanilla's leaf litter texture: grey leaves, 16 pixels square, each quarter one of its litter models' patches. */
    private static final Identifier LITTER = Identifier.withDefaultNamespace("textures/block/leaf_litter.png");
    private static final int LITTER_SIZE = 16, QUARTER = 8;
    /** How much of the screen's shorter side each corner's quarter of litter covers. */
    private static final float CORNER = 0.45f;
    /** How far the frame eases toward shown or hidden each tick. */
    private static final float EASE_PER_TICK = 0.25f;
    private static final float ALPHA = 0.95f;

    /** The average colour of each untinted leaf's texture, by its sprite: a resource reload makes new sprites. */
    private final Map<TextureAtlasSprite, Integer> averages = new WeakHashMap<>();

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
        BlockTintSource tint = minecraft.getBlockColors().getTintSource(this.leaves, 0);
        int colour = tint != null ? tint.colorInWorld(this.leaves, minecraft.level, this.at)
                : this.averages.computeIfAbsent(minecraft.getModelManager().getBlockStateModelSet().getParticleMaterial(this.leaves).sprite(), LeavesView::average);
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        // Whole screen pixels to a texture pixel, so every leaf pixel is the same size, square and sharp
        int pixel = Math.max(1, Math.round(Math.min(width, height) * CORNER / QUARTER)), size = pixel * QUARTER;
        int tinted = ARGB.color(ALPHA * this.strength, colour);
        for (int right = 0; right <= 1; right++) {
            for (int bottom = 0; bottom <= 1; bottom++) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, LITTER, right * (width - size), bottom * (height - size), right * QUARTER, bottom * QUARTER,
                        size, size, QUARTER, QUARTER, LITTER_SIZE, LITTER_SIZE, tinted);
            }
        }
    }

    /** The average colour of a sprite's texture, its see-through pixels left out; white when it can't be read. */
    private static int average(TextureAtlasSprite sprite) {
        Identifier name = sprite.contents().name();
        Identifier file = Identifier.fromNamespaceAndPath(name.getNamespace(), "textures/" + name.getPath() + ".png");
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(file); NativeImage image = NativeImage.read(in)) {
            long red = 0, green = 0, blue = 0, count = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getPixel(x, y);
                    if (ARGB.alpha(pixel) == 0) continue;
                    red += ARGB.red(pixel);
                    green += ARGB.green(pixel);
                    blue += ARGB.blue(pixel);
                    count++;
                }
            }
            return count == 0 ? 0xFFFFFF : ARGB.color((int) (red / count), (int) (green / count), (int) (blue / count));
        } catch (IOException e) {
            return 0xFFFFFF;
        }
    }
}
