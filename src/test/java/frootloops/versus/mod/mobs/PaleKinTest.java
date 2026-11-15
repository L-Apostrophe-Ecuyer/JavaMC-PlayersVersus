package frootloops.versus.mod.mobs;

import frootloops.versus.mod.environment.PaleLine;
import frootloops.versus.mod.environment.worldgen.TestGame;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where a natural spawn is its pale kin: below the pale line, in the overworld only. */
class PaleKinTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    @Test
    void belowThePaleLineInTheOverworld() {
        assertTrue(PaleKin.pale(Level.OVERWORLD, PaleLine.Y - 1), "just below the pale line");
        assertTrue(PaleKin.pale(Level.OVERWORLD, -60), "down by the bedrock");
        assertFalse(PaleKin.pale(Level.OVERWORLD, PaleLine.Y), "on the pale line");
        assertFalse(PaleKin.pale(Level.OVERWORLD, 64), "up at the sea");
    }

    @Test
    void notInTheNetherNorTheEnd() {
        assertFalse(PaleKin.pale(Level.NETHER, PaleLine.Y - 1), "the nether");
        assertFalse(PaleKin.pale(Level.END, PaleLine.Y - 1), "the end");
    }
}
