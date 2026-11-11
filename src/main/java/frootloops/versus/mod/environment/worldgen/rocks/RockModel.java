package frootloops.versus.mod.environment.worldgen.rocks;

import frootloops.versus.mod.environment.worldgen.rocks.RockPlan.Body;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One body of a rock formation as blocks: which cells are rock and of what, the plants on it, the ground's rise around
 * its foot. Built from the body's numbers alone, so the same body is the same blocks wherever and in whatever order its
 * chunks are made; built whole at once and only read after, so any number of threads may read it.
 *
 * <p>Its blocks are joined into cells ({@link RockVoxels}) and light is painted into their stone from above
 * ({@link RockLight}). Then moss (mostly) and grass grow on its level tops, more often higher up, the moss hanging a
 * block or two over their edges, plants on some of the grass; and the ground rises against its foot, all around it.
 *
 * <p>Coordinates are the body's own: x and z from its middle, y from the ground there (0 is the first block above the
 * ground).
 */
public final class RockModel {

    /** A plant standing in this cell, on the grass under it; {@code data} picks which of the palette's. */
    public record Decoration(int x, int y, int z, int data) {
    }

    /** In {@link #lowest} and {@link #highest}: a column with no rock. */
    public static final int NONE = Integer.MIN_VALUE;
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    /** The share of the grass a plant stands on. */
    private static final double PLANTS = 0.3;

    private final Body body;
    public final int minX, minY, minZ, sizeX, sizeY, sizeZ;
    /** Each cell's material's ordinal and one; 0 for air. */
    private final byte[] cells;
    private final int[] lowest, highest;

    /** The columns the ground's rise can reach: a wider rectangle than the rock's. */
    public final int groundMinX, groundMinZ, groundSizeX, groundSizeZ;
    private final byte[] rise;
    private final List<Decoration> decorations;
    private final int reach;

    private RockModel(Body body) {
        this.body = body;
        RockVoxels voxels = RockVoxels.of(body.blocks(), body.blend(), body.flare(), body.flareHeight(), body.seed());
        this.minX = voxels.minX;
        this.minY = voxels.minY;
        this.minZ = voxels.minZ;
        this.sizeX = voxels.sizeX;
        this.sizeY = voxels.sizeY;
        this.sizeZ = voxels.sizeZ;
        this.cells = new byte[this.sizeX * this.sizeY * this.sizeZ];
        RockMaterial[] tones = RockLight.paint(voxels, body.seed());
        for (int index = 0; index < tones.length; index++) if (tones[index] != null) this.cells[index] = (byte) (tones[index].ordinal() + 1);
        this.cover();
        this.decorations = List.copyOf(this.plants());

        this.lowest = new int[this.sizeX * this.sizeZ];
        this.highest = new int[this.sizeX * this.sizeZ];
        for (int x = this.minX; x < this.minX + this.sizeX; x++) {
            for (int z = this.minZ; z < this.minZ + this.sizeZ; z++) {
                int low = NONE, high = NONE;
                for (int y = this.minY; y < this.minY + this.sizeY; y++) {
                    if (this.cells[this.index(x, y, z)] == 0) continue;
                    if (low == NONE) low = y;
                    high = y;
                }
                this.lowest[(x - this.minX) * this.sizeZ + (z - this.minZ)] = low;
                this.highest[(x - this.minX) * this.sizeZ + (z - this.minZ)] = high;
            }
        }

        int around = around(body.height());
        this.groundMinX = this.minX - around;
        this.groundMinZ = this.minZ - around;
        this.groundSizeX = this.sizeX + 2 * around;
        this.groundSizeZ = this.sizeZ + 2 * around;
        this.rise = new byte[this.groundSizeX * this.groundSizeZ];
        this.ground();

        int reach = 0;
        for (int x = this.groundMinX; x < this.groundMinX + this.groundSizeX; x++) {
            for (int z = this.groundMinZ; z < this.groundMinZ + this.groundSizeZ; z++) {
                if (this.lowest(x, z) != NONE || this.rise(x, z) > 0) reach = Math.max(reach, Math.max(Math.abs(x), Math.abs(z)));
            }
        }
        this.reach = reach;
    }

    public static RockModel build(Body body) {
        return new RockModel(body);
    }

