package frootloops.versus.mod.mobs;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Places a mob could stand, for teleports: sturdy ground under it, room for its standing body, no fluid. */
public final class StandingSpots {

    private StandingSpots() {
    }

    /** A place the mob could stand in the column at (x, z), searching down from {@code top} to {@code bottom}; null if none. */
    @Nullable
    public static Vec3 find(Level level, Mob mob, double x, double z, int top, int bottom) {
        BlockPos.MutableBlockPos ground = new BlockPos.MutableBlockPos();
        int blockX = Mth.floor(x), blockZ = Mth.floor(z);
        for (int y = top; y >= bottom; y--) {
            ground.set(blockX, y - 1, blockZ);
            if (!level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) continue;
            Vec3 spot = new Vec3(blockX + 0.5, y, blockZ + 0.5);
            // The type's size, not the current one: a digging warden is shorter than it stands.
            AABB box = mob.getType().getDimensions().scale(mob.getScale()).makeBoundingBox(spot);
            if (level.noCollision(mob, box) && !level.containsAnyLiquid(box)) return spot;
        }
        return null;
    }

    /**
     * A place around {@code center}, {@code near} to {@code far} blocks away across and from {@code down} blocks below
     * it to {@code up} above, that passes {@code test}; null if {@code attempts} random tries find none.
     */
    @Nullable
    public static Vec3 around(Level level, Mob mob, Vec3 center, double near, double far, int up, int down, int attempts, Predicate<Vec3> test) {
        RandomSource random = mob.getRandom();
        int y = Mth.floor(center.y);
        for (int attempt = 0; attempt < attempts; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = near + random.nextDouble() * (far - near);
            Vec3 spot = find(level, mob, center.x + Math.cos(angle) * distance, center.z + Math.sin(angle) * distance, y + up, y - down);
            if (spot != null && test.test(spot)) return spot;
        }
        return null;
    }
}
