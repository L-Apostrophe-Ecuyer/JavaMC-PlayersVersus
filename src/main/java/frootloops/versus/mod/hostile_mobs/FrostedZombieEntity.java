package frootloops.versus.mod.hostile_mobs;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.world.World;

public class FrostedZombieEntity extends ZombieEntity {
    public FrostedZombieEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }
}
