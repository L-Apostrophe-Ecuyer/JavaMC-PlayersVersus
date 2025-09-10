package frootloops.versus.mixin.client.items.inventory;


import frootloops.versus.VersusMod;
import frootloops.versus.mod.items.inventory.HotbarCycling;
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
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
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



    /**
     * Pick block improved; will instead swap hotbar with a row of the inventory containing the match (or, if
     * none such row is found, will cycle to the next row.
     */
    @Inject(method = "doItemPick",at = @At("HEAD"), cancellable = true)
    private void doItemPick(CallbackInfo info) {
        if(!DO_HOTBAR_SWAPPING_ON_PICK_KEY) return;

        boolean isCreativeMode = this.player.getAbilities().creativeMode;
        boolean isCrouching = this.player.isSneaking() || Screen.hasControlDown();

        ItemStack stackToSwapTo = getPickItemStack(isCreativeMode, isCrouching);
        boolean isStackEmpty = (stackToSwapTo == null || stackToSwapTo.isEmpty());

        PlayerInventory playerInventory = this.player.getInventory();
        int slotToSwapTo = isStackEmpty ? -1: playerInventory.getSlotWithStack(stackToSwapTo);

        boolean doesPlayerAlreadyHaveStack = slotToSwapTo != -1;
        if (isCreativeMode && !doesPlayerAlreadyHaveStack && !isStackEmpty) {
            boolean hasEmptySlot = goToNextEmptySlot(playerInventory);
            if(hasEmptySlot || isCrouching) {
                if(this.crosshairTarget.getType() == HitResult.Type.BLOCK) this.interactionManager.pickItemFromBlock(((BlockHitResult)this.crosshairTarget).getBlockPos(), isCrouching);
                else if(this.crosshairTarget.getType() == HitResult.Type.ENTITY) this.interactionManager.pickItemFromEntity(((EntityHitResult)this.crosshairTarget).getEntity(), isCrouching);
            }
        }
        else if (playerInventory.getSelectedSlot() == slotToSwapTo && !isCrouching) {
            HotbarCycling.doHotbarSwap(playerInventory);
        }
        else if (PlayerInventory.isValidHotbarIndex(slotToSwapTo)) {
            playerInventory.setSelectedSlot(slotToSwapTo);
        }
        else {
            int numRowsToSwitch = slotToSwapTo == -1 ? 1 : 1 + (35 - slotToSwapTo) / 9;
            if(slotToSwapTo == -1) {
                while (numRowsToSwitch < 4) {
                    if(isRowEmpty(playerInventory, 36 - numRowsToSwitch * 9)) numRowsToSwitch += 1;
                    else break;
                }
            }
            HotbarCycling.doHotbarSwap(playerInventory, numRowsToSwitch);
            if(slotToSwapTo != -1) playerInventory.setSelectedSlot(slotToSwapTo % 9);
        }
        info.cancel();
    }



    private static boolean goToNextEmptySlot(PlayerInventory inventory) {
        if(inventory.getStack(inventory.getSelectedSlot()).isEmpty()) return true;
        for(int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                if (inventory.getStack(row * 9 + col).isEmpty()) {
                    int numRowsToSwitch = row == 0 ? 0 : 4 - row;
                    HotbarCycling.doHotbarSwap(inventory, numRowsToSwitch);
                    inventory.setSelectedSlot(col);
                    return true;
                }
            }
        }
        return false;
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

    private ItemStack getPickItemStack(boolean isCreativeMode, boolean includeData){
        HitResult.Type crosshairTargetType = this.crosshairTarget.getType();
        if (this.crosshairTarget == null || crosshairTargetType == HitResult.Type.MISS) return null;

        ItemStack itemStack;
        if (crosshairTargetType == HitResult.Type.BLOCK) {

            BlockPos blockPos = ((BlockHitResult)this.crosshairTarget).getBlockPos();
            BlockState blockState = this.world.getBlockState(blockPos);
            if (blockState.isAir()) return null;

            itemStack = blockState.getPickStack(this.world, blockPos, includeData);
            if (itemStack.isEmpty()) return null;
            return itemStack;

        } else if (crosshairTargetType == HitResult.Type.ENTITY && isCreativeMode) {
            Entity entity = ((EntityHitResult)this.crosshairTarget).getEntity();
            return entity.getPickBlockStack();
        }
        return null;
    }
}
