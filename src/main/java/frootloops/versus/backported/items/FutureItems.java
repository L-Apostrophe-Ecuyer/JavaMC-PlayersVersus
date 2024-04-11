package frootloops.versus.backported.items;

import frootloops.versus.backported.items.equipment.MaceItem;
import frootloops.versus.backported.items.equipment.MaceToolMaterial;
import frootloops.versus.backported.items.throwing.WindChargeItem;
import frootloops.versus.mod.Combat;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.MiningToolItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class FutureItems {

    public static final MaceToolMaterial MACE_TOOL_MATERIAL = new MaceToolMaterial();
    public static MiningToolItem MACE = new MaceItem(MACE_TOOL_MATERIAL,  5.0f - (float)Combat.PLAYER_BASE_ATTACK_DAMAGE, 1.2f - (float)Combat.PLAYER_BASE_ATTACK_SPEED, new Item.Settings());
    public static Item BREEZE_ROD = new Item(new Item.Settings());
    public static Item WIND_CHARGE = new WindChargeItem(new Item.Settings());

    public static Map<UUID, Long> LAST_WIND_CHARGE_USE_TIME = new HashMap<>();

    public static void onInitialize() {
        registerFutureItem("mace", MACE, ItemGroups.COMBAT);
        registerFutureItem("breeze_rod", BREEZE_ROD, ItemGroups.INGREDIENTS);
        registerFutureItem("wind_charge", WIND_CHARGE, ItemGroups.TOOLS);
    }

    public static Item registerFutureItem(String name, Item item, RegistryKey<ItemGroup> group) {
        if(group != null) ItemGroupEvents.modifyEntriesEvent(group).register(entries -> entries.add(item));
        return Registry.register(Registries.ITEM, new Identifier("minecraft", name), item);
    }

    public static Item registerFutureItem(String name, Item item, RegistryKey<ItemGroup> group1, RegistryKey<ItemGroup> group2) {
        if(group1 != null) ItemGroupEvents.modifyEntriesEvent(group1).register(entries -> entries.add(item));
        if(group2 != null) ItemGroupEvents.modifyEntriesEvent(group2).register(entries -> entries.add(item));
        return Registry.register(Registries.ITEM, new Identifier("minecraft", name), item);
    }
}
