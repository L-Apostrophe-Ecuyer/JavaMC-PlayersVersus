package frootloops.versus.gametest;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.PaleLine;
import frootloops.versus.mod.environment.blocks.mushrooms.StagedMushroomBlock;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.items_and_effects.conversion.Conversion;
import frootloops.versus.mod.items_and_effects.conversion.ConversionAttacks;
import frootloops.versus.mod.items_and_effects.conversion.Undeath;
import frootloops.versus.mod.mobs.hostile.overworld.PaleCreeperEntity;
import frootloops.versus.mod.mobs.hostile.overworld.PaleZombieEntity;
import frootloops.versus.mod.mobs.ModEntities;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
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
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Small mushrooms grow by stages and below y 0 turn pale; pale shrooms convert what stays in them, and so do pale
 * zombies', creakings' and (rarely) zombies' hits and rotten flesh; Conversion stacks, keeps food from regenerating
 * health into its white hearts, and all white, turns undead.
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

    private static boolean palePlace(GameTestHelper helper, BlockPos pos) {
        return PaleLine.isPale(helper.getLevel(), helper.absolutePos(pos));
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
        BlockState expected = staged(palePlace(helper, mushroom) ? CustomBlocks.PALE_SHROOM : CustomBlocks.BROWN_MUSHROOMS, 2);
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
        if (palePlace(helper, three)) {
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

    /** Each dose adds its amplifier + 1 to what was had; skeletons never take it. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void conversionStacksAndSparesSkeletons(GameTestHelper helper) {
        floor(helper, Blocks.STONE.defaultBlockState());
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        pig.addEffect(new MobEffectInstance(CustomStatusEffects.CONVERSION, 2400, 0));
        helper.assertTrue(pig.getEffect(CustomStatusEffects.CONVERSION).getAmplifier() == 0, "a first dose isn't amplifier 0");
        pig.addEffect(new MobEffectInstance(CustomStatusEffects.CONVERSION, 2400, 0));
        helper.assertTrue(pig.getEffect(CustomStatusEffects.CONVERSION).getAmplifier() == 1, "a second dose didn't stack to 1");
        pig.addEffect(new MobEffectInstance(CustomStatusEffects.CONVERSION, 2400, 1));
        helper.assertTrue(pig.getEffect(CustomStatusEffects.CONVERSION).getAmplifier() == 3, "a dose of 1 on 1 didn't stack to 3");
        // Skeletons have no flesh to convert (zombies do: they turn pale)
        Mob skeleton = helper.spawnWithNoFreeWill(EntityTypes.SKELETON, new Vec3(6.5, 1.0, 3.5));
        helper.assertTrue(!skeleton.addEffect(new MobEffectInstance(CustomStatusEffects.CONVERSION, 2400, 0)), "a skeleton took Conversion");
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

    /** All white, a villager shakes before it turns (what it turns into: UndeadFormsGameTest). */
    @GameTest(structure = ROOM, maxTicks = 200)
    public void anAllWhiteVillagerShakesThenTurns(GameTestHelper helper) {
        floor(helper, Blocks.STONE.defaultBlockState());
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(3.5, 1.0, 3.5));
        villager.addEffect(allWhite(villager));
        helper.runAfterDelay(VersusSettings.Mushrooms.TURNING_TICKS - 20, () ->
                helper.assertTrue(!villager.isRemoved(), "the villager turned before it was done shaking"));
        helper.succeedWhen(() -> {
            helper.assertTrue(villager.isRemoved(), "the villager is still a villager");
            List<Mob> turned = helper.getLevel().getEntitiesOfClass(Mob.class, room(helper));
            helper.assertTrue(turned.size() == 1, turned.size() + " mobs came of the villager, not one");
            helper.assertTrue(!turned.getFirst().hasEffect(CustomStatusEffects.CONVERSION), "what the villager turned into carries Conversion");
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

    /** A pale zombie's hit and a creaking's convert, and give nothing else (the pale zombie's wither is gone). */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void paleZombiesAndCreakingsHitsConvert(GameTestHelper helper) {
        floor(helper, Blocks.STONE.defaultBlockState());
        Mob first = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        Mob second = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(10.5, 1.0, 3.5));
        Mob paleZombie = helper.spawnWithNoFreeWill(ModEntities.PALE_ZOMBIE, new Vec3(3.5, 1.0, 5.5));
        Mob creaking = helper.spawnWithNoFreeWill(EntityTypes.CREAKING, new Vec3(10.5, 1.0, 5.5));
        helper.assertTrue(paleZombie.doHurtTarget(helper.getLevel(), first), "the pale zombie missed");
        helper.assertTrue(first.hasEffect(CustomStatusEffects.CONVERSION), "the pale zombie's hit didn't convert");
        helper.assertTrue(!first.hasEffect(MobEffects.WITHER), "the pale zombie's hit withered");
        helper.assertTrue(creaking.doHurtTarget(helper.getLevel(), second), "the creaking missed");
        helper.assertTrue(second.hasEffect(CustomStatusEffects.CONVERSION), "the creaking's hit didn't convert");
        helper.succeed();
    }

    /** A zombie's hit converts only by chance, and then in place of its own effect: a husk's hunger. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aZombiesRareConvertingHitGivesNothingElse(GameTestHelper helper) {
        floor(helper, Blocks.STONE.defaultBlockState());
        Mob converted = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(3.5, 1.0, 3.5));
        Mob spared = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(10.5, 1.0, 3.5));
        Mob notKin = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(6.5, 1.0, 1.5));
        Mob husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new Vec3(6.5, 1.0, 5.5));
        float chance = VersusSettings.Mushrooms.ZOMBIE_CONVERSION_CHANCE;
        try {
            VersusSettings.Mushrooms.ZOMBIE_CONVERSION_CHANCE = 1.0F;
            helper.assertTrue(husk.doHurtTarget(helper.getLevel(), converted), "the husk missed");
            helper.assertTrue(husk.doHurtTarget(helper.getLevel(), notKin), "the husk missed the pig");
            VersusSettings.Mushrooms.ZOMBIE_CONVERSION_CHANCE = 0.0F;
            helper.assertTrue(husk.doHurtTarget(helper.getLevel(), spared), "the husk missed its second");
        } finally {
            VersusSettings.Mushrooms.ZOMBIE_CONVERSION_CHANCE = chance;
        }
        helper.assertTrue(converted.hasEffect(CustomStatusEffects.CONVERSION), "the husk's converting hit didn't convert");
        helper.assertTrue(!converted.hasEffect(MobEffects.HUNGER), "the husk's converting hit also gave hunger");
        helper.assertTrue(!notKin.hasEffect(CustomStatusEffects.CONVERSION), "the husk converted a pig, not its own kind");
        helper.assertTrue(!spared.hasEffect(CustomStatusEffects.CONVERSION), "the husk's hit converted without the chance");
        helper.succeed();
    }

    /** All white, a zombie turns pale, a creeper turns into a pale creeper: Conversion makes the undead and their kin pale. */
    @GameTest(structure = ROOM, maxTicks = 200)
    public void allWhiteZombiesAndCreepersTurnPale(GameTestHelper helper) {
        floor(helper, Blocks.STONE.defaultBlockState());
        Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(3.5, 1.0, 3.5));
        Mob creeper = helper.spawnWithNoFreeWill(EntityTypes.CREEPER, new Vec3(10.5, 1.0, 3.5));
        zombie.addEffect(allWhite(zombie));
        creeper.addEffect(allWhite(creeper));
        helper.succeedWhen(() -> {
            helper.assertTrue(zombie.isRemoved() && creeper.isRemoved(), "the zombie or the creeper is still itself");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(PaleZombieEntity.class, room(helper)).size() == 1, "no pale zombie rose from the zombie");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(PaleCreeperEntity.class, room(helper)).size() == 1, "no pale creeper came of the creeper");
        });
    }

    /** All white, a pillager rises as the zombie of the place it turned in. */
    @GameTest(structure = ROOM, maxTicks = 200)
    public void anAllWhitePillagerRisesAsTheZombieOfThePlace(GameTestHelper helper) {
        floor(helper, Blocks.STONE.defaultBlockState());
        Mob pillager = helper.spawnWithNoFreeWill(EntityTypes.PILLAGER, new Vec3(3.5, 1.0, 3.5));
        var expected = Undeath.zombieOfThePlace(helper.getLevel(), pillager);
        pillager.addEffect(allWhite(pillager));
        helper.succeedWhen(() -> {
            helper.assertTrue(pillager.isRemoved(), "the pillager is still a pillager");
            List<Mob> risen = helper.getLevel().getEntitiesOfClass(Mob.class, room(helper), mob -> mob.getType() == expected);
            helper.assertTrue(risen.size() == 1, risen.size() + " " + expected + " rose, not one");
        });
    }

    /** A watchling's hit converts, as every pale thing's. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aWatchlingsHitConverts(GameTestHelper helper) {
        floor(helper, Blocks.STONE.defaultBlockState());
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        Mob watchling = helper.spawnWithNoFreeWill(ModEntities.WATCHLING, new Vec3(5.5, 1.0, 3.5));
        helper.assertTrue(watchling.doHurtTarget(helper.getLevel(), pig), "the watchling missed");
        helper.assertTrue(pig.hasEffect(CustomStatusEffects.CONVERSION), "the watchling's hit didn't convert");
        helper.assertTrue(!watchling.addEffect(ConversionAttacks.dose()), "a watchling took Conversion");
        helper.succeed();
    }

    /** Rotten flesh keeps its hunger and can convert too: a 1-in-8 chance of a minute's dose. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void rottenFleshCanConvert(GameTestHelper helper) {
        Consumable flesh = new ItemStack(Items.ROTTEN_FLESH).get(DataComponents.CONSUMABLE);
        helper.assertTrue(flesh != null, "rotten flesh can't be eaten");
        boolean converts = false, hungers = false;
        for (ConsumeEffect effect : flesh.onConsumeEffects()) {
            if (!(effect instanceof ApplyStatusEffectsConsumeEffect apply)) continue;
            for (MobEffectInstance instance : apply.effects()) {
                if (instance.is(CustomStatusEffects.CONVERSION) && instance.getAmplifier() == 0 && instance.getDuration() == 1200
                        && apply.probability() == 0.125F) converts = true;
                if (instance.is(MobEffects.HUNGER)) hungers = true;
            }
        }
        helper.assertTrue(converts, "rotten flesh has no 1-in-8 chance of a minute's Conversion: " + flesh.onConsumeEffects());
        helper.assertTrue(hungers, "rotten flesh lost its hunger");
        helper.succeed();
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
