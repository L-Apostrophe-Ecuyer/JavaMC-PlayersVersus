package frootloops.versus.mod.environment.worldgen.rocks;

import frootloops.versus.mod.environment.worldgen.rocks.RockPlan.Body;
import frootloops.versus.mod.environment.worldgen.rocks.RockPlan.Fracture;
import frootloops.versus.mod.environment.worldgen.rocks.RockPlan.Lobe;

import java.util.ArrayList;
import java.util.List;

/**
 * The kinds of rock: each makes a body of a height and a radius its own way, drawing the rest from the seed. A shape's
 * numbers are the look that was tuned next to the reference pictures; a template changes the few that a place calls for.
 *
 * <p>Each shape's own range of sizes is what it looks right at; the small ones fit a feature (everything within 15
 * blocks of its origin), the large ones are for formations.
 */
public enum RockShape {
    /** A stone or two or three against each other, part in the ground: ellipsoids cut flat by planes and split by cracks. */
    BOULDER(3, 6, 3.5, 5, 1.9, 0.25, 0.4, 0, 0.25, 0, 0.35),
    /** A heap of boulders: a large one with smaller ones against it. */
    PILE(3.5, 6, 4, 6, 1.5, 0.3, 0.45, 0, 0.25, 0, 0.6),
    /** A small angular outcrop: two or three upright lobes with flat sides. */
    CRAG(8, 13, 3.5, 5, 1.36, 0.8, 0.6, 0.3, 0.2, 0, 0.6),
    /** A rounded hill of rock, about as tall as it is wide, with a wide overgrown top. */
    DOME(10, 18, 9, 14, 1.4, 1.5, 0.6, 0.35, 0.15, 5, 0.5),
    /** A leaning egg of rock with a flat top and a smaller lobe or two at its foot. */
    MONOLITH(26, 38, 8, 11, 1.78, 1.4, 1.0, 0.25, 0.15, 8, 0.6),
    /** An upright tower with a flat top. */
    TOWER(30, 46, 7, 10, 1.48, 1.2, 1.1, 0.25, 0.15, 7, 0.5),
    /** A thin, leaning pinnacle. */
    NEEDLE(18, 34, 2.5, 4.5, 2.25, 0.6, 0.5, 0.3, 0.15, 3, 0.4),
    /** A great spire split into pinnacles of different heights, with clefts between them. */
    SPIRE(55, 85, 10, 14, 1.6, 1.8, 1.3, 0.3, 0.15, 9, 0.6),
    /** A wide mass with upright cliffs and several tops. */
    MASSIF(38, 56, 18, 26, 1.45, 3, 1.5, 0.5, 0.15, 10, 0.6);

    /** The sizes a shape looks right at. */
    public final double minHeight, maxHeight, minRadius, maxRadius;
    /** How far its lobes reach from its middle, as a multiple of its radius. */
    private final double spread;
    /** The most its outline is pushed out by its bulges, its pieces and its chunks. */
    private final double warp, pieceDepth, chunkDepth, roughness;
    private final double skirtWidth, scree;

    RockShape(double minHeight, double maxHeight, double minRadius, double maxRadius, double spread, double warp, double pieceDepth, double chunkDepth,
              double roughness, double skirtWidth, double scree) {
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.spread = spread;
        this.warp = warp;
        this.pieceDepth = pieceDepth;
        this.chunkDepth = chunkDepth;
        this.roughness = roughness;
        this.skirtWidth = skirtWidth;
        this.scree = scree;
    }

    /**
     * How far from its middle, along x or z, a body of this shape reaches at most: its rock, its skirt ({@code skirt}
     * times the shape's own) and its stones.
     */
    public int reach(double radius, double skirt, double scree) {
        double pushed = this.warp(radius) + this.pieceDepth + this.chunkDepth + this.roughness + 0.5;
        double around = Math.max(this.skirtWidth * skirt, scree > 0 ? 3 : 0);
        return (int) (Math.ceil(this.spread * radius) + Math.ceil(pushed) + Math.ceil(around));
    }

    /** Large rocks bulge more; small ones of the same shape would lose their outline to it. */
    private double warp(double radius) {
        return Math.min(this.warp, radius / 5);
    }

    /** The few large pieces a body is broken into: about its radius across, standing in and out by a share of it. */
    private Fracture pieces(double size, double tall, double depth, double gape) {
        return new Fracture(size, tall, Math.min(depth, this.pieceDepth), this == BOULDER || this == PILE ? 1 : 0.7, gape);
    }

