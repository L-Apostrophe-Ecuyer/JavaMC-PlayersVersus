package frootloops.versus;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The mod's tag files, and what a tag holds as the game merges them with its own (vanilla's tags read from the game's jar)
 * and follows the tags nested in them: what the tests ask of a tag is what it holds, not which file says so.
 */
public final class TagFiles {

    public static final Path DATA = Path.of("src/main/resources/data");
    /** The registries whose tags the mod has, longest first so that worldgen/biome isn't taken for worldgen. */
    private static final List<String> REGISTRIES = List.of("worldgen/world_preset", "worldgen/structure", "worldgen/biome", "entity_type",
            "damage_type", "enchantment", "block", "item");
    private static Map<String, JsonObject> tags;

    private TagFiles() {
    }

    /** The mod's tag files by namespace:path (path from the tags folder, e.g. block/leaves, worldgen/biome/is_forest). */
    public static synchronized Map<String, JsonObject> mod() {
        if (tags == null) {
            Map<String, JsonObject> read = new LinkedHashMap<>();
            try (Stream<Path> files = Files.walk(DATA)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".json") && f.toString().contains("/tags/")).sorted().toList()) {
                    Path relative = DATA.relativize(file);
                    String namespace = relative.getName(0).toString();
                    String path = relative.subpath(2, relative.getNameCount()).toString().replace('\\', '/').replaceFirst("\\.json$", "");
                    read.put(namespace + ":" + path, json(Files.readString(file)));
                }
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
            tags = Collections.unmodifiableMap(read);
        }
        return tags;
    }

    public static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    /** The game's own tag file, or null. */
    public static JsonObject vanilla(String namespace, String path) {
        try {
            Enumeration<URL> found = TagFiles.class.getClassLoader().getResources("data/" + namespace + "/tags/" + path + ".json");
            while (found.hasMoreElements()) {
                URL url = found.nextElement();
                if (!url.getProtocol().equals("jar")) continue;
                try (InputStream stream = url.openStream()) {
                    return json(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
            return null;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /** The registry of a tag's path (block/leaves: block). */
    public static String registry(String path) {
        return REGISTRIES.stream().filter(registry -> path.startsWith(registry + "/")).findFirst().orElseThrow(() -> new IllegalStateException("no registry for " + path));
    }

    /** A tag file's entries, as written (nested tags with their #). */
    public static List<String> values(JsonObject tag) {
        List<String> values = new ArrayList<>();
        if (tag == null) return values;
        for (JsonElement value : tag.getAsJsonArray("values")) values.add(value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString());
        return values;
    }

    public static boolean replaces(JsonObject tag) {
        return tag != null && tag.has("replace") && tag.get("replace").getAsBoolean();
    }

    /** What a tag (namespace:name, of the registry) holds: vanilla's file and the mod's merged, nested tags followed. */
    public static Set<String> resolve(String id, String registry) {
        return resolve(id, registry, new HashSet<>());
    }

    private static Set<String> resolve(String id, String registry, Set<String> seen) {
        Set<String> held = new LinkedHashSet<>();
        if (!seen.add(id)) return held;
        String namespace = id.substring(0, id.indexOf(':')), path = registry + "/" + id.substring(id.indexOf(':') + 1);
        JsonObject mod = mod().get(namespace + ":" + path);
        List<String> entries = new ArrayList<>(replaces(mod) ? List.of() : values(vanilla(namespace, path)));
        entries.addAll(values(mod));
        for (String entry : entries) {
            if (entry.startsWith("#")) held.addAll(resolve(entry.substring(1), registry, seen));
            else held.add(entry);
        }
        return held;
    }

    /** What the nested tags among these entries hold. */
    public static Set<String> nested(List<String> entries, String registry) {
        Set<String> held = new HashSet<>();
        for (String entry : entries) if (entry.startsWith("#")) held.addAll(resolve(entry.substring(1), registry));
        return held;
    }

    /** Whether the biome tag at this file's path (minecraft/tags/worldgen/biome/is_forest.json) holds the biome. */
    public static boolean biomeTagHolds(String file, String biome) {
        String namespace = file.substring(0, file.indexOf('/'));
        String name = file.substring(file.indexOf("/tags/worldgen/biome/") + "/tags/worldgen/biome/".length()).replaceFirst("\\.json$", "");
        return resolve(namespace + ":" + name, "worldgen/biome").contains(biome.contains(":") ? biome : "players-versus:" + biome);
    }
}
