package frootloops.versus.mod.mobs.hostile.overworld;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Red mushrooms growing out of one of a model's parts, as the mooshroom's grow out of its back and head
 * (MushroomCowMushroomLayer): the red mushroom's block model, drawn at each spot. The red mushroom's hosts wear them.
 */
@Environment(EnvType.CLIENT)
public class RedMushroomsLayer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {

    /** A render state that carries the red mushroom's block model, resolved each frame by its renderer. */
    public interface Holder {
        BlockModelRenderState redMushroom();
    }

    /** Where a mushroom grows on the part (blocks, the part's own axes, y down), turned about y, and how big. */
    public record Spot(float x, float y, float z, float yRot, float scale) {}

    private final String part;
    private final Spot[] spots;

    public RedMushroomsLayer(RenderLayerParent<S, M> renderer, String part, Spot... spots) {
        super(renderer);
        this.part = part;
        this.spots = spots;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
        if (state.isInvisible || !(state instanceof Holder holder) || holder.redMushroom().isEmpty()) return;
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        for (Spot spot : this.spots) {
            poseStack.pushPose();
            this.getParentModel().root().getChild(this.part).translateAndRotate(poseStack);
            poseStack.translate(spot.x(), spot.y(), spot.z());
            poseStack.rotateDegrees(Axis.YP, spot.yRot());
            // As the mooshroom's: the block's model upside down (the model's y points down), centred on the spot
            poseStack.scale(-spot.scale(), -spot.scale(), spot.scale());
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            holder.redMushroom().submit(poseStack, collector, light, overlay, state.outlineColor);
            poseStack.popPose();
        }
    }
}
