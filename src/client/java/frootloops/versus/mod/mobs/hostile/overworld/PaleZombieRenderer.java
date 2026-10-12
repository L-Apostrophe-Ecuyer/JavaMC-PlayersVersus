package frootloops.versus.mod.mobs.hostile.overworld;

import com.mojang.blaze3d.vertex.PoseStack;
import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.DrownedModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

/**
 * The pale zombie, built like the drowned: a black skeleton underneath (the skeleton's own model, its thin bones posed
 * like a zombie's), and over it a layer of pale flesh and rags the size of a zombie, torn where the bones show through.
 */
@Environment(EnvType.CLIENT)
public class PaleZombieRenderer extends AbstractZombieRenderer<PaleZombieEntity, ZombieRenderState, DrownedModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/pale_zombie.png");
    private static final Identifier OUTER_LAYER_TEXTURE = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/pale_zombie_outer_layer.png");

    /** Pale zombies are never babies, so the baby's models are the adult's too. */
    public PaleZombieRenderer(EntityRendererProvider.Context context) {
        super(context,
                new DrownedModel(context.bakeLayer(ModelLayers.SKELETON)),
                new DrownedModel(context.bakeLayer(ModelLayers.SKELETON)),
                ArmorModelSet.bake(ModelLayers.DROWNED_ARMOR, context.getModelSet(), DrownedModel::new),
                ArmorModelSet.bake(ModelLayers.DROWNED_ARMOR, context.getModelSet(), DrownedModel::new));
        this.addLayer(new OuterLayer(this, context.getModelSet()));
    }

    @Override
    public ZombieRenderState createRenderState() {
        return new ZombieRenderState();
    }

    @Override
    public Identifier getTextureLocation(ZombieRenderState state) {
        return TEXTURE;
    }

    /** The flesh over the bones, on the drowned's outer-layer model. */
    static class OuterLayer extends RenderLayer<ZombieRenderState, DrownedModel> {
        private final DrownedModel model;

        OuterLayer(RenderLayerParent<ZombieRenderState, DrownedModel> renderer, EntityModelSet models) {
            super(renderer);
            this.model = new DrownedModel(models.bakeLayer(ModelLayers.DROWNED_OUTER_LAYER));
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, ZombieRenderState state, float yRot, float xRot) {
            coloredCutoutModelCopyLayerRender(this.model, OUTER_LAYER_TEXTURE, poseStack, collector, light, state, -1, 1);
        }
    }
}
