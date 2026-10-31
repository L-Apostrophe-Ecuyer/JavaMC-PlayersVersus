package frootloops.versus.mod.environment.worldgen.features;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Feature type {@code players-versus:mushroom_ring}: a patch of mushroom grown into a cave's stone, as rings round the
 * spot it starts from: a stem core, brown cap round it, and a ragged edge where the cap thins out into the stone. Only
 * the stone open to the air turns, so it lies on the floor, a wall or the ceiling alike, whichever the origin, a spot in
 * the air, finds first in a random direction.
 *
 * @param radius the patch's radius, at most 6, so that it stays within the chunks a feature may write to
 */
public record MushroomRingFeature(IntProvider radius) implements Feature {

    public static final MapCodec<MushroomRingFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            IntProviders.codec(1, 6).fieldOf("radius").forGetter(MushroomRingFeature::radius)
    ).apply(instance, MushroomRingFeature::new));

    /** How far from the origin the stone it grows on may be. */
    private static final int REACH = 8;
    /** Where the rings end, as fractions of the radius: the stem's, the cap's whole. */
    private static final double STEM = 0.3, CAP = 0.7;
    /** How far a ring's edge wanders, as a fraction of the radius. */
    private static final double RAGGED = 0.3;

    private static final BlockState STEM_STATE = Blocks.MUSHROOM_STEM.defaultBlockState();
    private static final BlockState CAP_STATE = Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState();

    @Override
    public MapCodec<MushroomRingFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!level.getBlockState(origin).isAir()) return false;
        Direction towards = Direction.values()[random.nextInt(Direction.values().length)];
        BlockPos.MutableBlockPos centre = origin.mutable();
        for (int i = 0; i < REACH && level.getBlockState(centre).isAir(); i++) centre.move(towards);
        if (!level.getBlockState(centre).is(BlockTags.BASE_STONE_OVERWORLD)) return false;

        int radius = this.radius.sample(random);
        boolean placed = false;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
            double distance = Math.sqrt(pos.distSqr(centre)) / radius + (random.nextDouble() - 0.5) * RAGGED;
            if (distance > 1.0) continue;
            BlockState ring = ring(distance, random);
            if (ring == null || !level.getBlockState(pos).is(BlockTags.BASE_STONE_OVERWORLD) || !open(level, pos)) continue;
            level.setBlock(pos, ring, Block.UPDATE_CLIENTS);
            placed = true;
        }
        return placed;
    }

    /** What grows this far out (a fraction of the radius): stem, cap, then cap less and less often; null for stone. */
    private static BlockState ring(double distance, RandomSource random) {
        if (distance < STEM) return STEM_STATE;
        if (distance < CAP) return CAP_STATE;
        return random.nextDouble() * (1.0 - CAP) > distance - CAP ? CAP_STATE : null;
    }

    /** Whether the block is open to the air on a side: the stone of the cave's surface, not the rock behind it. */
    private static boolean open(WorldGenLevel level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            if (level.getBlockState(pos.relative(side)).isAir()) return true;
        }
        return false;
    }
}
