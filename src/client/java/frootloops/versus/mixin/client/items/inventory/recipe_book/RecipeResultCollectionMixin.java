package frootloops.versus.mixin.client.items.inventory.recipe_book;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import org.spongepowered.asm.mixin.Mixin;


@Environment(EnvType.CLIENT)
@Mixin(RecipeResultCollection.class)
public abstract class RecipeResultCollectionMixin {



}
