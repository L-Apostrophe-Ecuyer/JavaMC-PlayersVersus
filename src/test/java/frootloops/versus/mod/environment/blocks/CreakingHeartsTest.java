package frootloops.versus.mod.environment.blocks;

import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.entity.CreakingHeartBlockEntity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreakingHeartsTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    /** A heart is in a cave with no sky light 4, 8 and 12 blocks up; only those heights count. */
    @Test
    void aCaveHasNoSkyLight4To12BlocksUp() {
        assertTrue(CreakingHearts.noSkyLightAbove(up -> 0));
        assertTrue(CreakingHearts.noSkyLightAbove(up -> up > 12 ? 15 : 0));
        assertTrue(CreakingHearts.noSkyLightAbove(up -> up == 6 ? 15 : 0));
        assertFalse(CreakingHearts.noSkyLightAbove(up -> up == 4 ? 15 : 0));
        assertFalse(CreakingHearts.noSkyLightAbove(up -> up == 8 ? 1 : 0));
        assertFalse(CreakingHearts.noSkyLightAbove(up -> up == 12 ? 1 : 0));
    }

    /**
     * The mixins on the heart's block and block entity apply on 26.3: their handlers are in the classes. (A target
     * missing from 26.3 would already have failed loading the classes, as the mixins are required.)
     */
    @Test
    void theHeartMixinsApply() {
        assertTrue(hasHandler(CreakingHeartBlockEntity.class, "playersVersus$activeInCaves"), "block entity: waking and keeping the creaking");
        assertTrue(hasHandler(CreakingHeartBlock.class, "playersVersus$activeInCaves"), "block: the state when placed");
        assertTrue(hasHandler(CreakingHeartBlock.class, "playersVersus$idleInCaves"), "block: the idle sound");
    }

    private static boolean hasHandler(Class<?> target, String name) {
        return Arrays.stream(target.getDeclaredMethods()).map(Method::getName).anyMatch(method -> method.endsWith(name));
    }
}
