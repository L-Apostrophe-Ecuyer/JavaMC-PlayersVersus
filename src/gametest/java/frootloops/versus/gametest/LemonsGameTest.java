package frootloops.versus.gametest;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.blocks.fruit.LemonLeavesBlock;
import frootloops.versus.mod.environment.blocks.leaves.LeafReach;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.items_and_effects.fruit.Fruits;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Lemon bushes: two or three stripped oak logs under lemon leaves; the leaves go bare, blossom and fruit, by random
 * ticks or bone meal; lemons are picked by hand, 2 to 6, and the leaves go bare; oak logs keep the leaves alive; a lemon
 * makes one Sour.
 */
public class LemonsGameTest {

    private static final String ROOM = "players-versus-gametest:leaves_room"; // 14 x 8 x 8
    private static final String GROVE = "players-versus-gametest:leaves_grove"; // 48 x 40 x 48, a grass floor

    private static BlockState leaves(int fruit) {
        return CustomBlocks.LEMON_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true).setValue(LemonLeavesBlock.FRUIT, fruit);
    }

    private static int lemonsAround(GameTestHelper helper, BlockPos pos) {
        AABB around = new AABB(helper.absolutePos(pos)).inflate(2);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, item -> item.getItem().is(Fruits.LEMON)).stream()
                .mapToInt(item -> item.getItem().getCount()).sum();
    }

    @GameTest(structure = GROVE, maxTicks = 20)
    public void aLemonBushIsTwoOrThreeStrippedOakLogsUnderLemonLeaves(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int grown = 0;
        for (int i = 0; i < 6; i++) {
            BlockPos foot = new BlockPos(6 + 7 * i, 1, 8);
            boolean placed = level.registryAccess().lookupOrThrow(Registries.FEATURE).getOrThrow(CustomBlocks.LEMON_BUSH).value()
                    .place(level, level.getChunkSource().getGenerator(), RandomSource.create(i), helper.absolutePos(foot));
            if (!placed) continue;
            grown++;
            int logs = 0;
            while (helper.getBlockState(foot.above(logs)).is(Blocks.STRIPPED_OAK_LOG)) logs++;
            helper.assertTrue(logs >= 2 && logs <= 3, "a lemon bush grew " + logs + " stripped oak logs");
            helper.assertTrue(helper.getBlockState(foot.above(logs)).is(CustomBlocks.LEMON_LEAVES), "no lemon leaves top the trunk");
            int leaves = 0;
            for (BlockPos pos : BlockPos.betweenClosed(foot.offset(-2, 0, -2), foot.offset(2, logs + 1, 2))) {
                if (helper.getBlockState(pos).is(CustomBlocks.LEMON_LEAVES)) leaves++;
            }
            helper.assertTrue(leaves >= 12, "a lemon bush has only " + leaves + " leaves");
        }
        helper.assertTrue(grown > 0, "no lemon bush grew");
        helper.succeed();
    }

    @GameTest(structure = ROOM, maxTicks = 20)
    public void pickingLemonsGivesTwoToSixAndLeavesThemBare(GameTestHelper helper) {
        BlockPos pos = new BlockPos(6, 2, 4);
        helper.setBlock(pos, leaves(LemonLeavesBlock.LEMONS));
        ServerPlayer player = FakePlayer.get(helper.getLevel());
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        InteractionResult result = helper.getBlockState(pos).useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(result.consumesAction(), "picking the lemons did nothing: " + result);
        helper.assertTrue(helper.getBlockState(pos).getValue(LemonLeavesBlock.FRUIT) == LemonLeavesBlock.BARE, "the leaves still have lemons");
        int lemons = lemonsAround(helper, pos);
        helper.assertTrue(lemons >= 2 && lemons <= 6, lemons + " lemons picked, not 2 to 6");
        // bare leaves have nothing to pick
        InteractionResult again = helper.getBlockState(pos).useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(!again.consumesAction() && lemonsAround(helper, pos) == lemons, "bare leaves gave lemons");
        helper.succeed();
    }

    @GameTest(structure = ROOM, maxTicks = 20)
    public void bareLeavesBlossomThenFruitOnRandomTicksAndBoneMeal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(6, 2, 4);
        helper.setBlock(pos, leaves(LemonLeavesBlock.BARE));
        helper.assertTrue(helper.getBlockState(pos).isRandomlyTicking(), "bare lemon leaves don't tick");
        float chance = VersusSettings.Lemons.FRUIT_GROW_CHANCE;
        VersusSettings.Lemons.FRUIT_GROW_CHANCE = 1.0F;
        try {
            helper.getBlockState(pos).randomTick(helper.getLevel(), helper.absolutePos(pos), RandomSource.create(3));
            helper.assertTrue(helper.getBlockState(pos).getValue(LemonLeavesBlock.FRUIT) == LemonLeavesBlock.BLOSSOMS, "bare leaves didn't blossom");
            helper.getBlockState(pos).randomTick(helper.getLevel(), helper.absolutePos(pos), RandomSource.create(3));
            helper.assertTrue(helper.getBlockState(pos).getValue(LemonLeavesBlock.FRUIT) == LemonLeavesBlock.LEMONS, "blossoms didn't fruit");
        } finally {
            VersusSettings.Lemons.FRUIT_GROW_CHANCE = chance;
        }
        helper.assertTrue(!helper.getBlockState(pos).isRandomlyTicking(), "fruiting leaves on a tree still tick");

        BlockPos other = new BlockPos(8, 2, 4);
        helper.setBlock(other, leaves(LemonLeavesBlock.BARE));
        LemonLeavesBlock block = CustomBlocks.LEMON_LEAVES;
        BlockState bare = helper.getBlockState(other);
        helper.assertTrue(block.isValidBonemealTarget(helper.getLevel(), helper.absolutePos(other), bare, BonemealSource.INTERACTION), "bone meal can't help bare leaves");
        block.performBonemeal(helper.getLevel(), RandomSource.create(1), helper.absolutePos(other), bare, BonemealSource.INTERACTION);
        helper.assertTrue(helper.getBlockState(other).getValue(LemonLeavesBlock.FRUIT) == LemonLeavesBlock.BLOSSOMS, "bone meal didn't make them blossom");
        helper.assertTrue(!block.isValidBonemealTarget(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), BonemealSource.INTERACTION),
                "bone meal still takes fruiting leaves");
        helper.succeed();
    }

    @GameTest(structure = ROOM, maxTicks = 20)
    public void oakLogsStrippedOrNotKeepLemonLeavesAlive(GameTestHelper helper) {
        Block lemon = CustomBlocks.LEMON_LEAVES;
        helper.assertTrue(LeafReach.isOwnLog(lemon, Blocks.STRIPPED_OAK_LOG.defaultBlockState()), "a stripped oak log doesn't keep lemon leaves");
        helper.assertTrue(LeafReach.isOwnLog(lemon, Blocks.OAK_LOG.defaultBlockState()), "an oak log doesn't keep lemon leaves");
        helper.assertTrue(!LeafReach.isOwnLog(lemon, Blocks.BIRCH_LOG.defaultBlockState()), "a birch log keeps lemon leaves");
        helper.succeed();
    }

    @GameTest(structure = ROOM, maxTicks = 20)
    public void breakingFruitingLeavesDropsTheirLemons(GameTestHelper helper) {
        BlockPos pos = new BlockPos(6, 2, 4);
        helper.setBlock(pos, leaves(LemonLeavesBlock.LEMONS));
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(pos), helper.getLevel(), helper.absolutePos(pos), null, null, ItemStack.EMPTY);
        int lemons = drops.stream().filter(stack -> stack.is(Fruits.LEMON)).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(lemons >= 2 && lemons <= 6, "broken fruiting leaves dropped " + lemons + " lemons");
        List<ItemStack> bareDrops = Block.getDrops(leaves(LemonLeavesBlock.BARE), helper.getLevel(), helper.absolutePos(pos), null, null, ItemStack.EMPTY);
        helper.assertTrue(bareDrops.stream().noneMatch(stack -> stack.is(Fruits.LEMON)), "bare leaves dropped lemons");
        helper.succeed();
    }

    @GameTest(structure = ROOM, maxTicks = 20)
    public void eatingALemonMakesOneSour(GameTestHelper helper) {
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        ItemStack lemon = new ItemStack(Fruits.LEMON);
        lemon.finishUsingItem(helper.getLevel(), pig);
        MobEffectInstance sour = pig.getEffect(CustomStatusEffects.SOUR);
        helper.assertTrue(sour != null, "the lemon didn't make it Sour");
        helper.assertTrue(sour.getDuration() == VersusSettings.Lemons.SOUR_TICKS, "Sour lasts " + sour.getDuration() + " ticks");
        helper.succeed();
    }
}
