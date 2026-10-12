package frootloops.versus.mod.environment.worldgen.biome;

import com.mojang.datafixers.util.Pair;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes.PlacedBiome;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes.PlacedBiomeType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.Parameter;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;

/**
 * The layout the deleted {@code VanillaBiomeParametersOverworldMixin} produced (at commit 37efc29), replayed over
 * vanilla's entries, so tests can measure what {@link PvBiomeLayout} changed. The logic is copied as-is, quirks
 * included; do not fix it here. Its frosted caves are vanilla's ice caves now (26.4), as in the new layout.
 */
final class OldBiomeLayout {

    private static final Parameter DEPTH_ZERO = Parameter.point(0.0F);
    private static final Parameter DEPTH_ONE = Parameter.point(0.1F);
    private static final Parameter DEPTH_SURFACE_CAVE = Parameter.span(0.1F, 0.25F), DEPTH_CAVE = Parameter.span(0.2F, 0.4F),
            DEPTH_GENERIC_CAVE = Parameter.span(0.25F, 0.65F), DEPTH_DEEP_CAVE = Parameter.point(0.9F);
    private static final float MIN_EROSION_FOR_MOUNTAIN_TRANSITION = -0.475f;
    private static final long MIN_EROSION_FOR_MOUNTAIN_TRANSITION_LONG = Climate.quantizeCoord(MIN_EROSION_FOR_MOUNTAIN_TRANSITION);
    private static final float MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION = 0.03f;
    private static final long MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION_LONG = Climate.quantizeCoord(MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION);
    private static final long MAX_WEIRDNESS_FOR_RIVERS_LONG = Climate.quantizeCoord(0.3F), MIN_WEIRDNESS_FOR_RIVERS_LONG = Climate.quantizeCoord(-0.3F);
    private static final float MIN_TEMPERATURE_FOR_FROZEN_TRANSITION = -0.55f, MAX_TEMPERATURE_FOR_FROZEN_TRANSITION = -0.375f;
    private static final long MIN_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG = Climate.quantizeCoord(MIN_TEMPERATURE_FOR_FROZEN_TRANSITION),
            MAX_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG = Climate.quantizeCoord(MAX_TEMPERATURE_FOR_FROZEN_TRANSITION);
    private static final float MIN_HUMIDITY_FOR_FOREST_TRANSITION = 0.275f, MAX_HUMIDITY_FOR_FOREST_TRANSITION = 0.35f;
    private static final long MIN_HUMIDITY_FOR_FOREST_TRANSITION_LONG = Climate.quantizeCoord(MIN_HUMIDITY_FOR_FOREST_TRANSITION),
            MAX_HUMIDITY_FOR_FOREST_TRANSITION_LONG = Climate.quantizeCoord(MAX_HUMIDITY_FOR_FOREST_TRANSITION);

    private OldBiomeLayout() {
    }

