package frootloops.versus.mod.items_and_effects.containers;

import frootloops.versus.mixin.items_and_effects.containers.ShulkerBoxBlockInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
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

    /** Takes the stack in this slot into the player's inventory, as much as has room, if it is still this item; how many. */
    public static int take(ServerPlayer player, BlockPos pos, int slot, Item item) {
        Optional<ShulkerBoxBlockEntity> openable = openable(player, pos);
        if (openable.isEmpty()) return 0;
        ShulkerBoxBlockEntity box = openable.get();
        if (slot < 0 || slot >= box.getContainerSize()) return 0;
        ItemStack held = box.getItem(slot);
        if (held.isEmpty() || !held.is(item)) return 0;
        int taken = Math.min(held.getCount(), room(player.getInventory(), held));
        if (taken <= 0) return 0;
        // Only what has room, so that nothing is lost (a full inventory with infinite materials would delete the rest)
        player.getInventory().add(box.removeItem(slot, taken));
        player.level().playSound(null, pos, SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.BLOCKS, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
        return taken;
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
