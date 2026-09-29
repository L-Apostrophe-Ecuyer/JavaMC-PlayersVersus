package frootloops.versus.mod.mobs.hostile.nether;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.BlazeModel;
import net.minecraft.client.model.geom.ModelPart;

@Environment(EnvType.CLIENT)
public class WildfireEntityModel extends BlazeModel {

    public WildfireEntityModel(ModelPart modelPart) {
        super(modelPart);
    }
}

