package frootloops.versus.mod.environment.worldgen.rocks;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static frootloops.versus.mod.environment.worldgen.rocks.RockMaterial.BASE;
import static frootloops.versus.mod.environment.worldgen.rocks.RockMaterial.BRIGHT;
import static frootloops.versus.mod.environment.worldgen.rocks.RockMaterial.LIGHT;
import static frootloops.versus.mod.environment.worldgen.rocks.RockMaterial.SHADE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Light painted into the stone from above: undersides tuff, tops lighter than feet, most of it stone, clay rare. */
class RockLightTest {

    /** A box of half sizes (a, (tall + sink) / 2, c) standing at (x, z), square to the axes. */
    private static RockBlock box(double x, double z, double a, double c, double tall, double sink, int group) {
        return RockBlock.standing(x, z, tall, sink, RockBlock.planes(new RockDice(1), a, (tall + sink) / 2, c, 0, 0.7, 0, 0, false, 0), 0, 0, 0, group);
    }

    /** Twelve blocks in four columns of three, the joints mostly upright, each column's top cut at angles: a castle's. */
    private static RockVoxels columns() {
        RockDice dice = new RockDice(5);
        double yaw = dice.angle();
        List<RockBlock> blocks = new ArrayList<>();
        for (int column = 0; column < 4; column++) {
            double x = (column % 2) * 5.5 - 2.75 + dice.between(-0.4, 0.4), z = (column / 2) * 5.5 - 2.75 + dice.between(-0.4, 0.4);
            double top = dice.between(14, 20), y = -2, thick = (top - y) / 3;
            for (int layer = 0; layer < 3; layer++) {
                boolean last = layer == 2;
                double a = dice.between(2.6, 3.1), c = dice.between(2.6, 3.1);
                double[] planes = last ? RockBlock.planes(dice, a, thick / 2 + 0.75, c, dice.whole(5, 8), 0.68, 0.9, 0.06, false, 0)
                        : RockBlock.planes(dice, a, thick / 2 + 0.75, c, dice.whole(2, 4), 0.85, 0, 0.03, true, layer == 0 ? 1 : 0);
                blocks.add(new RockBlock(x, y + thick / 2, z, planes, RockBlock.level(yaw + dice.between(-0.1, 0.1)), column));
                y += thick;
            }
        }
        return RockVoxels.of(blocks, 0.6, 3, 9, 5);
    }

    /** Whether any of the cell's six neighbours is open. */
    private static boolean exposed(RockVoxels cells, int x, int y, int z) {
        return !cells.filled(x + 1, y, z) || !cells.filled(x - 1, y, z) || !cells.filled(x, y + 1, z) || !cells.filled(x, y - 1, z)
                || !cells.filled(x, y, z + 1) || !cells.filled(x, y, z - 1);
    }

    @Test
    void onlyTheFourTonesOfRock() {
        RockVoxels cells = RockVoxels.of(List.of(box(-2.5, 0, 3, 3, 7, 1, -1), box(2.5, 0.5, 2.5, 3, 5, 1, -1), box(0, 3.5, 2.5, 2, 9, 1, -1)), 0.6, 1, 3, 2);
        RockMaterial[] tones = RockLight.paint(cells, 2);
        assertEquals(cells.sizeX * cells.sizeY * cells.sizeZ, tones.length, "a tone for every cell");
        Set<RockMaterial> rock = EnumSet.of(SHADE, BASE, LIGHT, BRIGHT);
        int solid = 0;
        for (int x = cells.minX; x < cells.minX + cells.sizeX; x++) {
            for (int z = cells.minZ; z < cells.minZ + cells.sizeZ; z++) {
                for (int y = cells.minY; y < cells.minY + cells.sizeY; y++) {
                    RockMaterial tone = tones[cells.index(x, y, z)];
                    if (!cells.solid(x, y, z)) {
                        assertNull(tone, "air at " + x + " " + y + " " + z);
                        continue;
                    }
                    assertTrue(rock.contains(tone), tone + " at " + x + " " + y + " " + z);
                    solid++;
                }
            }
        }
        assertTrue(solid > 100, "the rock: " + solid);
    }

