package frootloops.versus.mod.mobs;

import frootloops.versus.mod.mobs.hostile.nether.WildfireEntity;
import frootloops.versus.mod.mobs.hostile.nether.WildfireEntityRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.DeeperCreeperRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.FrostedZombieRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.PaleCreeperRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.PaleSpiderRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.PaleZombieRenderer;
import frootloops.versus.mod.mobs.hostile.overworld.WitheredZombieRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import static frootloops.versus.mod.mobs.ModEntities.*;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

@Environment(EnvType.CLIENT)
public class ModEntitiesRenderers {

    public static void onInitialize() {

        // Register custom item or block entities and their renderers:
        EntityRendererRegistry.register(SLIMEBALL, context -> new ThrownItemRenderer(context, 1.0f, false));
        EntityRendererRegistry.register(DEEPER_CREEPER, context -> new DeeperCreeperRenderer(context));
        EntityRendererRegistry.register(FROSTED_ZOMBIE, context -> new FrostedZombieRenderer(context));
        EntityRendererRegistry.register(WITHERED_ZOMBIE, context -> new WitheredZombieRenderer(context));
        EntityRendererRegistry.register(PALE_CREEPER, context -> new PaleCreeperRenderer(context));
        EntityRendererRegistry.register(PALE_ZOMBIE, context -> new PaleZombieRenderer(context));
        EntityRendererRegistry.register(PALE_SPIDER, context -> new PaleSpiderRenderer(context));
        EntityRendererRegistry.register(WILDFIRE, context -> new WildfireEntityRenderer(context));
    }
}
