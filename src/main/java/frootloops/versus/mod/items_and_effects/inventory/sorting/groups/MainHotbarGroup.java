package frootloops.versus.mod.items_and_effects.inventory.sorting.groups;

import frootloops.versus.mod.items_and_effects.brewing.CustomPotions;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemOrdering;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemType;
import frootloops.versus.mod.items_and_effects.inventory.sorting.SortingDebug;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The player's hotbar: one slot per role (main weapon, pickaxe, axe, extra weapon, food, potion, clutch item, light
 * and building block), plus the misc items that come with them.
 */
public class MainHotbarGroup extends SortingGroup {

    private static final float MAX_BLOCK_HARDNESS = 6.0F;
    ItemSlot mainWeaponSlot = null, pickaxeSlot = null, axeSlot = null, extraWeaponSlot = null, foodSlot = null, potionSlot = null, clutchItem = null, lightSlot = null, blockSlot = null;
    private boolean hasGoodPotion = false;
    private boolean hasExcellentPotion = false;
    private boolean isBlockSlotLocked = false;

    public MainHotbarGroup() {super("GROUP: MAIN_HOTBAR");}

    public boolean hasAtLeastOneConsumable(){
        return (this.foodSlot != null || this.potionSlot != null || (this.clutchItem != null && (this.clutchItem.itemType() == ItemType.TOTEMS || this.clutchItem.itemType() == ItemType.GAPPLES)));
    }

    public boolean hasCombatItems(){
        boolean hasAtLeastOneWeapon = (this.mainWeaponSlot != null || this.extraWeaponSlot != null || this.axeSlot != null);
        if(!hasAtLeastOneWeapon) return false;
        boolean hasAtLeastTwoWeapons = (this.mainWeaponSlot != null && this.extraWeaponSlot != null) || (this.axeSlot != null && this.extraWeaponSlot != null) || (this.mainWeaponSlot != null && this.axeSlot != null) || (this.pickaxeSlot != null && this.axeSlot != null);
        return this.hasAtLeastOneConsumable() || hasAtLeastTwoWeapons;
    }

    public boolean hasBuildingItems(){
        if(isBlockSlotLocked) return true;
        if(this.blockSlot == null) return false;

        BlockState blockState = ((BlockItem)blockSlot.stack().getItem()).getBlock().defaultBlockState();
        if(blockState.getBlock().defaultDestroyTime() > MAX_BLOCK_HARDNESS && !(blockState.getBlock().getExplosionResistance() == Blocks.OBSIDIAN.getExplosionResistance() && this.canHaveObsidianAsBlock())) {
            return false;
        }
        if(this.pickaxeSlot != null) {
            Tool pickaxeComponent = pickaxeSlot.stack().get(DataComponents.TOOL);
            if(pickaxeComponent.isCorrectForDrops(blockState)) return true;
        }
        if(this.axeSlot != null) {
            Tool axeComponent = axeSlot.stack().get(DataComponents.TOOL);
            if(axeComponent.isCorrectForDrops(blockState)) return true;
        }
        return false;
    }

    private boolean canHaveObsidianAsBlock() {
        if(this.pickaxeSlot == null || pickaxeSlot.stack().getMaxDamage() < 256) return false;
        return miscItems.size() > 0 && miscItems.containsItem(Items.END_CRYSTAL);
    }

    /**
     * Whether the hotbar holds a weapon or a tool to work with.
     */
    public boolean hasWeaponOrTool() {
        if(this.mainWeaponSlot != null || this.extraWeaponSlot != null || this.pickaxeSlot != null || this.axeSlot != null) return true;
        for(int i = 0; i < miscItems.size(); i++) if(miscItems.getSlot(i).isToolOrWeapon()) return true;
        return false;
    }

    public boolean hasPickaxe(){
        return (this.pickaxeSlot != null);
    }

    public boolean hasAxe(){
        return (this.axeSlot != null && axeSlot.itemType() == ItemType.AXE);
    }

