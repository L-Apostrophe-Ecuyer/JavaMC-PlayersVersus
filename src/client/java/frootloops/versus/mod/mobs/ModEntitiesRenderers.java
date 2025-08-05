package frootloops.versus.mod.mobs;

import frootloops.versus.mod.mobs.hostile.nether.WildfireEntity;
import frootloops.versus.mod.mobs.hostile.nether.WildfireEntityRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.DeeperCreeperRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.FrostedZombieRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.WitheredZombieRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

import static frootloops.versus.mod.mobs.ModEntities.*;

@Environment(EnvType.CLIENT)
public class ModEntitiesRenderers {

    public static void onInitialize() {

        // Register custom item or block entities and their renderers:
        EntityRendererRegistry.register(SLIMEBALL, context -> new FlyingItemEntityRenderer(context, 1.0f, false));
        EntityRendererRegistry.register(DEEPER_CREEPER, context -> new DeeperCreeperRenderer(context));
        EntityRendererRegistry.register(FROSTED_ZOMBIE, context -> new FrostedZombieRenderer(context));
        EntityRendererRegistry.register(WITHERED_ZOMBIE, context -> new WitheredZombieRenderer(context));
        EntityRendererRegistry.register(WILDFIRE, context -> new WildfireEntityRenderer(context));
    }
}
