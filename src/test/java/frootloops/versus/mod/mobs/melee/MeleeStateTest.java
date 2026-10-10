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
        assertEquals(0.0F, swing.look(100).drawBack());
        assertEquals(0.5F, swing.look(104).drawBack(), 1.0E-6F);
        assertEquals(1.0F, swing.look(108).drawBack());
        assertTrue(swing.look(104).angry());
        assertEquals(0.0F, swing.look(104).crouch(), "a regular swing doesn't crouch");

        swing.onEvent(MobMeleePayload.HIT, 0, 108);
        assertEquals(1.0F, swing.look(108).drawBack());
        assertEquals(0.0F, swing.look(111).drawBack());
        assertEquals(0.0F, swing.look(110).lunge(), "a hit doesn't lunge");
        assertTrue(swing.look(115).angry());
        assertFalse(swing.look(116.5F).angry());
        assertEquals(MeleeState.Look.NONE, swing.look(123));
        assertEquals(MeleeState.Look.NONE, swing.look(110), "a played-out swing is forgotten");
    }

    /** A leap crouches, rises out of the crouch with its arms going up, and brings them down with the strike. */
    @Test
    void aLeapCrouchesThenRaisesItsArmsThenSlams() {
        MeleeState swing = new MeleeState();
        swing.onEvent(MobMeleePayload.HEAVY_WIND_UP, 10, 0);
        assertEquals(0.5F, swing.look(1.5F).crouch(), 1.0E-6F);
        assertEquals(1.0F, swing.look(9).crouch());
        assertEquals(0.0F, swing.look(9).raise(), "the arms stay back in the crouch");
        assertTrue(swing.look(9).angry());

        swing.onEvent(MobMeleePayload.LEAP, 12, 10);
        assertEquals(1.0F, swing.look(10).crouch());
        assertEquals(0.5F, swing.look(11).crouch(), 1.0E-6F);
        assertEquals(0.0F, swing.look(12).crouch());
        assertEquals(1.0F, swing.look(13).raise());

        swing.onEvent(MobMeleePayload.HIT, 0, 20);
        assertEquals(1.0F, swing.look(20).raise());
        assertEquals(0.0F, swing.look(23).raise(), "the arms come down with the strike");
        assertEquals(0.0F, swing.look(22).lunge(), "a hit doesn't lunge");
        assertEquals(0.0F, swing.look(22).drawBack());
    }

    /** A leap that misses lunges forward quickly, holds a moment and straightens up. */
    @Test
    void aMissLungesForwardThenStraightensUp() {
        MeleeState swing = new MeleeState();
        swing.onEvent(MobMeleePayload.HEAVY_WIND_UP, 10, 0);
        swing.onEvent(MobMeleePayload.LEAP, 12, 10);
        swing.onEvent(MobMeleePayload.MISS, 0, 20);
        assertEquals(0.0F, swing.look(20).lunge());
        assertEquals(1.0F, swing.look(23).lunge());
        assertEquals(1.0F, swing.look(25).lunge());
        float straightening = swing.look(30).lunge();
        assertTrue(straightening > 0.0F && straightening < 1.0F, "straightening up at " + straightening);
        assertEquals(0.0F, swing.look(34).lunge());
    }

    /** A swing cut short is dropped at once; one whose next step never comes, a while after it should have. */
    @Test
    void aCutShortOrLostSwingIsDropped() {
        MeleeState swing = new MeleeState();
        swing.onEvent(MobMeleePayload.WIND_UP, 8, 0);
        swing.onEvent(MobMeleePayload.CANCEL, 0, 3);
        assertEquals(MeleeState.Look.NONE, swing.look(4));

        swing.onEvent(MobMeleePayload.WIND_UP, 8, 10);
        assertTrue(swing.look(20).angry(), "still waiting for the strike");
        assertEquals(MeleeState.Look.NONE, swing.look(29));

        swing.onEvent(MobMeleePayload.HEAVY_WIND_UP, 10, 100);
        assertEquals(1.0F, swing.look(115).crouch(), "still waiting for the take-off");
        assertEquals(MeleeState.Look.NONE, swing.look(121));

        swing.onEvent(MobMeleePayload.HEAVY_WIND_UP, 10, 200);
        swing.onEvent(MobMeleePayload.LEAP, 12, 210);
        assertEquals(1.0F, swing.look(230).raise(), "still waiting for the strike");
        assertEquals(MeleeState.Look.NONE, swing.look(233));
    }

    /** A leap on the mob's own level comes down after 12 ticks, as a jump does; one down a ledge stays up longer. */
    @Test
    void aLeapStaysUpAsLongAsAJump() {
        assertEquals(12, MobMelee.flightTicks(0.42, 0.0));
        assertEquals(15, MobMelee.flightTicks(0.42, 2.0));
        assertEquals(MobMelee.LEAP_TIMEOUT, MobMelee.flightTicks(0.42, 40.0));
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
