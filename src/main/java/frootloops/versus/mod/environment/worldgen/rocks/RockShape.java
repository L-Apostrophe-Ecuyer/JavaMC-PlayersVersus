package frootloops.versus.mod.environment.worldgen.rocks;

import frootloops.versus.mod.environment.worldgen.rocks.RockPlan.Body;

import java.util.ArrayList;
import java.util.List;

/**
 * The kinds of rock: each puts angular blocks together its own way for a height and a radius, drawing the rest from the
 * seed. Interlacing blocks some 8 to 16 across, faces flat, corners and edges cut, some faces bent back; tall blocks
 * propped at their foot by smaller ones; the foot curving out into the ground.
 *
 * <p>Each shape's own range of sizes is what it looks right at; the small ones fit a feature (everything within 16
 * blocks of its origin), the large ones are for formations.
 */
public enum RockShape {
    /** A big block narrowing upward, one or two smaller ones leaning on it, up to two flat stones at its foot. */
    BOULDER(6, 10, 3, 5, 0.6),
    /** Layers of flat blocks, each layer narrower than the one under it, lying over each other: a cone as tall as wide. */
    PILE(5, 9, 5, 9, 0.6),
    /** A few upright columns close together. */
    CRAG(8, 13, 3.5, 5, 0.5),
    /** A boulder low and wide. */
    DOME(5, 8, 6, 9, 0.6),
    /** A tall leaning mass and a lower shoulder against it, opposite its lean; its radius is its width. */
    MONOLITH(26, 38, 8, 11, 0.85),
    /** Columns of blocks between upright joints, each column its own height, lower outward, its top block cut at angles. */
    TOWER(30, 46, 7, 10, 0.75),
    /** One narrow column. */
    NEEDLE(18, 34, 2.5, 4.5, 0.6),
    /** A tower with a needle beside it. */
    SPIRE(55, 85, 10, 14, 0.75),
    /** A wide mass of columns: many tops. */
    MASSIF(38, 56, 18, 26, 0.8);

    /** The sizes a shape looks right at. */
    public final double minHeight, maxHeight, minRadius, maxRadius;
    /** How much of its level tops is overgrown, unless a template says. */
    private final double cover;

    /** How much of its level tops is overgrown, unless a template says. */
    public double cover() {
        return this.cover;
    }

    RockShape(double minHeight, double maxHeight, double minRadius, double maxRadius, double cover) {
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.cover = cover;
    }

    /**
     * How far from its middle, along x or z, a body of this shape {@code height} tall and {@code radius} across reaches
     * at most, whatever its seed: its rock, and the ground risen around its foot.
     */
    public int reach(double height, double radius) {
        return (int) Math.ceil(this.spread(height, radius) + RockModel.riseReach(height));
    }

    /**
     * How far from its middle its rock reaches at most, along x or z, at its foot or anywhere: the farthest any of
     * hundreds of seeds reached, from the smallest to the largest of its sizes (and twice a boulder's), and a block more.
     */
    private double spread(double height, double radius) {
        return switch (this) {
            case BOULDER, DOME -> 2.1 * radius + 1.5;
            case PILE -> 1.35 * radius + 1;
            case CRAG -> radius + 6.5;
            case MONOLITH -> 1.2 * radius + 0.05 * height + 4.5;
            case TOWER -> 2 * radius + 4;
            case NEEDLE -> 2 * radius + 5;
            case SPIRE -> 1.6 * radius + 9;
            case MASSIF -> 1.15 * radius + 11.5;
        };
    }

