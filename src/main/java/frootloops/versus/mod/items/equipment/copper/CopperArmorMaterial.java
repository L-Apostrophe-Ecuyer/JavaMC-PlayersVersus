package frootloops.versus.mod.items.equipment.copper;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class CopperArmorMaterial implements ArmorMaterial {

    @Override
    public int getDurability(ArmorItem.Type type) {
        if(type == ArmorItem.Type.BOOTS) return 13 * 9;
        if(type == ArmorItem.Type.LEGGINGS) return 15 * 9;
        if(type == ArmorItem.Type.CHESTPLATE) return 16 * 9;
        else return 11 * 9;
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        if(type == ArmorItem.Type.BOOTS) return 1;
        if(type == ArmorItem.Type.LEGGINGS) return 3;
        if(type == ArmorItem.Type.CHESTPLATE) return 5;
        else return 2;
    }

    @Override
    public int getEnchantability() {
        return -4;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_GOLD;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(Items.COPPER_INGOT);
    }

    @Override
    public String getName() {
        return "copper";
    }

    @Override
    public float getToughness() {
        return 0;
    }

    @Override
    public float getKnockbackResistance() {
        return -10f;
    }
}
