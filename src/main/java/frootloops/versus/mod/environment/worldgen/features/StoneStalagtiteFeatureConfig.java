package frootloops.versus.mod.environment.worldgen.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record StoneStalagtiteFeatureConfig(int floorToCeilingSearchRange) implements FeatureConfiguration {

    public static final Codec<StoneStalagtiteFeatureConfig> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                            // you can add as many of these as you want, one for each parameter
                            ExtraCodecs.POSITIVE_INT.fieldOf("floorToCeilingSearchRange").forGetter(StoneStalagtiteFeatureConfig::floorToCeilingSearchRange)
                    ).apply(instance, StoneStalagtiteFeatureConfig::new));
}