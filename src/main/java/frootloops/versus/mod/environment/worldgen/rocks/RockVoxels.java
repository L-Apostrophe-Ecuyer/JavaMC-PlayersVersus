package frootloops.versus.mod.environment.worldgen.rocks;

import java.util.BitSet;
import java.util.List;

/**
 * A rock's blocks in cells. A cell's centre is rock where the blocks' distance, joined with a little blending where they
 * meet (a seam is a groove, never a slot), is below zero; the foot widens toward the ground, curving out; then single
 * cells standing out on five sides go and pits shut in on five sides fill. Some of the seams where two blocks meet are
 * drawn (later, in tuff): each one whole or not at all, never the joints inside one column.
 */
final class RockVoxels {

    final int minX, minY, minZ, sizeX, sizeY, sizeZ;
    private BitSet solid;
    private final BitSet seams;
    private final int top;

    private RockVoxels(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.solid = new BitSet(sizeX * sizeY * sizeZ);
        this.seams = new BitSet(sizeX * sizeY * sizeZ);
        this.top = Integer.MIN_VALUE;
    }

    private RockVoxels(RockVoxels grid, int top) {
        this.minX = grid.minX;
        this.minY = grid.minY;
        this.minZ = grid.minZ;
        this.sizeX = grid.sizeX;
        this.sizeY = grid.sizeY;
        this.sizeZ = grid.sizeZ;
        this.solid = grid.solid;
        this.seams = grid.seams;
        this.top = top;
    }

    /**
     * The blocks in cells, joined over {@code blend} blocks where they meet, their foot widened by {@code flare} at the
     * ground and less up to {@code flareHeight}; {@code seed} draws which seams are drawn.
     */
    static RockVoxels of(List<RockBlock> blocks, double blend, double flare, double flareHeight, long seed) {
        if (blocks.isEmpty()) return new RockVoxels(new RockVoxels(0, 0, 0, 1, 1, 1), Integer.MIN_VALUE);
        int[] cells = grid(blocks, flare);
        RockVoxels grid = new RockVoxels(cells[0], cells[1], cells[2], cells[3] - cells[0] + 1, cells[4] - cells[1] + 1, cells[5] - cells[2] + 1);
        grid.fill(blocks, blend, flare, flareHeight, seed);
        grid.clean();
        grid.clean();
        int top = Integer.MIN_VALUE;
        for (int index = grid.solid.nextSetBit(0); index >= 0; index = grid.solid.nextSetBit(index + 1)) {
            top = Math.max(top, index % grid.sizeY + grid.minY);
        }
        return new RockVoxels(grid, top);
    }

    /**
     * The cells the blocks can take, their foot widened by {@code flare}: {minX, minY, minZ, maxX, maxY, maxZ}, from
     * their centres to their reach, the flare and a cell more; no deeper than 4 under the ground.
     */
    static int[] grid(List<RockBlock> blocks, double flare) {
        if (blocks.isEmpty()) return new int[]{0, 0, 0, 0, 0, 0};
        double x0 = Double.MAX_VALUE, y0 = Double.MAX_VALUE, z0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, y1 = -Double.MAX_VALUE, z1 = -Double.MAX_VALUE;
        for (RockBlock block : blocks) {
            x0 = Math.min(x0, block.cx() - block.reach());
            x1 = Math.max(x1, block.cx() + block.reach());
            y0 = Math.min(y0, block.cy() - block.reach());
            y1 = Math.max(y1, block.cy() + block.reach());
            z0 = Math.min(z0, block.cz() - block.reach());
            z1 = Math.max(z1, block.cz() + block.reach());
        }
        double margin = flare + 1;
        int minY = Math.max(-4, (int) Math.floor(y0) - 1);
        return new int[]{(int) Math.floor(x0 - margin), minY, (int) Math.floor(z0 - margin), (int) Math.ceil(x1 + margin), Math.max(minY, (int) Math.ceil(y1) + 1),
                (int) Math.ceil(z1 + margin)};
    }

    private void fill(List<RockBlock> blocks, double blend, double flare, double flareHeight, long seed) {
        // The blocks each column of cells is within reach of, in their order: only those are looked at there
        int[] start = new int[this.sizeX * this.sizeZ + 1], near = this.near(blocks, start);
        for (int x = this.minX; x < this.minX + this.sizeX; x++) {
            for (int z = this.minZ; z < this.minZ + this.sizeZ; z++) {
                int column = (x - this.minX) * this.sizeZ + (z - this.minZ);
                for (int y = this.minY; y < this.minY + this.sizeY; y++) {
                    double px = x + 0.5, py = y + 0.5, pz = z + 0.5;
                    // The foot widens toward the ground, curving out: there a cell is rock further from the blocks. Under
                    // the ground it goes straight on
                    double rise = Math.clamp(1 - py / flareHeight, 0, 1), threshold = flare * rise * rise;
                    double joined = Double.MAX_VALUE, best = Double.MAX_VALUE, second = Double.MAX_VALUE;
                    int bestAt = -1, secondAt = -1;
                    for (int entry = start[column]; entry < start[column + 1]; entry++) {
                        int at = near[entry];
                        RockBlock block = blocks.get(at);
                        if (Math.abs(py - block.cy()) > block.reach()) continue;
                        double distance = block.distance(px, py, pz, threshold);
                        if (distance < best) {
                            second = best;
                            secondAt = bestAt;
                            best = distance;
                            bestAt = at;
                        } else if (distance < second) {
                            second = distance;
                            secondAt = at;
                        }
                        joined = joined == Double.MAX_VALUE ? distance : joined(joined, distance, blend);
                    }
                    if (joined == Double.MAX_VALUE) continue;
                    int index = this.index(x, y, z);
                    // Marked on air too: a pit the clean-up fills on a drawn seam is part of it
                    if (secondAt >= 0 && second - best < 1 && drawn(blocks.get(bestAt), bestAt, blocks.get(secondAt), secondAt, seed)) this.seams.set(index);
                    if (joined < threshold) this.solid.set(index);
                }
            }
        }
    }

