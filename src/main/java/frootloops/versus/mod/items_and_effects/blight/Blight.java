package frootloops.versus.mod.items_and_effects.blight;

import frootloops.versus.VersusSettings;

/**
 * Blight (the Wither effect's replacement, and pale shrooms' and the pale's), by numbers: its hearts are blighted (drawn
 * as the Wither's black hearts), one per amplifier + 1, from the end of the health bar down towards none. Each dose adds
 * its amplifier + 1 to what was had, and refreshes it without shortening it. Food doesn't regenerate health into
 * blighted hearts; the shaking starts when enough of them are blighted and builds. Fully blighted (as many hearts as the
 * bar has, max health / 2), it deals a heart every FULL_DAMAGE_TICKS, and whatever dies fully blighted is replaced by its
 * pale form (Undeath).
 */
public final class Blight {
    private Blight() {}

    /** Blighted hearts for an amplifier: one more than it. */
    public static int blightedHearts(int amplifier) {
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

    /** The health food regenerates up to: what the blighted hearts leave of the bar. */
    public static float regenCap(float maxHealth, int blightedHearts) {
        return Math.max(0.0F, maxHealth - 2.0F * blightedHearts);
    }

    /** Hearts in the bar for this health, a half counting as one. */
    public static int hearts(float maxHealth) {
        return (int) Math.ceil(maxHealth / 2.0F);
    }

    /** Whether it's fully blighted: as many hearts blighted as the bar has (max health / 2, a half counting as one). */
    public static boolean fullyBlighted(float maxHealth, int blightedHearts) {
        return blightedHearts > 0 && blightedHearts >= hearts(maxHealth);
    }

    /** Whether the heart at this index (0 the bar's first) is blighted, of a bar of this many with this many blighted. */
    public static boolean isBlighted(int heart, int hearts, int blightedHearts) {
        return heart >= 0 && heart < hearts && heart >= hearts - blightedHearts;
    }

    /** Whether the Blight deals its damage this tick, this many ticks after it became full: then and every FULL_DAMAGE_TICKS. */
    public static boolean damagesNow(long ticksFull) {
        return ticksFull >= 0 && ticksFull % VersusSettings.Blight.FULL_DAMAGE_TICKS == 0;
    }

    /** The amplifier of the Blight a Wither of this amplifier becomes: the same strength, at most WITHER_MAX_HEARTS hearts a dose. */
    public static int fromWitherAmplifier(int witherAmplifier) {
        return Math.max(0, Math.min(witherAmplifier, VersusSettings.Blight.WITHER_MAX_HEARTS - 1));
    }

    /** Ticks over which the world's grey fades as Blight wears off. */
    public static final int GREY_FADE_TICKS = 600;

    /**
     * How grey the world looks to someone blighted, 0 (not at all) to 1 (all grey): as much as their hearts are blighted,
     * fading in Blight's last half minute. An amplifier of -1 means no Blight.
     */
    public static float greyStrength(int amplifier, float maxHealth, int duration) {
        if (amplifier < 0) return 0.0F;
        float blighted = Math.min(1.0F, blightedHearts(amplifier) / (float) Math.max(1, hearts(maxHealth)));
        float time = duration < 0 ? 1.0F : Math.min(1.0F, duration / (float) GREY_FADE_TICKS);
        return blighted * time;
    }

    /** How hard the blighted shake (0 to 1): nothing until SHAKE_FROM of their hearts are blighted, all the way at all of them. */
    public static float shakeStrength(int amplifier, float maxHealth) {
        if (amplifier < 0) return 0.0F;
        float blighted = Math.min(1.0F, blightedHearts(amplifier) / (float) Math.max(1, hearts(maxHealth)));
        float from = VersusSettings.Blight.SHAKE_FROM;
        return Math.max(0.0F, Math.min(1.0F, (blighted - from) / (1.0F - from)));
    }

    /** The amplifier left after this many blighted hearts are purged: below nothing (-1 or less), none is left. */
    public static int purgedAmplifier(int amplifier, int hearts) {
        return amplifier - Math.max(0, hearts);
    }

    /**
     * The blighted hearts a healing potion purges: the hearts it heals (2 for Healing I, 4 for II...), by the part of its
     * strength that reaches (1 for a drunk one, less for a splash's edge), to the nearest heart.
     */
    public static int healingPurge(int amplifier, double scale) {
        return (int) Math.round((2 << Math.max(0, Math.min(amplifier, 10))) * Math.max(0.0, Math.min(1.0, scale)));
    }

    /** The blighted hearts absorption purges: one for each heart of it gained (rounded up). */
    public static int absorptionPurge(float gained) {
        return gained <= 0.0F ? 0 : (int) Math.ceil(gained / 2.0F);
    }
}
