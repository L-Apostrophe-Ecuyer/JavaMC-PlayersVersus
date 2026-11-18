package frootloops.versus.mod.environment.worldgen;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every structure template the mod ships is saved at the game's own data version, as vanilla's are, so the game never
 * data-fixes one while it generates a world. Since 26.4 structure starts are made on several threads at once, and the
 * data fixer builds its rules lazily in a map that fails ("Recursive update") when two templates are fixed at the same
 * time: the structure start was lost and the world's creation hung. When the game's version moves, re-save the templates
 * with StructureTemplateUpgrade.
 */
class StructureTemplatesTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    @Test
    void everyTemplateIsAtTheGamesDataVersion() throws IOException {
        int current = SharedConstants.getCurrentVersion().dataVersion().version();
        List<String> old = new ArrayList<>();
        int templates = 0;
        try (Stream<Path> files = Files.walk(Path.of("src/main/resources/data"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".nbt")).toList()) {
                templates++;
                CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                int version = NbtUtils.getDataVersion(tag, 500);
                if (version != current) old.add(file + " (" + version + ")");
            }
        }
        assertTrue(templates > 100, "only " + templates + " templates found");
        assertTrue(old.isEmpty(), old.size() + " of " + templates + " templates aren't at data version " + current + ", e.g. " + old.subList(0, Math.min(3, old.size())));
    }
}
