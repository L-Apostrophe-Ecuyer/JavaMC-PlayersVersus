package frootloops.versus.mod.environment.worldgen.rocks;

import frootloops.versus.mod.environment.worldgen.debug.WorldgenCensus;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Feature type {@code players-versus:rock}: a small rock (a boulder, a pile, a crag) drawn from its template with the
 * feature's own random, each of its bodies standing on the ground at its own middle. In the data:
 * <pre>{"type": "players-versus:rock", "rock": {"bodies": [{"shape": "boulder"}]}}</pre>
 * A feature may only write in the chunks around its own, so a template that reaches further than {@link #MAX_REACH}
 * blocks from its middle is an error when the data loads; larger rocks are formations ({@link RockFormationStructure}).
 */
public record RockFeature(RockTemplate rock) implements Feature {

    /** How far from its origin, along x or z, a feature may write: anywhere in the chunks around its own. */
    public static final int MAX_REACH = 16;

    /** A rock's template, read from its JSON with {@link RockTemplate#fromJson}; a mistake names its field. */
    public static final Codec<RockTemplate> TEMPLATE_CODEC = ExtraCodecs.JSON.comapFlatMap(RockFeature::read, RockTemplate::toJson);

    public static final MapCodec<RockFeature> CODEC = RecordCodecBuilder.<RockFeature>mapCodec(instance -> instance.group(
            TEMPLATE_CODEC.fieldOf("rock").forGetter(RockFeature::rock)
    ).apply(instance, RockFeature::new)).validate(feature -> feature.rock.reach() <= MAX_REACH ? DataResult.success(feature)
            : DataResult.error(() -> "rock: reaches " + feature.rock.reach() + " blocks from its middle, and a feature may only reach " + MAX_REACH
            + " (make it a rock formation)"));

    static DataResult<RockTemplate> read(JsonElement json) {
        if (!json.isJsonObject()) return DataResult.error(() -> "rock: an object with bodies");
        try {
            RockTemplate template = RockTemplate.fromJson(json.getAsJsonObject());
            String unknown = LevelRockCanvas.unknownBlock(template.palette());
            return unknown == null ? DataResult.success(template) : DataResult.error(() -> "rock: palette: no block " + unknown);
        } catch (IllegalArgumentException mistake) {
            return DataResult.error(() -> "rock: " + mistake.getMessage());
        }
    }

    @Override
    public MapCodec<RockFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        RockPlan plan = this.rock.plan(random.nextLong());
        LevelRockCanvas canvas = new LevelRockCanvas(level, plan.palette());
        int x = origin.getX(), z = origin.getZ();
        RockStamper.stamp(plan, x, z, canvas, x - MAX_REACH, z - MAX_REACH, x + MAX_REACH, z + MAX_REACH);
        if (canvas.placed() == 0) return false;
        WorldgenCensus.rockPlaced(level, origin);
        return true;
    }
}
