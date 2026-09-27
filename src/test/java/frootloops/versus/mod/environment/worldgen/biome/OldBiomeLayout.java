package frootloops.versus.mod.environment.worldgen.biome;

import com.mojang.datafixers.util.Pair;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes.PlacedBiome;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes.PlacedBiomeType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.NoiseHypercube;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.ParameterRange;
import net.minecraft.world.biome.source.util.VanillaBiomeParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The layout the deleted {@code VanillaBiomeParametersOverworldMixin} produced (at commit 37efc29), replayed over
 * vanilla's entries, so tests can measure what {@link PvBiomeLayout} changed. The logic is copied as-is, quirks
 * included; do not fix it here.
 */
final class OldBiomeLayout {

    private static final ParameterRange DEPTH_ZERO = ParameterRange.of(0.0F);
    private static final ParameterRange DEPTH_ONE = ParameterRange.of(0.1F);
    private static final ParameterRange DEPTH_SURFACE_CAVE = ParameterRange.of(0.1F, 0.25F), DEPTH_CAVE = ParameterRange.of(0.2F, 0.4F),
            DEPTH_GENERIC_CAVE = ParameterRange.of(0.25F, 0.65F), DEPTH_DEEP_CAVE = ParameterRange.of(0.9F);
    private static final float MIN_EROSION_FOR_MOUNTAIN_TRANSITION = -0.475f;
    private static final long MIN_EROSION_FOR_MOUNTAIN_TRANSITION_LONG = MultiNoiseUtil.toLong(MIN_EROSION_FOR_MOUNTAIN_TRANSITION);
    private static final float MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION = 0.03f;
    private static final long MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION_LONG = MultiNoiseUtil.toLong(MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION);
    private static final long MAX_WEIRDNESS_FOR_RIVERS_LONG = MultiNoiseUtil.toLong(0.3F), MIN_WEIRDNESS_FOR_RIVERS_LONG = MultiNoiseUtil.toLong(-0.3F);
    private static final float MIN_TEMPERATURE_FOR_FROZEN_TRANSITION = -0.55f, MAX_TEMPERATURE_FOR_FROZEN_TRANSITION = -0.375f;
    private static final long MIN_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG = MultiNoiseUtil.toLong(MIN_TEMPERATURE_FOR_FROZEN_TRANSITION),
            MAX_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG = MultiNoiseUtil.toLong(MAX_TEMPERATURE_FOR_FROZEN_TRANSITION);
    private static final float MIN_HUMIDITY_FOR_FOREST_TRANSITION = 0.275f, MAX_HUMIDITY_FOR_FOREST_TRANSITION = 0.35f;
    private static final long MIN_HUMIDITY_FOR_FOREST_TRANSITION_LONG = MultiNoiseUtil.toLong(MIN_HUMIDITY_FOR_FOREST_TRANSITION),
            MAX_HUMIDITY_FOR_FOREST_TRANSITION_LONG = MultiNoiseUtil.toLong(MAX_HUMIDITY_FOR_FOREST_TRANSITION);

    private OldBiomeLayout() {
    }

