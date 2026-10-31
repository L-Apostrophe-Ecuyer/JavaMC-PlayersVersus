package frootloops.versus.gametest;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.blocks.mushrooms.StagedMushroomBlock;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.items_and_effects.conversion.Conversion;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Small mushrooms grow by stages and below y 0 turn pale; pale shrooms convert what stays in them, Conversion stacks,
 * keeps food from regenerating health into its white hearts, and all white, shroomifies.
 */
public class StagedMushroomsGameTest {

    private static final String ROOM = "players-versus-gametest:leaves_room"; // 14 x 8 x 8

    private static void floor(GameTestHelper helper, BlockState ground) {
        for (int x = 0; x < 14; x++) for (int z = 0; z < 8; z++) helper.setBlock(x, 0, z, ground);
    }

    private static AABB room(GameTestHelper helper) {
        return AABB.encapsulatingFullBlocks(helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(13, 7, 7)));
    }

    /** A random tick of the block at this spot, every chance taken. */
    private static void randomTick(GameTestHelper helper, BlockPos pos) {
        randomTick(helper, pos, RandomSource.create(7));
    }

    private static void randomTick(GameTestHelper helper, BlockPos pos, RandomSource random) {
        float chance = VersusSettings.Mushrooms.GROW_CHANCE;
        VersusSettings.Mushrooms.GROW_CHANCE = 1.0F;
        try {
            helper.getBlockState(pos).randomTick(helper.getLevel(), helper.absolutePos(pos), random);
        } finally {
            VersusSettings.Mushrooms.GROW_CHANCE = chance;
        }
    }

    private static boolean belowZero(GameTestHelper helper, BlockPos pos) {
        return helper.absolutePos(pos).getY() < 0;
    }

    private static BlockState staged(StagedMushroomBlock block, int mushrooms) {
        return block.defaultBlockState().setValue(StagedMushroomBlock.MUSHROOMS, mushrooms);
    }

    /** On mycelium, beside another, a brown mushroom grows to two; below y 0, to two pale shrooms instead. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aMushroomBesideAnotherOnMyceliumGrows(GameTestHelper helper) {
        floor(helper, Blocks.MYCELIUM.defaultBlockState());
        BlockPos mushroom = new BlockPos(3, 1, 3);
        helper.setBlock(mushroom, Blocks.BROWN_MUSHROOM);
        helper.setBlock(new BlockPos(4, 1, 3), Blocks.BROWN_MUSHROOM);
        randomTick(helper, mushroom);
        BlockState grown = helper.getBlockState(mushroom);
        BlockState expected = staged(belowZero(helper, mushroom) ? CustomBlocks.PALE_SHROOM : CustomBlocks.BROWN_MUSHROOMS, 2);
        helper.assertTrue(grown.equals(expected), "the mushroom became " + grown + ", not " + expected);
        helper.succeed();
    }

    /** Alone, two mushrooms shrink back to one (vanilla's), as they can't grow there. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void twoMushroomsAloneShrinkToOne(GameTestHelper helper) {
        floor(helper, Blocks.STONE.defaultBlockState());
        BlockPos mushrooms = new BlockPos(3, 1, 3);
        helper.setBlock(mushrooms, staged(CustomBlocks.BROWN_MUSHROOMS, 2));
        randomTick(helper, mushrooms);
        helper.assertBlockPresent(Blocks.BROWN_MUSHROOM, mushrooms);
        helper.succeed();
    }

    /** Above y 0, on mycelium beside two grown ones, three spread a mushroom beside them. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void threeMushroomsSpread(GameTestHelper helper) {
        floor(helper, Blocks.MYCELIUM.defaultBlockState());
        BlockPos three = new BlockPos(6, 1, 4);
        if (belowZero(helper, three)) {
            helper.succeed();
            return;
        }
        helper.setBlock(three, staged(CustomBlocks.BROWN_MUSHROOMS, 3));
        helper.setBlock(new BlockPos(7, 1, 4), staged(CustomBlocks.BROWN_MUSHROOMS, 2));
        int before = count(helper);
        RandomSource random = RandomSource.create(11);
        for (int tick = 0; tick < 40 && count(helper) == before; tick++) {
            randomTick(helper, three, random);
            helper.assertTrue(helper.getBlockState(three).equals(staged(CustomBlocks.BROWN_MUSHROOMS, 3)), "the three changed: " + helper.getBlockState(three));
        }
        helper.assertTrue(count(helper) == before + 1, "the three didn't spread a mushroom in 40 ticks");
        helper.succeed();
    }

    private static int count(GameTestHelper helper) {
        int mushrooms = 0;
        for (BlockPos pos : BlockPos.betweenClosed(0, 1, 0, 13, 2, 7)) {
            if (helper.getBlockState(pos).is(Blocks.BROWN_MUSHROOM) || helper.getBlockState(pos).is(CustomBlocks.BROWN_MUSHROOMS)) mushrooms++;
        }
        return mushrooms;
    }

    /** Two mushrooms give off light 2, three light 4; their loot is as many mushrooms. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void grownMushroomsGiveOffLight(GameTestHelper helper) {
        helper.assertTrue(staged(CustomBlocks.BROWN_MUSHROOMS, 2).getLightEmission() == 2, "two brown mushrooms don't give off light 2");
        helper.assertTrue(staged(CustomBlocks.RED_MUSHROOMS, 3).getLightEmission() == 4, "three red mushrooms don't give off light 4");
        helper.assertTrue(staged(CustomBlocks.PALE_SHROOM, 3).getLightEmission() == 4, "three pale shrooms don't give off light 4");
        helper.succeed();
    }

    /** Each dose adds its amplifier + 1 to what was had; the undead never take it. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void conversionStacksAndSparesTheUndead(GameTestHelper helper) {
        floor(helper, Blocks.STONE.defaultBlockState());
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        pig.addEffect(new MobEffectInstance(CustomStatusEffects.CONVERSION, 2400, 0));
        helper.assertTrue(pig.getEffect(CustomStatusEffects.CONVERSION).getAmplifier() == 0, "a first dose isn't amplifier 0");
        pig.addEffect(new MobEffectInstance(CustomStatusEffects.CONVERSION, 2400, 0));
        helper.assertTrue(pig.getEffect(CustomStatusEffects.CONVERSION).getAmplifier() == 1, "a second dose didn't stack to 1");
        pig.addEffect(new MobEffectInstance(CustomStatusEffects.CONVERSION, 2400, 1));
        helper.assertTrue(pig.getEffect(CustomStatusEffects.CONVERSION).getAmplifier() == 3, "a dose of 1 on 1 didn't stack to 3");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(6.5, 1.0, 3.5));
        helper.assertTrue(!zombie.addEffect(new MobEffectInstance(CustomStatusEffects.CONVERSION, 2400, 0)), "a zombie took Conversion");
        helper.succeed();
    }

    /** Staying in pale shrooms gives a dose. */
    @GameTest(structure = ROOM, maxTicks = 200)
    public void staysInPaleShroomsConvert(GameTestHelper helper) {
        floor(helper, Blocks.STONE.defaultBlockState());
        helper.setBlock(new BlockPos(3, 1, 3), staged(CustomBlocks.PALE_SHROOM, 3));
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        helper.succeedWhen(() -> helper.assertTrue(pig.hasEffect(CustomStatusEffects.CONVERSION), "the pig in the pale shrooms isn't converting"));
    }

    /** Conversion enough to whiten all the entity's hearts (the mod changes some mobs' health: pigs have 16). */
    private static MobEffectInstance allWhite(LivingEntity entity) {
        return new MobEffectInstance(CustomStatusEffects.CONVERSION, 2400, Conversion.hearts(entity.getMaxHealth()) - 1);
    }

    /** All white, a villager shakes, then turns into a zombie villager. */
    @GameTest(structure = ROOM, maxTicks = 200)
    public void anAllWhiteVillagerTurnsZombie(GameTestHelper helper) {
        floor(helper, Blocks.STONE.defaultBlockState());
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(3.5, 1.0, 3.5));
        villager.addEffect(allWhite(villager));
        helper.runAfterDelay(VersusSettings.Mushrooms.SHROOMIFY_TICKS - 20, () ->
                helper.assertTrue(!villager.isRemoved(), "the villager turned before it was done shaking"));
        helper.succeedWhen(() -> {
            helper.assertTrue(villager.isRemoved(), "the villager is still a villager");
            List<ZombieVillager> zombies = helper.getLevel().getEntitiesOfClass(ZombieVillager.class, room(helper));
            helper.assertTrue(zombies.size() == 1, zombies.size() + " zombie villagers, not one");
            helper.assertTrue(!zombies.getFirst().hasEffect(CustomStatusEffects.CONVERSION), "the zombie villager carries Conversion");
        });
    }

    /** All white, a pig (with no zombie of its own) dies of it. */
    @GameTest(structure = ROOM, maxTicks = 200)
    public void anAllWhitePigDies(GameTestHelper helper) {
        floor(helper, Blocks.STONE.defaultBlockState());
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        pig.addEffect(allWhite(pig));
        helper.succeedWhen(() -> helper.assertTrue(pig.isDeadOrDying() || pig.isRemoved(), "the all-white pig lives"));
    }

    /** Food regenerates a player's health up to their white hearts, and no further. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void foodDoesntRegenerateWhiteHearts(GameTestHelper helper) {
        ServerPlayer player = FakePlayer.get(helper.getLevel());
        try {
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0F);
            player.setHealth(6.0F);
            // Five white hearts of ten: food stops at 10 health
            player.addEffect(new MobEffectInstance(CustomStatusEffects.CONVERSION, 2400, 4));
            for (int tick = 0; tick < 600; tick++) player.getFoodData().tick(player);
            helper.assertTrue(player.getHealth() > 6.0F, "food didn't regenerate the player at all: " + player.getHealth());
            helper.assertTrue(player.getHealth() <= 10.0F, "food regenerated the player into their white hearts: " + player.getHealth());
        } finally {
            player.removeEffect(CustomStatusEffects.CONVERSION);
            player.setHealth(player.getMaxHealth());
        }
        helper.succeed();
    }
}
