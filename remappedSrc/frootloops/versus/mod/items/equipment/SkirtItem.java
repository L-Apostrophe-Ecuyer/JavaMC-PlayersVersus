package frootloops.versus.mod.items.equipment;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.registry.entry.RegistryEntry;

public class SkirtItem extends ArmorItem {

    public SkirtItem(RegistryEntry<ArmorMaterial> material, Type type, net.minecraft.item.Item.Settings settings) {
        super(material, type, settings);
    }
}
