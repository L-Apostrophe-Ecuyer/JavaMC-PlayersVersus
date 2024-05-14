package frootloops.versus.mixin.enchantments;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.*;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import java.util.Set;

@Mixin(AnvilScreenHandler.class)
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
            if (resultDamage < resultStack.getDamage() && !resultStack.isEmpty()) {
                int repairCost = resultStack.getOrDefault(DataComponentTypes.REPAIR_COST, 0) + repairItemUsage;
                resultStack.setDamage(resultDamage);
                resultStack.set(DataComponentTypes.REPAIR_COST, repairCost);
                levelCostValue += 1 + repairCost/3;
                canSmithResult = true;
            }
        }

        // ENCHANTMENTS:
        boolean isApplyingEnchantedBook = repairStack.isOf(Items.ENCHANTED_BOOK) && repairStack.hasEnchantments();
        boolean isEnchanting = (isApplyingEnchantedBook) || (repairStack.hasEnchantments() && toolStack.isOf(repairStack.getItem()));
        if(isEnchanting) {

            ItemEnchantmentsComponent toolEnchantmentComponent = toolStack.getEnchantments();
            ItemEnchantmentsComponent repairEnchantmentComponent = repairStack.getEnchantments();
            ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(EnchantmentHelper.getEnchantments(resultStack));

            Set<RegistryEntry<Enchantment>> enchantmentsMapTool = toolEnchantmentComponent.getEnchantments();
            Set<RegistryEntry<Enchantment>> enchantmentsMapRepair = repairEnchantmentComponent.getEnchantments();

            // For the end result:
            int numEnchantsAdded = 0;
            int levelCostForEnchants = 1;

            // Check if we're merging with a higher level enchant:
            for (RegistryEntry<Enchantment> toolEnchant : enchantmentsMapTool) {
                int levelTool = toolEnchantmentComponent.getLevel(toolEnchant);
                int levelRepair = repairEnchantmentComponent.getLevel(toolEnchant);
                if(levelRepair > levelTool) {
                    builder.set(toolEnchant, levelRepair);
                    int levelCostForUpgraded = getLevelCostForApplying(toolEnchant, levelRepair);
                    int levelCostForCurrent = getLevelCostForApplying(toolEnchant, levelTool);
                    levelCostForEnchants += Math.max(1 + levelRepair - levelTool, levelCostForUpgraded - levelCostForCurrent);
                    numEnchantsAdded++;
                }
            }

            // Check if we're adding a new enchantment:
            for (RegistryEntry<Enchantment> repairEnchant : enchantmentsMapRepair) {
                int levelTool = toolEnchantmentComponent.getLevel(repairEnchant);
                int levelRepair = repairEnchantmentComponent.getLevel(repairEnchant);
                if(levelTool == 0 && repairEnchant.value().isAcceptableItem(toolStack)) {
                    boolean canAddNewEnchant = true;
                    for (RegistryEntry<Enchantment> toolEnchant : builder.getEnchantments()) {
                        if (!toolEnchant.equals(repairEnchant) && !!Enchantment.canBeCombined(repairEnchant, toolEnchant)) {
                            canAddNewEnchant = false;
                            break;
                        }
                    }
                    if (canAddNewEnchant) {
                        builder.add(repairEnchant, levelRepair);
                        levelCostForEnchants += getLevelCostForApplying(repairEnchant, levelRepair);
                        numEnchantsAdded++;
                    }
                }
            }

            // Set the enchantments for the result:
            if(numEnchantsAdded > 0) {
                EnchantmentHelper.set(resultStack, builder.build());
                levelCostValue += levelCostForEnchants / numEnchantsAdded;
                canSmithResult = true;
            }
        }

        // NAME
        if (this.newItemName == null || StringHelper.isBlank(this.newItemName)) {
            if (toolStack.contains(DataComponentTypes.CUSTOM_NAME)) {
                resultStack.remove(DataComponentTypes.CUSTOM_NAME);
            }
        } else if (!this.newItemName.equals(toolStack.getName().getString())) {
            resultStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(this.newItemName));
        }

        // UPDATE RESULT:
        if(canSmithResult) {
            output.setStack(0, resultStack);
            levelCost.set(levelCostValue);
        }
        sendContentUpdates();
    }


    private static int getLevelCostForApplying(RegistryEntry<Enchantment> enchantment, int level) {
        if(enchantment == null || level == 0) return 0;

        int rarityAdditive;
        if(enchantment == Enchantments.MENDING) rarityAdditive = 11;
        else if(enchantment == Enchantments.UNBREAKING) rarityAdditive = 4;
        else rarityAdditive = enchantment.value().getAnvilCost();

        return 1 + (rarityAdditive + Math.min(enchantment.value().getMinPower(level), 30))/4;
    }
}