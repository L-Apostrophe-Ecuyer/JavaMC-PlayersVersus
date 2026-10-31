package frootloops.versus.mod.items_and_effects.inventory.recipe_book;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

import java.util.List;
import java.util.Set;

/** A group: the recipes the book knows for one item, its button on the page or a cell in a collection's window. */
@Environment(EnvType.CLIENT)
public final class ItemRecipes extends SelectedRecipes {
    private final ItemStack icon;

    ItemRecipes(ItemStack icon, List<RecipeDisplayEntry> entries, Set<RecipeDisplayId> selected, Set<RecipeDisplayId> craftable) {
        super(entries, selected, craftable);
        this.icon = icon;
    }

    /** What the recipes make, as the window shows it. */
    public ItemStack icon() {
        return this.icon;
    }

    /** The recipe a click uses: the first that can be crafted now, else the first that fits the grid. */
    public RecipeDisplayEntry recipe() {
        List<RecipeDisplayEntry> craftable = this.getSelectedRecipes(CraftableStatus.CRAFTABLE);
        return craftable.isEmpty() ? this.getSelectedRecipes(CraftableStatus.ANY).getFirst() : craftable.getFirst();
    }

    /** Whether there are several recipes to choose from, whose shapes its popup shows. */
    public boolean hasSeveralRecipes() {
        return this.getSelectedRecipes(CraftableStatus.ANY).size() > 1;
    }
}
