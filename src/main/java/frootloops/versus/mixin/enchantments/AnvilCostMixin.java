package frootloops.versus.mixin.enchantments;

import frootloops.versus.VersusMod;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.*;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(value = AnvilScreenHandler.class)
public abstract class AnvilCostMixin extends ForgingScreenHandler {

    @Shadow
    private String newItemName;

    @Shadow
    private int repairItemUsage;

    @Shadow
    private final Property levelCost = Property.create();

    public AnvilCostMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    @Override
    public boolean canTakeOutput(PlayerEntity player, boolean present) {
        return (player.getAbilities().creativeMode || player.experienceLevel >= this.levelCost.get()) && !output.getStack(0).isEmpty();
    }


    @Overwrite
    public void updateResult() {

        repairItemUsage = 1;
        output.setStack(0, ItemStack.EMPTY);
        ItemStack toolStack = input.getStack(0);
        ItemStack repairStack = input.getStack(1);

        boolean canSmithResult = false;
        ItemStack resultStack = toolStack.copy();
        int levelCostValue = 0;

        // DURABILITY
        boolean isRepairing = (toolStack.isDamageable() && toolStack.getDamage() > 0) && (toolStack.isOf(repairStack.getItem()) || toolStack.getItem().canRepair(toolStack, repairStack));
        if(isRepairing) {
            int toolUsesLeft = toolStack.getMaxDamage() - toolStack.getDamage();
            int repairUsesLeft = repairStack.isDamageable()? repairStack.getMaxDamage() - repairStack.getDamage(): 0;
            int durabilityRecoveryChunk = toolStack.getMaxDamage() * 3 / 10;
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
            if (resultDamage < resultStack.getDamage()) {
                resultStack.setDamage(resultDamage);
                resultStack.setRepairCost(resultStack.getRepairCost() + repairItemUsage);
                levelCostValue += 1 + resultStack.getRepairCost()/3;
                canSmithResult = true;
            }
        }

        // ENCHANTMENTS:
        boolean isApplyingEnchantedBook = repairStack.isOf(Items.ENCHANTED_BOOK) && !EnchantedBookItem.getEnchantmentNbt(repairStack).isEmpty();
        boolean isEnchanting = (isApplyingEnchantedBook) || (repairStack.hasEnchantments() && toolStack.isOf(repairStack.getItem()));
        if(isEnchanting) {
            Map<Enchantment, Integer> enchantmentsMapTool = EnchantmentHelper.get(toolStack);
            Map<Enchantment, Integer> enchantmentsMapRepair = EnchantmentHelper.get(repairStack);

            // For the end result:
            int numEnchantsAdded = 0;
            int levelCostForEnchants = 1;

            // Check if we're merging with a higher level enchant:
            for (Enchantment toolEnchant : enchantmentsMapTool.keySet()) {
                if (toolEnchant == null) continue;
                if(enchantmentsMapRepair.containsKey(toolEnchant)) {
                    int levelTool = enchantmentsMapTool.get(toolEnchant);
                    int levelRepair = enchantmentsMapRepair.get(toolEnchant);
                    if(levelRepair > levelTool) {
                        enchantmentsMapTool.put(toolEnchant, enchantmentsMapRepair.get(toolEnchant));
                        int levelCostForUpgraded = getLevelCostForApplying(toolEnchant, levelRepair);
                        int levelCostForCurrent = getLevelCostForApplying(toolEnchant, levelTool);
                        levelCostForEnchants += Math.max(1 + levelRepair - levelTool, levelCostForUpgraded - levelCostForCurrent);
                        numEnchantsAdded++;
                    }
                }
            }

            // Check if we're adding a new enchantment:
            for (Enchantment repairEnchant : enchantmentsMapRepair.keySet()) {
                if(repairEnchant == null) continue;
                if(!enchantmentsMapTool.containsKey(repairEnchant) && repairEnchant.isAcceptableItem(toolStack)) {
                    boolean canAddNewEnchant = true;
                    for (Enchantment toolEnchant : enchantmentsMapTool.keySet()) {
                        if (toolEnchant != repairEnchant && !toolEnchant.canCombine(repairEnchant)) {
                            canAddNewEnchant = false;
                            break;
                        }
                    }
                    if (canAddNewEnchant) {
                        enchantmentsMapTool.put(repairEnchant, enchantmentsMapRepair.get(repairEnchant));
                        levelCostForEnchants += getLevelCostForApplying(repairEnchant, enchantmentsMapRepair.get(repairEnchant));
                        numEnchantsAdded++;
                    }
                }
            }

            // Set the enchantments for the result:
            if(numEnchantsAdded > 0) {
                EnchantmentHelper.set(enchantmentsMapTool, resultStack);
                levelCostValue += levelCostForEnchants / numEnchantsAdded;
                canSmithResult = true;
            }
        }

        // NAME
        if (StringUtils.isBlank(newItemName)) {
            if (toolStack.hasCustomName()) {
                resultStack.removeCustomName();
                canSmithResult = true;
            }
        } else {
            if (!newItemName.equals(toolStack.getName().getString())) {
                resultStack.setCustomName(Text.literal(newItemName));
                canSmithResult = true;
            }
        }

        // UPDATE RESULT:
        if(canSmithResult) {
            output.setStack(0, resultStack);
            levelCost.set(levelCostValue);
        }
        sendContentUpdates();
    }


    private static int getLevelCostForApplying(Enchantment enchantment, int level) {
        if(enchantment == null || level == 0) return 0;
        int rarityAdditive = 0;
        switch (enchantment.getRarity()) {
            case COMMON: {
                rarityAdditive = 1;
                break;
            }
            case UNCOMMON: {
                rarityAdditive = 2;
                break;
            }
            case RARE: {
                rarityAdditive = enchantment.isTreasure() ? 6 : 3;
                break;
            }
            case VERY_RARE: {
                rarityAdditive = enchantment.isTreasure() ? 8 : 6;
            }
        }
        return 1 + (rarityAdditive + Math.min(enchantment.getMinPower(level), 30))/4;
    }
}