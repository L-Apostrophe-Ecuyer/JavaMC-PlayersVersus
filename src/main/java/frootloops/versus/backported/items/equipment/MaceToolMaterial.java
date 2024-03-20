package frootloops.versus.backported.items.equipment;

import net.fabricmc.yarn.constants.MiningLevels;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public class MaceToolMaterial implements ToolMaterial {
    @Override
    public int getDurability() {
        return 256;
    }

    @Override
    public float getMiningSpeedMultiplier() {
        return 8.0f;
    }

    @Override
    public float getAttackDamage() {
        return 2.0f;
    }

    @Override
    public int getMiningLevel() {
        return MiningLevels.STONE;
    }

    @Override
    public int getEnchantability() {
        return -4;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(Items.CARROT);
    }
}
