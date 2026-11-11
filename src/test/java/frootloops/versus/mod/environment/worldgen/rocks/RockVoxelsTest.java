package frootloops.versus.mod.environment.worldgen.rocks;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A rock's blocks in cells: blended where they meet, the foot curving out, no knobs nor pits, seams drawn whole. */
class RockVoxelsTest {

    /** A box of half sizes (a, (tall + sink) / 2, c) standing at (x, z), square to the axes. */
    private static RockBlock box(double x, double z, double a, double c, double tall, double sink, int group) {
        return RockBlock.standing(x, z, tall, sink, RockBlock.planes(new RockDice(1), a, (tall + sink) / 2, c, 0, 0.7, 0, 0, false, 0), 0, 0, 0, group);
    }

    @Test
    void aBlockIsSolidInsideOnly() {
        RockVoxels cells = RockVoxels.of(List.of(box(0, 0, 3, 3, 6, 1, -1)), 0.6, 0, 1, 1);
        assertTrue(cells.solid(0, 3, 0), "inside");
        assertFalse(cells.solid(6, 3, 0), "beside it");
        assertFalse(cells.solid(0, 8, 0), "over it");
        assertEquals(5, cells.top(), "it stands 6 tall: its top cell is at 5");
    }

    @Test
    void blocksMeetInAGrooveNotASlot() {
        // Their faces at x 0.35 and 0.65: the cells' centres between them, at x 0.5, a cell's width from neither
        List<RockBlock> blocks = List.of(box(0.35 - 3, 0, 3, 3, 8, 1, -1), box(0.65 + 3, 0, 3, 3, 8, 1, -1));
        assertTrue(RockVoxels.of(blocks, 0.8, 0, 1, 1).solid(0, 3, 0), "blended, the gap between them is filled");
        assertFalse(RockVoxels.of(blocks, 0.01, 0, 1, 1).solid(0, 3, 0), "not blended, it isn't");
    }

    @Test
    void noKnobsNorPits() {
        // A knob: a cell's worth of rock on a block's face, which the clean-up takes off
        RockBlock knob = new RockBlock(3.5, 3.5, 0.5, RockBlock.planes(new RockDice(2), 0.9, 0.9, 0.9, 0, 0.7, 0, 0, false, 0), RockBlock.level(0), -1);
        RockVoxels knobbed = RockVoxels.of(List.of(box(0, 0, 3, 3, 6, 1, -1), knob), 0.01, 0, 1, 1);
        assertFalse(knobbed.solid(3, 3, 0), "the knob is gone");
        // And among blocks of all sizes, tipped every way, none of either
        RockDice dice = new RockDice(3);
        List<RockBlock> blocks = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            double a = dice.between(1.5, 4), c = dice.between(1.5, 4), tall = dice.between(3, 9), sink = 1;
            blocks.add(RockBlock.standing(dice.between(-4, 4), dice.between(-4, 4), tall, sink, RockBlock.planes(dice, a, (tall + sink) / 2, c, dice.whole(4, 10),
                    0.65, 0.5, 0.12, false, 1), dice.angle(), dice.angle(), dice.between(0, 0.35), -1));
        }
        RockVoxels cells = RockVoxels.of(blocks, 0.6, 1, 3, 4);
        int[][] six = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int x = cells.minX + 1; x < cells.minX + cells.sizeX - 1; x++) {
            for (int z = cells.minZ + 1; z < cells.minZ + cells.sizeZ - 1; z++) {
                for (int y = 0; y < cells.minY + cells.sizeY - 1; y++) {
                    int filled = 0;
                    for (int[] step : six) if (cells.filled(x + step[0], y + step[1], z + step[2])) filled++;
                    if (cells.solid(x, y, z)) assertTrue(filled >= 2, "a knob at " + x + " " + y + " " + z);
                    else assertTrue(filled <= 4, "a pit at " + x + " " + y + " " + z);
                }
            }
        }
    }

    /** How many cells wide the rock is along x, at this height, through z = 0. */
    private static int width(RockVoxels cells, int y) {
        int width = 0;
        for (int x = cells.minX; x < cells.minX + cells.sizeX; x++) if (cells.solid(x, y, 0)) width++;
        return width;
    }

    @Test
    void theFootCurvesOut() {
        List<RockBlock> tall = List.of(box(0, 0, 3, 3, 20, 1, -1));
        RockVoxels straight = RockVoxels.of(tall, 0.6, 0, 1, 1), flared = RockVoxels.of(tall, 0.6, 3, 9, 1);
        assertTrue(width(flared, 0) >= width(straight, 0) + 4, "wider at the ground: " + width(flared, 0) + " against " + width(straight, 0));
        assertTrue(Math.abs(width(flared, 12) - width(straight, 12)) <= 1, "the same higher up: " + width(flared, 12) + " against " + width(straight, 12));
    }

    @Test
    void seamsAreDrawnWholeAndNeverInsideAGroup() {
        // Two blocks pressed together, overlapping by half a block
        for (long seed = 1; seed <= 20; seed++) {
            RockVoxels column = RockVoxels.of(List.of(box(-2.75, 0, 3, 3, 6, 1, 7), box(2.75, 0, 3, 3, 6, 1, 7)), 0.6, 0, 1, seed);
            for (int x = column.minX; x < column.minX + column.sizeX; x++) {
                for (int z = column.minZ; z < column.minZ + column.sizeZ; z++) {
                    for (int y = column.minY; y < column.minY + column.sizeY; y++) assertFalse(column.seam(x, y, z), "a seam inside a column, seed " + seed);
                }
            }
        }
        int drawn = 0, left = 0;
        for (long seed = 1; seed <= 20; seed++) {
            RockBlock one = box(-2.75, 0, 3, 3, 6, 1, 1), two = box(2.75, 0, 3, 3, 6, 1, 2);
            RockVoxels cells = RockVoxels.of(List.of(one, two), 0.6, 0, 1, seed);
            // Where they meet: the rock within a block of being as near the one as the other
            Set<Long> meeting = new HashSet<>(), seams = new HashSet<>();
            for (int x = cells.minX; x < cells.minX + cells.sizeX; x++) {
                for (int z = cells.minZ; z < cells.minZ + cells.sizeZ; z++) {
                    for (int y = cells.minY; y < cells.minY + cells.sizeY; y++) {
                        long key = ((long) x << 40) ^ ((long) z << 20) ^ (y & 0xFFFFF);
                        if (cells.solid(x, y, z) && Math.abs(one.distance(x + 0.5, y + 0.5, z + 0.5) - two.distance(x + 0.5, y + 0.5, z + 0.5)) < 1) meeting.add(key);
                        if (cells.seam(x, y, z)) seams.add(key);
                    }
                }
            }
            assertFalse(meeting.isEmpty(), "they meet");
            if (seams.isEmpty()) left++;
            else {
                assertEquals(meeting, seams, "seed " + seed + ": a seam is drawn whole");
                drawn++;
            }
        }
        assertTrue(drawn > 0 && left > 0, "some seams are drawn, some not: " + drawn + " drawn, " + left + " not");
    }
}
