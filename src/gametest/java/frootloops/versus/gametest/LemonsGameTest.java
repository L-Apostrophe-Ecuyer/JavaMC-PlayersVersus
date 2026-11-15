package frootloops.versus.gametest;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.blocks.fruit.LemonBushBlock;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.items_and_effects.fruit.Fruits;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lemon bushes: two blocks tall, as a rose bush is, whole or gone; they go bare, blossom and fruit, by random ticks or
 * bone meal; lemons are picked by hand, 2 to 6, and the bush goes bare; the lemon sapling grows into one, and the Tuscan
 * grove's orchards are patches of them. A lemon is a little food and a quick Sour.
 */
public class LemonsGameTest {

    private static final String ROOM = "players-versus-gametest:leaves_room"; // 14 x 8 x 8
    private static final String GROVE = "players-versus-gametest:leaves_grove"; // 48 x 40 x 48, a grass floor

    private static int lemonsAround(GameTestHelper helper, BlockPos pos) {
        AABB around = new AABB(helper.absolutePos(pos)).inflate(2);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, item -> item.getItem().is(Fruits.LEMON)).stream()
                .mapToInt(item -> item.getItem().getCount()).sum();
    }

    private static BlockState bush(int fruit) {
        return CustomBlocks.LEMON_BUSH.defaultBlockState().setValue(LemonBushBlock.FRUIT, fruit);
    }

    /** A lemon bush planted on grass with its bottom at {@code lower} (relative), both halves. */
    private static BlockPos plantBush(GameTestHelper helper, BlockPos lower, int fruit) {
        helper.setBlock(lower.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        DoublePlantBlock.placeAt(helper.getLevel(), bush(fruit), helper.absolutePos(lower), Block.UPDATE_ALL);
        return lower;
    }

    /** Whether a whole lemon bush stands with its bottom at {@code lower}, both halves at that stage. */
    private static boolean wholeBush(GameTestHelper helper, BlockPos lower, int fruit) {
        BlockState bottom = helper.getBlockState(lower), top = helper.getBlockState(lower.above());
        return bottom.is(CustomBlocks.LEMON_BUSH) && top.is(CustomBlocks.LEMON_BUSH)
                && bottom.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER && top.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER
                && bottom.getValue(LemonBushBlock.FRUIT) == fruit && top.getValue(LemonBushBlock.FRUIT) == fruit;
    }

    private static int itemsAround(GameTestHelper helper, BlockPos pos, Item item) {
        AABB around = new AABB(helper.absolutePos(pos)).inflate(2);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, entity -> entity.getItem().is(item)).stream()
                .mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    /** Two blocks tall, as a rose bush is; broken by either half it goes whole, and drops itself once. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aLemonBushIsTwoBlocksTallAndBreaksWhole(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Item item = CustomBlocks.LEMON_BUSH.asItem();
        BlockPos first = plantBush(helper, new BlockPos(3, 2, 4), LemonBushBlock.BARE);
        helper.assertTrue(wholeBush(helper, first, LemonBushBlock.BARE), "the lemon bush isn't two halves: " + helper.getBlockState(first) + ", " + helper.getBlockState(first.above()));
        level.destroyBlock(helper.absolutePos(first.above()), true);
        helper.assertTrue(helper.getBlockState(first).isAir() && helper.getBlockState(first.above()).isAir(), "breaking its top left its bottom");
        BlockPos second = plantBush(helper, new BlockPos(9, 2, 4), LemonBushBlock.BARE);
        level.destroyBlock(helper.absolutePos(second), true);
        helper.assertTrue(helper.getBlockState(second).isAir() && helper.getBlockState(second.above()).isAir(), "breaking its bottom left its top");
        int fromTop = itemsAround(helper, first, item), fromBottom = itemsAround(helper, second, item);
        helper.assertTrue(fromTop == 1 && fromBottom == 1, "broken by its top it dropped " + fromTop + " bushes, by its bottom " + fromBottom + ", not one each");
        helper.succeed();
    }

    /** It blossoms, then fruits, both halves together: on random ticks of its bottom, and with bone meal on either half. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aLemonBushBlossomsThenFruitsOnRandomTicksAndBoneMeal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = plantBush(helper, new BlockPos(4, 2, 4), LemonBushBlock.BARE);
        helper.assertTrue(helper.getBlockState(pos).isRandomlyTicking(), "a bare lemon bush doesn't tick");
        helper.assertTrue(!helper.getBlockState(pos.above()).isRandomlyTicking(), "its top ticks too: it would fruit twice as fast");
        float chance = VersusSettings.Lemons.FRUIT_GROW_CHANCE;
        VersusSettings.Lemons.FRUIT_GROW_CHANCE = 1.0F;
        try {
            helper.getBlockState(pos).randomTick(level, helper.absolutePos(pos), RandomSource.create(3));
            helper.assertTrue(wholeBush(helper, pos, LemonBushBlock.BLOSSOMS), "a bare bush didn't blossom, both halves");
            helper.getBlockState(pos).randomTick(level, helper.absolutePos(pos), RandomSource.create(3));
            helper.assertTrue(wholeBush(helper, pos, LemonBushBlock.LEMONS), "a bush in blossom didn't fruit, both halves");
        } finally {
            VersusSettings.Lemons.FRUIT_GROW_CHANCE = chance;
        }
        helper.assertTrue(!helper.getBlockState(pos).isRandomlyTicking(), "a fruiting lemon bush still ticks");

        BlockPos other = plantBush(helper, new BlockPos(9, 2, 4), LemonBushBlock.BARE);
        LemonBushBlock block = CustomBlocks.LEMON_BUSH;
        BlockPos top = helper.absolutePos(other.above());
        helper.assertTrue(block.isValidBonemealTarget(level, top, helper.getBlockState(other.above()), BonemealSource.INTERACTION), "bone meal can't help a bare bush");
        block.performBonemeal(level, RandomSource.create(1), top, helper.getBlockState(other.above()), BonemealSource.INTERACTION);
        helper.assertTrue(wholeBush(helper, other, LemonBushBlock.BLOSSOMS), "bone meal on its top didn't make the bush blossom");
        helper.succeed();
    }

    /**
     * Hung with lemons it has nothing left to grow: bone meal gives a lemon sapling instead, as a rose bush gives another of
     * itself, and the bush keeps its lemons.
     */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void boneMealOnAFruitingLemonBushGivesASapling(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = plantBush(helper, new BlockPos(6, 2, 4), LemonBushBlock.LEMONS);
        LemonBushBlock block = CustomBlocks.LEMON_BUSH;
        helper.assertTrue(block.isValidBonemealTarget(level, helper.absolutePos(pos), helper.getBlockState(pos), BonemealSource.INTERACTION), "bone meal can't take a fruiting bush");
        block.performBonemeal(level, RandomSource.create(1), helper.absolutePos(pos), helper.getBlockState(pos), BonemealSource.INTERACTION);
        helper.assertTrue(itemsAround(helper, pos, CustomBlocks.LEMON_SAPLING.asItem()) == 1, "bone meal on a fruiting bush gave no lemon sapling");
        helper.assertTrue(wholeBush(helper, pos, LemonBushBlock.LEMONS), "the bush lost its lemons to bone meal");
        helper.succeed();
    }

    /** Picked by either half when hung with lemons, it gives 2 to 6 and goes bare; bare, it gives none. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void pickingALemonBushGivesTwoToSixAndLeavesItBare(GameTestHelper helper) {
        BlockPos pos = plantBush(helper, new BlockPos(6, 2, 4), LemonBushBlock.LEMONS);
        ServerPlayer player = FakePlayer.get(helper.getLevel());
        BlockPos top = helper.absolutePos(pos.above());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(top), Direction.NORTH, top, false);
        InteractionResult result = helper.getBlockState(pos.above()).useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(result.consumesAction(), "picking the lemons did nothing: " + result);
        helper.assertTrue(wholeBush(helper, pos, LemonBushBlock.BARE), "the bush isn't bare after picking, both halves");
        int lemons = lemonsAround(helper, pos.above());
        helper.assertTrue(lemons >= 2 && lemons <= 6, lemons + " lemons picked, not 2 to 6");
        BlockPos bottom = helper.absolutePos(pos);
        InteractionResult again = helper.getBlockState(pos).useWithoutItem(helper.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(bottom), Direction.NORTH, bottom, false));
        helper.assertTrue(!again.consumesAction() && lemonsAround(helper, pos.above()) == lemons, "a bare bush gave lemons");
        helper.succeed();
    }

    /** Broken, a fruiting bush drops itself (from its bottom: its top drops nothing of its own) and its lemons. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aBrokenFruitingLemonBushDropsItselfAndItsLemons(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = plantBush(helper, new BlockPos(6, 2, 4), LemonBushBlock.LEMONS);
        Item item = CustomBlocks.LEMON_BUSH.asItem();
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(pos), level, helper.absolutePos(pos), null, null, ItemStack.EMPTY);
        int bushes = drops.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
        int lemons = drops.stream().filter(stack -> stack.is(Fruits.LEMON)).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(bushes == 1, "a broken bush dropped " + bushes + " bushes");
        helper.assertTrue(lemons >= 2 && lemons <= 6, "a broken fruiting bush dropped " + lemons + " lemons");
        List<ItemStack> topDrops = Block.getDrops(helper.getBlockState(pos.above()), level, helper.absolutePos(pos.above()), null, null, ItemStack.EMPTY);
        helper.assertTrue(topDrops.isEmpty(), "its top dropped " + topDrops + " of its own");
        List<ItemStack> bareDrops = Block.getDrops(bush(LemonBushBlock.BARE), level, helper.absolutePos(pos), null, null, ItemStack.EMPTY);
        helper.assertTrue(bareDrops.stream().noneMatch(stack -> stack.is(Fruits.LEMON)), "a bare bush dropped lemons");
        helper.succeed();
    }

    /** The lemon sapling grows into a lemon bush, bare. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void theLemonSaplingGrowsIntoALemonBush(GameTestHelper helper) {
        BlockPos pos = new BlockPos(6, 2, 4);
        helper.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        helper.setBlock(pos, CustomBlocks.LEMON_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 1));
        ((SaplingBlock) CustomBlocks.LEMON_SAPLING).advanceTree(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), RandomSource.create(5));
        helper.assertTrue(wholeBush(helper, pos, LemonBushBlock.BARE), "the lemon sapling didn't grow into a bare lemon bush: " + helper.getBlockState(pos) + ", " + helper.getBlockState(pos.above()));
        helper.succeed();
    }

    /**
     * The orchards' bushes (lemon_bushes, what the Tuscan grove's lemon_orchards place): whole bushes, at more than one
     * stage.
     */
    @GameTest(structure = GROVE, maxTicks = 20)
    public void theOrchardsAreLemonBushes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Feature bushes = level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(Identifier.fromNamespaceAndPath("players-versus", "lemon_bushes"));
        helper.assertTrue(bushes != null, "no lemon_bushes feature");
        PlacedFeature orchards = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getValue(Identifier.fromNamespaceAndPath("players-versus", "lemon_orchards"));
        helper.assertTrue(orchards != null && orchards.feature().value() == bushes, "the lemon orchards don't place lemon_bushes");
        Set<Integer> stages = new HashSet<>();
        int placed = 0;
        for (int i = 0; i < 12; i++) {
            BlockPos at = new BlockPos(4 + 3 * i, 1, 10);
            if (!bushes.place(level, level.getChunkSource().getGenerator(), RandomSource.create(i), helper.absolutePos(at))) continue;
            placed++;
            BlockState bottom = helper.getBlockState(at);
            helper.assertTrue(bottom.is(CustomBlocks.LEMON_BUSH) && wholeBush(helper, at, bottom.getValue(LemonBushBlock.FRUIT)), "the orchard placed no whole bush at " + at.toShortString());
            stages.add(bottom.getValue(LemonBushBlock.FRUIT));
        }
        helper.assertTrue(placed >= 10, "only " + placed + " bushes placed of 12");
        helper.assertTrue(stages.size() > 1, "the orchard's bushes are all at one stage: " + stages);
        helper.succeed();
    }

    /**
     * A lemon is a little food and a quick Sour: 1 hunger, no saturation, 40 ticks to eat; eaten, it makes one Sour for 4
     * seconds, slow for 4 and hasty (Haste I) for 24.
     */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aLemonIsALittleFoodAndAQuickSour(GameTestHelper helper) {
        ItemStack lemon = new ItemStack(Fruits.LEMON);
        FoodProperties food = lemon.get(DataComponents.FOOD);
        helper.assertTrue(food != null && food.nutrition() == 1 && food.saturation() == 0.0F,
                "a lemon fills " + (food == null ? "nothing" : food.nutrition() + " hunger and " + food.saturation() + " saturation") + ", not 1 and 0");
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        helper.assertTrue(lemon.getUseDuration(pig) == 40, "a lemon takes " + lemon.getUseDuration(pig) + " ticks to eat, not 40");
        lemon.finishUsingItem(helper.getLevel(), pig);
        MobEffectInstance sour = pig.getEffect(CustomStatusEffects.SOUR);
        MobEffectInstance slow = pig.getEffect(MobEffects.SLOWNESS);
        MobEffectInstance haste = pig.getEffect(MobEffects.HASTE);
        helper.assertTrue(sour != null && sour.getDuration() == 80, "Sour lasts " + (sour == null ? 0 : sour.getDuration()) + " ticks, not 80");
        helper.assertTrue(slow != null && slow.getDuration() == 80 && slow.getAmplifier() == 0, "the lemon's slowness: " + slow);
        helper.assertTrue(haste != null && haste.getDuration() == 480 && haste.getAmplifier() == 0, "the lemon's haste: " + haste);
        helper.succeed();
    }
}
