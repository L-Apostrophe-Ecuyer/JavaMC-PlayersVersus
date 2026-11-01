package frootloops.versus.gametest;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.mobs.hostile.end.WatchlingEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A watchling left alone haunts; hit (or shot at) it stops and fights like an enderman, against whoever did it, and
 * haunts again once it has had no one to fight for a while.
 */
public class WatchlingGameTest {
    private static final String ROOM = "players-versus-gametest:leaves_room"; // 14 x 8 x 8

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 14; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
    }

    /** Nobody has touched it: it doesn't fight. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aWatchlingLeftAloneDoesNotFight(GameTestHelper helper) {
        floor(helper);
        WatchlingEntity watchling = helper.spawnWithNoFreeWill(ModEntities.WATCHLING, new Vec3(6.5, 1.0, 3.5));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(!watchling.isEnraged(), "it fights though nobody touched it");
            helper.assertTrue(watchling.getTarget() == null, "it has a target though nobody touched it");
            helper.succeed();
        });
    }

    /** A player's blow makes it fight that player instead of haunting. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aWatchlingHitByAPlayerFightsThemInsteadOfHaunting(GameTestHelper helper) {
        floor(helper);
        WatchlingEntity watchling = helper.spawnWithNoFreeWill(ModEntities.WATCHLING, new Vec3(6.5, 1.0, 3.5));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(watchling.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 1.0F), "the blow didn't land");
        helper.assertTrue(watchling.isEnraged(), "it doesn't fight after being hit");
        helper.assertTrue(!watchling.isHunting(), "it still haunts while it fights");
        helper.assertTrue(watchling.getTarget() == player, "it fights someone else than who hit it");
        helper.succeed();
    }

    /** A mob's blow does too: whoever hit it is whom it fights. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aWatchlingHitByAMobFightsIt(GameTestHelper helper) {
        floor(helper);
        WatchlingEntity watchling = helper.spawnWithNoFreeWill(ModEntities.WATCHLING, new Vec3(6.5, 1.0, 3.5));
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(4.5, 1.0, 3.5));
        watchling.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(pig), 1.0F);
        helper.assertTrue(watchling.isEnraged() && watchling.getTarget() == pig, "it doesn't fight the pig that hit it");
        helper.succeed();
    }

    /** With no one left to fight for CALM_TICKS, it calms and haunts again. */
    @GameTest(structure = ROOM, maxTicks = 400)
    public void aWatchlingWithNoOneLeftToFightCalmsAndHauntsAgain(GameTestHelper helper) {
        floor(helper);
        WatchlingEntity watchling = helper.spawnWithNoFreeWill(ModEntities.WATCHLING, new Vec3(6.5, 1.0, 3.5));
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(4.5, 1.0, 3.5));
        watchling.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(pig), 1.0F);
        helper.assertTrue(watchling.isEnraged(), "it doesn't fight after being hit");
        pig.discard();
        helper.runAfterDelay(VersusSettings.Watchlings.CALM_TICKS - 20, () ->
                helper.assertTrue(watchling.isEnraged(), "it calmed before its time"));
        helper.succeedWhen(() -> {
            helper.assertTrue(!watchling.isEnraged(), "it still fights though there's no one left");
            helper.assertTrue(watchling.isHunting(), "it doesn't haunt again once calm");
            helper.assertTrue(watchling.getTarget() == null, "it keeps a target once calm");
        });
    }
}
