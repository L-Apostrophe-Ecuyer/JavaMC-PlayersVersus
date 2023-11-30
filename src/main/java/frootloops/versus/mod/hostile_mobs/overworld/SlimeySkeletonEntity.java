package frootloops.versus.mod.hostile_mobs.overworld;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.world.World;

public class SlimeySkeletonEntity extends ZombieEntity {
    public SlimeySkeletonEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }
}
