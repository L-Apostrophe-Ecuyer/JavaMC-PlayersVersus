package frootloops.versus.mod.items.equipment.bronze;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class BronzeArmorMaterial implements ArmorMaterial {

    @Override
    public int getDurability(ArmorItem.Type type) {
        if(type == ArmorItem.Type.BOOTS) return 13 * 16;
        if(type == ArmorItem.Type.LEGGINGS) return 15 * 16;
        if(type == ArmorItem.Type.CHESTPLATE) return 16 * 16;
        else return 11 * 16;
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        if(type == ArmorItem.Type.BOOTS) return 1;
        if(type == ArmorItem.Type.LEGGINGS) return 4;
        if(type == ArmorItem.Type.CHESTPLATE) return 5;
        else return 2;
    }

    @Override
    public int getEnchantability() {
        return 7;
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
        return "bronze";
    }

    @Override
    public float getToughness() {
        return 0;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.1f;
    }
}
