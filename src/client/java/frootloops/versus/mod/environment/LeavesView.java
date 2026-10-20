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
 * With the camera inside a leaf block, leaves frame the view, as frost does in powder snow: a texture of our own
 * ({@code textures/misc/leaves_outline.png}, drawn by tools/leaves-outline.py) stretched over the screen, thick in the
 * corners and along the edges, clear in the middle, in the colour of the leaves the camera is in: their tint there, or
 * for leaves with none (cherry, azalea, pale oak) the average colour of their texture. It eases in and out over a few
 * ticks and isn't drawn for spectators. A HUD element after vanilla's camera overlays (a carved pumpkin, powder snow).
 */
@Environment(EnvType.CLIENT)
public final class LeavesView implements HudElement {

    private static final Identifier OUTLINE = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/misc/leaves_outline.png");
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
        graphics.blit(RenderPipelines.GUI_TEXTURED, OUTLINE, 0, 0, 0.0f, 0.0f, width, height, width, height, ARGB.color(ALPHA * this.strength, colour));
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
