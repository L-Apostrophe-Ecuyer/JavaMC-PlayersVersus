package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnderEyesLayer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** A watchling: an enderman whose eyes glow a creaking's yellow and orange rather than purple. */
@Environment(EnvType.CLIENT)
public class WatchlingRenderer extends EndermanRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/watchling.png");
    private static final RenderType EYES = RenderTypes.eyes(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/watchling_eyes.png"));

    public WatchlingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.layers.removeIf(layer -> layer instanceof EnderEyesLayer);
        this.addLayer(new Eyes(this));
    }

    @Override
    public Identifier getTextureLocation(EndermanRenderState state) {
        return TEXTURE;
    }

    static class Eyes extends EyesLayer<EndermanRenderState, EndermanModel<EndermanRenderState>> {
        Eyes(RenderLayerParent<EndermanRenderState, EndermanModel<EndermanRenderState>> renderer) {
            super(renderer);
        }

        @Override
        public RenderType renderType() {
            return EYES;
        }
    }
}
