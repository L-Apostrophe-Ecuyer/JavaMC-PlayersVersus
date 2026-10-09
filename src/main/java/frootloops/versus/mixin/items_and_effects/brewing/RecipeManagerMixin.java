package frootloops.versus.mixin.items_and_effects.brewing;

import frootloops.versus.mod.items_and_effects.brewing.BrewingSystem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Brewing in code, as 1.21.x's brewing registry was: the recipe manager indexes the brewing graph's recipes in place of
 * vanilla's brewing recipes ({@link BrewingSystem#replaceVanillaBrewing}). Whatever asks the recipe manager follows: the
 * brewing stand, its slots and hoppers (through the brewing property sets), and Fabric's recipe API, whose own hook on
 * {@code RecipeMap.create} still runs. The recipe registry itself is left as the data packs made it.
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {

    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/RecipeMap;create(Lnet/minecraft/core/HolderLookup;)Lnet/minecraft/world/item/crafting/RecipeMap;"
            )
    )
    private HolderLookup<Recipe<?>> playersVersus$brewTheGraph(HolderLookup<Recipe<?>> recipes) {
        return BrewingSystem.replaceVanillaBrewing(recipes);
    }
}
