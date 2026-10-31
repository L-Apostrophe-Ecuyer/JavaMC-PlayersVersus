package frootloops.versus.mixin.client.items_and_effects.inventory.recipe_book;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.items_and_effects.inventory.recipe_book.RecipeBookSorting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * A recipe group's button: its recipes are listed craftable first (then in the inventory sort's order), and its icon, and the recipe a click places, cycle through the craftable ones when any can be crafted. The
 * button still counts every recipe of the group, so its popup lists them all.
 */
@Environment(EnvType.CLIENT)
@Mixin(RecipeButton.class)
public abstract class RecipeButtonMixin {
    @Shadow private RecipeCollection collection;

    @WrapOperation(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeCollection;getSelectedRecipes(Lnet/minecraft/client/gui/screens/recipebook/RecipeCollection$CraftableStatus;)Ljava/util/List;"))
    private List<RecipeDisplayEntry> playersVersus$craftableFirst(RecipeCollection recipes, RecipeCollection.CraftableStatus status,
                                                                 Operation<List<RecipeDisplayEntry>> original, @Local(argsOnly = true) ContextMap context) {
        List<RecipeDisplayEntry> entries = original.call(recipes, status);
        if (!VersusSettings.QOL.DO_RECIPE_ICONS_CYCLE_CRAFTABLE) return entries;
        return RecipeBookSorting.sortGroupEntries(recipes, entries, context);
    }

    /** How many recipes the icon and the clicked recipe cycle through: the craftable ones (the list starts with them), or all. */
    @WrapOperation(method = {"getDisplayStack", "getCurrentRecipe"}, at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
    private int playersVersus$cycleThroughCraftable(List<?> entries, Operation<Integer> original) {
        int size = original.call(entries);
        if (!VersusSettings.QOL.DO_RECIPE_ICONS_CYCLE_CRAFTABLE) return size;
        int craftable = this.collection.getSelectedRecipes(RecipeCollection.CraftableStatus.CRAFTABLE).size();
        return craftable > 0 && craftable < size ? craftable : size;
    }
}
