package frootloops.versus.mod.mobs.passive;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Where a copper golem carrying something goes next. Vanilla walks to the nearest chest, finds out on arrival whether it
 * will take the item (an empty chest, or one already holding it, does) and, if not, tries the next one. The search is
 * ranked here instead, lowest cost first:
 * <ol>
 *   <li>a chest the item fits in because it already holds some, the one holding the most of it first (the nearest
 *       breaks a tie), then</li>
 *   <li>an empty chest, the nearest first, then</li>
 *   <li>when there is neither, the last one looked into: the furthest chest, full or not.</li>
 * </ol>
 * Whatever is chosen, the golem walks there and asks again on arrival, as in vanilla: can it take the item? Is the chest
 * empty? If not, the chest is marked visited and the search starts over. The cost replaces the distance vanilla
 * compares, so everything else (reach, locks, queues, visited chests) is vanilla's.
 */
public final class GolemChestRanking {

    /** Every empty chest ranks after every chest the item fits in, however far away. */
    static final double EMPTY_CHEST_COST = 1.0e9;
    /** The last resort ranks after every empty chest, however far away. */
    static final double LAST_RESORT_COST = 1.0e10;
    /** Per item already in the chest; the cap keeps the bonus under {@link #EMPTY_CHEST_COST}. */
    static final double COST_PER_ITEM_HELD = 1.0e3;
    static final int HELD_ITEMS_COUNTED = 100_000;

    private GolemChestRanking() {
    }

    /**
     * @param room         how many of the carried item the chest can still take
     * @param itemsHeld    how many of the carried item are in the chest already
     * @param chestEmpty   whether the chest holds nothing at all
     * @param distanceSq   squared distance from the golem to the chest
     * @return the cost of this chest; every chest has one, the best (lowest) is where the golem goes
     */
    public static double cost(int room, int itemsHeld, boolean chestEmpty, double distanceSq) {
        if (itemsHeld > 0 && room > 0) {
            return distanceSq - Math.min(itemsHeld, HELD_ITEMS_COUNTED) * COST_PER_ITEM_HELD;
        }
        return chestEmpty ? EMPTY_CHEST_COST + distanceSq : LAST_RESORT_COST - distanceSq;
    }

    /** The cost of {@code container} for {@code carried}, counted the way the golem's own placing does. */
    public static double cost(Container container, ItemStack carried, double distanceSq) {
        int room = 0;
        int itemsHeld = 0;
        boolean chestEmpty = true;
        for (ItemStack stack : container) {
            if (stack.isEmpty()) {
                room += carried.getMaxStackSize();
                continue;
            }
            chestEmpty = false;
            if (ItemStack.isSameItem(stack, carried)) {
                itemsHeld += stack.getCount();
            }
            if (ItemStack.isSameItemSameComponents(stack, carried)) {
                room += Math.max(0, stack.getMaxStackSize() - stack.getCount());
            }
        }
        return cost(room, itemsHeld, chestEmpty, distanceSq);
    }
}
