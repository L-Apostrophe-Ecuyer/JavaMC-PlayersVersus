package frootloops.versus.mod.items_and_effects.conversion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What a villager turns into where paleness holds: the illager of its trade. */
class PaleVillagersTest {

    @Test
    void smithsButchersMasonsAndLeatherworkersTakeUpTheAxe() {
        for (String trade : new String[]{"armorer", "weaponsmith", "toolsmith", "butcher", "mason", "leatherworker"}) {
            assertEquals(PaleVillagers.Illager.VINDICATOR, PaleVillagers.illagerFor(trade), trade);
        }
    }

    @Test
    void theLearnedTurnToIllusions() {
        for (String trade : new String[]{"cleric", "librarian", "cartographer"}) {
            assertEquals(PaleVillagers.Illager.ILLUSIONER, PaleVillagers.illagerFor(trade), trade);
        }
    }

    @Test
    void fieldHandsAndTheJoblessTakeUpTheCrossbow() {
        for (String trade : new String[]{"farmer", "fisherman", "fletcher", "shepherd", "nitwit", "none"}) {
            assertEquals(PaleVillagers.Illager.PILLAGER, PaleVillagers.illagerFor(trade), trade);
        }
    }

    @Test
    void anUnknownTradeTakesUpTheCrossbow() {
        // Another mod's profession
        assertEquals(PaleVillagers.Illager.PILLAGER, PaleVillagers.illagerFor("beekeeper"));
    }
}