    /** A body of this shape, {@code height} blocks tall and {@code radius} across at its widest lobe. */
    public Body body(long seed, double height, double radius) {
        Dice dice = new Dice(seed);
        List<Lobe> lobes = new ArrayList<>();
        double warp = this.warp(radius);
        return switch (this) {
            case BOULDER -> {
                lobes.add(blob(dice, 0, 0, height, radius));
                // More often than not a smaller stone or two lean against it
                int more = dice.chance(0.7) ? dice.whole(1, 2) : 0;
                double turn = dice.angle();
                for (int index = 0; index < more; index++) {
                    double angle = turn + Math.PI * index + dice.between(-0.7, 0.7), away = radius * dice.between(0.65, 0.95), size = dice.between(0.5, 0.75);
                    lobes.add(blob(dice, away * Math.cos(angle), away * Math.sin(angle), height * size * dice.between(0.9, 1.1), radius * size));
                }
                yield new Body(0, 0, lobes, warp, 5, this.pieces(Math.max(3, 1.1 * radius), 0.85, 0.4, 1.6), Fracture.NONE,
                        0, 0, 0, this.roughness, 0.18, 0.3, 1, 0.03, 0.3, true, 0, 0, this.scree, seed);
            }
            case PILE -> {
                lobes.add(blob(dice, 0, 0, height, 0.62 * radius));
                int more = dice.whole(3, 5);
                double turn = dice.angle();
                for (int index = 0; index < more; index++) {
                    double angle = turn + 2 * Math.PI * (index + dice.between(-0.3, 0.3)) / more, away = radius * dice.between(0.5, 0.85);
                    lobes.add(blob(dice, away * Math.cos(angle), away * Math.sin(angle), height * dice.between(0.45, 0.8), radius * dice.between(0.28, 0.5)));
                }
                yield new Body(0, 0, lobes, warp, 5, this.pieces(Math.max(3, 0.75 * radius), 0.85, 0.45, 1.6), Fracture.NONE,
                        0, 0, 0, this.roughness, 0.22, 0.3, 1, 0.04, 0.3, true, 0, 0, this.scree, seed);
            }
            case CRAG -> {
                double lean = dice.angle();
                lobes.add(column(dice, 0, 0, height, 0.75 * radius, dice.between(0.9, 1.1), 0.15 * radius * Math.cos(lean), 0.15 * radius * Math.sin(lean),
                        dice.between(2.5, 3.2), 0, 0, 0, 0.1, dice.whole(5, 7), 0.22));
                int more = dice.whole(1, 2);
                double turn = dice.angle();
                for (int index = 0; index < more; index++) {
                    double angle = turn + Math.PI * index + dice.between(-0.5, 0.5), away = radius * dice.between(0.5, 0.7);
                    lobes.add(column(dice, away * Math.cos(angle), away * Math.sin(angle), height * dice.between(0.45, 0.75), radius * dice.between(0.45, 0.6),
                            dice.between(0.9, 1.1), 0, 0, dice.between(2.5, 3.2), 0, 0, 0, 0.1, dice.whole(5, 7), 0.22));
                }
                yield new Body(0, 0, lobes, warp, 8, this.pieces(Math.max(3, 0.9 * radius), 1.3, 0.6, 2), new Fracture(3, 1.3, this.chunkDepth, 0.12, 1.2),
                        0, 0, 0, this.roughness, 0.4, 0.5, 1, 0.1, 0.35, true, this.skirtWidth, 1.5, this.scree, seed);
            }
            case DOME -> {
                double lean = dice.angle();
                lobes.add(column(dice, 0, 0, height, radius, dice.between(0.75, 1.1), 0.08 * radius * Math.cos(lean), 0.08 * radius * Math.sin(lean),
                        dice.between(2.0, 2.4), 0, 0.05, dice.between(0.08, 0.15), dice.between(0.14, 0.2), 0, 0));
                if (dice.chance(0.6)) {
                    double angle = dice.angle(), away = 0.6 * radius;
                    lobes.add(column(dice, away * Math.cos(angle), away * Math.sin(angle), height * dice.between(0.4, 0.65), radius * dice.between(0.35, 0.5),
                            dice.between(0.9, 1.1), 0, 0, 2.2, 0, 0, 0.1, 0.14, 0, 0));
                }
                yield new Body(0, 0, lobes, warp, Math.max(10, radius), this.pieces(0.8 * radius, 0.9, 0.9, 2.2), new Fracture(4.5, 0.9, this.chunkDepth, 0.12, 1.2),
                        0, 0, 0, this.roughness, 0.75, 0.5, 2, 0.12, 0.45, false, this.skirtWidth, 2, this.scree, seed);
            }
            case MONOLITH -> {
                double lean = dice.angle(), far = radius * dice.between(0.2, 0.4);
                lobes.add(column(dice, 0, 0, height, radius, dice.between(0.6, 0.9), far * Math.cos(lean), far * Math.sin(lean), dice.between(2.6, 3.4),
                        dice.between(0.06, 0.12), 0, dice.between(0.16, 0.26), dice.between(0.1, 0.15), dice.whole(5, 7), dice.between(0.12, 0.22)));
                int more = dice.chance(0.35) ? 2 : 1;
                double turn = dice.angle();
                for (int index = 0; index < more; index++) {
                    double angle = turn + Math.PI * index + dice.between(-0.6, 0.6), away = radius * dice.between(0.6, 0.75);
                    lobes.add(column(dice, away * Math.cos(angle), away * Math.sin(angle), height * dice.between(0.25, 0.42), radius * dice.between(0.45, 0.65),
                            dice.between(0.75, 1.0), 0, 0, 2.4, 0, 0, 0.1, 0.1, dice.whole(4, 6), 0.15));
                }
                yield new Body(0, 0, lobes, warp, 15, this.pieces(0.95 * radius, 1.4, Math.max(0.7, 0.11 * radius), 2.5), new Fracture(5, 1.6, this.chunkDepth, 0.12, 1.4),
                        0, 0, 0, this.roughness, 0.6, 0.7, 2, 0.2, 0.45, false, this.skirtWidth, 3, this.scree, seed);
            }
            case TOWER -> {
                double lean = dice.angle(), far = radius * dice.between(0.1, 0.25);
                lobes.add(column(dice, 0, 0, height, radius, dice.between(0.7, 0.98), far * Math.cos(lean), far * Math.sin(lean), dice.between(4.5, 6),
                        dice.between(0.04, 0.08), 0, dice.between(0.08, 0.15), dice.between(0.07, 0.11), dice.whole(5, 7), dice.between(0.1, 0.18)));
                if (dice.chance(0.6)) {
                    double angle = dice.angle(), away = 0.7 * radius;
                    lobes.add(column(dice, away * Math.cos(angle), away * Math.sin(angle), height * dice.between(0.25, 0.4), radius * dice.between(0.5, 0.6),
                            dice.between(0.8, 1.0), 0, 0, 3, 0, 0, 0.1, 0.1, dice.whole(4, 6), 0.15));
                }
                yield new Body(0, 0, lobes, warp, 14, this.pieces(radius, 1.6, Math.max(0.7, 0.11 * radius), 2.5), new Fracture(5, 1.8, this.chunkDepth, 0.12, 1.4),
                        0, 0, 0, this.roughness, 0.6, 0.8, 2, 0.25, 0.45, false, this.skirtWidth, 3, this.scree, seed);
            }
            case NEEDLE -> {
                double lean = dice.angle(), far = radius * dice.between(0.5, 1.2);
                lobes.add(column(dice, 0, 0, height, radius, dice.between(0.8, 1.0), far * Math.cos(lean), far * Math.sin(lean), dice.between(3, 4.5), 0, 0,
                        0, 0.05, dice.whole(4, 5), 0.15));
                yield new Body(0, 0, lobes, warp, 10, this.pieces(Math.max(3.5, 1.6 * radius), 2.2, 0.5, 2), new Fracture(3.5, 2.0, this.chunkDepth, 0.12, 1.2),
                        0, 0, 0, this.roughness, 0.35, 0.6, 1, 0.15, 0.3, false, this.skirtWidth, 1.5, this.scree, seed);
            }
            case SPIRE -> {
                double lean = dice.angle(), far = radius * dice.between(0.1, 0.3);
                lobes.add(column(dice, 0, 0, height, radius, dice.between(0.8, 1.08), far * Math.cos(lean), far * Math.sin(lean), dice.between(5.5, 7), 0.05, 0,
                        dice.between(0.1, 0.15), dice.between(0.04, 0.07), dice.whole(5, 7), 0.12));
                int more = dice.whole(1, 3);
                double turn = dice.angle();
                for (int index = 0; index < more; index++) {
                    double angle = turn + 2 * Math.PI * (index + dice.between(-0.25, 0.25)) / more, away = radius * dice.between(0.6, 0.8);
                    lobes.add(column(dice, away * Math.cos(angle), away * Math.sin(angle), height * dice.between(0.3, 0.6), radius * dice.between(0.45, 0.6),
                            dice.between(0.8, 1.0), 0, 0, 4, 0, 0, 0.1, 0.08, dice.whole(4, 6), 0.15));
                }
                yield new Body(0, 0, lobes, warp, 18, this.pieces(0.9 * radius, 1.8, Math.max(0.9, 0.1 * radius), 2.6), new Fracture(5.5, 1.8, this.chunkDepth, 0.12, 1.5),
                        radius * dice.between(0.9, 1.25), dice.between(0.3, 0.45), dice.between(0.9, 1.4), this.roughness, 0.55, 0.8, 2, 0.25, 0.4, false,
                        this.skirtWidth, 4, this.scree, seed);
            }
            case MASSIF -> {
                double lean = dice.angle();
                lobes.add(column(dice, 0, 0, height, radius * dice.between(0.6, 0.75), dice.between(0.85, 1.15), 0.1 * radius * Math.cos(lean),
                        0.1 * radius * Math.sin(lean), dice.between(7, 9), 0.04, 0, dice.between(0.08, 0.12), dice.between(0.05, 0.1), dice.whole(6, 8), 0.1));
                int more = dice.whole(2, 3);
                double turn = dice.angle();
                for (int index = 0; index < more; index++) {
                    double angle = turn + 2 * Math.PI * (index + dice.between(-0.2, 0.2)) / more, away = radius * dice.between(0.3, 0.45);
                    lobes.add(column(dice, away * Math.cos(angle), away * Math.sin(angle), height * dice.between(0.7, 0.95), radius * dice.between(0.5, 0.7),
                            dice.between(0.85, 1.15), 0, 0, dice.between(6, 8), 0.04, 0, dice.between(0.08, 0.12), dice.between(0.05, 0.1), dice.whole(5, 7), 0.1));
                }
                yield new Body(0, 0, lobes, warp, 24, this.pieces(0.6 * radius, 1.6, Math.max(1.1, 0.07 * radius), 2.8), new Fracture(6, 1.6, this.chunkDepth, 0.12, 1.5),
                        radius * dice.between(0.6, 0.85), dice.between(0.2, 0.35), dice.between(1.0, 1.6), this.roughness, 0.8, 0.9, 2, 0.25, 0.5, false,
                        this.skirtWidth, 5, this.scree, seed);
            }
        };
    }

