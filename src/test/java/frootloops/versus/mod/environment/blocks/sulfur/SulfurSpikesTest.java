package frootloops.versus.mod.environment.blocks.sulfur;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A sulfur spike that hurts something poisons it for a whole number of seconds from 1 to the damage it dealt, and
 * makes it nauseous for one from 5 to the damage dealt (5 when it dealt less), drawn evenly.
 */
class SulfurSpikesTest {

    private final RandomSource random = RandomSource.create(42);

    /** The lengths drawn for this damage, in seconds, checking each is a whole number of them. */
    private static Set<Integer> seconds(int draws, java.util.function.IntSupplier ticks) {
        Set<Integer> seen = new TreeSet<>();
        for (int i = 0; i < draws; i++) {
            int drawn = ticks.getAsInt();
            assertEquals(0, drawn % 20, drawn + " ticks isn't a whole number of seconds");
            seen.add(drawn / 20);
        }
        return seen;
    }

    @Test
    void poisonLastsFromOneSecondToTheDamageDealt() {
        assertEquals(Set.of(1, 2, 3, 4, 5, 6), seconds(2000, () -> SulfurSpikes.poisonTicks(6.0F, this.random)), "poison for 6 damage");
        // After armour the damage can be a fraction: its whole seconds count; under 2, a second
        assertEquals(Set.of(1, 2), seconds(500, () -> SulfurSpikes.poisonTicks(2.6F, this.random)), "poison for 2.6 damage");
        assertEquals(Set.of(1), seconds(50, () -> SulfurSpikes.poisonTicks(1.9F, this.random)), "poison for 1.9 damage");
        assertEquals(Set.of(1), seconds(50, () -> SulfurSpikes.poisonTicks(0.5F, this.random)), "poison for half a point of damage");
    }

    @Test
    void nauseaLastsFromFiveSecondsToTheDamageDealt() {
        assertEquals(Set.of(5, 6, 7, 8), seconds(2000, () -> SulfurSpikes.nauseaTicks(8.0F, this.random)), "nausea for 8 damage");
        assertEquals(Set.of(5), seconds(50, () -> SulfurSpikes.nauseaTicks(5.0F, this.random)), "nausea for 5 damage");
        // Less damage than five seconds' worth: five seconds
        assertEquals(Set.of(5), seconds(50, () -> SulfurSpikes.nauseaTicks(3.0F, this.random)), "nausea for 3 damage");
        assertEquals(Set.of(5), seconds(50, () -> SulfurSpikes.nauseaTicks(0.5F, this.random)), "nausea for half a point of damage");
        assertTrue(seconds(500, () -> SulfurSpikes.nauseaTicks(24.0F, this.random)).containsAll(Set.of(5, 24)), "nausea for 24 damage doesn't reach both ends");
    }
}
