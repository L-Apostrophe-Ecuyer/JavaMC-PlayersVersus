package frootloops.versus.mod.environment.worldgen.features;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.CaveSurface;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class StoneStalagtiteFeature extends Feature<StoneStalagtiteFeatureConfig> {
    public StoneStalagtiteFeature(Codec<StoneStalagtiteFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<StoneStalagtiteFeatureConfig> context) {
        StructureWorldAccess structureWorldAccess = context.getWorld();
        BlockPos blockPos = context.getOrigin();
        StoneStalagtiteFeatureConfig config = context.getConfig();
        Random random = context.getRandom();
        if (!StoneStalagtiteHelper.canGenerate(structureWorldAccess, blockPos)) {
            return false;
        }

        int floorToCeilingSearchRange = config.floorToCeilingSearchRange();
        float maxColumnRadiusToCaveHeightRatio = 0.33f;
        float stalactiteBluntness = 0.4f;
        int columnRadiusMin = 2;
        int columnRadiusMax = 4;
        float heightScale = 12.0f;
        float windSpeed = 0.1f;


        Optional<CaveSurface> optional = CaveSurface.create(structureWorldAccess, blockPos, floorToCeilingSearchRange, StoneStalagtiteHelper::canGenerate, StoneStalagtiteHelper::canReplaceOrLava);
        if (optional.isEmpty() || !(optional.get() instanceof CaveSurface.Bounded)) {
            return false;
        }
        CaveSurface.Bounded bounded = (CaveSurface.Bounded)optional.get();
        if (bounded.getHeight() < 4) {
            return false;
        }
        int i = (int)((float)bounded.getHeight() * maxColumnRadiusToCaveHeightRatio);
        int j = MathHelper.clamp(i, columnRadiusMin, columnRadiusMax);
        int k = MathHelper.nextBetween(random, columnRadiusMin, j);

        StoneStalagmiteGenerator generatorCeiling = createGenerator(blockPos.withY(bounded.getCeiling() - 1), false, random, k, stalactiteBluntness, heightScale);
        StoneStalagmiteGenerator generatorFloor = createGenerator(blockPos.withY(bounded.getFloor() + 1), true, random, k, stalactiteBluntness, heightScale);
        WindModifier windModifier = generatorCeiling.generateWind(config) && generatorFloor.generateWind(config) ? new WindModifier(blockPos.getY(), random, windSpeed) : WindModifier.create();
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

    private static StoneStalagmiteGenerator createGenerator(BlockPos pos, boolean isStalagmite, Random random, int scale, float bluntness, float heightScale) {
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

        boolean canGenerate(StructureWorldAccess world, WindModifier wind) {
            while (this.scale > 1) {
                BlockPos.Mutable mutable = this.pos.mutableCopy();
                int i = Math.min(10, this.getBaseScale());
                for (int j = 0; j < i; ++j) {
                    if (world.getBlockState(mutable).isOf(Blocks.LAVA)) {
                        return false;
                    }
                    if (StoneStalagtiteHelper.canGenerateBase(world, wind.modify(mutable), this.scale)) {
                        this.pos = mutable;
                        return true;
                    }
                    mutable.move(this.isStalagmite ? Direction.DOWN : Direction.UP);
                }
                this.scale /= 2;
            }
            return false;
        }

        private int scale(float height) {
            return (int)StoneStalagtiteHelper.scaleHeightFromRadius(height, this.scale, this.heightScale, this.bluntness);
        }

        void generate(StructureWorldAccess world, Random random, WindModifier wind) {
            for (int i = -this.scale; i <= this.scale; ++i) {
                forJ: for (int j = -this.scale; j <= this.scale; ++j) {
                    int scale;
                    float distance = MathHelper.sqrt(i * i + j * j);
                    if (distance > (float)this.scale || (scale = this.scale(distance)) <= 0) continue;
                    if ((double)random.nextFloat() < 0.2) {
                        scale = (int)((float)scale * MathHelper.nextBetween(random, 0.8f, 1.0f));
                    }
                    BlockPos.Mutable mutable = this.pos.add(i, 0, j).mutableCopy();
                    boolean hasPlacedBlock = false;
                    int topY = this.isStalagmite ? world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, mutable.getX(), mutable.getZ()) : Integer.MAX_VALUE;
                    for (int m = 0; m < scale && mutable.getY() < topY; ++m) {
                        BlockPos blockPos = wind.modify(mutable);
                        if (StoneStalagtiteHelper.canGenerateOrLava(world, blockPos)) {
                            hasPlacedBlock = true;
                            world.setBlockState(blockPos, Blocks.STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
                        } else if (hasPlacedBlock && world.getBlockState(blockPos).isIn(BlockTags.BASE_STONE_OVERWORLD)) {
                            continue forJ;
                        }
                        mutable.move(this.isStalagmite ? Direction.UP : Direction.DOWN);
                    }
                }
            }
        }

        boolean generateWind(StoneStalagtiteFeatureConfig config) {
            return this.scale >= 0.0 && this.bluntness >= (double)0.6;
        }
    }

    static final class WindModifier {
        private final int y;
        @Nullable
        private final Vec3d wind;

        WindModifier(int y, Random random, float wind) {
            this.y = y;
            float g = MathHelper.nextBetween(random, 0.0f, (float)Math.PI);
            this.wind = new Vec3d(MathHelper.cos(g) * wind, 0.0, MathHelper.sin(g) * wind);
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
            Vec3d vec3d = this.wind.multiply(i);
            return pos.add(MathHelper.floor(vec3d.x), 0, MathHelper.floor(vec3d.z));
        }
    }
}