    /**
     * A boulder that stands {@code height} above the ground: the upper half of an ellipsoid whose middle lies about at
     * the ground, so that its sides slope in from the foot up, cut by six to nine planes of which the last is its flat
     * top. The ellipsoid is a little larger than asked, as the planes take that much off again.
     */
    private static Lobe blob(Dice dice, double x, double z, double height, double radius) {
        double middle = height * dice.between(-0.05, 0.15);
        return new Lobe(true, x, z, 2.2 * (height - middle), middle, 1.1 * radius, dice.between(0.75, 1.15), dice.angle(), 0, 0, 1, 2, 2, 0, 0, 0, 0, 0,
                dice.whole(6, 9), dice.between(0.15, 0.3), dice.next());
    }

    private static Lobe column(Dice dice, double x, double z, double height, double radius, double aspect, double leanX, double leanZ, double cap,
                               double bulge, double foot, double lobing, double plateau, int facets, double facetDepth) {
        return new Lobe(false, x, z, height, 0, radius, aspect, dice.angle(), leanX, leanZ, 1.3, cap, 2, bulge, 0.3, foot, lobing, plateau,
                facets, facetDepth, dice.next());
    }

    /** Numbers drawn from a seed, one after another. */
    private static final class Dice {
        private final long seed;
        private int rolls;

        Dice(long seed) {
            this.seed = seed;
        }

        long next() {
            return RockNoise.hash(this.seed, ++this.rolls);
        }

        double between(double from, double to) {
            return from + (to - from) * RockNoise.unit(this.next());
        }

        int whole(int from, int to) {
            return from + (int) ((to - from + 1) * RockNoise.unit(this.next()));
        }

        double angle() {
            return this.between(0, 2 * Math.PI);
        }

        boolean chance(double chance) {
            return RockNoise.unit(this.next()) < chance;
        }
    }
}
