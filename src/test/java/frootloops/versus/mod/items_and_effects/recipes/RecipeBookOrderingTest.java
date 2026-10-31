package frootloops.versus.mod.items_and_effects.recipes;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeBookOrderingTest {

    private record Entry(String name, boolean craftable, int rank) {}

    private static final Comparator<Entry> BY_RANK = Comparator.comparingInt(Entry::rank);

    private static List<String> names(List<Entry> entries) {
        return entries.stream().map(Entry::name).toList();
    }

    @Test
    void craftablesComeBeforeUncraftablesEachSortedOnItsOwn() {
        List<Entry> entries = List.of(
                new Entry("lantern", false, 1), new Entry("torch", true, 5), new Entry("chest", false, 0),
                new Entry("stick", true, 2), new Entry("bowl", true, 9), new Entry("ladder", false, 7));
        List<Entry> ordered = RecipeBookOrdering.order(entries, Entry::craftable, BY_RANK);
        assertEquals(List.of("stick", "torch", "bowl", "chest", "lantern", "ladder"), names(ordered));
    }

    @Test
    void equalEntriesKeepTheirOrder() {
        List<Entry> entries = List.of(
                new Entry("a", true, 1), new Entry("b", false, 1), new Entry("c", true, 1), new Entry("d", false, 1), new Entry("e", true, 1));
        assertEquals(List.of("a", "c", "e", "b", "d"), names(RecipeBookOrdering.order(entries, Entry::craftable, BY_RANK)));
    }

    @Test
    void theInputIsLeftAlone() {
        List<Entry> entries = new ArrayList<>(List.of(new Entry("b", false, 2), new Entry("a", true, 1)));
        List<Entry> ordered = RecipeBookOrdering.order(entries, Entry::craftable, BY_RANK);
        assertNotSame(entries, ordered);
        assertEquals(List.of("b", "a"), names(entries));
    }

    @Test
    void emptyAndSingleListsWork() {
        assertTrue(RecipeBookOrdering.order(List.<Entry>of(), Entry::craftable, BY_RANK).isEmpty());
        List<Entry> one = List.of(new Entry("a", false, 3));
        assertEquals(one, RecipeBookOrdering.order(one, Entry::craftable, BY_RANK));
    }

    @Test
    void aComparatorThatIsNotAConsistentOrderNeverThrows() {
        // The inventory's "goes before" test says yes both ways for some pairs, and can go round in circles; List.sort would throw.
        Random random = new Random(20261012L);
        Comparator<Entry> chaotic = (a, b) -> (a.name().hashCode() * 31 + b.name().hashCode()) % 3 - 1;
        for (int round = 0; round < 200; round++) {
            List<Entry> entries = new ArrayList<>();
            int size = random.nextInt(60);
            for (int i = 0; i < size; i++) entries.add(new Entry("e" + random.nextInt(1000), random.nextBoolean(), random.nextInt(5)));
            List<Entry> ordered = RecipeBookOrdering.order(entries, Entry::craftable, chaotic);
            assertEquals(entries.size(), ordered.size());
            boolean seenUncraftable = false;
            for (Entry entry : ordered) {
                if (!entry.craftable()) seenUncraftable = true;
                else assertTrue(!seenUncraftable, "a craftable entry after an uncraftable one");
            }
        }
    }

    @Test
    void thatSortsLikeTheComparatorWhenItIsConsistent() {
        Random random = new Random(7L);
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 500; i++) entries.add(new Entry("e" + i, random.nextBoolean(), random.nextInt(50)));
        List<Entry> ordered = RecipeBookOrdering.order(entries, Entry::craftable, BY_RANK);
        List<Entry> expected = new ArrayList<>(entries);
        expected.sort(Comparator.comparing((Entry e) -> !e.craftable()).thenComparingInt(Entry::rank));
        assertEquals(expected, ordered);
    }

    @Test
    void aBeforeTestMakesAComparator() {
        Comparator<Integer> comparator = RecipeBookOrdering.fromBefore((a, b) -> a < b);
        assertTrue(comparator.compare(1, 2) < 0);
        assertTrue(comparator.compare(2, 1) > 0);
        assertEquals(0, comparator.compare(3, 3));
        // Yes both ways, as the inventory's test says for stacks it can't tell apart: a tie
        assertEquals(0, RecipeBookOrdering.<Integer>fromBefore((a, b) -> true).compare(1, 2));
        assertEquals(0, RecipeBookOrdering.<Integer>fromBefore((a, b) -> false).compare(1, 2));
    }
}