    @Test
    void undersidesAreTuff() {
        // A box 8 wide, from 5 up to 9, resting on one 3 wide
        RockVoxels cells = RockVoxels.of(List.of(box(0, 0, 1.5, 1.5, 6, 1, -1), box(0, 0, 4, 4, 9, -5, -1)), 0.6, 0, 1, 3);
        RockMaterial[] tones = RockLight.paint(cells, 3);
        int undersides = 0;
        for (int x = cells.minX; x < cells.minX + cells.sizeX; x++) {
            for (int z = cells.minZ; z < cells.minZ + cells.sizeZ; z++) {
                for (int y = 1; y < cells.minY + cells.sizeY; y++) {
                    // Facing down: air under it, rock over it
                    if (!cells.solid(x, y, z) || cells.solid(x, y - 1, z) || !cells.solid(x, y + 1, z)) continue;
                    assertEquals(SHADE, tones[cells.index(x, y, z)], "the underside at " + x + " " + y + " " + z);
                    undersides++;
                }
            }
        }
        assertTrue(undersides >= 40, "the overhang has an underside: " + undersides);
    }

    @Test
    void lightComesFromAbove() {
        RockVoxels cells = columns();
        RockMaterial[] tones = RockLight.paint(cells, 5);
        int top = cells.top();
        double high = 0, low = 0;
        int highs = 0, lows = 0;
        for (int x = cells.minX; x < cells.minX + cells.sizeX; x++) {
            for (int z = cells.minZ; z < cells.minZ + cells.sizeZ; z++) {
                for (int y = 0; y <= top; y++) {
                    if (!cells.solid(x, y, z) || !exposed(cells, x, y, z)) continue;
                    // SHADE 0, BASE 1, LIGHT 2, BRIGHT 3
                    int tone = tones[cells.index(x, y, z)].ordinal();
                    if (3 * y >= 2 * top && !cells.solid(x, y + 1, z)) {
                        high += tone;
                        highs++;
                    } else if (3 * y < top) {
                        low += tone;
                        lows++;
                    }
                }
            }
        }
        assertTrue(highs > 20 && lows > 20, "tops high up and faces low down: " + highs + ", " + lows);
        assertTrue(high / highs >= low / lows + 0.5, "the tops high up lighter than the faces low down: " + high / highs + " against " + low / lows);
    }

    @Test
    void stoneIsCommonestAndClayRare() {
        RockVoxels cells = columns();
        RockMaterial[] tones = RockLight.paint(cells, 5);
        Map<RockMaterial, Integer> counts = new EnumMap<>(RockMaterial.class);
        int exposed = 0;
        for (int x = cells.minX; x < cells.minX + cells.sizeX; x++) {
            for (int z = cells.minZ; z < cells.minZ + cells.sizeZ; z++) {
                for (int y = 0; y < cells.minY + cells.sizeY; y++) {
                    if (!cells.solid(x, y, z) || !exposed(cells, x, y, z)) continue;
                    counts.merge(tones[cells.index(x, y, z)], 1, Integer::sum);
                    exposed++;
                }
            }
        }
        // Of the faces painted by their light, all but the tuff: stone 46, andesite 21, clay 3 of every 70
        double lit = counts.getOrDefault(BASE, 0) + counts.getOrDefault(LIGHT, 0) + counts.getOrDefault(BRIGHT, 0);
        assertEquals(46 / 70.0, counts.getOrDefault(BASE, 0) / lit, 0.03, "stone the most: " + counts);
        assertEquals(21 / 70.0, counts.getOrDefault(LIGHT, 0) / lit, 0.03, "andesite: " + counts);
        assertEquals(3 / 70.0, counts.getOrDefault(BRIGHT, 0) / lit, 0.02, "clay: " + counts);
        assertTrue(counts.getOrDefault(BRIGHT, 0) <= 0.05 * exposed, "clay rare: " + counts);
        // Undersides, creases, the drawn cracks and the darkest faces: under three fifths, as on the prototype's rocks
        assertTrue(counts.getOrDefault(SHADE, 0) <= 0.6 * exposed, "tuff not everywhere: " + counts);
    }

    @Test
    void theSameRockTheSameTones() {
        assertArrayEquals(RockLight.paint(columns(), 5), RockLight.paint(columns(), 5));
    }
}
