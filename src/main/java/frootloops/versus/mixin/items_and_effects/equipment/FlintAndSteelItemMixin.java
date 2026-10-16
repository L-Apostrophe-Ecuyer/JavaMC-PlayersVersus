package frootloops.versus.mixin.items_and_effects.equipment;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public abstract class FlintAndSteelItemMixin extends Item {

    public FlintAndSteelItemMixin(Properties settings) {
        super(settings);
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    public void useOnBlock(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        BlockPos blockPos;
        Player playerEntity = context.getPlayer();
        Level world = context.getLevel();
        BlockState state = world.getBlockState(blockPos = context.getClickedPos());
        if(state.is(CustomBlocks.SMOLDERING_TORCH) || state.is(CustomBlocks.SMOLDERING_WALL_TORCH) || state.is(CustomBlocks.EXTINGUISHED_TORCH) || state.is(CustomBlocks.EXTINGUISHED_WALL_TORCH)) {
            if(state.getBlock() instanceof WallTorchBlock) world.setBlock(blockPos, Blocks.WALL_TORCH.withPropertiesOf(state), Block.UPDATE_ALL | Block.UPDATE_IMMEDIATE);
            else world.setBlock(blockPos, Blocks.TORCH.withPropertiesOf(state), Block.UPDATE_ALL | Block.UPDATE_IMMEDIATE);
            world.gameEvent((Entity)playerEntity, GameEvent.BLOCK_CHANGE, blockPos);
            world.playSound(playerEntity, blockPos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, world.getRandom().nextFloat() * 0.4f + 0.8f);
            if (playerEntity != null) context.getItemInHand().hurtAndBreak(1, playerEntity, context.getHand().asEquipmentSlot());
            cir.setReturnValue(InteractionResult.SUCCESS);
            cir.cancel();
        }
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        if(entity.isAlive()) {
            entity.igniteForSeconds(2);
            entity.setLastHurtByMob(user);
            if (user != null) stack.hurtAndBreak(1, user, hand.asEquipmentSlot());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

}
