package frootloops.versus.mod.environment.worldgen.density;

import frootloops.versus.VersusMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

import java.util.Arrays;

/**
 * Whether C2ME's density-function compiler is at work in this game.
 *
 * <p>The compiler turns vanilla's density-function types into bytecode and runs any other type through vanilla's
 * interface, one position at a time, with a new position object for each call. That costs little where a function
 * runs once per cell corner ({@link PvTerrain}, about a thousand corners per chunk), but the final density runs at
 * every block. So with the compiler, {@link PvFinalDensity} rebuilds itself from vanilla types
 * ({@link PvFinalDensity#asVanillaTypes}), which it compiles, all but the height bias in the flooded corridors' layers
 * ({@link PvCorridorBias}), kept Java on purpose; without it, the Java kernel is faster. Both give the same doubles
 * ({@code TerrainPortTest}).
 *
 * <p>{@code -Dpv.worldgen.vanillaTypes=false} keeps the Java kernel under the compiler too: slower, but it doesn't
 * depend on how the compiler treats vanilla's types.
 */
public final class DensityCompilerCompat {

    /** C2ME's compiler module. Its config can turn the compiler off, which keeps the module loaded but drops its mixins. */
    private static final String MODULE_ID = "c2me-opts-dfc";
    /** An interface the compiler's mixins add to vanilla's cache markers ({@code minecraft:interpolated} and the others). */
    private static final String MIXED_IN_INTERFACE = "com.ishland.c2me.opts.dfc.common.ducks.IFastCacheLike";
    /** Set to {@code false} to keep the Java kernel when the compiler is active. */
    private static final String VANILLA_TYPES_PROPERTY = "pv.worldgen.vanillaTypes";

    /** Whether the final density is built from vanilla types. Read once, on first use: when a world's noise router is first built. */
    public static final boolean ACTIVE = detect();

    private DensityCompilerCompat() {
    }

    private static boolean detect() {
        if (!FabricLoader.getInstance().isModLoaded(MODULE_ID)) return false;
        // the marker's class isn't public, so take it from an instance
        Class<?> marker = DensityFunctionTypes.interpolated(DensityFunctionTypes.constant(0.0)).getClass();
        boolean compiler = Arrays.stream(marker.getInterfaces()).anyMatch(type -> type.getName().equals(MIXED_IN_INTERFACE));
        if (!compiler) {
            VersusMod.MOD_LOGGER.info("C2ME is loaded, but its density-function compiler isn't active: the Players Versus final density stays a Java kernel");
            return false;
        }
        if ("false".equals(System.getProperty(VANILLA_TYPES_PROPERTY))) {
            VersusMod.MOD_LOGGER.info("C2ME's density-function compiler is active, but -D{}=false keeps the Players Versus final density a Java kernel",
                    VANILLA_TYPES_PROPERTY);
            return false;
        }
        VersusMod.MOD_LOGGER.info("C2ME's density-function compiler is active: the Players Versus final density is built from vanilla types, which it compiles");
        return true;
    }
}
