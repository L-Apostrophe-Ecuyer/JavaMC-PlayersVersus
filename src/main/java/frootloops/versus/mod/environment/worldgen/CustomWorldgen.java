package frootloops.versus.mod.environment.worldgen;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.features.MudPatchFeature;
import frootloops.versus.mod.environment.worldgen.features.MushroomRingFeature;
import frootloops.versus.mod.environment.worldgen.features.PvWorldFilter;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.biome.Biome;
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
    /** The Players Versus world type only (PvWorldFilter): a placement filter for what the mod adds to vanilla's biomes. */
    public static final Identifier PV_WORLD_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "players_versus_world");
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

    /** The temperate biomes beside water: muddy banks, reeds, tall grass and ferns at the water's edge. */
    private static final TagKey<Biome> HAS_RIVERBANKS = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "has_riverbanks"));
    /** Those of them whose trees are deciduous: willows on the bank too. */
    private static final TagKey<Biome> HAS_RIVERBANK_WILLOWS = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "has_riverbank_willows"));
    private static final ResourceKey<PlacedFeature> RIVERBANK_MUD = placed("riverbank_mud"), RIVERBANK_REEDS = placed("riverbank_reeds"),
            RIVERBANK_PLANTS = placed("riverbank_plants"), RIVERBANK_WILLOWS = placed("riverbank_willows");

    private static ResourceKey<PlacedFeature> placed(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, path));
    }

    public static void onInitialize() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, STONE_STALAGTITE_ID, StoneStalagtiteFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE, MUD_PATCH_ID, MudPatchFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE, MUSHROOM_RING_ID, MushroomRingFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE, MARSH_ID, frootloops.versus.mod.environment.worldgen.features.MarshFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE, ROCK_ID, RockFeature.CODEC);
        Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, ROCK_GROUND_ID, RockGroundFilter.CODEC);
        Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, PV_WORLD_ID, PvWorldFilter.CODEC);
        BiomeModifications.create(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "no_mushrooms_in_the_nether"))
                .add(ModificationPhase.REMOVALS, BiomeSelectors.foundInTheNether(), context -> {
                    for (ResourceKey<PlacedFeature> mushrooms : OVERWORLD_MUSHROOMS) {
                        context.getGenerationSettings().removeFeature(GenerationStep.Decoration.VEGETAL_DECORATION, mushrooms);
                    }
                });
        // River banks (tools/refine-biomes.py), in the biomes of their tags: the banks made muddy before anything grows,
        // then reeds, plants and willows by the water. Their placements keep them to the Players Versus world type
        BiomeModifications.addFeature(BiomeSelectors.tag(HAS_RIVERBANKS), GenerationStep.Decoration.LOCAL_MODIFICATIONS, RIVERBANK_MUD);
        BiomeModifications.addFeature(BiomeSelectors.tag(HAS_RIVERBANKS), GenerationStep.Decoration.VEGETAL_DECORATION, RIVERBANK_REEDS);
        BiomeModifications.addFeature(BiomeSelectors.tag(HAS_RIVERBANKS), GenerationStep.Decoration.VEGETAL_DECORATION, RIVERBANK_PLANTS);
        BiomeModifications.addFeature(BiomeSelectors.tag(HAS_RIVERBANK_WILLOWS), GenerationStep.Decoration.VEGETAL_DECORATION, RIVERBANK_WILLOWS);
        PvWorldgen.initialize();
    }
}
