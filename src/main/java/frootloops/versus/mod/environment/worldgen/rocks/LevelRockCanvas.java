package frootloops.versus.mod.environment.worldgen.rocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A rock's canvas in the world, while it generates ({@link RockFeature}, {@link RockFormationPiece}) or after
 * ({@code /pvwg rock}): the palette's blocks are looked up once, the ground is the height map that skips water and
 * plants, and rock only takes the place of what nature put there (air, water, plants, earth, sand, gravel, stone), never
 * of what someone built, nor of trees.
 */
public final class LevelRockCanvas implements RockCanvas {

    private final WorldGenLevel level;
    private final BlockState[] blocks = new BlockState[RockMaterial.values().length];
    private final List<BlockState> plants = new ArrayList<>();
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private int placed;

    public LevelRockCanvas(WorldGenLevel level, RockPalette palette) {
        this.level = level;
        for (RockMaterial material : RockMaterial.values()) this.blocks[material.ordinal()] = state(palette.block(material));
        for (String plant : palette.plants()) this.plants.add(state(plant));
    }

    /** The first block id of a palette that isn't a block, or null when all are. */
    @Nullable
    public static String unknownBlock(RockPalette palette) {
        List<String> ids = new ArrayList<>(palette.blocks().values());
        ids.addAll(palette.plants());
        for (String id : ids) {
            Identifier identifier = Identifier.tryParse(id);
            if (identifier == null || BuiltInRegistries.BLOCK.getOptional(identifier).isEmpty()) return id;
        }
        return null;
    }

    private static BlockState state(String id) {
        return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).orElseThrow(() -> new IllegalArgumentException("No block " + id)).defaultBlockState();
    }

    /** How many blocks it has put in the world. */
    public int placed() {
        return this.placed;
    }

    @Override
    public int ground(int x, int z) {
        return this.level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
    }

    @Override
    public boolean water(int x, int y, int z) {
        return this.level.getFluidState(this.pos.set(x, y, z)).is(FluidTags.WATER);
    }

    @Override
    public boolean replaceable(int x, int y, int z) {
        BlockState state = this.level.getBlockState(this.pos.set(x, y, z));
        if (state.is(BlockTags.FEATURES_CANNOT_REPLACE) || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) return false;
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.SUBSTRATE_OVERWORLD) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(BlockTags.SNOW)
                || this.isOwn(state);
    }

    /** One of this rock's own blocks: rock may be stamped over rock, as two bodies of a formation meet. */
    private boolean isOwn(BlockState state) {
        for (BlockState block : this.blocks) {
            if (block != null && state.is(block.getBlock())) return true;
        }
        return false;
    }

    @Override
    public void rock(int x, int y, int z, RockMaterial material) {
        this.set(x, y, z, this.blocks[material.ordinal()]);
        // What stood on the old ground here can no longer stand
        this.clearUnsupported(x, y + 1, z);
    }

    @Override
    public void raise(int x, int z, int from, int to) {
        BlockState old = this.level.getBlockState(this.pos.set(x, from - 1, z));
        BlockState top = old.isAir() || !old.getFluidState().isEmpty() || !old.isSolid() ? Blocks.GRASS_BLOCK.defaultBlockState() : old;
        // Under grass, podzol, mycelium or moss the ground is dirt; dirt, mud, sand, gravel and stone go on as they are
        BlockState fill = living(top) || top.is(BlockTags.DIRT) ? Blocks.DIRT.defaultBlockState() : top;
        if (living(old)) this.set(x, from - 1, z, Blocks.DIRT.defaultBlockState());
        // Nothing lives on the ground under water
        if (living(top) && this.water(x, to, z)) top = Blocks.DIRT.defaultBlockState();
        for (int y = from; y < to; y++) this.set(x, y, z, y == to - 1 ? top : fill);
        this.clearUnsupported(x, to, z);
    }

    /** Ground that grows: it can't be buried, nor lie under water. */
    private static boolean living(BlockState state) {
        return state.is(BlockTags.GRASS_BLOCKS) || state.is(BlockTags.MOSS_BLOCKS);
    }

    @Override
    public void plant(int x, int y, int z, int data) {
        if (this.plants.isEmpty() || !this.level.getBlockState(this.pos.set(x, y, z)).isAir()) return;
        BlockState plant = this.plants.get(Math.floorMod(data, this.plants.size()));
        if (plant.canSurvive(this.level, this.pos.set(x, y, z))) this.set(x, y, z, plant);
    }

    private void set(int x, int y, int z, BlockState state) {
        if (this.level.setBlock(this.pos.set(x, y, z), state, Block.UPDATE_CLIENTS)) this.placed++;
    }

    /** Removes a plant (or the lower half of a tall one's upper half) that the block under it no longer holds up. */
    private void clearUnsupported(int x, int y, int z) {
        BlockState above = this.level.getBlockState(this.pos.set(x, y, z));
        if (above.isAir() || !above.canBeReplaced() || !above.getFluidState().isEmpty() || above.canSurvive(this.level, this.pos)) return;
        this.level.setBlock(this.pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        BlockState upper = this.level.getBlockState(this.pos.set(x, y + 1, z));
        if (upper.is(above.getBlock())) this.level.setBlock(this.pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
}
