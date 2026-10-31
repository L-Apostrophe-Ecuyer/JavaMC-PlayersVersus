package frootloops.versus.mod.items_and_effects.inventory.recipe_book;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

import java.util.List;
import java.util.Set;

/**
 * Recipes the book shows on a button of its own, made from vanilla's collections: which of them fit the grid
 * (selected) and which can be crafted now are read from those collections when the book refreshes, as vanilla's own
 * answer them, so the page, its buttons and its popup treat them as any collection.
 */
@Environment(EnvType.CLIENT)
public class SelectedRecipes extends RecipeCollection {
    private final Set<RecipeDisplayId> selected, craftable;

    protected SelectedRecipes(List<RecipeDisplayEntry> entries, Set<RecipeDisplayId> selected, Set<RecipeDisplayId> craftable) {
        super(entries);
        this.selected = selected;
        this.craftable = craftable;
    }

    @Override
    public boolean isCraftable(RecipeDisplayId id) {
        return this.craftable.contains(id);
    }

    @Override
    public boolean hasCraftable() {
        return !this.craftable.isEmpty();
    }

    @Override
    public boolean hasAnySelected() {
        return !this.selected.isEmpty();
    }

    @Override
    public List<RecipeDisplayEntry> getSelectedRecipes(CraftableStatus status) {
        return this.getRecipes().stream().filter(entry -> switch (status) {
            case ANY -> this.selected.contains(entry.id());
            case CRAFTABLE -> this.craftable.contains(entry.id());
            case NOT_CRAFTABLE -> this.selected.contains(entry.id()) && !this.craftable.contains(entry.id());
            default -> this.selected.contains(entry.id());
        }).toList();
    }
}
