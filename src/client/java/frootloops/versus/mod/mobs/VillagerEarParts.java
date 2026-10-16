package frootloops.versus.mod.mobs;

import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class VillagerEarParts {

    private VillagerEarParts() {
    }

    public static void addTo(MeshDefinition modelData) {
        PartDefinition head = modelData.getRoot().getChild(PartNames.HEAD);
        head.addOrReplaceChild(PartNames.LEFT_EAR, CubeListBuilder.create().texOffs(56, 0)
                .addBox(0.0f, 0.0f, -2.0f, 1.0f, 4.0f, 3.0f, CubeDeformation.NONE),
                PartPose.offsetAndRotation(3.9f, -6.0f, 0.0f, 0.0f, 0.0f, -0.5235988f));
        head.addOrReplaceChild(PartNames.RIGHT_EAR, CubeListBuilder.create().texOffs(56, 0)
                .addBox(-1.0f, 0.0f, -2.0f, 1.0f, 4.0f, 3.0f, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-3.9f, -6.0f, 0.0f, 0.0f, 0.0f, 0.5235988f));
    }
}
