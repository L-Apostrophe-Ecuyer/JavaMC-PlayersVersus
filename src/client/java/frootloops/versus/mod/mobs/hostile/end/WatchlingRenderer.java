package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * A watchling: an enderman's body with a taller head and big arms (WatchlingModel), the head bearing three eyes that
 * glow a creaking's yellow and orange rather than purple. It carries nothing and never opens its mouth, so it has no more than the body and eyes.
 */
@Environment(EnvType.CLIENT)
public class WatchlingRenderer extends MobRenderer<WatchlingEntity, EndermanRenderState, WatchlingModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/watchling.png");
    private static final RenderType EYES = RenderTypes.eyes(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/watchling_eyes.png"));

    public WatchlingRenderer(EntityRendererProvider.Context context) {
        super(context, new WatchlingModel(WatchlingModel.createBodyLayer().bakeRoot()), 0.5F);
        this.addLayer(new Eyes(this));
    }

    @Override
    public EndermanRenderState createRenderState() {
        return new EndermanRenderState();
    }

    @Override
    public Identifier getTextureLocation(EndermanRenderState state) {
        return TEXTURE;
    }

    static class Eyes extends EyesLayer<EndermanRenderState, WatchlingModel> {
        Eyes(RenderLayerParent<EndermanRenderState, WatchlingModel> renderer) {
            super(renderer);
        }

        @Override
        public RenderType renderType() {
            return EYES;
        }
    }
}
