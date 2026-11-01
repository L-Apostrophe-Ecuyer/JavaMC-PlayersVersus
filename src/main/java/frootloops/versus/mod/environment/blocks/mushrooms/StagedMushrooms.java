package frootloops.versus.mod.environment.blocks.mushrooms;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.PaleLine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Small mushrooms growing in the world, by MushroomGrowth's rules: what's around one, and what its random tick does.
 * A mushroom's stage is how many grow together: vanilla's brown or red mushroom is one, their StagedMushroomBlock two or
 * three; a pale shroom one to three.
 */
public final class StagedMushrooms {
    /** What mushrooms grow on: the overworld's stones (deepslate and tuff with them), logs, mushroom blocks, mycelium. */
    public static final TagKey<Block> GROW_ON = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "mushrooms_grow_on"));

    /** Deepslate and its worked kinds: a brown or red mushroom on them is pale (tag {@code players-versus:deepslate}). */
    public static final TagKey<Block> DEEPSLATE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "deepslate"));

    private StagedMushrooms() {}

    /** The kind of small mushroom the state is, or null if it isn't one. */
    @Nullable
    public static MushroomGrowth.Kind kindOf(BlockState state) {
        if (state.is(Blocks.BROWN_MUSHROOM)) return MushroomGrowth.Kind.BROWN;
        if (state.is(Blocks.RED_MUSHROOM)) return MushroomGrowth.Kind.RED;
        return state.getBlock() instanceof StagedMushroomBlock staged ? staged.kind : null;
    }

    /** How many mushrooms grow together there: 1 to 3. */
    public static int stageOf(BlockState state) {
        return state.getBlock() instanceof StagedMushroomBlock ? state.getValue(StagedMushroomBlock.MUSHROOMS) : 1;
    }

    /** The block for this many mushrooms of the kind together. */
    public static BlockState stateOf(MushroomGrowth.Kind kind, int stage) {
        if (stage <= 1 && kind == MushroomGrowth.Kind.BROWN) return Blocks.BROWN_MUSHROOM.defaultBlockState();
        if (stage <= 1 && kind == MushroomGrowth.Kind.RED) return Blocks.RED_MUSHROOM.defaultBlockState();
        Block block = switch (kind) {
            case BROWN -> CustomBlocks.BROWN_MUSHROOMS;
            case RED -> CustomBlocks.RED_MUSHROOMS;
            case PALE -> CustomBlocks.PALE_SHROOM;
        };
        return block.defaultBlockState().setValue(StagedMushroomBlock.MUSHROOMS, Math.clamp(stage, 1, 3));
    }

    /** A random tick of a small mushroom: it grows, shrinks, turns pale or spreads, as MushroomGrowth has it. */
    public static void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        MushroomGrowth.Kind kind = kindOf(state);
        if (kind == null) return;
        int stage = stageOf(state);
        boolean lucky = random.nextFloat() < VersusSettings.Mushrooms.GROW_CHANCE;
        switch (MushroomGrowth.next(kind, stage, around(level, pos, kind))) {
            case GROW -> {
                if (lucky) level.setBlock(pos, stateOf(kind, stage + 1), Block.UPDATE_ALL);
            }
            case SHRINK -> level.setBlock(pos, stateOf(kind, stage - 1), Block.UPDATE_ALL);
            case TURN_PALE -> {
                if (lucky) level.setBlock(pos, stateOf(MushroomGrowth.Kind.PALE, 2), Block.UPDATE_ALL);
            }
            case SPREAD -> {
                if (lucky) spread(level, pos, kind, random);
            }
            case NONE -> {}
        }
    }

    /** What's around a mushroom of this kind there. */
    public static MushroomGrowth.Conditions around(ServerLevel level, BlockPos pos, MushroomGrowth.Kind kind) {
        BlockState below = level.getBlockState(pos.below());
        boolean mycelium = below.is(Blocks.MYCELIUM);
        boolean dark = mycelium || level.getRawBrightness(pos, 0) <= VersusSettings.Mushrooms.MAX_GROWING_LIGHT;
        // Paleness holds below y 0 and in the pale biomes, and wherever a mushroom stands on deepslate
        boolean pale = PaleLine.isPale(level, pos) || below.is(DEEPSLATE);
        boolean neighbour = false, grownNeighbour = false;
        for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (next.equals(pos)) continue;
            BlockState state = level.getBlockState(next);
            MushroomGrowth.Kind other = kindOf(state);
            // Where paleness holds, brown and red mushrooms turning pale count the pale shrooms beside them as their own
            boolean same = other == kind || other == MushroomGrowth.Kind.PALE && pale;
            if (!same) continue;
            neighbour = true;
            if (stageOf(state) >= 2) grownNeighbour = true;
        }
        return new MushroomGrowth.Conditions(dark, below.is(GROW_ON), neighbour, grownNeighbour, pale);
    }

    /** Three spread one mushroom of their kind beside them, where it can stand and their kind isn't crowded. */
    private static void spread(ServerLevel level, BlockPos pos, MushroomGrowth.Kind kind, RandomSource random) {
        BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
        // Spread onto deepslate, a brown or red mushroom is a pale shroom
        MushroomGrowth.Kind placed = kind != MushroomGrowth.Kind.PALE && level.getBlockState(target.below()).is(DEEPSLATE) ? MushroomGrowth.Kind.PALE : kind;
        BlockState mushroom = stateOf(placed, 1);
        if (target.equals(pos) || !level.isEmptyBlock(target) || !mushroom.canSurvive(level, target)) return;
        int crowd = 0;
        for (BlockPos next : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4))) {
            if (kindOf(level.getBlockState(next)) == kind && ++crowd >= VersusSettings.Mushrooms.SPREAD_CAP) return;
        }
        level.setBlock(target, mushroom, Block.UPDATE_ALL);
    }
}
