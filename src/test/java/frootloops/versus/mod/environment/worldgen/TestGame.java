package frootloops.versus.mod.environment.worldgen;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;

/**
 * Starts the parts of the game that tests need: the game version, vanilla's bootstrap, then this mod's worldgen types
 * (the registries don't exist before the bootstrap). Tests share one JVM, so every test class calls this instead of
 * bootstrapping on its own; otherwise whichever class ran first would decide whether the mod's types exist.
 */
public final class TestGame {

    private static boolean started;
    private static IllegalStateException failure;

    private TestGame() {
    }

    public static synchronized void start() {
        if (failure != null) throw failure;
        if (started) return;
        try {
            SharedConstants.createGameVersion();
            Bootstrap.initialize();
            PvWorldgen.initialize();
            started = true;
        } catch (RuntimeException | Error exception) {
            failure = new IllegalStateException("the test game failed to start", exception);
            throw failure;
        }
    }
}
