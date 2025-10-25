package frootloops.versus.mixin.environment.worldgen;

import com.mojang.datafixers.util.Pair;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes.PlacedBiome;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes.PlacedBiomeType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.biome.source.util.VanillaBiomeParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.function.Consumer;

@Mixin(VanillaBiomeParameters.class)
public class VanillaBiomeParametersOverworldMixin {

    private static final MultiNoiseUtil.ParameterRange DEPTH_ZERO = MultiNoiseUtil.ParameterRange.of(0.0F);
    private static final MultiNoiseUtil.ParameterRange DEPTH_ONE = MultiNoiseUtil.ParameterRange.of(0.1F);
    private static final MultiNoiseUtil.ParameterRange DEPTH_SURFACE_CAVE = MultiNoiseUtil.ParameterRange.of(0.1F, 0.25F), DEPTH_CAVE = MultiNoiseUtil.ParameterRange.of(0.2F, 0.4F), DEPTH_GENERIC_CAVE = MultiNoiseUtil.ParameterRange.of(0.25F, 0.65F), DEPTH_DEEP_CAVE = MultiNoiseUtil.ParameterRange.of(0.9F);

    private static final float MIN_EROSION_FOR_MOUNTAIN_TRANSITION = -0.475f;
    private static final long MIN_EROSION_FOR_MOUNTAIN_TRANSITION_LONG = MultiNoiseUtil.toLong(MIN_EROSION_FOR_MOUNTAIN_TRANSITION);

    private static final float MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION = 0.03f;
    private static final long MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION_LONG = MultiNoiseUtil.toLong(MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION);

    private static final long MAX_WEIRDNESS_FOR_RIVERS_LONG = MultiNoiseUtil.toLong(0.3F), MIN_WEIRDNESS_FOR_RIVERS_LONG = MultiNoiseUtil.toLong(-0.3F);

    private static final float MIN_TEMPERATURE_FOR_FROZEN_TRANSITION = -0.55f, MAX_TEMPERATURE_FOR_FROZEN_TRANSITION = -0.375f;
    private static final long MIN_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG = MultiNoiseUtil.toLong(MIN_TEMPERATURE_FOR_FROZEN_TRANSITION), MAX_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG = MultiNoiseUtil.toLong(MAX_TEMPERATURE_FOR_FROZEN_TRANSITION);

    private static final float MIN_HUMIDITY_FOR_FOREST_TRANSITION = 0.275f, MAX_HUMIDITY_FOR_FOREST_TRANSITION = 0.35f;
    private static final long MIN_HUMIDITY_FOR_FOREST_TRANSITION_LONG = MultiNoiseUtil.toLong(MIN_HUMIDITY_FOR_FOREST_TRANSITION), MAX_HUMIDITY_FOR_FOREST_TRANSITION_LONG = MultiNoiseUtil.toLong(MAX_HUMIDITY_FOR_FOREST_TRANSITION);



    @Inject(method="writeOverworldBiomeParameters", at = @At("TAIL"), cancellable = false)
    public void clearMemory(Consumer<Pair<MultiNoiseUtil.NoiseHypercube, RegistryKey<Biome>>> parameters, CallbackInfo info) {
        CustomOverworldBiomes.freeUpMemory();
    }

