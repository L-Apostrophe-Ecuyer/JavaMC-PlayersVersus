package frootloops.versus.gametest;

import frootloops.versus.mod.environment.worldgen.features.PvWorldFilter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.NoiseBasedCountPlacement;
import net.minecraft.world.level.levelgen.placement.NoiseThresholdCountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.List;

/** The mod's placed features, made fit to run over a test area. */
final class PlacedFeatures {

    private PlacedFeatures() {
    }

    /**
     * The placed feature of the mod's data (namespace players-versus) without its biome check (the test world's biome doesn't
     * list it) nor its world type's (the test world isn't a Players Versus one), and with {@code patches} in place of a count
     * taken from the noise of the place (the trees' noise or the biome's, which at the test area's chunk may well be zero): the
     * rest is the data's own.
     */
    static PlacedFeature withoutBiomeCheck(GameTestHelper helper, String name, int patches) {
        Registry<PlacedFeature> features = helper.getLevel().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        PlacedFeature feature = features.getValue(Identifier.parse("players-versus:" + name));
        helper.assertTrue(feature != null, "players-versus:" + name + " is not a placed feature");
        List<PlacementModifier> placement = feature.placement().stream().filter(modifier -> !(modifier instanceof BiomeFilter) && !(modifier instanceof PvWorldFilter))
                .map(modifier -> modifier instanceof NoiseBasedCountPlacement || modifier instanceof NoiseThresholdCountPlacement
                        ? (PlacementModifier) CountPlacement.of(patches) : modifier).toList();
        return new PlacedFeature(feature.feature(), placement);
    }
}
