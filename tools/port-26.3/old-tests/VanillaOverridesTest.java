package frootloops.versus.mod.environment.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
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
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Files under {@code data/minecraft/} in this mod replace vanilla's for every world type, not just "Improved". Carvers
 * are Improved-only (Section 10, question 1): no vanilla carver is overridden, and the overridden biomes keep vanilla's
 * carver lists. This prints the Improved carvers' tuning next to vanilla's, and what the biome overrides still change.
 */
class VanillaOverridesTest {

    @BeforeAll
    static void bootstrap() {
        TestGame.start();
    }

    @Test
    void improvedCarversComparedWithVanilla() throws IOException {
        assertFalse(Files.exists(Path.of("src/main/resources/data/minecraft/worldgen/configured_carver")),
                "vanilla carvers are overridden again; Improved's own are under players-versus");
        Map<String, CarverConfiguration> vanilla = new TreeMap<>();
        VanillaRegistries.createLookup().lookupOrThrow(Registries.CONFIGURED_CARVER).listElements()
                .forEach(entry -> vanilla.put(entry.key().location().getPath(), entry.value().config()));
        for (String carver : List.of("cave", "cave_extra_underground", "canyon")) {
            JsonObject improved = readConfig(Path.of("src/main/resources/data/players-versus/worldgen/configured_carver/" + carver + ".json"));
            CarverConfiguration original = vanilla.get(carver);
            assertNotNull(original, "vanilla has no carver " + carver);
            float probability = improved.get("probability").getAsFloat();
            System.out.printf(Locale.ROOT, "[overrides] carver %s probability vanilla %.4f, Improved %.4f (%.0f%% of vanilla)%n",
                    carver, original.probability, probability, 100.0 * probability / original.probability);
            System.out.printf(Locale.ROOT, "[overrides] carver %s y vanilla %s, Improved %s%n", carver,
                    HeightProvider.CODEC.encodeStart(JsonOps.INSTANCE, original.y).getOrThrow(), improved.get("y"));
            assertTrue(probability > 0);
        }
    }

    /**
     * The biomes this mod overrides, next to vanilla's: the same carvers, and features step by step (which ones this
     * mod adds or drops).
     */
    @Test
    void biomeOverridesComparedWithVanilla() throws IOException {
        HolderLookup.RegistryLookup<Biome> vanilla = VanillaRegistries.createLookup().lookupOrThrow(Registries.BIOME);
        List<Path> overrides;
        try (Stream<Path> files = Files.list(Path.of("src/main/resources/data/minecraft/worldgen/biome"))) {
            overrides = files.filter(file -> file.toString().endsWith(".json")).sorted().toList();
        }
        assertTrue(!overrides.isEmpty(), "no biome overrides found");
        for (Path file : overrides) {
            String name = file.getFileName().toString().replace(".json", "");
            JsonObject override = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            BiomeGenerationSettings original = vanilla.getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.withDefaultNamespace(name))).value()
                    .getGenerationSettings();
            List<String> originalCarvers = new ArrayList<>();
            original.getCarvers().forEach(carver -> originalCarvers.add(id(carver)));
            assertEquals(originalCarvers, strings(override.getAsJsonArray("carvers")), "minecraft:" + name + " overrides vanilla's carvers");
            JsonArray steps = override.getAsJsonArray("features");
            List<HolderSet<PlacedFeature>> originalSteps = original.features();
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

    private static String id(Holder<?> entry) {
        return entry.unwrapKey().orElseThrow().location().toString();
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
