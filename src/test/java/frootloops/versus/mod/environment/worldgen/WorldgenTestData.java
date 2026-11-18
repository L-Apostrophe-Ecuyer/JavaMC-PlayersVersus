package frootloops.versus.mod.environment.worldgen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Vanilla's data pack plus this mod's data (read from {@code src/main/resources}), loaded into the noise and density
 * function registries by the game's own loader, so a test can build this mod's noise router the way a world does and
 * sample it with the game's own evaluation ({@link RandomState#sampleBlockValueUncached}).
 */
public final class WorldgenTestData {

    public static final Path MOD_RESOURCES = Path.of("src/main/resources");
    private static final List<ResourceKey<? extends Registry<?>>> LOADED = List.of(Registries.NOISE, Registries.DENSITY_FUNCTION);

    private static RegistryAccess.Frozen registries;
    private static NoiseRouter router;

    private WorldgenTestData() {
    }

    /** Noise parameters and density functions: vanilla's, with this mod's on top. */
    public static synchronized RegistryAccess.Frozen registries() {
        if (registries == null) {
            TestGame.start();
            PackResources vanilla = ServerPacksSource.createVanillaPackSource().fullResources();
            PackResources mod = new PathPackResources(vanilla.location(), MOD_RESOURCES);
            try (MultiPackResourceManager resources = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(vanilla, mod))) {
                Executor sameThread = Runnable::run;
                registries = RegistryDataLoader.load(resources, List.of(),
                        RegistryDataLoader.WORLD_REGISTRIES.stream().filter(entry -> LOADED.contains(entry.key())).toList(), sameThread).join();
            } catch (CompletionException failure) {
                throw new IllegalStateException("the noise and density function data failed to load", failure.getCause());
            }
        }
        return registries;
    }

    /** This mod's noise router, decoded from the {@code noise_router} of its overworld noise settings. */
    public static synchronized NoiseRouter router() {
        if (router == null) {
            JsonObject settings = JsonParser.parseString(read("data/players-versus/worldgen/noise_settings/overworld.json")).getAsJsonObject();
            router = NoiseRouter.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries()), settings.get("noise_router")).getOrThrow();
        }
        return router;
    }

    /** The world's random state for {@code seed}: its noises, seeded, and this mod's router. */
    public static RandomState randomState(long seed) {
        return RandomState.create(registries().lookupOrThrow(Registries.NOISE), seed, false, 63, router());
    }

    /** A density function of the registries, by its id: vanilla's or this mod's. */
    public static DensityFunction density(String id) {
        return registries().lookupOrThrow(Registries.DENSITY_FUNCTION).getOrThrow(ResourceKey.create(Registries.DENSITY_FUNCTION, Identifier.parse(id))).value();
    }

    public static String read(String resource) {
        try {
            return Files.readString(MOD_RESOURCES.resolve(resource));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
