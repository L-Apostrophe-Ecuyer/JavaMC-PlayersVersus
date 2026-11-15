package frootloops.versus.mod.mobs;

import net.minecraft.world.entity.EntityTypes;

import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Night, for the pale wolves' forest spawns: dusk to dawn, any day. */
class MobSpawningTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    @Test
    void nightIsFromDuskToDawn() {
        assertTrue(MobSpawning.isNight(13000L));
        assertTrue(MobSpawning.isNight(18000L));
        assertTrue(MobSpawning.isNight(23000L));
        assertFalse(MobSpawning.isNight(12999L));
        assertFalse(MobSpawning.isNight(6000L));
        assertFalse(MobSpawning.isNight(23001L));
    }

    /**
     * Creepers spawn naturally only where the sky's light doesn't reach (caves, overhangs, under cover), on natural ground,
     * at every hour and moon.
     */
    @Test
    void creepersSpawnOnlyWhereNoSkyLightReaches() {
        assertTrue(MobSpawning.overworldRules(EntityTypes.CREEPER, true, 0, 30, false, false), "a dark cave");
        assertFalse(MobSpawning.overworldRules(EntityTypes.CREEPER, true, 1, 30, false, false), "a glimmer of sky");
        assertFalse(MobSpawning.overworldRules(EntityTypes.CREEPER, true, 7, 30, false, false), "a deep overhang");
        assertFalse(MobSpawning.overworldRules(EntityTypes.CREEPER, true, 15, 70, true, true), "the open sky, even at midnight");
        assertFalse(MobSpawning.overworldRules(EntityTypes.CREEPER, false, 0, 30, false, false), "a floor someone built");
    }

    /** Zombies and skeletons spawn naturally only on natural ground, at every hour; midnight lifts only the rule against the open sky up high. */
    @Test
    void zombiesAndSkeletonsSpawnOnlyOnNaturalGround() {
        for (var type : java.util.List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON)) {
            assertTrue(MobSpawning.overworldRules(type, true, 0, 30, false, false), type + " on stone in a cave");
            assertFalse(MobSpawning.overworldRules(type, false, 0, 30, false, false), type + " on planks");
            assertFalse(MobSpawning.overworldRules(type, false, 0, 30, false, true), type + " on planks at midnight");
            assertFalse(MobSpawning.overworldRules(type, true, 15, 80, true, false), type + " under the open sky up high");
            assertTrue(MobSpawning.overworldRules(type, true, 15, 80, true, true), type + " under the open sky up high at midnight");
            assertTrue(MobSpawning.overworldRules(type, true, 15, 60, true, false), type + " under the open sky down low");
            assertFalse(MobSpawning.overworldRules(type, true, 0, -5, false, false), type + " below the pale line (its pale kin's place)");
        }
    }

    /**
     * Spiders spawn as other monsters do, in the dark, the open sky included (the world's land lies mostly above y 64:
     * they had been kept off it), but not on the mountains' heights nor below the pale line (their pale kin's place).
     */
    @Test
    void spidersSpawnInTheOpenAtNightAsInCaves() {
        assertTrue(MobSpawning.overworldRules(EntityTypes.SPIDER, false, 15, 80, true, false), "the open sky on the land, at night");
        assertTrue(MobSpawning.overworldRules(EntityTypes.SPIDER, true, 0, 20, false, false), "a cave");
        assertFalse(MobSpawning.overworldRules(EntityTypes.SPIDER, true, 15, 150, true, false), "a mountain's height");
        assertFalse(MobSpawning.overworldRules(EntityTypes.SPIDER, true, 0, -5, false, false), "below the pale line");
    }

    @Test
    void otherMonstersAreLeftToTheirOwnRules() {
        assertTrue(MobSpawning.overworldRules(EntityTypes.ENDERMAN, false, 15, 70, true, false));
    }

    @Test
    void nightComesEveryDay() {
        assertTrue(MobSpawning.isNight(24000L * 7 + 18000L));
        assertFalse(MobSpawning.isNight(24000L * 7 + 6000L));
    }
}
