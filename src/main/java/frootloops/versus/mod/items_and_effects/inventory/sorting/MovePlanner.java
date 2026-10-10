package frootloops.versus.mod.items_and_effects.inventory.sorting;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Plans the clicks that lay an inventory out as sorted, each click swapping one slot with a spare slot.
 * Slots are compared by class: stacks of the same class are interchangeable, and class 0 is an empty slot.
 * Slots already holding their target class stay put, and each cycle of moves costs one click per slot plus one.
 */
public final class MovePlanner {

    private MovePlanner() {
    }

    /**
     * The positions to swap with the spare, in order, to turn {@code currentClass} into {@code targetClass}.
     * {@code spare} is the spare's own position when it is one of the positions, else -1: the spare then holds the
     * same stack before and after.
     *
     * @throws IllegalArgumentException if the two arrays don't hold the same classes
     */
    public static int[] swapsWithSpare(int[] currentClass, int[] targetClass, int spare) {
        int numSlots = currentClass.length;
        if (targetClass.length != numSlots) throw new IllegalArgumentException("Sorting " + numSlots + " slots into " + targetClass.length);

        // Within each class, the k-th stack to move goes to the k-th slot to fill, both in ascending order.
        Map<Integer, ArrayDeque<Integer>> stacksToMove = new HashMap<>();
        for (int p = 0; p < numSlots; p++) {
            if (currentClass[p] != targetClass[p]) stacksToMove.computeIfAbsent(currentClass[p], c -> new ArrayDeque<>()).add(p);
        }
        int[] destination = new int[numSlots];
        Arrays.fill(destination, -1);
        for (int p = 0; p < numSlots; p++) {
            if (currentClass[p] == targetClass[p]) continue;
            ArrayDeque<Integer> stacks = stacksToMove.get(targetClass[p]);
            if (stacks == null || stacks.isEmpty()) throw new IllegalArgumentException("The target has more stacks of class " + targetClass[p]);
            destination[stacks.poll()] = p;
        }
        for (Map.Entry<Integer, ArrayDeque<Integer>> stacks : stacksToMove.entrySet()) {
            if (!stacks.getValue().isEmpty()) throw new IllegalArgumentException("The target has fewer stacks of class " + stacks.getKey());
        }

        // Each swap puts the spare's stack where it belongs and picks up the stack that was there.
        int[] swaps = new int[2 * numSlots];
        int numSwaps = 0;
        boolean[] placed = new boolean[numSlots];
        if (spare >= 0 && destination[spare] >= 0) {
            placed[spare] = true;
            for (int p = destination[spare]; p != spare; p = destination[p]) {
                swaps[numSwaps++] = p;
                placed[p] = true;
            }
        }
        for (int start = 0; start < numSlots; start++) {
            if (placed[start] || destination[start] < 0) continue;
            swaps[numSwaps++] = start;
            placed[start] = true;
            for (int p = destination[start]; p != start; p = destination[p]) {
                swaps[numSwaps++] = p;
                placed[p] = true;
            }
            swaps[numSwaps++] = start;
        }
        return Arrays.copyOf(swaps, numSwaps);
    }

    /**
     * The first empty hotbar slot, else the first one whose stack the sorted slots accept, else -1.
     */
    public static int chooseSpare(boolean[] emptyHotbar, boolean[] placeableHotbar) {
        for (int i = 0; i < emptyHotbar.length; i++) if (emptyHotbar[i]) return i;
        for (int i = 0; i < placeableHotbar.length; i++) if (placeableHotbar[i]) return i;
        return -1;
    }
}