    static List<Pair<ParameterPoint, ResourceKey<Biome>>> build() {
        List<Pair<ParameterPoint, ResourceKey<Biome>>> surface = new ArrayList<>();
        List<Pair<ParameterPoint, ResourceKey<Biome>>> underground = new ArrayList<>();
        new OverworldBiomeBuilder().addBiomes(pair -> {
            ParameterPoint h = pair.getFirst();
            ResourceKey<Biome> biome = pair.getSecond();
            float offset = Climate.unquantizeCoord(h.offset());
            if (PvBiomeLayout.isPoint(h.depth(), 0.0F)) {
                writeBiomeParameters(surface::add, h.temperature(), h.humidity(), h.continentalness(), h.erosion(), h.weirdness(), offset, biome);
            } else if (PvBiomeLayout.isPoint(h.depth(), 1.0F)) {
                // the mixin cancelled writeBiomeParameters, so vanilla's depth-1 copy never existed
            } else if (biome == Biomes.LUSH_CAVES || biome == Biomes.DRIPSTONE_CAVES) {
                writeCaveBiomeParameters(underground::add, h.temperature(), h.humidity(), h.continentalness(), h.erosion(), h.weirdness(), offset, biome);
            } else {
                underground.add(pair);
            }
        });
        List<Pair<ParameterPoint, ResourceKey<Biome>>> out = new ArrayList<>(surface);
        for (PlacedBiome b : CustomOverworldBiomes.landBiomesToPlaceInOverorld) {
            if (b.type() != PlacedBiomeType.SURFACE) continue;
            out.add(Pair.of(Climate.parameters(b.temperature(), b.humidity(), b.continentalness(), b.erosion(), DEPTH_ZERO, b.weirdness(), b.isRare() ? 0.04F : -0.01F), b.biome()));
            out.add(Pair.of(Climate.parameters(b.temperature(), b.humidity(), b.continentalness(), b.erosion(), DEPTH_ONE, b.weirdness(), b.isRare() ? 0.04F : -0.01F), b.biome()));
        }
        out.addAll(underground);
        for (PlacedBiome b : CustomOverworldBiomes.caveBiomesToPlaceInOverorld) {
            if (b.type() == PlacedBiomeType.SURFACE) continue;
            Parameter depth = b.type() == PlacedBiomeType.SURFACE_CAVE ? DEPTH_SURFACE_CAVE : (b.type() == PlacedBiomeType.CAVE ? DEPTH_CAVE : (b.type() == PlacedBiomeType.GENERIC_CAVE ? DEPTH_GENERIC_CAVE : DEPTH_DEEP_CAVE));
            float rarityOffset = (b.type() == PlacedBiomeType.GENERIC_CAVE) ? 0.07F : (b.type() == PlacedBiomeType.GENERIC_DEEP_CAVE) ? 0.05F : b.isRare() ? 0.04F : 0.0F;
            out.add(Pair.of(Climate.parameters(b.temperature(), b.humidity(), b.continentalness(), b.erosion(), depth, b.weirdness(), rarityOffset), b.biome()));
        }
        return out;
    }

    private static void writeCaveBiomeParameters(Consumer<Pair<ParameterPoint, ResourceKey<Biome>>> parameters, Parameter temperature,
                                                 Parameter humidity, Parameter continentalness, Parameter erosion,
                                                 Parameter weirdness, float offset, ResourceKey<Biome> biome) {
        if (biome == Biomes.LUSH_CAVES) {
            parameters.accept(Pair.of(Climate.parameters(Parameter.span(-0.4F, 0.8F), humidity, continentalness, erosion, Parameter.span(0.15F, 0.5F), weirdness, offset + 0.01f), biome));
            parameters.accept(Pair.of(Climate.parameters(Parameter.span(-0.3F, 0.8F), Parameter.span(-0.6F, -0.4F), continentalness, erosion, Parameter.span(0.2F, 0.5F), weirdness, offset + 0.01f), biome));
            parameters.accept(Pair.of(Climate.parameters(Parameter.span(-1.0F, -0.8F), humidity, continentalness, erosion, Parameter.span(0.15F, 0.5F), weirdness, offset), Biomes.ICE_CAVES));
            parameters.accept(Pair.of(Climate.parameters(Parameter.span(-1.0F, -0.5F), Parameter.span(-0.6F, -0.4F), continentalness, Parameter.span(-1.0F, -0.6F), Parameter.span(0.2F, 0.5F), weirdness, offset), Biomes.ICE_CAVES));
        } else if (biome == Biomes.DRIPSTONE_CAVES) {
            parameters.accept(Pair.of(Climate.parameters(Parameter.span(-0.6F, 1.0F), humidity, continentalness, erosion, continentalness, weirdness, offset), biome));
            parameters.accept(Pair.of(Climate.parameters(Parameter.span(-1.0F, -0.7F), humidity, continentalness, erosion, continentalness, weirdness, offset), Biomes.ICE_CAVES));
        }
    }

