package frootloops.versus.mod.items_and_effects;

import frootloops.versus.mod.environment.CustomBlockItems;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class BurningConversion {

    public record ItemBurningConversionRecord(int itemExtraHealth, Item resultItem, Item veryHotResultItem, boolean canExtinguishFire){
    }

    public static final Map<Item, ItemBurningConversionRecord> ITEM_BURNING_CONVERSION_MAP = new HashMap<>();
    static {

        ITEM_BURNING_CONVERSION_MAP.put(Items.PAPER, new ItemBurningConversionRecord(-9999, null, null, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.STICK, new ItemBurningConversionRecord(-9999, null, null, false));

        ITEM_BURNING_CONVERSION_MAP.put(Items.SNOWBALL, new ItemBurningConversionRecord(-9999, null, null, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.SNOW, new ItemBurningConversionRecord(-70, null, null, true));
        ITEM_BURNING_CONVERSION_MAP.put(Items.ICE, new ItemBurningConversionRecord(-40, null, null, true));
        ITEM_BURNING_CONVERSION_MAP.put(Items.PACKED_ICE, new ItemBurningConversionRecord(-20, Items.ICE, null, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.BLUE_ICE, new ItemBurningConversionRecord(-10, Items.PACKED_ICE, Items.ICE, false));

        ITEM_BURNING_CONVERSION_MAP.put(Items.BRICK, new ItemBurningConversionRecord(120, null, null, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.TERRACOTTA, new ItemBurningConversionRecord(20, null, null, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CLAY_BALL, new ItemBurningConversionRecord(200, Items.BRICK, null, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CLAY, new ItemBurningConversionRecord(200, Items.TERRACOTTA, Items.DYED_TERRACOTTA.black(), false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomBlockItems.BROWN_MUD, new ItemBurningConversionRecord(10, Items.PACKED_MUD, Items.PACKED_MUD, false));

        ITEM_BURNING_CONVERSION_MAP.put(Items.GOLD_NUGGET, new ItemBurningConversionRecord(240, null, null, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.GOLD_BLOCK, new ItemBurningConversionRecord(180, Items.GOLD_INGOT, Items.GOLD_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.GOLD_INGOT, new ItemBurningConversionRecord(120, Items.GOLD_NUGGET, Items.GOLD_NUGGET, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.RAW_GOLD_BLOCK, new ItemBurningConversionRecord(240, Items.RAW_GOLD, Items.GOLD_BLOCK, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.RAW_GOLD, new ItemBurningConversionRecord(180, null, Items.GOLD_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.GOLD_ORE, new ItemBurningConversionRecord(180, null, Items.GOLD_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DEEPSLATE_GOLD_ORE, new ItemBurningConversionRecord(180, null, Items.GOLD_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.NETHER_GOLD_ORE, new ItemBurningConversionRecord(360, Items.GOLD_NUGGET, Items.GOLD_NUGGET, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.GOLDEN_AXE, new ItemBurningConversionRecord(60, Items.GOLD_NUGGET, Items.GOLD_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.GOLDEN_PICKAXE, new ItemBurningConversionRecord(60, Items.GOLD_NUGGET, Items.GOLD_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.GOLDEN_SHOVEL, new ItemBurningConversionRecord(60, Items.GOLD_NUGGET, Items.GOLD_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.GOLDEN_SWORD, new ItemBurningConversionRecord(60, Items.GOLD_NUGGET, Items.GOLD_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.GOLDEN_HOE, new ItemBurningConversionRecord(60, Items.GOLD_NUGGET, Items.GOLD_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.GOLDEN_HELMET, new ItemBurningConversionRecord(60, Items.GOLD_NUGGET, Items.GOLD_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.GOLDEN_CHESTPLATE, new ItemBurningConversionRecord(60, Items.GOLD_NUGGET, Items.GOLD_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.GOLDEN_LEGGINGS, new ItemBurningConversionRecord(60, Items.GOLD_NUGGET, Items.GOLD_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.GOLDEN_BOOTS, new ItemBurningConversionRecord(60, Items.GOLD_NUGGET, Items.GOLD_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.LIGHT_WEIGHTED_PRESSURE_PLATE, new ItemBurningConversionRecord(60, Items.GOLD_NUGGET, Items.GOLD_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.GOLDEN_APPLE, new ItemBurningConversionRecord(60, Items.GOLD_NUGGET, Items.GOLD_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.ENCHANTED_GOLDEN_APPLE, new ItemBurningConversionRecord(30, Items.GOLDEN_APPLE, Items.GOLDEN_APPLE, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CLOCK, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));


        ITEM_BURNING_CONVERSION_MAP.put(Items.IRON_NUGGET, new ItemBurningConversionRecord(240, null, null, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.IRON_BLOCK, new ItemBurningConversionRecord(180, Items.IRON_INGOT, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.IRON_INGOT, new ItemBurningConversionRecord(120, Items.IRON_NUGGET, Items.IRON_NUGGET, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.RAW_IRON_BLOCK, new ItemBurningConversionRecord(240, Items.RAW_IRON, Items.IRON_BLOCK, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.RAW_IRON, new ItemBurningConversionRecord(180, null, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.IRON_ORE, new ItemBurningConversionRecord(180, null, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DEEPSLATE_IRON_ORE, new ItemBurningConversionRecord(180, null, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.IRON_AXE, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.IRON_PICKAXE, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.IRON_SHOVEL, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.IRON_SWORD, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.IRON_HOE, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.IRON_HELMET, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.IRON_CHESTPLATE, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.IRON_LEGGINGS, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.IRON_BOOTS, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.HEAVY_WEIGHTED_PRESSURE_PLATE, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.IRON_BARS, new ItemBurningConversionRecord(40, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.HOPPER, new ItemBurningConversionRecord(80, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COMPASS, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.RECOVERY_COMPASS, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.MINECART, new ItemBurningConversionRecord(160, Items.IRON_INGOT, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CHEST_MINECART, new ItemBurningConversionRecord(160, Items.IRON_INGOT, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.FURNACE_MINECART, new ItemBurningConversionRecord(160, Items.IRON_INGOT, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.TNT_MINECART, new ItemBurningConversionRecord(0, Items.MINECART, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.ANVIL, new ItemBurningConversionRecord(360, Items.IRON_BLOCK, Items.IRON_BLOCK, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CHIPPED_ANVIL, new ItemBurningConversionRecord(180, Items.IRON_BLOCK, Items.IRON_BLOCK, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DAMAGED_ANVIL, new ItemBurningConversionRecord(60, Items.IRON_BLOCK, Items.IRON_BLOCK, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.BUCKET, new ItemBurningConversionRecord(260, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.POWDER_SNOW_BUCKET, new ItemBurningConversionRecord(-20, Items.WATER_BUCKET, Items.BUCKET, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.MILK_BUCKET, new ItemBurningConversionRecord(0, Items.BUCKET, Items.BUCKET, false));


        ITEM_BURNING_CONVERSION_MAP.put(Items.LAPIS_LAZULI, new ItemBurningConversionRecord(40, null, null, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.LAPIS_BLOCK, new ItemBurningConversionRecord(120, Items.LAPIS_LAZULI, Items.LAPIS_LAZULI, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.LAPIS_ORE, new ItemBurningConversionRecord(120, Items.LAPIS_LAZULI, Items.LAPIS_LAZULI, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DEEPSLATE_LAPIS_ORE, new ItemBurningConversionRecord(120, Items.LAPIS_LAZULI, Items.LAPIS_LAZULI, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.LAPIS_LAZULI, new ItemBurningConversionRecord(120, Items.LAPIS_LAZULI, Items.LAPIS_LAZULI, false));


        ITEM_BURNING_CONVERSION_MAP.put(Items.EMERALD, new ItemBurningConversionRecord(60, null, null, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.EMERALD_BLOCK, new ItemBurningConversionRecord(220, Items.EMERALD, Items.EMERALD, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.EMERALD_ORE, new ItemBurningConversionRecord(260, Items.EMERALD, Items.EMERALD, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DEEPSLATE_EMERALD_ORE, new ItemBurningConversionRecord(260, Items.EMERALD, Items.EMERALD, false));


        ITEM_BURNING_CONVERSION_MAP.put(Items.REDSTONE, new ItemBurningConversionRecord(30, null, null, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.REDSTONE_BLOCK, new ItemBurningConversionRecord(60, Items.REDSTONE, Items.REDSTONE, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.REDSTONE_ORE, new ItemBurningConversionRecord(120, Items.REDSTONE, Items.REDSTONE, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DEEPSLATE_REDSTONE_ORE, new ItemBurningConversionRecord(120, Items.REDSTONE, Items.REDSTONE, false));


        ITEM_BURNING_CONVERSION_MAP.put(Items.DIAMOND, new ItemBurningConversionRecord(360, null, null, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DIAMOND_BLOCK, new ItemBurningConversionRecord(360, Items.DIAMOND, Items.DIAMOND, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DIAMOND_ORE, new ItemBurningConversionRecord(360, Items.DIAMOND, Items.DIAMOND, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DEEPSLATE_DIAMOND_ORE, new ItemBurningConversionRecord(360, Items.DIAMOND, Items.DIAMOND, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DIAMOND_AXE, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DIAMOND_PICKAXE, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DIAMOND_SHOVEL, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DIAMOND_SWORD, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DIAMOND_HOE, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DIAMOND_HELMET, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DIAMOND_CHESTPLATE, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DIAMOND_LEGGINGS, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DIAMOND_BOOTS, new ItemBurningConversionRecord(60, Items.IRON_NUGGET, Items.IRON_INGOT, false));


        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_INGOT, new ItemBurningConversionRecord(120, null, null, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.RAW_COPPER_BLOCK, new ItemBurningConversionRecord(240, Items.RAW_COPPER, Items.COPPER_BLOCK.weathering().unaffected(), false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.RAW_COPPER, new ItemBurningConversionRecord(180, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_ORE, new ItemBurningConversionRecord(180, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.DEEPSLATE_COPPER_ORE, new ItemBurningConversionRecord(180, null, Items.COPPER_INGOT, false));

        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_AXE, new ItemBurningConversionRecord(60, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_PICKAXE, new ItemBurningConversionRecord(60, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_SHOVEL, new ItemBurningConversionRecord(60, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_SWORD, new ItemBurningConversionRecord(60, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_HOE, new ItemBurningConversionRecord(60, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_AXE_WAXED, new ItemBurningConversionRecord(60, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_PICKAXE_WAXED, new ItemBurningConversionRecord(60, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_SHOVEL_WAXED, new ItemBurningConversionRecord(60, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_SWORD_WAXED, new ItemBurningConversionRecord(60, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_HOE_WAXED, new ItemBurningConversionRecord(60, null, Items.COPPER_INGOT, false));

        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_HELMET, new ItemBurningConversionRecord(60, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_CHESTPLATE, new ItemBurningConversionRecord(60, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_LEGGINGS, new ItemBurningConversionRecord(60, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_BOOTS, new ItemBurningConversionRecord(60, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_HELMET_EXPOSED, new ItemBurningConversionRecord(20, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_CHESTPLATE_EXPOSED, new ItemBurningConversionRecord(20, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_LEGGINGS_EXPOSED, new ItemBurningConversionRecord(20, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_BOOTS_EXPOSED, new ItemBurningConversionRecord(20, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_HELMET_WAXED, new ItemBurningConversionRecord(20, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_CHESTPLATE_WAXED, new ItemBurningConversionRecord(20, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_LEGGINGS_WAXED, new ItemBurningConversionRecord(20, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(CustomEquipment.COPPER_BOOTS_WAXED, new ItemBurningConversionRecord(20, null, Items.COPPER_INGOT, false));

        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_BLOCK.weathering().unaffected(), new ItemBurningConversionRecord(240, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_BLOCK.waxed().unaffected(), new ItemBurningConversionRecord(-10, Items.COPPER_BLOCK.weathering().unaffected(), Items.COPPER_BLOCK.weathering().unaffected(), false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CHISELED_COPPER.weathering().unaffected(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CHISELED_COPPER.waxed().unaffected(), new ItemBurningConversionRecord(-10, Items.CHISELED_COPPER.weathering().unaffected(), Items.CHISELED_COPPER.weathering().unaffected(), false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER.weathering().unaffected(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER_SLAB.weathering().unaffected(), new ItemBurningConversionRecord(80, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER_STAIRS.weathering().unaffected(), new ItemBurningConversionRecord(90, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER.waxed().unaffected(), new ItemBurningConversionRecord(-20, Items.CUT_COPPER.weathering().unaffected(), Items.CUT_COPPER.weathering().unaffected(), false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER_SLAB.waxed().unaffected(), new ItemBurningConversionRecord(-20, Items.CUT_COPPER_SLAB.weathering().unaffected(), Items.CUT_COPPER_SLAB.weathering().unaffected(), false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER_STAIRS.waxed().unaffected(), new ItemBurningConversionRecord(-20, Items.CUT_COPPER_STAIRS.weathering().unaffected(), Items.CUT_COPPER_STAIRS.weathering().unaffected(), false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_BULB.weathering().unaffected(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_GRATE.weathering().unaffected(), new ItemBurningConversionRecord(20, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_DOOR.weathering().unaffected(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_TRAPDOOR.weathering().unaffected(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_BULB.waxed().unaffected(), new ItemBurningConversionRecord(-20, Items.COPPER_BULB.weathering().unaffected(), Items.COPPER_BULB.weathering().unaffected(), false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_GRATE.waxed().unaffected(), new ItemBurningConversionRecord(-20, Items.COPPER_GRATE.weathering().unaffected(), Items.COPPER_GRATE.weathering().unaffected(), false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_DOOR.waxed().unaffected(), new ItemBurningConversionRecord(-20, Items.COPPER_DOOR.weathering().unaffected(), Items.COPPER_DOOR.weathering().unaffected(), false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_TRAPDOOR.waxed().unaffected(), new ItemBurningConversionRecord(-20, Items.COPPER_TRAPDOOR.weathering().unaffected(), Items.COPPER_TRAPDOOR.weathering().unaffected(), false));

        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_BLOCK.weathering().exposed(), new ItemBurningConversionRecord(240, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_BLOCK.waxed().exposed(), new ItemBurningConversionRecord(-10, Items.COPPER_BLOCK.weathering().exposed(), Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CHISELED_COPPER.weathering().exposed(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CHISELED_COPPER.waxed().exposed(), new ItemBurningConversionRecord(-10, Items.CHISELED_COPPER.weathering().unaffected(), Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER.weathering().exposed(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER_SLAB.weathering().exposed(), new ItemBurningConversionRecord(80, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER_STAIRS.weathering().exposed(), new ItemBurningConversionRecord(90, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER.waxed().exposed(), new ItemBurningConversionRecord(-20, Items.CUT_COPPER.weathering().unaffected(), Items.CUT_COPPER.weathering().unaffected(), false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER_SLAB.waxed().exposed(), new ItemBurningConversionRecord(-20, Items.CUT_COPPER_SLAB.weathering().unaffected(), Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER_STAIRS.waxed().exposed(), new ItemBurningConversionRecord(-20, Items.CUT_COPPER_STAIRS.weathering().unaffected(), Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_BULB.weathering().exposed(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_GRATE.weathering().exposed(), new ItemBurningConversionRecord(20, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_DOOR.weathering().exposed(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_TRAPDOOR.weathering().exposed(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_BULB.waxed().exposed(), new ItemBurningConversionRecord(-20, Items.COPPER_BULB.weathering().unaffected(), Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_GRATE.waxed().exposed(), new ItemBurningConversionRecord(-20, Items.COPPER_GRATE.weathering().unaffected(), Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_DOOR.waxed().exposed(), new ItemBurningConversionRecord(-20, Items.COPPER_DOOR.weathering().unaffected(), Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_TRAPDOOR.waxed().exposed(), new ItemBurningConversionRecord(-20, Items.COPPER_TRAPDOOR.weathering().unaffected(), Items.COPPER_INGOT, false));

        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_BLOCK.weathering().weathered(), new ItemBurningConversionRecord(240, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_BLOCK.waxed().weathered(), new ItemBurningConversionRecord(-10, Items.COPPER_BLOCK.weathering().weathered(), Items.COPPER_BLOCK.weathering().weathered(), false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CHISELED_COPPER.weathering().weathered(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CHISELED_COPPER.waxed().weathered(), new ItemBurningConversionRecord(-10, Items.CHISELED_COPPER.weathering().weathered(), Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER.weathering().weathered(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER_SLAB.weathering().weathered(), new ItemBurningConversionRecord(80, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER_STAIRS.weathering().weathered(), new ItemBurningConversionRecord(90, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER.waxed().weathered(), new ItemBurningConversionRecord(-20, Items.CUT_COPPER.weathering().weathered(), Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER_SLAB.waxed().weathered(), new ItemBurningConversionRecord(-20, Items.CUT_COPPER_SLAB.weathering().weathered(), Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER_STAIRS.waxed().weathered(), new ItemBurningConversionRecord(-20, Items.CUT_COPPER_STAIRS.weathering().weathered(), Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_BULB.weathering().weathered(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_GRATE.weathering().weathered(), new ItemBurningConversionRecord(20, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_DOOR.weathering().weathered(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_TRAPDOOR.weathering().weathered(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_BULB.waxed().weathered(), new ItemBurningConversionRecord(-20, Items.COPPER_BULB.weathering().exposed(), Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_GRATE.waxed().weathered(), new ItemBurningConversionRecord(-20, Items.COPPER_GRATE.weathering().exposed(), Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_DOOR.waxed().weathered(), new ItemBurningConversionRecord(-20, Items.COPPER_DOOR.weathering().exposed(), Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_TRAPDOOR.waxed().weathered(), new ItemBurningConversionRecord(-20, Items.COPPER_TRAPDOOR.weathering().exposed(), Items.COPPER_INGOT, false));

        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_BLOCK.weathering().oxidized(), new ItemBurningConversionRecord(240, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CHISELED_COPPER.weathering().oxidized(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER.weathering().oxidized(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER_SLAB.weathering().oxidized(), new ItemBurningConversionRecord(80, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.CUT_COPPER_STAIRS.weathering().oxidized(), new ItemBurningConversionRecord(90, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_BULB.weathering().oxidized(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_GRATE.weathering().oxidized(), new ItemBurningConversionRecord(20, null, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_DOOR.weathering().oxidized(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
        ITEM_BURNING_CONVERSION_MAP.put(Items.COPPER_TRAPDOOR.weathering().oxidized(), new ItemBurningConversionRecord(120, Items.COPPER_INGOT, Items.COPPER_INGOT, false));
    }
}