    /** A body of this shape, {@code height} blocks tall and {@code radius} across. */
    public Body body(long seed, double height, double radius) {
        RockDice dice = new RockDice(seed);
        Blocks rock = new Blocks();
        return switch (this) {
            case BOULDER, DOME -> {
                boulder(dice, rock, height, radius);
                rock.props(dice, 0.55);
                yield rock.body(0.6, 1, 3, this.cover, height, seed);
            }
            case PILE -> {
                pile(dice, rock, height, radius, 3);
                yield rock.body(0.5, 0.5, 2, this.cover, height, seed);
            }
            case CRAG -> {
                castle(dice, rock, height, radius, 0.9 * radius, 6, 0.5);
                rock.props(dice, 0.85);
                // A boulder's foot: it is no taller than one
                yield rock.body(0.6, 1, 3, this.cover, height, seed);
            }
            case TOWER, MASSIF -> {
                if (this == TOWER) castle(dice, rock, height, radius, 0.9 * radius, 14, 0.4);
                else castle(dice, rock, height, radius, 10, 16, 0.8);
                rock.props(dice, 0.85);
                yield rock.body(0.6, 3, 9, this.cover, height, seed);
            }
            case NEEDLE -> {
                column(dice, rock, 0, 0, radius * dice.between(0.95, 1.05), radius * dice.between(0.75, 0.95), dice.angle(), height * dice.between(0.92, 1.0), 12, 0, true);
                rock.props(dice, 0.85);
                yield rock.body(0.6, 3, 9, this.cover, height, seed);
            }
            case SPIRE -> {
                castle(dice, rock, height, radius, 0.9 * radius, 16, 0.55);
                // A needle beside it
                double angle = dice.angle(), away = 1.2 * radius, half = radius * dice.between(0.25, 0.35);
                column(dice, rock, away * Math.cos(angle), away * Math.sin(angle), half, half * dice.between(0.8, 1.0), dice.angle(), height * dice.between(0.5, 0.7), 12,
                        NEEDLE_GROUP, true);
                rock.props(dice, 0.85);
                yield rock.body(0.6, 3, 9, this.cover, height, seed);
            }
            case MONOLITH -> {
                monolith(dice, rock, height, radius);
                rock.props(dice, 0.9);
                yield rock.body(0.8, 3, 9, this.cover, height, seed);
            }
        };
    }

    /** The least half size across of a small block: the planes' soft maximum rounds a smaller one away to a speck. */
    private static final double SMALLEST = 1.3;

    /** The spire's needle's column: none of a castle's. */
    private static final int NEEDLE_GROUP = 1_000_000;

    /** A big angular block narrowing upward, one or two smaller ones leaning on it, a flat stone or two at its foot. */
    private static void boulder(RockDice dice, Blocks rock, double height, double radius) {
        double yaw = dice.between(0, Math.PI), mr = radius * dice.between(0.8, 1.0), sink = dice.between(1, 2), mz = mr * dice.between(0.7, 0.9);
        rock.stand(0, 0, height, sink, RockBlock.planes(dice, mr, (height + sink) / 2, mz, dice.whole(8, 12), dice.between(0.6, 0.72), 0.6, 0.12, false, dice.whole(1, 2)),
                mr, mz, yaw, dice.angle(), dice.between(0, 0.12));
        int more = dice.whole(1, 2);
        double turn = dice.angle();
        for (int index = 0; index < more; index++) {
            double angle = turn + index * dice.between(1.6, 2.6), size = dice.between(0.5, 0.72), away = mr * dice.between(0.95, 1.2);
            double tall = height * dice.between(0.55, 0.85), a = Math.max(SMALLEST, mr * size), c = Math.max(SMALLEST, a * dice.between(0.7, 0.95)), low = dice.between(1, 2);
            rock.stand(away * Math.cos(angle), away * Math.sin(angle), tall, low, RockBlock.planes(dice, a, (tall + low) / 2, c, dice.whole(6, 10), dice.between(0.6, 0.75), 0.5,
                    0.15, false, 1), a, c, dice.between(0, Math.PI), angle + Math.PI, dice.between(0.12, 0.35));
        }
        int plates = dice.whole(0, 2);
        for (int index = 0; index < plates; index++) {
            double angle = dice.angle(), away = mr * dice.between(0.9, 1.3), size = Math.max(SMALLEST, mr * dice.between(0.35, 0.55)), tall = dice.between(1.2, 2.4);
            double across = Math.max(SMALLEST, 0.75 * size);
            rock.stand(away * Math.cos(angle), away * Math.sin(angle), tall, 1, RockBlock.planes(dice, size, (tall + 1) / 2, across, dice.whole(3, 6), 0.75, 0, 0.08, false, 0),
                    size, across, dice.between(0, Math.PI), dice.angle(), dice.between(0, 0.1));
        }
    }

