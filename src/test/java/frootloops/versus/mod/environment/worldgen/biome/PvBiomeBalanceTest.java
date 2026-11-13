package frootloops.versus.mod.environment.worldgen.biome;

import com.mojang.datafixers.util.Pair;
import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The layout's balance in a real world's climate: seed 1234, this mod's noise router evaluated by the game
 * ({@link WorldgenTestData}), a grid every {@value #STEP} blocks over 22.4 km a side. How much of the world each biome takes,
 * and which biomes border which (a border is two grid neighbours of different biomes). The numbers the refinement pass
 * started from (spec docs/superpowers/specs/2026-11-13-refinement-pass-design.md) are in the assertions' messages.
 */
class PvBiomeBalanceTest {

    private static final int STEP = 32, SIDE = 700;
    private static final Map<String, Integer> AREA = new HashMap<>();
    private static final Map<String, Integer> BORDERS = new HashMap<>();
    private static int columns, borderPairs;

    @BeforeAll
    static void sample() {
        RandomState state = WorldgenTestData.randomState(1234L);
        NoiseRouter router = WorldgenTestData.router();
        List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> entries = new ArrayList<>();
        for (PvBiomeLayout.Entry entry : PvBiomeLayout.build()) entries.add(Pair.of(entry.parameters(), entry.biome()));
        Climate.ParameterList<ResourceKey<Biome>> layout = new Climate.ParameterList<>(entries);
        String[][] grid = new String[SIDE][SIDE];
        for (int i = 0; i < SIDE; i++) {
            for (int j = 0; j < SIDE; j++) {
                int x = (i - SIDE / 2) * STEP, z = (j - SIDE / 2) * STEP;
                float t = (float) state.sampleBlockValueUncached(router.temperature(), x, 0, z);
                float h = (float) state.sampleBlockValueUncached(router.vegetation(), x, 0, z);
                float c = (float) state.sampleBlockValueUncached(router.continents(), x, 0, z);
                float e = (float) state.sampleBlockValueUncached(router.erosion(), x, 0, z);
                float w = (float) state.sampleBlockValueUncached(router.ridges(), x, 0, z);
                String biome = layout.findValue(Climate.target(t, h, c, e, 0.0F, w)).identifier().getPath();
                grid[i][j] = biome;
                AREA.merge(biome, 1, Integer::sum);
                columns++;
            }
        }
        for (int i = 0; i < SIDE; i++) {
            for (int j = 0; j < SIDE; j++) {
                if (i + 1 < SIDE) neighbours(grid[i][j], grid[i + 1][j]);
                if (j + 1 < SIDE) neighbours(grid[i][j], grid[i][j + 1]);
            }
        }
        StringBuilder report = new StringBuilder("[balance] shares of the world:");
        AREA.entrySet().stream().filter(entry -> !entry.getKey().contains("ocean")).sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(entry -> report.append(String.format(Locale.ROOT, " %s %.2f%%;", entry.getKey(), 100.0 * entry.getValue() / columns)));
        System.out.println(report);
    }

    private static void neighbours(String one, String two) {
        if (one.equals(two)) return;
        borderPairs++;
        BORDERS.merge(one.compareTo(two) < 0 ? one + "|" + two : two + "|" + one, 1, Integer::sum);
    }

    /** The biome's share of all the columns, in percent. */
    private static double share(String biome) {
        return 100.0 * AREA.getOrDefault(biome, 0) / columns;
    }

    /** The two biomes' share of all the borders, in percent. */
    private static double border(String one, String two) {
        return 100.0 * BORDERS.getOrDefault(one.compareTo(two) < 0 ? one + "|" + two : two + "|" + one, 0) / borderPairs;
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.3f%%", value);
    }

    @Test
    void thePlainDarkForestKeepsMostOfItsFamily() {
        double plain = share("dark_forest"), family = plain + share("dark_birch_forest") + share("warm_dark_forest") + share("dark_taiga_forest");
        assertTrue(plain >= 0.4 * family, "the plain dark forest is " + percent(plain) + " of the world, of its family's " + percent(family)
                + " (the pass started from 0.28% of 2.14%)");
    }

    @Test
    void theGrassyWetlandsAndTheSwampRimAreFoundNearby() {
        assertTrue(share("grassy_wetlands") >= 0.15, "the grassy wetlands are " + percent(share("grassy_wetlands")) + " of the world (from 0.05%)");
        assertTrue(share("humid_oak_forest") >= 0.06, "the humid oak forest is " + percent(share("humid_oak_forest")) + " of the world (from 0.03%)");
    }

    @Test
    void noJungleBordersAPlainForestOrPlains() {
        double plain = 0;
        for (String jungle : List.of("jungle", "sparse_jungle", "bamboo_jungle")) plain += border("forest", jungle) + border("plains", jungle);
        assertTrue(plain <= 0.01, "jungles border plain forest and plains for " + percent(plain) + " of all borders (from 1.08%)");
    }

    @Test
    void forestAndPlainsMeetThroughTheForestEdge() {
        double direct = border("forest", "plains"), through = border("forest", "forest_edge") + border("plains", "forest_edge");
        assertTrue(direct <= 0.15 * (direct + through), "forest and plains border directly for " + percent(direct) + " of all borders, through the forest edge "
                + percent(through) + " (they were 3.76% directly)");
    }

    @Test
    void forestAndTaigaMeetThroughTheMixedForest() {
        double direct = border("forest", "taiga"), through = border("forest", "mixed_forest") + border("taiga", "mixed_forest");
        assertTrue(direct <= 0.15 * (direct + through), "forest and taiga border directly for " + percent(direct) + " of all borders, through the mixed forest "
                + percent(through) + " (they were 1.05% directly)");
    }

    @Test
    void theNewBiomesAreFoundAndRareOnesTooRare() {
        for (String biome : List.of("forest_edge", "mixed_forest", "old_oak_forest", "prairie")) {
            assertTrue(share(biome) >= 0.2, biome + " is " + percent(share(biome)) + " of the world");
        }
        for (String biome : List.of("pale_taiga", "pale_swamp")) {
            assertTrue(share(biome) > 0.003, biome + " is " + percent(share(biome)) + " of the world");
        }
        // The rocky snowy plains: found, and a part of the snowy plains only
        double rocky = share("rocky_snowy_plains"), snowy = share("snowy_plains") + rocky;
        assertTrue(rocky >= 0.1 && rocky <= 0.5 * snowy, "the rocky snowy plains are " + percent(rocky) + " of the world, of the snowy plains' " + percent(snowy));
        // Sub-biomes stay pockets: the forest and the plains keep most of their climate
        assertTrue(share("old_oak_forest") <= 0.25 * share("forest"), "the old oak forest " + percent(share("old_oak_forest")) + " against the forest's " + percent(share("forest")));
        assertTrue(share("prairie") <= 0.25 * share("plains"), "the prairie " + percent(share("prairie")) + " against the plains' " + percent(share("plains")));
    }
}
