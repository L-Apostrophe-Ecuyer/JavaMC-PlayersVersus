package frootloops.versus.mod.environment.worldgen;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Improved worlds' carvers ({@link PvCarvers}) against what those worlds carved with before carvers became
 * Improved-only: vanilla's three under this mod's tuning (now {@code players-versus:cave} and so on), and the deep
 * dark's own list. Carvers keep their places in each list, since a carver's seed comes from its index.
 */
class PvCarversTest {

    private static final Path DATA = Path.of("src/main/resources/data");
    private static final Identifier DEEP_DARK = Identifier.withDefaultNamespace("deep_dark");

    @BeforeAll
    static void bootstrap() {
        TestGame.start();
    }

    @Test
    void vanillaBiomesCarveWithTheImprovedCopies() {
        HolderLookup.RegistryLookup<Biome> biomes = VanillaRegistries.createLookup().lookupOrThrow(Registries.BIOME);
        Map<String, Integer> replaced = new TreeMap<>();
        biomes.listElements().forEach(entry -> {
            Identifier biome = entry.key().location();
            List<Identifier> vanilla = new ArrayList<>();
            entry.value().getGenerationSettings().getCarvers()
                    .forEach(carver -> vanilla.add(carver.unwrapKey().orElseThrow().location()));
            List<Identifier> improved = PvCarvers.improved(biome, vanilla);
            if (biome.equals(DEEP_DARK)) {
                // its override listed minecraft:cave (under this mod's tuning) and players-versus:deep_dark_canyon
                assertEquals(List.of(id("cave"), id("deep_dark_canyon")), improved);
                return;
            }
            assertEquals(vanilla.size(), improved.size(), biome + ": carvers added or dropped");
            for (int i = 0; i < vanilla.size(); i++) {
                Identifier expected = PvCarvers.COPIES.getOrDefault(vanilla.get(i), vanilla.get(i));
                assertEquals(expected, improved.get(i), biome + ": carver " + i);
                if (!expected.equals(vanilla.get(i))) replaced.merge(vanilla.get(i).toString(), 1, Integer::sum);
            }
        });
        System.out.println("[carvers] vanilla carvers replaced in Improved worlds, by biomes naming them: " + replaced);
        assertTrue(replaced.size() == PvCarvers.COPIES.size(), "some vanilla carver is named by no biome: " + replaced);
    }

    /**
     * The mod's biome files, its own and its vanilla overrides: Improved worlds keep the lists they had, with the tuned
     * carvers under their new names. Every carver named anywhere exists.
     */
    @Test
    void theModsBiomesKeepTheirCarvers() throws IOException {
        int biomes = 0;
        for (String namespace : List.of("players-versus", "minecraft")) {
            Path folder = DATA.resolve(namespace).resolve("worldgen/biome");
            List<Path> files;
            try (Stream<Path> walk = Files.walk(folder)) {
                files = walk.filter(file -> file.toString().endsWith(".json")).sorted().toList();
            }
            for (Path file : files) {
                String path = folder.relativize(file).toString().replace('\\', '/').replace(".json", "");
                Identifier biome = Identifier.fromNamespaceAndPath(namespace, path);
                List<Identifier> carvers = new ArrayList<>();
                JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonArray("carvers")
                        .forEach(carver -> carvers.add(Identifier.parse(carver.getAsString())));
                List<Identifier> improved = PvCarvers.improved(biome, carvers);
                if (namespace.equals("players-versus")) {
                    // Improved worlds only: the file names the carvers Improved worlds use
                    assertEquals(carvers, improved, biome + " names carvers Improved worlds replace");
                    carvers.forEach(carver -> assertEquals("players-versus", carver.getNamespace(), biome + " names " + carver));
                }
                for (Identifier carver : improved) {
                    if (carver.getNamespace().equals("players-versus")) {
                        Path json = DATA.resolve("players-versus/worldgen/configured_carver/" + carver.getPath() + ".json");
                        assertTrue(Files.isRegularFile(json), biome + " carves with " + carver + ", which doesn't exist");
                    }
                }
                biomes++;
            }
        }
        System.out.println("[carvers] " + biomes + " biome files checked");
        assertTrue(biomes > 20, "only " + biomes + " biome files found");
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("players-versus", path);
    }
}
