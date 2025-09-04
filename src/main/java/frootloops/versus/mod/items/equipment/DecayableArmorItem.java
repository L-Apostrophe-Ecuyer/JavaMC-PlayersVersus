package frootloops.versus.mod.items.equipment;

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

    public DecayableArmorItem(ArmorMaterial material, EquipmentType type, Settings settings, Item damagedVersion) {
        super(settings.armor(material, type));
        DAMAGED_VERSION = damagedVersion;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        if(slot == null || world.getTime() % 1200L != 0L || stack.getDamage() < 32) return;
        if(entity instanceof ServerPlayerEntity serverPlayer && world.getRandom().nextInt(stack.getMaxDamage()) < stack.getDamage()) {
            serverPlayer.equipStack(slot, stack.withItem(DAMAGED_VERSION));
            serverPlayer.getInventory().markDirty();
        }
    }
}
