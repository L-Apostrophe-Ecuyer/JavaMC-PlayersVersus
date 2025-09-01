package frootloops.versus.mod.items.inventory.sorting;

import frootloops.versus.VersusMod;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

import static frootloops.versus.mod.items.inventory.sorting.InventorySortingHelper.DEBUG_SORTING_GROUPS;

public record ItemSlot(int slodId, ItemStack stack, ItemType itemType) {


    public boolean isWeapon() {
        return (itemType == ItemType.SWORD || itemType == ItemType.SPECIAL_WEAPON || itemType == ItemType.BOW || itemType == ItemType.CROSSBOW);
    }

    public boolean isWeaponOrShield() {
        return this.isWeapon() || this.itemType == ItemType.SHIELD;
    }

    public boolean isTool() {
        return this.itemType.compareTo(ItemType.HOE) < 1;
    }

    public boolean isToolOrWeapon() {
        return this.isTool() || this.isWeapon();
    }

    public boolean isArmor() {
        return this.itemType == ItemType.CHESTPLATE || this.itemType == ItemType.LEGGINGS || this.itemType == ItemType.BOOTS || this.itemType == ItemType.HELMET;
    }
    public boolean isBlock() {
        return this.itemType.compareTo(ItemType.BLOCK_FULL) >= 0 && this.itemType.compareTo(ItemType.BLOCK_OTHER) <= 0;
    }

    public boolean isFood() {
        return this.itemType == ItemType.FOOD;
    }

    public boolean hasSameType(ItemSlot slot, boolean compareBlockTypes) {
        if(slot == null) return false;
        if(!compareBlockTypes && this.isBlock()) return slot.isBlock();
        return this.itemType == slot.itemType;
    }

    @Override
    public String toString() {
        return this.stack.getName().getString();
    }
}