    @Override
    public void addSlot(ItemSlot slot) {
        slot = tryAddingSlot(slot);
        if(slot != null) this.miscItems.add(slot);
    }

    @Override
    public boolean addSlots(List<ItemSlot> newSlots) {
        if(newSlots == null || newSlots.isEmpty()) return false;
        for (ItemSlot slot : newSlots) this.addSlot(slot);
        return true;
    }

    @Override
    public ItemType getItemType() {
        return ItemType.SWORD;
    }

    @Override
    public int getMaxNumRows() {
        int size = this.size();
        return size > 0 ? 1 + size/9 : 0;
    }

    @Override
    public ItemSlot tryInsertingSlot(ItemSlot slot) {
        return this.tryInsertingSlot(slot, false, false, false);
    }

    public ItemSlot tryInsertingSlot(ItemSlot slot, boolean isInDeepDark, boolean isInNether, boolean isInWater) {
        if(slot.stack().is(ItemTags.SWORDS) || (isInDeepDark && slot.stack().is(ItemTags.HOES))) {
            boolean isBetterWeapon = this.mainWeaponSlot == null || ItemOrdering.shouldGoBefore(slot, mainWeaponSlot, true);
            if(isInDeepDark && this.mainWeaponSlot != null) {
                boolean isInsertingHoe = slot.stack().is(ItemTags.HOES);
                if(isInsertingHoe && !mainWeaponSlot.stack().is(ItemTags.HOES)) isBetterWeapon = true;
                if(!isInsertingHoe && mainWeaponSlot.stack().is(ItemTags.HOES)) isBetterWeapon = false;
            }
            if(isBetterWeapon) {
                ItemSlot itemSlotToReturn = this.mainWeaponSlot;
                this.mainWeaponSlot = slot;
                return this.logInsert(slot, itemSlotToReturn, "main weapon");
            }
        }
        else if(slot.isWeaponOrShield()) {
            if(this.extraWeaponSlot == null || ItemOrdering.shouldGoBefore(slot, extraWeaponSlot, true)) {
                ItemSlot itemSlotToReturn = this.extraWeaponSlot;
                this.extraWeaponSlot = slot;
                return this.logInsert(slot, itemSlotToReturn, "weapon");
            }
        }
        else if(slot.isFood()) {
            if(this.foodSlot == null || ItemOrdering.shouldGoBefore(slot, foodSlot)) {
                ItemSlot itemSlotToReturn = this.foodSlot;
                this.foodSlot = slot;
                return this.logInsert(slot, itemSlotToReturn, "food");
            }
        }
        else if(!this.hasExcellentPotion && slot.stack().has(DataComponents.POTION_CONTENTS)) {
            Optional<Holder<Potion>> potionID = slot.stack().getComponents().get(DataComponents.POTION_CONTENTS).potion();
            if(potionID.isEmpty()) return slot;
            boolean isExcellentPotion = (potionID.get() == Potions.LONG_FIRE_RESISTANCE || potionID.get() == Potions.STRONG_STRENGTH || potionID.get() == Potions.STRONG_SWIFTNESS);
            boolean isGoodPotion = isExcellentPotion || potionID.get() == Potions.FIRE_RESISTANCE || potionID.get() == Potions.STRENGTH || potionID.get() == Potions.LONG_STRENGTH || potionID.get() == Potions.SWIFTNESS || potionID.get() == Potions.LONG_SWIFTNESS || potionID.get() == Potions.HEALING || potionID.get() == Potions.STRONG_HEALING || potionID.get() == Potions.REGENERATION || potionID.get() == Potions.STRONG_REGENERATION || potionID.get() == Potions.LONG_REGENERATION || potionID.get() == Potions.STRONG_TURTLE_MASTER || potionID.get() == Potions.LONG_TURTLE_MASTER || potionID.get() == CustomPotions.HASTE_STRONG;
            // Situational potions:
            if(isInNether) isExcellentPotion = (potionID.get() == Potions.FIRE_RESISTANCE || potionID.get() == CustomPotions.FIRE_RESISTANCE_STRONG);
            if(isInWater) isExcellentPotion = (potionID.get() == Potions.WATER_BREATHING || potionID.get() == Potions.LONG_WATER_BREATHING);
            if(this.potionSlot == null || (isExcellentPotion && !this.hasExcellentPotion) || (isGoodPotion && !this.hasGoodPotion)) {
                ItemSlot itemSlotToReturn = this.potionSlot;
                this.potionSlot = slot;
                this.hasGoodPotion = isGoodPotion || isExcellentPotion;
                this.hasExcellentPotion = isExcellentPotion;
                return this.logInsert(slot, itemSlotToReturn, "potion");
            }
        }
        else if(slot.stack().is(Items.TORCH) || slot.stack().is(Items.SOUL_TORCH) || slot.stack().is(Items.LANTERN) || slot.stack().is(Items.SOUL_LANTERN)) {
            if(this.lightSlot == null || this.lightSlot.stack().getCount() < slot.stack().getCount()) {
                ItemSlot itemSlotToReturn = this.lightSlot;
                this.lightSlot = slot;
                return this.logInsert(slot, itemSlotToReturn, "light source");
            }
        }
        else if(slot.itemType() == ItemType.TOTEMS && (this.clutchItem == null || this.clutchItem.itemType() != ItemType.TOTEMS)) {
            ItemSlot itemSlotToReturn = this.clutchItem;
            this.clutchItem = slot;
            return this.logInsert(slot, itemSlotToReturn, "totem, for clutch item");
        }
        else if(slot.itemType() == ItemType.GAPPLES && (this.clutchItem == null || ItemOrdering.shouldGoBefore(slot, clutchItem))) {
            ItemSlot itemSlotToReturn = this.clutchItem;
            this.clutchItem = slot;
            return this.logInsert(slot, itemSlotToReturn, "gapple, for clutch item");
        }
        else if(isInDeepDark) {
            if(slot.itemType() == ItemType.SHEARS) {
                ItemSlot itemSlotToReturn = this.clutchItem;
                this.clutchItem = slot;
                return this.logInsert(slot, itemSlotToReturn, "deep dark clutch item");
            }
            else if(slot.stack().getItem() instanceof BlockItem blockItem && blockItem.getBlock().defaultBlockState().is(BlockTags.OCCLUDES_VIBRATION_SIGNALS)) {
                if(blockSlot == null || blockSlot.stack().getCount() <= slot.stack().getCount()) {
                    ItemSlot itemSlotToReturn = this.blockSlot;
                    this.blockSlot = slot;
                    this.isBlockSlotLocked = true;
                    return this.logInsert(slot, itemSlotToReturn, "deep dark block");
                }
            }
            else if(slot.isUsedToClutch() && (clutchItem == null || (clutchItem.itemType() != ItemType.SHEARS && ItemOrdering.shouldGoBefore(slot, clutchItem)))) {
                ItemSlot itemSlotToReturn = this.clutchItem;
                this.clutchItem = slot;
                return this.logInsert(slot, itemSlotToReturn, "deep dark clutch item");
            }
        }
        else if(isInNether) {
            if(slot.stack().is(Items.POWDER_SNOW_BUCKET) || slot.stack().is(Items.WARPED_FUNGUS_ON_A_STICK)) {
                ItemSlot itemSlotToReturn = this.clutchItem;
                this.clutchItem = slot;
                return this.logInsert(slot, itemSlotToReturn, "nether clutch item");
            }
            else if(slot.isUsedToClutch() && (clutchItem == null || (!clutchItem.stack().is(Items.POWDER_SNOW_BUCKET) && !clutchItem.stack().is(Items.WARPED_FUNGUS_ON_A_STICK) && ItemOrdering.shouldGoBefore(slot, clutchItem)))) {
                ItemSlot itemSlotToReturn = this.clutchItem;
                this.clutchItem = slot;
                return this.logInsert(slot, itemSlotToReturn, "nether clutch item");
            }
        }
        else if(isInWater) {
            if(slot.stack().is(Items.FILLED_MAP) || slot.stack().is(Items.MAGMA_BLOCK)) {
                ItemSlot itemSlotToReturn = this.clutchItem;
                this.clutchItem = slot;
                return this.logInsert(slot, itemSlotToReturn, "ocean clutch item");
            }
            else if(slot.isUsedToClutch() && (clutchItem == null || (!clutchItem.stack().is(Items.FILLED_MAP) && !clutchItem.stack().is(Items.MAGMA_BLOCK) && ItemOrdering.shouldGoBefore(slot, clutchItem)))) {
                ItemSlot itemSlotToReturn = this.clutchItem;
                this.clutchItem = slot;
                return this.logInsert(slot, itemSlotToReturn, "ocean clutch item");
            }
        }
        else if(slot.isUsedToClutch() && (this.clutchItem == null || ItemOrdering.shouldGoBefore(slot, this.clutchItem))) {
            ItemSlot itemSlotToReturn = this.clutchItem;
            this.clutchItem = slot;
            return this.logInsert(slot, itemSlotToReturn, "clutch item");
        }
        return slot;
    }

