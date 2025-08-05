package frootloops.versus.mod.mobs.hostile.nether;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BlazeEntityModel;

@Environment(EnvType.CLIENT)
public class WildfireEntityModel extends BlazeEntityModel {

    public WildfireEntityModel(ModelPart modelPart) {
        super(modelPart);
    }
}

