package frootloops.versus.mixin.items.brewing;

import frootloops.versus.mod.items.brewing.BrewingSystem;
import net.minecraft.recipe.BrewingRecipeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BrewingRecipeRegistry.class)
public class BrewingRecipeRegistryMixin {

    @Overwrite
    public static void registerDefaults(BrewingRecipeRegistry.Builder builder) {
        BrewingSystem.setBrewingRecipeRegistry(builder);
        builder.build();
    }
}


