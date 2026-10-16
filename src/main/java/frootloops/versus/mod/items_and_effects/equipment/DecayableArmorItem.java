package frootloops.versus.mod.items_and_effects.equipment;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import org.jetbrains.annotations.Nullable;

public class DecayableArmorItem extends Item {

    private final Item DAMAGED_VERSION;
    private final int MIN_DAMAGE_TO_DECAY, DURABILITY_CAP;

    public DecayableArmorItem(ArmorMaterial material, ArmorType type, Properties settings, Item damagedVersion, int minDamageToDecay) {
        super(settings.humanoidArmor(material, type));
        DAMAGED_VERSION = damagedVersion;
        MIN_DAMAGE_TO_DECAY = minDamageToDecay;
        DURABILITY_CAP = type.getDurability(material.durability()) - minDamageToDecay;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        if(slot == null || world.getGameTime() % 1200L != 0L || stack.getDamageValue() < MIN_DAMAGE_TO_DECAY) return;
        if(entity instanceof ServerPlayer serverPlayer && world.getRandom().nextInt(DURABILITY_CAP + 1) < stack.getDamageValue()) {
            serverPlayer.setItemSlot(slot, stack.transmuteCopy(DAMAGED_VERSION));
            serverPlayer.getInventory().setChanged();
        }
    }
}
