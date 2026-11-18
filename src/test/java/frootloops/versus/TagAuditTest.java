package frootloops.versus;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mod's tags, every file of them, as the game would merge them with its own (vanilla's tags read from the game's jar):
 * no entry said twice, in a file, by vanilla's own file, or by a tag nested in either; nothing named that the mod
 * doesn't have; the climate variants as tools/animals.py has them; natural ground the overworld's own ground; every
 * item tag of the mod's named, as Fabric asks.
 */
class TagAuditTest {

    private static final Path DATA = TagFiles.DATA, ASSETS = Path.of("src/main/resources/assets");
    private static final String MOD = "players-versus";
    private static final Map<String, JsonObject> TAGS = TagFiles.mod();

    private static JsonObject json(String text) {
        return TagFiles.json(text);
    }

    private static JsonObject vanilla(String namespace, String path) {
        return TagFiles.vanilla(namespace, path);
    }

    private static String registry(String path) {
        return TagFiles.registry(path);
    }

    private static List<String> values(JsonObject tag) {
        return TagFiles.values(tag);
    }

    private static boolean replaces(JsonObject tag) {
        return TagFiles.replaces(tag);
    }

    private static Set<String> resolve(String id, String registry) {
        return TagFiles.resolve(id, registry);
    }

    private static Set<String> nested(List<String> entries, String registry) {
        return TagFiles.nested(entries, registry);
    }

    @Test
    void noTagListsAnEntryTwice() {
        List<String> twice = new ArrayList<>();
        TAGS.forEach((id, tag) -> {
            Set<String> seen = new HashSet<>();
            for (String value : values(tag)) if (!seen.add(value)) twice.add(id + ": " + value);
        });
        assertTrue(twice.isEmpty(), "entries listed twice: " + twice);
    }

    @Test
    void everyEntryNamesSomethingThatExists() throws IOException {
        JsonObject names = json(Files.readString(ASSETS.resolve(MOD + "/lang/en_us.json")));
        List<String> missing = new ArrayList<>();
        TAGS.forEach((id, tag) -> {
            String path = id.substring(id.indexOf(':') + 1), registry = registry(path);
            for (String value : values(tag)) {
                if (value.startsWith("#")) {
                    String nested = value.substring(1), namespace = nested.substring(0, nested.indexOf(':'));
                    String nestedPath = registry + "/" + nested.substring(nested.indexOf(':') + 1);
                    if (!TAGS.containsKey(namespace + ":" + nestedPath) && vanilla(namespace, nestedPath) == null) missing.add(id + ": " + value);
                    continue;
                }
                if (!value.startsWith(MOD + ":")) continue;
                String name = value.substring(MOD.length() + 1);
                boolean exists = switch (registry) {
                    case "block" -> Files.exists(ASSETS.resolve(MOD + "/blockstates/" + name + ".json"));
                    case "item" -> Files.exists(ASSETS.resolve(MOD + "/items/" + name + ".json"));
                    case "entity_type" -> names.has("entity." + MOD + "." + name);
                    default -> Files.exists(DATA.resolve(MOD + "/" + registry + "/" + name + ".json"));
                };
                if (!exists) missing.add(id + ": " + value);
            }
        });
        assertTrue(missing.isEmpty(), "entries naming nothing the mod has: " + missing);
    }

    @Test
    void noTagRepeatsWhatItsTagsAlreadyHold() {
        List<String> repeated = new ArrayList<>();
        TAGS.forEach((id, tag) -> {
            String namespace = id.substring(0, id.indexOf(':')), path = id.substring(id.indexOf(':') + 1), registry = registry(path);
            List<String> entries = values(tag);
            JsonObject vanilla = replaces(tag) ? null : vanilla(namespace, path);
            Set<String> held = new HashSet<>(values(vanilla));
            held.addAll(nested(values(vanilla), registry));
            held.addAll(nested(entries, registry));
            for (String entry : entries) if (!entry.startsWith("#") && held.contains(entry)) repeated.add(id + ": " + entry);
        });
        assertTrue(repeated.isEmpty(), repeated.size() + " entries the tag holds already (vanilla's own file, or a tag nested in it): " + repeated);
    }

    /** A list of biome names in tools/animals.py, as players-versus ids. */
    private static Set<String> animalsTable(String name) throws IOException {
        Matcher list = Pattern.compile("^" + name + " = \\[(.*?)]", Pattern.MULTILINE | Pattern.DOTALL).matcher(Files.readString(Path.of("tools/animals.py")));
        assertTrue(list.find(), "tools/animals.py has no " + name);
        Set<String> biomes = new TreeSet<>();
        Matcher quoted = Pattern.compile("\"([a-z_]+)\"").matcher(list.group(1));
        while (quoted.find()) biomes.add(MOD + ":" + quoted.group(1));
        return biomes;
    }

    @Test
    void theClimateVariantsAreTheAnimalsTables() throws IOException {
        Map<String, String> tables = Map.of("spawns_warm_variant_farm_animals", "WARM", "spawns_warm_variant_frogs", "WARM",
                "spawns_cold_variant_farm_animals", "COLD", "spawns_cold_variant_frogs", "COLD_FROGS");
        for (Map.Entry<String, String> variant : tables.entrySet()) {
            String path = "worldgen/biome/" + variant.getKey();
            // The table's biomes, but those vanilla's own tag holds already (a jungle by #is_jungle, say)
            JsonObject vanilla = vanilla("minecraft", path);
            Set<String> held = new HashSet<>(values(vanilla));
            held.addAll(nested(values(vanilla), "worldgen/biome"));
            Set<String> expected = new TreeSet<>(animalsTable(variant.getValue()));
            expected.removeAll(held);
            Set<String> listed = new TreeSet<>(values(TAGS.get("minecraft:" + path)));
            listed.removeIf(entry -> !entry.startsWith(MOD + ":"));
            assertEquals(expected, listed, variant.getKey() + " isn't tools/animals.py's " + variant.getValue());
        }
    }

    @Test
    void naturalGroundIsTheOverworldsOwnGround() {
        Set<String> ground = resolve(MOD + ":natural_ground", "block");
        List<String> missing = new ArrayList<>();
        for (String block : List.of("stone", "deepslate", "dirt", "sand", "gravel", "coal_ore", "deepslate_coal_ore", "iron_ore", "copper_ore",
                "gold_ore", "redstone_ore", "lapis_ore", "diamond_ore", "deepslate_diamond_ore", "emerald_ore", "infested_stone", "infested_deepslate",
                "raw_iron_block", "raw_copper_block", "suspicious_gravel", "blue_ice", "sulfur", "cinnabar")) {
            if (!ground.contains("minecraft:" + block)) missing.add(block);
        }
        assertTrue(missing.isEmpty(), "natural ground misses " + missing);
        for (String built : List.of("cobblestone", "stone_bricks", "polished_andesite", "oak_planks")) {
            assertTrue(!ground.contains("minecraft:" + built), "natural ground takes in " + built);
        }
    }

    @Test
    void everyItemTagHasAName() throws IOException {
        for (String language : List.of("en_us", "fr_fr")) {
            JsonObject names = json(Files.readString(ASSETS.resolve(MOD + "/lang/" + language + ".json")));
            List<String> unnamed = new ArrayList<>();
            for (String id : TAGS.keySet()) {
                if (!id.startsWith(MOD + ":item/")) continue;
                String key = "tag.item." + MOD + "." + id.substring((MOD + ":item/").length()).replace('/', '.');
                if (!names.has(key)) unnamed.add(key);
            }
            assertTrue(unnamed.isEmpty(), language + " names no " + unnamed);
        }
    }
}