    /** Logs a slot taking a role in the hotbar, and returns the slot it replaced. */
    private ItemSlot logInsert(ItemSlot slot, ItemSlot replaced, String role) {
        SortingDebug.log(() -> "              -> Inserting " + slot + " into " + this.GROUP_NAME + (replaced == null ? " as " + role : ", replacing " + replaced));
        return replaced;
    }

    private ItemSlot tryAddingSlot(ItemSlot slot) {
        slot = this.tryInsertingSlot(slot);
        if(slot != null) {
            if(slot.itemType() == ItemType.PICKAXE) {
                if(this.pickaxeSlot == null || ItemOrdering.shouldGoBefore(slot, pickaxeSlot)) {
                    ItemSlot slotToReplace = this.pickaxeSlot;
                    this.pickaxeSlot = slot;
                    return this.logInsert(slot, slotToReplace, "pickaxe");
                }
            }
            else if(slot.itemType() == ItemType.AXE) {
                if(this.axeSlot == null || ItemOrdering.shouldGoBefore(slot, axeSlot)) {
                    ItemSlot slotToReplace = this.axeSlot;
                    this.axeSlot = slot;
                    return this.logInsert(slot, slotToReplace, "axe");
                }
                else if(this.extraWeaponSlot == null || (extraWeaponSlot.itemType() == ItemType.AXE && ItemOrdering.shouldGoBefore(slot, extraWeaponSlot))) {
                    ItemSlot slotToReplace = this.extraWeaponSlot;
                    this.extraWeaponSlot = slot;
                    return this.logInsert(slot, slotToReplace, "special weapon");
                }
            }
            else if(slot.isToolOrWeapon()) {
                boolean mustBeWeapon = this.extraWeaponSlot == null || (slot.isWeapon() && !extraWeaponSlot.isWeapon());
                boolean cannotBeWeapon = !mustBeWeapon && extraWeaponSlot != null && extraWeaponSlot.isWeapon() && !slot.isWeapon();
                if(!cannotBeWeapon && (mustBeWeapon || ItemOrdering.shouldGoBefore(slot, extraWeaponSlot))) {
                    ItemSlot slotToReplace = this.extraWeaponSlot;
                    this.extraWeaponSlot = slot;
                    return this.logInsert(slot, slotToReplace, "extra weapon");
                }
            }
            else if(slot.itemType() == ItemType.BLOCK_FULL || slot.itemType() == ItemType.BLOCK_WORKSTATION) {
                if(this.blockSlot == null) {
                    this.blockSlot = slot;
                    return this.logInsert(slot, null, "block");
                }
                else if(!isBlockSlotLocked && !blockSlot.stack().is(ItemTags.WOOL) && !ItemOrdering.shouldGoBefore(blockSlot, slot)) {
                    ItemSlot oldBlockSlot = blockSlot;
                    this.blockSlot = slot;
                    return this.logInsert(slot, oldBlockSlot, "block");
                }
            }
        }
        return slot;
    }

