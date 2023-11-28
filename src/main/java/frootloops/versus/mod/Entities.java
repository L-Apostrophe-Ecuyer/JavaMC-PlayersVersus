package frootloops.versus.mod;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.items.throwing.SlimeballEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class Entities {

    public static final EntityType<SlimeballEntity> SLIMEBALL = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(VersusMod.MOD_ID, "slimeball"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, (EntityType.EntityFactory<SlimeballEntity>)SlimeballEntity::new)
                    .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
                    .trackRangeChunks(4).trackedUpdateRate(10)
                    .build()
            );

    public static void onInitialize() {
        EntityRendererRegistry.register(SLIMEBALL, context -> new FlyingItemEntityRenderer(context, 1.0f, false));
    }
}
