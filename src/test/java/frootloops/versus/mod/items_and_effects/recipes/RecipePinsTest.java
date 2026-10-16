package frootloops.versus.mod.items_and_effects.recipes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipePinsTest {
    @TempDir Path folder;

    @Test
    void togglingPinsAndUnpins() {
        RecipePins pins = new RecipePins(folder.resolve("pins.json"), message -> {});
        assertFalse(pins.isPinned("r:minecraft:torch"));
        assertTrue(pins.toggle("r:minecraft:torch"));
        assertTrue(pins.isPinned("r:minecraft:torch"));
        assertFalse(pins.toggle("r:minecraft:torch"));
        assertFalse(pins.isPinned("r:minecraft:torch"));
    }

    @Test
    void pinsSurviveARestart() {
        Path file = folder.resolve("deeper").resolve("pins.json");
        RecipePins pins = new RecipePins(file, message -> {});
        pins.toggle("r:minecraft:torch");
        pins.toggle("g:minecraft:oak_boat");
        pins.toggle("r:minecraft:chest");
        pins.toggle("r:minecraft:chest");

        RecipePins reopened = new RecipePins(file, message -> {});
        assertTrue(reopened.isPinned("r:minecraft:torch"));
        assertTrue(reopened.isPinned("g:minecraft:oak_boat"));
        assertFalse(reopened.isPinned("r:minecraft:chest"));
    }

    @Test
    void aBrokenFileIsEmptyAndWarned() throws IOException {
        Path file = folder.resolve("pins.json");
        Files.writeString(file, "{ not json");
        List<String> warnings = new ArrayList<>();
        RecipePins pins = new RecipePins(file, warnings::add);
        assertFalse(pins.isPinned("r:minecraft:torch"));
        assertEquals(1, warnings.size());
        assertTrue(pins.toggle("r:minecraft:torch"));
        assertTrue(new RecipePins(file, message -> {}).isPinned("r:minecraft:torch"));
    }

    @Test
    void otherJsonThanAnArrayOfStringsIsIgnored() throws IOException {
        Path file = folder.resolve("pins.json");
        Files.writeString(file, "[\"r:minecraft:torch\", 3, null, {\"a\": 1}, \"g:minecraft:oak_boat\"]");
        RecipePins pins = new RecipePins(file, message -> {});
        assertTrue(pins.isPinned("r:minecraft:torch"));
        assertTrue(pins.isPinned("g:minecraft:oak_boat"));
        Files.writeString(file, "{\"r:minecraft:torch\": true}");
        assertFalse(new RecipePins(file, message -> {}).isPinned("r:minecraft:torch"));
    }

    @Test
    void aGroupsKeyIsTheSameWhateverTheOrderOfItsItems() {
        assertEquals("g:minecraft:oak_boat", RecipePins.groupKey(List.of("minecraft:oak_stairs", "minecraft:oak_boat", "minecraft:oak_door")));
        assertEquals("g:minecraft:oak_boat", RecipePins.groupKey(List.of("minecraft:oak_door", "minecraft:oak_stairs", "minecraft:oak_boat")));
    }

    @Test
    void aGroupOfOneRecipeIsThatRecipe() {
        assertEquals(RecipePins.recipeKey("minecraft:torch"), RecipePins.groupKey(List.of("minecraft:torch")));
    }
}
