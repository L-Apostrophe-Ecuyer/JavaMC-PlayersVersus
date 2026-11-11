package frootloops.versus.mod.environment.worldgen.rocks;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** One angular block of a rock: a box, its faces off square, its edges and corners cut, its big faces bent. */
class RockBlockTest {

    private static double[] apply(double[] m, double x, double y, double z) {
        return new double[]{m[0] * x + m[1] * y + m[2] * z, m[3] * x + m[4] * y + m[5] * z, m[6] * x + m[7] * y + m[8] * z};
    }

    @Test
    void aBoxIsItsHalfSizesAcross() {
        RockBlock box = new RockBlock(0, 0, 0, RockBlock.planes(new RockDice(1), 3, 2, 1, 0, 0.7, 0, 0, false, 0), RockBlock.level(0), -1);
        assertTrue(box.distance(0, 0, 0) < -0.5 && box.distance(0, 0, 0) > -1.1, "the middle is about a block inside: " + box.distance(0, 0, 0));
        assertTrue(box.distance(2.5, 0, 0) < 0 && box.distance(3.5, 0, 0) > 0, "x reaches 3");
        assertTrue(box.distance(0, 1.5, 0) < 0 && box.distance(0, 2.5, 0) > 0, "y reaches 2");
        assertTrue(box.distance(0, 0, 0.5) < 0 && box.distance(0, 0, 1.5) > 0, "z reaches 1");
    }

    /** How many centres of a 1-block grid (from -6 to 6) lie inside, at all heights or at one. */
    private static int inside(RockBlock block, Double y) {
        int count = 0;
        for (double x = -5.5; x <= 5.5; x += 1) {
            for (double z = -5.5; z <= 5.5; z += 1) {
                if (y != null) {
                    if (block.distance(x, y, z) < 0) count++;
                    continue;
                }
                for (double h = -6.5; h <= 6.5; h += 1) if (block.distance(x, h, z) < 0) count++;
            }
        }
        return count;
    }

    @Test
    void cutsTakeOffEdgesAndCorners() {
        RockBlock whole = new RockBlock(0, 0, 0, RockBlock.planes(new RockDice(2), 4, 4, 4, 0, 0.6, 0, 0, false, 0), RockBlock.level(0), -1);
        RockBlock cut = new RockBlock(0, 0, 0, RockBlock.planes(new RockDice(2), 4, 4, 4, 8, 0.6, 0, 0, false, 0), RockBlock.level(0), -1);
        assertTrue(cut.distance(0, 0, 0) < 0, "the middle stays inside");
        int before = inside(whole, null), after = inside(cut, null);
        assertTrue(after <= 0.9 * before, "eight cuts take off a tenth or more: " + after + " of " + before);
    }

    @Test
    void aBentFaceLeansBackAboveItsLine() {
        // Below every bend's line (it lies 20 to 60% of the way up) the block is whole; near its top, bent faces stand back
        RockBlock straight = new RockBlock(0, 0, 0, RockBlock.planes(new RockDice(3), 3, 6, 3, 0, 0.7, 0, 0, false, 0), RockBlock.level(0), -1);
        RockBlock bent = new RockBlock(0, 0, 0, RockBlock.planes(new RockDice(3), 3, 6, 3, 0, 0.7, 0, 0, false, 3), RockBlock.level(0), -1);
        assertEquals(inside(straight, -5.0), inside(bent, -5.0), "nothing changes low on its faces");
        assertTrue(inside(bent, 5.5) < inside(bent, -5.0), "near its top it's narrower: " + inside(bent, 5.5) + " against " + inside(bent, -5.0));
    }

    @Test
    void aStandingBlockStandsOnItsFoot() {
        double[] box = RockBlock.planes(new RockDice(4), 2, 3.75, 2, 0, 0.7, 0, 0, false, 0);
        RockBlock upright = RockBlock.standing(0, 0, 6, 1.5, box, 0, 0, 0, -1);
        assertTrue(upright.distance(0, -1.2, 0) < 0, "its foot is in the ground");
        assertTrue(upright.distance(0, 5.8, 0) < 0 && upright.distance(0, 6.5, 0) > 0, "it stands 6 tall");
        RockBlock tipped = RockBlock.standing(0, 0, 6, 1.5, box, 0, 0, 0.3, -1);
        assertTrue(tipped.distance(0.9 * 6 * Math.sin(0.3), 0.9 * 6 * Math.cos(0.3), 0) < 0, "tipped toward +x, its top leans that way");
        assertTrue(tipped.distance(-0.9 * 6 * Math.sin(0.3), 0.9 * 6 * Math.cos(0.3), 0) > 0, "and not the other way");
        assertTrue(tipped.distance(0, -1.2, 0) < 0, "its foot stays where it was");
    }

