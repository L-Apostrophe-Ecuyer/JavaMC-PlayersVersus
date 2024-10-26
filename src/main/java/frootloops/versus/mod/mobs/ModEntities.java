package frootloops.versus.mod.mobs;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.mobs.hostile.overworld.DeeperCreeperEntity;
import frootloops.versus.mod.mobs.hostile.overworld.FrostedZombieEntity;
import frootloops.versus.mod.items.throwing.SlimeballEntity;


import frootloops.versus.mod.mobs.hostile.overworld.WitheredZombieEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import static frootloops.versus.mod.items.RegisteringCustomItems.getSettings;

public class ModEntities {

    public static final EntityType<SlimeballEntity> SLIMEBALL = register(
            "slimeball",
            EntityType.Builder.create((EntityType.EntityFactory<SlimeballEntity>)SlimeballEntity::new, SpawnGroup.MISC).dimensions(0.25f, 0.25f).maxTrackingRange(4).trackingTickInterval(10)
    );

    public static final EntityType<DeeperCreeperEntity> DEEPER_CREEPER = register(
           "deeper_creeper",
            EntityType.Builder.create(DeeperCreeperEntity::new, SpawnGroup.MONSTER).dimensions(0.6f, 1.7f).maxTrackingRange(6)
    );

    public static final EntityType<FrostedZombieEntity> FROSTED_ZOMBIE = register(
            "frosted_zombie",
            EntityType.Builder.create(FrostedZombieEntity::new, SpawnGroup.MONSTER).dimensions(0.6f, 1.95f).eyeHeight(1.74f).passengerAttachments(2.0125f).vehicleAttachment(-0.7f).maxTrackingRange(8)
    );

    public static final EntityType<WitheredZombieEntity> WITHERED_ZOMBIE = register(
            "withered_zombie",
            EntityType.Builder.create(WitheredZombieEntity::new, SpawnGroup.MONSTER).dimensions(0.6f, 1.95f).eyeHeight(1.74f).passengerAttachments(2.0125f).vehicleAttachment(-0.7f).maxTrackingRange(8)
    );

    public static final Item DEEPER_CREEPER_SPAWN_EGG =  new SpawnEggItem(DEEPER_CREEPER, 0x4d4b4a, 0, getSettings("deeper_creeper_spawn_egg"));
    public static final Item FROSTED_ZOMBIE_SPAWN_EGG =  new SpawnEggItem(FROSTED_ZOMBIE, 0x46b3b3 , 0x2e3e7d, getSettings("frosted_zombie_spawn_egg"));
    public static final Item WITHERED_ZOMBIE_SPAWN_EGG =  new SpawnEggItem(WITHERED_ZOMBIE, 0x334545, 0x101017, getSettings("withered_zombie_spawn_egg"));


    public static void onInitialize() {

        // Register custom entities:
        FabricDefaultAttributeRegistry.register(DEEPER_CREEPER, DeeperCreeperEntity.createDeeperCreeperAttributes());
        FabricDefaultAttributeRegistry.register(FROSTED_ZOMBIE, FrostedZombieEntity.createFrostedAttributes());
        FabricDefaultAttributeRegistry.register(WITHERED_ZOMBIE, WitheredZombieEntity.createWitheredAttributes());

        // Make them spawn in the world:
        MobSpawning.addCustomSpawns();
    }


    private static <T extends Entity> EntityType<T> register(RegistryKey<EntityType<?>> key, EntityType.Builder<T> type) {
        return Registry.register(Registries.ENTITY_TYPE, key, type.build(key));
    }

    private static RegistryKey<EntityType<?>> keyOf(String id) {
        return RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(VersusMod.MOD_ID, id));
    }

    private static <T extends Entity> EntityType<T> register(String id, EntityType.Builder<T> type) {
        return register(keyOf(id), type);
    }

    public static Identifier getId(EntityType<?> type) {
        return Registries.ENTITY_TYPE.getId(type);
    }

}
