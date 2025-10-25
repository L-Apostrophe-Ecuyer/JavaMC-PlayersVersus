package frootloops.versus.mod.items_and_effects.equipment;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

public class DecayableArmorItem extends Item {

    private final Item DAMAGED_VERSION;
    private final int MIN_DAMAGE_TO_DECAY, DURABILITY_CAP;

    public DecayableArmorItem(ArmorMaterial material, EquipmentType type, net.minecraft.item.Item.Settings settings, Item damagedVersion, int minDamageToDecay) {
        super(settings.armor(material, type));
        DAMAGED_VERSION = damagedVersion;
        MIN_DAMAGE_TO_DECAY = minDamageToDecay;
        DURABILITY_CAP = type.getMaxDamage(material.durability()) - minDamageToDecay;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        if(slot == null || world.getTime() % 1200L != 0L || stack.getDamage() < MIN_DAMAGE_TO_DECAY) return;
        if(entity instanceof ServerPlayerEntity serverPlayer && world.getRandom().nextInt(DURABILITY_CAP + 1) < stack.getDamage()) {
            serverPlayer.equipStack(slot, stack.withItem(DAMAGED_VERSION));
            serverPlayer.getInventory().markDirty();
        }
    }
}
