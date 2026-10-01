package frootloops.versus.mod.environment.worldgen.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
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
 * @param paleOakLogChance chance for generated stone to be pale oak logs
 * @param creakingHeartChance chance for adjacent pale oak logs to contain a creaking heart
 */
public record StoneStalagtiteFeature(int floorToCeilingSearchRange, float paleOakLogChance, float creakingHeartChance) implements Feature {

    public static final MapCodec<StoneStalagtiteFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("floorToCeilingSearchRange").forGetter(StoneStalagtiteFeature::floorToCeilingSearchRange),
            Codec.floatRange(0.0f, 1.0f).optionalFieldOf("paleOakLogChance", 0.0f).forGetter(StoneStalagtiteFeature::paleOakLogChance),
            Codec.floatRange(0.0f, 1.0f).optionalFieldOf("creakingHeartChance", 0.0f).forGetter(StoneStalagtiteFeature::creakingHeartChance)
    ).apply(instance, StoneStalagtiteFeature::new));

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

        StoneStalagmiteGenerator generatorCeiling = createGenerator(blockPos.atY(bounded.ceiling() - 1), false, radius, stalactiteBluntness, heightScale);
        StoneStalagmiteGenerator generatorFloor = createGenerator(blockPos.atY(bounded.floor() + 1), true, radius, stalactiteBluntness, heightScale);
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

    private StoneStalagmiteGenerator createGenerator(BlockPos pos, boolean isStalagmite, int scale, float bluntness, float heightScale) {
        return new StoneStalagmiteGenerator(pos, isStalagmite, scale, bluntness, heightScale, this.paleOakLogChance, this.creakingHeartChance);
    }

    static final class StoneStalagmiteGenerator {
        private BlockPos pos;
        private final boolean isStalagmite;
        private int scale;
        private final double bluntness;
        private final double heightScale;
        private final float paleOakLogChance;
        private final float creakingHeartChance;

        StoneStalagmiteGenerator(BlockPos pos, boolean isStalagmite, int scale, double bluntness, double heightScale,
                                 float paleOakLogChance, float creakingHeartChance) {
            this.pos = pos;
            this.isStalagmite = isStalagmite;
            this.scale = scale;
            this.bluntness = bluntness;
            this.heightScale = heightScale;
            this.paleOakLogChance = paleOakLogChance;
            this.creakingHeartChance = creakingHeartChance;
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
            List<BlockPos> paleOakLogs = this.creakingHeartChance > 0.0f ? new ArrayList<>() : List.of();
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
                            if (this.paleOakLogChance > 0.0f && random.nextFloat() < this.paleOakLogChance) {
                                world.setBlock(blockPos, Blocks.PALE_OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y), Block.UPDATE_CLIENTS);
                                if (this.creakingHeartChance > 0.0f) {
                                    paleOakLogs.add(blockPos.immutable());
                                }
                            } else {
                                world.setBlock(blockPos, STALAGMITE_BLOCKSTATE, Block.UPDATE_CLIENTS);
                            }
                        } else if (hasPlacedBlock && StoneStalagtiteHelper.canReplace(state)) {
                            continue forZ;
                        }
                        mutable.move(this.isStalagmite ? Direction.UP : Direction.DOWN);
                    }
                }
            }
            for (BlockPos logPos : paleOakLogs) {
                if (random.nextFloat() < this.creakingHeartChance
                        && world.getBlockState(logPos.above()).is(Blocks.PALE_OAK_LOG)
                        && world.getBlockState(logPos.below()).is(Blocks.PALE_OAK_LOG)) {
                    BlockState heart = Blocks.CREAKING_HEART.defaultBlockState()
                            .setValue(CreakingHeartBlock.AXIS, Direction.Axis.Y)
                            .setValue(CreakingHeartBlock.STATE, CreakingHeartState.DORMANT)
                            .setValue(CreakingHeartBlock.NATURAL, true);
                    world.setBlock(logPos, heart, Block.UPDATE_CLIENTS);
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
