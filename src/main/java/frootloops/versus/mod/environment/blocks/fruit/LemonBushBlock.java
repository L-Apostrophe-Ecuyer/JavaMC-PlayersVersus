package frootloops.versus.mod.environment.blocks.fruit;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The lemon bush: two blocks tall, as a rose bush is (a lower and an upper half, whole or gone), leafy, and fruiting as
 * fruit trees do: bare, then in blossom, then hung with lemons ({@link #FRUIT} 0 to 2), both halves together, a
 * stage at a time on random ticks (its lower half's, so it doesn't grow twice as fast) or with bone meal on either half.
 * Lemons are picked by hand, 2 to 6 at a time (the harvest/lemon_bush loot table), and the bush goes bare. Broken, it
 * drops itself from its lower half, as a rose bush does, and its lemons if it has them. Hung with lemons it has nothing
 * left to grow: bone meal gives a lemon sapling instead (which grows into a bush), as a rose bush gives another of itself.
 */
public class LemonBushBlock extends DoublePlantBlock implements BonemealableBlock {

    /** Its stages: bare, in blossom, hung with lemons. */
    public static final int BARE = 0, BLOSSOMS = 1, LEMONS = 2;
    public static final IntegerProperty FRUIT = IntegerProperty.create("fruit", BARE, LEMONS);
    public static final ResourceKey<LootTable> HARVEST = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "harvest/lemon_bush"));

    public LemonBushBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FRUIT, BARE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FRUIT);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && state.getValue(FRUIT) < LEMONS;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < VersusSettings.Lemons.FRUIT_GROW_CHANCE) setFruit(level, pos, state, state.getValue(FRUIT) + 1);
    }

    /** Sets both halves to a stage, from either half. */
    private static void setFruit(Level level, BlockPos pos, BlockState state, int fruit) {
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        for (BlockPos half : new BlockPos[]{lower, lower.above()}) {
            BlockState now = level.getBlockState(half);
            if (now.is(state.getBlock())) level.setBlock(half, now.setValue(FRUIT, fruit), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(FRUIT) != LEMONS) return super.useWithoutItem(state, level, pos, player, hitResult);
        if (level instanceof ServerLevel serverLevel) {
            Block.dropFromBlockInteractLootTable(serverLevel, HARVEST, pos, state, null, null, player,
                    (lootLevel, stack) -> Block.popResource(lootLevel, pos, stack));
            serverLevel.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F,
                    0.8F + serverLevel.getRandom().nextFloat() * 0.4F);
            setFruit(serverLevel, pos, state, BARE);
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, serverLevel.getBlockState(pos)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        if (state.getValue(FRUIT) < LEMONS) setFruit(level, pos, state, state.getValue(FRUIT) + 1);
        else Block.popResource(level, pos, new ItemStack(CustomBlocks.LEMON_SAPLING));
    }
}
