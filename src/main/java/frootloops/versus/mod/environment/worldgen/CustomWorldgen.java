package frootloops.versus.mod.environment.worldgen;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.features.MudPatchFeature;
import frootloops.versus.mod.environment.worldgen.features.MushroomRingFeature;
import frootloops.versus.mod.environment.worldgen.features.StoneStalagtiteFeature;
import frootloops.versus.mod.environment.worldgen.rocks.RockFeature;
import frootloops.versus.mod.environment.worldgen.rocks.RockGroundFilter;
import frootloops.versus.mod.environment.worldgen.rocks.RockFormationPiece;
import frootloops.versus.mod.environment.worldgen.rocks.RockFormationStructure;
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
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
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

    /** The wetlands' ground, islands and channels, from one smooth noise (MarshFeature). */
    public static final Identifier MARSH_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "marsh");
    public static final Identifier STONE_STALAGTITE_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "stone_stalagtite");
    public static final Identifier MUD_PATCH_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "mud_patch");
    public static final Identifier MUSHROOM_RING_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "mushroom_ring");
    /** Small rocks (RockFeature), and large ones on the world's grid (RockFormationStructure, one piece a body). */
    public static final Identifier ROCK_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "rock");
    public static final Identifier ROCK_FORMATION_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "rock_formation");
    /** Where rock may stand (RockGroundFilter): a placement filter for the rocks' placed features. */
    public static final Identifier ROCK_GROUND_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "rock_ground");
    public static final StructureType<RockFormationStructure> ROCK_FORMATION =
            Registry.register(BuiltInRegistries.STRUCTURE_TYPE, ROCK_FORMATION_ID, () -> RockFormationStructure.CODEC);
    public static final StructurePieceType ROCK_FORMATION_PIECE =
            Registry.register(BuiltInRegistries.STRUCTURE_PIECE, ROCK_FORMATION_ID, (StructurePieceType.ContextlessType) RockFormationPiece::new);

    /**
     * The overworld's own mushroom patches, which vanilla also lists in the nether's biomes, where they grow on its
     * bedrock roof. The nether's shrooms are crimson fungi (red_mushroom_nether's data: patches of crimson nylium).
     */
    private static final List<ResourceKey<PlacedFeature>> OVERWORLD_MUSHROOMS = List.of(
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace("brown_mushroom_normal")),
            ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace("red_mushroom_normal")));

    /** Rare-ish clusters of rocks, on rock ground (RockGroundFilter keeps the middle temperatures and level ground). */
    private static final ResourceKey<PlacedFeature> SPARSE_ROCKS = ResourceKey.create(Registries.PLACED_FEATURE,
            Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "rock/sparse"));
    /** A cluster of rocks now and then. */
    private static final ResourceKey<PlacedFeature> MEADOW_ROCKS = ResourceKey.create(Registries.PLACED_FEATURE,
            Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "rock/meadow_cluster"));
    /** The middle biomes, vanilla's and the mod's: plains, forests, savannas and jungles, and their transitions. */
    private static final List<ResourceKey<Biome>> SPARSE_ROCK_BIOMES = List.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST,
            Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.JUNGLE,
            Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE, CustomOverworldBiomes.DAPPLED_FOREST, CustomOverworldBiomes.SPARSE_DAPPLED_FOREST,
            CustomOverworldBiomes.DARK_BIRCH_FOREST, CustomOverworldBiomes.TUSCAN_GROVE, CustomOverworldBiomes.WARM_OAK_FOREST,
            CustomOverworldBiomes.DARK_JUNGLE_FOREST);

    public static void onInitialize() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, STONE_STALAGTITE_ID, StoneStalagtiteFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE, MUD_PATCH_ID, MudPatchFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE, MUSHROOM_RING_ID, MushroomRingFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE, MARSH_ID, frootloops.versus.mod.environment.worldgen.features.MarshFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE, ROCK_ID, RockFeature.CODEC);
        Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, ROCK_GROUND_ID, RockGroundFilter.CODEC);
        BiomeModifications.create(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "no_mushrooms_in_the_nether"))
                .add(ModificationPhase.REMOVALS, BiomeSelectors.foundInTheNether(), context -> {
                    for (ResourceKey<PlacedFeature> mushrooms : OVERWORLD_MUSHROOMS) {
                        context.getGenerationSettings().removeFeature(GenerationStep.Decoration.VEGETAL_DECORATION, mushrooms);
                    }
                });
        // Rare-ish clusters of rocks in the middle biomes, rarer in meadows: the step before trees, which they would cut
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(SPARSE_ROCK_BIOMES), GenerationStep.Decoration.LOCAL_MODIFICATIONS, SPARSE_ROCKS);
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(Biomes.MEADOW), GenerationStep.Decoration.LOCAL_MODIFICATIONS, MEADOW_ROCKS);
        PvWorldgen.initialize();
    }
}
