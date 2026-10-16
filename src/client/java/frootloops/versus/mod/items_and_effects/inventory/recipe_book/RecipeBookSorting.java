package frootloops.versus.mod.items_and_effects.inventory.recipe_book;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemComparaisonHelper;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items_and_effects.recipes.RecipeBookOrdering;
import frootloops.versus.mod.items_and_effects.recipes.RecipePins;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The recipe book's order, by what the recipes make, the way the inventory sort orders items (ItemComparaisonHelper: its
 * types, then rarity, tool and armor value, food value, block kind and color). The two sections, craftable first, are
 * RecipeBookOrdering's, with what the player pinned first in its section (RecipePins). A sort that fails (some item the
 * comparison can't handle) leaves the list as it was.
 */
@Environment(EnvType.CLIENT)
public final class RecipeBookSorting {
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    /** The pin keys of groups and recipes, worked out once: they only depend on what the recipes make. */
    private static final Map<RecipeCollection, String> GROUP_KEYS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<RecipeDisplayEntry, String> RECIPE_KEYS = Collections.synchronizedMap(new WeakHashMap<>());

    private RecipeBookSorting() {}

    /** The groups the page lists: those with a recipe to craft now first, then the others, each by their best recipe's result. */
    public static List<RecipeCollection> sortCollections(List<RecipeCollection> collections, ContextMap context) {
        try {
            Ordering ordering = new Ordering();
            Map<RecipeCollection, ItemStack> representatives = new IdentityHashMap<>();
            for (RecipeCollection collection : collections) representatives.put(collection, ordering.best(entriesOf(collection), context));
            Comparator<RecipeCollection> byResult = (a, b) -> ordering.compare(representatives.get(a), representatives.get(b));
            return RecipeBookOrdering.order(collections, RecipeCollection::hasCraftable, collection -> isPinned(collection, context), byResult);
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
            return RecipeBookOrdering.order(entries, entry -> true, entry -> isPinned(entry, context), (a, b) -> ordering.compare(results.get(a), results.get(b)));
        } catch (RuntimeException exception) {
            warn(exception);
            return entries;
        }
    }

    /**
     * The recipes of one group for its button: those that can be crafted first, then the others (the button's icon and the
     * recipe a click places go through the craftable ones), each section in the inventory sort's order when that is on.
     */
    public static List<RecipeDisplayEntry> sortGroupEntries(RecipeCollection collection, List<RecipeDisplayEntry> entries, ContextMap context) {
        try {
            Ordering ordering = new Ordering();
            Map<RecipeDisplayEntry, ItemStack> results = new IdentityHashMap<>();
            for (RecipeDisplayEntry entry : entries) results.put(entry, resultOf(entry, context));
            boolean smart = VersusSettings.QOL.DO_SMART_RECIPE_ORDER;
            return RecipeBookOrdering.order(entries, entry -> collection.isCraftable(entry.id()), entry -> isPinned(entry, context),
                    (a, b) -> smart ? ordering.compare(results.get(a), results.get(b)) : 0);
        } catch (RuntimeException exception) {
            warn(exception);
            return entries;
        }
    }

    // Pins

    /** Whether the group is pinned (a group of a single recipe is pinned as that recipe). */
    public static boolean isPinned(RecipeCollection collection, ContextMap context) {
        if (!VersusSettings.QOL.DO_RECIPE_BOOK_PINS) return false;
        return RecipePinStore.get().isPinned(groupKey(collection, context));
    }

    /** As above, for drawing: the context comes from the level. */
    public static boolean isPinned(RecipeCollection collection) {
        if (!VersusSettings.QOL.DO_RECIPE_BOOK_PINS || Minecraft.getInstance().level == null) return false;
        String key = GROUP_KEYS.get(collection);
        if (key == null) key = groupKey(collection, SlotDisplayContext.fromLevel(Minecraft.getInstance().level));
        return RecipePinStore.get().isPinned(key);
    }

    public static boolean isPinned(RecipeDisplayEntry entry, ContextMap context) {
        if (!VersusSettings.QOL.DO_RECIPE_BOOK_PINS) return false;
        String key = recipeKey(entry, context);
        return key != null && RecipePinStore.get().isPinned(key);
    }

    /** The key a group is pinned by. */
    public static String groupKey(RecipeCollection collection, ContextMap context) {
        return GROUP_KEYS.computeIfAbsent(collection, key -> {
            Set<String> ids = new HashSet<>();
            for (RecipeDisplayEntry entry : collection.getRecipes()) {
                String id = idOf(resultOf(entry, context));
                if (id != null) ids.add(id);
            }
            return ids.isEmpty() ? "g:" : RecipePins.groupKey(ids);
        });
    }

    /** The key a recipe is pinned by, or null for a recipe that makes nothing to pin it by. */
    @Nullable
    public static String recipeKey(RecipeDisplayEntry entry, ContextMap context) {
        String key = RECIPE_KEYS.computeIfAbsent(entry, e -> {
            String id = idOf(resultOf(e, context));
            return id == null ? "" : RecipePins.recipeKey(id);
        });
        return key.isEmpty() ? null : key;
    }

    @Nullable
    private static String idOf(ItemStack stack) {
        return stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
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
