package frootloops.versus.mod.items_and_effects.inventory;

/** When a hand that has run out is refilled, and from where (the game-free part of HotbarRefill, which is tested). */
public final class HotbarRefillRules {
    private HotbarRefillRules() {}

    /**
     * Whether a hand's stack was used up, as opposed to moved or thrown away by the player: it held a stackable item
     * last tick, holds nothing now, is the same hand (the selected slot didn't change), and no key that moves items out
     * of a hand (drop, swap hands) was pressed.
     */
    public static boolean usedUp(boolean heldStackableLastTick, boolean emptyNow, boolean sameHand, boolean movedItemsByKey) {
        return heldStackableLastTick && emptyNow && sameHand && !movedItemsByKey;
    }

    /** The index of the biggest count, the first of equal ones; -1 if every count is zero (nothing to refill from). */
    public static int largest(int[] counts) {
        int best = -1, bestCount = 0;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > bestCount) {
                best = i;
                bestCount = counts[i];
            }
        }
        return best;
    }
}
