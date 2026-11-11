package frootloops.versus.mod.environment.worldgen.rocks;

import frootloops.versus.mod.environment.worldgen.rocks.RockPlan.Body;
import frootloops.versus.mod.environment.worldgen.rocks.RockPlan.Fracture;
import frootloops.versus.mod.environment.worldgen.rocks.RockPlan.Lobe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * One body of a rock formation as blocks: which cells are rock and of what, the plants on it, the ground's rise around
 * its foot. Built from the body's numbers alone, so the same body is the same blocks wherever and in whatever order its
 * chunks are made.
 *
 * <p>The shape is a field over space, positive inside the rock and about the distance to its surface:
 * <ol>
 *   <li>the outline: the body's lobes fused together (each pulled out of round, cut by upright planes, its top cut
 *   level), pushed in and out by a smooth noise (its large bulges);</li>
 *   <li>pinnacles: a Voronoi diagram over the ground plan splits the body into columns, each with its own height, with
 *   clefts between them that open below the lower one's top and widen upward;</li>
 *   <li>pieces, then chunks: Voronoi diagrams in space at two scales break the body into blocks of rock that each
 *   stand a little out of the outline or back into it, with open cracks a block wide along the borders between them:
 *   the few large pieces make it look like two or three rocks together, the chunks break up its faces;</li>
 *   <li>roughness: a block-sized noise.</li>
 * </ol>
 * Cells whose middle is inside are rock. Then, in order: specks are removed and pinholes filled, whatever isn't joined
 * to the part in the ground is dropped, each cell gets its material, the level faces get their plant cover, moss hangs
 * a little down from its edges, slabs smooth the bare tops, and vines, plants and loose stones are listed.
 *
 * <p>The rock's three tones are a shading, as a builder would paint it: {@link RockMaterial#SHADE} lines the cracks (any
 * gap a block wide) and what is shut in or faces down; {@link RockMaterial#LIGHT} is in patches on what faces up, and
 * in streaks down the faces for texture; the rest, most of it, is {@link RockMaterial#BASE}.
 *
 * <p>Coordinates are the body's own: x and z from its middle, y from the ground there (0 is the first block above the
 * ground).
 */
public final class RockModel {

    public enum DecorationKind { VINE, PLANT, STONE }

    /**
     * Something on or around the rock. A vine hangs in this cell on the rock toward ({@code dx}, {@code dz}); a plant
     * stands in this cell; a stone lies on the ground of this column ({@code y} blocks above it), of {@code material}.
     */
    public record Decoration(int x, int y, int z, DecorationKind kind, int dx, int dz, int data, RockMaterial material) {
    }

    /** In {@link #lowest} and {@link #highest}: a column with no rock. */
    public static final int NONE = Integer.MIN_VALUE;
    /** The bit of a cell that makes it a slab of its material. */
    private static final int SLAB = 0x40;
    /**
     * Where the sun stands for the rocks' shading, level with the ground: to the south and a little east, in every
     * rock alike. The game lights north and south faces better than east and west ones; this turns one of the two
     * toward the light.
     */
    private static final double SUN_X = 0.38, SUN_Z = 0.92;
    /** How far lobes blend into each other where they meet, in blocks. */
    private static final double FUSE = 2.5;
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final Body body;
    private final long seed;
    private final double top;
    private final double reach;
    private final double tiltX, tiltZ;
    /** Per lobe: its facets' planes as (nx, ny, nz, offset as a share of the radius that way). */
    private final double[][] facets;
    /** Per lobe: where around it its two and its three swellings lie. */
    private final double[][] swells;
    private final long tallest;
    private final RockNoise.Cells plan = new RockNoise.Cells(), piece = new RockNoise.Cells(), chunk = new RockNoise.Cells(), column = new RockNoise.Cells();
    private double faceX, faceY, faceZ;
    /** The size across of the columns whose joints are the flutes, in blocks (0: none). */
    private final double flutes;

    public final int minX, minY, minZ, sizeX, sizeY, sizeZ;
    private final byte[] cells;
    private final int[] lowest, highest;

    /** The columns around the rock that the skirt and the scree reach: a wider rectangle than the rock's. */
    public final int groundMinX, groundMinZ, groundSizeX, groundSizeZ;
    private final byte[] rise;
    private final List<Decoration> decorations = new ArrayList<>();

    private RockModel(Body body) {
        this.body = body;
        this.seed = body.seed();
        double top = 0, maxRadius = 0;
        double x0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, z0 = Double.MAX_VALUE, z1 = -Double.MAX_VALUE, y0 = -2;
        this.reach = body.warp() + body.pieces().depth() + body.chunks().depth() + body.roughness() + 0.5;
        this.flutes = body.flutes() > 0 ? Math.max(1, body.fluteScale()) : 0;
        this.facets = new double[body.lobes().size()][];
        this.swells = new double[body.lobes().size()][];
        for (int index = 0; index < body.lobes().size(); index++) {
            Lobe lobe = body.lobes().get(index);
            double lobeTop = lobe.blob() ? lobe.centerY() + lobe.height() / 2 : lobe.height() * (1 - lobe.plateau());
            top = Math.max(top, lobeTop);
            double radius = lobe.radius() * Math.max(1, lobe.aspect()) * (1 + Math.max(lobe.bulge(), 0) + Math.max(lobe.foot(), 0) + lobe.lobing());
            this.swells[index] = new double[]{2 * Math.PI * RockNoise.unit(RockNoise.hash(lobe.seed(), 7)), 2 * Math.PI * RockNoise.unit(RockNoise.hash(lobe.seed(), 8))};
            maxRadius = Math.max(maxRadius, radius);
            x0 = Math.min(x0, Math.min(lobe.x(), lobe.x() + lobe.leanX()) - radius);
            x1 = Math.max(x1, Math.max(lobe.x(), lobe.x() + lobe.leanX()) + radius);
            z0 = Math.min(z0, Math.min(lobe.z(), lobe.z() + lobe.leanZ()) - radius);
            z1 = Math.max(z1, Math.max(lobe.z(), lobe.z() + lobe.leanZ()) + radius);
            if (lobe.blob()) y0 = Math.min(y0, lobe.centerY() - lobe.height() / 2 - this.reach - 1);
            this.facets[index] = facets(lobe);
        }
        this.top = top;
        // A column goes on into the ground, deeper the wider it is, so that it still stands where the ground falls away
        if (body.lobes().stream().anyMatch(lobe -> !lobe.blob())) y0 = Math.min(y0, -Math.clamp(2 + maxRadius / 2, 3, 8));
        double margin = Math.ceil(this.reach);
        this.minX = RockNoise.floor(x0 - margin);
        this.minZ = RockNoise.floor(z0 - margin);
        this.minY = RockNoise.floor(y0);
        this.sizeX = RockNoise.floor(x1 + margin) + 1 - this.minX;
        this.sizeZ = RockNoise.floor(z1 + margin) + 1 - this.minZ;
        this.sizeY = RockNoise.floor(top + this.reach + 2) + 1 - this.minY;
        this.cells = new byte[this.sizeX * this.sizeY * this.sizeZ];
        this.lowest = new int[this.sizeX * this.sizeZ];
        this.highest = new int[this.sizeX * this.sizeZ];
        // The rock's beds lean a little, their own way for each rock
        double lean = 0.12 * RockNoise.unit(RockNoise.hash(this.seed, 40)), turn = 2 * Math.PI * RockNoise.unit(RockNoise.hash(this.seed, 41));
        this.tiltX = lean * Math.cos(turn);
        this.tiltZ = lean * Math.sin(turn);
        // The pinnacle over the first lobe's top keeps the whole height
        long tallest = 0;
        if (body.pinnacles() > 0 && !body.lobes().isEmpty()) {
            Lobe first = body.lobes().getFirst();
            this.pinnacle(first.x() + first.leanX(), top, first.z() + first.leanZ());
            tallest = this.plan.id1;
        }
        this.tallest = tallest;

        int around = (int) Math.ceil(Math.max(body.skirtWidth(), body.scree() > 0 ? 3 : 0));
        this.groundMinX = this.minX - around;
        this.groundMinZ = this.minZ - around;
        this.groundSizeX = this.sizeX + 2 * around;
        this.groundSizeZ = this.sizeZ + 2 * around;
        this.rise = new byte[this.groundSizeX * this.groundSizeZ];
    }

    public static RockModel build(Body body) {
        RockModel model = new RockModel(body);
        if (body.lobes().isEmpty()) return model;
        BitSet solid = model.solid();
        model.tidy(solid);
        model.keepGrounded(solid);
        model.columns(solid);
        model.materials(solid);
        model.cover(solid);
        if (body.slabs()) model.slabs(solid);
        model.ground(solid);
        return model;
    }

    public Body body() {
        return this.body;
    }

    public List<Decoration> decorations() {
        return this.decorations;
    }

    /** How far from the body's middle, along x or z, anything of it lies: its rock, its skirt, its stones. */
    public int reach() {
        return Math.max(Math.max(-this.groundMinX, this.groundMinX + this.groundSizeX - 1), Math.max(-this.groundMinZ, this.groundMinZ + this.groundSizeZ - 1));
    }

    // The grid

    private int index(int x, int y, int z) {
        return ((x - this.minX) * this.sizeZ + (z - this.minZ)) * this.sizeY + (y - this.minY);
    }

    private boolean inside(int x, int y, int z) {
        return x >= this.minX && x < this.minX + this.sizeX && z >= this.minZ && z < this.minZ + this.sizeZ && y >= this.minY && y < this.minY + this.sizeY;
    }

    /** What this cell is made of, a slab or a whole block; null for air and outside the model. */
    public RockMaterial material(int x, int y, int z) {
        int cell = this.inside(x, y, z) ? this.cells[this.index(x, y, z)] : 0;
        return cell == 0 ? null : RockMaterial.of((cell & ~SLAB) - 1);
    }

    public boolean slab(int x, int y, int z) {
        return this.inside(x, y, z) && (this.cells[this.index(x, y, z)] & SLAB) != 0;
    }

    private boolean inColumns(int x, int z) {
        return x >= this.minX && x < this.minX + this.sizeX && z >= this.minZ && z < this.minZ + this.sizeZ;
    }

    /** The lowest rock of this column, or {@link #NONE}. */
    public int lowest(int x, int z) {
        return this.inColumns(x, z) ? this.lowest[(x - this.minX) * this.sizeZ + (z - this.minZ)] : NONE;
    }

    /** The highest cell of this column that isn't air (a slab counts), or {@link #NONE}. */
    public int highest(int x, int z) {
        return this.inColumns(x, z) ? this.highest[(x - this.minX) * this.sizeZ + (z - this.minZ)] : NONE;
    }

    private boolean inGround(int x, int z) {
        return x >= this.groundMinX && x < this.groundMinX + this.groundSizeX && z >= this.groundMinZ && z < this.groundMinZ + this.groundSizeZ;
    }

    /** How many blocks above the body's ground level the ground of this column rises to, or 0. */
    public int rise(int x, int z) {
        return this.inGround(x, z) ? this.rise[(x - this.groundMinX) * this.groundSizeZ + (z - this.groundMinZ)] : 0;
    }

    // The shape

    /** Positive inside the rock, about the distance to its surface in blocks. */
    private double density(double x, double y, double z) {
        Body body = this.body;
        double scale = 1, cleft = Double.MAX_VALUE;
        if (body.pinnacles() > 0) {
            this.pinnacle(x, y, z);
            double here = this.drop(this.plan.id1), beside = this.drop(this.plan.id2);
            scale = 1 - here;
            // The cleft between two pinnacles opens some way below the lower one's top, and widens upward
            double lower = this.top * (1 - Math.max(here, beside));
            double opens = lower * (1 - (0.15 + 0.5 * RockNoise.unit(RockNoise.mix(this.plan.id1 ^ this.plan.id2))));
            if (y > opens) cleft = this.plan.edge - 0.4 - body.cleft() * RockNoise.smoothstep(opens, opens + 0.3 * this.top, y);
        }
        double shape = this.outline(x, y, z, scale);
        if (shape < -this.reach) return shape;
        double density = shape;
        if (shape <= this.reach) {
            if (body.warp() > 0) density += body.warp() * RockNoise.fbm(this.seed + 1, x / body.warpScale(), y / (body.warpScale() * 1.5), z / body.warpScale(), 2);
            // Every block of rock stands in or out first; then the cracks between them are cut, so that none is filled again
            double bedded = y + this.tiltX * x + this.tiltZ * z;
            Fracture pieces = body.pieces(), chunks = body.chunks();
            if (pieces.size() > 0) {
                RockNoise.cells(this.seed + 70, x, bedded, z, pieces.size(), pieces.size() * pieces.tall(), pieces.size(), this.piece);
                density += pieces.depth() * (2 * RockNoise.unit(RockNoise.mix(this.piece.id1 + 3)) - 1);
            }
            if (chunks.size() > 0) {
                RockNoise.cells(this.seed + 60, x, bedded, z, chunks.size(), chunks.size() * chunks.tall(), chunks.size(), this.chunk);
                density += chunks.depth() * (2 * RockNoise.unit(RockNoise.mix(this.chunk.id1 + 3)) - 1);
            }
            if (body.roughness() > 0) density += body.roughness() * RockNoise.noise(this.seed + 4, x / 2.6, y / 2.6, z / 2.6);
            if (pieces.size() > 0) density = crack(pieces, this.piece, density);
            if (chunks.size() > 0) density = crack(chunks, this.chunk, density);
            if (this.flutes > 0) {
                // Columns: the cells of a Voronoi diagram over the ground plan, a few blocks across, as columnar rock is
                // jointed; the joints between them are grooves a block wide down the faces, wandering a little with height
                double wanderX = 0.8 * RockNoise.noise(this.seed + 56, y / 11, 0.5, 3.5), wanderZ = 0.8 * RockNoise.noise(this.seed + 57, y / 11, 7.5, 1.5);
                RockNoise.cells(this.seed + 58, x + wanderX, z + wanderZ, this.flutes, this.column);
                // Not across the top: the joints fade out in its last few blocks, where plants grow on it
                double depth = body.flutes() * RockNoise.smoothstep(this.top - 0.5, this.top - 5, y);
                if (depth > 0) density = Math.min(density, Math.max(this.column.edge - 0.45, density - depth));
            }
        }
        return Math.min(density, cleft);
    }

    /** An open crack along the border between two cells: a groove a block wide, as deep as the fracture's gape. */
    private static double crack(Fracture fracture, RockNoise.Cells cells, double density) {
        if (RockNoise.unit(RockNoise.mix(cells.id1 ^ cells.id2)) >= fracture.open()) return density;
        return Math.min(density, Math.max(cells.edge - 0.5, density - fracture.gape()));
    }

    /** Which pinnacles this point is in and beside (into {@link #plan}); their borders wander a little with height. */
    private void pinnacle(double x, double y, double z) {
        double wanderX = 1.5 * RockNoise.noise(this.seed + 50, y / 14, 0.5, 3.5), wanderZ = 1.5 * RockNoise.noise(this.seed + 51, y / 14, 7.5, 1.5);
        RockNoise.cells(this.seed + 52, x + wanderX, z + wanderZ, this.body.pinnacles(), this.plan);
    }

    /**
     * The share of the body's height this pinnacle is lower by: none for the tallest; of the rest, four in five stand
     * nearly as tall (a twentieth to an eighth lower), the others much lower, up to the body's most: shoulders.
     */
    private double drop(long pinnacle) {
        if (pinnacle == this.tallest) return 0;
        double roll = RockNoise.unit(RockNoise.mix(pinnacle + 5));
        return roll < 0.82 ? 0.05 + 0.075 * roll / 0.82 : 0.25 + (this.body.pinnacleDrop() - 0.25) * (roll - 0.82) / 0.18;
    }

    private double outline(double x, double y, double z, double heightScale) {
        double shape = -Double.MAX_VALUE;
        List<Lobe> lobes = this.body.lobes();
        for (int index = 0; index < lobes.size(); index++) {
            Lobe lobe = lobes.get(index);
            double distance = lobe.blob() ? blob(lobe, this.facets[index], x, y, z) : column(lobe, this.facets[index], this.swells[index], x, y, z, heightScale);
            // Boulders lie against each other with a crease between; columns grow into each other
            shape = index == 0 ? distance : smoothMax(shape, distance, lobe.blob() ? 0.6 : FUSE);
        }
        return shape;
    }

    private static double smoothMax(double a, double b, double k) {
        double h = Math.max(k - Math.abs(a - b), 0) / k;
        return Math.max(a, b) + h * h * k * 0.25;
    }

    private static double column(Lobe lobe, double[] facets, double[] swells, double x, double y, double z, double heightScale) {
        double height = lobe.height() * heightScale;
        double t = Math.max(y / height, 0);
        double lean = Math.pow(Math.min(t, 1.2), lobe.leanCurve());
        double dx = x - lobe.x() - lobe.leanX() * lean, dz = z - lobe.z() - lobe.leanZ() * lean;
        double cos = Math.cos(lobe.rotation()), sin = Math.sin(lobe.rotation());
        double u = dx * cos + dz * sin, v = -dx * sin + dz * cos;
        double wide = 1 + lobe.bulge() * Math.exp(-square((t - lobe.bulgeAt()) / 0.28)) + lobe.foot() * Math.exp(-t / 0.07);
        if (lobe.lobing() > 0) {
            // Out of round: it swells and narrows twice and three times around
            double around = Math.atan2(v / lobe.aspect(), u);
            wide *= 1 + lobe.lobing() * (0.6 * Math.cos(2 * around + swells[0]) + 0.4 * Math.cos(3 * around + swells[1]));
        }
        double ru = lobe.radius() * wide, rv = lobe.radius() * lobe.aspect() * wide;
        double q = Math.sqrt(u * u / (ru * ru) + v * v / (rv * rv));
        // Inside where q^shoulder + t^cap < 1; divided by its slope, that is about the distance to the surface
        double value = 1 - Math.pow(q, lobe.shoulder()) - Math.pow(t, lobe.cap());
        double slopeOut = lobe.shoulder() * Math.pow(q, lobe.shoulder() - 1) / Math.min(ru, rv);
        double slopeUp = lobe.cap() * Math.pow(t, lobe.cap() - 1) / height;
        double distance = value / Math.max(Math.sqrt(slopeOut * slopeOut + slopeUp * slopeUp), 0.02);
        if (facets != null) {
            // Upright planes that cut its round sides flat; they close in as it narrows toward the top
            double narrow = Math.pow(Math.max(1 - Math.pow(Math.min(t, 1), lobe.cap()), 0), 1 / lobe.shoulder());
            for (int facet = 0; facet < facets.length; facet += 4) {
                double support = Math.sqrt(square(facets[facet] * ru) + square(facets[facet + 2] * rv));
                distance = Math.min(distance, support * facets[facet + 3] * narrow - (u * facets[facet] + v * facets[facet + 2]));
            }
        }
        // Its top is cut level
        if (lobe.plateau() > 0) distance = Math.min(distance, height * (1 - lobe.plateau()) - y);
        return Math.clamp(distance, -64, 64);
    }

    private static double blob(Lobe lobe, double[] facets, double x, double y, double z) {
        double dx = x - lobe.x(), dy = y - lobe.centerY(), dz = z - lobe.z();
        double cos = Math.cos(lobe.rotation()), sin = Math.sin(lobe.rotation());
        double u = dx * cos + dz * sin, v = -dx * sin + dz * cos;
        double ru = lobe.radius(), ry = lobe.height() / 2, rv = lobe.radius() * lobe.aspect();
        // An ellipsoid's distance, near enough (the scaled length, times how far it is from 1, over the gradient's)
        double k0 = Math.sqrt(square(u / ru) + square(dy / ry) + square(v / rv));
        double k1 = Math.sqrt(square(u / (ru * ru)) + square(dy / (ry * ry)) + square(v / (rv * rv)));
        double distance = k1 < 1e-6 ? Math.min(ru, Math.min(ry, rv)) : k0 * (1 - k0) / k1;
        if (facets != null) {
            for (int facet = 0; facet < facets.length; facet += 4) {
                double support = Math.sqrt(square(facets[facet] * ru) + square(facets[facet + 1] * ry) + square(facets[facet + 2] * rv));
                distance = Math.min(distance, support * facets[facet + 3] - (u * facets[facet] + dy * facets[facet + 1] + v * facets[facet + 2]));
            }
        }
        return distance;
    }

    /**
     * The planes that cut a lobe flat, as the borders of a Voronoi cell would: each lies between the lobe's middle and
     * a neighbour some way off. A boulder's neighbours are spread evenly over the directions from a little above level
     * to straight up (a golden spiral, each nudged), so its faces lean in and its last plane is its flat top; a column's
     * stand around it.
     */
    private static double[] facets(Lobe lobe) {
        if (lobe.facets() <= 0) return null;
        double[] planes = new double[lobe.facets() * 4];
        for (int facet = 0; facet < lobe.facets(); facet++) {
            long hash = RockNoise.hash(lobe.seed(), 100 + facet);
            double turn, up;
            if (lobe.blob()) {
                turn = facet * 2.39996 + 0.9 * RockNoise.unit(hash);
                up = Math.min(0.1 + 0.9 * (facet + 0.2 + 0.6 * RockNoise.unit(RockNoise.mix(hash + 1))) / lobe.facets(), 1);
                if (facet == lobe.facets() - 1) up = 1;
            } else {
                turn = 2 * Math.PI * (facet + 0.7 * RockNoise.unit(hash)) / lobe.facets();
                up = -0.08 + 0.16 * RockNoise.unit(RockNoise.mix(hash + 1));
            }
            double level = Math.sqrt(Math.max(1 - up * up, 0));
            planes[facet * 4] = level * Math.cos(turn);
            planes[facet * 4 + 1] = up;
            planes[facet * 4 + 2] = level * Math.sin(turn);
            // A boulder's flat top is cut less deep than its sides, so that it stays about as tall as asked
            double deep = lobe.blob() && facet == lobe.facets() - 1 ? 0.2 + 0.3 * RockNoise.unit(RockNoise.mix(hash + 2)) : 0.3 + 0.7 * RockNoise.unit(RockNoise.mix(hash + 2));
            planes[facet * 4 + 3] = 1 - lobe.facetDepth() * deep;
        }
        return planes;
    }

    private static double square(double value) {
        return value * value;
    }

    // From the shape to cells

    private BitSet solid() {
        BitSet solid = new BitSet(this.cells.length);
        for (int x = this.minX; x < this.minX + this.sizeX; x++) {
            for (int z = this.minZ; z < this.minZ + this.sizeZ; z++) {
                for (int y = this.minY; y < this.minY + this.sizeY; y++) {
                    if (this.density(x + 0.5, y + 0.5, z + 0.5) > 0) solid.set(this.index(x, y, z));
                }
            }
        }
        return solid;
    }

    private boolean solid(BitSet solid, int x, int y, int z) {
        // Under the grid the rock goes on as its lowest layer does
        if (y < this.minY) return this.inColumns(x, z) && solid.get(this.index(x, this.minY, z));
        return this.inside(x, y, z) && solid.get(this.index(x, y, z));
    }

    /** Rock, or the ground the rock stands in: what shuts a face in. */
    private boolean filled(BitSet solid, int x, int y, int z) {
        return y < 0 || this.inside(x, y, z) && solid.get(this.index(x, y, z));
    }

    private int neighbours(BitSet solid, int x, int y, int z) {
        int count = 0;
        if (this.solid(solid, x + 1, y, z)) count++;
        if (this.solid(solid, x - 1, y, z)) count++;
        if (this.solid(solid, x, y + 1, z)) count++;
        if (this.solid(solid, x, y - 1, z)) count++;
        if (this.solid(solid, x, y, z + 1)) count++;
        if (this.solid(solid, x, y, z - 1)) count++;
        return count;
    }

    /** Twice over: removes the specks (rock with rock on one side at most) and fills the pinholes (air with rock on five sides). */
    private void tidy(BitSet solid) {
        for (int pass = 0; pass < 2; pass++) {
            BitSet before = (BitSet) solid.clone();
            for (int x = this.minX; x < this.minX + this.sizeX; x++) {
                for (int z = this.minZ; z < this.minZ + this.sizeZ; z++) {
                    for (int y = this.minY; y < this.minY + this.sizeY; y++) {
                        int neighbours = this.neighbours(before, x, y, z);
                        if (before.get(this.index(x, y, z))) {
                            if (neighbours <= 1) solid.clear(this.index(x, y, z));
                        } else if (neighbours >= 5) {
                            solid.set(this.index(x, y, z));
                        }
                    }
                }
            }
        }
    }

    /** Drops whatever isn't joined, face to face, to the rock at or under the ground. */
    private void keepGrounded(BitSet solid) {
        BitSet kept = new BitSet(this.cells.length);
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (int x = this.minX; x < this.minX + this.sizeX; x++) {
            for (int z = this.minZ; z < this.minZ + this.sizeZ; z++) {
                for (int y = this.minY; y <= 0; y++) {
                    int index = this.index(x, y, z);
                    if (solid.get(index) && !kept.get(index)) {
                        kept.set(index);
                        queue.add(new int[]{x, y, z});
                    }
                }
            }
        }
        int[][] steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            for (int[] step : steps) {
                int x = cell[0] + step[0], y = cell[1] + step[1], z = cell[2] + step[2];
                if (!this.inside(x, y, z)) continue;
                int index = this.index(x, y, z);
                if (solid.get(index) && !kept.get(index)) {
                    kept.set(index);
                    queue.add(new int[]{x, y, z});
                }
            }
        }
        solid.and(kept);
    }

    private void columns(BitSet solid) {
        for (int x = this.minX; x < this.minX + this.sizeX; x++) {
            for (int z = this.minZ; z < this.minZ + this.sizeZ; z++) {
                int low = NONE, high = NONE;
                for (int y = this.minY; y < this.minY + this.sizeY; y++) {
                    if (!solid.get(this.index(x, y, z))) continue;
                    if (low == NONE) low = y;
                    high = y;
                }
                this.lowest[(x - this.minX) * this.sizeZ + (z - this.minZ)] = low;
                this.highest[(x - this.minX) * this.sizeZ + (z - this.minZ)] = high;
            }
        }
    }

    // Materials

    private void materials(BitSet solid) {
        for (int x = this.minX; x < this.minX + this.sizeX; x++) {
            for (int z = this.minZ; z < this.minZ + this.sizeZ; z++) {
                for (int y = this.minY; y < this.minY + this.sizeY; y++) {
                    int index = this.index(x, y, z);
                    if (solid.get(index)) this.cells[index] = (byte) (this.rock(solid, x, y, z).ordinal() + 1);
                }
            }
        }
    }

    /** How much an open cell faces up: 1 on level rock, 0 on an upright face, negative under an overhang. */
    private double upness(BitSet solid, int x, int y, int z) {
        this.face(solid, x, y, z);
        return this.faceY;
    }

    /** Which way an open cell faces, into {@link #faceX}, {@link #faceY} and {@link #faceZ}: the open cells around it pull it their way. */
    private void face(BitSet solid, int x, int y, int z) {
        double nx = 0, ny = 0, nz = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0 || this.filled(solid, x + dx, y + dy, z + dz)) continue;
                    double inverse = 1 / Math.sqrt(dx * dx + dy * dy + dz * dz);
                    nx += dx * inverse;
                    ny += dy * inverse;
                    nz += dz * inverse;
                }
            }
        }
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        this.faceX = length < 1e-6 ? 0 : nx / length;
        this.faceY = length < 1e-6 ? 0 : ny / length;
        this.faceZ = length < 1e-6 ? 0 : nz / length;
    }

    /**
     * Whether a cell looks into a crack: a gap a block wide with rock again right behind it, that goes on two blocks
     * deep or more (across, from the face in). A flute, a groove a block deep, isn't one: the game's own light shades it.
     */
    private boolean inCrack(BitSet solid, int x, int y, int z) {
        for (int[] side : SIDES) {
            int dx = side[0], dz = side[1];
            if (!this.slot(solid, x + dx, y, z + dz, dx, dz)) continue;
            if (this.slot(solid, x + dx + dz, y, z + dz + dx, dx, dz) || this.slot(solid, x + dx - dz, y, z + dz - dx, dx, dz)) return true;
        }
        return false;
    }

    /** Whether this cell is open with rock on both sides of it along ({@code dx}, {@code dz}). */
    private boolean slot(BitSet solid, int x, int y, int z, int dx, int dz) {
        return !this.filled(solid, x, y, z) && this.filled(solid, x - dx, y, z - dz) && this.filled(solid, x + dx, y, z + dz);
    }

    private RockMaterial rock(BitSet solid, int x, int y, int z) {
        double cx = x + 0.5, cy = y + 0.5, cz = z + 0.5;
        // Streaks of lighter rock down the faces, for texture: a noise drawn far out along the height
        double streak = RockNoise.noise(this.seed + 13, cx / 3.2, cy / 14.0, cz / 3.2) + 0.5 * RockNoise.noise(this.seed + 17, cx / 1.3, cy / 3.5, cz / 1.3);
        double grain = 2 * RockNoise.unit(RockNoise.hash(this.seed + 12, x, y, z)) - 1;
        if (this.neighbours(solid, x, y, z) == 6) {
            // Inside, where no light tells: the streaks alone, for whoever digs in
            return streak > 0.45 ? RockMaterial.LIGHT : RockMaterial.BASE;
        }
        if (this.inCrack(solid, x, y, z)) return RockMaterial.SHADE;
        // How open the cell stands: the share of open cells two blocks around it. A flat wall is about a half
        int open = 0, all = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    all++;
                    if (!this.filled(solid, x + dx, y + dy, z + dz)) open++;
                }
            }
        }
        double openness = Math.clamp(((double) open / all - 0.47) / 0.25, -1, 1);
        this.face(solid, x, y, z);
        double up = this.faceY;
        // How much the face is turned toward the sun, or away from it (negative)
        double sun = this.faceX * SUN_X + this.faceZ * SUN_Z;
        // Shade: what faces down, what is shut in (the creases where two stones meet), and on the side turned from the sun
        // its foot and patches of its face
        double shadow = -0.5 * openness - 0.7 * up + 0.08 * grain;
        if (!this.filled(solid, x, y - 1, z) || openness < -0.6 && this.filled(solid, x, y + 1, z) || shadow > 0.42) return RockMaterial.SHADE;
        if (sun < -0.3) {
            double patch = RockNoise.noise(this.seed + 19, cx / 4.0, cy / 6.0, cz / 4.0) + 0.2 * grain;
            if (patch > (cy < 1 ? 0.0 : 0.42 + 0.5 * (1 + sun)) && openness < 0.6) return RockMaterial.SHADE;
        }
        // Light: in patches on what faces up, more of it on the edges that stand out; and the streaks down the faces where
        // nothing shades them. Both more on the side turned toward the sun
        boolean lit = !this.filled(solid, x, y + 1, z)
                && RockNoise.noise(this.seed + 18, cx / 4.5, cy / 3.0, cz / 4.5) + 0.25 * grain > (openness > 0.35 ? 0.12 : 0.36) - 0.16 * sun;
        // And texture: single blocks of it scattered over the faces
        boolean speck = grain > 0.86 && shadow < 0.2;
        return lit || speck || streak > 0.43 - 0.12 * sun && shadow < 0.2 ? RockMaterial.LIGHT : RockMaterial.BASE;
    }

    // Plant cover

    private void cover(BitSet solid) {
        Body body = this.body;
        if (body.cover() <= 0) return;
        List<int[]> covered = new ArrayList<>();
        BitSet vined = new BitSet();
        for (int x = this.minX; x < this.minX + this.sizeX; x++) {
            for (int z = this.minZ; z < this.minZ + this.sizeZ; z++) {
                int high = this.highest(x, z);
                if (high == NONE) continue;
                for (int y = Math.max(this.lowest(x, z), 0); y <= high; y++) {
                    if (!solid.get(this.index(x, y, z)) || this.solid(solid, x, y + 1, z)) continue;
                    // Plants keep to level rock: the flat top, the wider ledges
                    double upness = this.upness(solid, x, y, z);
                    double score = body.cover() + body.coverTop() * (y / this.top - 0.5) + 0.45 * RockNoise.noise(this.seed + 20, x / 7.5, y / 11.0, z / 7.5)
                            + 1.2 * (upness - 0.75);
                    if (score <= 0.5) continue;
                    // Grass under the open sky, with moss in patches among it; moss alone where rock hangs over
                    boolean grass = y == high && RockNoise.noise(this.seed + 23, x / 2.2, z / 2.2) + 0.5 * RockNoise.unit(RockNoise.hash(this.seed + 24, x, y, z)) < 0.42;
                    this.cells[this.index(x, y, z)] = (byte) ((grass ? RockMaterial.GRASS : RockMaterial.MOSS).ordinal() + 1);
                    // Earth under the grass where the cover is thick
                    if (grass && score > 0.95 && this.neighbours(solid, x, y - 1, z) == 6) this.cells[this.index(x, y - 1, z)] = (byte) (RockMaterial.SOIL.ordinal() + 1);
                    covered.add(new int[]{x, y, z, grass ? 1 : 0});
                }
            }
        }
        for (int[] cell : covered) {
            int x = cell[0], y = cell[1], z = cell[2];
            long hash = RockNoise.hash(this.seed + 21, x, y, z);
            if (cell[3] == 1 && RockNoise.unit(hash) < body.plants()) {
                this.decorations.add(new Decoration(x, y + 1, z, DecorationKind.PLANT, 0, 0, (int) (hash >>> 40), null));
            }
            // Over some of its open edges moss hangs a block or two down the face, and vines from there
            for (int side = 0; side < SIDES.length; side++) {
                int dx = SIDES[side][0], dz = SIDES[side][1];
                if (this.solid(solid, x + dx, y, z + dz)) continue;
                long sideHash = RockNoise.mix(hash + side);
                double clump = 0.5 + 0.5 * RockNoise.noise(this.seed + 22, x / 5.0, z / 5.0);
                int length = RockNoise.unit(sideHash) < 0.25 + 0.6 * clump ? 1 + (int) (body.drape() * clump * RockNoise.unit(RockNoise.mix(sideHash + 5))) : 0;
                int down = 1;
                for (; down <= length; down++) {
                    if (y - down < 0 || !this.solid(solid, x, y - down, z) || this.solid(solid, x + dx, y - down, z + dz)) break;
                    int index = this.index(x, y - down, z);
                    if (RockMaterial.of(this.cells[index] - 1).isRock()) this.cells[index] = (byte) (RockMaterial.MOSS.ordinal() + 1);
                }
                if (RockNoise.unit(RockNoise.mix(sideHash + 7)) >= body.vines() * clump * 1.5) continue;
                int vines = 2 + (int) (6 * RockNoise.unit(RockNoise.mix(sideHash + 8)));
                for (int hang = 0; hang < vines; hang++) {
                    int vy = y - down + 1 - hang;
                    // One vine a cell: where another already hangs, this one ends (it would cut the other's string)
                    if (vy < 1 || !this.inside(x + dx, vy, z + dz) || this.solid(solid, x + dx, vy, z + dz) || vined.get(this.index(x + dx, vy, z + dz))) break;
                    vined.set(this.index(x + dx, vy, z + dz));
                    this.decorations.add(new Decoration(x + dx, vy, z + dz, DecorationKind.VINE, -dx, -dz, 0, null));
                }
            }
        }
    }

    /** A slab on each bare, upward face that the rock's surface passes through the lower half of. */
    private void slabs(BitSet solid) {
        for (int x = this.minX; x < this.minX + this.sizeX; x++) {
            for (int z = this.minZ; z < this.minZ + this.sizeZ; z++) {
                int high = this.highest(x, z);
                if (high == NONE) continue;
                for (int y = Math.max(this.lowest(x, z), 0); y <= high; y++) {
                    if (!solid.get(this.index(x, y, z)) || !this.inside(x, y + 1, z) || solid.get(this.index(x, y + 1, z))) continue;
                    // Not in a gap one block high: a slab there would wedge under the rock above
                    if (this.solid(solid, x, y + 2, z)) continue;
                    RockMaterial below = RockMaterial.of(this.cells[this.index(x, y, z)] - 1);
                    if (!below.isRock() || this.density(x + 0.5, y + 1.22, z + 0.5) <= 0) continue;
                    this.cells[this.index(x, y + 1, z)] = (byte) ((below.ordinal() + 1) | SLAB);
                    if (y == high) this.highest[(x - this.minX) * this.sizeZ + (z - this.minZ)] = y + 1;
                }
            }
        }
    }

    // The ground around it

    /** The skirt (the ground rising toward the rock's foot along part of it) and the loose stones around. */
    private void ground(BitSet solid) {
        Body body = this.body;
        // How far each column is from the rock's foot: the columns with rock at the ground
        float[] distance = new float[this.groundSizeX * this.groundSizeZ];
        Arrays.fill(distance, Float.MAX_VALUE);
        for (int x = this.minX; x < this.minX + this.sizeX; x++) {
            for (int z = this.minZ; z < this.minZ + this.sizeZ; z++) {
                if (this.solid(solid, x, -1, z) || this.solid(solid, x, 0, z) || this.solid(solid, x, 1, z)) {
                    distance[(x - this.groundMinX) * this.groundSizeZ + (z - this.groundMinZ)] = 0;
                }
            }
        }
        // Two sweeps (forward, then back) with steps of 1 and of a diagonal: near enough the true distance
        float diagonal = 1.4142f;
        for (int pass = 0; pass < 2; pass++) {
            int step = pass == 0 ? 1 : -1;
            for (int gx = pass == 0 ? 0 : this.groundSizeX - 1; gx >= 0 && gx < this.groundSizeX; gx += step) {
                for (int gz = pass == 0 ? 0 : this.groundSizeZ - 1; gz >= 0 && gz < this.groundSizeZ; gz += step) {
                    float best = distance[gx * this.groundSizeZ + gz];
                    int px = gx - step, pz = gz - step;
                    if (px >= 0 && px < this.groundSizeX) {
                        best = Math.min(best, distance[px * this.groundSizeZ + gz] + 1);
                        if (gz > 0) best = Math.min(best, distance[px * this.groundSizeZ + gz - 1] + diagonal);
                        if (gz < this.groundSizeZ - 1) best = Math.min(best, distance[px * this.groundSizeZ + gz + 1] + diagonal);
                    }
                    if (pz >= 0 && pz < this.groundSizeZ) best = Math.min(best, distance[gx * this.groundSizeZ + pz] + 1);
                    distance[gx * this.groundSizeZ + gz] = best;
                }
            }
        }
        // The ground rises on one side of the rock, where its scree gathers (under the side it leans over), and thins out
        // around it: a ramp, not a ring
        Lobe first = body.lobes().getFirst();
        double heavy = Math.hypot(first.leanX(), first.leanZ()) > 1.5
                ? Math.atan2(first.leanZ(), first.leanX()) + 0.8 * (RockNoise.unit(RockNoise.hash(this.seed, 30)) - 0.5)
                : 2 * Math.PI * RockNoise.unit(RockNoise.hash(this.seed, 30));
        double stones = Math.max(body.skirtWidth(), 3);
        for (int gx = 0; gx < this.groundSizeX; gx++) {
            for (int gz = 0; gz < this.groundSizeZ; gz++) {
                int x = this.groundMinX + gx, z = this.groundMinZ + gz;
                float away = distance[gx * this.groundSizeZ + gz];
                if (away <= 0 || away == Float.MAX_VALUE) continue;
                if (body.skirtWidth() > 0 && away <= body.skirtWidth()) {
                    double side = RockNoise.smoothstep(0.1, 0.75, Math.cos(Math.atan2(z, x) - heavy) + 0.7 * RockNoise.noise(this.seed + 31, x / 9.0, z / 9.0));
                    double height = body.skirtHeight() * Math.pow(1 - (away - 1) / body.skirtWidth(), 1.25) * side;
                    this.rise[gx * this.groundSizeZ + gz] = (byte) Math.clamp(Math.round(height), 0, (long) Math.ceil(body.skirtHeight()) + 1);
                }
                if (body.scree() <= 0 || away > stones) continue;
                // Single stones lying around the foot, more of them near it
                long hash = RockNoise.hash(this.seed + 32, x, 0, z);
                if (RockNoise.unit(RockNoise.mix(hash + 2)) < 0.07 * body.scree() * (1.4 - away / (stones + 1))) {
                    double kind = RockNoise.unit(RockNoise.mix(hash + 3));
                    RockMaterial material = kind < 0.6 ? RockMaterial.BASE : kind < 0.8 ? RockMaterial.LIGHT : RockMaterial.SHADE;
                    this.decorations.add(new Decoration(x, 0, z, DecorationKind.STONE, 0, 0, (int) (hash >>> 40), material));
                }
            }
        }
    }
}
