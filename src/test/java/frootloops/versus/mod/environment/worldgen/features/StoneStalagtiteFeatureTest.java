package frootloops.versus.mod.environment.worldgen.features;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoneStalagtiteFeatureTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    private static StoneStalagtiteFeature decode(String name) throws IOException {
        try (Reader reader = Files.newBufferedReader(Path.of("src/main/resources/data/players-versus/worldgen/feature", name + ".json"))) {
            return StoneStalagtiteFeature.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
        }
    }

    /**
     * The Pale Grotto's pillars, as the data sets them: across a 30-block gap they are stone or tuff at the floor and
     * ceiling and only pale oak wood around the middle, with tuff on the way. Each height draws its block many times.
     */
    @Test
    void grottoPillarsGoFromStoneToWoodInTheMiddle() throws IOException {
        StoneStalagtiteFeature grotto = decode("caves/pale_grotto_pillars");
        float wood = grotto.paleOakWoodReach(), tuff = grotto.tuffReach(), blend = StoneStalagtiteFeature.GRADIENT_BLEND;
        int floor = 0, ceiling = 31;
        double middle = (floor + ceiling) / 2.0, half = (ceiling - floor) / 2.0;
        StoneStalagtiteFeature.Gradient gradient = new StoneStalagtiteFeature.Gradient(middle, half, wood, tuff);
        RandomSource random = RandomSource.create(42L);
        boolean sawTuff = false;
        for (int y = floor + 1; y < ceiling; ++y) {
            double distance = Math.abs(y - middle) / half;
            for (int draw = 0; draw < 64; ++draw) {
                Block block = gradient.blockAt(y, random).getBlock();
                sawTuff |= block == Blocks.TUFF;
                if (y == 15 || y == 16 || distance < wood - blend) assertEquals(Blocks.PALE_OAK_WOOD, block, "middle at y " + y);
                if (y == floor + 1 || y == ceiling - 1) assertTrue(block == Blocks.STONE || block == Blocks.TUFF, "end at y " + y);
                if (distance > wood + blend) assertNotEquals(Blocks.PALE_OAK_WOOD, block, "y " + y);
                if (distance > tuff + blend) assertEquals(Blocks.STONE, block, "y " + y);
            }
        }
        assertTrue(sawTuff, "no tuff between the stone and the wood");
        assertTrue(grotto.creakingHeartChance() > 0.0f, "the grotto's pillars have no hearts");
    }

    /** The other caves' pillars set neither reach nor hearts, so they stay plain stone. */
    @Test
    void otherPillarsStayStone() throws IOException {
        for (String name : new String[]{"stone_stalagtite", "stone_overhangs_pillars_vegetation"}) {
            StoneStalagtiteFeature feature = decode(name);
            assertEquals(0.0f, feature.creakingHeartChance(), name);
            StoneStalagtiteFeature.Gradient gradient = new StoneStalagtiteFeature.Gradient(10.0, 10.0, feature.paleOakWoodReach(), feature.tuffReach());
            for (int y = 0; y <= 20; ++y) {
                assertEquals(Blocks.STONE, gradient.blockAt(y, RandomSource.create(y)).getBlock(), name + " at y " + y);
            }
        }
    }

    /** The heart goes in the centre column as near the middle as it has pale oak wood right above and below. */
    @Test
    void theHeartSitsInTheMiddleBetweenWood() {
        assertEquals(OptionalInt.of(15), StoneStalagtiteFeature.heartY(15.0, 5.0, y -> y >= 10 && y <= 20));
        assertEquals(OptionalInt.of(15), StoneStalagtiteFeature.heartY(15.5, 5.0, y -> y >= 10 && y <= 20));
        // Tuff at the middle: the nearest height with wood on both sides.
        assertEquals(OptionalInt.of(17), StoneStalagtiteFeature.heartY(15.0, 5.0, y -> y >= 10 && y <= 20 && y != 15));
        // A stalactite and a stalagmite that don't meet: the nearer tip that's long enough.
        assertEquals(OptionalInt.of(11), StoneStalagtiteFeature.heartY(15.0, 7.5, y -> y <= 12 || y >= 19));
        // None where the wood is too thin, or only further from the middle than the reach.
        assertEquals(OptionalInt.empty(), StoneStalagtiteFeature.heartY(15.0, 5.0, y -> y == 15 || y == 16));
        assertEquals(OptionalInt.empty(), StoneStalagtiteFeature.heartY(15.0, 3.0, y -> y <= 11));
    }
}