    static List<Pair<NoiseHypercube, RegistryKey<Biome>>> build() {
        List<Pair<NoiseHypercube, RegistryKey<Biome>>> surface = new ArrayList<>();
        List<Pair<NoiseHypercube, RegistryKey<Biome>>> underground = new ArrayList<>();
        new VanillaBiomeParameters().writeOverworldBiomeParameters(pair -> {
            NoiseHypercube h = pair.getFirst();
            RegistryKey<Biome> biome = pair.getSecond();
            float offset = MultiNoiseUtil.toFloat(h.offset());
            if (PvBiomeLayout.isPoint(h.depth(), 0.0F)) {
                writeBiomeParameters(surface::add, h.temperature(), h.humidity(), h.continentalness(), h.erosion(), h.weirdness(), offset, biome);
            } else if (PvBiomeLayout.isPoint(h.depth(), 1.0F)) {
                // the mixin cancelled writeBiomeParameters, so vanilla's depth-1 copy never existed
            } else if (biome == BiomeKeys.LUSH_CAVES || biome == BiomeKeys.DRIPSTONE_CAVES) {
                writeCaveBiomeParameters(underground::add, h.temperature(), h.humidity(), h.continentalness(), h.erosion(), h.weirdness(), offset, biome);
            } else {
                underground.add(pair);
            }
        });
        List<Pair<NoiseHypercube, RegistryKey<Biome>>> out = new ArrayList<>(surface);
        for (PlacedBiome b : CustomOverworldBiomes.landBiomesToPlaceInOverorld) {
            if (b.type() != PlacedBiomeType.SURFACE) continue;
            out.add(Pair.of(MultiNoiseUtil.createNoiseHypercube(b.temperature(), b.humidity(), b.continentalness(), b.erosion(), DEPTH_ZERO, b.weirdness(), b.isRare() ? 0.04F : -0.01F), b.biome()));
            out.add(Pair.of(MultiNoiseUtil.createNoiseHypercube(b.temperature(), b.humidity(), b.continentalness(), b.erosion(), DEPTH_ONE, b.weirdness(), b.isRare() ? 0.04F : -0.01F), b.biome()));
        }
        out.addAll(underground);
        for (PlacedBiome b : CustomOverworldBiomes.caveBiomesToPlaceInOverorld) {
            if (b.type() == PlacedBiomeType.SURFACE) continue;
            ParameterRange depth = b.type() == PlacedBiomeType.SURFACE_CAVE ? DEPTH_SURFACE_CAVE : (b.type() == PlacedBiomeType.CAVE ? DEPTH_CAVE : (b.type() == PlacedBiomeType.GENERIC_CAVE ? DEPTH_GENERIC_CAVE : DEPTH_DEEP_CAVE));
            float rarityOffset = (b.type() == PlacedBiomeType.GENERIC_CAVE) ? 0.07F : (b.type() == PlacedBiomeType.GENERIC_DEEP_CAVE) ? 0.05F : b.isRare() ? 0.04F : 0.0F;
            out.add(Pair.of(MultiNoiseUtil.createNoiseHypercube(b.temperature(), b.humidity(), b.continentalness(), b.erosion(), depth, b.weirdness(), rarityOffset), b.biome()));
        }
        return out;
    }

