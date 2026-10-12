package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.monster.spider.SpiderModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.SpiderEyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** A pale spider, whose eyes glow the creaking's orange rather than red. */
@Environment(EnvType.CLIENT)
public class PaleSpiderRenderer extends SpiderRenderer<PaleSpiderEntity> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/pale_spider.png");
    private static final RenderType EYES = RenderTypes.eyes(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/pale_spider_eyes.png"));

    public PaleSpiderRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.layers.removeIf(layer -> layer instanceof SpiderEyesLayer);
        this.addLayer(new Eyes(this));
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    static class Eyes extends EyesLayer<LivingEntityRenderState, SpiderModel> {
        Eyes(RenderLayerParent<LivingEntityRenderState, SpiderModel> renderer) {
            super(renderer);
        }

        @Override
        public RenderType renderType() {
            return EYES;
        }
    }
}
