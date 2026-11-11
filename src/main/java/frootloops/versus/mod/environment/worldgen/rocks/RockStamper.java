package frootloops.versus.mod.environment.worldgen.rocks;

import frootloops.versus.mod.environment.worldgen.rocks.RockModel.Decoration;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Puts a rock's model into a canvas, column by column. A column's ground is read once, before anything is written in
 * it, and nothing a column gets depends on another column of the canvas: so a rock stamped a chunk at a time, in any
 * order, is the rock stamped whole.
 *
 * <p>For each column inside the box given: the ground rises to the skirt's height (never into the rock overhead, and
 * no more than the skirt's own height and two blocks where the ground has fallen away); then the rock's cells are
 * written, a column that stands at the body's ground going on down to the ground it finds
 * (at most {@link #MAX_FILL} blocks); then what grows and lies on it. Under water and inside the ground the plant
 * cover is rock instead, and nothing is planted.
 */
public final class RockStamper {

    /** How far down a rock's foot is filled where the ground falls away under it. */
    public static final int MAX_FILL = 24;

    private RockStamper() {
    }

    /** Stamps a whole formation: each body stands on the canvas's ground at its own middle. */
    public static void stamp(RockPlan plan, int originX, int originZ, RockCanvas canvas, int minX, int minZ, int maxX, int maxZ) {
        // Every body's ground before any is stamped: one's skirt doesn't lift the next
        int[] anchors = new int[plan.bodies().size()];
        for (int index = 0; index < anchors.length; index++) {
            RockPlan.Body body = plan.bodies().get(index);
            anchors[index] = canvas.ground(originX + body.dx(), originZ + body.dz());
        }
        for (int index = 0; index < anchors.length; index++) {
            RockPlan.Body body = plan.bodies().get(index);
            stamp(RockModel.build(body), originX + body.dx(), anchors[index], originZ + body.dz(), canvas, minX, minZ, maxX, maxZ);
        }
    }

    /** Stamps the part of a body's model inside the box ({@code minX}, {@code minZ}) to ({@code maxX}, {@code maxZ}), both inclusive. */
    public static void stamp(RockModel model, int anchorX, int anchorY, int anchorZ, RockCanvas canvas, int minX, int minZ, int maxX, int maxZ) {
        int fromX = Math.max(minX, anchorX + model.groundMinX), toX = Math.min(maxX, anchorX + model.groundMinX + model.groundSizeX - 1);
        int fromZ = Math.max(minZ, anchorZ + model.groundMinZ), toZ = Math.min(maxZ, anchorZ + model.groundMinZ + model.groundSizeZ - 1);
        if (fromX > toX || fromZ > toZ) return;
        Map<Long, List<Decoration>> decorations = new HashMap<>();
        for (Decoration decoration : model.decorations()) {
            decorations.computeIfAbsent(column(decoration.x(), decoration.z()), key -> new ArrayList<>()).add(decoration);
        }
        for (int worldX = fromX; worldX <= toX; worldX++) {
            for (int worldZ = fromZ; worldZ <= toZ; worldZ++) {
                column(model, worldX - anchorX, worldZ - anchorZ, anchorX, anchorY, anchorZ, canvas, decorations.get(column(worldX - anchorX, worldZ - anchorZ)));
            }
        }
    }

    private static long column(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static void column(RockModel model, int x, int z, int anchorX, int anchorY, int anchorZ, RockCanvas canvas, List<Decoration> decorations) {
        int worldX = anchorX + x, worldZ = anchorZ + z;
        int ground = canvas.ground(worldX, worldZ);
        int low = model.lowest(x, z), high = model.highest(x, z);
        boolean rock = low != RockModel.NONE;
        // The skirt: the ground rises toward the foot, but not into rock that hangs over it
        int floor = ground;
        int rise = model.rise(x, z);
        if (rise > 0) {
            int to = Math.min(anchorY + rise, ground + rise + 2);
            if (rock) to = Math.min(to, anchorY + low);
            if (to > ground) {
                canvas.raise(worldX, worldZ, ground, to);
                floor = to;
            }
        }
        boolean onGround = rock && anchorY + low <= floor;

        // Where grass was put as grass, for the plants on it
        BitSet grass = new BitSet();
        if (rock) {
            // A foot: a column that stands at the body's ground goes on down to the ground it finds
            if (low <= 1) {
                for (int y = anchorY + low - 1, filled = 0; y >= floor && filled < MAX_FILL; y--, filled++) {
                    if (canvas.replaceable(worldX, y, worldZ)) canvas.rock(worldX, y, worldZ, RockMaterial.BASE);
                }
            }
            for (int y = low; y <= high; y++) {
                RockMaterial material = model.material(x, y, z);
                int worldY = anchorY + y;
                // No slabs: where the model has one, nothing
                if (material == null || model.slab(x, y, z) || !canvas.replaceable(worldX, worldY, worldZ)) continue;
                if (!material.isRock() && (worldY < ground || canvas.water(worldX, worldY, worldZ))) {
                    // No plants in the ground or under water: plain rock where they would be
                    material = RockMaterial.BASE;
                }
                if (material == RockMaterial.GRASS) grass.set(y - low);
                canvas.rock(worldX, worldY, worldZ, material);
            }
        }
        if (decorations == null) return;
        for (Decoration decoration : decorations) {
            int worldY = anchorY + decoration.y();
            if (decoration.kind() != RockModel.DecorationKind.PLANT) continue;
            // On the grass under it, if that was put as grass: not under water, not in the ground
            boolean onGrass = rock && decoration.y() - 1 >= low && grass.get(decoration.y() - 1 - low);
            if (onGrass && !canvas.water(worldX, worldY, worldZ) && canvas.replaceable(worldX, worldY, worldZ)) {
                canvas.plant(worldX, worldY, worldZ, decoration.data());
            }
        }
    }
}
