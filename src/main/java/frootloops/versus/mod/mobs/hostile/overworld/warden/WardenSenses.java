package frootloops.versus.mod.mobs.hostile.overworld.warden;

import net.minecraft.world.entity.Entity;

/**
 * What a warden can make out. Close up it smells its prey, within {@link #SMELL_RANGE} blocks across and
 * {@link #SMELL_RANGE_VERTICAL} up or down, as far as its sniffing reaches (WardenMixin); further off it only hears them,
 * and not their steps unless they sprint.
 */
public final class WardenSenses {
    public static final double SMELL_RANGE = 6.0, SMELL_RANGE_VERTICAL = 8.0;

    private WardenSenses() {
    }

    public static boolean inSmellRange(Entity warden, Entity entity) {
        double dx = entity.getX() - warden.getX(), dz = entity.getZ() - warden.getZ();
        return dx * dx + dz * dz < SMELL_RANGE * SMELL_RANGE && Math.abs(entity.getY() - warden.getY()) < SMELL_RANGE_VERTICAL;
    }

    /** Whether the warden loses track of someone: they don't sprint and are out of its smell range. */
    public static boolean losesTrackOf(Entity warden, Entity entity) {
        return !entity.isSprinting() && !inSmellRange(warden, entity);
    }
}
