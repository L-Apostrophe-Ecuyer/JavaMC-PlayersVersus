package frootloops.versus.mod.environment.worldgen;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.features.MudPatchFeature;
import frootloops.versus.mod.environment.worldgen.features.MudPatchFeatureConfig;
import frootloops.versus.mod.environment.worldgen.features.StoneStalagtiteFeature;
import frootloops.versus.mod.environment.worldgen.features.StoneStalagtiteFeatureConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

public class CustomWorldgen {

    public static final Identifier BETTER_WORLDGEN_PRESET_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "better_world");
    public static final ResourceKey<WorldPreset> BETTER_WORLDGEN_PRESET = ResourceKey.create(Registries.WORLD_PRESET, BETTER_WORLDGEN_PRESET_ID);
    public static final ResourceKey<NoiseGeneratorSettings> OVERWORLD = ResourceKey.create(Registries.NOISE_SETTINGS, BETTER_WORLDGEN_PRESET_ID);


    public static final Identifier STONE_STALAGTITE_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "stone_stalagtite");
    public static final StoneStalagtiteFeature STONE_STALAGTITE_FEATURE = new StoneStalagtiteFeature(StoneStalagtiteFeatureConfig.CODEC);

    public static final Identifier MUD_PATCH_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "mud_patch");
    public static final MudPatchFeature MUD_PATCH_FEATURE = new MudPatchFeature(MudPatchFeatureConfig.CODEC);

    public static final ConfiguredFeature<StoneStalagtiteFeatureConfig, StoneStalagtiteFeature> STONE_STALAGTITE_FEATURE_CONFIGURED = new ConfiguredFeature<>(STONE_STALAGTITE_FEATURE,
            new StoneStalagtiteFeatureConfig(16)
    );

    public static void onInitialize() {
        Registry.register(BuiltInRegistries.FEATURE, STONE_STALAGTITE_ID, STONE_STALAGTITE_FEATURE);
        Registry.register(BuiltInRegistries.FEATURE, MUD_PATCH_ID, MUD_PATCH_FEATURE);
        PvWorldgen.initialize();
    }

    public enum VeinType {
        COPPER(Blocks.COPPER_ORE.defaultBlockState(), Blocks.RAW_COPPER_BLOCK.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), 32, 96, 0.6F, 0.25F),
        IRON(Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(), Blocks.RAW_IRON_BLOCK.defaultBlockState(), Blocks.TUFF.defaultBlockState(), -8, 36, 0.35F, 0.08F);

        public final BlockState ore;
        public final BlockState rawOreBlock;
        public final BlockState stone;
        public final int minY;
        public final int maxY;
        public final float oreChance;
        public final float rawBlockChance;

        private VeinType(final BlockState ore, final BlockState rawOreBlock, final BlockState stone, final int minY, final int maxY, final float oreChance, final float rawBlockChance) {
            this.ore = ore;
            this.rawOreBlock = rawOreBlock;
            this.stone = stone;
            this.minY = minY;
            this.maxY = maxY;
            this.oreChance = oreChance;
            this.rawBlockChance = rawBlockChance;
        }
    }

}
