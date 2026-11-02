package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusSettings;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Now and then an enderman carries a shulker in its arms instead of a block (SHULKER_CARRIER_CHANCE of those that spawn
 * naturally or with the world). The shulker rides it, but held before it rather than on its head: the enderman walks
 * and teleports with it (EndermanMixin keeps the shulker from steering it, and widens it so the shulker is always inside
 * its box: wherever the enderman fits, the shulker isn't in a wall), and carries no block while it has it. The shulker
 * can't teleport out of its arms (ShulkerMixin) and shoots on its own, its bullets passing through the one carrying it
 * (ShulkerBulletMixin). Killed, the enderman drops it.
 */
public final class EndermanShulkerCarrier {
    private EndermanShulkerCarrier() {}

    /** Whether one in this many spawned endermen carries a shulker: the roll, 0 to 1. */
    public static boolean rollsCarrier(float roll) {
        return roll < VersusSettings.Endermen.SHULKER_CARRIER_CHANCE;
    }

    /** Whether an enderman so spawned may be one that carries a shulker: spawned naturally or with the world. */
    public static boolean mayCarry(EntitySpawnReason reason) {
        return reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
    }

    /** The shulker the entity carries, if it's an enderman carrying one. */
    @Nullable
    public static Shulker carried(Entity entity) {
        return entity instanceof Enderman && entity.getFirstPassenger() instanceof Shulker shulker ? shulker : null;
    }

    public static boolean carries(Entity entity) {
        return carried(entity) != null;
    }

    /** Whether the entity is a shulker an enderman carries. */
    public static boolean isCarried(Entity entity) {
        return entity instanceof Shulker && entity.getVehicle() instanceof Enderman;
    }

    /** Whether the entity is what carries the shooter (a projectile's owner): its own shulker's bullets pass through it. */
    public static boolean carriesShooter(Entity entity, @Nullable Entity shooter) {
        return shooter != null && isCarried(shooter) && shooter.getVehicle() == entity;
    }

    /** As an enderman spawns: now and then, a shulker for its arms. */
    public static void maybeGiveShulker(ServerLevelAccessor level, Enderman enderman, DifficultyInstance difficulty, EntitySpawnReason reason, RandomSource random) {
        if (enderman.getType() != EntityTypes.ENDERMAN || !mayCarry(reason) || !rollsCarrier(random.nextFloat()) || enderman.isVehicle()) return;
        Shulker shulker = EntityTypes.SHULKER.create(level.getLevel(), EntitySpawnReason.JOCKEY);
        if (shulker == null) return;
        shulker.snapTo(enderman.getX(), enderman.getY(), enderman.getZ(), enderman.getYRot(), 0.0F);
        shulker.finalizeSpawn(level, difficulty, EntitySpawnReason.JOCKEY, null);
        shulker.startRiding(enderman, true, false);
    }
}
