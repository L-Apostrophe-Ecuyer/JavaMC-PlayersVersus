package frootloops.versus.mod.environment;

import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * The time of day, moon phase and dimension traits the mod read from the level before 26.1, from what 26.3 has
 * instead: world clocks and environment attributes.
 */
public final class WorldTime {

    private WorldTime() {
    }

    /** The overworld clock's ticks, as {@code getDayTime} gave them: not wrapped at a day. 0 without a level. */
    public static long dayTime(LevelAccessor world) {
        if (world instanceof Level level) return level.getOverworldClockTime();
        if (world instanceof ServerLevelAccessor accessor) return accessor.getLevel().getOverworldClockTime();
        return 0L;
    }

    /** The dimension's moon phase, 0 (full moon) to 7, as {@code getMoonPhase} gave it. */
    public static int moonPhase(LevelReader world) {
        return world.environmentAttributes().getDimensionValue(EnvironmentAttributes.MOON_PHASE).index();
    }

    /**
     * The sun's angle as {@code getTimeOfDay} gave it for a day time: a fraction of a turn, 0 at noon and 0.5 at
     * midnight (1.21.10's {@code DimensionType#timeOfDay}).
     */
    public static float timeOfDay(long dayTime) {
        double d = Mth.frac(dayTime / 24000.0 - 0.25);
        double e = 0.5 - Math.cos(d * Math.PI) / 2.0;
        return (float) (d * 2.0 + e) / 3.0F;
    }

    /** What {@code DimensionType#ultraWarm} said (the nether): 26.x splits it into attributes, water evaporating among them. */
    public static boolean ultraWarm(LevelReader world) {
        return world.environmentAttributes().getDimensionValue(EnvironmentAttributes.WATER_EVAPORATES);
    }

    /** What {@code DimensionType#hasRaids} said. */
    public static boolean hasRaids(LevelReader world) {
        return world.environmentAttributes().getDimensionValue(EnvironmentAttributes.CAN_START_RAID);
    }
}
