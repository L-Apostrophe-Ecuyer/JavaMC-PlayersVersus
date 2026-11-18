package frootloops.versus.mod.items_and_effects.containers;

import frootloops.versus.mixin.items_and_effects.containers.ShulkerBoxBlockInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The server's side of the peek at a placed shulker box: it shows only a box the player could open now (in reach, not
 * locked to them, its lid free, no loot table still to roll, not a spectator, not under spawn protection or past the world
 * border), and only to a player with an empty main hand (the hand that opens it); and lets be taken from it only the item
 * the player saw, into their inventory as far as it has room.
 */
public final class ShulkerPeeking {

    private ShulkerPeeking() {
    }

    /** The box at this place, if the player could open it now. */
    public static Optional<ShulkerBoxBlockEntity> openable(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.level();
        if (player.isSpectator() || !player.isAlive() || !player.getMainHandItem().isEmpty() || !level.isLoaded(pos) || !player.isWithinBlockInteractionRange(pos, 1.0)) return Optional.empty();
        // Spawn protection and the world border, as for any use of a block
        if (!level.mayInteract(player, pos)) return Optional.empty();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ShulkerBoxBlock) || !(level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity box)) return Optional.empty();
        if (box.getLootTable() != null || !box.canOpen(player) || !ShulkerBoxBlockInvoker.playersVersus$canOpen(state, level, pos, box)) return Optional.empty();
        return Optional.of(box);
    }

    /** What the peek shows of the box at this place (nothing for a box the player couldn't open). */
    public static Optional<ItemContainerContents> contents(ServerPlayer player, BlockPos pos) {
        return openable(player, pos).map(ShulkerPeeking::contentsOf);
    }

    public static ItemContainerContents contentsOf(ShulkerBoxBlockEntity box) {
        List<ItemStack> items = new ArrayList<>(box.getContainerSize());
        for (int slot = 0; slot < box.getContainerSize(); slot++) items.add(box.getItem(slot));
        return ItemContainerContents.fromItems(items);
    }

    /**
     * Takes the stack in this slot into the player's inventory, as much as has room, if it is still the stack the player
     * saw there (its item and components; its count may have changed); how many. The held slot is kept free if it can be.
     */
    public static int take(ServerPlayer player, BlockPos pos, int slot, ItemStack seen) {
        Optional<ShulkerBoxBlockEntity> openable = openable(player, pos);
        if (openable.isEmpty()) return 0;
        ShulkerBoxBlockEntity box = openable.get();
        if (slot < 0 || slot >= box.getContainerSize()) return 0;
        ItemStack held = box.getItem(slot);
        if (held.isEmpty() || !ItemStack.isSameItemSameComponents(held, seen)) return 0;
        int taken = Math.min(held.getCount(), room(player.getInventory(), held));
        if (taken <= 0) return 0;
        give(player.getInventory(), box.removeItem(slot, taken));
        player.level().playSound(null, pos, SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.BLOCKS, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
        return taken;
    }

    /**
     * Puts a stack the inventory has room for into it as the game would (onto stacks of the same item, then the first free
     * slot), but past the held slot, so that the hand stays empty and the peek with it; the held slot only if nothing else
     * has room. (Vanilla's Inventory.add puts it in the first free slot, often the held one.)
     */
    private static void give(Inventory inventory, ItemStack stack) {
        List<ItemStack> items = inventory.getNonEquipmentItems();
        int most = Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize(stack));
        for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
            ItemStack there = items.get(i);
            if (!there.isEmpty() && ItemStack.isSameItemSameComponents(there, stack) && there.getCount() < most) {
                int moved = Math.min(stack.getCount(), most - there.getCount());
                there.grow(moved);
                stack.shrink(moved);
            }
        }
        int held = inventory.getSelectedSlot();
        for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
            if (i != held && items.get(i).isEmpty()) inventory.setItem(i, stack.split(most));
        }
        if (!stack.isEmpty() && items.get(held).isEmpty()) inventory.setItem(held, stack.split(most));
        inventory.setChanged();
    }

    /** How many of this stack the player's inventory (its 36 slots, not the armour's) has room for. */
    private static int room(Inventory inventory, ItemStack stack) {
        int room = 0, most = Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize(stack));
        for (ItemStack held : inventory.getNonEquipmentItems()) {
            if (held.isEmpty()) room += most;
            else if (ItemStack.isSameItemSameComponents(held, stack)) room += Math.max(0, most - held.getCount());
        }
        return room;
    }
}
