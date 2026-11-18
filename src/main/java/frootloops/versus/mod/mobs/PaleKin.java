package frootloops.versus.mod.mobs;

import frootloops.versus.mod.environment.PaleLine;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;

/**
 * Below the pale line everything is pale: a zombie, creeper or spider about to spawn naturally there (a husk or a zombie
 * villager, a cave spider too) is its pale kin, by the pale kin's own spawn rules (NaturalSpawnerPaleKinMixin). In the
 * overworld only: the pale line is the deepslate's.
 */
public final class PaleKin {

    private PaleKin() {}

    /** Whether a natural spawn there, in that dimension at that height, is its pale kin. */
    public static boolean pale(ResourceKey<Level> dimension, int y) {
        return y < PaleLine.Y && dimension == Level.OVERWORLD;
    }

    /** The kind that spawns naturally in place of {@code type} there: its pale kin below the pale line, itself elsewhere. */
    public static EntityType<?> at(EntityType<?> type, Level level, BlockPos pos) {
        if (!pale(level.dimension(), pos.getY())) return type;
        if (type == EntityTypes.ZOMBIE || type == EntityTypes.HUSK || type == EntityTypes.ZOMBIE_VILLAGER) return ModEntities.PALE_ZOMBIE;
        if (type == EntityTypes.CREEPER) return ModEntities.PALE_CREEPER;
        if (type == EntityTypes.SPIDER || type == EntityTypes.CAVE_SPIDER) return ModEntities.PALE_SPIDER;
        return type;
    }
}