    @Inject(method="writeLandBiomes", at = @At("TAIL"), cancellable = false)
    public void addNewLandBiomes(Consumer<Pair<MultiNoiseUtil.NoiseHypercube, RegistryKey<Biome>>> parameters, CallbackInfo info) {
        for (PlacedBiome b : CustomOverworldBiomes.landBiomesToPlaceInOverorld){
            if(b.type() != PlacedBiomeType.SURFACE) continue;
            parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(b.temperature(), b.humidity(), b.continentalness(), b.erosion(), DEPTH_ZERO, b.weirdness(),b.isRare() ? 0.04F : -0.01F), b.biome()));
            parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(b.temperature(), b.humidity(), b.continentalness(), b.erosion(), DEPTH_ONE, b.weirdness(), b.isRare() ? 0.04F : -0.01F), b.biome()));
        }
    }

    @Inject(method="writeCaveBiomes", at = @At("TAIL"), cancellable = false)
    public void addNewCaveBiomes(Consumer<Pair<MultiNoiseUtil.NoiseHypercube, RegistryKey<Biome>>> parameters, CallbackInfo info) {
        for (PlacedBiome b : CustomOverworldBiomes.caveBiomesToPlaceInOverorld){
            if(b.type() == PlacedBiomeType.SURFACE) continue;

            MultiNoiseUtil.ParameterRange depth = b.type() == PlacedBiomeType.SURFACE_CAVE ? DEPTH_SURFACE_CAVE : (b.type() == PlacedBiomeType.CAVE ? DEPTH_CAVE : (b.type() == PlacedBiomeType.GENERIC_CAVE ? DEPTH_GENERIC_CAVE : DEPTH_DEEP_CAVE));
            float rarityOffset = (b.type() == PlacedBiomeType.GENERIC_CAVE) ? 0.07F : (b.type() == PlacedBiomeType.GENERIC_DEEP_CAVE) ? 0.05F : b.isRare() ? 0.04F : 0.0F;
            parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(b.temperature(), b.humidity(), b.continentalness(), b.erosion(), depth, b.weirdness(),rarityOffset), b.biome()));
        }
    }


    @Inject(method="writeCaveBiomeParameters", at = @At("HEAD"), cancellable = true)
    private void tweakCaveBiomePlacement(
            Consumer<Pair<MultiNoiseUtil.NoiseHypercube, RegistryKey<Biome>>> parameters,
            MultiNoiseUtil.ParameterRange temperature,
            MultiNoiseUtil.ParameterRange humidity,
            MultiNoiseUtil.ParameterRange continentalness,
            MultiNoiseUtil.ParameterRange erosion,
            MultiNoiseUtil.ParameterRange weirdness,
            float offset,
            RegistryKey<Biome> biome,
            CallbackInfo info
    ) {
        if(biome == BiomeKeys.LUSH_CAVES) {
            parameters.accept(Pair.of( MultiNoiseUtil.createNoiseHypercube(MultiNoiseUtil.ParameterRange.of(-0.4F, 0.8F), humidity, continentalness, erosion, MultiNoiseUtil.ParameterRange.of(0.15F, 0.5F), weirdness, offset + 0.01f), biome));
            parameters.accept(Pair.of( MultiNoiseUtil.createNoiseHypercube(MultiNoiseUtil.ParameterRange.of(-0.3F, 0.8F), MultiNoiseUtil.ParameterRange.of(-0.6F, -0.4F), continentalness, erosion, MultiNoiseUtil.ParameterRange.of(0.2F, 0.5F), weirdness, offset + 0.01f), biome));
            parameters.accept(Pair.of( MultiNoiseUtil.createNoiseHypercube(MultiNoiseUtil.ParameterRange.of(-1.0F, -0.8F), humidity, continentalness, erosion, MultiNoiseUtil.ParameterRange.of(0.15F, 0.5F), weirdness, offset), CustomOverworldBiomes.FROSTED_CAVE));
            parameters.accept(Pair.of( MultiNoiseUtil.createNoiseHypercube(MultiNoiseUtil.ParameterRange.of(-1.0F, -0.5F), MultiNoiseUtil.ParameterRange.of(-0.6F, -0.4F), continentalness, MultiNoiseUtil.ParameterRange.of(-1.0F, -0.6F), MultiNoiseUtil.ParameterRange.of(0.2F, 0.5F), weirdness, offset), CustomOverworldBiomes.FROSTED_CAVE));
            info.cancel();
        }
        else if(biome == BiomeKeys.DRIPSTONE_CAVES) {
            parameters.accept(Pair.of( MultiNoiseUtil.createNoiseHypercube(MultiNoiseUtil.ParameterRange.of(-0.6F, 1.0F), humidity, continentalness, erosion, continentalness, weirdness, offset), biome));
            parameters.accept(Pair.of( MultiNoiseUtil.createNoiseHypercube(MultiNoiseUtil.ParameterRange.of(-1.0F, -0.7F), humidity, continentalness, erosion, continentalness, weirdness, offset), CustomOverworldBiomes.FROSTED_CAVE));
            info.cancel();
        }
    }

    @Inject(method="writeBiomeParameters", at = @At("HEAD"), cancellable = true)
    private void tweakOverworldBiomePlacement(
            Consumer<Pair<MultiNoiseUtil.NoiseHypercube, RegistryKey<Biome>>> parameters,
            MultiNoiseUtil.ParameterRange temperature,
            MultiNoiseUtil.ParameterRange humidity,
            MultiNoiseUtil.ParameterRange continentalness,
            MultiNoiseUtil.ParameterRange erosion,
            MultiNoiseUtil.ParameterRange weirdness,
            float offset,
            RegistryKey<Biome> biome,
            CallbackInfo info
    ) {
        RegistryKey<Biome> transitionBiome;
        boolean hasTweakedBiomePlacement = false;
        MultiNoiseUtil.ParameterRange newErosion = null;
        MultiNoiseUtil.ParameterRange newContinentalness = null;
        MultiNoiseUtil.ParameterRange newTemperature = null;
        MultiNoiseUtil.ParameterRange newHumidity = null;

        // Check if biome is near river or valley:
        boolean isInRiverValley = weirdness.min() >= MIN_WEIRDNESS_FOR_RIVERS_LONG && weirdness.max() <= MAX_WEIRDNESS_FOR_RIVERS_LONG;

        // MOUNTAINS:
        // Add smoother transitions for regular biomes near mountain slopes:
        if(!isInRiverValley && erosion.min() < MIN_EROSION_FOR_MOUNTAIN_TRANSITION_LONG && continentalness.max() > MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION_LONG) {
            transitionBiome = CustomOverworldBiomes.getMountainTransitionBiome(biome);
            if(transitionBiome == CustomOverworldBiomes.MOUNTAINSIDE_FOREST && temperature.min() > MultiNoiseUtil.toLong(0.1998f)) transitionBiome = CustomOverworldBiomes.MOUNTAINSIDE_FOREST_WARM;
            if(transitionBiome != null) {
                float minErosion =  MultiNoiseUtil.toFloat(erosion.min());
                float maxErosion =  Math.max(MultiNoiseUtil.toFloat(erosion.max()), MIN_EROSION_FOR_MOUNTAIN_TRANSITION);
                float minContinentalness =  Math.max(MultiNoiseUtil.toFloat(continentalness.min()), MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION);
                float maxContinentalness =  MultiNoiseUtil.toFloat(continentalness.max());
                parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(temperature, humidity, rangeOf(minContinentalness, maxContinentalness), rangeOf(minErosion, maxErosion), DEPTH_ZERO, weirdness, offset), transitionBiome));
                parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(temperature, humidity, rangeOf(minContinentalness, maxContinentalness), rangeOf(minErosion, maxErosion), DEPTH_ONE, weirdness, offset), transitionBiome));
                if(maxErosion == MIN_EROSION_FOR_MOUNTAIN_TRANSITION && minContinentalness == MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION) { // If the entire biome slice was in the mountain range, replace it entirely
                    info.cancel();
                    return;
                }
                else {
                    if(MIN_EROSION_FOR_MOUNTAIN_TRANSITION < maxErosion) newErosion = rangeOf(MIN_EROSION_FOR_MOUNTAIN_TRANSITION, maxErosion); // Otherwise, only replace what's above "EROSION FOR MOUNTAIN TRANSITION"
                    if(MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION > minContinentalness) newContinentalness = rangeOf(minContinentalness, MIN_CONTINENTALNESS_FOR_MOUNTAIN_TRANSITION); // Otherwise, only replace what's above "EROSION FOR MOUNTAIN TRANSITION"
                    hasTweakedBiomePlacement = true;
                }
            }
        }

        // FROZEN BIOMES:
        // Add smoother transitions between completely snowy and partially snowy:
        // Add smoother transitions for regular biomes near mountain slopes:
        if(temperature.min() < MIN_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG && temperature.max() > MIN_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG) {
            transitionBiome = CustomOverworldBiomes.getSnowyToTemperateTransitionBiome(biome);
            if(transitionBiome != null) {
                float minTemperature =  Math.max(MultiNoiseUtil.toFloat(temperature.min()), MIN_TEMPERATURE_FOR_FROZEN_TRANSITION);
                float maxTemperature =  MultiNoiseUtil.toFloat(temperature.max());
                parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(rangeOf(minTemperature, maxTemperature), humidity, continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
                newTemperature = rangeOf(MultiNoiseUtil.toFloat(temperature.min()), MIN_TEMPERATURE_FOR_FROZEN_TRANSITION);
                hasTweakedBiomePlacement = true;
            }
        }
        else if(temperature.min() < MAX_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG && temperature.max() > MAX_TEMPERATURE_FOR_FROZEN_TRANSITION_LONG) {
            transitionBiome = CustomOverworldBiomes.getSnowyToTemperateTransitionBiome(biome);
            if(transitionBiome != null) {
                float minTemperature =  MultiNoiseUtil.toFloat(temperature.min());
                float maxTemperature =  Math.min(MultiNoiseUtil.toFloat(temperature.max()), MAX_TEMPERATURE_FOR_FROZEN_TRANSITION);
                parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(rangeOf(minTemperature, maxTemperature), humidity, continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
                newTemperature = rangeOf(MAX_TEMPERATURE_FOR_FROZEN_TRANSITION, MultiNoiseUtil.toFloat(temperature.max()));
                hasTweakedBiomePlacement = true;
            }
        }

        // OTHER TEMPERATURE CHANGES: MOUNTAINS
        // Add smoother transitions for temperature changes
        if((biome == BiomeKeys.GROVE || biome == BiomeKeys.SNOWY_SLOPES) && temperature.max() > MultiNoiseUtil.toLong(0.145f)) {
            transitionBiome = biome == BiomeKeys.GROVE ? BiomeKeys.TAIGA : BiomeKeys.WINDSWEPT_HILLS;
            float maxTemp = MultiNoiseUtil.toFloat(temperature.max());
            float minTemp = Math.max(0.145f, MultiNoiseUtil.toFloat(temperature.min()));
            parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(rangeOf(minTemp, maxTemp), humidity, continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
            if (minTemp > 0.145f) {
                info.cancel();
                return;
            } else {
                newTemperature = rangeOf(MultiNoiseUtil.toFloat(temperature.min()), minTemp);
                hasTweakedBiomePlacement = true;
            }
        }
        else if(biome == BiomeKeys.STONY_PEAKS && temperature.min() < MultiNoiseUtil.toLong(0.235f)) {
            transitionBiome = BiomeKeys.WINDSWEPT_GRAVELLY_HILLS;
            float maxTemp = Math.min(0.235f, MultiNoiseUtil.toFloat(temperature.max()));
            float minTemp = MultiNoiseUtil.toFloat(temperature.min());
            parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(rangeOf(minTemp, maxTemp), humidity, continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
            if (maxTemp < 0.235f) {
                info.cancel();
                return;
            } else {
                newTemperature = rangeOf(maxTemp, MultiNoiseUtil.toFloat(temperature.max()));
                hasTweakedBiomePlacement = true;
            }
        }

        // HUMID BIOMES:
        // Add smoother transitions for humidity changes (dark forests, jungles)
        if(humidity.min() < MIN_HUMIDITY_FOR_FOREST_TRANSITION_LONG && humidity.max() > MIN_HUMIDITY_FOR_FOREST_TRANSITION_LONG) {
            transitionBiome = CustomOverworldBiomes.getSnowyToTemperateTransitionBiome(biome);
            if(transitionBiome != null) {
                float minHumidity =  Math.max(MultiNoiseUtil.toFloat(humidity.min()), MIN_HUMIDITY_FOR_FOREST_TRANSITION);
                float maxHumidity =  MultiNoiseUtil.toFloat(humidity.max());
                parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(temperature, rangeOf(minHumidity, maxHumidity), continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
                newHumidity = rangeOf(MultiNoiseUtil.toFloat(humidity.min()), MIN_HUMIDITY_FOR_FOREST_TRANSITION);
                hasTweakedBiomePlacement = true;
            }
        }
        else if(humidity.min() < MAX_HUMIDITY_FOR_FOREST_TRANSITION_LONG && humidity.max() > MAX_HUMIDITY_FOR_FOREST_TRANSITION_LONG) {
            transitionBiome = CustomOverworldBiomes.getHumidTransitionBiome(biome);
            if(transitionBiome != null) {
                float minHumidity =  MultiNoiseUtil.toFloat(humidity.min());
                float maxHumidity =  Math.min(MultiNoiseUtil.toFloat(humidity.max()), MAX_HUMIDITY_FOR_FOREST_TRANSITION);
                parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube(temperature, rangeOf(minHumidity, maxHumidity), continentalness, erosion, DEPTH_ZERO, weirdness, offset), transitionBiome));
                newHumidity = rangeOf(MAX_HUMIDITY_FOR_FOREST_TRANSITION, MultiNoiseUtil.toFloat(humidity.max()));
                hasTweakedBiomePlacement = true;
            }
        }

        // Place the original biome, but with tweaked parameters:
        parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube((newTemperature == null? temperature: newTemperature), (newHumidity == null? humidity: newHumidity), (newContinentalness == null? continentalness: newContinentalness), (newErosion == null? erosion: newErosion), DEPTH_ZERO, weirdness, offset), biome));

        // If biome has a surface cave associated, add it:
        RegistryKey<Biome> caveBiome = CustomOverworldBiomes.getSurfaceCaveBiome(biome);
        if(caveBiome != null) {
            parameters.accept(Pair.of(MultiNoiseUtil.createNoiseHypercube((newTemperature == null? temperature: newTemperature), (newHumidity == null? humidity: newHumidity), (newContinentalness == null? continentalness: newContinentalness), (newErosion == null? erosion: newErosion), DEPTH_SURFACE_CAVE, weirdness, offset + 0.075F), caveBiome));
        }

        // Cancel return, to override vanilla behavior:
        info.cancel();
    }


    private static final MultiNoiseUtil.ParameterRange[] cachedRanges = new MultiNoiseUtil.ParameterRange[32];

    private static MultiNoiseUtil.ParameterRange rangeOf(float min, float max) {
        // Note: this only runs once per world loading, so really no need to optimize speed, but very much need to optimize memory usage.
        // The idea here is to have only a few object instances of ParameterRange, to reduce memory lookups during chunk generation
        int lastIndex = 0;
        for(int i = 0; i < cachedRanges.length; i++) {
            if(cachedRanges[i] == null) break;
            lastIndex = i;
            if(MultiNoiseUtil.toFloat(cachedRanges[i].min()) == min && MultiNoiseUtil.toFloat(cachedRanges[i].max()) == max) return cachedRanges[i];
        }
        MultiNoiseUtil.ParameterRange newRange = MultiNoiseUtil.ParameterRange.of(min, max);
        cachedRanges[lastIndex] = newRange;
        return newRange;
    }
}
