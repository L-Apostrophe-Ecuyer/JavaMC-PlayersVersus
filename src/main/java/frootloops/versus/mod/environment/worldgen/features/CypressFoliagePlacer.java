package frootloops.versus.mod.environment.worldgen.features;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import frootloops.versus.mod.environment.worldgen.CustomWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.material.Fluids;

/**
 * The cypress's crown: a fuzzy pear down its trunk, as the owner's picture of a "Medium Cypress Tree". From its foot (a
 * block or two above the ground) up: a narrow foot (a plus around the trunk), the wide lower part (the square of the
 * crown's {@code radius}, and now and then a leaf one block further out: fuzz), the middle (the square with ragged
 * corners, a little fuzz), the top (a plus), then a spire of two or three single leaves over the trunk's top. Its leaves
 * are the tree's foliage (spruce leaves), with the {@code highlight_provider}'s (cypress leaves) for highlights, the more
 * the higher up: about one leaf in six at the foot, two in three at the top, the spire nearly all of them.
 */
public class CypressFoliagePlacer extends FoliagePlacer {

    public static final MapCodec<CypressFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(instance -> foliagePlacerParts(instance)
            .and(BlockStateProvider.CODEC.fieldOf("highlight_provider").forGetter(placer -> placer.highlights))
            .apply(instance, CypressFoliagePlacer::new));

    /** Where the parts of the crown begin, as a share of its height from its foot (0) to its top row (1). */
    static final double WIDE_FROM = 0.1, MIDDLE_FROM = 0.45, TOP_FROM = 0.75;
    /** How often a leaf sticks out one block further: in the wide part, in the middle. */
    static final double WIDE_FUZZ = 0.35, MIDDLE_FUZZ = 0.1;
    /** How often a corner of the middle's square stays. */
    static final double MIDDLE_CORNERS = 0.6;
    /** How often a leaf is a highlight: at the foot, and how much more at the top; fuzz and spire more often. */
    static final double HIGHLIGHT_FOOT = 0.15, HIGHLIGHT_RISE = 0.55, HIGHLIGHT_FUZZ = 0.2, HIGHLIGHT_SPIRE = 0.9;

    private final Holder<BlockStateProvider> highlights;

    public CypressFoliagePlacer(IntProvider radius, IntProvider offset, Holder<BlockStateProvider> highlights) {
        super(radius, offset);
        this.highlights = highlights;
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return CustomWorldgen.CYPRESS_FOLIAGE_PLACER;
    }

    /** Down the trunk to a block or two above the ground. */
    @Override
    public int foliageHeight(RandomSource random, int treeHeight, TreeFeature tree) {
        return treeHeight - 1 - random.nextInt(2);
    }

    @Override
    protected boolean shouldSkipLocation(RandomSource random, int dx, int y, int dz, int currentRadius, boolean doubleTrunk) {
        return false;
    }

    @Override
    protected void createFoliage(WorldGenLevel level, FoliageSetter foliageSetter, RandomSource random, TreeFeature tree, int treeHeight,
                                 FoliageAttachment foliageAttachment, int foliageHeight, int leafRadius, int offset) {
        // The attachment is the block over the trunk's top: the spire stands from there up
        BlockPos top = foliageAttachment.pos().above(offset);
        int spire = 2 + random.nextInt(2);
        for (int y = 0; y < spire; y++) this.leaf(level, foliageSetter, random, tree, top.above(y), HIGHLIGHT_SPIRE);
        // The crown's rows down the trunk, from the trunk's top (depth 1) to its foot
        for (int depth = 1; depth <= foliageHeight; depth++) {
            double height = 1.0 - (double) (depth - 1) / Math.max(1, foliageHeight - 1);
            this.row(level, foliageSetter, random, tree, top.below(depth), Math.max(1, leafRadius), height);
        }
    }

    /** One row of the crown around {@code middle}, its shape by its height in the crown (0 the foot, 1 the top). */
    private void row(WorldGenLevel level, FoliageSetter setter, RandomSource random, TreeFeature tree, BlockPos middle, int radius, double height) {
        double highlight = HIGHLIGHT_FOOT + HIGHLIGHT_RISE * height;
        boolean square = height >= WIDE_FROM && height < TOP_FROM;
        double fuzz = height < WIDE_FROM || height >= TOP_FROM ? 0.0 : height < MIDDLE_FROM ? WIDE_FUZZ : MIDDLE_FUZZ;
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                int ax = Math.abs(dx), az = Math.abs(dz);
                boolean inSquare = ax <= radius && az <= radius;
                if (inSquare) {
                    boolean corner = ax == radius && az == radius;
                    // The foot and the top are a plus; the middle's corners are ragged
                    if (!square && ax + az > radius) continue;
                    if (corner && height >= MIDDLE_FROM && random.nextDouble() >= MIDDLE_CORNERS) continue;
                    this.leaf(level, setter, random, tree, middle.offset(dx, 0, dz), highlight);
                } else if (Math.min(ax, az) <= radius && fuzz > 0.0 && random.nextDouble() < fuzz) {
                    // One block out from a side of the square (never from a corner: no leaf only touching an edge)
                    this.leaf(level, setter, random, tree, middle.offset(dx, 0, dz), highlight + HIGHLIGHT_FUZZ);
                }
            }
        }
    }

    /** A leaf, as the foliage placers place theirs, the highlight's leaves this often. */
    private void leaf(WorldGenLevel level, FoliageSetter setter, RandomSource random, TreeFeature tree, BlockPos pos, double highlight) {
        boolean persistent = level.isStateAtPosition(pos, state -> state.getValueOrElse(BlockStateProperties.PERSISTENT, false));
        if (persistent || !TreeFeature.validTreePos(level, pos)) return;
        Holder<BlockStateProvider> provider = random.nextDouble() < highlight ? this.highlights : tree.foliageProvider();
        BlockState state = provider.value().getState(level, random, pos);
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, level.isFluidAtPosition(pos, fluid -> fluid.isSourceOfType(Fluids.WATER)));
        }
        setter.set(pos, state);
    }
}
