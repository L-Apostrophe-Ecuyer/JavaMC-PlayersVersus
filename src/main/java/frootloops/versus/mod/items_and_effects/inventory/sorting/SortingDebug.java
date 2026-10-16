package frootloops.versus.mod.items_and_effects.inventory.sorting;

import frootloops.versus.VersusMod;

import java.util.function.Supplier;

/**
 * Debug logging for the inventory sort, off unless the game runs with {@code -Dpv.sorting.debug=true}.
 */
public final class SortingDebug {
    public static final boolean ENABLED = Boolean.getBoolean("pv.sorting.debug");

    private SortingDebug() {
    }

    public static void log(Supplier<String> message) {
        if (ENABLED) VersusMod.MOD_LOGGER.info(message.get());
    }
}