    private static void writeBiomeParameters(Consumer<Pair<ParameterPoint, ResourceKey<Biome>>> parameters, Parameter temperature,
                                             Parameter humidity, Parameter continentalness, Parameter erosion,
                                             Parameter weirdness, float offset, ResourceKey<Biome> biome) {
        ResourceKey<Biome> transitionBiome;
        Parameter newErosion = null;
        Parameter newContinentalness = null;
        Parameter newTemperature = null;
        Parameter newHumidity = null;

        boolean isInRiverValley = weirdness.min() >= MIN_WEIRDNESS_FOR_RIVERS_LONG && weirdness.max() <= MAX_WEIRDNESS_FOR_RIVERS_LONG;

        if (!isInRiverValley && erosion.min() < MIN_EROSION_FOR_MOUNTAIN_TRANSITION_LONG && continentalness.max() > MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION_LONG) {
            transitionBiome = CustomOverworldBiomes.getMountainTransitionBiome(biome);
            if (transitionBiome == CustomOverworldBiomes.MOUNTAINSIDE_FOREST && temperature.min() > Climate.quantizeCoord(0.1998f)) transitionBiome = CustomOverworldBiomes.MOUNTAINSIDE_FOREST_WARM;
            if (transitionBiome != null) {
                float minErosion = Climate.unquantizeCoord(erosion.min());
                float maxErosion = Math.max(Climate.unquantizeCoord(erosion.max()), MIN_EROSION_FOR_MOUNTAIN_TRANSITION);
                float minContinentalness = Math.max(Climate.unquantizeCoord(continentalness.min()), MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION);
                float maxContinentalness = Climate.unquantizeCoord(continentalness.max());
                parameters.accept(Pair.of(Climate.parameters(temperature, humidity, Parameter.span(minContinentalness, maxContinentalness), Parameter.span(minErosion, maxErosion), DEPTH_ZERO, weirdness, offset), transitionBiome));
                parameters.accept(Pair.of(Climate.parameters(temperature, humidity, Parameter.span(minContinentalness, maxContinentalness), Parameter.span(minErosion, maxErosion), DEPTH_ONE, weirdness, offset), transitionBiome));
                if (maxErosion == MIN_EROSION_FOR_MOUNTAIN_TRANSITION && minContinentalness == MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION) {
                    return;
                } else {
                    if (MIN_EROSION_FOR_MOUNTAIN_TRANSITION < maxErosion) newErosion = Parameter.span(MIN_EROSION_FOR_MOUNTAIN_TRANSITION, maxErosion);
                    if (MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION > minContinentalness) newContinentalness = Parameter.span(minContinentalness, MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION);
                }
            }
        }

        if (temperature.min() < MIN_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG && temperature.max() > MIN_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG) {
            transitionBiome = CustomOverworldBiomes.getSnowyToTemperateTransitionBiome(biome);
            if (transitionBiome != null) {
                float minTemperature = Math.max(Climate.unquantizeCoord(temperature.min()), MIN_TEMPERATURE_FOR_FROZEN_TRANSITION);
                float maxTemperature = Climate.unquantizeCoord(temperature.max());
                parameters.accept(Pair.of(Climate.parameters(Parameter.span(minTemperature, maxTemperature), humidity, continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
                newTemperature = Parameter.span(Climate.unquantizeCoord(temperature.min()), MIN_TEMPERATURE_FOR_FROZEN_TRANSITION);
            }
        } else if (temperature.min() < MAX_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG && temperature.max() > MAX_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG) {
            transitionBiome = CustomOverworldBiomes.getSnowyToTemperateTransitionBiome(biome);
            if (transitionBiome != null) {
                float minTemperature = Climate.unquantizeCoord(temperature.min());
                float maxTemperature = Math.min(Climate.unquantizeCoord(temperature.max()), MAX_TEMPERATURE_FOR_FROZEN_TRANSITION);
                parameters.accept(Pair.of(Climate.parameters(Parameter.span(minTemperature, maxTemperature), humidity, continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
                newTemperature = Parameter.span(MAX_TEMPERATURE_FOR_FROZEN_TRANSITION, Climate.unquantizeCoord(temperature.max()));
            }
        }

        if ((biome == Biomes.GROVE || biome == Biomes.SNOWY_SLOPES) && temperature.max() > Climate.quantizeCoord(0.145f)) {
            transitionBiome = biome == Biomes.GROVE ? Biomes.TAIGA : Biomes.WINDSWEPT_HILLS;
            float maxTemp = Climate.unquantizeCoord(temperature.max());
            float minTemp = Math.max(0.145f, Climate.unquantizeCoord(temperature.min()));
            parameters.accept(Pair.of(Climate.parameters(Parameter.span(minTemp, maxTemp), humidity, continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
            if (minTemp > 0.145f) {
                return;
            } else {
                newTemperature = Parameter.span(Climate.unquantizeCoord(temperature.min()), minTemp);
            }
        } else if (biome == Biomes.STONY_PEAKS && temperature.min() < Climate.quantizeCoord(0.235f)) {
            transitionBiome = Biomes.WINDSWEPT_GRAVELLY_HILLS;
            float maxTemp = Math.min(0.235f, Climate.unquantizeCoord(temperature.max()));
            float minTemp = Climate.unquantizeCoord(temperature.min());
            parameters.accept(Pair.of(Climate.parameters(Parameter.span(minTemp, maxTemp), humidity, continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
            if (maxTemp < 0.235f) {
                return;
            } else {
                newTemperature = Parameter.span(maxTemp, Climate.unquantizeCoord(temperature.max()));
            }
        }

        if (humidity.min() < MIN_HUMIDITY_FOR_FOREST_TRANSITION_LONG && humidity.max() > MIN_HUMIDITY_FOR_FOREST_TRANSITION_LONG) {
            transitionBiome = CustomOverworldBiomes.getSnowyToTemperateTransitionBiome(biome);
            if (transitionBiome != null) {
                float minHumidity = Math.max(Climate.unquantizeCoord(humidity.min()), MIN_HUMIDITY_FOR_FOREST_TRANSITION);
                float maxHumidity = Climate.unquantizeCoord(humidity.max());
                parameters.accept(Pair.of(Climate.parameters(temperature, Parameter.span(minHumidity, maxHumidity), continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
                newHumidity = Parameter.span(Climate.unquantizeCoord(humidity.min()), MIN_HUMIDITY_FOR_FOREST_TRANSITION);
            }
        } else if (humidity.min() < MAX_HUMIDITY_FOR_FOREST_TRANSITION_LONG && humidity.max() > MAX_HUMIDITY_FOR_FOREST_TRANSITION_LONG) {
            transitionBiome = CustomOverworldBiomes.getHumidTransitionBiome(biome);
            if (transitionBiome != null) {
                float minHumidity = Climate.unquantizeCoord(humidity.min());
                float maxHumidity = Math.min(Climate.unquantizeCoord(humidity.max()), MAX_HUMIDITY_FOR_FOREST_TRANSITION);
                parameters.accept(Pair.of(Climate.parameters(temperature, Parameter.span(minHumidity, maxHumidity), continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
                newHumidity = Parameter.span(MAX_HUMIDITY_FOR_FOREST_TRANSITION, Climate.unquantizeCoord(humidity.max()));
            }
        }

        parameters.accept(Pair.of(Climate.parameters(newTemperature == null ? temperature : newTemperature, newHumidity == null ? humidity : newHumidity,
                newContinentalness == null ? continentalness : newContinentalness, newErosion == null ? erosion : newErosion, DEPTH_ZERO, weirdness, offset), biome));

        ResourceKey<Biome> caveBiome = CustomOverworldBiomes.getSurfaceCaveBiome(biome);
        if (caveBiome != null) {
            parameters.accept(Pair.of(Climate.parameters(newTemperature == null ? temperature : newTemperature, newHumidity == null ? humidity : newHumidity,
                    newContinentalness == null ? continentalness : newContinentalness, newErosion == null ? erosion : newErosion, DEPTH_SURFACE_CAVE, weirdness, offset + 0.075F), caveBiome));
        }
    }
}
