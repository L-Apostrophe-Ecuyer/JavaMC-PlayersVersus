package frootloops.versus.mod.mobs;

import frootloops.versus.mod.mobs.hostile.end.WatchlingRenderer;
import frootloops.versus.mod.mobs.hostile.nether.WildfireEntityRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.IceCubeRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.MushroomCreeperRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.MushroomSpiderRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.PaleCreeperRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.PaleSpiderRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.PaleVillagerRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.PaleWanderingTraderRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.PaleWolfRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.PaleZombieRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.PlayerZombieRenderer;
import frootloops.versus.mod.mobs.passive.ZombieChickenRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.core.registries.BuiltInClientRegistries;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import static frootloops.versus.mod.mobs.ModEntities.*;

@Environment(EnvType.CLIENT)
public class ModEntitiesRenderers {

    public static void onInitialize() {

        // Register custom item or block entities and their renderers:
        register(SLIMEBALL, context -> new ThrownItemRenderer(context, 1.0f, false));
        register(ICE_CUBE, context -> new IceCubeRenderer(context));
        register(PALE_ZOMBIE, context -> new PaleZombieRenderer(context));
        register(PLAYER_ZOMBIE, context -> new PlayerZombieRenderer(context));
        register(PALE_CREEPER, context -> new PaleCreeperRenderer(context));
        register(PALE_SPIDER, context -> new PaleSpiderRenderer(context));
        register(MUSHROOM_CREEPER, context -> new MushroomCreeperRenderer(context));
        register(MUSHROOM_SPIDER, context -> new MushroomSpiderRenderer(context));
        register(WILDFIRE, context -> new WildfireEntityRenderer(context));
        register(WATCHLING, context -> new WatchlingRenderer(context));
        register(ZOMBIE_CHICKEN, context -> new ZombieChickenRenderer(context));
        // The zombie wolf is a wolf in its own skin (ZombieWolfEntity#getTexture): the wolf's renderer draws it
        register(ZOMBIE_WOLF, context -> new WolfRenderer(context));
        register(PALE_WOLF, context -> new PaleWolfRenderer(context));
        register(PALE_VILLAGER, context -> new PaleVillagerRenderer(context));
        register(PALE_WANDERING_TRADER, context -> new PaleWanderingTraderRenderer(context));
    }

    /** Entity renderers are a client registry since 26.4-snapshot-4, keyed as their entity types are. */
    private static <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
        Registry.register(BuiltInClientRegistries.ENTITY_RENDERER, EntityType.getKey(type), provider);
    }
}
