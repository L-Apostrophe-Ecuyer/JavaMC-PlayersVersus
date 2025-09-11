package frootloops.versus.mod.items.inventory.sorting;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

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

    public boolean isToolOrWeapon() { return this.itemType != ItemType.SHIELD && this.itemType.compareTo(ItemType.FISHING_ROD) < 1;}

    public boolean isUsedToClutch() {
        return this.itemType == ItemType.CLUTCH_TOOL || this.itemType == ItemType.TOTEM;
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

    public boolean isSameItem(ItemSlot other) {
        if(other == null) return false;
        if(other.stack.getMaxCount() == 1) return other.stack.getItem() == this.stack.getItem();
        return ItemStack.areItemsAndComponentsEqual(this.stack, other.stack);
    }

    public boolean hasSameType(ItemSlot other, boolean compareBlockTypes) {
        if(other == null) return false;
        if(!compareBlockTypes && this.isBlock()) return other.isBlock();
        return this.itemType == other.itemType;
    }

    public boolean isVerySimilarTo(ItemSlot other, boolean strict) {
        if(other == null) return false;
        if(this.isSameItem(other)) return true;
        if(this.hasSameType(other, true)) {
            if(this.isBlock()) {
                BlockState thisState = ((BlockItem)this.stack.getItem()).getBlock().getDefaultState();
                BlockState otherState = ((BlockItem)other.stack.getItem()).getBlock().getDefaultState();
                if(thisState.getSoundGroup() == otherState.getSoundGroup()) {
                    if(strict) return true;
                    else return thisState.getMapColor(MinecraftClient.getInstance().world, BlockPos.ORIGIN).color == otherState.getMapColor(MinecraftClient.getInstance().world, BlockPos.ORIGIN).color;
                }
            }
            else return true;
        }
        if(this.isTool()) {
            if(other.isTool()) {
                if(!strict) return true;
                return this.stack.getMaxDamage() == other.stack.getMaxDamage();
            }
            else return false;
        }
        if(!strict && this.isArmor()) return other.isArmor();
        return false;
    }

    public boolean shouldAlwaysGoBefore(ItemSlot other, boolean proritizeArmor) {
        if(other == null) return false;

        // Rubbish always in the back!
        if(!this.hasSameType(other, true)) {
            if(this.itemType.compareTo(ItemType.MISC) > 0) return this.itemType.compareTo(other.itemType) < 0;
            if(other.itemType.compareTo(ItemType.MISC) > 0) return this.itemType.compareTo(other.itemType) < 0;
        }

        // Highest count first:
        if(this.isSameItem(other)) return this.stack.getCount() > other.stack.getCount();

        // Otherwise, tools and potentially armor first:
        if(this.isToolOrWeapon()) {
            if(!other.isToolOrWeapon()) return true;
            else return ItemComparaisonHelper.shouldGoBefore(this, other);
        }
        if(other.isToolOrWeapon()) return false;
        if(proritizeArmor) {
            if(this.isArmor()) {
                if(!other.isArmor()) return true;
                else return ItemComparaisonHelper.shouldGoBefore(this, other);
            }
            if(other.isArmor()) return false;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.stack.getName().getString() + (this.stack.getCount() != stack.getMaxCount() ? "(" + stack.getCount() + ")" : "");
    }
}
