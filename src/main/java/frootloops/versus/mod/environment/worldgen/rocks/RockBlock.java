package frootloops.versus.mod.environment.worldgen.rocks;

import java.util.ArrayList;
import java.util.List;

/**
 * One block of a rock: a box whose faces are a little off square, its edges and corners cut off by planes, its big faces
 * bent back above a line partway up. Its planes are in its own frame; it has a centre and an orientation in the rock's.
 * Its distance is the planes' soft maximum, so that its edges are barely rounded: a rock is a few of these, angular,
 * never noise.
 */
public final class RockBlock {

    /** How much the planes' soft maximum rounds an edge, in blocks. */
    static final double SOFT = 0.4;

    private final double cx, cy, cz, reach;
    private final double[] planes, orientation;
    private final int group;

    /**
     * @param planes      {nx, ny, nz, d, ...} in the block's own frame, unit normals, inside where n . p < d; the first six
     *                    its faces (x, y, z, each its negative side first), as {@link #planes} makes them
     * @param orientation row-major 3 by 3, its columns the block's axes in the rock's frame
     * @param group       -1, or the column it is part of: the joints inside a column are never drawn
     */
    public RockBlock(double cx, double cy, double cz, double[] planes, double[] orientation, int group) {
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.planes = planes;
        this.orientation = orientation;
        this.group = group;
        this.reach = corner(planes) + 2;
    }

    /**
     * A block standing at (x, z): from {@code sink} into the ground up to {@code tall} above it (its planes made with
     * half the sum as their height), turned by {@code yaw}, then tipped by {@code tip} radians toward the level direction
     * {@code tipToward} about its foot.
     */
    public static RockBlock standing(double x, double z, double tall, double sink, double[] planes, double yaw, double tipToward, double tip, int group) {
        double half = (tall + sink) / 2;
        RockBlock block = new RockBlock(x, tall - half, z, planes, level(yaw), group);
        return tip == 0 ? block : block.turned(tip(tipToward, tip), x, -sink, z);
    }

    public double cx() {
        return this.cx;
    }

    public double cy() {
        return this.cy;
    }

    public double cz() {
        return this.cz;
    }

    /** How far from its centre any of it lies, and two blocks more. */
    public double reach() {
        return this.reach;
    }

    public int group() {
        return this.group;
    }

    /** About the distance to its surface in blocks, negative inside: the planes' soft maximum. */
    public double distance(double x, double y, double z) {
        return this.distance(x, y, z, Double.MAX_VALUE);
    }

