package frootloops.versus.mod.environment.worldgen.rocks;

/** How a rock's top stands: its highest column, and how broad its cap is. */
final class RockTips {

    private RockTips() {
    }

    /** The highest column's x, z and top. */
    static int[] highest(RockModel model) {
        int[] best = {0, 0, Integer.MIN_VALUE};
        for (int x = model.minX; x < model.minX + model.sizeX; x++) {
            for (int z = model.minZ; z < model.minZ + model.sizeZ; z++) {
                int top = model.highest(x, z);
                if (top > best[2]) best = new int[]{x, z, top};
            }
        }
        return best;
    }

    /** How many columns stand within a block of the top: how broad the rock's cap is. */
    static int cap(RockModel model, int[] tip) {
        int cap = 0;
        for (int x = model.minX; x < model.minX + model.sizeX; x++) {
            for (int z = model.minZ; z < model.minZ + model.sizeZ; z++) if (model.highest(x, z) >= tip[2] - 1) cap++;
        }
        return cap;
    }

}
