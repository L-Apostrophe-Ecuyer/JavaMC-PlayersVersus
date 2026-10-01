package frootloops.versus.mixin.items_and_effects.brewing;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

@Mixin(RecipeMap.class)
public class RecipeMapMixin {

    @Inject(method = "getRecipesFor", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void replaceVanillaBrewingRecipes(
            RecipeType<T> recipeType,
            I input,
            Level level,
            CallbackInfoReturnable<Stream<RecipeHolder<T>>> cir
    ) {
        if (recipeType == RecipeType.BREWING) {
            cir.setReturnValue(cir.getReturnValue().filter(holder ->
                    !holder.id().identifier().getNamespace().equals(Identifier.DEFAULT_NAMESPACE)));
        }
    }
}
