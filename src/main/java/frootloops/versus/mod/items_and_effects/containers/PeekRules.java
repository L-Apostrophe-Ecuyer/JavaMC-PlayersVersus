package frootloops.versus.mod.items_and_effects.containers;

/**
 * When the peek at a placed shulker box takes the wheel and the use from the game: only while it shows, is wanted, and the
 * main hand is empty (with something in it, the wheel is the hotbar's and the use is that item's, or the box's opening).
 */
public final class PeekRules {

    private PeekRules() {
    }

    /**
     * @param enabled    the peek turned on (its key)
     * @param shows      a box looked at, the server's answer in, something in it
     * @param screenOpen a screen open (the HUD, and so the peek, under it)
     * @param sneaking   the player sneaking (the way out: the hotbar's wheel, vanilla's use)
     * @param hudHidden  the HUD hidden (F1): the peek isn't drawn
     * @param emptyHand  nothing in the main hand
     */
    public static boolean takesInput(boolean enabled, boolean shows, boolean screenOpen, boolean sneaking, boolean hudHidden, boolean emptyHand) {
        return enabled && shows && !screenOpen && !sneaking && !hudHidden && emptyHand;
    }
}