    /**
     * Flat angular stones in layers about {@code layer} thick, each layer's narrower than the one under it, lying over
     * and against each other; none so small that it doesn't make whole blocks.
     */
    private static void pile(RockDice dice, Blocks rock, double height, double radius, double layer) {
        double y = -dice.between(0.6, 1.2);
        for (int index = 0; index < 12; index++) {
            double thick = layer * dice.between(0.8, 1.2), mid = y + thick / 2;
            double reach = radius * (1 - mid / height);
            // Two layers at least: a small stone on the first, at least
            if (reach < 1.6) {
                if (index >= 2) break;
                reach = 1.6;
            }
            int count = Math.max(1, (int) Math.round(reach * reach / (radius * 1.1)) + dice.whole(-1, 1));
            double turn = dice.angle();
            for (int stone = 0; stone < count; stone++) {
                double angle = turn + 2 * Math.PI * (stone + dice.between(-0.3, 0.3)) / count;
                double away = count == 1 ? reach * dice.between(0, 0.25) : reach * dice.between(0.3, 0.6);
                double a = Math.max(SMALLEST, reach * dice.between(0.45, 0.65)), c = Math.max(SMALLEST, a * dice.between(0.6, 0.9)), b = thick / 2 + 0.4;
                double x = away * Math.cos(angle), z = away * Math.sin(angle);
                RockBlock flat = new RockBlock(x, mid, z, RockBlock.planes(dice, a, b, c, dice.whole(4, 8), dice.between(0.7, 0.82), 0.3, 0.08, false, 0),
                        RockBlock.level(dice.between(0, Math.PI)), -1);
                rock.blocks.add(flat.turned(RockBlock.tip(dice.angle(), dice.between(0, 0.12)), x, mid, z));
            }
            y += thick;
        }
    }

    /**
     * Castle-like: joints mostly upright, every {@code width} or so in two directions (a jittered grid), and across every
     * {@code joint} or so; each column of blocks its own height, lower outward ({@code flat}: how little lower).
     */
    private static void castle(RockDice dice, Blocks rock, double height, double radius, double width, double joint, double flat) {
        double yaw = dice.between(0, Math.PI), c = Math.cos(yaw), s = Math.sin(yaw);
        double[] us = joints(dice, -radius, radius, width), ws = joints(dice, -radius, radius, width);
        // Per column: x, z, half sizes, turn, top, group, whether propped
        List<double[]> columns = new ArrayList<>();
        double tallest = 0;
        for (int i = 0; i + 1 < us.length; i++) {
            for (int j = 0; j + 1 < ws.length; j++) {
                double mu = (us[i] + us[i + 1]) / 2, mw = (ws[j] + ws[j + 1]) / 2, r = Math.hypot(mu, mw) / radius;
                if (r > 0.8 + 0.35 * dice.unit()) continue;
                double top = height * (1 - (1 - flat) * Math.pow(Math.min(r, 1.2), 1.6)) * dice.between(0.72, 1.04);
                if (top < 3) continue;
                double hu = (us[i + 1] - us[i]) / 2 * dice.between(0.95, 1.1), hw = (ws[j + 1] - ws[j]) / 2 * dice.between(0.95, 1.1);
                double ou = dice.between(-0.1, 0.1) * width, ow = dice.between(-0.1, 0.1) * width;
                double x = c * (mu + ou) - s * (mw + ow), z = s * (mu + ou) + c * (mw + ow);
                columns.add(new double[]{x, z, hu, hw, yaw + dice.between(-0.1, 0.1), top, i * 1000 + j, r > 0.5 ? 1 : 0});
                tallest = Math.max(tallest, top);
            }
        }
        // The tallest column stands about as tall as the rock was asked to be, where no column stood in its middle; the
        // others as much lower than it as they were
        double scale = tallest > 0 ? height * dice.between(0.92, 1.0) / tallest : 1;
        for (double[] column : columns) {
            column(dice, rock, column[0], column[1], column[2], column[3], column[4], column[5] * scale, joint, (int) column[6], column[7] > 0);
        }
    }

