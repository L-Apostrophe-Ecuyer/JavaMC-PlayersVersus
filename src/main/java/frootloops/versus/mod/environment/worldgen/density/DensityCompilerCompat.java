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
 * ({@link PvFinalDensity#asVanillaTypes}), which it compiles; without it, the Java kernel is faster. Both give the
 * same doubles ({@code TerrainPortTest}).
 */
public final class DensityCompilerCompat {

    /** C2ME's compiler module. Its config can turn the compiler off, which keeps the module loaded but drops its mixins. */
    private static final String MODULE_ID = "c2me-opts-dfc";
    /** An interface the compiler's mixins add to vanilla's cache markers ({@code minecraft:interpolated} and the others). */
    private static final String MIXED_IN_INTERFACE = "com.ishland.c2me.opts.dfc.common.ducks.IFastCacheLike";

    /** Read once, on first use: when a world's noise router is first built. */
    public static final boolean ACTIVE = detect();

    private DensityCompilerCompat() {
    }

    private static boolean detect() {
        if (!FabricLoader.getInstance().isModLoaded(MODULE_ID)) return false;
        // the marker's class isn't public, so take it from an instance
        Class<?> marker = DensityFunctionTypes.interpolated(DensityFunctionTypes.constant(0.0)).getClass();
        boolean active = Arrays.stream(marker.getInterfaces()).anyMatch(type -> type.getName().equals(MIXED_IN_INTERFACE));
        VersusMod.MOD_LOGGER.info(active
                ? "C2ME's density-function compiler is active: the Players Versus final density is built from vanilla types, which it compiles"
                : "C2ME is loaded, but its density-function compiler isn't active: the Players Versus final density stays a Java kernel");
        return active;
    }
}
