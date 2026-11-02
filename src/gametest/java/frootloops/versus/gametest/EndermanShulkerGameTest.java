package frootloops.versus.gametest;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.mobs.hostile.end.EndermanShulkerCarrier;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Now and then an enderman carries a shulker in its arms (EndermanShulkerCarrier): it holds it at its chest, inside its
 * widened box, and goes where it means to; the shulker can't teleport out of its arms, and its bullets pass through it.
 */
public class EndermanShulkerGameTest {
    private static final String ROOM = "players-versus-gametest:leaves_room"; // 14 x 8 x 8

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 14; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
    }

    /** A shulker in the arms of the enderman standing at {@code at}, both standing still. */
    private static Shulker carried(GameTestHelper helper, Enderman enderman, Vec3 at) {
        Shulker shulker = helper.spawnWithNoFreeWill(EntityTypes.SHULKER, at);
        helper.assertTrue(shulker.startRiding(enderman, true, false), "the shulker didn't go into its arms");
        return shulker;
    }

    /** It holds the shulker at its chest, inside its box (widened to the shulker's, near enough), and steers itself. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void anEndermanHoldsItsShulkerInItsArms(GameTestHelper helper) {
        floor(helper);
        Vec3 at = new Vec3(6.5, 1.0, 3.5);
        Enderman enderman = helper.spawnWithNoFreeWill(EntityTypes.ENDERMAN, at);
        Shulker shulker = carried(helper, enderman, at);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(EndermanShulkerCarrier.carries(enderman), "the enderman carries nothing");
            helper.assertTrue(Math.abs(enderman.getBbWidth() - VersusSettings.Endermen.SHULKER_CARRIER_WIDTH) < 1.0E-4F, "the enderman is " + enderman.getBbWidth() + " wide, not as wide as its shulker");
            helper.assertTrue(enderman.getControllingPassenger() == null, "the shulker steers the enderman");
            double held = shulker.getY() - enderman.getY();
            helper.assertTrue(Math.abs(held - VersusSettings.Endermen.SHULKER_HELD_AT) < 0.01, "the shulker is held " + held + " up, not at its chest");
            AABB room = enderman.getBoundingBox().inflate(0.011);
            AABB box = shulker.getBoundingBox();
            helper.assertTrue(box.minX >= room.minX && box.maxX <= room.maxX && box.minZ >= room.minZ && box.maxZ <= room.maxZ
                    && box.minY >= room.minY && box.maxY <= room.maxY, "the shulker sticks out of the enderman's box: " + box + " in " + enderman.getBoundingBox());
            helper.succeed();
        });
    }

    /** Hurt low, a shulker in an enderman's arms doesn't teleport out of them. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aCarriedShulkerCantTeleportAway(GameTestHelper helper) {
        floor(helper);
        Vec3 at = new Vec3(6.5, 1.0, 3.5);
        Enderman enderman = helper.spawnWithNoFreeWill(EntityTypes.ENDERMAN, at);
        Shulker shulker = carried(helper, enderman, at);
        shulker.setHealth(shulker.getMaxHealth() * 0.4F);
        // A shulker below half health teleports one hit in four: forty hits, and it would all but surely have gone
        for (int i = 0; i < 40; i++) {
            shulker.invulnerableTime = 0;
            shulker.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 0.01F);
        }
        helper.assertTrue(shulker.isAlive() && shulker.getVehicle() == enderman, "the shulker left the enderman's arms");
        helper.succeed();
    }

    /** Its shulker's bullets pass through the enderman carrying it (which would otherwise rise, and teleport away). */
    @GameTest(structure = ROOM, maxTicks = 200)
    public void itsShulkersBulletsPassThroughIt(GameTestHelper helper) {
        floor(helper);
        Vec3 at = new Vec3(6.5, 1.0, 3.5);
        Enderman enderman = helper.spawnWithNoFreeWill(EntityTypes.ENDERMAN, at);
        Shulker shulker = carried(helper, enderman, at);
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(11.5, 1.0, 3.5));
        Vec3 standing = enderman.position();
        // A bullet of its shulker's, fired from the far side, at the pig beyond the enderman: low, under the shulker
        ShulkerBullet bullet = new ShulkerBullet(helper.getLevel(), shulker, pig, Direction.Axis.Y);
        Vec3 from = helper.absoluteVec(new Vec3(2.5, 1.3, 3.5));
        bullet.snapTo(from.x, from.y, from.z, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(bullet);
        helper.succeedWhen(() -> {
            helper.assertTrue(!enderman.hasEffect(MobEffects.LEVITATION) && enderman.getHealth() == enderman.getMaxHealth()
                    && enderman.position().distanceTo(standing) < 0.01, "its shulker's bullet hit the enderman");
            helper.assertTrue(pig.hasEffect(MobEffects.LEVITATION), "the bullet hasn't reached the pig beyond yet");
        });
    }
}
