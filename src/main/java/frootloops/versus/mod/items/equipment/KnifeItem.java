package frootloops.versus.mod.items.equipment;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class KnifeItem extends SwordItem {
    public KnifeItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient && !state.isIn(BlockTags.FIRE)) {
            stack.damage(1, miner, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        }
        if (state.isIn(BlockTags.LEAVES) || state.isOf(Blocks.COBWEB) || state.isOf(Blocks.SHORT_GRASS) || state.isOf(Blocks.FERN) || state.isOf(Blocks.DEAD_BUSH) || state.isOf(Blocks.HANGING_ROOTS) || state.isOf(Blocks.VINE) || state.isOf(Blocks.TRIPWIRE) || state.isIn(BlockTags.WOOL)) {
            return true;
        }
        return super.postMine(stack, world, state, pos, miner);
    }

    @Override
    public boolean isSuitableFor(BlockState state) {
        return state.isOf(Blocks.COBWEB) || state.isOf(Blocks.REDSTONE_WIRE) || state.isOf(Blocks.TRIPWIRE) || state.isOf(Blocks.VINE);
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        if (state.isOf(Blocks.COBWEB) || state.isIn(BlockTags.LEAVES)) {
            return 15.0f;
        }
        if (state.isIn(BlockTags.WOOL)) {
            return 5.0f;
        }
        if (state.isOf(Blocks.VINE) || state.isOf(Blocks.GLOW_LICHEN)) {
            return 2.0f;
        }
        return super.getMiningSpeedMultiplier(stack, state);
    }


    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        AbstractPlantStemBlock abstractPlantStemBlock;
        BlockPos blockPos;
        World world = context.getWorld();
        BlockState blockState = world.getBlockState(blockPos = context.getBlockPos());
        Block block = blockState.getBlock();

        // Trim a plant stem!
        if (block instanceof AbstractPlantStemBlock && !(abstractPlantStemBlock = (AbstractPlantStemBlock)block).hasMaxAge(blockState)) {
            PlayerEntity playerEntity = context.getPlayer();
            ItemStack itemStack = context.getStack();
            if (playerEntity instanceof ServerPlayerEntity) {
                Criteria.ITEM_USED_ON_BLOCK.trigger((ServerPlayerEntity)playerEntity, blockPos, itemStack);
            }
            world.playSound(playerEntity, blockPos, SoundEvents.BLOCK_GROWING_PLANT_CROP, SoundCategory.BLOCKS, 1.0f, 1.0f);
            BlockState blockState2 = abstractPlantStemBlock.withMaxAge(blockState);
            world.setBlockState(blockPos, blockState2);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Emitter.of(context.getPlayer(), blockState2));
            if (playerEntity != null) itemStack.damage(1, playerEntity, player -> player.sendToolBreakStatus(context.getHand()));
            return ActionResult.success(world.isClient);
        }

        // Carve a pumpkin!
        if (block instanceof PumpkinBlock) {
            PlayerEntity playerEntity = context.getPlayer();
            ItemStack itemStack = context.getStack();
            if (playerEntity instanceof ServerPlayerEntity) {
                Criteria.ITEM_USED_ON_BLOCK.trigger((ServerPlayerEntity)playerEntity, blockPos, itemStack);
            }

            Direction direction = context.getSide();
            Direction direction2 = direction.getAxis() == Direction.Axis.Y ? playerEntity.getHorizontalFacing().getOpposite() : direction;

            world.playSound(null, blockPos, SoundEvents.BLOCK_PUMPKIN_CARVE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            world.setBlockState(blockPos, (BlockState)Blocks.CARVED_PUMPKIN.getDefaultState().with(CarvedPumpkinBlock.FACING, direction2), Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD);
            ItemEntity itemEntity = new ItemEntity(world, (double)blockPos.getX() + 0.5 + (double)direction2.getOffsetX() * 0.65, (double)blockPos.getY() + 0.1, (double)blockPos.getZ() + 0.5 + (double)direction2.getOffsetZ() * 0.65, new ItemStack(Items.PUMPKIN_SEEDS, 4));
            itemEntity.setVelocity(0.05 * (double)direction2.getOffsetX() + world.random.nextDouble() * 0.02, 0.05, 0.05 * (double)direction2.getOffsetZ() + world.random.nextDouble() * 0.02);
            world.spawnEntity(itemEntity);
            itemStack.damage(1, playerEntity, player -> player.sendToolBreakStatus(context.getHand()));
            world.emitGameEvent((Entity)playerEntity, GameEvent.SHEAR, blockPos);
            playerEntity.incrementStat(Stats.USED.getOrCreateStat(Items.SHEARS));
        }

        return super.useOnBlock(context);
    }
}
