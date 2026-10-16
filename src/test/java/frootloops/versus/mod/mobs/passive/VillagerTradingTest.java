package frootloops.versus.mod.mobs.passive;

import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The villager and wandering trader mixins apply on 26.3, shadows included: a vanilla field or method they name that
 * 26.3 lacks (the villager's last trading player, its special prices) would fail loading the class.
 */
class VillagerTradingTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    @Test
    void villagersGossipRestockAndLevelUp() {
        assertTrue(hasHandler(Villager.class, "talkWithVillager"), "gossip doesn't restock or level villagers up");
        assertTrue(hasHandler(Villager.class, "restockTimeWithoutGossiping"), "villagers restock as often as vanilla's");
    }

    @Test
    void wanderingTradersSettleDown() {
        // Vanilla's trader leaves notifyTrade to AbstractVillager: the one in its class is the mixin's, which settles it.
        assertTrue(hasHandler(WanderingTrader.class, "notifyTrade"), "wandering traders never settle down");
    }

    private static boolean hasHandler(Class<?> target, String name) {
        return Arrays.stream(target.getDeclaredMethods()).map(Method::getName).anyMatch(method -> method.endsWith(name));
    }
}