    @Override
    protected void addSlotsToMisc(List<ItemSlot> newSlots) {
        for (ItemSlot slot : newSlots) {
            int pos = miscItems.add(slot);
            SortingDebug.log(() -> "              -> Inserting " + slot + " into " + this.GROUP_NAME + "'s misc items at pos " + pos + ": " + miscItems);
        }
    }

    private List<ItemSlot> takeImportantItems(int numSlotsToTake) {
        List<ItemSlot> slotsTaken = new ArrayList<>();
        if(this.mainWeaponSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(mainWeaponSlot);
            mainWeaponSlot = null;
            numSlotsToTake--;
        }
        if(this.pickaxeSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(pickaxeSlot);
            pickaxeSlot = null;
            numSlotsToTake--;
        }
        if(this.axeSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(axeSlot);
            axeSlot = null;
            numSlotsToTake--;
        }
        if(this.extraWeaponSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(extraWeaponSlot);
            extraWeaponSlot = null;
            numSlotsToTake--;
        }
        if(this.potionSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(potionSlot);
            potionSlot = null;
            numSlotsToTake--;
        }
        if(this.foodSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(foodSlot);
            foodSlot = null;
            numSlotsToTake--;
        }
        if(this.clutchItem != null && numSlotsToTake > 0) {
            slotsTaken.add(clutchItem);
            clutchItem = null;
            numSlotsToTake--;
        }
        if(this.lightSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(lightSlot);
            lightSlot = null;
            numSlotsToTake--;
        }
        if(this.blockSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(blockSlot);
            blockSlot = null;
        }
        return slotsTaken;
    }

