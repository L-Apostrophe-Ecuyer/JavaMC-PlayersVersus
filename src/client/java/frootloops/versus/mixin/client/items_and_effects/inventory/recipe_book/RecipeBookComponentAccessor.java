package frootloops.versus.mixin.client.items_and_effects.inventory.recipe_book;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The recipe book's search box, page and recipe placing, for the collections' window (FamilyWindow). */
@Environment(EnvType.CLIENT)
@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccessor {
    @Accessor("searchBox")
    EditBox playersVersus$searchBox();

    @Accessor("recipeBookPage")
    RecipeBookPage playersVersus$page();

    @Invoker("tryPlaceRecipe")
    boolean playersVersus$tryPlaceRecipe(RecipeCollection collection, RecipeDisplayId recipe, boolean forMost);
}
