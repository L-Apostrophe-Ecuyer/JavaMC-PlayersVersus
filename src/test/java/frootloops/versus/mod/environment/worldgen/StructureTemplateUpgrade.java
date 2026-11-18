package frootloops.versus.mod.environment.worldgen;

import com.mojang.datafixers.DataFixer;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Re-saves every structure template the mod ships at the game's data version, data-fixed as the game fixes one when it
 * loads it ({@code TemplateSource}: the structure's data fix, then the current data version stamped on, as saving does).
 * StructureTemplatesTest says why the templates must be current. A tool, not a test: it runs only when asked,
 *
 *     PV_UPGRADE_TEMPLATES=true ./gradlew test --tests '*StructureTemplateUpgrade'
 *
 * and rewrites the templates under src/main/resources/data that are older than the game.
 */
@EnabledIfEnvironmentVariable(named = "PV_UPGRADE_TEMPLATES", matches = "true")
class StructureTemplateUpgrade {

    @Test
    void upgradeTheTemplates() throws IOException {
        TestGame.start();
        DataFixer fixer = DataFixers.getDataFixer();
        int current = SharedConstants.getCurrentVersion().dataVersion().version();
        int upgraded = 0;
        try (Stream<Path> files = Files.walk(Path.of("src/main/resources/data"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".nbt")).toList()) {
                CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                int version = NbtUtils.getDataVersion(tag, 500);
                if (version >= current) continue;
                CompoundTag fixed = NbtUtils.addCurrentDataVersion(DataFixTypes.STRUCTURE.updateToCurrentVersion(fixer, tag, version));
                NbtIo.writeCompressed(fixed, file);
                upgraded++;
            }
        }
        System.out.println("[templates] " + upgraded + " upgraded to data version " + current);
    }
}
