package frootloops.versus.mod.environment.worldgen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.aquifer.AquiferInputs;
import frootloops.versus.mod.environment.worldgen.density.PvFinalDensity;
import frootloops.versus.mod.environment.worldgen.density.PvHighRiver;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Vanilla's data pack plus this mod's data (read from {@code src/main/resources}), loaded into worldgen registries by
 * the game's own loader, so tests can build this mod's noise router the way a world does.
 *
 * <p>A third pack, {@code src/test/resources/reference}, holds the density-function JSON that Java kernels replaced,
 * under the namespace {@value #REFERENCE}: the tests compare the kernels with it.
 */
public final class WorldgenTestData {

    public static final Path MOD_RESOURCES = Path.of("src/main/resources");
    public static final Path REFERENCE_RESOURCES = Path.of("src/test/resources/reference");
    /** The namespace of the replaced JSON: {@code pv_reference:overworld/…} is what {@code players-versus:overworld/…} was. */
    public static final String REFERENCE = "pv_reference";
    private static final List<ResourceKey<? extends Registry<?>>> LOADED = List.of(Registries.NOISE, Registries.DENSITY_FUNCTION);

    private static RegistryAccess.Frozen registries;
    private static NoiseGeneratorSettings pvSettings;

    private WorldgenTestData() {
    }

    /** Noise parameters and density functions: vanilla's, with this mod's pack and the reference pack on top. */
    public static synchronized RegistryAccess.Frozen registries() {
        if (registries == null) {
            TestGame.start();
            PackResources vanilla = ServerPacksSource.createVanillaPackSource();
            PackResources mod = new PathPackResources(vanilla.location(), MOD_RESOURCES);
            PackResources reference = new PathPackResources(vanilla.location(), REFERENCE_RESOURCES);
            try (MultiPackResourceManager resources = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(vanilla, mod, reference))) {
                registries = RegistryDataLoader.load(resources, List.of(),
                        RegistryDataLoader.WORLDGEN_REGISTRIES.stream().filter(entry -> LOADED.contains(entry.key())).toList());
            }
        }
        return registries;
    }

    /**
     * This mod's noise settings, decoded from its JSON against {@link #registries()}. The surface rule is swapped for
     * plain stone: it names this mod's blocks, which only exist after the whole mod initialized, and these tests build
     * no surfaces.
     */
    public static synchronized NoiseGeneratorSettings pvSettings() {
        if (pvSettings == null) {
            JsonObject json = JsonParser.parseString(read("data/" + VersusMod.MOD_ID + "/worldgen/noise_settings/overworld.json")).getAsJsonObject();
            json.add("surface_rule", JsonParser.parseString("{\"type\": \"minecraft:block\", \"result_state\": {\"Name\": \"minecraft:stone\"}}"));
            pvSettings = NoiseGeneratorSettings.DIRECT_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries()), json).getOrThrow();
        }
        return pvSettings;
    }

    public static RandomState noiseConfig(long seed) {
        return RandomState.create(pvSettings(), registries().lookupOrThrow(Registries.NOISE), seed);
    }

    /**
     * A density function from the registry, seeded by the aquifer's own seeding ({@link AquiferInputs#seeding}), which
     * must match how {@link RandomState} seeds its router.
     */
    public static DensityFunction seeded(RandomState config, String id) {
        return registries().lookupOrThrow(Registries.DENSITY_FUNCTION)
                .getOrThrow(ResourceKey.create(Registries.DENSITY_FUNCTION, Identifier.parse(id))).value()
                .mapAll(AquiferInputs.seeding(config));
    }

    /** A density function written inline in JSON, its references resolved in {@link #registries()}, seeded like {@link #seeded}. */
    public static DensityFunction parse(RandomState config, String json) {
        return DensityFunction.HOLDER_HELPER_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries()), JsonParser.parseString(json))
                .getOrThrow().mapAll(AquiferInputs.seeding(config));
    }

    /** The high river's valley ({@link PvHighRiver}) from this mod's final density, seeded like {@link #seeded}. */
    public static PvHighRiver highRiver(RandomState config) {
        PvFinalDensity finalDensity = (PvFinalDensity) seeded(config, VersusMod.MOD_ID + ":overworld/final_density");
        return (PvHighRiver) PvWorldgen.unwrap(finalDensity.highRiver());
    }

    /**
     * A chunk the high river runs through: its water at the surface at the chunk's centre (a point of the river's
     * lattice, so exact values decide it), the first of random chunks near the origin.
     */
    public static ChunkPos highRiverChunk(RandomState config) {
        PvHighRiver river = highRiver(config);
        Random random = new Random(8675309L);
        for (int i = 0; i < 40000; i++) {
            int chunkX = random.nextInt(500) - 250, chunkZ = random.nextInt(500) - 250;
            DensityFunction.FunctionContext center = new DensityFunction.SinglePointContext(chunkX * 16 + 8, PvWorldgenConstants.HIGH_RIVER_Y, chunkZ * 16 + 8);
            double channel = river.channel().compute(center);
            if (Math.abs(channel) >= PvWorldgenConstants.HIGH_RIVER_HALF_WIDTH) continue;
            if (PvHighRiver.waterAt(PvWorldgenConstants.HIGH_RIVER_Y, channel, river.depth().compute(center), river.terrain().compute(center))) {
                return new ChunkPos(chunkX, chunkZ);
            }
        }
        throw new AssertionError("no chunk with the high river's water near the origin");
    }

    public static String read(String resource) {
        try {
            return Files.readString(MOD_RESOURCES.resolve(resource));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
