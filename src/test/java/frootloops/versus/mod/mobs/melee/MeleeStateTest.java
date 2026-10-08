package frootloops.versus.mod.mobs.melee;

import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeleeStateTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    /** A regular swing draws back over its wind-up, comes through within a few ticks of the strike, then is forgotten. */
    @Test
    void aRegularSwingWindsUpThenComesThrough() {
        MeleeState swing = new MeleeState();
        swing.onEvent(MobMeleePayload.WIND_UP, 8, 100);
        assertEquals(0.0F, swing.look(100).windUp());
        assertEquals(0.5F, swing.look(104).windUp(), 1.0E-6F);
        assertEquals(1.0F, swing.look(108).windUp());
        assertTrue(swing.look(104).angry());
        assertFalse(swing.look(104).heavy());

        swing.onEvent(MobMeleePayload.HIT, 0, 108);
        assertEquals(1.0F, swing.look(108).windUp());
        assertEquals(0.0F, swing.look(111).windUp());
        assertEquals(0.0F, swing.look(110).lunge(), "a hit doesn't lunge");
        assertTrue(swing.look(115).angry());
        assertFalse(swing.look(116.5F).angry());
        assertEquals(MeleeState.Look.NONE, swing.look(123));
        assertEquals(MeleeState.Look.NONE, swing.look(110), "a played-out swing is forgotten");
    }

    /** A heavy swing that misses lunges forward quickly, holds a moment and straightens up. */
    @Test
    void aMissLungesForwardThenStraightensUp() {
        MeleeState swing = new MeleeState();
        swing.onEvent(MobMeleePayload.HEAVY_WIND_UP, 20, 0);
        assertTrue(swing.look(10).heavy());
        swing.onEvent(MobMeleePayload.MISS, 0, 20);
        assertEquals(0.0F, swing.look(20).lunge());
        assertEquals(1.0F, swing.look(23).lunge());
        assertEquals(1.0F, swing.look(25).lunge());
        float straightening = swing.look(30).lunge();
        assertTrue(straightening > 0.0F && straightening < 1.0F, "straightening up at " + straightening);
        assertEquals(0.0F, swing.look(34).lunge());
    }

    /** A swing cut short is dropped at once; one whose strike never comes, a while after it should have. */
    @Test
    void aCutShortOrLostSwingIsDropped() {
        MeleeState swing = new MeleeState();
        swing.onEvent(MobMeleePayload.WIND_UP, 8, 0);
        swing.onEvent(MobMeleePayload.CANCEL, 0, 3);
        assertEquals(MeleeState.Look.NONE, swing.look(4));

        swing.onEvent(MobMeleePayload.WIND_UP, 8, 10);
        assertTrue(swing.look(20).angry(), "still waiting for the strike");
        assertEquals(MeleeState.Look.NONE, swing.look(29));
    }

    /** The melee mixins apply on 26.3: their handlers are in the classes, and every mob carries a swing. */
    @Test
    void theMeleeMixinsApply() throws NoSuchMethodException {
        assertTrue(MeleeHolder.class.isAssignableFrom(Mob.class), "mobs don't carry a swing");
        MeleeAttackGoal.class.getDeclaredMethod("strike", LivingEntity.class);
        assertTrue(hasHandler(MeleeAttack.class, "playersVersus$windUp"), "the brain's melee doesn't wind up");
        assertTrue(hasHandler(MeleeAttack.class, "playersVersus$swingAtTheStrike"), "the brain's melee swings at once");
        assertTrue(hasHandler(MeleeAttack.class, "playersVersus$coolDownAfterTheSwing"), "the brain's cooldown doesn't wait for the swing");
    }

    private static boolean hasHandler(Class<?> target, String name) {
        return Arrays.stream(target.getDeclaredMethods()).map(Method::getName).anyMatch(method -> method.endsWith(name));
    }
}
