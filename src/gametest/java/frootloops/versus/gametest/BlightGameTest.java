package frootloops.versus.gametest;

import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Blight's doses: every way of giving one stacks on the last. */
public class BlightGameTest {
    private static final String ROOM = "players-versus-gametest:leaves_room"; // 14 x 8 x 8

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 14; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
    }

    /** A lingering cloud dosing whoever stands in it every second adds to what they had: white hearts pile up. */
    @GameTest(structure = ROOM, maxTicks = 300)
    public void aCloudOfItStacksItsDoses(GameTestHelper helper) {
        floor(helper);
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        Vec3 at = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
        AreaEffectCloud cloud = new AreaEffectCloud(helper.getLevel(), at.x, at.y, at.z);
        cloud.setRadius(3.0F);
        cloud.setWaitTime(0);
        cloud.setDuration(400);
        cloud.addEffect(new MobEffectInstance(CustomStatusEffects.CONVERSION, 2400, 0));
        helper.getLevel().addFreshEntity(cloud);
        helper.succeedWhen(() -> {
            MobEffectInstance had = pig.getEffect(CustomStatusEffects.CONVERSION);
            helper.assertTrue(had != null && had.getAmplifier() >= 2, "the pig in the cloud has amplifier " + (had == null ? "none" : had.getAmplifier()) + ", not stacking");
        });
    }
}
