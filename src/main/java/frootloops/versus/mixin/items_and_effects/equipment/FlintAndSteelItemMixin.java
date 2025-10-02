package frootloops.versus.mixin.items_and_effects.equipment;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public abstract class FlintAndSteelItemMixin extends Item {

    public FlintAndSteelItemMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    public void useOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        BlockPos blockPos;
        PlayerEntity playerEntity = context.getPlayer();
        World world = context.getWorld();
        BlockState state = world.getBlockState(blockPos = context.getBlockPos());
        if(state.isOf(CustomBlocks.SMOLDERING_TORCH) || state.isOf(CustomBlocks.SMOLDERING_WALL_TORCH) || state.isOf(CustomBlocks.EXTINGUISHED_TORCH) || state.isOf(CustomBlocks.EXTINGUISHED_WALL_TORCH)) {
            if(state.getBlock() instanceof WallTorchBlock) world.setBlockState(blockPos, Blocks.WALL_TORCH.getStateWithProperties(state), Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD);
            else world.setBlockState(blockPos, Blocks.TORCH.getStateWithProperties(state), Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD);
            world.emitGameEvent((Entity)playerEntity, GameEvent.BLOCK_CHANGE, blockPos);
            world.playSound(playerEntity, blockPos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0f, world.getRandom().nextFloat() * 0.4f + 0.8f);
            if (playerEntity != null) context.getStack().damage(1, playerEntity, context.getHand().getEquipmentSlot());
            cir.setReturnValue(ActionResult.SUCCESS);
            cir.cancel();
        }
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if(entity.isAlive()) {
            entity.setOnFireFor(2);
            entity.setAttacker(user);
            if (user != null) stack.damage(1, user, hand.getEquipmentSlot());
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

}
