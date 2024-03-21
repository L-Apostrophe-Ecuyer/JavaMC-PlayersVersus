package frootloops.versus.backported.entities;

import frootloops.versus.backported.entities.wind_charge.WindChargeEntityModel;
import frootloops.versus.backported.entities.wind_charge.WindChargeEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class FutureEntitiesClient {
    public static final EntityModelLayer WIND_CHARGE_MODEL_LAYER = new EntityModelLayer(new Identifier("minecraft", "wind_charge"), "main");
    public static void onInitializeClient() {

        EntityRendererRegistry.register(FutureEntities.WIND_CHARGE_ENTITY, (context) -> {return new WindChargeEntityRenderer(context);});
        EntityModelLayerRegistry.registerModelLayer(WIND_CHARGE_MODEL_LAYER, WindChargeEntityModel::getTexturedModelData);
    }
}