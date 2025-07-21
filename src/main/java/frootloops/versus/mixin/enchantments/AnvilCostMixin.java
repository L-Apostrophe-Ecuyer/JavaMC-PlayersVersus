package frootloops.versus.mixin.enchantments;

import frootloops.versus.VersusMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EnchantableComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.ForgingSlotsManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(value = AnvilScreenHandler.class, priority = 999)
public abstract class AnvilCostMixin extends ForgingScreenHandler {

    @Shadow
    private String newItemName;

    @Shadow
    private int repairItemUsage;

    @Shadow
    private final Property levelCost = Property.create();

    ItemStack toolStack;
    ItemStack repairStack;

    public AnvilCostMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, ForgingSlotsManager forgingSlotsManager) {
        super(type, syncId, playerInventory, context, forgingSlotsManager);
    }

    @Override
    public boolean canTakeOutput(PlayerEntity player, boolean present) {
        return (player.getAbilities().creativeMode || player.experienceLevel >= this.levelCost.get()) && !output.getStack(0).isEmpty();
    }

    @Redirect(method = "onTakeOutput", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;addExperienceLevels(I)V"))
    public void levelsOnlyUsedWhenUsingBooks(PlayerEntity player, int lvlCost) {
        if(repairStack.isOf(Items.ENCHANTED_BOOK)) {
            player.addExperienceLevels(lvlCost);
            player.getWorld().playSoundAtBlockCenter(player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.5f, 1f, true);
        }
        return;
    }


    @Overwrite
    public void updateResult() {
        boolean isUpdatingName = isRenaming();
        boolean isUpdatingItems = (toolStack != input.getStack(0) || repairStack != input.getStack(1));
        if(!isUpdatingItems && !isUpdatingName) return;

        repairItemUsage = 1;
        toolStack = input.getStack(0);
        repairStack = input.getStack(1);

        ItemStack resultStack;
        if(isUpdatingItems || (isUpdatingName && !output.getStack(0).isOf(toolStack.getItem()))) resultStack = toolStack.copy();
        else resultStack = output.getStack(0);

        boolean canSmithResult = false;
        int newRepairCost = Math.max(toolStack.getOrDefault(DataComponentTypes.REPAIR_COST, 0), repairStack.getOrDefault(DataComponentTypes.REPAIR_COST, 0));
        int levelRequiredToSmith = newRepairCost;

        // DURABILITY
        boolean isRepairing = isUpdatingItems && (toolStack.isDamageable() && toolStack.getDamage() > 0) && (toolStack.isOf(repairStack.getItem()) || toolStack.canRepairWith(repairStack));
        if(isRepairing) {
            int toolUsesLeft = toolStack.getMaxDamage() - toolStack.getDamage();
            int repairUsesLeft = repairStack.isDamageable()? repairStack.getMaxDamage() - repairStack.getDamage(): 0;

            int durabilityRecoveryChunk = toolStack.getMaxDamage() * 3 / (10 + levelRequiredToSmith/2);
            int durabilityRecovered = repairUsesLeft + durabilityRecoveryChunk;
            int resultDamage = resultStack.getMaxDamage() - (toolUsesLeft + durabilityRecovered);

            if(resultDamage > 0 && repairStack.getCount() > 1) {
                for (int i = 2; i <= repairStack.getCount(); i++) {
                    resultDamage = resultDamage - durabilityRecoveryChunk;
                    repairItemUsage++;
                    if(resultDamage <= 0) break;
                }
            }

            if (resultDamage < 0) resultDamage = 0;
            if (resultDamage < resultStack.getDamage() && !resultStack.isEmpty()) {
                newRepairCost = levelRequiredToSmith + repairItemUsage;
                resultStack.setDamage(resultDamage);
                levelRequiredToSmith += 1 + newRepairCost;
                canSmithResult = true;
            }
        }
        else if (repairStack.isOf(toolStack.getItem()) && toolStack.getDamage() > 0) {
            int durabilityOnRepairStack = Math.min(repairStack.getMaxDamage() - repairStack.getDamage(), repairStack.getMaxDamage() / 4);
            int resultDamage = Math.max(toolStack.getDamage(), toolStack.getDamage() - durabilityOnRepairStack);
            if(durabilityOnRepairStack > 0) {
                newRepairCost = levelRequiredToSmith + repairItemUsage;
                resultStack.setDamage(resultDamage);
                levelRequiredToSmith += 1 + newRepairCost;
                canSmithResult = true;
            }
        }

        // ENCHANTMENTS:
        boolean isUsingBook = repairStack.isOf(Items.ENCHANTED_BOOK);
        boolean isEnchanting = isUpdatingItems && (isUsingBook || (toolStack.isOf(repairStack.getItem()) && repairStack.hasEnchantments()));
        if(isEnchanting) {
            ItemEnchantmentsComponent.Builder toolEnchantBuilder = new ItemEnchantmentsComponent.Builder(EnchantmentHelper.getEnchantments(toolStack));
            ItemEnchantmentsComponent repairEnchantmentComponent = EnchantmentHelper.getEnchantments(repairStack);

            // For the end result:
            int numNewEnchantments = 0;
            int levelForEnchants = 1;

            // Check if we're merging with a higher level enchant:
            for (RegistryEntry<Enchantment> toolEnchant : toolEnchantBuilder.getEnchantments()) {
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
            boolean isEnchantingBook = toolStack.isOf(Items.ENCHANTED_BOOK);
            for (RegistryEntry<Enchantment> repairEnchant : repairEnchantmentComponent.getEnchantments()) {
                int levelTool = toolEnchantBuilder.getLevel(repairEnchant);
                int levelRepair = repairEnchantmentComponent.getLevel(repairEnchant);
                if(levelTool == 0 && (repairEnchant.value().isAcceptableItem(toolStack) || isEnchantingBook)) {

                    boolean canAddNewEnchant = true;
                    for (RegistryEntry<Enchantment> toolEnchant : toolEnchantBuilder.getEnchantments()) {
                        if (toolEnchant.equals(repairEnchant) || (!Enchantment.canBeCombined(repairEnchant, toolEnchant) && !isEnchantingBook)) {
                            canAddNewEnchant = false;
                            break;
                        }
                    }
                    if (canAddNewEnchant) {
                        toolEnchantBuilder.add(repairEnchant, levelRepair);
                        levelForEnchants += getLevelForApplying(repairEnchant, levelRepair);
                        numNewEnchantments++;
                    }
                }
            }

            // Set the enchantments for the result:
            if(numNewEnchantments > 0) {

                // High enchantability -> lower cost
                levelForEnchants -= getEnchantabilityRebate(toolStack, levelForEnchants);

                EnchantmentHelper.set(resultStack, toolEnchantBuilder.build());
                levelRequiredToSmith += Math.max(1, levelForEnchants);
                newRepairCost += numNewEnchantments;
                canSmithResult = true;
            }
        }

        // REPAIR COST
        if(newRepairCost > 0) {
            resultStack.set(DataComponentTypes.REPAIR_COST, newRepairCost);
        }

        // NAME
        if(isUpdatingName) {
            if(!isRepairing && !isEnchanting && !repairStack.isEmpty()) {
                canSmithResult = false;
            }
            else if (newItemName == null || StringHelper.isBlank(newItemName)) {
                if (toolStack.contains(DataComponentTypes.CUSTOM_NAME)) {
                    resultStack.remove(DataComponentTypes.CUSTOM_NAME);
                    canSmithResult = true;
                }
            } else if (!newItemName.equals(toolStack.getName().getString())) {
                resultStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(newItemName));
                canSmithResult = true;
            }
        }

        // UPDATE RESULT:
        if(canSmithResult) {
            output.setStack(0, resultStack);
            levelCost.set(Math.max(0, levelRequiredToSmith));
        }
        else {
            output.setStack(0, ItemStack.EMPTY);
            levelCost.set(0);
        }
        sendContentUpdates();
    }

    private boolean isRenaming() {
        if(input.getStack(0).isEmpty()) return false;
        if(StringHelper.isBlank(newItemName)) return input.getStack(0).contains(DataComponentTypes.CUSTOM_NAME);
        return !newItemName.equals(input.getStack(0).getName().getString());
    }

    private int getEnchantabilityRebate(ItemStack toolStack, int currentLevelRequired) {
        EnchantableComponent ench = toolStack.getOrDefault(DataComponentTypes.ENCHANTABLE, null);
        return (ench == null)? 0 : Math.min(currentLevelRequired - 1, ench.value()/2);
    }

    private static int getLevelForApplying(RegistryEntry<Enchantment> enchantment, int level) {
        if(enchantment == null || level == 0) return 0;
        int levelRequired = 0;
        if(enchantment == Enchantments.MENDING) levelRequired = 12;
        else if(enchantment == Enchantments.PROTECTION) levelRequired = 6;
        else if(enchantment.isIn(EnchantmentTags.CURSE)) levelRequired = -12;
        levelRequired += Math.max(levelRequired + enchantment.value().getMinPower(level), enchantment.value().getAnvilCost());
        return Math.clamp(levelRequired, 3, 30);
    }
}