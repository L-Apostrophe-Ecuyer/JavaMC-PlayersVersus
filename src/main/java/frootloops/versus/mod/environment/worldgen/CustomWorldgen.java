package frootloops.versus.mod.environment.worldgen;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.features.MudPatchFeature;
import frootloops.versus.mod.environment.worldgen.features.StoneStalagtiteFeature;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

/**
 * The mod's world generation registrations: its feature types and the Improved world type ({@link PvWorldgen}).
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

    public static void onInitialize() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, STONE_STALAGTITE_ID, StoneStalagtiteFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE, MUD_PATCH_ID, MudPatchFeature.CODEC);
        PvWorldgen.initialize();
    }
}
