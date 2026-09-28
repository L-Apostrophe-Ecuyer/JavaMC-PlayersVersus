package frootloops.versus.mod.environment.worldgen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.aquifer.AquiferInputs;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryLoader;
import net.minecraft.registry.RegistryOps;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.VanillaDataPackProvider;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Vanilla's data pack plus this mod's data (read from {@code src/main/resources}), loaded into worldgen registries by
 * the game's own loader, so tests can build this mod's noise router the way a world does.
 */
public final class WorldgenTestData {

    public static final Path MOD_RESOURCES = Path.of("src/main/resources");
    private static final List<RegistryKey<? extends Registry<?>>> LOADED = List.of(RegistryKeys.NOISE_PARAMETERS, RegistryKeys.DENSITY_FUNCTION);

    private static DynamicRegistryManager.Immutable registries;
    private static ChunkGeneratorSettings pvSettings;

    private WorldgenTestData() {
    }

    /** Noise parameters and density functions: vanilla's, with this mod's pack on top. */
    public static synchronized DynamicRegistryManager.Immutable registries() {
        if (registries == null) {
            TestGame.start();
            ResourcePack vanilla = VanillaDataPackProvider.createDefaultPack();
            ResourcePack mod = new DirectoryResourcePack(vanilla.getInfo(), MOD_RESOURCES);
            try (LifecycledResourceManagerImpl resources = new LifecycledResourceManagerImpl(ResourceType.SERVER_DATA, List.of(vanilla, mod))) {
                registries = RegistryLoader.loadFromResource(resources, List.of(),
                        RegistryLoader.DYNAMIC_REGISTRIES.stream().filter(entry -> LOADED.contains(entry.key())).toList());
            }
        }
        return registries;
    }

    /**
     * This mod's noise settings, decoded from its JSON against {@link #registries()}. The surface rule is swapped for
     * plain stone: it names this mod's blocks, which only exist after the whole mod initialized, and these tests build
     * no surfaces.
     */
    public static synchronized ChunkGeneratorSettings pvSettings() {
        if (pvSettings == null) {
            JsonObject json = JsonParser.parseString(read("data/" + VersusMod.MOD_ID + "/worldgen/noise_settings/overworld.json")).getAsJsonObject();
            json.add("surface_rule", JsonParser.parseString("{\"type\": \"minecraft:block\", \"result_state\": {\"Name\": \"minecraft:stone\"}}"));
            pvSettings = ChunkGeneratorSettings.CODEC.parse(RegistryOps.of(JsonOps.INSTANCE, registries()), json).getOrThrow();
        }
        return pvSettings;
    }

    public static NoiseConfig noiseConfig(long seed) {
        return NoiseConfig.create(pvSettings(), registries().getOrThrow(RegistryKeys.NOISE_PARAMETERS), seed);
    }

    /**
     * A density function from the registry, seeded by the aquifer's own seeding ({@link AquiferInputs#seeding}), which
     * must match how {@link NoiseConfig} seeds its router.
     */
    public static DensityFunction seeded(NoiseConfig config, String id) {
        return registries().getOrThrow(RegistryKeys.DENSITY_FUNCTION)
                .getOrThrow(RegistryKey.of(RegistryKeys.DENSITY_FUNCTION, Identifier.of(id))).value()
                .apply(AquiferInputs.seeding(config));
    }

    /** A density function written inline in JSON, its references resolved in {@link #registries()}, seeded like {@link #seeded}. */
    public static DensityFunction parse(NoiseConfig config, String json) {
        return DensityFunction.FUNCTION_CODEC.parse(RegistryOps.of(JsonOps.INSTANCE, registries()), JsonParser.parseString(json))
                .getOrThrow().apply(AquiferInputs.seeding(config));
    }

    public static String read(String resource) {
        try {
            return Files.readString(MOD_RESOURCES.resolve(resource));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