    private static void writeCaveBiomeParameters(Consumer<Pair<NoiseHypercube, RegistryKey<Biome>>> parameters, ParameterRange temperature,
                                                 ParameterRange humidity, ParameterRange continentalness, ParameterRange erosion,
                                                 ParameterRange weirdness, float offset, RegistryKey<Biome> biome) {
        if (biome == BiomeKeys.LUSH_CAVES) {
            parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(ParameterRange.of(-0.4F, 0.8F), humidity, continentalness, erosion, ParameterRange.of(0.15F, 0.5F), weirdness, offset + 0.01f), biome));
            parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(ParameterRange.of(-0.3F, 0.8F), ParameterRange.of(-0.6F, -0.4F), continentalness, erosion, ParameterRange.of(0.2F, 0.5F), weirdness, offset + 0.01f), biome));
            parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(ParameterRange.of(-1.0F, -0.8F), humidity, continentalness, erosion, ParameterRange.of(0.15F, 0.5F), weirdness, offset), CustomOverworldBiomes.FROSTED_CAVE));
            parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(ParameterRange.of(-1.0F, -0.5F), ParameterRange.of(-0.6F, -0.4F), continentalness, ParameterRange.of(-1.0F, -0.6F), ParameterRange.of(0.2F, 0.5F), weirdness, offset), CustomOverworldBiomes.FROSTED_CAVE));
        } else if (biome == BiomeKeys.DRIPSTONE_CAVES) {
            parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(ParameterRange.of(-0.6F, 1.0F), humidity, continentalness, erosion, continentalness, weirdness, offset), biome));
            parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(ParameterRange.of(-1.0F, -0.7F), humidity, continentalness, erosion, continentalness, weirdness, offset), CustomOverworldBiomes.FROSTED_CAVE));
        }
    }

    private static void writeBiomeParameters(Consumer<Pair<NoiseHypercube, RegistryKey<Biome>>> parameters, ParameterRange temperature,
                                             ParameterRange humidity, ParameterRange continentalness, ParameterRange erosion,
                                             ParameterRange weirdness, float offset, RegistryKey<Biome> biome) {
        RegistryKey<Biome> transitionBiome;
        ParameterRange newErosion = null;
        ParameterRange newContinentalness = null;
        ParameterRange newTemperature = null;
        ParameterRange newHumidity = null;

        boolean isInRiverValley = weirdness.min() >= MIN_WEIRDNESS_FOR_RIVERS_LONG && weirdness.max() <= MAX_WEIRDNESS_FOR_RIVERS_LONG;

        if (!isInRiverValley && erosion.min() < MIN_EROSION_FOR_MOUNTAIN_TRANSITION_LONG && continentalness.max() > MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION_LONG) {
            transitionBiome = CustomOverworldBiomes.getMountainTransitionBiome(biome);
            if (transitionBiome == CustomOverworldBiomes.MOUNTAINSIDE_FOREST && temperature.min() > MultiNoiseUtil.toLong(0.1998f)) transitionBiome = CustomOverworldBiomes.MOUNTAINSIDE_FOREST_WARM;
            if (transitionBiome != null) {
                float minErosion = MultiNoiseUtil.toFloat(erosion.min());
                float maxErosion = Math.max(MultiNoiseUtil.toFloat(erosion.max()), MIN_EROSION_FOR_MOUNTAIN_TRANSITION);
                float minContinentalness = Math.max(MultiNoiseUtil.toFloat(continentalness.min()), MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION);
                float maxContinentalness = MultiNoiseUtil.toFloat(continentalness.max());
                parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(temperature, humidity, ParameterRange.of(minContinentalness, maxContinentalness), ParameterRange.of(minErosion, maxErosion), DEPTH_ZERO, weirdness, offset), transitionBiome));
                parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(temperature, humidity, ParameterRange.of(minContinentalness, maxContinentalness), ParameterRange.of(minErosion, maxErosion), DEPTH_ONE, weirdness, offset), transitionBiome));
                if (maxErosion == MIN_EROSION_FOR_MOUNTAIN_TRANSITION && minContinentalness == MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION) {
                    return;
                } else {
                    if (MIN_EROSION_FOR_MOUNTAIN_TRANSITION < maxErosion) newErosion = ParameterRange.of(MIN_EROSION_FOR_MOUNTAIN_TRANSITION, maxErosion);
                    if (MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION > minContinentalness) newContinentalness = ParameterRange.of(minContinentalness, MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION);
                }
            }
        }

        if (temperature.min() < MIN_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG && temperature.max() > MIN_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG) {
            transitionBiome = CustomOverworldBiomes.getSnowyToTemperateTransitionBiome(biome);
            if (transitionBiome != null) {
                float minTemperature = Math.max(MultiNoiseUtil.toFloat(temperature.min()), MIN_TEMPERATURE_FOR_FROZEN_TRANSITION);
                float maxTemperature = MultiNoiseUtil.toFloat(temperature.max());
                parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(ParameterRange.of(minTemperature, maxTemperature), humidity, continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
                newTemperature = ParameterRange.of(MultiNoiseUtil.toFloat(temperature.min()), MIN_TEMPERATURE_FOR_FROZEN_TRANSITION);
            }
        } else if (temperature.min() < MAX_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG && temperature.max() > MAX_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG) {
            transitionBiome = CustomOverworldBiomes.getSnowyToTemperateTransitionBiome(biome);
            if (transitionBiome != null) {
                float minTemperature = MultiNoiseUtil.toFloat(temperature.min());
                float maxTemperature = Math.min(MultiNoiseUtil.toFloat(temperature.max()), MAX_TEMPERATURE_FOR_FROZEN_TRANSITION);
                parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(ParameterRange.of(minTemperature, maxTemperature), humidity, continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
                newTemperature = ParameterRange.of(MAX_TEMPERATURE_FOR_FROZEN_TRANSITION, MultiNoiseUtil.toFloat(temperature.max()));
            }
        }

        if ((biome == BiomeKeys.GROVE || biome == BiomeKeys.SNOWY_SLOPES) && temperature.max() > MultiNoiseUtil.toLong(0.145f)) {
            transitionBiome = biome == BiomeKeys.GROVE ? BiomeKeys.TAIGA : BiomeKeys.WINDSWEPT_HILLS;
            float maxTemp = MultiNoiseUtil.toFloat(temperature.max());
            float minTemp = Math.max(0.145f, MultiNoiseUtil.toFloat(temperature.min()));
            parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(ParameterRange.of(minTemp, maxTemp), humidity, continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
            if (minTemp > 0.145f) {
                return;
            } else {
                newTemperature = ParameterRange.of(MultiNoiseUtil.toFloat(temperature.min()), minTemp);
            }
        } else if (biome == BiomeKeys.STONY_PEAKS && temperature.min() < MultiNoiseUtil.toLong(0.235f)) {
            transitionBiome = BiomeKeys.WINDSWEPT_GRAVELLY_HILLS;
            float maxTemp = Math.min(0.235f, MultiNoiseUtil.toFloat(temperature.max()));
            float minTemp = MultiNoiseUtil.toFloat(temperature.min());
            parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(ParameterRange.of(minTemp, maxTemp), humidity, continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
            if (maxTemp < 0.235f) {
                return;
            } else {
                newTemperature = ParameterRange.of(maxTemp, MultiNoiseUtil.toFloat(temperature.max()));
            }
        }

        if (humidity.min() < MIN_HUMIDITY_FOR_FOREST_TRANSITION_LONG && humidity.max() > MIN_HUMIDITY_FOR_FOREST_TRANSITION_LONG) {
            transitionBiome = CustomOverworldBiomes.getSnowyToTemperateTransitionBiome(biome);
            if (transitionBiome != null) {
                float minHumidity = Math.max(MultiNoiseUtil.toFloat(humidity.min()), MIN_HUMIDITY_FOR_FOREST_TRANSITION);
                float maxHumidity = MultiNoiseUtil.toFloat(humidity.max());
                parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(temperature, ParameterRange.of(minHumidity, maxHumidity), continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
                newHumidity = ParameterRange.of(MultiNoiseUtil.toFloat(humidity.min()), MIN_HUMIDITY_FOR_FOREST_TRANSITION);
            }
        } else if (humidity.min() < MAX_HUMIDITY_FOR_FOREST_TRANSITION_LONG && humidity.max() > MAX_HUMIDITY_FOR_FOREST_TRANSITION_LONG) {
            transitionBiome = CustomOverworldBiomes.getHumidTransitionBiome(biome);
            if (transitionBiome != null) {
                float minHumidity = MultiNoiseUtil.toFloat(humidity.min());
                float maxHumidity = Math.min(MultiNoiseUtil.toFloat(humidity.max()), MAX_HUMIDITY_FOR_FOREST_TRANSITION);
                parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(temperature, ParameterRange.of(minHumidity, maxHumidity), continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
                newHumidity = ParameterRange.of(MAX_HUMIDITY_FOR_FOREST_TRANSITION, MultiNoiseUtil.toFloat(humidity.max()));
            }
        }

        parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(newTemperature == null ? temperature : newTemperature, newHumidity == null ? humidity : newHumidity,
                newContinentalness == null ? continentalness : newContinentalness, newErosion == null ? erosion : newErosion, DEPTH_ZERO, weirdness, offset), biome));

        RegistryKey<Biome> caveBiome = CustomOverworldBiomes.getSurfaceCaveBiome(biome);
        if (caveBiome != null) {
            parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(newTemperature == null ? temperature : newTemperature, newHumidity == null ? humidity : newHumidity,
                    newContinentalness == null ? continentalness : newContinentalness, newErosion == null ? erosion : newErosion, DEPTH_SURFACE_CAVE, weirdness, offset + 0.075F), caveBiome));
        }
    }
}
