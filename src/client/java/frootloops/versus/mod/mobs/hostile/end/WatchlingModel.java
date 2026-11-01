package frootloops.versus.mod.mobs.hostile.end;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;

/**
 * The watchling's model: the enderman's, with a taller head (12 where the enderman's is 8, as in Minecraft Dungeons).
 * Its head box is 8 x 12 x 8 from texel (0, 0), so the top and bottom take rows 0 to 8 and the four sides rows 8 to 20
 * (tools/watchling-head.py draws the texture so); the jaw layer under the enderman's head, never seen on one that
 * doesn't open its mouth, is left out.
 */
@Environment(EnvType.CLIENT)
public class WatchlingModel extends EndermanModel<EndermanRenderState> {
    /** How tall the head is, in model units. */
    private static final float HEAD_HEIGHT = 12.0F;

    public WatchlingModel(ModelPart root) {
        super(root);
    }

    /** The enderman's layer with its head swapped for the watchling's; baked by the renderer (no layer registration needed). */
    public static LayerDefinition createBodyLayer() {
        return EndermanModel.createBodyLayer().apply(mesh -> {
            // Where the enderman's head sits: its neck, at the top of the body
            PartPose neck = PartPose.offset(0.0F, -13.0F, 0.0F);
            mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -HEAD_HEIGHT, -4.0F, 8.0F, HEAD_HEIGHT, 8.0F), neck);
            mesh.getRoot().addOrReplaceChild("hat", CubeListBuilder.create(), neck);
            return mesh;
        });
    }
}
