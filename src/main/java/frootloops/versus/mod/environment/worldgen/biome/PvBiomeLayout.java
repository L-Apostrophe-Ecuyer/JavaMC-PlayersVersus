package frootloops.versus.mod.environment.worldgen.biome;

import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes.PlacedBiome;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes.PlacedBiomeType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.Parameter;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;

/**
 * The biome layout of the Players Versus world type, derived from vanilla's overworld layout:
 * <ul>
 *   <li>each vanilla surface slice is cut by the transition rules ({@link #RULES}) into disjoint boxes: the part inside
 *   a rule's region gets the transition biome, the rest keeps the slice's biome;</li>
 *   <li>vanilla's second copy of each surface slice (depth 1) is dropped, so the underground belongs to the cave
 *   biomes;</li>
 *   <li>vanilla's lush and dripstone cave entries are replaced, and the Players Versus cave biomes are added.</li>
 * </ul>
 * Only {@link PvBiomeSource} uses this; vanilla world types keep vanilla's layout.
 */
public final class PvBiomeLayout {

    /** One entry of the layout, and the name of the rule that placed it (shown by {@code /pvwg probe} and F3). */
    public record Entry(ParameterPoint parameters, ResourceKey<Biome> biome, String rule) {
    }

    static final Parameter SURFACE_DEPTH = Parameter.point(0.0F);
    static final Parameter SURFACE_CAVE_DEPTH = Parameter.span(0.1F, 0.25F);
    private static final float SURFACE_CAVE_OFFSET = 0.075F;
    /**
     * Depth of the dripstone and frosted caves that replace vanilla's dripstone entry. The old biome mixin passed the
     * continentalness range (0.8..1.0) as depth by mistake; this keeps that placement, because the dripstone rarity
     * was tuned in game with it.
     */
    static final Parameter DEEP_DRIPSTONE_DEPTH = Parameter.span(0.8F, 1.0F);

    private static final long MOUNTAIN_EROSION_MAX = Climate.quantizeCoord(-0.475F);
    private static final long MOUNTAIN_CONTINENTALNESS_MIN = Climate.quantizeCoord(0.03F);
    private static final long RIVER_VALLEY_WEIRDNESS = Climate.quantizeCoord(0.3F);
    private static final long WARM_MOUNTAINSIDE_FOREST_TEMPERATURE = Climate.quantizeCoord(0.1998F);
    /**
     * Birch forests turn dappled where the weirdness is above this and the temperature below {@link #DAPPLED_COOL}, and
     * old growth birch forests turn into vanilla's dappled forest. Vanilla's birch forests reach down to -0.15, where
     * they border taigas.
     */
    static final float DAPPLED_WEIRDNESS = 0.0F;
    static final float DAPPLED_COOL = 0.0F;
    /** Below this temperature, next to the taigas, birch forests become dappled taiga instead of sparse dappled forest. */
    static final float DAPPLED_COLD = -0.075F;

    private record Rule(String name, Box region, BiFunction<Box, ResourceKey<Biome>, ResourceKey<Biome>> target) {
    }

