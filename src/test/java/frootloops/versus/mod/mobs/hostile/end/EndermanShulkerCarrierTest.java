package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.worldgen.TestGame;
import net.minecraft.world.entity.EntitySpawnReason;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Two to four endermen in a hundred, of those that spawn naturally or with the world, carry a shulker. */
class EndermanShulkerCarrierTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    @Test
    void twoToFourInAHundredCarryAShulker() {
        float chance = VersusSettings.Endermen.SHULKER_CARRIER_CHANCE;
        assertTrue(chance >= 0.02F && chance <= 0.04F, "the chance is " + chance);
        assertTrue(EndermanShulkerCarrier.rollsCarrier(0.0F));
        assertTrue(EndermanShulkerCarrier.rollsCarrier(chance * 0.99F));
        assertFalse(EndermanShulkerCarrier.rollsCarrier(chance));
        assertFalse(EndermanShulkerCarrier.rollsCarrier(0.5F));
    }

    @Test
    void onlyThoseThatSpawnNaturallyOrWithTheWorld() {
        assertTrue(EndermanShulkerCarrier.mayCarry(EntitySpawnReason.NATURAL));
        assertTrue(EndermanShulkerCarrier.mayCarry(EntitySpawnReason.CHUNK_GENERATION));
        assertFalse(EndermanShulkerCarrier.mayCarry(EntitySpawnReason.JOCKEY));
        assertFalse(EndermanShulkerCarrier.mayCarry(EntitySpawnReason.MOB_SUMMONED));
    }
}
