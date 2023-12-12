package frootloops.versus.mod;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.mobs.hostile.MobSpawning;
import frootloops.versus.mod.mobs.hostile.overworld.DeeperCreeperEntity;
import frootloops.versus.mod.mobs.hostile.overworld.DeeperCreeperRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.FrostedZombieEntity;
import frootloops.versus.mod.mobs.hostile.overworld.FrostedZombieRenderer;
import frootloops.versus.mod.items.throwing.SlimeballEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<SlimeballEntity> SLIMEBALL = Registry.register(
            Registries.ENTITY_TYPE, new Identifier(VersusMod.MOD_ID, "slimeball"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, (EntityType.EntityFactory<SlimeballEntity>)SlimeballEntity::new).dimensions(EntityDimensions.fixed(0.25f, 0.25f)).trackRangeChunks(4).trackedUpdateRate(10).build()
    );

    public static final EntityType<DeeperCreeperEntity> DEEPER_CREEPER = Registry.register(
            Registries.ENTITY_TYPE, new Identifier(VersusMod.MOD_ID, "deeper_creeper"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DeeperCreeperEntity::new).dimensions(EntityDimensions.fixed(1, 2)).build()
    );

    public static final EntityType<FrostedZombieEntity> FROSTED_ZOMBIE = Registry.register(
            Registries.ENTITY_TYPE, new Identifier(VersusMod.MOD_ID, "frosted_zombie"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, FrostedZombieEntity::new).dimensions(EntityDimensions.fixed(1, 2)).build()
    );

    public static void onInitialize() {

        // Register custom item or block entities and their renderers:
        EntityRendererRegistry.register(SLIMEBALL, context -> new FlyingItemEntityRenderer(context, 1.0f, false));

        // Register custom hostile entities and their renderers:
        EntityRendererRegistry.register(DEEPER_CREEPER, context -> new DeeperCreeperRenderer(context));
        FabricDefaultAttributeRegistry.register(DEEPER_CREEPER, DeeperCreeperEntity.createDeeperCreeperAttributes());
        Registry.register(Registries.ITEM, new Identifier(VersusMod.MOD_ID, "deeper_creeper_spawn_egg"), new SpawnEggItem(DEEPER_CREEPER, 0x3B4978, 0x191A1C, new Item.Settings()));

        // Register custom hostile entities and their renderers:
        EntityRendererRegistry.register(FROSTED_ZOMBIE, context -> new FrostedZombieRenderer(context));
        FabricDefaultAttributeRegistry.register(FROSTED_ZOMBIE, FrostedZombieEntity.createFrostedAttributes());
        Registry.register(Registries.ITEM, new Identifier(VersusMod.MOD_ID, "frosted_zombie_spawn_egg"), new SpawnEggItem(FROSTED_ZOMBIE, 0x3B4978, 0x191A1C, new Item.Settings()));

        // Make them spawn in the world:
        MobSpawning.addCustomSpawns();
    }
}
