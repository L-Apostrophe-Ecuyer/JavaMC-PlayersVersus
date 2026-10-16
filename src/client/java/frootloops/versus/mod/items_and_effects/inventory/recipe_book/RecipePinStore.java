package frootloops.versus.mod.items_and_effects.inventory.recipe_book;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.items_and_effects.recipes.RecipePins;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

/** The player's pinned recipes, kept in {@code config/players-versus/pinned_recipes.json}. */
@Environment(EnvType.CLIENT)
public final class RecipePinStore {
    private static RecipePins pins;

    private RecipePinStore() {}

    public static synchronized RecipePins get() {
        if (pins == null) {
            pins = new RecipePins(FabricLoader.getInstance().getConfigDir().resolve(VersusMod.MOD_ID).resolve("pinned_recipes.json"), VersusMod.MOD_LOGGER::warn);
        }
        return pins;
    }
}
