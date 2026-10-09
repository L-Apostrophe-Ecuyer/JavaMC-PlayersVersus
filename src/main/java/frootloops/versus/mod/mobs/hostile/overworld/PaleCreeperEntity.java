package frootloops.versus.mod.mobs.hostile.overworld;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

/** The stalker of the Pale Garden and the Pale Grotto: the same creeping, frozen while watched, in pale oak instead of stone. */
public class PaleCreeperEntity extends DeeperCreeperEntity {

    public PaleCreeperEntity(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }
}
