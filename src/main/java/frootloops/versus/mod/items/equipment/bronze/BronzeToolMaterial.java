package frootloops.versus.mod.items.equipment.bronze;

import net.fabricmc.yarn.constants.MiningLevels;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public class BronzeToolMaterial implements ToolMaterial {
    @Override
    public int getDurability() {
        return 300;
    }

    @Override
    public float getMiningSpeedMultiplier() {
        return 11.0f;
    }

    @Override
    public float getAttackDamage() {
        return 3.0f;
    }

    @Override
    public int getMiningLevel() {
        return MiningLevels.IRON;
    }

    @Override
    public int getEnchantability() {
        return 4;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(Items.COPPER_INGOT);
    }
}
