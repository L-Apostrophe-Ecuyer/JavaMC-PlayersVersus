package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.features.StoneStalagtiteFeature;
import frootloops.versus.mod.environment.worldgen.features.StoneStalagtiteFeatureConfig;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;

public class CustomWorldgen {
    public static final Identifier STONE_STALAGTITE_ID = Identifier.of(VersusMod.MOD_ID, "stone_stalagtite");
    public static final StoneStalagtiteFeature STONE_STALAGTITE_FEATURE = new StoneStalagtiteFeature( StoneStalagtiteFeatureConfig.CODEC);

    public static final ConfiguredFeature<StoneStalagtiteFeatureConfig, StoneStalagtiteFeature> STONE_STALAGTITE_FEATURE_CONFIGURED = new ConfiguredFeature<>(STONE_STALAGTITE_FEATURE,
            new StoneStalagtiteFeatureConfig(16)
    );

    public static void onInitialize() {
        Registry.register(Registries.FEATURE, STONE_STALAGTITE_ID, STONE_STALAGTITE_FEATURE);
    }

}
