package frootloops.versus.mod.items_and_effects.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * What a player pinned in the recipe book, kept in a JSON file (an array of keys) so it outlives the game. A key stands for
 * a recipe by what it makes ({@code r:minecraft:oak_stairs}) or for a group by the first (alphabetically) of the items its
 * recipes make ({@code g:minecraft:oak_boat}): ids that don't change between sessions and servers, unlike the recipes'
 * numbers. A file that can't be read is treated as empty, and one that can't be written only costs the pin its memory.
 */
public final class RecipePins {
    private final Path file;
    private final Consumer<String> warn;
    private final Set<String> keys = new LinkedHashSet<>();

    public RecipePins(Path file, Consumer<String> warn) {
        this.file = file;
        this.warn = warn;
        load();
    }

    public static String recipeKey(String resultItemId) {
        return "r:" + resultItemId;
    }

    /** A group's key: that of its only recipe, or the first id of the several its recipes make. */
    public static String groupKey(Collection<String> resultItemIds) {
        if (resultItemIds.size() == 1) return recipeKey(resultItemIds.iterator().next());
        return "g:" + resultItemIds.stream().min(String::compareTo).orElse("");
    }

    public boolean isPinned(String key) {
        return keys.contains(key);
    }

    /** Pins what isn't pinned and unpins what is; returns whether it is pinned now. */
    public boolean toggle(String key) {
        boolean pinned = keys.add(key);
        if (!pinned) keys.remove(key);
        save();
        return pinned;
    }

    private void load() {
        if (!Files.isRegularFile(file)) return;
        try {
            JsonElement json = JsonParser.parseString(Files.readString(file));
            if (!json.isJsonArray()) return;
            for (JsonElement key : json.getAsJsonArray()) {
                if (key.isJsonPrimitive() && key.getAsJsonPrimitive().isString()) keys.add(key.getAsString());
            }
        } catch (IOException | RuntimeException exception) {
            warn.accept("The pinned recipes couldn't be read (" + file + "): " + exception.getMessage());
            keys.clear();
        }
    }

    private void save() {
        try {
            JsonArray array = new JsonArray();
            keys.forEach(array::add);
            Files.createDirectories(file.getParent());
            Files.writeString(file, array.toString());
        } catch (IOException exception) {
            warn.accept("The pinned recipes couldn't be saved (" + file + "): " + exception.getMessage());
        }
    }
}
