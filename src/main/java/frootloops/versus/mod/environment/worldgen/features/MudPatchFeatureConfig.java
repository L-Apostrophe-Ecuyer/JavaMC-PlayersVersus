package frootloops.versus.mod.environment.worldgen.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.gen.feature.FeatureConfig;

public record MudPatchFeatureConfig(boolean isBrownMud, IntProvider size) implements FeatureConfig {

    public static final Codec<MudPatchFeatureConfig> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                            Codec.BOOL.fieldOf("is_brown_mud").forGetter(config -> config.isBrownMud),
                            IntProvider.createValidatingCodec(0, 16).fieldOf("size").forGetter(config -> config.size)
                    )
                    .apply(instance, MudPatchFeatureConfig::new)
    );
}
