package frootloops.versus.mod.items_and_effects.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Recipes changed by rule as their files load, rather than vanilla's files replaced one by one: the rules reach every
 * recipe of their kind, a mod's or a data pack's as well as vanilla's, and a recipe that isn't of their kind (a data
 * pack's own shape for stairs, say) is left as it is.
 * <ul>
 * <li>Stairs take three blocks (an L of two by two) instead of six, for the same four stairs.</li>
 * <li>Slabs take two blocks in a row instead of three, for four slabs instead of six.</li>
 * <li>Pressure plates take three blocks in a row instead of two (the slabs' old shape, as slabs took theirs), for two
 * plates.</li>
 * </ul>
 * Only vanilla's shapes are changed: a stairs recipe whose pattern isn't the staircase of six, a slab's that isn't a row
 * of three, a plate's that isn't a row of two, keeps its own. (The recipe book gathers recipes in collections by
 * itself, RecipeFamilies: the files keep their groups.)
 */
public final class RecipeRules {
    private RecipeRules() {}

    private static final AtomicBoolean LOGGED = new AtomicBoolean();

    /** A recipe's file as the rules have it, logging the first the rules change (that they run, in a server's log). */
    public static JsonElement apply(Identifier id, JsonElement json) {
        JsonElement changed = apply(json);
        if (changed != json && !LOGGED.getAndSet(true)) VersusMod.MOD_LOGGER.info("Recipe rules: changing {} and the like as they load", id);
        return changed;
    }

    /** A recipe's file as the rules have it: a changed copy, or the same file if no rule applies. */
    public static JsonElement apply(JsonElement json) {
        if (!(json instanceof JsonObject recipe)) return json;
        Identifier typeId = string(recipe, "type") instanceof String name ? Identifier.tryParse(name) : null;
        Identifier result = resultId(recipe);
        if (typeId == null || result == null) return json;
        String type = typeId.toString();

        JsonObject changed = null;
        if (VersusSettings.Items.DO_CHEAPER_STAIRS_AND_SLABS && type.equals("minecraft:crafting_shaped")) {
            changed = reshape(recipe, result);
        }
        return changed == null ? json : changed;
    }

    // Shapes

    private static final Set<Cell> STAIRCASE = Set.of(new Cell(0, 0), new Cell(1, 0), new Cell(1, 1), new Cell(2, 0), new Cell(2, 1), new Cell(2, 2));
    private static final Set<Cell> STAIRCASE_MIRRORED = Set.of(new Cell(0, 2), new Cell(1, 1), new Cell(1, 2), new Cell(2, 0), new Cell(2, 1), new Cell(2, 2));

    private static final Set<Cell> ROW_OF_THREE = Set.of(new Cell(0, 0), new Cell(0, 1), new Cell(0, 2));
    private static final Set<Cell> ROW_OF_TWO = Set.of(new Cell(0, 0), new Cell(0, 1));

    private record Cell(int row, int column) {}

    /** A changed copy of a stairs, slab or pressure plate recipe in vanilla's shape; null for any other. */
    @Nullable
    private static JsonObject reshape(JsonObject recipe, Identifier result) {
        Block block = block(result);
        if (block == null || !(recipe.get("key") instanceof JsonObject key) || key.size() != 1) return null;
        if (!(recipe.get("pattern") instanceof JsonArray pattern) || !(recipe.get("result") instanceof JsonObject)) return null;
        String symbol = key.keySet().iterator().next();
        Set<Cell> cells = cells(pattern);
        if (cells == null) return null;
        int count = count(recipe);

        List<String> shape;
        int made;
        if (block instanceof StairBlock && (cells.equals(STAIRCASE) || cells.equals(STAIRCASE_MIRRORED))) {
            shape = List.of(symbol + " ", symbol + symbol);
            made = count;
        } else if (block instanceof SlabBlock && cells.equals(ROW_OF_THREE)) {
            shape = List.of(symbol + symbol);
            made = Math.max(1, Math.round(count * 2.0F / 3.0F));
        } else if (block instanceof BasePressurePlateBlock && cells.equals(ROW_OF_TWO)) {
            shape = List.of(symbol + symbol + symbol);
            made = count * 2;
        } else {
            return null;
        }
        JsonObject changed = recipe.deepCopy();
        JsonArray rows = new JsonArray();
        shape.forEach(rows::add);
        changed.add("pattern", rows);
        changed.getAsJsonObject("result").addProperty("count", made);
        return changed;
    }

    /** The filled cells of a pattern, or null if it isn't one of strings of a single symbol and spaces. */
    @Nullable
    private static Set<Cell> cells(JsonArray pattern) {
        Set<Cell> cells = new HashSet<>();
        Character symbol = null;
        for (int row = 0; row < pattern.size(); row++) {
            if (!(pattern.get(row) instanceof JsonPrimitive line) || !line.isString()) return null;
            String text = line.getAsString();
            for (int column = 0; column < text.length(); column++) {
                char c = text.charAt(column);
                if (c == ' ') continue;
                if (symbol != null && symbol != c) return null;
                symbol = c;
                cells.add(new Cell(row, column));
            }
        }
        return cells;
    }

    // Reading

    @Nullable
    private static String string(JsonObject object, String member) {
        return object.get(member) instanceof JsonPrimitive primitive && primitive.isString() ? primitive.getAsString() : null;
    }

    @Nullable
    private static Identifier resultId(JsonObject recipe) {
        JsonElement result = recipe.get("result");
        String id = result instanceof JsonObject object ? string(object, "id") : result instanceof JsonPrimitive primitive && primitive.isString() ? primitive.getAsString() : null;
        return id == null ? null : Identifier.tryParse(id);
    }

    private static int count(JsonObject recipe) {
        return recipe.get("result") instanceof JsonObject result && result.get("count") instanceof JsonPrimitive count && count.isNumber() ? count.getAsInt() : 1;
    }

    @Nullable
    private static Block block(Identifier id) {
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        return item instanceof BlockItem blockItem ? blockItem.getBlock() : null;
    }
}
