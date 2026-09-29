package frootloops.versus.mod.environment.worldgen;

import frootloops.versus.VersusMod;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;

/**
 * The carvers of Improved worlds ({@link PvChunkGenerator}). Biomes name vanilla's {@code cave},
 * {@code cave_extra_underground} and {@code canyon}; Improved worlds carve those with this mod's copies
 * ({@code players-versus:cave} and so on), whose tuning used to replace vanilla's in every world type. Other world
 * types keep vanilla's carvers (Section 10, question 1 of the refactor plan).
 *
 * <p>The carvers keep their places in each biome's list: a carver's random seed comes from its index there, so the same
 * lists give the same caves as before.
 */
public final class PvCarvers {

    /** Vanilla's carvers that Improved worlds replace, and their Players Versus copies. */
    static final Map<Identifier, Identifier> COPIES = Map.of(
            Identifier.withDefaultNamespace("cave"), id("cave"),
            Identifier.withDefaultNamespace("cave_extra_underground"), id("cave_extra_underground"),
            Identifier.withDefaultNamespace("canyon"), id("canyon"));

    /**
     * Biomes whose whole list differs in Improved worlds. The deep dark carves with this mod's caves and its own canyon;
     * other world types get vanilla's list, which the deep dark's override keeps.
     */
    static final Map<Identifier, List<Identifier>> LISTS = Map.of(
            Identifier.withDefaultNamespace("deep_dark"), List.of(id("cave"), id("deep_dark_canyon")));

    private final HolderGetter<ConfiguredWorldCarver<?>> carvers;

    PvCarvers(HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        this.carvers = carvers;
    }

    /**
     * A biome's carvers in Improved worlds, by id: its own list with vanilla's three replaced, or its whole list from
     * {@link #LISTS}.
     */
    static List<Identifier> improved(Identifier biome, List<Identifier> carvers) {
        List<Identifier> list = LISTS.get(biome);
        if (list != null) return list;
        return carvers.stream().map(carver -> COPIES.getOrDefault(carver, carver)).toList();
    }

    /** {@code settings} with the biome's Improved carvers, or {@code settings} itself where they're the same. */
    BiomeGenerationSettings withImprovedCarvers(Holder<Biome> biome, BiomeGenerationSettings settings) {
        List<Holder<ConfiguredWorldCarver<?>>> carvers = new ArrayList<>();
        settings.getCarvers().forEach(carvers::add);
        Optional<Identifier> biomeId = biome.unwrapKey().map(ResourceKey::location);
        List<Identifier> ids = new ArrayList<>(carvers.size());
        for (Holder<ConfiguredWorldCarver<?>> carver : carvers) {
            Optional<ResourceKey<ConfiguredWorldCarver<?>>> key = carver.unwrapKey();
            if (key.isEmpty()) return settings;  // an inline carver: nothing to match, keep the biome's list as it is
            ids.add(key.get().location());
        }
        List<Identifier> improved = biomeId.map(id -> improved(id, ids)).orElse(ids);
        if (improved.equals(ids)) return settings;
        List<Holder<ConfiguredWorldCarver<?>>> entries = improved.stream()
                .<Holder<ConfiguredWorldCarver<?>>>map(id -> this.carvers.getOrThrow(ResourceKey.create(Registries.CONFIGURED_CARVER, id)))
                .toList();
        return new BiomeGenerationSettings(HolderSet.direct(entries), settings.features());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, path);
    }
}
