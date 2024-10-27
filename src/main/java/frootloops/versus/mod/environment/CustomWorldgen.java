package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.features.StoneStalagtiteFeature;
import frootloops.versus.mod.environment.worldgen.features.StoneStalagtiteFeatureConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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

    public static enum VeinType {
        COPPER(Blocks.COPPER_ORE.getDefaultState(), Blocks.RAW_COPPER_BLOCK.getDefaultState(), Blocks.DRIPSTONE_BLOCK.getDefaultState(), 32, 72),
        IRON(Blocks.DEEPSLATE_IRON_ORE.getDefaultState(), Blocks.RAW_IRON_BLOCK.getDefaultState(), Blocks.TUFF.getDefaultState(), -32, 8);

        public final BlockState ore;
        public final BlockState rawOreBlock;
        public final BlockState stone;
        public final int minY;
        public final int maxY;

        private VeinType(final BlockState ore, final BlockState rawOreBlock, final BlockState stone, final int minY, final int maxY) {
            this.ore = ore;
            this.rawOreBlock = rawOreBlock;
            this.stone = stone;
            this.minY = minY;
            this.maxY = maxY;
        }
    }

}
