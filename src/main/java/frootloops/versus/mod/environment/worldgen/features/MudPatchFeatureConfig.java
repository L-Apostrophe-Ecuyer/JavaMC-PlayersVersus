package frootloops.versus.mod.environment.worldgen.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record MudPatchFeatureConfig(boolean isBrownMud, IntProvider size) implements FeatureConfiguration {

    public static final Codec<MudPatchFeatureConfig> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                            Codec.BOOL.fieldOf("is_brown_mud").forGetter(config -> config.isBrownMud),
                            IntProvider.codec(0, 16).fieldOf("size").forGetter(config -> config.size)
                    )
                    .apply(instance, MudPatchFeatureConfig::new)
    );
}