    /**
     * For each column of cells, the blocks whose reach takes in its middle along x and z, in their order: the column's
     * from {@code start[column]} up to {@code start[column + 1]} of what this returns.
     */
    private int[] near(List<RockBlock> blocks, int[] start) {
        int[][] spans = new int[blocks.size()][];
        for (int at = 0; at < blocks.size(); at++) {
            RockBlock block = blocks.get(at);
            // The cells whose middle, half a block in, lies within reach of the block's centre
            spans[at] = new int[]{Math.max(this.minX, (int) Math.ceil(block.cx() - block.reach() - 0.5)), Math.min(this.minX + this.sizeX - 1, (int) Math.floor(block.cx() + block.reach() - 0.5)),
                    Math.max(this.minZ, (int) Math.ceil(block.cz() - block.reach() - 0.5)), Math.min(this.minZ + this.sizeZ - 1, (int) Math.floor(block.cz() + block.reach() - 0.5))};
            for (int x = spans[at][0]; x <= spans[at][1]; x++) {
                for (int z = spans[at][2]; z <= spans[at][3]; z++) start[(x - this.minX) * this.sizeZ + (z - this.minZ) + 1]++;
            }
        }
        for (int column = 0; column < this.sizeX * this.sizeZ; column++) start[column + 1] += start[column];
        int[] near = new int[start[this.sizeX * this.sizeZ]], filled = new int[this.sizeX * this.sizeZ];
        for (int at = 0; at < blocks.size(); at++) {
            for (int x = spans[at][0]; x <= spans[at][1]; x++) {
                for (int z = spans[at][2]; z <= spans[at][3]; z++) {
                    int column = (x - this.minX) * this.sizeZ + (z - this.minZ);
                    near[start[column] + filled[column]++] = at;
                }
            }
        }
        return near;
    }

    /** The smooth minimum: within {@code blend} of each other two distances join a little below the smaller. */
    private static double joined(double a, double b, double blend) {
        if (blend <= 0) return Math.min(a, b);
        double h = Math.max(blend - Math.abs(a - b), 0) / blend;
        return Math.min(a, b) - h * h * blend / 4;
    }

    /** Whether the seam between these two blocks is drawn: never inside a column; of the others, half, each whole. */
    private static boolean drawn(RockBlock one, int first, RockBlock two, int second, long seed) {
        if (one.group() >= 0 && one.group() == two.group()) return false;
        long low = Math.min(first, second), high = Math.max(first, second);
        return RockNoise.unit(RockNoise.mix(seed + 31 * low + 7919 * high)) < 0.5;
    }

    /** Single cells standing out on five sides go, pits shut in on five sides fill; above the ground only. */
    private void clean() {
        BitSet next = (BitSet) this.solid.clone();
        for (int x = this.minX; x < this.minX + this.sizeX; x++) {
            for (int z = this.minZ; z < this.minZ + this.sizeZ; z++) {
                for (int y = Math.max(0, this.minY); y < this.minY + this.sizeY; y++) {
                    int filled = 0;
                    if (this.filled(x + 1, y, z)) filled++;
                    if (this.filled(x - 1, y, z)) filled++;
                    if (this.filled(x, y + 1, z)) filled++;
                    if (this.filled(x, y - 1, z)) filled++;
                    if (this.filled(x, y, z + 1)) filled++;
                    if (this.filled(x, y, z - 1)) filled++;
                    int index = this.index(x, y, z);
                    if (this.solid.get(index) && filled <= 1) next.clear(index);
                    else if (!this.solid.get(index) && filled >= 5) next.set(index);
                }
            }
        }
        this.solid = next;
    }

    int index(int x, int y, int z) {
        return ((x - this.minX) * this.sizeZ + (z - this.minZ)) * this.sizeY + (y - this.minY);
    }

    private boolean inside(int x, int y, int z) {
        return x >= this.minX && x < this.minX + this.sizeX && y >= this.minY && y < this.minY + this.sizeY && z >= this.minZ && z < this.minZ + this.sizeZ;
    }

    /** Whether this cell is rock; false outside the grid. */
    boolean solid(int x, int y, int z) {
        return this.inside(x, y, z) && this.solid.get(this.index(x, y, z));
    }

    /** Rock, or the ground (below 0): what shuts a cell in. */
    boolean filled(int x, int y, int z) {
        return y < 0 || this.solid(x, y, z);
    }

    /** Whether this cell of rock lies on a drawn seam. */
    boolean seam(int x, int y, int z) {
        return this.inside(x, y, z) && this.solid.get(this.index(x, y, z)) && this.seams.get(this.index(x, y, z));
    }

    /** The highest cell of rock; {@link Integer#MIN_VALUE} when there is none. */
    int top() {
        return this.top;
    }
}
