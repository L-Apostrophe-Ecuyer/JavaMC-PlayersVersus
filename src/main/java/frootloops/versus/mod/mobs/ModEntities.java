package frootloops.versus.mod.mobs;

import frootloops.versus.mod.mobs.hostile.end.WatchlingEntity;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.mobs.hostile.nether.WildfireEntity;
import frootloops.versus.mod.mobs.hostile.overworld.PaleCreeperEntity;
import frootloops.versus.mod.mobs.hostile.overworld.IceCubeEntity;
import frootloops.versus.mod.mobs.hostile.overworld.MushroomCreeperEntity;
import frootloops.versus.mod.mobs.hostile.overworld.MushroomSpiderEntity;
import frootloops.versus.mod.mobs.hostile.overworld.PaleSpiderEntity;
import frootloops.versus.mod.mobs.hostile.overworld.PaleVillagerEntity;
import frootloops.versus.mod.mobs.hostile.overworld.PaleWanderingTraderEntity;
import frootloops.versus.mod.mobs.hostile.overworld.PaleZombieEntity;
import frootloops.versus.mod.mobs.passive.ZombieChickenEntity;
import frootloops.versus.mod.items_and_effects.throwing.SlimeballEntity;


import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.Vec3;

import static frootloops.versus.mod.items_and_effects.RegisteringCustomItems.getItemSettings;

public class ModEntities {

    public static final EntityType<SlimeballEntity> SLIMEBALL = register(
            "slimeball",
            EntityType.Builder.of((EntityType.EntityFactory<SlimeballEntity>)SlimeballEntity::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10)
    );


    /** A blue slime of the ice caves whose touch freezes; sized and tracked like vanilla's slime. */
    public static final EntityType<IceCubeEntity> ICE_CUBE = register(
            "ice_cube",
            EntityType.Builder.of(IceCubeEntity::new, MobCategory.MONSTER).sized(0.52f, 0.52f).eyeHeight(0.325f).spawnDimensionsScale(4.0f).clientTrackingRange(10)
    );

    public static final EntityType<PaleZombieEntity> PALE_ZOMBIE = register(
            "pale_zombie",
            EntityType.Builder.of(PaleZombieEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).eyeHeight(1.74f).passengerAttachments(2.0125f).ridingOffset(-0.7f).clientTrackingRange(8)
    );

    public static final EntityType<PaleCreeperEntity> PALE_CREEPER = register(
            "pale_creeper",
            EntityType.Builder.of(PaleCreeperEntity::new, MobCategory.MONSTER).sized(0.6f, 1.7f).clientTrackingRange(6)
    );

    public static final EntityType<PaleSpiderEntity> PALE_SPIDER = register(
            "pale_spider",
            EntityType.Builder.of(PaleSpiderEntity::new, MobCategory.MONSTER).sized(1.4f, 0.9f).eyeHeight(0.65f).passengerAttachments(0.765f).clientTrackingRange(8)
    );

    /** The mushroom caves' creeper and spider, sized and tracked as vanilla's. */
    public static final EntityType<MushroomCreeperEntity> MUSHROOM_CREEPER = register(
            "mushroom_creeper",
            EntityType.Builder.of(MushroomCreeperEntity::new, MobCategory.MONSTER).sized(0.6f, 1.7f).clientTrackingRange(8)
    );

    public static final EntityType<MushroomSpiderEntity> MUSHROOM_SPIDER = register(
            "mushroom_spider",
            EntityType.Builder.of(MushroomSpiderEntity::new, MobCategory.MONSTER).sized(1.4f, 0.9f).eyeHeight(0.65f).passengerAttachments(0.765f).clientTrackingRange(8)
    );

    /** The watchling: an enderman with a creaking's eyes, sized and tracked as one. */
    public static final EntityType<WatchlingEntity> WATCHLING = register(
            "watchling",
            EntityType.Builder.of(WatchlingEntity::new, MobCategory.MONSTER).sized(0.6f, 2.9f).eyeHeight(2.55f).passengerAttachments(2.80625f).clientTrackingRange(8)
    );

    /** A villager the paleness took (withered), sized and tracked as a zombie villager. */
    public static final EntityType<PaleVillagerEntity> PALE_VILLAGER = register(
            "pale_villager",
            EntityType.Builder.of(PaleVillagerEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).eyeHeight(1.74f).passengerAttachments(2.125f).ridingOffset(-0.7f).clientTrackingRange(8)
    );

