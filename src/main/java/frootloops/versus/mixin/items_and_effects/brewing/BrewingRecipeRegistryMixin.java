package frootloops.versus.mixin.items_and_effects.brewing;

import frootloops.versus.mod.items_and_effects.brewing.BrewingSystem;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(PotionBrewing.class)
public class BrewingRecipeRegistryMixin {

    @Overwrite
    public static void addVanillaMixes(PotionBrewing.Builder builder) {
        BrewingSystem.setBrewingRecipeRegistry(builder);
        builder.build();
    }
}


