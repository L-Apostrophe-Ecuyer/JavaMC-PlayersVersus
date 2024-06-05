package frootloops.versus.mod.mobs;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.mobs.hostile.overworld.DeeperCreeperEntity;
import frootloops.versus.mod.mobs.hostile.overworld.FrostedZombieEntity;
import frootloops.versus.mod.items.throwing.SlimeballEntity;


import frootloops.versus.mod.mobs.hostile.overworld.WitheredZombieEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {


    public static final EntityType<SlimeballEntity> SLIMEBALL = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of(VersusMod.MOD_ID, "slimeball"),
            EntityType.Builder.create((EntityType.EntityFactory<SlimeballEntity>)SlimeballEntity::new, SpawnGroup.MISC).dimensions(0.25f, 0.25f).maxTrackingRange(4).trackingTickInterval(10).build()
    );

    public static final EntityType<DeeperCreeperEntity> DEEPER_CREEPER = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of(VersusMod.MOD_ID, "deeper_creeper"),
            EntityType.Builder.create(DeeperCreeperEntity::new, SpawnGroup.MONSTER).dimensions(0.6f, 1.7f).maxTrackingRange(6).build()
    );

    public static final EntityType<FrostedZombieEntity> FROSTED_ZOMBIE = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of(VersusMod.MOD_ID, "frosted_zombie"),
            EntityType.Builder.create(FrostedZombieEntity::new, SpawnGroup.MONSTER).dimensions(0.6f, 1.95f).eyeHeight(1.74f).passengerAttachments(2.0125f).vehicleAttachment(-0.7f).maxTrackingRange(8).build()
    );

    public static final EntityType<WitheredZombieEntity> WITHERED_ZOMBIE = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of(VersusMod.MOD_ID, "withered_zombie"),
            EntityType.Builder.create(WitheredZombieEntity::new, SpawnGroup.MONSTER).dimensions(0.6f, 1.95f).eyeHeight(1.74f).passengerAttachments(2.0125f).vehicleAttachment(-0.7f).maxTrackingRange(8).build()
    );

    public static void onInitialize() {

        // Register custom hostile entities and their renderers:
        FabricDefaultAttributeRegistry.register(DEEPER_CREEPER, DeeperCreeperEntity.createDeeperCreeperAttributes());
        Registry.register(Registries.ITEM, Identifier.of(VersusMod.MOD_ID, "deeper_creeper_spawn_egg"), new SpawnEggItem(DEEPER_CREEPER, 0x3B4978, 0x191A1C, new Item.Settings()));

        // Register custom hostile entities and their renderers:
        FabricDefaultAttributeRegistry.register(FROSTED_ZOMBIE, FrostedZombieEntity.createFrostedAttributes());
        Registry.register(Registries.ITEM, Identifier.of(VersusMod.MOD_ID, "frosted_zombie_spawn_egg"), new SpawnEggItem(FROSTED_ZOMBIE, 0x3B4978, 0x191A1C, new Item.Settings()));

        // Register custom hostile entities and their renderers:
        FabricDefaultAttributeRegistry.register(WITHERED_ZOMBIE, WitheredZombieEntity.createWitheredAttributes());
        Registry.register(Registries.ITEM, Identifier.of(VersusMod.MOD_ID, "withered_zombie_spawn_egg"), new SpawnEggItem(WITHERED_ZOMBIE, 0x3B4978, 0x191A1C, new Item.Settings()));

        // Make them spawn in the world:
        MobSpawning.addCustomSpawns();
    }

}
