package frootloops.versus.mod.environment.worldgen;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.carver.CarverConfig;
import net.minecraft.world.gen.heightprovider.HeightProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

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

    /** Reads the mod's copy from the source tree: on the classpath, vanilla's jar may shadow it. */
    private static JsonObject readConfig(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonObject("config");
    }
}