    /**
     * As {@link #distance(double, double, double)} within {@code near} of its surface; further out or in, the planes' hard
     * maximum, which is as good there and much cheaper.
     */
    double distance(double x, double y, double z, double near) {
        double px = x - this.cx, py = y - this.cy, pz = z - this.cz;
        double[] o = this.orientation, p = this.planes;
        double ux = o[0] * px + o[3] * py + o[6] * pz, uy = o[1] * px + o[4] * py + o[7] * pz, uz = o[2] * px + o[5] * py + o[8] * pz;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < p.length; i += 4) max = Math.max(max, p[i] * ux + p[i + 1] * uy + p[i + 2] * uz - p[i + 3]);
        if (Math.abs(max) >= near) return max;
        double sum = 0;
        for (int i = 0; i < p.length; i += 4) sum += Math.exp((p[i] * ux + p[i + 1] * uy + p[i + 2] * uz - p[i + 3] - max) / SOFT);
        return max + SOFT * Math.log(sum);
    }

    /** This block turned by {@code turn} about the point ({@code px}, {@code py}, {@code pz}). */
    public RockBlock turned(double[] turn, double px, double py, double pz) {
        double dx = this.cx - px, dy = this.cy - py, dz = this.cz - pz;
        return new RockBlock(px + turn[0] * dx + turn[1] * dy + turn[2] * dz, py + turn[3] * dx + turn[4] * dy + turn[5] * dz,
                pz + turn[6] * dx + turn[7] * dy + turn[8] * dz, this.planes, multiply(turn, this.orientation), this.group);
    }

    /**
     * The planes of a block of half sizes ({@code a}, {@code b}, {@code c}): its six faces, each tipped off its axis by up
     * to {@code skew}; {@code cuts} planes across its edges and corners, cutting in to {@code depth} of the way out (the
     * lower, the deeper), deeper still on its upper half by {@code crown}, where it narrows upward ({@code upright}: mostly
     * across its upright edges, a prism's); and {@code bends} of its upright faces leaning back above a line partway up.
     */
    public static double[] planes(RockDice dice, double a, double b, double c, int cuts, double depth, double crown, double skew, boolean upright, int bends) {
        List<double[]> planes = new ArrayList<>();
        double[] half = {a, b, c};
        for (int axis = 0; axis < 3; axis++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                double[] n = new double[3];
                n[axis] = sign;
                for (int other = 0; other < 3; other++) if (other != axis) n[other] = dice.between(-skew, skew);
                normalize(n);
                planes.add(new double[]{n[0], n[1], n[2], half[axis] * Math.abs(n[axis])});
            }
        }
        for (int cut = 0; cut < cuts; cut++) {
            double sx = dice.chance(0.5) ? -1 : 1, sz = dice.chance(0.5) ? -1 : 1, sy = dice.chance(0.5 + 0.35 * crown) ? 1 : -1;
            double rx = dice.between(0.35, 1) / a, ry = dice.between(0.35, 1) / b, rz = dice.between(0.35, 1) / c;
            // Across an edge (two of the axes) or a corner (all three)
            double pick = dice.unit();
            if (upright) {
                if (pick < 0.7) ry = 0;
            } else if (pick < 0.25) {
                rx = 0;
            } else if (pick < 0.5) {
                rz = 0;
            } else if (pick < 0.65) {
                ry = 0;
            }
            double[] n = {sx * rx, sy * ry, sz * rz};
            normalize(n);
            double support = a * Math.abs(n[0]) + b * Math.abs(n[1]) + c * Math.abs(n[2]);
            double share = dice.between(depth, depth + 0.15) - (n[1] > 0.3 ? 0.25 * crown : 0);
            planes.add(new double[]{n[0], n[1], n[2], support * Math.max(0.35, share)});
        }
        for (int bend = 0; bend < bends; bend++) {
            // The broader faces more often
            int axis = dice.chance(c / (a + c)) ? 0 : 2;
            double sign = dice.chance(0.5) ? -1 : 1, lean = dice.between(0.12, 0.32), at = dice.between(0.2, 0.6), out = axis == 0 ? a : c;
            double[] n = new double[3];
            n[axis] = sign * Math.cos(lean);
            n[1] = Math.sin(lean);
            planes.add(new double[]{n[0], n[1], n[2], Math.cos(lean) * out + Math.sin(lean) * (-b + 2 * b * at)});
        }
        double[] flat = new double[planes.size() * 4];
        for (int index = 0; index < planes.size(); index++) System.arraycopy(planes.get(index), 0, flat, index * 4, 4);
        return flat;
    }

    /** About the vertical: local x goes to (cos yaw, 0, sin yaw), local z to (-sin yaw, 0, cos yaw). */
    public static double[] level(double yaw) {
        double c = Math.cos(yaw), s = Math.sin(yaw);
        return new double[]{c, 0, -s, 0, 1, 0, s, 0, c};
    }

    /** About the level axis square to {@code toward}: tips up (+y) toward (cos toward, 0, sin toward) by {@code angle}. */
    public static double[] tip(double toward, double angle) {
        double ax = -Math.sin(toward), az = Math.cos(toward);
        double cos = Math.cos(angle), sin = -Math.sin(angle), t = 1 - cos;
        return new double[]{
                t * ax * ax + cos, -sin * az, t * ax * az,
                sin * az, cos, -sin * ax,
                t * ax * az, sin * ax, t * az * az + cos};
    }

    public static double[] multiply(double[] a, double[] b) {
        double[] m = new double[9];
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                m[row * 3 + column] = a[row * 3] * b[column] + a[row * 3 + 1] * b[3 + column] + a[row * 3 + 2] * b[6 + column];
            }
        }
        return m;
    }

    private static void normalize(double[] n) {
        double length = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        for (int i = 0; i < 3; i++) n[i] /= length;
    }

    /** How far from the centre the farthest corner of the box its six faces make lies: the block is inside that box. */
    private static double corner(double[] planes) {
        if (planes.length < 24) {
            double most = 0;
            for (int i = 3; i < planes.length; i += 4) most = Math.max(most, Math.abs(planes[i]));
            return most * Math.sqrt(3);
        }
        double far = 0;
        for (int corner = 0; corner < 8; corner++) {
            double[] x = face(planes, (corner & 1)), y = face(planes, 2 + (corner >> 1 & 1)), z = face(planes, 4 + (corner >> 2 & 1));
            // Where the three planes meet (Cramer's rule)
            double det = x[0] * (y[1] * z[2] - y[2] * z[1]) - x[1] * (y[0] * z[2] - y[2] * z[0]) + x[2] * (y[0] * z[1] - y[1] * z[0]);
            double px = x[3] * (y[1] * z[2] - y[2] * z[1]) - x[1] * (y[3] * z[2] - y[2] * z[3]) + x[2] * (y[3] * z[1] - y[1] * z[3]);
            double py = x[0] * (y[3] * z[2] - y[2] * z[3]) - x[3] * (y[0] * z[2] - y[2] * z[0]) + x[2] * (y[0] * z[3] - y[3] * z[0]);
            double pz = x[0] * (y[1] * z[3] - y[3] * z[1]) - x[1] * (y[0] * z[3] - y[3] * z[0]) + x[3] * (y[0] * z[1] - y[1] * z[0]);
            far = Math.max(far, Math.sqrt(px * px + py * py + pz * pz) / Math.abs(det));
        }
        return far;
    }

    private static double[] face(double[] planes, int index) {
        return new double[]{planes[index * 4], planes[index * 4 + 1], planes[index * 4 + 2], planes[index * 4 + 3]};
    }
}
