package frootloops.versus.gametest;

import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.mobs.hostile.overworld.MushroomCreeperEntity;
import frootloops.versus.mod.mobs.hostile.overworld.MushroomSpiderEntity;
import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The mushroom caves' creepers and spiders turn the plain ones: a mushroom creeper's swelling ends in a spore puff that
 * turns the creepers near it and converts what else lives there, never an explosion; a mushroom spider's bite turns a
 * spider, unharmed.
 */
public class MushroomMobsGameTest {

    private static final String ROOM = "players-versus-gametest:leaves_room"; // 14 x 8 x 8

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 14; x++) for (int z = 0; z < 8; z++) helper.setBlock(x, 0, z, Blocks.STONE.defaultBlockState());
    }

    private static AABB room(GameTestHelper helper) {
        return AABB.encapsulatingFullBlocks(helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(13, 7, 7)));
    }

    /** A mushroom creeper set on a creeper swells, then puffs: the creeper turns, nothing breaks, and it lives on. */
    @GameTest(structure = ROOM, maxTicks = 80)
    public void aMushroomCreepersSporePuffTurnsTheCreepersNearIt(GameTestHelper helper) {
        floor(helper);
        MushroomCreeperEntity mushroom = helper.spawnWithNoFreeWill(ModEntities.MUSHROOM_CREEPER, new Vec3(4.5, 1.0, 3.5));
        Creeper near = helper.spawnWithNoFreeWill(EntityTypes.CREEPER, new Vec3(6.5, 1.0, 3.5));
        Creeper far = helper.spawnWithNoFreeWill(EntityTypes.CREEPER, new Vec3(12.5, 1.0, 3.5));
        mushroom.setTarget(near);
        mushroom.setSwellDir(1);
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(mushroom.isAlive(), "the mushroom creeper blew itself up");
            helper.assertTrue(near.isRemoved(), "the creeper beside it didn't turn");
            helper.assertTrue(!far.isRemoved(), "a creeper eight blocks away turned");
            List<MushroomCreeperEntity> mushrooms = helper.getLevel().getEntitiesOfClass(MushroomCreeperEntity.class, room(helper));
            helper.assertTrue(mushrooms.size() == 2, mushrooms.size() + " mushroom creepers, not two");
            helper.assertTrue(helper.getBlockState(new BlockPos(5, 0, 3)).is(Blocks.STONE), "the puff broke the floor");
            helper.assertTrue(mushroom.getSwellDir() < 0, "it's still swelling after its puff");
            helper.succeed();
        });
    }

    /** Lit, a mushroom creeper still only puffs: the floor stays, it lives, a pig beside it converts, and it's lit no more. */
    @GameTest(structure = ROOM, maxTicks = 80)
    public void aLitMushroomCreeperPuffsConversion(GameTestHelper helper) {
        floor(helper);
        MushroomCreeperEntity mushroom = helper.spawnWithNoFreeWill(ModEntities.MUSHROOM_CREEPER, new Vec3(4.5, 1.0, 3.5));
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(6.0, 1.0, 3.5));
        mushroom.ignite();
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(mushroom.isAlive(), "the lit mushroom creeper blew itself up");
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 0, 3)).is(Blocks.STONE), "the puff broke the floor");
            helper.assertTrue(pig.hasEffect(CustomStatusEffects.CONVERSION), "the pig beside the puff didn't convert");
            helper.assertTrue(!mushroom.isIgnited(), "it's still lit after its puff");
            helper.succeed();
        });
    }

    /** A mushroom spider's bite turns a plain spider into one of its own, unharmed. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aMushroomSpidersBiteTurnsASpider(GameTestHelper helper) {
        floor(helper);
        MushroomSpiderEntity mushroom = helper.spawnWithNoFreeWill(ModEntities.MUSHROOM_SPIDER, new Vec3(3.5, 1.0, 3.5));
        Spider spider = helper.spawnWithNoFreeWill(EntityTypes.SPIDER, new Vec3(5.5, 1.0, 3.5));
        helper.assertTrue(mushroom.doHurtTarget(helper.getLevel(), spider), "the bite missed");
        helper.assertTrue(spider.isRemoved(), "the bitten spider didn't turn");
        List<MushroomSpiderEntity> mushrooms = helper.getLevel().getEntitiesOfClass(MushroomSpiderEntity.class, room(helper));
        helper.assertTrue(mushrooms.size() == 2, mushrooms.size() + " mushroom spiders, not two");
        for (MushroomSpiderEntity turned : mushrooms) {
            helper.assertTrue(turned.getHealth() == turned.getMaxHealth(), "a mushroom spider was hurt: " + turned.getHealth());
        }
        helper.succeed();
    }
}
