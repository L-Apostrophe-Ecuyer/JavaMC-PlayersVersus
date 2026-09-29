package frootloops.versus.mixin.enchantments;

import frootloops.versus.VersusMod;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(value = AnvilMenu.class, priority = 999)
public abstract class AnvilCostMixin extends ItemCombinerMenu {

    @Shadow
    private String itemName;

    @Shadow
    private int repairItemCountCost;

    @Shadow
    private final DataSlot cost = DataSlot.standalone();

    ItemStack toolStack;
    ItemStack repairStack;

    public AnvilCostMixin(@Nullable MenuType<?> type, int syncId, Inventory playerInventory, ContainerLevelAccess context, ItemCombinerMenuSlotDefinition forgingSlotsManager) {
        super(type, syncId, playerInventory, context, forgingSlotsManager);
    }

    @Override
    public boolean mayPickup(Player player, boolean present) {
        return (player.getAbilities().instabuild || player.experienceLevel >= this.cost.get()) && !resultSlots.getItem(0).isEmpty();
    }

    @Redirect(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;giveExperienceLevels(I)V"))
    public void levelsOnlyUsedWhenUsingBooks(Player player, int lvlCost) {
        if(repairStack.is(Items.ENCHANTED_BOOK)) {
            player.giveExperienceLevels(lvlCost);
            player.level().playSound(player, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.5f, 1f);
        }
    }


    @Overwrite
    public void createResult() {
        boolean isUpdatingName = isRenaming();
        boolean isUpdatingItems = (toolStack != inputSlots.getItem(0) || repairStack != inputSlots.getItem(1));
        if(!isUpdatingItems && !isUpdatingName) return;

        repairItemCountCost = 1;
        toolStack = inputSlots.getItem(0);
        repairStack = inputSlots.getItem(1);

        ItemStack resultStack;
        if(isUpdatingItems || (isUpdatingName && !resultSlots.getItem(0).is(toolStack.getItem()))) resultStack = toolStack.copy();
        else resultStack = resultSlots.getItem(0);

        boolean canSmithResult = false;
        int newRepairCost = Math.max(toolStack.getOrDefault(DataComponents.REPAIR_COST, 0), repairStack.getOrDefault(DataComponents.REPAIR_COST, 0));
        int levelRequiredToSmith = newRepairCost;

        // DURABILITY
        boolean isRepairing = isUpdatingItems && (toolStack.isDamageableItem() && toolStack.getDamageValue() > 0) && (toolStack.is(repairStack.getItem()) || toolStack.isValidRepairItem(repairStack));
        if(isRepairing) {
            int toolUsesLeft = toolStack.getMaxDamage() - toolStack.getDamageValue();
            int repairUsesLeft = repairStack.isDamageableItem()? repairStack.getMaxDamage() - repairStack.getDamageValue(): 0;

            int durabilityRecoveryChunk = toolStack.getMaxDamage() * 3 / (10 + levelRequiredToSmith/2);
            int durabilityRecovered = repairUsesLeft + durabilityRecoveryChunk;
            int resultDamage = resultStack.getMaxDamage() - (toolUsesLeft + durabilityRecovered);

            if(resultDamage > 0 && repairStack.getCount() > 1) {
                for (int i = 2; i <= repairStack.getCount(); i++) {
                    resultDamage = resultDamage - durabilityRecoveryChunk;
                    repairItemCountCost++;
                    if(resultDamage <= 0) break;
                }
            }

            if (resultDamage < 0) resultDamage = 0;
            if (resultDamage < resultStack.getDamageValue() && !resultStack.isEmpty()) {
                newRepairCost = levelRequiredToSmith + repairItemCountCost;
                resultStack.setDamageValue(resultDamage);
                levelRequiredToSmith += 1 + newRepairCost;
                canSmithResult = true;
            }
        }
        else if (repairStack.is(toolStack.getItem()) && toolStack.getDamageValue() > 0) {
            int durabilityOnRepairStack = Math.min(repairStack.getMaxDamage() - repairStack.getDamageValue(), repairStack.getMaxDamage() / 4);
            int resultDamage = Math.max(toolStack.getDamageValue(), toolStack.getDamageValue() - durabilityOnRepairStack);
            if(durabilityOnRepairStack > 0) {
                newRepairCost = levelRequiredToSmith + repairItemCountCost;
                resultStack.setDamageValue(resultDamage);
                levelRequiredToSmith += 1 + newRepairCost;
                canSmithResult = true;
            }
        }

        // ENCHANTMENTS:
        boolean isUsingBook = repairStack.is(Items.ENCHANTED_BOOK);
        boolean isEnchanting = isUpdatingItems && (isUsingBook || (toolStack.is(repairStack.getItem()) && repairStack.isEnchanted()));
        if(isEnchanting) {
            ItemEnchantments.Mutable toolEnchantBuilder = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(toolStack));
            ItemEnchantments repairEnchantmentComponent = EnchantmentHelper.getEnchantmentsForCrafting(repairStack);

            // For the end result:
            int numNewEnchantments = 0;
            int levelForEnchants = 1;

            // Check if we're merging with a higher level enchant:
            for (Holder<Enchantment> toolEnchant : toolEnchantBuilder.keySet()) {
                int levelTool = toolEnchantBuilder.getLevel(toolEnchant);
                int levelRepair = repairEnchantmentComponent.getLevel(toolEnchant);
                if(isUsingBook && levelRepair == levelTool) levelRepair = Math.min(toolEnchant.value().getMaxLevel(), levelRepair + 1); // Improve enchantments by combining them
                if(levelRepair > levelTool) {
                    toolEnchantBuilder.set(toolEnchant, levelRepair);
                    numNewEnchantments++;
                }
                if(isUsingBook) levelForEnchants += getLevelForApplying(toolEnchant, Math.clamp(1 + levelRepair - levelTool, 0, levelRepair));
                else levelForEnchants += getLevelForApplying(toolEnchant, Math.max(levelRepair, levelTool));
            }

            // Check if we're adding a new enchantment:
            boolean isEnchantingBook = toolStack.is(Items.ENCHANTED_BOOK);
            for (Holder<Enchantment> repairEnchant : repairEnchantmentComponent.keySet()) {
                int levelTool = toolEnchantBuilder.getLevel(repairEnchant);
                int levelRepair = repairEnchantmentComponent.getLevel(repairEnchant);
                if(levelTool == 0 && (repairEnchant.value().canEnchant(toolStack) || isEnchantingBook)) {

                    boolean canAddNewEnchant = true;
                    for (Holder<Enchantment> toolEnchant : toolEnchantBuilder.keySet()) {
                        if (toolEnchant.equals(repairEnchant) || (!Enchantment.areCompatible(repairEnchant, toolEnchant) && !isEnchantingBook)) {
                            canAddNewEnchant = false;
                            break;
                        }
                    }
                    if (canAddNewEnchant) {
                        toolEnchantBuilder.upgrade(repairEnchant, levelRepair);
                        levelForEnchants += getLevelForApplying(repairEnchant, levelRepair);
                        numNewEnchantments++;
                    }
                }
            }

            // Set the enchantments for the result:
            if(numNewEnchantments > 0) {

                // High enchantability -> lower cost
                levelForEnchants -= getEnchantabilityRebate(toolStack, levelForEnchants);

                EnchantmentHelper.setEnchantments(resultStack, toolEnchantBuilder.toImmutable());
                levelRequiredToSmith += Math.max(1, levelForEnchants);
                newRepairCost += numNewEnchantments;
                canSmithResult = true;
            }
        }

        // REPAIR COST
        if(newRepairCost > 0) {
            resultStack.set(DataComponents.REPAIR_COST, newRepairCost);
        }

        // NAME
        if(isUpdatingName) {
            if(!isRepairing && !isEnchanting && !repairStack.isEmpty()) {
                canSmithResult = false;
            }
            else if (itemName == null || StringUtil.isBlank(itemName)) {
                if (toolStack.has(DataComponents.CUSTOM_NAME)) {
                    resultStack.remove(DataComponents.CUSTOM_NAME);
                    canSmithResult = true;
                }
            } else if (!itemName.equals(toolStack.getHoverName().getString())) {
                resultStack.set(DataComponents.CUSTOM_NAME, Component.literal(itemName));
                canSmithResult = true;
            }
        }

        // UPDATE RESULT:
        if(canSmithResult) {
            resultSlots.setItem(0, resultStack);
            cost.set(Math.max(0, levelRequiredToSmith));
        }
        else {
            resultSlots.setItem(0, ItemStack.EMPTY);
            cost.set(0);
        }
        broadcastChanges();
    }

    private boolean isRenaming() {
        if(inputSlots.getItem(0).isEmpty()) return false;
        if(StringUtil.isBlank(itemName)) return inputSlots.getItem(0).has(DataComponents.CUSTOM_NAME);
        return !itemName.equals(inputSlots.getItem(0).getHoverName().getString());
    }

    private int getEnchantabilityRebate(ItemStack toolStack, int currentLevelRequired) {
        Enchantable ench = toolStack.getOrDefault(DataComponents.ENCHANTABLE, null);
        return (ench == null)? 0 : Math.min(currentLevelRequired - 1, ench.value()/2);
    }

    private static int getLevelForApplying(Holder<Enchantment> enchantment, int level) {
        if(enchantment == null || level == 0) return 0;
        int levelRequired = 0;
        if(enchantment == Enchantments.MENDING) levelRequired = 12;
        else if(enchantment == Enchantments.PROTECTION) levelRequired = 6;
        else if(enchantment.is(EnchantmentTags.CURSE)) levelRequired = -12;
        levelRequired += Math.max(levelRequired + enchantment.value().getMinCost(level), enchantment.value().getAnvilCost());
        return Math.clamp(levelRequired, 3, 30);
    }
}