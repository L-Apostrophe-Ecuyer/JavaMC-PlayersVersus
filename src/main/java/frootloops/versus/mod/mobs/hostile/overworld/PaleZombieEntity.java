package frootloops.versus.mod.mobs.hostile.overworld;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/** The decayed of the Pale Garden: the same withered zombie, its pale flesh torn open on a black skull and bones. */
public class PaleZombieEntity extends WitheredZombieEntity {

    public PaleZombieEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }
}