    /**
     * Everything a body can write, without building it: {minX, minY, minZ, maxX, maxY, maxZ}, in its own coordinates
     * (x and z from its middle, y from the ground there): its rock, the plants on it, the ground risen around it. Its
     * foot can also be filled further down, to the ground ({@link RockStamper#MAX_FILL}).
     */
    public static int[] bounds(Body body) {
        int[] grid = RockVoxels.grid(body.blocks(), body.flare());
        int around = around(body.height());
        return new int[]{grid[0] - around, grid[1], grid[2] - around, grid[3] + around, grid[4], grid[5] + around};
    }

    public Body body() {
        return this.body;
    }

    /** The plants on its grass. */
    public List<Decoration> decorations() {
        return this.decorations;
    }

    /** How far from the body's middle, along x or z, anything of it lies: its rock and the ground risen around it. */
    public int reach() {
        return this.reach;
    }

    // The grid

    private int index(int x, int y, int z) {
        return ((x - this.minX) * this.sizeZ + (z - this.minZ)) * this.sizeY + (y - this.minY);
    }

    private boolean inside(int x, int y, int z) {
        return x >= this.minX && x < this.minX + this.sizeX && z >= this.minZ && z < this.minZ + this.sizeZ && y >= this.minY && y < this.minY + this.sizeY;
    }

    /** What this cell is made of; null for air and outside the model. */
    public RockMaterial material(int x, int y, int z) {
        int cell = this.inside(x, y, z) ? this.cells[this.index(x, y, z)] : 0;
        return cell == 0 ? null : RockMaterial.of(cell - 1);
    }

    /** Rock or its cover here, or the ground (below 0). */
    private boolean filled(int x, int y, int z) {
        return y < 0 || this.inside(x, y, z) && this.cells[this.index(x, y, z)] != 0;
    }

    private boolean inColumns(int x, int z) {
        return x >= this.minX && x < this.minX + this.sizeX && z >= this.minZ && z < this.minZ + this.sizeZ;
    }

    /** The lowest cell of this column that isn't air, or {@link #NONE}. */
    public int lowest(int x, int z) {
        return this.inColumns(x, z) ? this.lowest[(x - this.minX) * this.sizeZ + (z - this.minZ)] : NONE;
    }

    /** The highest cell of this column that isn't air, or {@link #NONE}. */
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

    // Plant cover

    /**
     * Moss (mostly) and grass on level tops (three of the four columns beside one within a block of its height), more
     * often the higher it stands, in patches; moss hanging a block or two down the open sides.
     */
    private void cover() {
        Body body = this.body;
        if (body.cover() <= 0) return;
        long seed = body.seed();
        // Each column's highest rock above the ground
        int[] tops = new int[this.sizeX * this.sizeZ];
        Arrays.fill(tops, NONE);
        for (int x = this.minX; x < this.minX + this.sizeX; x++) {
            for (int z = this.minZ; z < this.minZ + this.sizeZ; z++) {
                for (int y = this.minY + this.sizeY - 1; y >= Math.max(0, this.minY); y--) {
                    if (this.cells[this.index(x, y, z)] == 0) continue;
                    tops[(x - this.minX) * this.sizeZ + (z - this.minZ)] = y;
                    break;
                }
            }
        }
        for (int x = this.minX + 1; x < this.minX + this.sizeX - 1; x++) {
            for (int z = this.minZ + 1; z < this.minZ + this.sizeZ - 1; z++) {
                int top = tops[(x - this.minX) * this.sizeZ + (z - this.minZ)];
                if (top == NONE || top < 1) continue;
                int level = 0;
                for (int[] side : SIDES) {
                    int other = tops[(x + side[0] - this.minX) * this.sizeZ + (z + side[1] - this.minZ)];
                    if (other != NONE && Math.abs(other - top) <= 1) level++;
                }
                double chance = body.cover() * (0.45 + 0.55 * Math.min(1, top / Math.max(4, 0.6 * body.height())));
                double patch = RockNoise.noise(seed + 40, x / 5.0, top / 5.0, z / 5.0);
                if (level < 3 || patch >= 2 * chance - 1 + 0.15 * (RockNoise.unit(RockNoise.hash(seed + 41, x, top, z)) - 0.5)) continue;
                boolean moss = RockNoise.noise(seed + 42, x / 3.5, top / 3.5, z / 3.5) > -0.25;
                this.cells[this.index(x, top, z)] = (byte) ((moss ? RockMaterial.MOSS : RockMaterial.GRASS).ordinal() + 1);
                // Over an open side moss hangs a block or two down the face
                for (int[] side : SIDES) {
                    int sx = x + side[0], sz = z + side[1];
                    if (this.filled(sx, top, sz)) continue;
                    for (int down = 1; down <= 2; down++) {
                        if (!this.inside(x, top - down, z) || this.cells[this.index(x, top - down, z)] == 0 || this.filled(sx, top - down, sz)) break;
                        if (RockNoise.unit(RockNoise.hash(seed + 43 + down, x, top - down, z)) < 0.55) {
                            this.cells[this.index(x, top - down, z)] = (byte) (RockMaterial.MOSS.ordinal() + 1);
                        }
                    }
                }
            }
        }
    }

