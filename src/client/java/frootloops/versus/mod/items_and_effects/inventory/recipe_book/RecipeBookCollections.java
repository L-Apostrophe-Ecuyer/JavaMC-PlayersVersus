package frootloops.versus.mod.items_and_effects.inventory.recipe_book;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.items_and_effects.recipes.RecipeFamilies;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * What the recipe book's page lists, from vanilla's collections for the tab (those with recipes that fit the grid,
 * matching the search, craftable ones only when filtering): their recipes split into groups by the item they make
 * (ItemRecipes), and the groups gathered into collections where two or more share a family (FamilyRecipes,
 * RecipeFamilies); the rest stand alone. A recipe's group in its file doesn't matter.
 */
@Environment(EnvType.CLIENT)
public final class RecipeBookCollections {
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private RecipeBookCollections() {}

    /**
     * @param search    what the book's search box holds (vanilla kept a collection when any of its recipes matched, so its
     *                  other items are dropped here unless they match too), or empty
     * @param filtering whether the book shows only what can be crafted now
     */
    public static List<RecipeCollection> regroup(List<RecipeCollection> collections, ContextMap context, String search, boolean filtering) {
        try {
            List<ItemRecipes> groups = groups(collections, context, search.trim().toLowerCase(Locale.ROOT), filtering);
            Map<String, List<ItemRecipes>> families = new LinkedHashMap<>();
            List<RecipeCollection> listed = new ArrayList<>();
            for (ItemRecipes group : groups) {
                String family = RecipeFamilies.collection(BuiltInRegistries.ITEM.getKey(group.icon().getItem()).getPath());
                if (family == null) listed.add(group);
                else families.computeIfAbsent(family, key -> new ArrayList<>()).add(group);
            }
            for (Map.Entry<String, List<ItemRecipes>> family : families.entrySet()) {
                if (family.getValue().size() == 1) listed.add(family.getValue().getFirst());
                else listed.add(FamilyRecipes.of(family.getKey(), sorted(family.getValue(), context)));
            }
            return RecipeBookSorting.sortCollections(listed, context);
        } catch (RuntimeException exception) {
            if (!WARNED.getAndSet(true)) VersusMod.MOD_LOGGER.warn("The recipe book couldn't gather its collections; it keeps vanilla's groups", exception);
            return collections;
        }
    }

    /** The groups, one per item made, in the order the collections list their recipes. */
    private static List<ItemRecipes> groups(List<RecipeCollection> collections, ContextMap context, String search, boolean filtering) {
        Map<Item, Builder> byItem = new LinkedHashMap<>();
        for (RecipeCollection collection : collections) {
            Set<RecipeDisplayId> selected = ids(collection.getSelectedRecipes(RecipeCollection.CraftableStatus.ANY));
            Set<RecipeDisplayId> craftable = ids(collection.getSelectedRecipes(RecipeCollection.CraftableStatus.CRAFTABLE));
            for (RecipeDisplayEntry entry : collection.getRecipes()) {
                ItemStack result = RecipeBookSorting.resultOf(entry, context);
                if (result.isEmpty()) continue;
                Builder builder = byItem.computeIfAbsent(result.getItem(), item -> new Builder(result));
                builder.entries.add(entry);
                if (selected.contains(entry.id())) builder.selected.add(entry.id());
                if (craftable.contains(entry.id())) builder.craftable.add(entry.id());
            }
        }
        List<ItemRecipes> groups = new ArrayList<>();
        for (Builder builder : byItem.values()) {
            if (builder.selected.isEmpty() || filtering && builder.craftable.isEmpty() || !matches(builder.icon, search)) continue;
            groups.add(new ItemRecipes(builder.icon, builder.entries, builder.selected, builder.craftable));
        }
        return groups;
    }

    /** The groups in the book's order: craftable first, then by the inventory sort. */
    private static List<ItemRecipes> sorted(List<ItemRecipes> groups, ContextMap context) {
        List<ItemRecipes> sorted = new ArrayList<>();
        for (RecipeCollection collection : RecipeBookSorting.sortCollections(new ArrayList<>(groups), context)) sorted.add((ItemRecipes) collection);
        return sorted;
    }

    /** Whether the item answers the search, by its name or its id, as vanilla's search tree has them. */
    private static boolean matches(ItemStack stack, String search) {
        if (search.isEmpty()) return true;
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(search)
                || BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains(search);
    }

    private static Set<RecipeDisplayId> ids(List<RecipeDisplayEntry> entries) {
        Set<RecipeDisplayId> ids = new HashSet<>();
        for (RecipeDisplayEntry entry : entries) ids.add(entry.id());
        return ids;
    }

    private static final class Builder {
        final ItemStack icon;
        final List<RecipeDisplayEntry> entries = new ArrayList<>();
        final Set<RecipeDisplayId> selected = new HashSet<>(), craftable = new HashSet<>();

        Builder(ItemStack icon) {
            this.icon = icon.copyWithCount(1);
        }
    }
}
