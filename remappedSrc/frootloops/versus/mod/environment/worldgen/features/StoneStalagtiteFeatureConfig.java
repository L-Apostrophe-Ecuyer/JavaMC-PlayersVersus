package frootloops.versus.mod.environment.worldgen.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.gen.feature.FeatureConfig;

public record StoneStalagtiteFeatureConfig(int floorToCeilingSearchRange) implements FeatureConfig {

    public static final Codec<StoneStalagtiteFeatureConfig> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                            // you can add as many of these as you want, one for each parameter
                            Codecs.POSITIVE_INT.fieldOf("floorToCeilingSearchRange").forGetter(StoneStalagtiteFeatureConfig::floorToCeilingSearchRange)
                    ).apply(instance, StoneStalagtiteFeatureConfig::new));
}