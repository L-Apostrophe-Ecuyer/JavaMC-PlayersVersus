package frootloops.versus.mod.environment.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.gen.carver.CarverConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.heightprovider.HeightProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Files under {@code data/minecraft/} in this mod replace vanilla's for every world type, not just "Improved". This
 * prints what the carver overrides change (how often a carver starts, and at which heights), next to vanilla's values.
 */
class VanillaOverridesTest {

    @BeforeAll
    static void bootstrap() {
        TestGame.start();
    }

    @Test
    void carverOverridesComparedWithVanilla() throws IOException {
        Map<String, CarverConfig> vanilla = new TreeMap<>();
        BuiltinRegistries.createWrapperLookup().getOrThrow(RegistryKeys.CONFIGURED_CARVER).streamEntries()
                .forEach(entry -> vanilla.put(entry.registryKey().getValue().getPath(), entry.value().config()));
        for (String carver : List.of("cave", "cave_extra_underground", "canyon")) {
            JsonObject override = readConfig(Path.of("src/main/resources/data/minecraft/worldgen/configured_carver/" + carver + ".json"));
            CarverConfig original = vanilla.get(carver);
            assertNotNull(original, "vanilla has no carver " + carver);
            float probability = override.get("probability").getAsFloat();
            System.out.printf(Locale.ROOT, "[overrides] carver minecraft:%s probability vanilla %.4f, this mod %.4f (%.0f%% of vanilla)%n",
                    carver, original.probability, probability, 100.0 * probability / original.probability);
            System.out.printf(Locale.ROOT, "[overrides] carver minecraft:%s y vanilla %s, this mod %s%n", carver,
                    HeightProvider.CODEC.encodeStart(JsonOps.INSTANCE, original.y).getOrThrow(), override.get("y"));
            assertTrue(probability > 0);
        }
    }

    /**
     * The biomes this mod overrides, next to vanilla's: carvers, and features step by step (which ones this mod adds
     * or drops).
     */
    @Test
    void biomeOverridesComparedWithVanilla() throws IOException {
        RegistryWrapper.Impl<Biome> vanilla = BuiltinRegistries.createWrapperLookup().getOrThrow(RegistryKeys.BIOME);
        List<Path> overrides;
        try (Stream<Path> files = Files.list(Path.of("src/main/resources/data/minecraft/worldgen/biome"))) {
            overrides = files.filter(file -> file.toString().endsWith(".json")).sorted().toList();
        }
        assertTrue(!overrides.isEmpty(), "no biome overrides found");
        for (Path file : overrides) {
            String name = file.getFileName().toString().replace(".json", "");
            JsonObject override = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            GenerationSettings original = vanilla.getOrThrow(RegistryKey.of(RegistryKeys.BIOME, Identifier.ofVanilla(name))).value()
                    .getGenerationSettings();
            List<String> originalCarvers = new ArrayList<>();
            original.getCarversForStep().forEach(carver -> originalCarvers.add(id(carver)));
            System.out.printf(Locale.ROOT, "[overrides] biome minecraft:%s carvers vanilla %s, this mod %s%n", name, originalCarvers,
                    strings(override.getAsJsonArray("carvers")));
            JsonArray steps = override.getAsJsonArray("features");
            List<RegistryEntryList<PlacedFeature>> originalSteps = original.getFeatures();
            for (int step = 0; step < Math.max(steps.size(), originalSteps.size()); step++) {
                List<String> before = step < originalSteps.size() ? originalSteps.get(step).stream().map(VanillaOverridesTest::id).toList() : List.of();
                List<String> after = step < steps.size() ? strings(steps.get(step).getAsJsonArray()) : List.of();
                if (before.equals(after)) continue;
                List<String> added = after.stream().filter(feature -> !before.contains(feature)).toList();
                List<String> dropped = before.stream().filter(feature -> !after.contains(feature)).toList();
                System.out.printf(Locale.ROOT, "[overrides] biome minecraft:%s features step %d: adds %s, drops %s%s%n", name, step,
                        added, dropped, added.isEmpty() && dropped.isEmpty() ? " (same features, other order)" : "");
            }
        }
    }

    private static String id(RegistryEntry<?> entry) {
        return entry.getKey().orElseThrow().getValue().toString();
    }

    private static List<String> strings(JsonArray array) {
        List<String> strings = new ArrayList<>();
        array.forEach(element -> strings.add(element.getAsString()));
        return strings;
    }

    /** Reads the mod's copy from the source tree: on the classpath, vanilla's jar may shadow it. */
    private static JsonObject readConfig(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonObject("config");
    }
}