    /** A wandering trader the paleness took (withered), sized and tracked as a wandering trader. */
    public static final EntityType<PaleWanderingTraderEntity> PALE_WANDERING_TRADER = register(
            "pale_wandering_trader",
            EntityType.Builder.of(PaleWanderingTraderEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).eyeHeight(1.62f).clientTrackingRange(10)
    );

    /** A chicken Conversion turned undead: a chicken in all but its eggs, sized and tracked as one. */
    public static final EntityType<ZombieChickenEntity> ZOMBIE_CHICKEN = register(
            "zombie_chicken",
            EntityType.Builder.of(ZombieChickenEntity::new, MobCategory.CREATURE).sized(0.4f, 0.7f).eyeHeight(0.644f).passengerAttachments(new Vec3(0.0, 0.7, -0.1)).clientTrackingRange(10)
    );

    public static final EntityType<WildfireEntity> WILDFIRE = register(
            "wildfire", EntityType.Builder.of(WildfireEntity::new, MobCategory.MONSTER).fireImmune().sized(0.6F, 1.8F).clientTrackingRange(8)
    );

    public static final Item ICE_CUBE_SPAWN_EGG =  new SpawnEggItem(getItemSettings("ice_cube_spawn_egg").spawnEgg(ICE_CUBE));
    public static final Item PALE_CREEPER_SPAWN_EGG =  new SpawnEggItem(getItemSettings("pale_creeper_spawn_egg").spawnEgg(PALE_CREEPER));
    public static final Item PALE_ZOMBIE_SPAWN_EGG =  new SpawnEggItem(getItemSettings("pale_zombie_spawn_egg").spawnEgg(PALE_ZOMBIE));
    public static final Item PALE_SPIDER_SPAWN_EGG =  new SpawnEggItem(getItemSettings("pale_spider_spawn_egg").spawnEgg(PALE_SPIDER));
    public static final Item MUSHROOM_CREEPER_SPAWN_EGG =  new SpawnEggItem(getItemSettings("mushroom_creeper_spawn_egg").spawnEgg(MUSHROOM_CREEPER));
    public static final Item MUSHROOM_SPIDER_SPAWN_EGG =  new SpawnEggItem(getItemSettings("mushroom_spider_spawn_egg").spawnEgg(MUSHROOM_SPIDER));
    public static final Item WATCHLING_SPAWN_EGG =  new SpawnEggItem(getItemSettings("watchling_spawn_egg").spawnEgg(WATCHLING));
    public static final Item PALE_VILLAGER_SPAWN_EGG =  new SpawnEggItem(getItemSettings("pale_villager_spawn_egg").spawnEgg(PALE_VILLAGER));
    public static final Item PALE_WANDERING_TRADER_SPAWN_EGG =  new SpawnEggItem(getItemSettings("pale_wandering_trader_spawn_egg").spawnEgg(PALE_WANDERING_TRADER));
    public static final Item ZOMBIE_CHICKEN_SPAWN_EGG =  new SpawnEggItem(getItemSettings("zombie_chicken_spawn_egg").spawnEgg(ZOMBIE_CHICKEN));
    public static final Item WILDFIRE_SPAWN_EGG =  new SpawnEggItem(getItemSettings("wildfire_spawn_egg").spawnEgg(WILDFIRE));


    public static void onInitialize() {

        // Register custom entities:
        FabricDefaultAttributeRegistry.register(ICE_CUBE, IceCubeEntity.createIceCubeAttributes());
        FabricDefaultAttributeRegistry.register(PALE_CREEPER, PaleCreeperEntity.createPaleCreeperAttributes());
        FabricDefaultAttributeRegistry.register(PALE_ZOMBIE, PaleZombieEntity.createPaleZombieAttributes());
        FabricDefaultAttributeRegistry.register(PALE_SPIDER, Spider.createAttributes());
        FabricDefaultAttributeRegistry.register(MUSHROOM_CREEPER, Creeper.createAttributes());
        FabricDefaultAttributeRegistry.register(MUSHROOM_SPIDER, Spider.createAttributes());
        FabricDefaultAttributeRegistry.register(WILDFIRE, WildfireEntity.createWildfireAttributes());
        FabricDefaultAttributeRegistry.register(WATCHLING, WatchlingEntity.createWatchlingAttributes());
        FabricDefaultAttributeRegistry.register(ZOMBIE_CHICKEN, Chicken.createAttributes());
        FabricDefaultAttributeRegistry.register(PALE_VILLAGER, PaleVillagerEntity.createPaleVillagerAttributes());
        FabricDefaultAttributeRegistry.register(PALE_WANDERING_TRADER, PaleVillagerEntity.createPaleVillagerAttributes());

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
