package frootloops.versus.mod.environment.blocks;

import java.util.function.IntUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/**
 * When creaking hearts are active (awake, spawning their creaking and keeping it): at night, as vanilla's
 * {@code creaking_active} attribute says, or at any time in a cave.
 */
public final class CreakingHearts {

    /** How far above a heart to look for sky light: any there and the heart isn't in a cave. */
    private static final int[] CAVE_CHECK_HEIGHTS = {4, 8, 12};

    private CreakingHearts() {
    }

    /**
     * {@code value}, vanilla's reading of {@code attribute} at a heart, made true for {@code creaking_active} where the
     * heart is in a cave.
     */
    public static Object activeInCaves(Object value, EnvironmentAttribute<?> attribute, Level level, BlockPos pos) {
        return attribute == EnvironmentAttributes.CREAKING_ACTIVE && Boolean.FALSE.equals(value) && inCave(level, pos) ? Boolean.TRUE : value;
    }

    /** In a cave: no sky light 4, 8 or 12 blocks above, in a dimension that has sky light (not the nether or the end). */
    public static boolean inCave(Level level, BlockPos pos) {
        return level.dimensionType().hasSkyLight() && noSkyLightAbove(up -> level.getBrightness(LightLayer.SKY, pos.above(up)));
    }

    /** Whether the sky light {@code up} blocks above, as {@code skyLightAbove} gives it, is 0 at each height checked. */
    static boolean noSkyLightAbove(IntUnaryOperator skyLightAbove) {
        for (int up : CAVE_CHECK_HEIGHTS) {
            if (skyLightAbove.applyAsInt(up) > 0) return false;
        }
        return true;
    }
}