    /** Joints across {@code from} to {@code to}, {@code spacing} apart give or take a third, from a little before to a little after. */
    private static double[] joints(RockDice dice, double from, double to, double spacing) {
        List<Double> joints = new ArrayList<>();
        double at = from - spacing * (0.5 + dice.unit());
        while (at < to + spacing) {
            joints.add(at);
            at += spacing * (1 + 0.35 * (2 * dice.unit() - 1));
        }
        return joints.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /**
     * A column of blocks {@code hu} by {@code hw} across, from a little under the ground up to {@code top}, a joint every
     * {@code joint} or so; each block reaches a little into the one under it, and only the top one is cut at angles.
     * {@code propped}: smaller blocks prop its foot.
     */
    private static void column(RockDice dice, Blocks rock, double x, double z, double hu, double hw, double turn, double top, double joint, int group, boolean propped) {
        double y = -dice.between(1.5, 3);
        while (y < top - 0.5) {
            double thick = joint * dice.between(0.7, 1.3);
            if (top - (y + thick) < 0.35 * joint) thick = top - y;
            boolean last = y + thick >= top - 0.01, first = y < 0;
            double shrink = dice.between(0.9, 1.0), a = hu * shrink, c = hw * shrink;
            double bottom = y - (y > 0 ? 1.5 : 0), half = (y + thick - bottom) / 2;
            double[] planes = last
                    ? RockBlock.planes(dice, a, half, c, dice.whole(5, 8), dice.between(0.62, 0.75), 0.9, 0.06, false, first ? dice.whole(1, 2) : 0)
                    : RockBlock.planes(dice, a, half, c, dice.whole(2, 4), dice.between(0.78, 0.9), 0, 0.03, true, first ? dice.whole(1, 2) : 0);
            if (first && propped) rock.feet.add(new double[]{x, z, a, c, turn, top});
            rock.blocks.add(new RockBlock(x, bottom + half, z, planes, RockBlock.level(turn), group));
            y += thick;
        }
    }

    /** A tall angular mass {@code width} across, a lower shoulder against it, both leaning away from the shoulder. */
    private static void monolith(RockDice dice, Blocks rock, double height, double width) {
        double yaw = dice.angle(), c = Math.cos(yaw), s = Math.sin(yaw);
        double hu = width / 2 * dice.between(0.9, 1.05), hw = width / 2 * dice.between(0.7, 0.85);
        // Two of its faces across from each other, bent back over twenty blocks and more, can meet well under its top: then
        // it is a wedge, not a tall mass. Drawn again (four times at most, the tallest kept) until it stands tall
        double[] planes = null;
        int tallest = Integer.MIN_VALUE;
        for (int attempt = 0; attempt < 4 && tallest < 0.85 * height; attempt++) {
            double[] drawn = RockBlock.planes(dice, hu, (height + 2) / 2, hw, dice.whole(9, 13), dice.between(0.62, 0.74), 0.7, 0.08, false, dice.whole(2, 3));
            int top = RockVoxels.of(List.of(RockBlock.standing(0, 0, height, 2, drawn, 0, 0, 0, -1)), 0, 0, 1, 0).top() + 1;
            if (top > tallest) {
                planes = drawn;
                tallest = top;
            }
        }
        rock.stand(0, 0, height, 2, planes, hu, hw, yaw, 0, 0);
        double shoulder = height * dice.between(0.5, 0.68), su = hu * dice.between(0.7, 0.85), sw = hw * dice.between(0.85, 1.05);
        double away = -(hu + su) * dice.between(0.7, 0.82), side = dice.between(-0.3, 0.3) * hw;
        rock.stand(c * away - s * side, s * away + c * side, shoulder, 2, RockBlock.planes(dice, su, (shoulder + 2) / 2, sw, dice.whole(8, 12), dice.between(0.6, 0.72), 0.7, 0.1,
                false, dice.whole(1, 2)), su, sw, yaw + dice.between(-0.2, 0.2), 0, 0);
        rock.lean(yaw, dice.between(0.16, 0.26));
    }

    /** The blocks a recipe puts down, and the tall ones standing on the ground, whose feet are propped. */
    private static final class Blocks {

        final List<RockBlock> blocks = new ArrayList<>();
        /** Per tall block standing on the ground: x, z, half sizes along its own x and z, its turn, its top. */
        final List<double[]> feet = new ArrayList<>();

        /** A block standing at (x, z), {@code sink} into the ground and {@code tall} above it, tipped by {@code tip} toward {@code toward}. */
        void stand(double x, double z, double tall, double sink, double[] planes, double halfX, double halfZ, double yaw, double toward, double tip) {
            this.blocks.add(RockBlock.standing(x, z, tall, sink, planes, yaw, toward, tip, -1));
            if (tall >= 6) this.feet.add(new double[]{x, z, halfX, halfZ, yaw, tall});
        }

        /** Every block turned about the middle of the ground, its top toward {@code toward} by {@code angle}. */
        void lean(double toward, double angle) {
            double[] turn = RockBlock.tip(toward, angle);
            this.blocks.replaceAll(block -> block.turned(turn, 0, 0, 0));
        }

        /** About the distance to the rock, before the blocks are joined: the nearest block's. */
        double nearest(double x, double y, double z) {
            double best = Double.MAX_VALUE;
            for (RockBlock block : this.blocks) best = Math.min(best, block.distance(x, y, z));
            return best;
        }

        /**
         * Smaller blocks propping the feet of the tall ones, each with {@code chance}: one to three each, two to four for
         * those 16 or more tall, leaning in against one of its faces (the broader more often), never where rock already is.
         */
        void props(RockDice dice, double chance) {
            List<RockBlock> props = new ArrayList<>();
            for (double[] foot : this.feet) {
                double x = foot[0], z = foot[1], hx = foot[2], hz = foot[3], yaw = foot[4], top = foot[5];
                boolean tall = top >= 16;
                int count = tall ? dice.whole(2, 4) : dice.whole(1, 3);
                for (int index = 0; index < count; index++) {
                    double size = Math.max(1.6, (tall ? dice.between(0.3, 0.5) : dice.between(0.22, 0.4)) * Math.max(hx, hz));
                    if (dice.unit() > chance) continue;
                    boolean across = dice.unit() < hz / (hx + hz);
                    double sign = dice.chance(0.5) ? -1 : 1, along = dice.between(-0.6, 0.6);
                    double lx = across ? sign * (hx + 0.3 * size) : along * hx, lz = across ? along * hz : sign * (hz + 0.3 * size);
                    double wx = x + Math.cos(yaw) * lx - Math.sin(yaw) * lz, wz = z + Math.sin(yaw) * lx + Math.cos(yaw) * lz;
                    if (this.nearest(wx, 1, wz) < -0.5 * size) continue;
                    double high = Math.clamp(top * (tall ? dice.between(0.18, 0.4) : dice.between(0.14, 0.3)), 2, 14), sink = 1.2, c = size * dice.between(0.6, 0.9);
                    props.add(RockBlock.standing(wx, wz, high, sink, RockBlock.planes(dice, size, (high + sink) / 2, c, dice.whole(6, 9), dice.between(0.6, 0.72), 0.6, 0.12,
                            false, 0), dice.between(0, Math.PI), Math.atan2(z - wz, x - wx), dice.between(0.1, 0.3), -1));
                }
            }
            this.blocks.addAll(props);
        }

        Body body(double blend, double flare, double flareHeight, double cover, double height, long seed) {
            return new Body(0, 0, this.blocks, blend, flare, flareHeight, cover, height, seed);
        }
    }
}
