package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.warden.Warden;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** The spider, creaking and warden mixins apply on 26.3: their handlers are in the game's classes. */
class ScarierMobsTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    @Test
    void spidersPathAlongSurfacesAndKeepTheirGrip() {
        assertTrue(hasHandler(Spider.class, "playersVersus$pathAlongSurfaces"), "spiders path as walkers");
        assertTrue(hasHandler(Spider.class, "playersVersus$crawl"), "spiders don't crawl");
        assertTrue(hasHandler(Spider.class, "playersVersus$keepGrip"), "spiders don't track their surface");
        assertTrue(hasHandler(Spider.class, "playersVersus$defineGripData"), "spiders don't sync their surface");
    }

    @Test
    void creakingsTeleportWhenStuckAndGambleOnHits() {
        assertTrue(hasHandler(Creaking.class, "playersVersus$teleportWhenStuck"), "creakings stay stuck");
        assertTrue(hasHandler(Creaking.class, "playersVersus$hitIsAGamble"), "hits on creakings are no gamble");
    }

    @Test
    void wardensGuessSearchAndLeave() {
        assertTrue(hasHandler(Warden.class, "playersVersus$huntByGuesswork"), "wardens chase where you are");
        assertTrue(hasHandler(Warden.class, "playersVersus$searchForTheNearestPlayer"), "wardens don't come to far players");
        assertTrue(hasHandler(Warden.class, "playersVersus$goFindTheNearestPlayer"), "wardens don't look for far players when they rise");
    }

    private static boolean hasHandler(Class<?> target, String name) {
        return Arrays.stream(target.getDeclaredMethods()).map(Method::getName).anyMatch(method -> method.endsWith(name));
    }
}
