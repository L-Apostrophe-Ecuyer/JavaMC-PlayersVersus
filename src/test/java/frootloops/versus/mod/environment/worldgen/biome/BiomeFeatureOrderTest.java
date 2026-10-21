package frootloops.versus.mod.environment.worldgen.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The biomes agree on the order of their features. The game chains each biome's features, step by step, and refuses
 * to generate a world when two biomes disagree on which of two features comes first (FeatureSorter's "Feature order
 * cycle found", at the first chunk). Checked over vanilla's biomes from the game's jar, with the mod's in place of
 * those it overrides, and the mod's own: when vanilla moves a feature (26.4-snapshot-4 put ore_gravel after the
 * granite ores), the mod's biomes have to follow.
 */
class BiomeFeatureOrderTest {

    private static final String BIOMES = "worldgen/biome";
    private static final Path MOD_DATA = Path.of("src/main/resources/data");

    @Test
    void theBiomesAgreeOnTheOrderOfTheirFeatures() throws IOException, URISyntaxException {
        Map<String, JsonArray> biomes = new TreeMap<>();
        URL plains = BiomeFeatureOrderTest.class.getClassLoader().getResource("data/minecraft/" + BIOMES + "/plains.json");
        assertNotNull(plains, "the game's biomes are not on the test classpath");
        URI uri = plains.toURI();
        if ("jar".equals(uri.getScheme())) {
            try {
                FileSystems.newFileSystem(uri, Map.of());
            } catch (FileSystemAlreadyExistsException alreadyOpen) {
                // Opened by an earlier test: Path.of finds it
            }
        }
        int vanilla = read(Path.of(uri).getParent(), "minecraft", biomes);
        assertTrue(vanilla > 50, "only " + vanilla + " vanilla biomes found");
        try (Stream<Path> namespaces = Files.list(MOD_DATA)) {
            for (Path namespace : namespaces.toList()) {
                Path dir = namespace.resolve(BIOMES);
                if (Files.isDirectory(dir)) read(dir, namespace.getFileName().toString(), biomes);
            }
        }

        // Each biome's features in one chain, step after step, as FeatureSorter links them; the first biome to link two
        // features is kept to name in a failure.
        Map<String, Map<String, String>> next = new HashMap<>();
        for (Map.Entry<String, JsonArray> biome : biomes.entrySet()) {
            List<String> chain = new ArrayList<>();
            JsonArray steps = biome.getValue();
            for (int step = 0; step < steps.size(); step++) {
                JsonElement features = steps.get(step);
                if (features.isJsonArray()) for (JsonElement feature : features.getAsJsonArray()) chain.add(step + " " + feature.getAsString());
                else chain.add(step + " " + features.getAsString());
            }
            for (int i = 1; i < chain.size(); i++) next.computeIfAbsent(chain.get(i - 1), key -> new LinkedHashMap<>()).putIfAbsent(chain.get(i), biome.getKey());
        }
        Map<String, Integer> state = new HashMap<>();
        for (String feature : next.keySet()) {
            List<String> cycle = cycleFrom(feature, next, state, new ArrayList<>());
            if (cycle != null) {
                StringBuilder message = new StringBuilder("feature order cycle (step feature, then the biome that orders it before the next):");
                for (int i = 0; i + 1 < cycle.size(); i++) message.append("\n  ").append(cycle.get(i)).append("  [").append(next.get(cycle.get(i)).get(cycle.get(i + 1))).append(']');
                message.append("\n  ").append(cycle.getLast());
                fail(message.toString());
            }
        }
    }

    /** Reads every biome under this directory into the map, by id, in place of any read before; returns how many. */
    private static int read(Path dir, String namespace, Map<String, JsonArray> biomes) throws IOException {
        int count = 0;
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                String name = dir.relativize(file).toString().replace('\\', '/');
                JsonElement features = JsonParser.parseString(Files.readString(file)).getAsJsonObject().get("features");
                biomes.put(namespace + ":" + name.substring(0, name.length() - ".json".length()), features != null ? features.getAsJsonArray() : new JsonArray());
                count++;
            }
        }
        return count;
    }

    /** A cycle through this feature's successors, from its first repeat to the repeat, or null; state 1 is on the path, 2 done. */
    private static List<String> cycleFrom(String feature, Map<String, Map<String, String>> next, Map<String, Integer> state, List<String> path) {
        Integer seen = state.get(feature);
        if (seen != null && seen == 2) return null;
        path.add(feature);
        if (seen != null) return new ArrayList<>(path.subList(path.indexOf(feature), path.size()));
        state.put(feature, 1);
        for (String after : next.getOrDefault(feature, Map.of()).keySet()) {
            List<String> cycle = cycleFrom(after, next, state, path);
            if (cycle != null) return cycle;
        }
        state.put(feature, 2);
        path.removeLast();
        return null;
    }
}
