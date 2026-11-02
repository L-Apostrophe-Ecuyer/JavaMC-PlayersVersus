package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** A pale wolf: a wolf in its own skin (PaleWolfEntity#getTexture), whose eyes glow the creaking's orange, as the pale spider's. */
@Environment(EnvType.CLIENT)
public class PaleWolfRenderer extends WolfRenderer {
    private static final RenderType EYES = RenderTypes.eyes(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/pale_wolf_eyes.png"));

    public PaleWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new Eyes(this));
    }

    static class Eyes extends EyesLayer<WolfRenderState, WolfModel> {
        Eyes(RenderLayerParent<WolfRenderState, WolfModel> renderer) {
            super(renderer);
        }

        @Override
        public RenderType renderType() {
            return EYES;
        }
    }
}
