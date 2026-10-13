package frootloops.versus.mod.mobs.passive;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Where a copper golem puts what it carries. Vanilla walks to the nearest chest, finds out on arrival whether it will
 * take the item (only an empty chest, or one already holding it, does), and tries the next one if not. Ranked here
 * instead, before it sets off, lowest cost first:
 * <ol>
 *   <li>a chest that already holds the item, the one holding the most of it first (the nearest breaks a tie), then</li>
 *   <li>an empty chest, the nearest first, then</li>
 *   <li>when there is neither, the furthest chest that has room, so the golem is never stuck holding the item;</li>
 *   <li>never a chest that has no room for it.</li>
 * </ol>
 * The cost replaces the distance vanilla compares, so everything else (reach, locks, queues, visited chests) is vanilla's.
 * The last tier is a chest vanilla would refuse the item, so the golem is also told to accept it ({@link #roomFor}).
 */
public final class GolemChestRanking {

    /** Every empty chest ranks after every chest already holding the item, however far away. */
    static final double EMPTY_CHEST_COST = 1.0e9;
    /** Chests holding only other things rank after every empty chest, however far away. */
    static final double OTHER_CHEST_COST = 1.0e10;
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
     * @return the cost of this chest, {@link Double#POSITIVE_INFINITY} if the golem should not go there
     */
    public static double cost(int room, int itemsHeld, boolean chestEmpty, double distanceSq) {
        if (room <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        if (itemsHeld > 0) {
            return distanceSq - Math.min(itemsHeld, HELD_ITEMS_COUNTED) * COST_PER_ITEM_HELD;
        }
        return chestEmpty ? EMPTY_CHEST_COST + distanceSq : OTHER_CHEST_COST - distanceSq;
    }

    /** The cost of putting {@code carried} into {@code container}, counted the way the golem's own placing does. */
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
            room += roomIn(stack, carried);
        }
        return cost(room, itemsHeld, chestEmpty, distanceSq);
    }

    /** How many of {@code carried} the container can take, an empty slot taking a full stack. */
    public static int roomFor(Container container, ItemStack carried) {
        int room = 0;
        for (ItemStack stack : container) {
            room += stack.isEmpty() ? carried.getMaxStackSize() : roomIn(stack, carried);
        }
        return room;
    }

    private static int roomIn(ItemStack stack, ItemStack carried) {
        return ItemStack.isSameItemSameComponents(stack, carried) ? Math.max(0, stack.getMaxStackSize() - stack.getCount()) : 0;
    }
}
