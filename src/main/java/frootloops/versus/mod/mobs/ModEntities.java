package frootloops.versus.mod.mobs;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.mobs.hostile.nether.WildfireEntity;
import frootloops.versus.mod.mobs.hostile.overworld.PaleCreeperEntity;
import frootloops.versus.mod.mobs.hostile.overworld.FrostedZombieEntity;
import frootloops.versus.mod.mobs.hostile.overworld.PaleCreeperEntity;
import frootloops.versus.mod.mobs.hostile.overworld.PaleSpiderEntity;
import frootloops.versus.mod.mobs.hostile.overworld.PaleZombieEntity;
import frootloops.versus.mod.items_and_effects.throwing.SlimeballEntity;


import frootloops.versus.mod.mobs.hostile.overworld.WitheredZombieEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import static frootloops.versus.mod.items_and_effects.RegisteringCustomItems.getItemSettings;

public class ModEntities {

    public static final EntityType<SlimeballEntity> SLIMEBALL = register(
            "slimeball",
            EntityType.Builder.of((EntityType.EntityFactory<SlimeballEntity>)SlimeballEntity::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10)
    );

    public static final EntityType<PaleCreeperEntity> PALE_CREEPER = register(
           "pale_creeper",
            EntityType.Builder.of(PaleCreeperEntity::new, MobCategory.MONSTER).sized(0.6f, 1.7f).clientTrackingRange(6)
    );

    public static final EntityType<FrostedZombieEntity> FROSTED_ZOMBIE = register(
            "frosted_zombie",
            EntityType.Builder.of(FrostedZombieEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).eyeHeight(1.74f).passengerAttachments(2.0125f).ridingOffset(-0.7f).clientTrackingRange(8)
    );

    public static final EntityType<WitheredZombieEntity> WITHERED_ZOMBIE = register(
            "withered_zombie",
            EntityType.Builder.of(WitheredZombieEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).eyeHeight(1.74f).passengerAttachments(2.0125f).ridingOffset(-0.7f).clientTrackingRange(8)
    );

    public static final EntityType<PaleCreeperEntity> PALE_CREEPER = register(
            "pale_creeper",
            EntityType.Builder.of(PaleCreeperEntity::new, MobCategory.MONSTER).sized(0.6f, 1.7f).clientTrackingRange(6)
    );

    public static final EntityType<PaleZombieEntity> PALE_ZOMBIE = register(
            "pale_zombie",
            EntityType.Builder.of(PaleZombieEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).eyeHeight(1.74f).passengerAttachments(2.0125f).ridingOffset(-0.7f).clientTrackingRange(8)
    );

    public static final EntityType<PaleSpiderEntity> PALE_SPIDER = register(
            "pale_spider",
            EntityType.Builder.of(PaleSpiderEntity::new, MobCategory.MONSTER).sized(1.4f, 0.9f).eyeHeight(0.65f).passengerAttachments(0.765f).clientTrackingRange(8)
    );

    public static final EntityType<WildfireEntity> WILDFIRE = register(
            "wildfire", EntityType.Builder.of(WildfireEntity::new, MobCategory.MONSTER).fireImmune().sized(0.6F, 1.8F).clientTrackingRange(8)
    );

    public static final Item PALE_CREEPER_SPAWN_EGG =  new SpawnEggItem(getItemSettings("pale_creeper_spawn_egg").spawnEgg(PALE_CREEPER));
    public static final Item FROSTED_ZOMBIE_SPAWN_EGG =  new SpawnEggItem(getItemSettings("frosted_zombie_spawn_egg").spawnEgg(FROSTED_ZOMBIE));
    public static final Item WITHERED_ZOMBIE_SPAWN_EGG =  new SpawnEggItem(getItemSettings("withered_zombie_spawn_egg").spawnEgg(WITHERED_ZOMBIE));
    public static final Item PALE_CREEPER_SPAWN_EGG =  new SpawnEggItem(getItemSettings("pale_creeper_spawn_egg").spawnEgg(PALE_CREEPER));
    public static final Item PALE_ZOMBIE_SPAWN_EGG =  new SpawnEggItem(getItemSettings("pale_zombie_spawn_egg").spawnEgg(PALE_ZOMBIE));
    public static final Item PALE_SPIDER_SPAWN_EGG =  new SpawnEggItem(getItemSettings("pale_spider_spawn_egg").spawnEgg(PALE_SPIDER));
    public static final Item WILDFIRE_SPAWN_EGG =  new SpawnEggItem(getItemSettings("wildfire_spawn_egg").spawnEgg(WILDFIRE));


    public static void onInitialize() {

        // Register custom entities:
        FabricDefaultAttributeRegistry.register(PALE_CREEPER, PaleCreeperEntity.createPaleCreeperAttributes());
        FabricDefaultAttributeRegistry.register(FROSTED_ZOMBIE, FrostedZombieEntity.createFrostedAttributes());
        FabricDefaultAttributeRegistry.register(WITHERED_ZOMBIE, WitheredZombieEntity.createWitheredAttributes());
        FabricDefaultAttributeRegistry.register(PALE_CREEPER, PaleCreeperEntity.createPaleCreeperAttributes());
        FabricDefaultAttributeRegistry.register(PALE_ZOMBIE, WitheredZombieEntity.createWitheredAttributes());
        FabricDefaultAttributeRegistry.register(PALE_SPIDER, Spider.createAttributes());
        FabricDefaultAttributeRegistry.register(WILDFIRE, WildfireEntity.createWildfireAttributes());

        // Make them spawn in the world:
        MobSpawning.addCustomSpawns();
    }


    private static <T extends Entity> EntityType<T> register(ResourceKey<EntityType<?>> key, EntityType.Builder<T> type) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type.build(key));
    }

    private static ResourceKey<EntityType<?>> keyOf(String id) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, id));
    }

    private static <T extends Entity> EntityType<T> register(String id, EntityType.Builder<T> type) {
        return register(keyOf(id), type);
    }

    public static Identifier getId(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }

}