    /** Earlier rules win where regions overlap. A target of {@code null} means the rule doesn't apply to that slice. */
    private static final List<Rule> RULES = List.of(
            new Rule("mountainside",
                    Box.ALL.withMax(Box.E, MOUNTAIN_EROSION_MAX).withMin(Box.C, MOUNTAIN_CONTINENTALNESS_MIN),
                    PvBiomeLayout::mountainTarget),
            new Rule("peak-warm", Box.ALL.withMin(Box.T, Climate.quantizeCoord(0.145F)),
                    (slice, biome) -> biome == Biomes.GROVE ? Biomes.TAIGA
                            : biome == Biomes.SNOWY_SLOPES ? Biomes.WINDSWEPT_HILLS : null),
            new Rule("peak-cold", Box.ALL.withMax(Box.T, Climate.quantizeCoord(0.235F)),
                    (slice, biome) -> biome == Biomes.STONY_PEAKS ? Biomes.WINDSWEPT_GRAVELLY_HILLS : null),
            new Rule("frozen", Box.ALL.withMin(Box.T, Climate.quantizeCoord(-0.55F)).withMax(Box.T, Climate.quantizeCoord(-0.375F)),
                    (slice, biome) -> CustomOverworldBiomes.getSnowyToTemperateTransitionBiome(biome)),
            new Rule("humid", Box.ALL.withMin(Box.H, Climate.quantizeCoord(0.275F)).withMax(Box.H, Climate.quantizeCoord(0.35F)),
                    (slice, biome) -> CustomOverworldBiomes.getHumidTransitionBiome(biome)),
            // Dappled trees (26.3's poplars) where the weirdness is positive and the temperature negative, after the
            // transitions, so only in the birch forests they leave; vanilla's own dappled forests give way to flower forests.
            new Rule("flower-forest", Box.ALL,
                    (slice, biome) -> CustomOverworldBiomes.DAPPLED_FOREST.equals(biome) ? Biomes.FLOWER_FOREST : null),
            new Rule("weird-cold", Box.ALL.withMin(Box.W, Climate.quantizeCoord(DAPPLED_WEIRDNESS)).withMax(Box.T, Climate.quantizeCoord(DAPPLED_COLD)),
                    (slice, biome) -> biome == Biomes.BIRCH_FOREST ? CustomOverworldBiomes.DAPPLED_TAIGA
                            : biome == Biomes.OLD_GROWTH_BIRCH_FOREST ? CustomOverworldBiomes.DAPPLED_FOREST : null),
            new Rule("weird-cool", Box.ALL.withMin(Box.W, Climate.quantizeCoord(DAPPLED_WEIRDNESS)).withMax(Box.T, Climate.quantizeCoord(DAPPLED_COOL)),
                    (slice, biome) -> biome == Biomes.BIRCH_FOREST ? CustomOverworldBiomes.SPARSE_DAPPLED_FOREST
                            : biome == Biomes.OLD_GROWTH_BIRCH_FOREST ? CustomOverworldBiomes.DAPPLED_FOREST : null));

    private PvBiomeLayout() {
    }

    public static List<Entry> build() {
        List<Entry> surface = new ArrayList<>();
        List<Entry> underground = new ArrayList<>();
        new OverworldBiomeBuilder().addBiomes(pair -> {
            ParameterPoint parameters = pair.getFirst();
            ResourceKey<Biome> biome = pair.getSecond();
            if (isPoint(parameters.depth(), 0.0F)) {
                emitSurface(parameters, biome, surface);
            } else if (isPoint(parameters.depth(), 1.0F)) {
                // vanilla repeats every surface slice at depth 1; the Players Versus underground belongs to cave biomes
            } else if (biome == Biomes.LUSH_CAVES) {
                emitLushReplacement(parameters, underground);
            } else if (biome == Biomes.DRIPSTONE_CAVES) {
                emitDripstoneReplacement(parameters, underground);
            } else {
                underground.add(new Entry(parameters, biome, "vanilla-cave"));
            }
        });

        List<Entry> entries = new ArrayList<>(surface);
        for (PlacedBiome land : CustomOverworldBiomes.landBiomesToPlaceInOverorld) {
            if (land.type() != PlacedBiomeType.SURFACE) continue;
            entries.add(new Entry(Climate.parameters(land.temperature(), land.humidity(), land.continentalness(),
                    land.erosion(), SURFACE_DEPTH, land.weirdness(), land.isRare() ? 0.04F : -0.01F), land.biome(), "added-surface"));
        }
        entries.addAll(underground);
        for (PlacedBiome cave : CustomOverworldBiomes.caveBiomesToPlaceInOverorld) {
            if (cave.type() == PlacedBiomeType.SURFACE) continue;
            entries.add(new Entry(Climate.parameters(cave.temperature(), cave.humidity(), cave.continentalness(),
                    cave.erosion(), caveDepth(cave.type()), cave.weirdness(), caveOffset(cave)), cave.biome(), "cave"));
        }
        return entries;
    }

