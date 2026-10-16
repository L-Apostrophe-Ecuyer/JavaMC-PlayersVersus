package frootloops.versus.mod.items_and_effects.recipes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * How the recipe book orders what it lists, in two sections: what can be crafted now, then what lacks an ingredient,
 * each section ordered the way the inventory sort orders items. The order within a section comes from a comparator the
 * caller supplies (the client's adapter to the inventory sort), which is kept apart from here so this can be tested
 * without the game.
 */
public final class RecipeBookOrdering {
    private RecipeBookOrdering() {}

    /** A copy of the items with the craftable ones first, then the rest, each section in the comparator's order (equal items keep theirs). */
    public static <T> List<T> order(List<T> items, Predicate<? super T> craftable, Comparator<? super T> within) {
        List<T> craftables = new ArrayList<>(), others = new ArrayList<>();
        for (T item : items) (craftable.test(item) ? craftables : others).add(item);
        List<T> ordered = new ArrayList<>(items.size());
        ordered.addAll(mergeSort(craftables, within));
        ordered.addAll(mergeSort(others, within));
        return ordered;
    }

    /** As {@link #order(List, Predicate, Comparator)}, with the pinned items first in their section. */
    public static <T> List<T> order(List<T> items, Predicate<? super T> craftable, Predicate<? super T> pinned, Comparator<? super T> within) {
        return order(items, craftable, (a, b) -> {
            boolean aPinned = pinned.test(a), bPinned = pinned.test(b);
            return aPinned == bPinned ? within.compare(a, b) : aPinned ? -1 : 1;
        });
    }

    /**
     * A comparator from a "goes before" test, as the inventory sort has: before one way only decides, before both ways or
     * neither is a tie (the test isn't a consistent order, and says yes both ways for stacks it can't tell apart).
     */
    public static <T> Comparator<T> fromBefore(BiPredicate<? super T, ? super T> before) {
        return (a, b) -> {
            boolean aFirst = before.test(a, b), bFirst = before.test(b, a);
            return aFirst == bFirst ? 0 : aFirst ? -1 : 1;
        };
    }

    /** A stable merge sort: unlike {@link List#sort}, it never throws for a comparator that isn't a consistent order. */
    private static <T> List<T> mergeSort(List<T> items, Comparator<? super T> comparator) {
        if (items.size() < 2) return items;
        int middle = items.size() / 2;
        List<T> left = mergeSort(items.subList(0, middle), comparator), right = mergeSort(items.subList(middle, items.size()), comparator);
        List<T> merged = new ArrayList<>(items.size());
        int l = 0, r = 0;
        while (l < left.size() && r < right.size()) {
            merged.add(comparator.compare(right.get(r), left.get(l)) < 0 ? right.get(r++) : left.get(l++));
        }
        while (l < left.size()) merged.add(left.get(l++));
        while (r < right.size()) merged.add(right.get(r++));
        return merged;
    }
}
