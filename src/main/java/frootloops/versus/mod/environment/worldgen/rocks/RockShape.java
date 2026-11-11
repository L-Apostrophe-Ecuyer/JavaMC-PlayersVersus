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
    /** Two or three rounded stones against each other, part in the ground, with a low flat one or two beside them. */
    BOULDER(5, 9, 3.5, 5, 2.0, 0.5, 0.15, 0, 0.3, 0, 0, 0.35),
    /** A heap of boulders: a large one with smaller ones and flat ones against it. */
    PILE(4, 7, 4.5, 6, 1.6, 0.5, 0.15, 0, 0.25, 0, 0, 0.5),
    /** A small angular outcrop: two or three upright lobes with flat sides. */
    CRAG(8, 13, 3.5, 5, 1.36, 0.8, 0.4, 0.2, 0.2, 0.5, 0, 0.6),
    /** A rounded hill of rock, about as tall as it is wide, with a wide overgrown top. */
    DOME(10, 18, 9, 14, 1.4, 1.5, 0.4, 0.2, 0.15, 0.3, 5, 0.5),
    /** A leaning egg of rock: one side upright, the other a slope down to a lower lobe, overgrown from the top down. */
    MONOLITH(26, 38, 8, 11, 2.3, 1.4, 0.2, 0.12, 0.12, 0.4, 10, 0.6),
    /** An upright tower with a flat overgrown top and fluted sides. */
    TOWER(30, 46, 7, 10, 1.48, 1.2, 0.2, 0.12, 0.12, 1.4, 7, 0.5),
    /** A thin, leaning pinnacle. */
    NEEDLE(18, 34, 2.5, 4.5, 2.25, 0.6, 0.25, 0.15, 0.15, 0.5, 3, 0.4),
    /** A great spire: one fluted column with a flat overgrown top, a needle standing beside it. */
    SPIRE(55, 85, 10, 14, 2.3, 1.2, 0.25, 0.12, 0.12, 1.2, 9, 0.6),
    /** A wide mass with upright fluted cliffs and several tops. */
    MASSIF(38, 56, 18, 26, 1.45, 3, 0.5, 0.2, 0.12, 1.0, 10, 0.6);

    /** The sizes a shape looks right at. */
    public final double minHeight, maxHeight, minRadius, maxRadius;
    /** How far its lobes reach from its middle, as a multiple of its radius. */
    private final double spread;
    /** The most its outline is pushed out by its bulges, its pieces and its chunks. */
    private final double warp, pieceDepth, chunkDepth, roughness;
    /** How deep the grooves down its faces are. */
    private final double flutes;
    private final double skirtWidth, scree;

    RockShape(double minHeight, double maxHeight, double minRadius, double maxRadius, double spread, double warp, double pieceDepth, double chunkDepth,
              double roughness, double flutes, double skirtWidth, double scree) {
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.spread = spread;
        this.warp = warp;
        this.pieceDepth = pieceDepth;
        this.chunkDepth = chunkDepth;
        this.roughness = roughness;
        this.flutes = flutes;
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

    /**
     * The few large pieces a body is broken into: about its radius across, each standing a little in or out, with a
     * crack along some of the borders between them.
     */
    private Fracture pieces(double size, double tall, double gape) {
        return new Fracture(size, tall, this.pieceDepth, this == BOULDER || this == PILE ? 0.18 : 0.3, gape);
    }

    /** The small plates a face is broken into: a faint relief, with a rare crack. */
    private Fracture chunks(double size, double tall) {
        return new Fracture(size, tall, this.chunkDepth, 0.08, 1.2);
    }

    /** A body of this shape, {@code height} blocks tall and {@code radius} across at its widest lobe. */
    public Body body(long seed, double height, double radius) {
        Dice dice = new Dice(seed);
        List<Lobe> lobes = new ArrayList<>();
        double warp = this.warp(radius);
        return switch (this) {
            case BOULDER -> {
                lobes.add(mound(dice, 0, 0, height, radius));
                // A smaller stone or two lean against it, far enough out to be stones of their own
                int more = dice.whole(1, 2);
                double turn = dice.angle();
                for (int index = 0; index < more; index++) {
                    double size = dice.between(0.5, 0.7), angle = turn + 2.2 * index + dice.between(-0.4, 0.4), away = radius * (1 + size) * dice.between(0.88, 1.02);
                    lobes.add(mound(dice, away * Math.cos(angle), away * Math.sin(angle), height * dice.between(0.55, 0.85), radius * size));
                }
                plates(dice, lobes, dice.whole(0, 2), radius, turn + Math.PI);
                yield new Body(0, 0, fit(lobes, this.spread * radius), warp, Math.max(4, radius), this.pieces(Math.max(3.5, 1.2 * radius), 0.85, 1.2), Fracture.NONE,
                        0, 0, 0, this.roughness, this.flutes, Math.max(3.2, 0.32 * radius), 0.14, 0.3, 1, 0.03, 0.3, true, 0, 0, this.scree, seed);
            }
            case PILE -> {
                lobes.add(mound(dice, 0, 0, height, 0.7 * radius));
                int more = dice.whole(3, 5);
                double turn = dice.angle();
                for (int index = 0; index < more; index++) {
                    double size = dice.between(0.3, 0.5), angle = turn + 2 * Math.PI * (index + dice.between(-0.3, 0.3)) / more;
                    double away = radius * (0.7 + size) * dice.between(0.7, 0.9);
                    lobes.add(mound(dice, away * Math.cos(angle), away * Math.sin(angle), height * dice.between(0.4, 0.75), radius * size));
                }
                plates(dice, lobes, dice.whole(1, 3), radius, turn);
                yield new Body(0, 0, fit(lobes, this.spread * radius), warp, Math.max(4, radius), this.pieces(Math.max(3.5, 0.8 * radius), 0.85, 1.2), Fracture.NONE,
                        0, 0, 0, this.roughness, this.flutes, Math.max(3.2, 0.32 * radius), 0.18, 0.3, 1, 0.04, 0.3, true, 0, 0, this.scree, seed);
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
                yield new Body(0, 0, lobes, warp, 8, this.pieces(Math.max(3, 0.9 * radius), 1.3, 2), this.chunks(3, 1.3),
                        0, 0, 0, this.roughness, this.flutes, Math.max(3.2, 0.32 * radius), 0.45, 0.5, 2, 0.1, 0.35, true, 0, 0, this.scree, seed);
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
                yield new Body(0, 0, lobes, warp, Math.max(10, radius), this.pieces(0.8 * radius, 0.9, 2.2), this.chunks(4.5, 0.9),
                        0, 0, 0, this.roughness, this.flutes, Math.max(3.2, 0.32 * radius), 0.8, 0.5, 3, 0.12, 0.45, false, this.skirtWidth, 2, this.scree, seed);
            }
            case MONOLITH -> {
                double lean = dice.angle(), far = radius * dice.between(0.65, 0.9);
                lobes.add(column(dice, 0, 0, height, radius, dice.between(0.65, 0.9), far * Math.cos(lean), far * Math.sin(lean), dice.between(2.2, 2.8),
                        dice.between(0.12, 0.2), -dice.between(0.04, 0.1), dice.between(0.22, 0.32), dice.between(0.1, 0.14), dice.whole(5, 7), dice.between(0.08, 0.15)));
                // The lower lobe on the side the top leans away from: the slope the plants climb
                int more = dice.chance(0.35) ? 2 : 1;
                for (int index = 0; index < more; index++) {
                    double angle = lean + Math.PI + dice.between(-0.6, 0.6) + (index == 0 ? 0 : (dice.chance(0.5) ? 1.4 : -1.4));
                    double away = radius * dice.between(0.78, 0.92);
                    lobes.add(column(dice, away * Math.cos(angle), away * Math.sin(angle), height * dice.between(0.3, 0.45), radius * dice.between(0.48, 0.62),
                            dice.between(0.75, 1.0), 0, 0, 2.4, 0, 0, 0.1, 0.1, dice.whole(4, 6), 0.12));
                }
                yield new Body(0, 0, lobes, warp, 15, this.pieces(0.95 * radius, 1.4, 2.2), this.chunks(5, 1.6),
                        0, 0, 0, this.roughness, this.flutes, Math.max(3.2, 0.32 * radius), 0.95, 1.3, 5, 0.2, 0.5, false, this.skirtWidth, 6, this.scree, seed);
            }
            case TOWER -> {
                double lean = dice.angle(), far = radius * dice.between(0.1, 0.25);
                lobes.add(column(dice, 0, 0, height, radius, dice.between(0.7, 0.98), far * Math.cos(lean), far * Math.sin(lean), dice.between(6, 8),
                        dice.between(0.04, 0.08), 0, dice.between(0.08, 0.15), dice.between(0.07, 0.11), dice.whole(5, 7), dice.between(0.1, 0.18)));
                if (dice.chance(0.6)) {
                    double angle = dice.angle(), away = 0.7 * radius;
                    lobes.add(column(dice, away * Math.cos(angle), away * Math.sin(angle), height * dice.between(0.25, 0.4), radius * dice.between(0.5, 0.6),
                            dice.between(0.8, 1.0), 0, 0, 3, 0, 0, 0.1, 0.1, dice.whole(4, 6), 0.15));
                }
                yield new Body(0, 0, lobes, warp, 14, this.pieces(radius, 1.6, 2.5), this.chunks(5, 1.8),
                        0, 0, 0, this.roughness, this.flutes, Math.max(3.2, 0.32 * radius), 0.75, 0.9, 4, 0.25, 0.45, false, this.skirtWidth, 2.5, this.scree, seed);
            }
            case NEEDLE -> {
                double lean = dice.angle(), far = radius * dice.between(0.5, 1.2);
                lobes.add(column(dice, 0, 0, height, radius, dice.between(0.8, 1.0), far * Math.cos(lean), far * Math.sin(lean), dice.between(3, 4.5), 0, 0,
                        0, 0.05, dice.whole(4, 5), 0.15));
                yield new Body(0, 0, lobes, warp, 10, this.pieces(Math.max(3.5, 1.6 * radius), 2.2, 2), this.chunks(3.5, 2),
                        0, 0, 0, this.roughness, this.flutes, Math.max(3.2, 0.32 * radius), 0.35, 0.6, 1, 0.15, 0.3, false, this.skirtWidth, 1.5, this.scree, seed);
            }
            case SPIRE -> {
                double lean = dice.angle(), far = radius * dice.between(0.05, 0.2);
                lobes.add(column(dice, 0, 0, height, radius, dice.between(0.8, 1.05), far * Math.cos(lean), far * Math.sin(lean), dice.between(8, 11), 0.04, 0.12,
                        dice.between(0.08, 0.14), dice.between(0.05, 0.08), dice.whole(5, 7), 0.1));
                // A needle beside it, a gap of a few blocks between them above their feet
                double angle = dice.angle(), away = radius * dice.between(1.4, 1.55);
                lobes.add(column(dice, away * Math.cos(angle), away * Math.sin(angle), height * dice.between(0.4, 0.62), radius * dice.between(0.28, 0.38),
                        dice.between(0.85, 1.0), 0, 0, dice.between(2.4, 3.2), 0, 0.1, 0.08, 0.02, dice.whole(4, 5), 0.12));
                // Now and then a low shoulder on another side
                if (dice.chance(0.5)) {
                    double side = angle + Math.PI + dice.between(-0.8, 0.8), out = radius * dice.between(0.6, 0.75);
                    lobes.add(column(dice, out * Math.cos(side), out * Math.sin(side), height * dice.between(0.15, 0.3), radius * dice.between(0.45, 0.6),
                            dice.between(0.8, 1.0), 0, 0, 2.4, 0, 0, 0.1, 0.1, dice.whole(4, 6), 0.15));
                }
                yield new Body(0, 0, lobes, warp, 18, this.pieces(0.9 * radius, 1.8, 2.6), this.chunks(5.5, 1.8),
                        0, 0, 0, this.roughness, this.flutes, Math.max(3.2, 0.32 * radius), 0.7, 0.9, 3, 0.25, 0.4, false,
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
                yield new Body(0, 0, lobes, warp, 24, this.pieces(0.6 * radius, 1.6, 2.8), this.chunks(6, 1.6),
                        radius * dice.between(0.8, 1.1), dice.between(0.35, 0.5), dice.between(0.7, 1.2), this.roughness, this.flutes, Math.max(3.2, 0.32 * radius), 0.85, 0.9, 4, 0.25, 0.5, false,
                        this.skirtWidth, 3, this.scree, seed);
            }
        };
    }

    /**
     * A rounded stone that stands {@code height} above the ground: a column whose sides draw in from the foot up to a
     * blunt point, a rounded cone (a low {@code shoulder} and {@code cap}), as a boulder half sunk in the ground is;
     * a little out of round, its top off its middle.
     */
    private static Lobe mound(Dice dice, double x, double z, double height, double radius) {
        double lean = dice.angle(), far = radius * dice.between(0.15, 0.4);
        return new Lobe(false, x, z, height, 0, radius, dice.between(0.8, 1.2), dice.angle(), far * Math.cos(lean), far * Math.sin(lean), 1.2,
                dice.between(1.4, 1.7), 1.3, 0, 0.3, 0, dice.between(0.06, 0.16), 0, 0, 0, dice.next());
    }

    /** Low flat stones beside a boulder: a block or two high, cut to angular plates; slabs make their layers thin. */
    private static void plates(Dice dice, List<Lobe> lobes, int count, double radius, double turn) {
        for (int index = 0; index < count; index++) {
            double size = radius * dice.between(0.4, 0.6), angle = turn + 1.9 * index + dice.between(-0.5, 0.5), away = radius * dice.between(0.9, 1.25);
            double height = dice.between(1.2, 2.2);
            lobes.add(new Lobe(true, away * Math.cos(angle), away * Math.sin(angle), 2.2 * height, 0.05, 1.05 * size, dice.between(0.7, 1.3), dice.angle(),
                    0, 0, 1, 2, 2, 0, 0, 0, 0, 0, dice.whole(5, 7), dice.between(0.15, 0.3), dice.next()));
        }
    }

    /**
     * The lobes moved so that the rectangle around them has its middle at the body's, and drawn in toward it if they
     * still reach further than {@code limit}: a feature's rock must fit around its origin.
     */
    private static List<Lobe> fit(List<Lobe> lobes, double limit) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (Lobe lobe : lobes) {
            double reach = lobe.radius() * Math.max(1, lobe.aspect());
            minX = Math.min(minX, lobe.x() - reach);
            maxX = Math.max(maxX, lobe.x() + reach);
            minZ = Math.min(minZ, lobe.z() - reach);
            maxZ = Math.max(maxZ, lobe.z() + reach);
        }
        double middleX = (minX + maxX) / 2, middleZ = (minZ + maxZ) / 2;
        double scale = 1;
        for (Lobe lobe : lobes) {
            double reach = lobe.radius() * Math.max(1, lobe.aspect());
            double far = Math.max(Math.abs(lobe.x() - middleX), Math.abs(lobe.z() - middleZ));
            if (far + reach > limit && far > 0) scale = Math.min(scale, Math.max(limit - reach, 0) / far);
        }
        List<Lobe> fitted = new ArrayList<>();
        for (Lobe lobe : lobes) {
            fitted.add(new Lobe(lobe.blob(), (lobe.x() - middleX) * scale, (lobe.z() - middleZ) * scale, lobe.height(), lobe.centerY(), lobe.radius(),
                    lobe.aspect(), lobe.rotation(), lobe.leanX(), lobe.leanZ(), lobe.leanCurve(), lobe.cap(), lobe.shoulder(), lobe.bulge(), lobe.bulgeAt(),
                    lobe.foot(), lobe.lobing(), lobe.plateau(), lobe.facets(), lobe.facetDepth(), lobe.seed()));
        }
        return fitted;
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
