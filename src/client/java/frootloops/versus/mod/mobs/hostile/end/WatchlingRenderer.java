package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * A watchling: an enderman's body with a taller head, a jaw and big arms (WatchlingModel), the head bearing three eyes
 * that glow a creaking's yellow and orange rather than purple. It carries nothing, so it has no more than the body and
 * eyes. Its state carries a humanoid's swing (as the enderman's renderer fills it) and how its maw and head move.
 */
@Environment(EnvType.CLIENT)
public class WatchlingRenderer extends MobRenderer<WatchlingEntity, WatchlingRenderState, WatchlingModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/watchling.png");
    private static final RenderType EYES = RenderTypes.eyes(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/watchling_eyes.png"));

    public WatchlingRenderer(EntityRendererProvider.Context context) {
        super(context, new WatchlingModel(WatchlingModel.createBodyLayer().bakeRoot()), 0.5F);
        this.addLayer(new Eyes(this));
    }

    @Override
    public WatchlingRenderState createRenderState() {
        return new WatchlingRenderState();
    }

    @Override
    public void extractRenderState(WatchlingEntity watchling, WatchlingRenderState state, float partialTick) {
        super.extractRenderState(watchling, state, partialTick);
        // Its swing, arm and held items, as a humanoid's: without them it never swings
        HumanoidMobRenderer.extractHumanoidRenderState(watchling, state, partialTick, this.itemModelResolver);
        state.isCreepy = watchling.isCreepy();
        state.jawOpen = watchling.jawOpen(partialTick);
        state.headTilt = watchling.headTilt(partialTick);
    }

    @Override
    public Identifier getTextureLocation(WatchlingRenderState state) {
        return TEXTURE;
    }

    static class Eyes extends EyesLayer<WatchlingRenderState, WatchlingModel> {
        Eyes(RenderLayerParent<WatchlingRenderState, WatchlingModel> renderer) {
            super(renderer);
        }

        @Override
        public RenderType renderType() {
            return EYES;
        }
    }
}
