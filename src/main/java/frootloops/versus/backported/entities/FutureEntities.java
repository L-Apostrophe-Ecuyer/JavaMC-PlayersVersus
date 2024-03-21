package frootloops.versus.backported.entities;

import frootloops.versus.backported.entities.wind_charge.WindChargeEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class FutureEntities {

    public static final EntityType<WindChargeEntity> WIND_CHARGE_ENTITY = Registry.register(
            Registries.ENTITY_TYPE, new Identifier("minecraft", "wind_charge"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, (EntityType.EntityFactory<WindChargeEntity>)WindChargeEntity::new).dimensions(EntityDimensions.fixed(0.25f, 0.25f)).trackRangeChunks(4).trackedUpdateRate(10).build()
    );

    public static void onInitialize() {

    }
}
