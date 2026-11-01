package frootloops.versus.mod.mobs.hostile.end;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;

/**
 * The watchling's model: the enderman's, with a taller head (12 where the enderman's is 8) and big arms (4 across where
 * the enderman's are 2), as in Minecraft Dungeons, where it is a melee mob. The texture is 64 x 64: the head box is
 * 8 x 12 x 8 from texel (0, 0), so the top and bottom take rows 0 to 8 and the four sides rows 8 to 20; each arm is
 * 4 x 30 x 4 from texel (0, 30) (tools/watchling-head.py draws the texture so). The jaw layer under the enderman's head,
 * never seen on one that doesn't open its mouth, is left out.
 */
@Environment(EnvType.CLIENT)
public class WatchlingModel extends EndermanModel<EndermanRenderState> {
    /** How tall the head is, in model units. */
    private static final float HEAD_HEIGHT = 12.0F;

    public WatchlingModel(ModelPart root) {
        super(root);
    }

    /** The enderman's layer with its head and arms swapped for the watchling's; baked by the renderer (no layer registration needed). */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition[] built = new MeshDefinition[1];
        EndermanModel.createBodyLayer().apply(mesh -> {
            // Where the enderman's head sits: its neck, at the top of the body
            PartPose neck = PartPose.offset(0.0F, -13.0F, 0.0F);
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -HEAD_HEIGHT, -4.0F, 8.0F, HEAD_HEIGHT, 8.0F), neck);
            root.addOrReplaceChild("hat", CubeListBuilder.create(), neck);
            // Arms twice as thick, flush against the body's sides (the body is 8 across, the arms 4)
            root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 30).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 30.0F, 4.0F), PartPose.offset(-6.0F, -12.0F, 0.0F));
            root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 30).mirror().addBox(-2.0F, -2.0F, -2.0F, 4.0F, 30.0F, 4.0F), PartPose.offset(6.0F, -12.0F, 0.0F));
            built[0] = mesh;
            return mesh;
        });
        return LayerDefinition.create(built[0], 64, 64);
    }
}