    @Test
    void turningKeepsTheShape() {
        RockBlock block = RockBlock.standing(1.5, -2, 7, 1, RockBlock.planes(new RockDice(5), 3, 4, 2.5, 9, 0.65, 0.6, 0.12, false, 2), 0.4, 2.1, 0.2, -1);
        double[] turn = RockBlock.multiply(RockBlock.level(0.7), RockBlock.tip(1.1, 0.4));
        RockBlock turned = block.turned(turn, 1, 2, 3);
        Random random = new Random(6);
        for (int index = 0; index < 100; index++) {
            double x = 12 * random.nextDouble() - 6, y = 12 * random.nextDouble() - 4, z = 12 * random.nextDouble() - 6;
            double[] moved = apply(turn, x - 1, y - 2, z - 3);
            assertEquals(block.distance(x, y, z), turned.distance(moved[0] + 1, moved[1] + 2, moved[2] + 3), 1e-9, "at " + x + " " + y + " " + z);
        }
    }

    @Test
    void levelAndTipTurnTheirOwnWay() {
        assertArrayEquals(new double[]{0, 0, 1}, apply(RockBlock.level(Math.PI / 2), 1, 0, 0), 1e-12, "a level turn takes local x to its yaw");
        assertArrayEquals(new double[]{-1, 0, 0}, apply(RockBlock.level(Math.PI / 2), 0, 0, 1), 1e-12, "and local z a right angle on");
        assertArrayEquals(new double[]{0, Math.cos(0.5), Math.sin(0.5)}, apply(RockBlock.tip(Math.PI / 2, 0.5), 0, 1, 0), 1e-12,
                "a tip toward +z tips the top toward +z");
    }

    @Test
    void theSameSeedGivesTheSameNumbers() {
        RockDice first = new RockDice(5), second = new RockDice(5), other = new RockDice(6);
        boolean differs = false;
        for (int index = 0; index < 10; index++) {
            double a = first.between(0, 1), b = second.between(0, 1), c = other.between(0, 1);
            assertEquals(a, b, "the same seed, the same draws");
            differs |= a != c;
        }
        assertTrue(differs, "another seed, other draws");
        assertFalse(new RockDice(7).whole(3, 3) != 3, "whole(3, 3) is 3");
    }

    @Test
    void blocksMadeAlikeAreEqual() {
        // A plan is equal to the same plan drawn again: its bodies' blocks are values
        RockBlock one = RockBlock.standing(1, 2, 6, 1, RockBlock.planes(new RockDice(3), 2, 3.5, 2, 5, 0.65, 0.5, 0.1, false, 1), 0.4, 1, 0.2, 7);
        RockBlock same = RockBlock.standing(1, 2, 6, 1, RockBlock.planes(new RockDice(3), 2, 3.5, 2, 5, 0.65, 0.5, 0.1, false, 1), 0.4, 1, 0.2, 7);
        RockBlock other = RockBlock.standing(1, 2, 6, 1, RockBlock.planes(new RockDice(4), 2, 3.5, 2, 5, 0.65, 0.5, 0.1, false, 1), 0.4, 1, 0.2, 7);
        assertEquals(one, same);
        assertEquals(one.hashCode(), same.hashCode());
        assertFalse(one.equals(other), "other planes, another block");
        assertFalse(one.equals(same.turned(RockBlock.level(0.1), 0, 0, 0)), "turned, another block");
    }

    @Test
    void theCheapDistanceFallsOnTheSameSideAsTheExactOne() {
        // Whatever value a decision turns on (0 at the face, more out in the foot's flare), the cheap distance taken around
        // it puts every point on the same side of it as the exact distance does
        RockDice dice = new RockDice(9);
        int near = 0;
        for (int index = 0; index < 20; index++) {
            double tall = dice.between(4, 12);
            RockBlock block = RockBlock.standing(0, 0, tall, 1, RockBlock.planes(dice, dice.between(1.5, 5), (tall + 1) / 2, dice.between(1.5, 5), dice.whole(4, 12),
                    0.62, 0.7, 0.1, false, dice.whole(0, 3)), dice.angle(), dice.angle(), dice.between(0, 0.3), -1);
            for (int point = 0; point < 2000; point++) {
                double x = dice.between(-10, 10), y = dice.between(-4, 16), z = dice.between(-10, 10), around = dice.between(0, 6);
                double exact = block.distance(x, y, z);
                assertEquals(exact < around, block.distance(x, y, z, around) < around, "at " + x + " " + y + " " + z + " around " + around);
                if (Math.abs(exact - around) < 0.5) near++;
            }
        }
        assertTrue(near > 1000, "many points near the value: " + near);
    }
}
