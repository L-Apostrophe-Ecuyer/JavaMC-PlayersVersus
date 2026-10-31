package frootloops.versus.mod.environment.worldgen;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.features.MudPatchFeature;
import frootloops.versus.mod.environment.worldgen.features.MushroomRingFeature;
import frootloops.versus.mod.environment.worldgen.features.StoneStalagtiteFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.List;

/**
 * The mod's world generation registrations: its feature types, the overworld's mushrooms out of the nether, and the
 * Improved world type ({@link PvWorldgen}).
 *
 * <p>Since 26.3 a feature is its type and settings in one record, placed from the data ({@code worldgen/feature}), and
 * the Improved world's ore veins are material rules in its data ({@code worldgen/material_rule/overworld.json}).
 */
public class CustomWorldgen {

    public static final Identifier BETTER_WORLDGEN_PRESET_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "better_world");
    public static final ResourceKey<WorldPreset> BETTER_WORLDGEN_PRESET = ResourceKey.create(Registries.WORLD_PRESET, BETTER_WORLDGEN_PRESET_ID);
    public static final ResourceKey<NoiseGeneratorSettings> OVERWORLD = ResourceKey.create(Registries.NOISE_SETTINGS, BETTER_WORLDGEN_PRESET_ID);

    public static final Identifier STONE_STALAGTITE_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "stone_stalagtite");
    public static final Identifier MUD_PATCH_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "mud_patch");
    public static final Identifier MUSHROOM_RING_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "mushroom_ring");

    /**
     * The overworld's own mushroom patches, which vanilla also lists in the nether's biomes, where they grow on its
     * bedrock roof. The nether's shrooms are crimson fungi (red_mushroom_nether's data: patches of crimson nylium).
     */
    private static final List<ResourceKey<PlacedFeature>> OVERWORLD_MUSHROOMS = List.of(
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace("brown_mushroom_normal")),
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace("red_mushroom_normal")));

    public static void onInitialize() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, STONE_STALAGTITE_ID, StoneStalagtiteFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE, MUD_PATCH_ID, MudPatchFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE, MUSHROOM_RING_ID, MushroomRingFeature.CODEC);
        BiomeModifications.create(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "no_mushrooms_in_the_nether"))
                .add(ModificationPhase.REMOVALS, BiomeSelectors.foundInTheNether(), context -> {
                    for (ResourceKey<PlacedFeature> mushrooms : OVERWORLD_MUSHROOMS) {
                        context.getGenerationSettings().removeFeature(GenerationStep.Decoration.VEGETAL_DECORATION, mushrooms);
                    }
                });
        PvWorldgen.initialize();
    }
}
