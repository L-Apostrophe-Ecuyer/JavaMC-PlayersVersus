package frootloops.versus.mod.environment;

import frootloops.versus.VersusSettings;

/**
 * How dark a night is for its moon: the lightmap's sky factor (vanilla's {@code visual/sky_light_factor}, 1 by day and
 * 0.24 by night) less a darkening that grows with how high the moon is and how little of it shows. A full moon's night
 * is as vanilla's; around a new moon's midnight the sky gives next to no light, so only block light and the dimension's
 * faint ambient light are left. Day isn't touched. Pure, for the client's lightmap (LightmapMixin) and for tests.
 *
 * <p>This is 1.21.10's moon darkening (then on {@code getSkyDarken}, which the lightmap scaled by 0.95 and lifted by
 * 0.05 into its sky factor), carried to 26.x's lightmap. The phase moves smoothly from one night to the next on the
 * overworld clock, as the {@code minecraft:moon} timeline turns the moon (full from tick 0, new from 96000, of 192000).
 */
public final class LunarDarkness {
    /** A sky factor this high or more is day (1.21.10's sky darkening of 0.99, as a sky factor): left alone. */
    static final float DAY = 0.99F * 0.95F + 0.05F;
    /** The lightmap's sky factor is 0.95 of 1.21.10's sky darkening (plus 0.05). */
    private static final float SKY_SCALE = 0.95F;
    private static final long DAY_TICKS = 24000L, CYCLE_TICKS = 8L * DAY_TICKS;

    private LunarDarkness() {
    }

    /**
     * How little of the moon shows, smoothly: -0.1 on the full moon's night (a touch lighter than vanilla) up to 3.9 on
     * the new moon's.
     */
    public static float moonlessness(long dayTime) {
        float days = (float) Math.floorMod(dayTime + 6000L + 3L * DAY_TICKS, CYCLE_TICKS) / DAY_TICKS;
        return Math.abs(4.0F - days) - 0.1F;
    }

    /** How high the moon is, 0 to 0.8: highest a little after midnight, nothing at dusk and dawn. */
    public static float moonHeight(long dayTime) {
        float fromMidnight = Math.abs((float) (Math.floorMod(dayTime - 750L, 12000L) - 6000L) / 12000.0F);
        return 0.3F / (0.25F + fromMidnight) - 0.4F;
    }

    /** How much of 1.21.10's sky darkening the moon takes away at this time (the overworld clock's ticks). */
    public static float darkening(long dayTime) {
        float moonless = moonlessness(dayTime);
        return moonHeight(dayTime) * (moonless / 24.0F + moonless * moonless / 96.0F) * VersusSettings.Moon.DARKENING;
    }

    /** The sky factor under this moon at this time. Never below nothing. */
    public static float skyFactor(float skyFactor, long dayTime) {
        if (skyFactor >= DAY) return skyFactor;
        return Math.max(0.0F, skyFactor - SKY_SCALE * darkening(dayTime));
    }
}