    /** A plant on some of the grass. */
    private List<Decoration> plants() {
        List<Decoration> plants = new ArrayList<>();
        long seed = this.body.seed();
        byte grass = (byte) (RockMaterial.GRASS.ordinal() + 1);
        for (int x = this.minX; x < this.minX + this.sizeX; x++) {
            for (int z = this.minZ; z < this.minZ + this.sizeZ; z++) {
                for (int y = this.minY; y < this.minY + this.sizeY; y++) {
                    if (this.cells[this.index(x, y, z)] != grass || this.filled(x, y + 1, z)) continue;
                    if (RockNoise.unit(RockNoise.hash(seed + 77, x, y, z)) < PLANTS) plants.add(new Decoration(x, y + 1, z, (int) RockNoise.hash(seed + 78, x, y, z)));
                }
            }
        }
        return plants;
    }

    // The ground around it

    /** How high the ground rises against the foot of a rock this tall, at most. */
    private static double riseHeight(double height) {
        return Math.clamp(0.06 * height + 0.5, 1, 2.5);
    }

    /** How far out from the foot of a rock this tall the ground rises, before it wanders. */
    private static double riseOut(double height) {
        return Math.clamp(0.3 * height + 2, 3, 10);
    }

    /** How far out from its foot the ground rises a block or more around a rock this tall, at most. */
    static double riseReach(double height) {
        return 1.25 * riseOut(height) * (1 - Math.pow(0.5 / riseHeight(height), 1 / 1.4));
    }

    /** How far past the rock's grid the rise can reach. */
    private static int around(double height) {
        return (int) Math.ceil(1.25 * riseOut(height)) + 1;
    }

    /**
     * The ground rises against the foot all around it: up to {@link #riseHeight} at the foot, down to nothing
     * {@link #riseOut} out, give or take a quarter (it wanders, so that the rise isn't a ring).
     */
    private void ground() {
        double height = riseHeight(this.body.height()), out = riseOut(this.body.height());
        int far = (int) Math.ceil(1.25 * out);
        // The steps out to that far, nearest first: the first that finds the foot finds the nearest of it
        List<int[]> steps = new ArrayList<>();
        for (int dx = -far; dx <= far; dx++) {
            for (int dz = -far; dz <= far; dz++) if (dx * dx + dz * dz <= far * far) steps.add(new int[]{dx, dz, dx * dx + dz * dz});
        }
        steps.sort((one, two) -> Integer.compare(one[2], two[2]));
        long seed = this.body.seed();
        for (int x = this.groundMinX; x < this.groundMinX + this.groundSizeX; x++) {
            for (int z = this.groundMinZ; z < this.groundMinZ + this.groundSizeZ; z++) {
                if (this.material(x, 0, z) != null) continue;
                double near = -1;
                for (int[] step : steps) {
                    if (this.material(x + step[0], 0, z + step[1]) == null) continue;
                    near = Math.sqrt(step[2]);
                    break;
                }
                if (near < 0) continue;
                double reach = out * (0.75 + 0.5 * RockNoise.unit(RockNoise.hash(seed + 90, x >> 2, 0, z >> 2)));
                if (near > reach) continue;
                this.rise[(x - this.groundMinX) * this.groundSizeZ + (z - this.groundMinZ)] = (byte) Math.round(height * Math.pow(1 - near / reach, 1.4));
            }
        }
    }
}
