package frootloops.versus.mod.mobs;

import frootloops.versus.mod.mobs.hostile.nether.WildfireEntityRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.IceCubeRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.PaleCreeperRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.PaleSpiderRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.PaleZombieRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.core.registries.BuiltInClientRegistries;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
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
        register(PALE_CREEPER, context -> new PaleCreeperRenderer(context));
        register(PALE_SPIDER, context -> new PaleSpiderRenderer(context));
        register(WILDFIRE, context -> new WildfireEntityRenderer(context));
    }

    /** Entity renderers are a client registry since 26.4-snapshot-4, keyed as their entity types are. */
    private static <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
        Registry.register(BuiltInClientRegistries.ENTITY_RENDERER, EntityType.getKey(type), provider);
    }
}
