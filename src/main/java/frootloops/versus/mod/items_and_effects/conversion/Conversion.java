package frootloops.versus.mod.items_and_effects.conversion;

/**
 * Conversion, pale shrooms' effect, by numbers: its hearts turn white, one per amplifier + 1, from the end of the health
 * bar down towards none. Each dose adds its amplifier + 1 to what was had, and refreshes it without shortening it. Food doesn't regenerate health into white
 * hearts; all hearts white, the entity turns undead (Undeath).
 */
public final class Conversion {
    private Conversion() {}

    /** White hearts for an amplifier: one more than it. */
    public static int whiteHearts(int amplifier) {
        return amplifier + 1;
    }

    /** The amplifier after a dose of this amplifier, on one had: the dose's hearts added to those had. */
    public static int stackedAmplifier(int had, int dose) {
        return had + dose + 1;
    }

    /** The duration after a dose on one had: the longer of the two (-1, endless, stays endless). */
    public static int stackedDuration(int had, int dose) {
        return had < 0 ? had : Math.max(had, dose);
    }

    /** The health food regenerates up to: what the white hearts leave of the bar. */
    public static float regenCap(float maxHealth, int whiteHearts) {
        return Math.max(0.0F, maxHealth - 2.0F * whiteHearts);
    }

    /** Hearts in the bar for this health, a half counting as one. */
    public static int hearts(float maxHealth) {
        return (int) Math.ceil(maxHealth / 2.0F);
    }

    /** Whether all the bar's hearts are white. */
    public static boolean allWhite(float maxHealth, int whiteHearts) {
        return whiteHearts > 0 && whiteHearts >= hearts(maxHealth);
    }

    /** Whether the heart at this index (0 the bar's first) is white, of a bar of this many with this many white. */
    public static boolean isWhite(int heart, int hearts, int whiteHearts) {
        return heart >= 0 && heart < hearts && heart >= hearts - whiteHearts;
    }

    /** Ticks over which the world's grey fades as Conversion wears off. */
    public static final int GREY_FADE_TICKS = 600;
    /** How many steps of grey the screen has (the post effects players-versus:conversion_1 to _4). */
    public static final int GREY_STEPS = 4;

    /**
     * How grey the world looks to someone converting (0 none, GREY_STEPS all): as much of it as their hearts are white,
     * fading in Conversion's last half minute. An amplifier of -1 means no Conversion.
     */
    public static int greyStep(int amplifier, float maxHealth, int duration) {
        if (amplifier < 0) return 0;
        float white = Math.min(1.0F, whiteHearts(amplifier) / (float) Math.max(1, hearts(maxHealth)));
        float time = duration < 0 ? 1.0F : Math.min(1.0F, duration / (float) GREY_FADE_TICKS);
        float strength = white * time;
        return strength <= 0.0F ? 0 : Math.min(GREY_STEPS, (int) Math.ceil(strength * GREY_STEPS - 1.0E-4F));
    }
}