    @Override
    public List<ItemSlot> takeAllItems() {
        for (ItemSlot slot : this.takeImportantItems(Integer.MAX_VALUE)) this.miscItems.addBetween(slot, 0, miscItems.size());
        return miscItems.takeAll();
    }

    @Override
    public int getNextListSize() {
        return this.size();
    }

    @Override
    public List<ItemSlot> takeNextList() {
        return this.takeAllItems();
    }

    @Override
    public void tryFormingRows() {} // Do nothing!

    @Override
    public List<ItemSlot> takeFirstSlots(int numSlotsToTake, boolean splitUpSubgroups) {
        if(numSlotsToTake == this.size()) return this.takeAllItems();
        if(numSlotsToTake == this.size() - this.miscItems.size()) return this.takeImportantItems(numSlotsToTake);
        if(numSlotsToTake == this.miscItems.size()) return this.miscItems.takeAll();
        if(!splitUpSubgroups) return new ArrayList<>();

        List<ItemSlot> slotsTaken = this.takeImportantItems(numSlotsToTake);
        slotsTaken.addAll(this.miscItems.take(numSlotsToTake - slotsTaken.size()));
        return slotsTaken;
    }

    @Override
    public List<ItemSlot> tryTakingExactNumSlots(int numSlotsToTake, boolean withTools, boolean startFromEnd) {
        if(numSlotsToTake == this.size() || numSlotsToTake == 9) return this.takeAllItems();
        if(numSlotsToTake == this.size() - this.miscItems.size()) return this.takeImportantItems(numSlotsToTake);
        if(numSlotsToTake == this.miscItems.size()) return this.miscItems.takeAll();
        return new ArrayList<>();
    }

