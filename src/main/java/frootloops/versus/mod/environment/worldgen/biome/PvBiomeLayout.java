package frootloops.versus.mod.environment.worldgen.biome;

import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes.PlacedBiome;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes.PlacedBiomeType;
import frootloops.versus.mod.environment.worldgen.PvWorldgenConstants;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
 *   <li>each vanilla surface slice is first cut by the replacements ({@link #REPLACEMENTS}): the part inside one's region
 *   is another biome from then on (the windswept biomes: in the 3D-noise zone, below temperature 0, the one that fits,
 *   anywhere else what lies beside; old growth birch forests above freezing; the cold half of swamps);</li>
 *   <li>then each piece is cut by the transition rules ({@link #RULES}) into disjoint boxes: the part inside a rule's
 *   region gets the transition biome, the rest keeps the piece's biome;</li>
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
    /** Where the mushroom islands' continentalness ends (vanilla's mushroom fields: -1.2 to -1.05). */
    static final long ISLANDS_END = Climate.quantizeCoord(-1.05F);
    /**
     * Depth of the dripstone and ice caves that replace vanilla's dripstone entry. The old biome mixin passed the
     * continentalness range (0.8..1.0) as depth by mistake; this keeps that placement, because the dripstone rarity
     * was tuned in game with it.
     */
    static final Parameter DEEP_DRIPSTONE_DEPTH = Parameter.span(0.8F, 1.0F);

    /** Mountainsides cover erosion up to this (the tests probe just above it). */
    static final long MOUNTAIN_EROSION_MAX = Climate.quantizeCoord(-0.385F);
    private static final long MOUNTAIN_CONTINENTALNESS_MIN = Climate.quantizeCoord(0.03F);
    private static final long RIVER_VALLEY_WEIRDNESS = Climate.quantizeCoord(0.3F);
    private static final long WARM_MOUNTAINSIDE_FOREST_TEMPERATURE = Climate.quantizeCoord(0.1998F);
    /**
     * Birch forests turn dappled where the weirdness is above this and the temperature below {@link #DAPPLED_COOL}, old
     * growth birch forests turn into vanilla's dappled forest ({@link #weirdTarget}).
     * Vanilla's birch forests reach down to -0.15, where they border taigas.
     */
    static final float DAPPLED_WEIRDNESS = 0.0F;
    static final float DAPPLED_COOL = 0.0F;
    static final float DAPPLED_COLD = -0.075F;

    /**
     * The warm band: the cool edge of savannas and jungles (vanilla's from temperature 0.2), up to this temperature. The
     * old biome mixin's warm transition stopped there too.
     */
    static final float WARM_BAND_MAX = 0.3F;
    /** Jungles with a humidity up to this border birch forests and forests (warm oak forest); wetter ones dark forests. */
    static final float WARM_HUMIDITY_SPLIT = 0.3F;
    /**
     * The swamp's rim: swamps (vanilla's from erosion 0.55) up to this erosion turn into humid oak forest, mangrove swamps
     * into the mangrove marsh.
     */
    static final float SWAMP_RIM_MAX = 0.565F;
    /** The swamps (warm and cold) and the mangrove swamps, past the rim, turn into wetlands (grassy wetlands, mangrove marsh) at this humidity and under. */
    static final float WETLAND_HUMIDITY_MAX = -0.15F;
    /** The dark forest's warm side (vanilla's dark forests reach temperature 0.2, where the dark jungle forest begins): the warm dark forest, from this up. */
    static final float WARM_DARK_FOREST_MIN = 0.12F;
    /** The dark birch forest's strip on the dark forest's side of their border (0.3): dark forest up to this humidity. */
    static final float DARK_BIRCH_MAX = 0.31F;

    /** Plains (vanilla's humidity -0.35 to -0.1) and forest (-0.1 to 0.1) meet at this humidity; the forest edge reaches this far either side. */
    static final float FOREST_EDGE_HUMIDITY = -0.1F, FOREST_EDGE_HALF_WIDTH = 0.05F;
    /** Forest and taiga meet at this humidity (temperatures -0.45 to -0.15); the mixed forest reaches this far either side. */
    static final float MIXED_FOREST_HUMIDITY = 0.1F, MIXED_FOREST_HALF_WIDTH = 0.05F;
    /** The taigas meet the birch and dark forests at this temperature; their transitions reach this far either side. */
    static final float TAIGA_EDGE_TEMPERATURE = -0.15F, TAIGA_EDGE_HALF_WIDTH = 0.05F;
    /** Vanilla's warm climates (savannas, jungles) begin at this temperature; its forest there, and its plains' strip next to the jungle, are warm oak forest. */
    static final float WARM_CLIMATE_MIN = 0.2F;
    static final float WARM_PLAINS_JUNGLE_SIDE = 0.04F;
    /** The old oak forest: a pocket of the forest's climate where the weirdness is positive. */
    static final float OLD_OAK_TEMPERATURE_MIN = -0.1F, OLD_OAK_TEMPERATURE_MAX = 0.1F, OLD_OAK_HUMIDITY_MIN = -0.04F, OLD_OAK_HUMIDITY_MAX = 0.06F;
    /** The prairie: a pocket of the plains' climate where the weirdness is negative. */
    static final float PRAIRIE_TEMPERATURE_MIN = -0.3F, PRAIRIE_TEMPERATURE_MAX = -0.05F, PRAIRIE_HUMIDITY_MIN = -0.3F, PRAIRIE_HUMIDITY_MAX = -0.2F;
    /** The pale garden's cold strip (its temperatures start at -0.15), next to the old growth taigas: the pale taiga. */
    static final float PALE_TAIGA_MAX = -0.1F;
    /**
     * The pale swamp: swamps (warm and cold) this wet, near the dark forests' humidity (few swamps are: about one in
     * twenty of the swamps' columns, against one in two hundred with a weirdness condition too).
     */
    static final float PALE_SWAMP_HUMIDITY_MIN = 0.25F;
    /** Swamps (vanilla's from temperature -0.45 to 0.2) up to this temperature are cold swamps: they border taigas. */
    static final float COLD_SWAMP_MAX = -0.15F;
    /** Above this temperature, windswept biomes and old growth birch forests give way to plainer ones. */
    static final float TEMPERATE_MIN = 0.0F;
    /** The rocky biomes lie where the ground is level: erosion from this up. */
    static final float ROCKY_EROSION_MIN = 0.05F;
    /** The rocky jungle: the jungles from the warm band up to this temperature. */
    static final float ROCKY_JUNGLE_MAX = 0.36F;
    /**
     * The 3D-noise terrain's climate (part 2's zone, {@link PvWorldgenConstants}): humidity 0 to 0.3 with erosion 0.3 to
     * 0.6. Rugged, overhanging ground, where no rocky biome goes.
     */
    static final float NOISE_HUMIDITY_MAX = (float) PvWorldgenConstants.NOISE_ZONE_HUMIDITY_MAX;
    static final float NOISE_EROSION_MIN = (float) PvWorldgenConstants.NOISE_ZONE_EROSION_MIN, NOISE_EROSION_MAX = (float) PvWorldgenConstants.NOISE_ZONE_EROSION_MAX;

    /**
     * The windswept biomes are the rugged terrain's, so they stand in the 3D-noise zone: where its window is at half
     * strength or more (humidity 0.025 to 0.275, continentalness 0.075 and up: the half of each fade), below
     * {@link #TEMPERATE_MIN} (the rule that keeps them out of the warm climates).
     */
    static final long WINDSWEPT_HUMIDITY_MIN = Climate.quantizeCoord((float) (PvWorldgenConstants.NOISE_ZONE_HUMIDITY_MIN + PvWorldgenConstants.NOISE_ZONE_HUMIDITY_FADE / 2));
    static final long WINDSWEPT_HUMIDITY_MAX = Climate.quantizeCoord((float) (PvWorldgenConstants.NOISE_ZONE_HUMIDITY_MAX - PvWorldgenConstants.NOISE_ZONE_HUMIDITY_FADE / 2));
    static final long WINDSWEPT_CONTINENTALNESS_MIN = Climate.quantizeCoord((float) (PvWorldgenConstants.NOISE_ZONE_CONTINENTALNESS_MIN + PvWorldgenConstants.NOISE_ZONE_CONTINENTALNESS_FADE / 2));
    /** In the zone, from this humidity (vanilla's, where its windswept biomes turn to forest) the windswept forest, below it hills. */
    private static final float WINDSWEPT_FOREST_HUMIDITY = 0.1F;
    /** The zone's hills are gravelly (stony, dry) below this temperature: the cold, dry windswept biome of vanilla's tables. */
    private static final float WINDSWEPT_GRAVELLY_MAX_TEMPERATURE = -0.15F;

    private static final Set<ResourceKey<Biome>> WINDSWEPT = Set.of(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
            Biomes.WINDSWEPT_SAVANNA);

    private record Rule(String name, Box region, BiFunction<Box, ResourceKey<Biome>, ResourceKey<Biome>> target) {
    }

    /**
     * Biomes swapped for others before the transitions: the part of a slice inside the region is the target from then on,
     * so a transition can still apply to it (a windswept savanna's cool edge turns into the Tuscan grove as a savanna's
     * does). A target of {@code null} means the replacement doesn't apply; it is given the part inside the region.
     */
    private static final List<Rule> REPLACEMENTS = List.of(
            // Vanilla's windswept slices: in the 3D-noise zone the windswept biome that fits, anywhere else below temperature 0 what lies beside
            new Rule("windswept-zone", Box.ALL.withMin(Box.H, WINDSWEPT_HUMIDITY_MIN).withMax(Box.H, WINDSWEPT_HUMIDITY_MAX)
                    .withMin(Box.C, WINDSWEPT_CONTINENTALNESS_MIN).withMax(Box.T, Climate.quantizeCoord(TEMPERATE_MIN)), PvBiomeLayout::windsweptZoneTarget),
            new Rule("windswept", Box.ALL.withMax(Box.T, Climate.quantizeCoord(TEMPERATE_MIN)), PvBiomeLayout::windsweptBesideTarget),
            new Rule("temperate", Box.ALL.withMin(Box.T, Climate.quantizeCoord(TEMPERATE_MIN)), PvBiomeLayout::temperateTarget),
            new Rule("cold-swamp", Box.ALL.withMax(Box.T, Climate.quantizeCoord(COLD_SWAMP_MAX)),
                    (part, biome) -> biome == Biomes.SWAMP ? CustomOverworldBiomes.COLD_SWAMP : null));

    /** Earlier rules win where regions overlap. A target of {@code null} means the rule doesn't apply to that slice. */
    private static final List<Rule> RULES = List.of(
            new Rule("mountainside",
                    Box.ALL.withMax(Box.E, MOUNTAIN_EROSION_MAX).withMin(Box.C, MOUNTAIN_CONTINENTALNESS_MIN),
                    PvBiomeLayout::mountainTarget),
            new Rule("peak-warm", Box.ALL.withMin(Box.T, Climate.quantizeCoord(0.145F)),
                    // Snowy slopes this warm are above freezing: an alpine meadow, not windswept hills
                    (slice, biome) -> biome == Biomes.GROVE ? Biomes.TAIGA
                            : biome == Biomes.SNOWY_SLOPES ? Biomes.MEADOW : null),
            new Rule("peak-cold", Box.ALL.withMax(Box.T, Climate.quantizeCoord(0.235F)),
                    // Stony peaks start at 0.2, above freezing: their cold edge is an alpine meadow, not windswept hills
                    (slice, biome) -> biome == Biomes.STONY_PEAKS ? Biomes.MEADOW : null),
            new Rule("frozen", Box.ALL.withMin(Box.T, Climate.quantizeCoord(-0.55F)).withMax(Box.T, Climate.quantizeCoord(-0.375F)),
                    (slice, biome) -> CustomOverworldBiomes.getSnowyToTemperateTransitionBiome(biome)),
            // The level part of the savannas' cool edge, before the Tuscan grove takes the rest
            new Rule("rocky-plains", Box.ALL.withMax(Box.T, Climate.quantizeCoord(WARM_BAND_MAX)).withMin(Box.E, Climate.quantizeCoord(ROCKY_EROSION_MIN)),
                    (slice, biome) -> biome == Biomes.SAVANNA || biome == Biomes.SAVANNA_PLATEAU ? CustomOverworldBiomes.ROCKY_PLAINS : null),
            // Before the humid band, which would otherwise cut a strip of sparse jungle across the warm one
            new Rule("warm", Box.ALL.withMax(Box.T, Climate.quantizeCoord(WARM_BAND_MAX)), PvBiomeLayout::warmTarget),
            // The level jungle just past the warm band, out of the 3D-noise terrain's climate: three boxes
            new Rule("rocky-jungle", rockyJungle().withMin(Box.H, Climate.quantizeCoord(NOISE_HUMIDITY_MAX)), PvBiomeLayout::rockyJungleTarget),
            new Rule("rocky-jungle", rockyJungle().withMax(Box.H, Climate.quantizeCoord(NOISE_HUMIDITY_MAX)).withMax(Box.E, Climate.quantizeCoord(NOISE_EROSION_MIN)),
                    PvBiomeLayout::rockyJungleTarget),
            new Rule("rocky-jungle", rockyJungle().withMax(Box.H, Climate.quantizeCoord(NOISE_HUMIDITY_MAX)).withMin(Box.E, Climate.quantizeCoord(NOISE_EROSION_MAX)),
                    PvBiomeLayout::rockyJungleTarget),
            new Rule("humid", Box.ALL.withMin(Box.H, Climate.quantizeCoord(0.275F)).withMax(Box.H, Climate.quantizeCoord(0.35F)),
                    (slice, biome) -> CustomOverworldBiomes.getHumidTransitionBiome(biome)),
            // The dark birch strip's dark side, narrower than the humid band's: the dark forest keeps most of its climate
            new Rule("dark-birch", Box.ALL.withMax(Box.H, Climate.quantizeCoord(DARK_BIRCH_MAX)),
                    (slice, biome) -> biome == Biomes.DARK_FOREST ? CustomOverworldBiomes.DARK_BIRCH_FOREST : null),
            new Rule("warm-dark", Box.ALL.withMin(Box.T, Climate.quantizeCoord(WARM_DARK_FOREST_MIN)),
                    (slice, biome) -> biome == Biomes.DARK_FOREST ? CustomOverworldBiomes.WARM_DARK_FOREST : null),
            // The taigas' warm edge (temperature -0.15): with the dark forest, the dark taiga forest (dark oak among spruce)
            new Rule("dark-taiga", taigaEdge(),
                    (slice, biome) -> biome == Biomes.DARK_FOREST || biome == Biomes.OLD_GROWTH_SPRUCE_TAIGA || biome == Biomes.OLD_GROWTH_PINE_TAIGA
                            ? CustomOverworldBiomes.DARK_TAIGA_FOREST : null),
            // The pale garden's cold strip, next to the old growth taigas
            new Rule("pale-taiga", Box.ALL.withMax(Box.T, Climate.quantizeCoord(PALE_TAIGA_MAX)),
                    (slice, biome) -> biome == Biomes.PALE_GARDEN ? CustomOverworldBiomes.PALE_TAIGA : null),
            // With the birch forest, the birch taiga forest; where the weirdness is positive the birch forest's cold side is
            // the dappled taiga (weird-cold), and the taiga's warm edge is dappled taiga with it
            new Rule("taiga-birch", taigaEdge().withMax(Box.W, 0L),
                    (slice, biome) -> biome == Biomes.TAIGA || biome == Biomes.BIRCH_FOREST ? CustomOverworldBiomes.BIRCH_TAIGA_FOREST : null),
            new Rule("taiga-dappled", taigaEdge().withMin(Box.W, 0L),
                    (slice, biome) -> biome == Biomes.TAIGA ? CustomOverworldBiomes.DAPPLED_TAIGA : null),
            // Forest and taiga, plains and forest: their humidity borders
            new Rule("mixed-forest", Box.ALL.withMax(Box.T, Climate.quantizeCoord(TAIGA_EDGE_TEMPERATURE))
                    .withMin(Box.H, Climate.quantizeCoord(MIXED_FOREST_HUMIDITY - MIXED_FOREST_HALF_WIDTH))
                    .withMax(Box.H, Climate.quantizeCoord(MIXED_FOREST_HUMIDITY + MIXED_FOREST_HALF_WIDTH)),
                    (slice, biome) -> biome == Biomes.FOREST || biome == Biomes.TAIGA ? CustomOverworldBiomes.MIXED_FOREST : null),
            new Rule("forest-edge", Box.ALL.withMax(Box.T, Climate.quantizeCoord(WARM_CLIMATE_MIN))
                    .withMin(Box.H, Climate.quantizeCoord(FOREST_EDGE_HUMIDITY - FOREST_EDGE_HALF_WIDTH))
                    .withMax(Box.H, Climate.quantizeCoord(FOREST_EDGE_HUMIDITY + FOREST_EDGE_HALF_WIDTH)),
                    (slice, biome) -> biome == Biomes.FOREST || biome == Biomes.PLAINS ? CustomOverworldBiomes.FOREST_EDGE : null),
            // Pockets of the forest's and the plains' climates
            new Rule("old-oak", Box.ALL.withMin(Box.T, Climate.quantizeCoord(OLD_OAK_TEMPERATURE_MIN)).withMax(Box.T, Climate.quantizeCoord(OLD_OAK_TEMPERATURE_MAX))
                    .withMin(Box.H, Climate.quantizeCoord(OLD_OAK_HUMIDITY_MIN)).withMax(Box.H, Climate.quantizeCoord(OLD_OAK_HUMIDITY_MAX)).withMin(Box.W, 0L),
                    (slice, biome) -> biome == Biomes.FOREST ? CustomOverworldBiomes.OLD_OAK_FOREST : null),
            new Rule("prairie", Box.ALL.withMin(Box.T, Climate.quantizeCoord(PRAIRIE_TEMPERATURE_MIN)).withMax(Box.T, Climate.quantizeCoord(PRAIRIE_TEMPERATURE_MAX))
                    .withMin(Box.H, Climate.quantizeCoord(PRAIRIE_HUMIDITY_MIN)).withMax(Box.H, Climate.quantizeCoord(PRAIRIE_HUMIDITY_MAX)).withMax(Box.W, 0L),
                    (slice, biome) -> biome == Biomes.PLAINS ? CustomOverworldBiomes.PRAIRIE : null),
            // In the warm climates vanilla's forest (between savanna and jungle) and its plains' strip next to the sparse jungle
            new Rule("hot-forest", Box.ALL.withMin(Box.T, Climate.quantizeCoord(WARM_CLIMATE_MIN)),
                    (slice, biome) -> biome == Biomes.FOREST ? CustomOverworldBiomes.WARM_OAK_FOREST : null),
            new Rule("hot-plains", Box.ALL.withMin(Box.T, Climate.quantizeCoord(WARM_CLIMATE_MIN)).withMin(Box.H, Climate.quantizeCoord(WARM_PLAINS_JUNGLE_SIDE)),
                    (slice, biome) -> biome == Biomes.PLAINS ? CustomOverworldBiomes.WARM_OAK_FOREST : null),
            new Rule("swamp-rim", Box.ALL.withMax(Box.E, Climate.quantizeCoord(SWAMP_RIM_MAX)),
                    (slice, biome) -> biome == Biomes.SWAMP || biome == CustomOverworldBiomes.COLD_SWAMP ? CustomOverworldBiomes.HUMID_OAK_FOREST
                            : biome == Biomes.MANGROVE_SWAMP ? CustomOverworldBiomes.MANGROVE_MARSH : null),
            // Past the rim: the wettest swamps are pale, the drier part of every swamp wetlands
            new Rule("pale-swamp", Box.ALL.withMin(Box.H, Climate.quantizeCoord(PALE_SWAMP_HUMIDITY_MIN)),
                    (slice, biome) -> biome == Biomes.SWAMP || biome == CustomOverworldBiomes.COLD_SWAMP ? CustomOverworldBiomes.PALE_SWAMP : null),
            new Rule("wetlands", Box.ALL.withMax(Box.H, Climate.quantizeCoord(WETLAND_HUMIDITY_MAX)),
                    (slice, biome) -> biome == Biomes.SWAMP || biome == CustomOverworldBiomes.COLD_SWAMP ? CustomOverworldBiomes.GRASSY_WETLANDS
                            : biome == Biomes.MANGROVE_SWAMP ? CustomOverworldBiomes.MANGROVE_MARSH : null),

            // The cold, level side of old growth birch forests, before it turns dappled
            new Rule("rocky-birch", Box.ALL.withMax(Box.T, Climate.quantizeCoord(DAPPLED_COLD)).withMin(Box.E, Climate.quantizeCoord(ROCKY_EROSION_MIN))
                    .withMax(Box.E, Climate.quantizeCoord(NOISE_EROSION_MIN)),
                    (slice, biome) -> biome == Biomes.OLD_GROWTH_BIRCH_FOREST ? CustomOverworldBiomes.ROCKY_BIRCH_FOREST : null),
            new Rule("flower-forest", Box.ALL,
                    (slice, biome) -> CustomOverworldBiomes.DAPPLED_FOREST.equals(biome) ? Biomes.FLOWER_FOREST : null),
            new Rule("weird-cold", Box.ALL.withMin(Box.W, Climate.quantizeCoord(DAPPLED_WEIRDNESS)).withMax(Box.T, Climate.quantizeCoord(DAPPLED_COLD)),
                    (slice, biome) -> weirdTarget(biome, true)),
            new Rule("weird-cool", Box.ALL.withMin(Box.W, Climate.quantizeCoord(DAPPLED_WEIRDNESS)).withMax(Box.T, Climate.quantizeCoord(DAPPLED_COOL)),
                    (slice, biome) -> weirdTarget(biome, false)));

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

        // The mushroom islands' caves are theirs alone: no other cave reaches their continentalness (vanilla's ice caves do)
        underground.replaceAll(PvBiomeLayout::offTheIslands);
        underground.removeIf(Objects::isNull);

        List<Entry> entries = new ArrayList<>(surface);
        for (PlacedBiome land : CustomOverworldBiomes.landBiomesToPlaceInOverorld) {
            if (land.type() != PlacedBiomeType.SURFACE) continue;
            entries.add(new Entry(Climate.parameters(land.temperature(), land.humidity(), land.continentalness(),
                    land.erosion(), SURFACE_DEPTH, land.weirdness(), land.isRare() ? 0.04F : -0.01F), land.biome(), "added-surface"));
        }
        entries.addAll(underground);
        for (PlacedBiome cave : CustomOverworldBiomes.caveBiomesToPlaceInOverorld) {
            if (cave.type() == PlacedBiomeType.SURFACE) continue;
            Entry entry = new Entry(Climate.parameters(cave.temperature(), cave.humidity(), cave.continentalness(),
                    cave.erosion(), caveDepth(cave.type()), cave.weirdness(), caveOffset(cave)), cave.biome(), "cave");
            if (cave.biome() != CustomOverworldBiomes.MUSHROOM_CAVES) entry = offTheIslands(entry);
            if (entry != null) entries.add(entry);
        }
        return entries;
    }

    private static void emitSurface(ParameterPoint slice, ResourceKey<Biome> biome, List<Entry> out) {
        // The replacements first: each part of the slice goes on as its own biome
        List<Box> kept = List.of(Box.of(slice));
        for (Rule replacement : REPLACEMENTS) {
            List<Box> rest = new ArrayList<>();
            for (Box piece : kept) {
                Box inside = piece.intersect(replacement.region());
                ResourceKey<Biome> swapped = inside == null ? null : replacement.target().apply(inside, biome);
                if (swapped == null) {
                    rest.add(piece);
                    continue;
                }
                emitPiece(inside, swapped, replacement.name(), slice.offset(), out);
                rest.addAll(piece.minus(replacement.region()));
            }
            kept = rest;
        }
        for (Box piece : kept) emitPiece(piece, biome, "vanilla", slice.offset(), out);
    }

    /**
     * One piece of a slice, as this biome (named {@code untouched} where no transition applies): cut by the transition
     * rules, and the part no rule takes over gets the biome's surface cave below it.
     */
    private static void emitPiece(Box pieceBox, ResourceKey<Biome> biome, String untouched, long offset, List<Entry> out) {
        List<Box> original = List.of(pieceBox);
        for (Rule rule : RULES) {
            ResourceKey<Biome> target = rule.target().apply(pieceBox, biome);
            if (target == null) continue;
            List<Box> rest = new ArrayList<>();
            for (Box piece : original) {
                Box inside = piece.intersect(rule.region());
                if (inside == null) {
                    rest.add(piece);
                    continue;
                }
                out.add(new Entry(inside.toHypercube(SURFACE_DEPTH, offset), target, rule.name()));
                rest.addAll(piece.minus(rule.region()));
            }
            original = rest;
        }
        for (Box piece : original) {
            out.add(new Entry(piece.toHypercube(SURFACE_DEPTH, offset), biome, untouched));
        }
        ResourceKey<Biome> cave = CustomOverworldBiomes.getSurfaceCaveBiome(biome);
        if (cave != null) {
            long caveOffset = Climate.quantizeCoord(Climate.unquantizeCoord(offset) + SURFACE_CAVE_OFFSET);
            for (Box piece : original) {
                out.add(new Entry(piece.toHypercube(SURFACE_CAVE_DEPTH, caveOffset), cave, "surface-cave"));
            }
        }
    }

    /**
     * Above freezing, the windswept biomes and old growth birch forests give way to what they break up: windswept hills
     * (and gravelly hills) to plains, windswept forests and old growth birch forests to forests, and windswept savannas to
     * plains below the savannas' temperature, to savannas from it.
     */
    @Nullable
    private static ResourceKey<Biome> temperateTarget(Box part, ResourceKey<Biome> biome) {
        if (biome == Biomes.WINDSWEPT_HILLS || biome == Biomes.WINDSWEPT_GRAVELLY_HILLS) return Biomes.PLAINS;
        if (biome == Biomes.WINDSWEPT_FOREST || biome == Biomes.OLD_GROWTH_BIRCH_FOREST) return Biomes.FOREST;
        if (biome == Biomes.WINDSWEPT_SAVANNA) return part.min[Box.T] >= WARM_BAND_START ? Biomes.SAVANNA : Biomes.PLAINS;
        return null;
    }

    /**
     * A windswept slice's part inside the zone (humidity, continentalness, below {@link #TEMPERATE_MIN}): the windswept biome that
     * fits: forest from humidity {@link #WINDSWEPT_FOREST_HUMIDITY}, below it hills, gravelly where it is colder than
     * {@link #WINDSWEPT_GRAVELLY_MAX_TEMPERATURE}.
     */
    @Nullable
    private static ResourceKey<Biome> windsweptZoneTarget(Box part, ResourceKey<Biome> biome) {
        if (!WINDSWEPT.contains(biome)) return null;
        if (part.min[Box.H] >= Climate.quantizeCoord(WINDSWEPT_FOREST_HUMIDITY)) return Biomes.WINDSWEPT_FOREST;
        return part.max[Box.T] <= Climate.quantizeCoord(WINDSWEPT_GRAVELLY_MAX_TEMPERATURE) ? Biomes.WINDSWEPT_GRAVELLY_HILLS : Biomes.WINDSWEPT_HILLS;
    }

    /**
     * A windswept slice's part anywhere else below {@link #TEMPERATE_MIN}: what lies beside it, vanilla's middle biome of
     * its temperature and humidity (snowy plains, snowy and plain taiga, plains, forest, old growth spruce taiga); from
     * -0.15 up the temperate rule's (hills and savannas to plains, forests to forest).
     */
    @Nullable
    private static ResourceKey<Biome> windsweptBesideTarget(Box part, ResourceKey<Biome> biome) {
        if (!WINDSWEPT.contains(biome)) return null;
        long humidity = part.min[Box.H];
        if (part.max[Box.T] <= Climate.quantizeCoord(-0.45F)) {
            return humidity < Climate.quantizeCoord(0.1F) ? Biomes.SNOWY_PLAINS : humidity < Climate.quantizeCoord(0.3F) ? Biomes.SNOWY_TAIGA : Biomes.TAIGA;
        }
        if (part.max[Box.T] <= Climate.quantizeCoord(-0.15F)) {
            return humidity < Climate.quantizeCoord(-0.1F) ? Biomes.PLAINS : humidity < Climate.quantizeCoord(0.1F) ? Biomes.FOREST
                    : humidity < Climate.quantizeCoord(0.3F) ? Biomes.TAIGA : Biomes.OLD_GROWTH_SPRUCE_TAIGA;
        }
        return temperateTarget(part, biome);
    }

    /** The taigas' warm edge: within {@link #TAIGA_EDGE_HALF_WIDTH} of temperature {@link #TAIGA_EDGE_TEMPERATURE}. */
    private static Box taigaEdge() {
        return Box.ALL.withMin(Box.T, Climate.quantizeCoord(TAIGA_EDGE_TEMPERATURE - TAIGA_EDGE_HALF_WIDTH))
                .withMax(Box.T, Climate.quantizeCoord(TAIGA_EDGE_TEMPERATURE + TAIGA_EDGE_HALF_WIDTH));
    }

    /** The rocky jungle's box before the 3D-noise terrain's climate is cut out of it: just warmer than the warm band, level. */
    private static Box rockyJungle() {
        return Box.ALL.withMin(Box.T, Climate.quantizeCoord(WARM_BAND_MAX)).withMax(Box.T, Climate.quantizeCoord(ROCKY_JUNGLE_MAX))
                .withMin(Box.E, Climate.quantizeCoord(ROCKY_EROSION_MIN));
    }

    @Nullable
    private static ResourceKey<Biome> rockyJungleTarget(Box slice, ResourceKey<Biome> biome) {
        return biome == Biomes.JUNGLE || biome == Biomes.SPARSE_JUNGLE || biome == Biomes.BAMBOO_JUNGLE ? CustomOverworldBiomes.ROCKY_JUNGLE : null;
    }

    /** Where vanilla's warm biomes (savannas, jungles) begin. */
    private static final long WARM_BAND_START = Climate.quantizeCoord(0.2F);

    /**
     * What the cool edge of a warm biome turns into, by what lies beyond it: savannas, next to plains, into the Tuscan
     * grove; the drier jungles, next to birch forests and forests, into the warm oak forest; the wettest, next to dark
     * forests, into the dark jungle forest.
     */
    @Nullable
    private static ResourceKey<Biome> warmTarget(Box slice, ResourceKey<Biome> biome) {
        if (biome == Biomes.SAVANNA || biome == Biomes.SAVANNA_PLATEAU) return CustomOverworldBiomes.TUSCAN_GROVE;
        boolean wet = slice.min[Box.H] >= Climate.quantizeCoord(WARM_HUMIDITY_SPLIT);
        if (biome == Biomes.JUNGLE || biome == Biomes.SPARSE_JUNGLE) return wet ? CustomOverworldBiomes.DARK_JUNGLE_FOREST : CustomOverworldBiomes.WARM_OAK_FOREST;
        if (biome == Biomes.BAMBOO_JUNGLE) return CustomOverworldBiomes.DARK_JUNGLE_FOREST;
        return null;
    }

    /**
     * What a biome becomes where the weirdness is positive and the temperature negative: birch forest turns dappled
     * (dappled taiga nearer the taigas, below {@link #DAPPLED_COLD}, else sparse dappled forest), old growth birch into
     * vanilla's dappled forest. (Dark forests no longer: their dark taiga forest is the taigas' transition, rule dark-taiga.)
     */
    @Nullable
    private static ResourceKey<Biome> weirdTarget(ResourceKey<Biome> biome, boolean cold) {
        if (biome == Biomes.BIRCH_FOREST) return cold ? CustomOverworldBiomes.DAPPLED_TAIGA : CustomOverworldBiomes.SPARSE_DAPPLED_FOREST;
        if (biome == Biomes.OLD_GROWTH_BIRCH_FOREST) return CustomOverworldBiomes.DAPPLED_FOREST;
        return null;
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
        Parameter coldHumidity = Parameter.span(-0.6F, -0.4F);
        out.add(new Entry(Climate.parameters(Parameter.span(-0.4F, 0.8F), lush.humidity(), lush.continentalness(),
                lush.erosion(), Parameter.span(0.15F, 0.5F), lush.weirdness(), offset + 0.01F), Biomes.LUSH_CAVES, "lush"));
        out.add(new Entry(Climate.parameters(Parameter.span(-0.3F, 0.8F), coldHumidity, lush.continentalness(),
                lush.erosion(), Parameter.span(0.2F, 0.5F), lush.weirdness(), offset + 0.01F), Biomes.LUSH_CAVES, "lush"));
        out.add(new Entry(Climate.parameters(Parameter.span(-1.0F, -0.8F), lush.humidity(), lush.continentalness(),
                lush.erosion(), Parameter.span(0.15F, 0.5F), lush.weirdness(), offset), Biomes.ICE_CAVES, "lush-ice"));
        out.add(new Entry(Climate.parameters(Parameter.span(-1.0F, -0.5F), coldHumidity, lush.continentalness(),
                Parameter.span(-1.0F, -0.6F), Parameter.span(0.2F, 0.5F), lush.weirdness(), offset),
                Biomes.ICE_CAVES, "lush-ice"));
    }

    private static void emitDripstoneReplacement(ParameterPoint dripstone, List<Entry> out) {
        float offset = Climate.unquantizeCoord(dripstone.offset());
        out.add(new Entry(Climate.parameters(Parameter.span(-0.6F, 1.0F), dripstone.humidity(), dripstone.continentalness(),
                dripstone.erosion(), DEEP_DRIPSTONE_DEPTH, dripstone.weirdness(), offset), Biomes.DRIPSTONE_CAVES, "dripstone"));
        out.add(new Entry(Climate.parameters(Parameter.span(-1.0F, -0.7F), dripstone.humidity(), dripstone.continentalness(),
                dripstone.erosion(), DEEP_DRIPSTONE_DEPTH, dripstone.weirdness(), offset), Biomes.ICE_CAVES, "dripstone-ice"));
    }

    /**
     * A cave entry kept off the mushroom islands' continentalness, so that under them the mushroom caves are nearest
     * rather than tied (the search tree keeps its last answer on a tie): an entry reaching into it starts just past its
     * end, one wholly inside it is dropped (null).
     */
    @Nullable
    private static Entry offTheIslands(Entry entry) {
        ParameterPoint p = entry.parameters();
        Parameter continentalness = p.continentalness();
        if (continentalness.min() > ISLANDS_END) return entry;
        if (continentalness.max() <= ISLANDS_END) return null;
        return new Entry(new ParameterPoint(p.temperature(), p.humidity(), new Parameter(ISLANDS_END + 1, continentalness.max()),
                p.erosion(), p.depth(), p.weirdness(), p.offset()), entry.biome(), entry.rule());
    }

    private static Parameter caveDepth(PlacedBiomeType type) {
        return switch (type) {
            case SURFACE_CAVE -> Parameter.span(0.1F, 0.25F);
            case CAVE -> Parameter.span(0.2F, 0.4F);
            case GENERIC_CAVE -> Parameter.span(0.25F, 0.65F);
            case ALL_DEPTHS -> Parameter.span(0.1F, 1.0F);
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
