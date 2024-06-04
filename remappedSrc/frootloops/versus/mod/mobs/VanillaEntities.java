package frootloops.versus.mod.mobs;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class VanillaEntities {

    public static final EntityType<CreeperEntity> CREEPER = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of("minecraft", "creeper"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, CreeperEntity::new).dimensions(EntityDimensions.fixed(0.6f, 1.7f)).build()
    );

    public static final EntityType<WitherSkeletonEntity> WITHER_SKELETON = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of("minecraft", "wither_skeleton"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, WitherSkeletonEntity::new).fireImmune().dimensions(EntityDimensions.fixed(0.7f, 2.4f)).build()
    );

}
