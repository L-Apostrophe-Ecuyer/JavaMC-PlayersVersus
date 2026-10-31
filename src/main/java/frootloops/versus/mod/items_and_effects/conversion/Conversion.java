package frootloops.versus.mod.items_and_effects.conversion;

/**
 * Conversion, pale shrooms' effect, by numbers: its hearts turn white, one per amplifier + 1, from the end of the health
 * bar down towards none. Each dose adds its amplifier + 1 to what was had, and refreshes it without shortening it. Food doesn't regenerate health into white
 * hearts; all hearts white, the entity shroomifies (Shroomify).
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
}
