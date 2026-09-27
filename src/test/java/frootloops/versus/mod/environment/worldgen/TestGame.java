package frootloops.versus.mod.environment.worldgen;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;

/**
 * Starts the parts of the game that tests need, in the game's order: the game version, this mod's worldgen types,
 * then vanilla's bootstrap, which freezes the static registries. Tests share one JVM, so every test class calls this
 * instead of bootstrapping on its own; otherwise whichever class ran first would decide whether the mod's types
 * exist.
 */
public final class TestGame {

    private static boolean started;

    private TestGame() {
    }

    public static synchronized void start() {
        if (started) return;
        started = true;
        SharedConstants.createGameVersion();
        PvWorldgen.initialize();
        Bootstrap.initialize();
    }
}
