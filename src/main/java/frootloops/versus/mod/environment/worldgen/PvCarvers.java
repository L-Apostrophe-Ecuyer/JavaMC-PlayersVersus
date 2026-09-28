package frootloops.versus.mod.environment.worldgen;

import frootloops.versus.VersusMod;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.gen.carver.ConfiguredCarver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            Identifier.ofVanilla("cave"), id("cave"),
            Identifier.ofVanilla("cave_extra_underground"), id("cave_extra_underground"),
            Identifier.ofVanilla("canyon"), id("canyon"));

    /**
     * Biomes whose whole list differs in Improved worlds. The deep dark carves with this mod's caves and its own canyon;
     * other world types get vanilla's list, which the deep dark's override keeps.
     */
    static final Map<Identifier, List<Identifier>> LISTS = Map.of(
            Identifier.ofVanilla("deep_dark"), List.of(id("cave"), id("deep_dark_canyon")));

    private final RegistryEntryLookup<ConfiguredCarver<?>> carvers;

    PvCarvers(RegistryEntryLookup<ConfiguredCarver<?>> carvers) {
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
    GenerationSettings withImprovedCarvers(RegistryEntry<Biome> biome, GenerationSettings settings) {
        List<RegistryEntry<ConfiguredCarver<?>>> carvers = new ArrayList<>();
        settings.getCarversForStep().forEach(carvers::add);
        Optional<Identifier> biomeId = biome.getKey().map(RegistryKey::getValue);
        List<Identifier> ids = new ArrayList<>(carvers.size());
        for (RegistryEntry<ConfiguredCarver<?>> carver : carvers) {
            Optional<RegistryKey<ConfiguredCarver<?>>> key = carver.getKey();
            if (key.isEmpty()) return settings;  // an inline carver: nothing to match, keep the biome's list as it is
            ids.add(key.get().getValue());
        }
        List<Identifier> improved = biomeId.map(id -> improved(id, ids)).orElse(ids);
        if (improved.equals(ids)) return settings;
        List<RegistryEntry<ConfiguredCarver<?>>> entries = improved.stream()
                .<RegistryEntry<ConfiguredCarver<?>>>map(id -> this.carvers.getOrThrow(RegistryKey.of(RegistryKeys.CONFIGURED_CARVER, id)))
                .toList();
        return new GenerationSettings(RegistryEntryList.of(entries), settings.getFeatures());
    }

    private static Identifier id(String path) {
        return Identifier.of(VersusMod.MOD_ID, path);
    }
}
