package frootloops.versus.mod.environment.blocks.fruit;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The lemon bush's leaves, which fruit and fruit again: bare, then in blossom, then hung with lemons ({@link #FRUIT} 0 to
 * 2), a stage at a time on random ticks or with bone meal. Lemons are picked by hand, 2 to 6 at a time (the
 * harvest/lemon_leaves loot table), and the leaves go bare again. Otherwise they are leaves like oak's: they live by an
 * oak log, stripped or not, and decay, shed and fall as the mod's leaves do (LeavesMixin).
 */
public class LemonLeavesBlock extends TintedParticleLeavesBlock implements BonemealableBlock {

    public static final int BARE = 0, BLOSSOMS = 1, LEMONS = 2;
    public static final IntegerProperty FRUIT = IntegerProperty.create("fruit", BARE, LEMONS);
    public static final ResourceKey<LootTable> HARVEST = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "harvest/lemon_leaves"));

    public LemonLeavesBlock(BlockBehaviour.Properties properties) {
        super(0.01F, properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FRUIT, BARE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FRUIT);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return super.isRandomlyTicking(state) || state.getValue(FRUIT) < LEMONS;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        // Decayed leaves are gone; living ones grow their fruit
        BlockState now = level.getBlockState(pos);
        if (now.is(this) && now.getValue(FRUIT) < LEMONS && random.nextFloat() < VersusSettings.Lemons.FRUIT_GROW_CHANCE) {
            level.setBlock(pos, now.setValue(FRUIT, now.getValue(FRUIT) + 1), Block.UPDATE_CLIENTS);
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
            BlockState picked = state.setValue(FRUIT, BARE);
            serverLevel.setBlock(pos, picked, Block.UPDATE_CLIENTS);
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return state.getValue(FRUIT) < LEMONS;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        level.setBlock(pos, state.setValue(FRUIT, Math.min(LEMONS, state.getValue(FRUIT) + 1)), Block.UPDATE_CLIENTS);
    }
}