    private static void emitSurface(ParameterPoint slice, ResourceKey<Biome> biome, List<Entry> out) {
        Box sliceBox = Box.of(slice);
        List<Box> original = List.of(sliceBox);
        for (Rule rule : RULES) {
            ResourceKey<Biome> target = rule.target().apply(sliceBox, biome);
            if (target == null) continue;
            List<Box> rest = new ArrayList<>();
            for (Box piece : original) {
                Box inside = piece.intersect(rule.region());
                if (inside == null) {
                    rest.add(piece);
                    continue;
                }
                out.add(new Entry(inside.toHypercube(SURFACE_DEPTH, slice.offset()), target, rule.name()));
                rest.addAll(piece.minus(rule.region()));
            }
            original = rest;
        }
        for (Box piece : original) {
            out.add(new Entry(piece.toHypercube(SURFACE_DEPTH, slice.offset()), biome, "vanilla"));
        }
        ResourceKey<Biome> cave = CustomOverworldBiomes.getSurfaceCaveBiome(biome);
        if (cave != null) {
            long caveOffset = Climate.quantizeCoord(Climate.unquantizeCoord(slice.offset()) + SURFACE_CAVE_OFFSET);
            for (Box piece : original) {
                out.add(new Entry(piece.toHypercube(SURFACE_CAVE_DEPTH, caveOffset), cave, "surface-cave"));
            }
        }
    }

    @Nullable
    private static ResourceKey<Biome> mountainTarget(Box slice, ResourceKey<Biome> biome) {
        boolean riverValley = slice.min[Box.W] >= -RIVER_VALLEY_WEIRDNESS && slice.max[Box.W] <= RIVER_VALLEY_WEIRDNESS;
        if (riverValley) return null;
        ResourceKey<Biome> target = CustomOverworldBiomes.getMountainTransitionBiome(biome);
        if (target == CustomOverworldBiomes.MOUNTAINSIDE_FOREST && slice.min[Box.T] > WARM_MOUNTAINSIDE_FOREST_TEMPERATURE) {
            return CustomOverworldBiomes.MOUNTAINSIDE_FOREST_WARM;
        }
        return target;
    }

    private static void emitLushReplacement(ParameterPoint lush, List<Entry> out) {
        float offset = Climate.unquantizeCoord(lush.offset());
        Parameter frostedHumidity = Parameter.span(-0.6F, -0.4F);
        out.add(new Entry(Climate.parameters(Parameter.span(-0.4F, 0.8F), lush.humidity(), lush.continentalness(),
                lush.erosion(), Parameter.span(0.15F, 0.5F), lush.weirdness(), offset + 0.01F), Biomes.LUSH_CAVES, "lush"));
        out.add(new Entry(Climate.parameters(Parameter.span(-0.3F, 0.8F), frostedHumidity, lush.continentalness(),
                lush.erosion(), Parameter.span(0.2F, 0.5F), lush.weirdness(), offset + 0.01F), Biomes.LUSH_CAVES, "lush"));
        out.add(new Entry(Climate.parameters(Parameter.span(-1.0F, -0.8F), lush.humidity(), lush.continentalness(),
                lush.erosion(), Parameter.span(0.15F, 0.5F), lush.weirdness(), offset), CustomOverworldBiomes.FROSTED_CAVE, "lush-frosted"));
        out.add(new Entry(Climate.parameters(Parameter.span(-1.0F, -0.5F), frostedHumidity, lush.continentalness(),
                Parameter.span(-1.0F, -0.6F), Parameter.span(0.2F, 0.5F), lush.weirdness(), offset),
                CustomOverworldBiomes.FROSTED_CAVE, "lush-frosted"));
    }

    private static void emitDripstoneReplacement(ParameterPoint dripstone, List<Entry> out) {
        float offset = Climate.unquantizeCoord(dripstone.offset());
        out.add(new Entry(Climate.parameters(Parameter.span(-0.6F, 1.0F), dripstone.humidity(), dripstone.continentalness(),
                dripstone.erosion(), DEEP_DRIPSTONE_DEPTH, dripstone.weirdness(), offset), Biomes.DRIPSTONE_CAVES, "dripstone"));
        out.add(new Entry(Climate.parameters(Parameter.span(-1.0F, -0.7F), dripstone.humidity(), dripstone.continentalness(),
                dripstone.erosion(), DEEP_DRIPSTONE_DEPTH, dripstone.weirdness(), offset), CustomOverworldBiomes.FROSTED_CAVE, "dripstone-frosted"));
    }

