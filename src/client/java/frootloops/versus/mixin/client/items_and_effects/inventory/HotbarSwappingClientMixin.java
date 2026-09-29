package frootloops.versus.mixin.client.items_and_effects.inventory;


import com.mojang.blaze3d.platform.WindowEventHandler;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.items_and_effects.inventory.HotbarCycling;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(value = Minecraft.class)
public abstract class HotbarSwappingClientMixin extends ReentrantBlockableEventLoop<Runnable> implements WindowEventHandler {
    @Shadow public LocalPlayer player;
    @Shadow public ClientLevel level;
    @Shadow @Nullable public HitResult hitResult;
    @Shadow @Nullable public MultiPlayerGameMode gameMode;

    public HotbarSwappingClientMixin(String string) {
        super(string);
    }

    @Shadow
    public boolean hasControlDown() {return false;}

    private static int prevItemPickTime = 0;
    private static Item prevItemPick = null;


    /**
     * Pick block improved; will instead swap hotbar with a row of the inventory containing the match (or, if
     * none such row is found, will cycle to the next row.
     */
    @Inject(method = "pickBlock",at = @At("HEAD"), cancellable = true)
    private void doItemPick(CallbackInfo info) {
        if(!VersusSettings.QOL.DO_HOTBAR_SWAPPING_ON_PICK_KEY) return;

        boolean isCreativeMode = this.player.getAbilities().instabuild;
        boolean isCrouching = this.player.isShiftKeyDown() || this.hasControlDown();

        ItemStack stackToSwapTo = getPickItemStack(isCreativeMode, isCrouching);
        boolean isStackEmpty = (stackToSwapTo == null || stackToSwapTo.isEmpty());
        int currentPickTime = player.tickCount;

        Inventory playerInventory = this.player.getInventory();
        int slotToSwapTo = isStackEmpty ? -1: playerInventory.findSlotMatchingItem(stackToSwapTo);

        boolean doesPlayerAlreadyHaveStack = slotToSwapTo != -1;
        if (isCreativeMode && !doesPlayerAlreadyHaveStack && !isStackEmpty) {
            boolean hasEmptySlot = goToNextEmptySlot(playerInventory);
            if(hasEmptySlot || isCrouching) {
                if(this.hitResult.getType() == HitResult.Type.BLOCK) this.gameMode.handlePickItemFromBlock(((BlockHitResult)this.hitResult).getBlockPos(), isCrouching);
                else if(this.hitResult.getType() == HitResult.Type.ENTITY) this.gameMode.handlePickItemFromEntity(((EntityHitResult)this.hitResult).getEntity(), isCrouching);
            }
        }
        else {
            if (!doesPlayerAlreadyHaveStack || playerInventory.getSelectedSlot() == slotToSwapTo || currentPickTime - prevItemPickTime < 6 || (currentPickTime - prevItemPickTime < 12 && stackToSwapTo.getItem() == prevItemPick)) {
                slotToSwapTo = -1; // Force a hotbar swap!
            }
            if (Inventory.isHotbarSlot(slotToSwapTo)) {
                playerInventory.setSelectedSlot(slotToSwapTo);
            } else {
                int numRowsToSwitch = slotToSwapTo == -1 ? 1 : 1 + (35 - slotToSwapTo) / 9;
                if (slotToSwapTo == -1) {
                    while (numRowsToSwitch < 4) {
                        if (isRowEmpty(playerInventory, 36 - numRowsToSwitch * 9)) numRowsToSwitch += 1;
                        else break;
                    }
                }
                HotbarCycling.doHotbarSwap(playerInventory, numRowsToSwitch);
                if (slotToSwapTo != -1) playerInventory.setSelectedSlot(slotToSwapTo % 9);
            }
        }
        prevItemPickTime = this.player.tickCount;
        prevItemPick = isStackEmpty ? null : stackToSwapTo.getItem();
        info.cancel();
    }



    private static boolean goToNextEmptySlot(Inventory inventory) {
        if(inventory.getItem(inventory.getSelectedSlot()).isEmpty()) return true;
        for(int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                if (inventory.getItem(row * 9 + col).isEmpty()) {
                    int numRowsToSwitch = row == 0 ? 0 : 4 - row;
                    HotbarCycling.doHotbarSwap(inventory, numRowsToSwitch);
                    inventory.setSelectedSlot(col);
                    return true;
                }
            }
        }
        return false;
    }


    private static boolean isRowEmpty(Inventory inventory, int firstSlotOfRow) {
        for(int i = 0; i < 9; i++) {
            if(!inventory.getItem(i + firstSlotOfRow).isEmpty()) return false;
        }
        return true;
    }

    private static boolean isRowFull(Inventory inventory, int firstSlotOfRow) {
        for(int i = 0; i < 9; i++) {
            if(inventory.getItem(i + firstSlotOfRow).isEmpty()) return false;
        }
        return true;
    }

    private ItemStack getPickItemStack(boolean isCreativeMode, boolean includeData){
        HitResult.Type crosshairTargetType = this.hitResult.getType();
        if (this.hitResult == null || crosshairTargetType == HitResult.Type.MISS) return null;

        ItemStack itemStack;
        if (crosshairTargetType == HitResult.Type.BLOCK) {

            BlockPos blockPos = ((BlockHitResult)this.hitResult).getBlockPos();
            BlockState blockState = this.level.getBlockState(blockPos);
            if (blockState.isAir()) return null;

            itemStack = blockState.getCloneItemStack(this.level, blockPos, includeData);
            if (itemStack.isEmpty()) return null;
            return itemStack;

        } else if (crosshairTargetType == HitResult.Type.ENTITY && isCreativeMode) {
            Entity entity = ((EntityHitResult)this.hitResult).getEntity();
            return entity.getPickResult();
        }
        return null;
    }
}
