package frootloops.versus.mod.items_and_effects.inventory.recipe_book;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemComparaisonHelper;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items_and_effects.recipes.RecipeBookOrdering;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The recipe book's order, by what the recipes make, the way the inventory sort orders items (ItemComparaisonHelper: its
 * types, then rarity, tool and armor value, food value, block kind and color). The two sections, craftable first, are
 * RecipeBookOrdering's. A sort that fails (some item the comparison can't handle) leaves the list as it was.
 */
@Environment(EnvType.CLIENT)
public final class RecipeBookSorting {
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private RecipeBookSorting() {}

    /** The groups the page lists: those with a recipe to craft now first, then the others, each by their best recipe's result. */
    public static List<RecipeCollection> sortCollections(List<RecipeCollection> collections, ContextMap context) {
        try {
            Ordering ordering = new Ordering();
            Map<RecipeCollection, ItemStack> representatives = new IdentityHashMap<>();
            for (RecipeCollection collection : collections) representatives.put(collection, ordering.best(entriesOf(collection), context));
            Comparator<RecipeCollection> byResult = (a, b) -> ordering.compare(representatives.get(a), representatives.get(b));
            return RecipeBookOrdering.order(collections, RecipeCollection::hasCraftable, byResult);
        } catch (RuntimeException exception) {
            warn(exception);
            return collections;
        }
    }

    /** One section of a group's popup (all craftable, or all not): its recipes by what they make. */
    public static List<RecipeDisplayEntry> sortEntries(List<RecipeDisplayEntry> entries, ContextMap context) {
        try {
            Ordering ordering = new Ordering();
            Map<RecipeDisplayEntry, ItemStack> results = new IdentityHashMap<>();
            for (RecipeDisplayEntry entry : entries) results.put(entry, resultOf(entry, context));
            return RecipeBookOrdering.order(entries, entry -> true, (a, b) -> ordering.compare(results.get(a), results.get(b)));
        } catch (RuntimeException exception) {
            warn(exception);
            return entries;
        }
    }

    private static List<RecipeDisplayEntry> entriesOf(RecipeCollection collection) {
        List<RecipeDisplayEntry> entries = collection.getSelectedRecipes(RecipeCollection.CraftableStatus.CRAFTABLE);
        if (entries.isEmpty()) entries = collection.getSelectedRecipes(RecipeCollection.CraftableStatus.ANY);
        return entries.isEmpty() ? collection.getRecipes() : entries;
    }

    private static ItemStack resultOf(RecipeDisplayEntry entry, ContextMap context) {
        List<ItemStack> results = entry.resultItems(context);
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst();
    }

    private static void warn(RuntimeException exception) {
        if (!WARNED.getAndSet(true)) VersusMod.MOD_LOGGER.warn("The recipe book couldn't be ordered; it keeps vanilla's order", exception);
    }

    /** The inventory sort's comparison of stacks, as an order: its "goes before" test, then the registry's order for the ties. */
    private static final class Ordering {
        private final Map<ItemStack, ItemSlot> slots = new IdentityHashMap<>();
        private final Comparator<ItemStack> byInventorySort = RecipeBookOrdering.fromBefore((a, b) -> ItemComparaisonHelper.shouldGoBefore(slot(a), slot(b)));

        int compare(ItemStack a, ItemStack b) {
            if (a.isEmpty() != b.isEmpty()) return a.isEmpty() ? 1 : -1;
            if (a.isEmpty()) return 0;
            int byTest = byInventorySort.compare(a, b);
            if (byTest != 0) return byTest;
            return Integer.compare(BuiltInRegistries.ITEM.getId(a.getItem()), BuiltInRegistries.ITEM.getId(b.getItem()));
        }

        /** The stack that goes first of what the recipes make. */
        ItemStack best(List<RecipeDisplayEntry> entries, ContextMap context) {
            ItemStack best = ItemStack.EMPTY;
            for (RecipeDisplayEntry entry : entries) {
                ItemStack result = resultOf(entry, context);
                if (best.isEmpty() || compare(result, best) < 0) best = result;
            }
            return best;
        }

        private ItemSlot slot(ItemStack stack) {
            return slots.computeIfAbsent(stack, key -> new ItemSlot(-1, key, ItemComparaisonHelper.getItemTypeOf(key)));
        }
    }
}
