package frootloops.versus.mod.items.equipment;

import frootloops.versus.VersusMod;
import net.minecraft.entity.Entity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public class DecayableArmorItem extends ArmorItem {

    private final Item DAMAGED_VERSION;

    public DecayableArmorItem(ArmorMaterial material, EquipmentType type, Settings settings, Item damagedVersion) {
        super(material, type, settings);
        DAMAGED_VERSION = damagedVersion;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if(selected || world.isClient() || world.getTime() % 1200L != 0L || stack.getDamage() < 32) return;
        if(entity instanceof ServerPlayerEntity serverPlayer && world.getRandom().nextInt(stack.getMaxDamage()) < stack.getDamage()) {
            if(slot < 4 && serverPlayer.getInventory().getArmorStack(slot).equals(stack)) {
                serverPlayer.getInventory().armor.set(slot, stack.withItem(DAMAGED_VERSION));
                serverPlayer.getInventory().markDirty();
            }
            else {
                serverPlayer.getInventory().setStack(slot, stack.withItem(DAMAGED_VERSION));
            }
        }
    }
}
