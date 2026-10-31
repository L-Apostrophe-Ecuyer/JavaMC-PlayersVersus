package frootloops.versus.mod.items_and_effects.inventory.recipe_book;

import frootloops.versus.mod.items_and_effects.recipes.RecipeFamilies;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A collection: groups of similar items (RecipeFamilies), its button on the page showing them in turn; its popup and
 * window (FamilyWindow) list the items' icons.
 */
@Environment(EnvType.CLIENT)
public final class FamilyRecipes extends SelectedRecipes {
    private final String family;
    private final List<ItemRecipes> items;

    private FamilyRecipes(String family, List<ItemRecipes> items, List<RecipeDisplayEntry> entries, Set<RecipeDisplayId> selected, Set<RecipeDisplayId> craftable) {
        super(entries, selected, craftable);
        this.family = family;
        this.items = items;
    }

    /** The collection of these groups, in this order. */
    static FamilyRecipes of(String family, List<ItemRecipes> items) {
        List<RecipeDisplayEntry> entries = new ArrayList<>();
        Set<RecipeDisplayId> selected = new HashSet<>(), craftable = new HashSet<>();
        for (ItemRecipes item : items) {
            entries.addAll(item.getRecipes());
            for (RecipeDisplayEntry entry : item.getSelectedRecipes(CraftableStatus.ANY)) selected.add(entry.id());
            for (RecipeDisplayEntry entry : item.getSelectedRecipes(CraftableStatus.CRAFTABLE)) craftable.add(entry.id());
        }
        return new FamilyRecipes(family, List.copyOf(items), entries, selected, craftable);
    }

    /** Its key in RecipeFamilies, which names it across refreshes. */
    public String family() {
        return this.family;
    }

    public String title() {
        return RecipeFamilies.title(this.family);
    }

    public List<ItemRecipes> items() {
        return this.items;
    }
}
