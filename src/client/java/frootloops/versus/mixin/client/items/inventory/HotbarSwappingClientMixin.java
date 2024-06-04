package frootloops.versus.mixin.client.items.inventory;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static frootloops.versus.VersusSettings.DO_HOTBAR_SWAPPING_ON_PICK_KEY;

@Environment(EnvType.CLIENT)
@Mixin(value = MinecraftClient.class)
public abstract class HotbarSwappingClientMixin extends ReentrantThreadExecutor<Runnable> implements WindowEventHandler {
    @Shadow public ClientPlayerEntity player;
    @Shadow public ClientWorld world;
    @Shadow @Nullable public HitResult crosshairTarget;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;

    public HotbarSwappingClientMixin(String string) {
        super(string);
    }

    @Shadow private void addBlockEntityNbt(ItemStack stack, BlockEntity blockEntity, DynamicRegistryManager registryManager) {
        NbtCompound nbtCompound = blockEntity.createComponentlessNbtWithIdentifyingData(registryManager);
        blockEntity.removeFromCopiedStackNbt(nbtCompound);
        BlockItem.setBlockEntityData(stack, blockEntity.getType(), nbtCompound);
        stack.applyComponentsFrom(blockEntity.createComponentMap());
    }

    /**
     * Pick block improved; will instead swap hotbar with a row of the inventory containing the match (or, if
     * none such row is found, will cycle to the next row.
     */
    @Inject(method = "doItemPick",at = @At("HEAD"), cancellable = true)
    private void doItemPick(CallbackInfo info) {
        if(!DO_HOTBAR_SWAPPING_ON_PICK_KEY) return;
        boolean isCreativeMode = this.player.getAbilities().creativeMode;
        ItemStack stackToSwapTo = getPickedBlock(isCreativeMode);
        boolean isStackEmpty = (stackToSwapTo == null || stackToSwapTo.isEmpty());

        PlayerInventory playerInventory = this.player.getInventory();
        int slotToSwapTo = isStackEmpty ? -1: playerInventory.getSlotWithStack(stackToSwapTo);
        if (slotToSwapTo == -1 && isCreativeMode && !isStackEmpty && (Screen.hasControlDown() || playerInventory.getStack(playerInventory.selectedSlot).isEmpty() || !isRowFull(playerInventory, 0))) {
            playerInventory.addPickBlock(stackToSwapTo);
            this.interactionManager.clickCreativeStack(this.player.getStackInHand(Hand.MAIN_HAND), 36 + playerInventory.selectedSlot);
        }
        else if (PlayerInventory.isValidHotbarIndex(slotToSwapTo)) {
            playerInventory.selectedSlot = slotToSwapTo;
        }
        else {
            int numRowsToSwitch = slotToSwapTo == -1 ? 1 : 1 + (35 - slotToSwapTo) / 9;
            if(slotToSwapTo == -1) {
                while (numRowsToSwitch < 4) {
                    if(isRowEmpty(playerInventory, 36 - numRowsToSwitch * 9)) numRowsToSwitch += 1;
                    else break;
                }
            }

            if (numRowsToSwitch < 1 || numRowsToSwitch > 3) return;
            for (int i = 0; i < 9; i++) {
                if (numRowsToSwitch == 1) {
                    swapItemsFromSlots(playerInventory, i, i + 9);
                    swapItemsFromSlots(playerInventory, i, i + 18);
                    swapItemsFromSlots(playerInventory, i, i + 27);
                } else if (numRowsToSwitch == 2) {
                    swapItemsFromSlots(playerInventory, i, i + 9);
                    swapItemsFromSlots(playerInventory, i, i + 27);
                    swapItemsFromSlots(playerInventory, i, i + 9);
                    swapItemsFromSlots(playerInventory, i, i + 18);
                } else if (numRowsToSwitch == 3) {
                    swapItemsFromSlots(playerInventory, i, i + 27);
                    swapItemsFromSlots(playerInventory, i, i + 18);
                    swapItemsFromSlots(playerInventory, i, i + 9);
                }
            }
            if(slotToSwapTo != -1) playerInventory.selectedSlot = slotToSwapTo % 9;
        }
        info.cancel();
    }


    private static boolean isRowEmpty(PlayerInventory inventory, int firstSlotOfRow) {
        for(int i = 0; i < 9; i++) {
            if(!inventory.getStack(i + firstSlotOfRow).isEmpty()) return false;
        }
        return true;
    }

    private static boolean isRowFull(PlayerInventory inventory, int firstSlotOfRow) {
        for(int i = 0; i < 9; i++) {
            if(inventory.getStack(i + firstSlotOfRow).isEmpty()) return false;
        }
        return true;
    }

    private void swapItemsFromSlots(PlayerInventory inventory, int hotbarSlot, int slotTwo) {
        ItemStack stackOne = inventory.getStack(hotbarSlot);
        ItemStack stackTwo = inventory.getStack(slotTwo);
        if(stackOne.isEmpty() && stackTwo.isEmpty()) return;
        this.interactionManager.clickSlot(0, slotTwo, hotbarSlot, SlotActionType.SWAP, player);
    }

    private ItemStack getPickedBlock(boolean isCreativeMode){
        HitResult.Type crosshairTargetType = this.crosshairTarget.getType();
        if (this.crosshairTarget == null || crosshairTargetType == HitResult.Type.MISS) return null;

        BlockEntity blockEntity;
        ItemStack itemStack;
        if (crosshairTargetType == HitResult.Type.BLOCK) {

            BlockPos blockPos = ((BlockHitResult)this.crosshairTarget).getBlockPos();
            BlockState blockState = this.world.getBlockState(blockPos);
            if (blockState.isAir()) return null;

            itemStack = blockState.getBlock().getPickStack(this.world, blockPos, blockState);
            if (itemStack.isEmpty()) return null;

            if (isCreativeMode && Screen.hasControlDown() && blockState.hasBlockEntity()) {
                blockEntity = this.world.getBlockEntity(blockPos);
                if (blockEntity != null) addBlockEntityNbt(itemStack, blockEntity, this.world.getRegistryManager());
            }
            return itemStack;

        } else if (crosshairTargetType == HitResult.Type.ENTITY && isCreativeMode) {
            Entity entity = ((EntityHitResult)this.crosshairTarget).getEntity();
            return entity.getPickBlockStack();
        }
        return null;
    }
}
