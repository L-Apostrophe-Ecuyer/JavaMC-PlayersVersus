package frootloops.versus.mod.environment.worldgen.features;

import frootloops.versus.mod.environment.worldgen.TestGame;
import frootloops.versus.mod.environment.worldgen.features.MarshFeature.Ground;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarshFeatureTest {

    @BeforeAll
    static void bootstrap() {
        TestGame.start();
    }

    /** From wet to dry the rings come in order, each once: channel, gray mud, brown mud, edge, grass. */
    @Test
    void theRingsFollowTheDrynessInOrder() {
        Ground previous = Ground.CHANNEL;
        for (float dryness = -1.0F; dryness <= 1.0F; dryness += 0.001F) {
            Ground ground = MarshFeature.ground(dryness);
            assertTrue(ground.ordinal() >= previous.ordinal(), "the rings went back from " + previous + " to " + ground + " at " + dryness);
            assertTrue(ground.ordinal() <= previous.ordinal() + 1, "a ring was skipped from " + previous + " to " + ground + " at " + dryness);
            previous = ground;
        }
        assertEquals(Ground.GRASS, previous, "the driest ground isn't grass");
    }

    /** Channels lie under the water (two deep where wettest), land is flush with it, hummocks a block higher. */
    @Test
    void islandsAndChannelsSitAgainstTheWaterLine() {
        int water = 62;
        assertEquals(water - 2, MarshFeature.surface(MarshFeature.DEEP - 0.01F, water));
        assertEquals(water - 1, MarshFeature.surface(MarshFeature.LAND - 0.01F, water));
        assertEquals(water, MarshFeature.surface(MarshFeature.LAND, water));
        assertEquals(water + 1, MarshFeature.surface(MarshFeature.HUMMOCK, water));
        for (float dryness = -1.0F; dryness < 1.0F; dryness += 0.01F) {
            boolean channel = MarshFeature.ground(dryness) == Ground.CHANNEL;
            assertEquals(channel, MarshFeature.surface(dryness, water) < water, "land and water disagree at " + dryness);
        }
    }

    /** The noise makes islands and channels both, with every ring, over a stretch of ground. */
    @Test
    void theNoiseMakesEveryRingOverAStretchOfMarsh() {
        Map<Ground, Integer> found = new EnumMap<>(Ground.class);
        for (int x = 0; x < 256; x++) for (int z = 0; z < 256; z++) found.merge(MarshFeature.ground(MarshFeature.noise(8675309L, x, z)), 1, Integer::sum);
        for (Ground ground : Ground.values()) assertTrue(found.getOrDefault(ground, 0) > 256 * 256 / 50, ground + " is too rare: " + found);
        System.out.println("[marsh] rings over 256 x 256: " + found);
    }
}