    private static Parameter caveDepth(PlacedBiomeType type) {
        return switch (type) {
            case SURFACE_CAVE -> Parameter.span(0.1F, 0.25F);
            case CAVE -> Parameter.span(0.2F, 0.4F);
            case GENERIC_CAVE -> Parameter.span(0.25F, 0.65F);
            default -> Parameter.point(0.9F);
        };
    }

    private static float caveOffset(PlacedBiome cave) {
        if (cave.type() == PlacedBiomeType.GENERIC_CAVE) return 0.07F;
        if (cave.type() == PlacedBiomeType.GENERIC_DEEP_CAVE) return 0.05F;
        return cave.isRare() ? 0.04F : 0.0F;
    }

    static boolean isPoint(Parameter range, float value) {
        long point = Climate.quantizeCoord(value);
        return range.min() == point && range.max() == point;
    }

    /**
     * A box over temperature, humidity, continentalness, erosion and weirdness, in {@link Climate#quantizeCoord} units.
     * Boxes are closed like {@link Parameter}; two boxes that only share a face don't overlap.
     */
    static final class Box {
        static final int T = 0, H = 1, C = 2, E = 3, W = 4, AXES = 5;
        static final Box ALL = new Box(filled(Long.MIN_VALUE), filled(Long.MAX_VALUE));

        final long[] min;
        final long[] max;

        Box(long[] min, long[] max) {
            this.min = min;
            this.max = max;
        }

        static Box of(ParameterPoint cube) {
            Parameter[] ranges = {cube.temperature(), cube.humidity(), cube.continentalness(), cube.erosion(), cube.weirdness()};
            long[] min = new long[AXES], max = new long[AXES];
            for (int axis = 0; axis < AXES; axis++) {
                min[axis] = ranges[axis].min();
                max[axis] = ranges[axis].max();
            }
            return new Box(min, max);
        }

        Box withMin(int axis, long value) {
            long[] newMin = this.min.clone();
            newMin[axis] = value;
            return new Box(newMin, this.max.clone());
        }

        Box withMax(int axis, long value) {
            long[] newMax = this.max.clone();
            newMax[axis] = value;
            return new Box(this.min.clone(), newMax);
        }

        /** The overlap with {@code other}, or {@code null} if they only touch or don't meet. */
        @Nullable
        Box intersect(Box other) {
            long[] lo = new long[AXES], hi = new long[AXES];
            for (int axis = 0; axis < AXES; axis++) {
                lo[axis] = Math.max(this.min[axis], other.min[axis]);
                hi[axis] = Math.min(this.max[axis], other.max[axis]);
                if (lo[axis] >= hi[axis]) return null;
            }
            return new Box(lo, hi);
        }

        /** Disjoint boxes that together cover this box minus {@code other}. */
        List<Box> minus(Box other) {
            if (intersect(other) == null) return List.of(this);
            List<Box> pieces = new ArrayList<>();
            long[] lo = this.min.clone(), hi = this.max.clone();
            for (int axis = 0; axis < AXES; axis++) {
                if (lo[axis] < other.min[axis]) {
                    pieces.add(slab(lo, hi, axis, lo[axis], other.min[axis]));
                    lo[axis] = other.min[axis];
                }
                if (hi[axis] > other.max[axis]) {
                    pieces.add(slab(lo, hi, axis, other.max[axis], hi[axis]));
                    hi[axis] = other.max[axis];
                }
            }
            return pieces;
        }

        ParameterPoint toHypercube(Parameter depth, long offset) {
            return new ParameterPoint(range(T), range(H), range(C), range(E), depth, range(W), offset);
        }

        private Parameter range(int axis) {
            return new Parameter(this.min[axis], this.max[axis]);
        }

        private static Box slab(long[] lo, long[] hi, int axis, long from, long to) {
            long[] min = lo.clone(), max = hi.clone();
            min[axis] = from;
            max[axis] = to;
            return new Box(min, max);
        }

        private static long[] filled(long value) {
            long[] array = new long[AXES];
            Arrays.fill(array, value);
            return array;
        }
    }
}
