package frootloops.versus.mod.items.equipment.slime;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class SlimeArmorMaterial implements ArmorMaterial {

    @Override
    public int getDurability(ArmorItem.Type type) {
        if(type == ArmorItem.Type.BOOTS) return 13 * 3;
        if(type == ArmorItem.Type.LEGGINGS) return 15 * 3;
        if(type == ArmorItem.Type.CHESTPLATE) return 16 * 3;
        else return 11 * 3;
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        if(type == ArmorItem.Type.BOOTS) return 1;
        if(type == ArmorItem.Type.LEGGINGS) return 1;
        if(type == ArmorItem.Type.CHESTPLATE) return 2;
        else return 1;
    }

    @Override
    public int getEnchantability() {
        return 12;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ENTITY_SLIME_SQUISH;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(Items.SLIME_BALL);
    }

    @Override
    public String getName() {
        return "slime";
    }

    @Override
    public float getToughness() {
        return 0;
    }

    @Override
    public float getKnockbackResistance() {
        return -0.2f;
    }
}
