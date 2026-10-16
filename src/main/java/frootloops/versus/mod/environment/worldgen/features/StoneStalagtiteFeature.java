package frootloops.versus.mod.environment.worldgen.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntPredicate;
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
 * and floor. With a pale oak wood or tuff reach the pair grades from stone at the ceiling and floor through tuff to pale
 * oak wood around the middle (the Pale Grotto's pillars), and can hold a creaking heart in its centre.
 *
 * @param floorToCeilingSearchRange how far up and down to look for the ceiling and floor
 * @param paleOakWoodReach how far from the middle pale oak wood reaches, as a fraction of half the floor-to-ceiling height
 * @param tuffReach how far from the middle tuff reaches past the wood, as the same fraction; stone beyond
 * @param creakingHeartChance chance for a pillar to get a creaking heart in its centre column, between the pale oak wood
 *                            above and below that lets it wake
 */
public record StoneStalagtiteFeature(int floorToCeilingSearchRange, float paleOakWoodReach, float tuffReach,
                                     float creakingHeartChance) implements Feature {

    public static final MapCodec<StoneStalagtiteFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("floorToCeilingSearchRange").forGetter(StoneStalagtiteFeature::floorToCeilingSearchRange),
            Codec.floatRange(0.0f, 1.0f).optionalFieldOf("paleOakWoodReach", 0.0f).forGetter(StoneStalagtiteFeature::paleOakWoodReach),
            Codec.floatRange(0.0f, 1.0f).optionalFieldOf("tuffReach", 0.0f).forGetter(StoneStalagtiteFeature::tuffReach),
            Codec.floatRange(0.0f, 1.0f).optionalFieldOf("creakingHeartChance", 0.0f).forGetter(StoneStalagtiteFeature::creakingHeartChance)
    ).apply(instance, StoneStalagtiteFeature::new));

    private static final BlockState STALAGMITE_BLOCKSTATE = Blocks.STONE.defaultBlockState();
    private static final BlockState TUFF = Blocks.TUFF.defaultBlockState();
    private static final BlockState PALE_OAK_WOOD = Blocks.PALE_OAK_WOOD.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
    private static final BlockState CREAKING_HEART = Blocks.CREAKING_HEART.defaultBlockState()
            .setValue(CreakingHeartBlock.AXIS, Direction.Axis.Y)
            .setValue(CreakingHeartBlock.STATE, CreakingHeartState.DORMANT)
            .setValue(CreakingHeartBlock.NATURAL, true);
    /** How far each way, as a fraction of half the height, the stone, tuff and wood blend into each other. */
    static final float GRADIENT_BLEND = 0.2f;

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

        // The gradient runs from the middle of the gap at the origin out to its floor and ceiling.
        double middle = (bounded.floor() + bounded.ceiling()) / 2.0;
        double halfHeight = (bounded.ceiling() - bounded.floor()) / 2.0;
        Gradient gradient = new Gradient(middle, halfHeight, this.paleOakWoodReach, this.tuffReach);
        StoneStalagmiteGenerator generatorCeiling = createGenerator(blockPos.atY(bounded.ceiling() - 1), false, radius, stalactiteBluntness, heightScale, gradient);
        StoneStalagmiteGenerator generatorFloor = createGenerator(blockPos.atY(bounded.floor() + 1), true, radius, stalactiteBluntness, heightScale, gradient);
        WindModifier windModifier = generatorCeiling.generateWind() && generatorFloor.generateWind() ? new WindModifier(blockPos.getY(), random, windSpeed) : WindModifier.create();
        boolean canGenerateCeiling = generatorCeiling.canGenerate(structureWorldAccess, windModifier);
        boolean canGenerateFloor = generatorFloor.canGenerate(structureWorldAccess, windModifier);
        if (canGenerateCeiling) {
            generatorCeiling.generate(structureWorldAccess, random, windModifier);
        }
        if (canGenerateFloor) {
            generatorFloor.generate(structureWorldAccess, random, windModifier);
        }
        if (this.creakingHeartChance > 0.0f && random.nextFloat() < this.creakingHeartChance) {
            heartY(middle, this.paleOakWoodReach * halfHeight, y -> structureWorldAccess.getBlockState(blockPos.atY(y)).is(Blocks.PALE_OAK_WOOD))
                    .ifPresent(y -> structureWorldAccess.setBlock(blockPos.atY(y), CREAKING_HEART, Block.UPDATE_CLIENTS));
        }
        return true;
    }

    private static StoneStalagmiteGenerator createGenerator(BlockPos pos, boolean isStalagmite, int scale, float bluntness, float heightScale, Gradient gradient) {
        return new StoneStalagmiteGenerator(pos, isStalagmite, scale, bluntness, heightScale, gradient);
    }

    /**
     * What a pillar is made of at each height: pale oak wood within {@code woodReach} of the middle, tuff within
     * {@code tuffReach}, stone further out, as fractions of half the floor-to-ceiling height, blended by
     * {@link #GRADIENT_BLEND}. Plain stone without either reach.
     */
    record Gradient(double middle, double halfHeight, float woodReach, float tuffReach) {
        BlockState blockAt(int y, RandomSource random) {
            if (this.woodReach <= 0.0f && this.tuffReach <= 0.0f) return STALAGMITE_BLOCKSTATE;
            double distance = Math.abs(y - this.middle) / this.halfHeight + (random.nextFloat() * 2.0f - 1.0f) * GRADIENT_BLEND;
            return material(Math.max(distance, 0.0), this.woodReach, this.tuffReach);
        }
    }

    /** The block at {@code distance} from a pillar's middle, as a fraction of half its height. */
    static BlockState material(double distance, float woodReach, float tuffReach) {
        return distance < woodReach ? PALE_OAK_WOOD : distance < tuffReach ? TUFF : STALAGMITE_BLOCKSTATE;
    }

    /**
     * Where a pillar's creaking heart goes: the height in its centre column nearest the middle, at most {@code reach}
     * away, with pale oak wood there and right above and below, which the heart needs to wake.
     */
    static OptionalInt heartY(double middle, double reach, IntPredicate paleOakWoodAt) {
        int centre = Mth.floor(middle);
        for (int i = 0; i <= 2 * Mth.ceil(reach) + 1; ++i) {
            int y = centre + ((i & 1) == 1 ? (i + 1) / 2 : -i / 2);
            if (Math.abs(y - middle) <= reach && paleOakWoodAt.test(y - 1) && paleOakWoodAt.test(y) && paleOakWoodAt.test(y + 1)) {
                return OptionalInt.of(y);
            }
        }
        return OptionalInt.empty();
    }

    static final class StoneStalagmiteGenerator {
        private BlockPos pos;
        private final boolean isStalagmite;
        private int scale;
        private final double bluntness;
        private final double heightScale;
        private final Gradient gradient;

        StoneStalagmiteGenerator(BlockPos pos, boolean isStalagmite, int scale, double bluntness, double heightScale, Gradient gradient) {
            this.pos = pos;
            this.isStalagmite = isStalagmite;
            this.scale = scale;
            this.bluntness = bluntness;
            this.heightScale = heightScale;
            this.gradient = gradient;
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
                            world.setBlock(blockPos, this.gradient.blockAt(blockPos.getY(), random), Block.UPDATE_CLIENTS);
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
