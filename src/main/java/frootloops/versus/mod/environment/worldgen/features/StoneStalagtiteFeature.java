package frootloops.versus.mod.environment.worldgen.features;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.phys.Vec3;

/**
 * Feature type {@code players-versus:stone_stalagtite}: a stone stalactite and stalagmite pair between a cave's ceiling
 * and floor.
 *
 * @param floorToCeilingSearchRange how far up and down to look for the ceiling and floor
 */
public record StoneStalagtiteFeature(int floorToCeilingSearchRange) implements Feature {

    public static final MapCodec<StoneStalagtiteFeature> CODEC = ExtraCodecs.POSITIVE_INT.fieldOf("floorToCeilingSearchRange")
            .xmap(StoneStalagtiteFeature::new, StoneStalagtiteFeature::floorToCeilingSearchRange);

    private static final BlockState STALAGMITE_BLOCKSTATE = Blocks.STONE.defaultBlockState();

    @Override
    public MapCodec<StoneStalagtiteFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel structureWorldAccess, ChunkGenerator generator, RandomSource random, BlockPos blockPos) {
        if (!StoneStalagtiteHelper.isAirOrWater(structureWorldAccess, blockPos)) {
            return false;
        }

        int floorToCeilingSearchRange = this.floorToCeilingSearchRange;
        float stalactiteBluntness = 0.8f;
        int columnRadiusMin = 5;
        int columnRadiusMax = 9;
        float heightScale = 1.5f;
        float windSpeed = 0.0f;

        Optional<Column> optional = Column.scan(structureWorldAccess, blockPos, floorToCeilingSearchRange, StoneStalagtiteHelper::isAirOrWater, StoneStalagtiteHelper::canReplace);
        if (optional.isEmpty() || !(optional.get() instanceof Column.Range)) {
            return false;
        }
        Column.Range bounded = (Column.Range)optional.get();
        int height = bounded.height();
        if (height < columnRadiusMin) return false;
        int radius = Mth.randomBetweenInclusive(random, columnRadiusMin, Math.min(height, columnRadiusMax));

        StoneStalagmiteGenerator generatorCeiling = createGenerator(blockPos.atY(bounded.ceiling() - 1), false, random, radius, stalactiteBluntness, heightScale);
        StoneStalagmiteGenerator generatorFloor = createGenerator(blockPos.atY(bounded.floor() + 1), true, random, radius, stalactiteBluntness, heightScale);
        WindModifier windModifier = generatorCeiling.generateWind() && generatorFloor.generateWind() ? new WindModifier(blockPos.getY(), random, windSpeed) : WindModifier.create();
        boolean canGenerateCeiling = generatorCeiling.canGenerate(structureWorldAccess, windModifier);
        boolean canGenerateFloor = generatorFloor.canGenerate(structureWorldAccess, windModifier);
        if (canGenerateCeiling) {
            generatorCeiling.generate(structureWorldAccess, random, windModifier);
        }
        if (canGenerateFloor) {
            generatorFloor.generate(structureWorldAccess, random, windModifier);
        }
        return true;
    }

    private static StoneStalagmiteGenerator createGenerator(BlockPos pos, boolean isStalagmite, RandomSource random, int scale, float bluntness, float heightScale) {
        return new StoneStalagmiteGenerator(pos, isStalagmite, scale, bluntness, heightScale);
    }

    static final class StoneStalagmiteGenerator {
        private BlockPos pos;
        private final boolean isStalagmite;
        private int scale;
        private final double bluntness;
        private final double heightScale;

        StoneStalagmiteGenerator(BlockPos pos, boolean isStalagmite, int scale, double bluntness, double heightScale) {
            this.pos = pos;
            this.isStalagmite = isStalagmite;
            this.scale = scale;
            this.bluntness = bluntness;
            this.heightScale = heightScale;
        }

        private int getBaseScale() {
            return this.scale(0.0f);
        }

        private int getBottomY() {
            if (this.isStalagmite) {
                return this.pos.getY();
            }
            return this.pos.getY() - this.getBaseScale();
        }

        private int getTopY() {
            if (!this.isStalagmite) {
                return this.pos.getY();
            }
            return this.pos.getY() + this.getBaseScale();
        }

        boolean canGenerate(WorldGenLevel world, WindModifier wind) {
            while (this.scale > 1) {
                BlockPos.MutableBlockPos mutable = this.pos.mutable();
                int i = Math.min(10, this.getBaseScale());
                for (int j = 0; j < i; ++j) {
                    if (StoneStalagtiteHelper.canGenerateBase(world, wind.modify(mutable), this.scale)) {
                        this.pos = mutable;
                        return true;
                    }
                    mutable.move(this.isStalagmite ? Direction.DOWN : Direction.UP);
                }
                this.scale /= 2;
            }
            return !this.isStalagmite;
        }

        private int scale(float height) {
            return (int)StoneStalagtiteHelper.scaleHeightFromRadius(height, this.scale, this.heightScale, this.bluntness);
        }

        void generate(WorldGenLevel world, RandomSource random, WindModifier wind) {
            for (int x = -this.scale; x <= this.scale; ++x) {
                forZ: for (int z = -this.scale; z <= this.scale; ++z) {
                    int scale;
                    float distance = (x == 0 && z == 0) ? 1.0f : Mth.sqrt(x * x + z * z);
                    if (distance > (float)this.scale || (scale = this.scale(distance)) <= 0) continue;
                    if ((double)random.nextFloat() < 0.2) {
                        scale = (int)((float)scale * Mth.randomBetween(random, 0.8f, 1.0f));
                    }
                    BlockPos.MutableBlockPos mutable = this.pos.offset(x, 0, z).mutable();
                    boolean hasPlacedBlock = false;
                    int topY = this.isStalagmite ? world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, mutable.getX(), mutable.getZ()) : Integer.MAX_VALUE;
                    for (int y = 0; y < scale && mutable.getY() < topY; ++y) {
                        BlockPos blockPos = wind.modify(mutable);
                        BlockState state = world.getBlockState(blockPos);
                        if (StoneStalagtiteHelper.isAirOrWater(state)) {
                            hasPlacedBlock = true;
                            world.setBlock(blockPos, STALAGMITE_BLOCKSTATE, Block.UPDATE_CLIENTS);
                        } else if (hasPlacedBlock && StoneStalagtiteHelper.canReplace(state)) {
                            continue forZ;
                        }
                        mutable.move(this.isStalagmite ? Direction.UP : Direction.DOWN);
                    }
                }
            }
        }

        boolean generateWind() {
            return this.scale >= 0.0 && this.bluntness >= (double)0.6;
        }
    }

    static final class WindModifier {
        private final int y;
        @Nullable
        private final Vec3 wind;

        WindModifier(int y, RandomSource random, float wind) {
            this.y = y;
            float g = Mth.randomBetween(random, 0.0f, (float)Math.PI);
            this.wind = new Vec3(Mth.cos(g) * wind, 0.0, Mth.sin(g) * wind);
        }

        private WindModifier() {
            this.y = 0;
            this.wind = null;
        }

        static WindModifier create() {
            return new WindModifier();
        }

        BlockPos modify(BlockPos pos) {
            if (this.wind == null) {
                return pos;
            }
            int i = this.y - pos.getY();
            Vec3 vec3d = this.wind.scale(i);
            return pos.offset(Mth.floor(vec3d.x), 0, Mth.floor(vec3d.z));
        }
    }
}