    /**
     * Drops what doesn't make a useful hotbar, and returns it to be sorted into the other groups.
     */
    public List<ItemSlot> keepOnlyEssentials() {
        List<ItemSlot> itemsToRemove = new ArrayList<>();
        boolean hasEnoughCombatItems = this.hasCombatItems();
        if(!hasEnoughCombatItems) {
            // The main weapon stays: without food or a second weapon, it's still the one to hold.
            SortingDebug.log(() -> "              -> Hotbar: Not enough combat items!");
            if(this.extraWeaponSlot != null) itemsToRemove.add(this.extraWeaponSlot);
            if(this.potionSlot != null) itemsToRemove.add(this.potionSlot);
            if(this.clutchItem != null) itemsToRemove.add(this.clutchItem);
            this.extraWeaponSlot = null;
            this.potionSlot = null;
            this.clutchItem = null;
        }
        boolean hasEnoughBuildingItems = this.hasBuildingItems();
        if(!hasEnoughBuildingItems && !isBlockSlotLocked) {
            SortingDebug.log(() -> "              -> Hotbar: Not enough building items!");
            if(this.blockSlot != null) itemsToRemove.add(this.blockSlot);
            this.blockSlot = null;
        }
        if(!hasEnoughCombatItems && !hasEnoughBuildingItems) { // No valid hotbar group
            SortingDebug.log(() -> "              -> Hotbar: Not enough items to make a good hotbar, sorting extras back into groups");
            if(this.lightSlot != null) itemsToRemove.add(this.lightSlot);
            this.lightSlot = null;
            if(this.miscItems.size() > 0) itemsToRemove.addAll(miscItems.takeAll());
        }
        if(this.miscItems.size() < 3 && this.size() != 9) itemsToRemove.addAll(miscItems.takeAll());
        return itemsToRemove;
    }

    /**
     * Gives back misc stacks, least important first, until the hotbar holds at most {@code maxSize} stacks.
     */
    public List<ItemSlot> trimTo(int maxSize) {
        List<ItemSlot> removed = new ArrayList<>();
        while(this.size() > maxSize && this.miscItems.size() > 0) removed.add(this.miscItems.removeLast());
        return removed;
    }

    @Override
    public int size() {
        int currentSize = 0;
        if(this.mainWeaponSlot != null) currentSize++;
        if(this.pickaxeSlot != null) currentSize++;
        if(this.axeSlot != null) currentSize++;
        if(this.extraWeaponSlot != null) currentSize++;
        if(this.foodSlot != null) currentSize++;
        if(this.potionSlot != null) currentSize++;
        if(this.clutchItem != null) currentSize++;
        if(this.lightSlot != null) currentSize++;
        if(this.blockSlot != null) currentSize++;
        return currentSize + this.miscItems.size();
    }

    @Override
    public String toString() {
        if( this.size() == 0 ) return "";
        StringBuilder hotBarOutput = new StringBuilder();
        if(this.mainWeaponSlot != null) hotBarOutput.append(this.mainWeaponSlot).append(" (Weapon), ");
        if(this.pickaxeSlot != null) hotBarOutput.append(this.pickaxeSlot).append(" (Pick), ");
        if(this.axeSlot != null) hotBarOutput.append(this.axeSlot).append(" (Axe), ");
        if(this.extraWeaponSlot != null) hotBarOutput.append(this.extraWeaponSlot).append(" (Special), ");
        if(this.foodSlot != null) hotBarOutput.append(this.foodSlot).append(" (Food), ");
        if(this.potionSlot != null) hotBarOutput.append(this.potionSlot).append(" (Potion), ");
        if(this.clutchItem != null) hotBarOutput.append(this.clutchItem).append(" (Clutch), ");
        if(this.lightSlot != null) hotBarOutput.append(this.lightSlot).append(" (Light), ");
        if(this.blockSlot != null) hotBarOutput.append(this.blockSlot).append(" (Block), ");
        String output = "\n              [ SORTED " + this.GROUP_NAME + " ] (Size: " + this.size() + ")\n";
        if(!hotBarOutput.isEmpty()) output += "              Hotbar List : " + hotBarOutput + "\n";
        if(this.miscItems.size() > 0) output += "              Other Items List: " + this.miscItems + "\n";
        return output;
    }
}
