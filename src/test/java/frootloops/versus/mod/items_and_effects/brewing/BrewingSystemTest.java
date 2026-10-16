package frootloops.versus.mod.items_and_effects.brewing;

import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import net.minecraft.world.item.crafting.RecipeManager;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BrewingSystemTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    /**
     * The recipe manager's mixin, which swaps vanilla's brewing recipes for the brewing graph's, applies on 26.3: its
     * handler is in the class. (A target missing from 26.3 would already have failed loading the class, as the mixins are
     * required.)
     */
    @Test
    void theRecipeManagerMixinApplies() {
        assertTrue(Arrays.stream(RecipeManager.class.getDeclaredMethods()).map(Method::getName)
                .anyMatch(method -> method.endsWith("playersVersus$brewTheGraph")));
    }
}
