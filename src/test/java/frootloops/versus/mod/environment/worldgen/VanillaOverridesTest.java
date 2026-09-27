package frootloops.versus.mod.environment.worldgen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryKeys;
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
 * prints what the carver overrides change, next to vanilla's own values.
 */
class VanillaOverridesTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @Test
    void carverOverridesComparedWithVanilla() throws IOException {
        Map<String, Float> vanilla = new TreeMap<>();
        BuiltinRegistries.createWrapperLookup().getWrapperOrThrow(RegistryKeys.CONFIGURED_CARVER).streamEntries()
                .forEach(entry -> vanilla.put(entry.registryKey().getValue().getPath(), entry.value().config().probability));
        for (String carver : List.of("cave", "cave_extra_underground", "canyon")) {
            float override = readProbability(Path.of("src/main/resources/data/minecraft/worldgen/configured_carver/" + carver + ".json"));
            Float original = vanilla.get(carver);
            assertNotNull(original, "vanilla has no carver " + carver);
            System.out.printf(Locale.ROOT, "[overrides] carver minecraft:%s probability vanilla %.4f, this mod %.4f (%.0f%% of vanilla)%n",
                    carver, original, override, 100.0 * override / original);
            assertTrue(override > 0);
        }
    }

    /** Reads the mod's copy from the source tree: on the classpath, vanilla's jar may shadow it. */
    private static float readProbability(Path file) throws IOException {
        JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        return json.getAsJsonObject("config").get("probability").getAsFloat();
    }
}
