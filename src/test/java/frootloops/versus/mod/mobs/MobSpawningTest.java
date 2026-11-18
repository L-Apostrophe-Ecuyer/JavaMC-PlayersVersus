package frootloops.versus.mod.mobs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;

import java.util.List;

import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The monsters' natural spawn rules in the overworld (creepers under a roof, zombies, skeletons and their kin on natural
 * ground, spiders back), and night, for the pale wolves' forest spawns: dusk to dawn, any day.
 */
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
        assertTrue(MobSpawning.overworldRules(EntityTypes.CREEPER, true, 0, 30, false), "a dark cave");
        assertFalse(MobSpawning.overworldRules(EntityTypes.CREEPER, true, 1, 30, false), "a glimmer of sky");
        assertFalse(MobSpawning.overworldRules(EntityTypes.CREEPER, true, 7, 30, false), "a deep overhang");
        assertFalse(MobSpawning.overworldRules(EntityTypes.CREEPER, true, 15, 70, true), "the open sky, even at midnight");
        assertFalse(MobSpawning.overworldRules(EntityTypes.CREEPER, false, 0, 30, false), "a floor someone built");
    }

    /** Zombies and skeletons spawn naturally only on natural ground, at every hour; midnight lifts only the rule against the open sky up high. */
    @Test
    void zombiesAndSkeletonsSpawnOnlyOnNaturalGround() {
        for (var type : java.util.List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON)) {
            assertTrue(MobSpawning.overworldRules(type, true, 0, 30, false), type + " on stone in a cave");
            assertFalse(MobSpawning.overworldRules(type, false, 0, 30, false), type + " on planks");
            assertFalse(MobSpawning.overworldRules(type, false, 0, 30, true), type + " on planks at midnight");
            assertFalse(MobSpawning.overworldRules(type, true, 15, 80, false), type + " under the open sky up high");
            assertTrue(MobSpawning.overworldRules(type, true, 15, 80, true), type + " under the open sky up high at midnight");
            assertTrue(MobSpawning.overworldRules(type, true, 15, 60, false), type + " under the open sky down low");
            assertFalse(MobSpawning.overworldRules(type, true, 0, -5, false), type + " below the pale line (its pale kin's place)");
        }
    }

    /**
     * Spiders spawn as other monsters do, in the dark, the open sky included (the world's land lies mostly above y 64:
     * they had been kept off it), but not on the mountains' heights nor below the pale line (their pale kin's place).
     */
    @Test
    void spidersSpawnInTheOpenAtNightAsInCaves() {
        assertTrue(MobSpawning.overworldRules(EntityTypes.SPIDER, false, 15, 80, false), "the open sky on the land, at night");
        assertTrue(MobSpawning.overworldRules(EntityTypes.SPIDER, true, 0, 20, false), "a cave");
        assertFalse(MobSpawning.overworldRules(EntityTypes.SPIDER, true, 15, 150, false), "a mountain's height");
        assertFalse(MobSpawning.overworldRules(EntityTypes.SPIDER, true, 0, -5, false), "below the pale line");
    }

    @Test
    void otherMonstersAreLeftToTheirOwnRules() {
        assertTrue(MobSpawning.overworldRules(EntityTypes.ENDERMAN, false, 15, 70, false));
    }

    @Test
    void nightComesEveryDay() {
        assertTrue(MobSpawning.isNight(24000L * 7 + 18000L));
        assertFalse(MobSpawning.isNight(24000L * 7 + 6000L));
    }

    /** The rules are the overworld's: the nether's skeletons (on soul sand, in fortresses) and the end's keep vanilla's. */
    @Test
    void theRulesAreTheOverworldsOnly() {
        assertTrue(MobSpawning.ruled(EntityTypes.SKELETON, Level.OVERWORLD), "the overworld's skeletons");
        assertFalse(MobSpawning.ruled(EntityTypes.SKELETON, Level.NETHER), "the soul sand valleys' skeletons");
        assertFalse(MobSpawning.ruled(EntityTypes.ZOMBIE, Level.END), "the end");
        assertFalse(MobSpawning.ruled(EntityTypes.BLAZE, Level.OVERWORLD), "a monster the rules don't cover");
    }

    /**
     * Zombies' and skeletons' kin (husks, zombie villagers, strays, bogged, parched, frostbites) spawn only on natural
     * ground too; the sky is left to their own rules (husks, strays and parched want it).
     */
    @Test
    void zombiesAndSkeletonsKinSpawnOnlyOnNaturalGround() {
        for (EntityType<?> kin : List.of(EntityTypes.HUSK, EntityTypes.ZOMBIE_VILLAGER, EntityTypes.STRAY, EntityTypes.BOGGED,
                EntityTypes.PARCHED, EntityTypes.FROSTBITE)) {
            assertTrue(MobSpawning.ruled(kin, Level.OVERWORLD), kin + " isn't ruled");
            assertFalse(MobSpawning.overworldRules(kin, false, 0, 30, false), kin + " on a floor someone built");
            assertTrue(MobSpawning.overworldRules(kin, true, 15, 70, false), kin + " on natural ground under the sky");
            assertTrue(MobSpawning.overworldRules(kin, true, 0, 30, true), kin + " on natural ground in a cave");
        }
    }
}
