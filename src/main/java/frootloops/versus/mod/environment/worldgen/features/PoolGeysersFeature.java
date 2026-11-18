package frootloops.versus.mod.environment.worldgen.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/**
 * Feature type {@code players-versus:pool_geysers}: geysers on the floor of the pool around its origin, as the sulfur
 * pools place them after their lake. Of the pool's floor, the solid blocks under its water, a {@code share} (rounded up,
 * and at least {@code minimum}: all of it on a smaller floor), chosen at random, become the {@code vent} (potent sulfur)
 * with the {@code heat} (a magma block) beneath: a geyser, erupting now and then. Only where both blocks pass
 * {@code can_replace}, hold no block entity and the one beneath is solid too, so no heat hangs into a cave below.
 *
 * <p>The pool is a lake's: the box from 8 blocks west, 4 down and 8 north of the origin, 16 by 8 by 16, its water in the
 * box's lower half; its floor is in the box's bottom three layers.
 */
public record PoolGeysersFeature(Holder<BlockStateProvider> vent, Holder<BlockStateProvider> heat, float share, int minimum,
                                 BlockPredicate canReplace) implements Feature {

    public static final MapCodec<PoolGeysersFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("vent").forGetter(PoolGeysersFeature::vent),
            BlockStateProvider.CODEC.fieldOf("heat").forGetter(PoolGeysersFeature::heat),
            Codec.floatRange(0.0F, 1.0F).fieldOf("share").forGetter(PoolGeysersFeature::share),
            Codec.intRange(0, 256).fieldOf("minimum").forGetter(PoolGeysersFeature::minimum),
            BlockPredicate.CODEC.fieldOf("can_replace").forGetter(PoolGeysersFeature::canReplace)
    ).apply(instance, PoolGeysersFeature::new));

    @Override
    public MapCodec<PoolGeysersFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        List<BlockPos> floor = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -4, -8), origin.offset(7, -2, 7))) {
            if (level.getFluidState(pos.above()).isSourceOfType(Fluids.WATER) && this.takes(level, pos) && this.takes(level, pos.below())) {
                floor.add(pos.immutable());
            }
        }
        if (floor.isEmpty()) return false;
        Util.shuffle(floor, random);
        int geysers = Math.min(floor.size(), Math.max(this.minimum, Mth.ceil(this.share * floor.size())));
        for (BlockPos pos : floor.subList(0, geysers)) {
            level.setBlock(pos.below(), this.heat.value().getState(level, random, pos.below()), Block.UPDATE_CLIENTS);
            level.setBlock(pos, this.vent.value().getState(level, random, pos), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    /** Whether a geyser's block may go here: solid ground, no fluid, no block entity, nothing the pools never replace. */
    private boolean takes(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isSolid() && state.getFluidState().isEmpty() && !state.hasBlockEntity() && this.canReplace.test(level, pos);
    }
}
