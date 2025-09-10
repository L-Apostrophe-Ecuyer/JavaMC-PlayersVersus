package frootloops.versus.mod.items.inventory.sorting.lists;

import frootloops.versus.mod.items.inventory.sorting.ItemSortingMaps;
import frootloops.versus.mod.items.inventory.sorting.ItemType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.BlockSoundGroup;

public class SortedItemLists {

    /** MISC - GOODIES ------------------------------------------------------------   */
    public static final SortedTaggedItemList SHULKER_BOXES = new SortedTaggedItemList(ItemTags.SHULKER_BOXES, ItemType.ITEM_CONTAINER);
    public static final SortedTaggedItemList BUNDLES = new SortedTaggedItemList(ItemTags.BUNDLES, ItemType.ITEM_CONTAINER);
    public static final SortedMappedItemList TREASURE_ITEMS = new SortedMappedItemList(ItemSortingMaps.ITEMS_TREASURE, ItemType.MISC);
    public static final SortedTypedItemList SMITHING_ITEMS = new SortedTypedItemList(new ItemType[]{ItemType.SMITHING_TEMPLATE});
    public static final SortedItemList[] MISC_GOODIES = {SHULKER_BOXES, BUNDLES, TREASURE_ITEMS, SMITHING_ITEMS};


    /** MINEABLE GOODIES ------------------------------------------------------------ */
    public static final SortedMappedItemList ORE_BLOCKS = new SortedMappedItemList(ItemSortingMaps.ITEMS_ORE_BLOCKS, ItemType.MISC);
    public static final SortedMappedItemList REFINED_MINERALS = new SortedMappedItemList(ItemSortingMaps.ITEMS_REFINED_MINERALS, ItemType.MISC);
    public static final SortedMappedItemList REDSTONE_RAW = new SortedMappedItemList(ItemSortingMaps.ITEMS_REDSTONE_COMPONENTS, ItemType.MISC);
    public static final SortedBlockItemList GOLD_ITEMS_AND_BLOCKS = new SortedBlockItemList(ItemSortingMaps.ITEMS_AND_BLOCKS_OF_GOLD, ItemType.MISC);
    public static final SortedBlockItemList IRON_ITEMS_AND_BLOCKS = new SortedBlockItemList(ItemSortingMaps.ITEMS_AND_BLOCKS_OF_IRON, ItemType.MISC);
    public static final SortedBlockItemList COPPER_ITEMS_AND_BLOCKS = new SortedBlockItemList(BlockSoundGroup.COPPER, ItemSortingMaps.ITEMS_AND_BLOCKS_OF_COPPER);
    public static final SortedItemList[] MINERAL_RESSOURCE_ITEMS = {ORE_BLOCKS, REFINED_MINERALS, REDSTONE_RAW, GOLD_ITEMS_AND_BLOCKS, IRON_ITEMS_AND_BLOCKS, COPPER_ITEMS_AND_BLOCKS};
    public static final SortedItemList[] MINERAL_RESSOURCE_COMMON_ITEMS = {new SortedMappedItemList(ItemSortingMaps.ITEMS_REFINED_MINERALS, ItemType.MISC)};


    /** REDSTONE -------------------------------------------------------------------- */
    public static final SortedMappedItemList REDSTONE_COMPONENTS = new SortedMappedItemList(ItemSortingMaps.ITEMS_REDSTONE_COMPONENTS, ItemType.BLOCK_OTHER);
    public static final SortedBlockItemList MINECART_ITEMS = new SortedBlockItemList(BlockTags.RAILS, ItemSortingMaps.ITEMS_MINECARTS);
    public static final SortedItemList[] REDSTONE_ITEMS = {REDSTONE_COMPONENTS, MINECART_ITEMS};


    /** MISC - COMBAT ------------------------------------------------------------   */
    public static final SortedTypedItemList FOOD_ITEMS = new SortedTypedItemList(new ItemType[]{ItemType.FOOD});
    public static final SortedTypedItemList POTION_ITEMS = new SortedTypedItemList(new ItemType[]{ItemType.POTIONS});
    public static final SortedItemList[] CONSUMEABLE_ITEMS = {FOOD_ITEMS, POTION_ITEMS};

    public static final SortedTypedItemList WEAPONS = new SortedTypedItemList(new ItemType[]{ItemType.SWORD, ItemType.SPECIAL_WEAPON, ItemType.BOW, ItemType.CROSSBOW});
    public static final SortedTypedItemList ARMOR  = new SortedTypedItemList(new ItemType[]{ItemType.CHESTPLATE, ItemType.LEGGINGS, ItemType.HELMET, ItemType.BOOTS});
    public static final SortedTypedItemList OTHER_COMBAT_ITEMS = new SortedTypedItemList(new ItemType[]{ItemType.TOTEM, ItemType.CLUTCH_TOOL, ItemType.COMBAT_ITEMS, ItemType.ARROWS});
    public static final SortedItemList[] COMBAT_ITEMS = {ARMOR, OTHER_COMBAT_ITEMS};


    /** MISC - MYSTICISM & MOBS ---------------------------------------------------------   */
    public static final SortedMappedItemList MISC_BREWING_ITEMS = new SortedMappedItemList(ItemSortingMaps.ITEMS_BREWING_MISC, ItemType.BREWING_INGREDIENT);
    public static final SortedTypedItemList CONCENTRATES = new SortedTypedItemList(new ItemType[]{ItemType.BREWING_INGREDIENT});
    public static final SortedItemList[] MISC_BREWING = {MISC_BREWING_ITEMS, CONCENTRATES};


    /** MISC - WORLD AND ARCHEOLOGY ---------------------------------------------------------   */
    public static final SortedMappedItemList FISHING = new SortedMappedItemList(ItemSortingMaps.ITEMS_FISHING, ItemType.MISC_TOOL);
    public static final SortedTypedItemList MISC_TOOLS = new SortedTypedItemList(new ItemType[]{ItemType.SPYGLASS, ItemType.MISC_TOOL});
    public static final SortedBlockItemList BEDS = new SortedBlockItemList(ItemSortingMaps.ITEMS_BEDS);
    public static final SortedBlockItemList CANDLES = new SortedBlockItemList(ItemSortingMaps.ITEMS_CANDLES);
    public static final SortedBlockItemList BANNERS = new SortedBlockItemList(ItemSortingMaps.ITEMS_BANNERS);
    public static final SortedTypedItemList POTTERY_AND_BANNERS = new SortedTypedItemList(new ItemType[]{ItemType.POTTERY, ItemType.BANNER_PATTERNS});
    public static final SortedTypedItemList DISCS = new SortedTypedItemList(new ItemType[]{ItemType.DISCS});
    public static final SortedItemList[] MISC_EXPLORATION = {MISC_TOOLS, BEDS, CANDLES, BANNERS, POTTERY_AND_BANNERS, DISCS};


    /** MISC - ANYTHING ELSE ---------------------------------------------------------   */
    public static final SortedMappedItemList MISC_TRASH = new SortedMappedItemList(ItemSortingMaps.ITEMS_TRUE_TRASH, ItemType.TRASH);
    public static final SortedMiscItemList MISC_EVERYTHING_BUT_MONSTER = new SortedMiscItemList(ItemSortingMaps.ITEMS_TRUE_TRASH);
    public static final SortedItemList[] MISC_OTHER = {MISC_EVERYTHING_BUT_MONSTER, MISC_TRASH};


    /** SHEARS ------------------------------------------------------------   */
    public static final SortedTypedItemList SHEARS = new SortedTypedItemList(new ItemType[]{ItemType.SHEARS});
    public static final SortedMappedItemList SHEARS_ANIMAL_HANDLING = new SortedMappedItemList(ItemSortingMaps.ITEMS_ANIMALS, ItemType.MISC);
    public static final SortedBlockItemList WOOL_BLOCKS = new SortedBlockItemList(ItemSortingMaps.ITEMS_SHEARS_WOOL_BLOCKS);
    public static final SortedMappedItemList DYES = new SortedMappedItemList(ItemSortingMaps.ITEMS_DYES, ItemType.MISC);
    public static final SortedBlockItemList FLOWERS = new SortedBlockItemList(BlockTags.FLOWERS, ItemSortingMaps.ITEMS_FLOWERS);
    public static final SortedBlockItemList SAPLINGS = new SortedBlockItemList(BlockTags.SAPLINGS);
    public static final SortedBlockItemList LEAVES = new SortedBlockItemList(BlockTags.LEAVES);
    public static final SortedBlockItemList SHEARS_SHRUBBERY = new SortedBlockItemList(null, BlockSoundGroup.GRASS, ItemSortingMaps.ITEMS_SHEARS_SHRUBBERY, 0.0F, 0.1F, ItemType.BLOCK_OTHER);
    public static final SortedMappedItemList SHEARS_AQUATIC = new SortedMappedItemList(ItemSortingMaps.ITEMS_FLOWERS, ItemType.BLOCK_OTHER);
    public static final SortedItemList[] SHEAR_MINEABLES = {SHEARS_ANIMAL_HANDLING, WOOL_BLOCKS, DYES, FLOWERS, SAPLINGS, LEAVES, SHEARS_SHRUBBERY, SHEARS_AQUATIC};


    /** SHOVELS ------------------------------------------------------------   */
    public static final SortedTypedItemList SHOVELS = new SortedTypedItemList(new ItemType[]{ItemType.SHOVEL});
    public static final SortedItemList[] SHOVEL_MINEABLES = {new SortedBlockItemList(BlockTags.SHOVEL_MINEABLE, null, ItemSortingMaps.ITEMS_SHOVEL_MINEABLE, 0.2f, 16.0f, ItemType.BLOCK_FULL)};


    /** HOES -------------------------------------------------------------   */
    public static final SortedTypedItemList HOES = new SortedTypedItemList(new ItemType[]{ItemType.HOE});
    public static final SortedMappedItemList HOE_SPECIAL = new SortedMappedItemList(ItemSortingMaps.ITEMS_HOE_SPECIAL, ItemType.BLOCK_OTHER);
    public static final SortedMappedItemList HOE_MINEABLE_FARMING = new SortedMappedItemList(ItemSortingMaps.ITEMS_HOE_FARMING, ItemType.MISC);
    public static final SortedBlockItemList ITEMS_HOE_MOSS = new SortedBlockItemList(BlockSoundGroup.MOSS_BLOCK, ItemSortingMaps.ITEMS_HOE_MOSS);
    public static final SortedMappedItemList HOE_MINEABLE_SCULK = new SortedMappedItemList(ItemSortingMaps.ITEMS_HOE_SCULK, ItemType.MISC);
    public static final SortedMappedItemList HOE_MINEABLE_NETHER = new SortedMappedItemList(ItemSortingMaps.ITEMS_HOE_NETHER_BLOCKS, ItemType.BLOCK_OTHER);
    public static final SortedItemList[] HOES_MINEABLE = {HOE_MINEABLE_FARMING, HOE_SPECIAL, ITEMS_HOE_MOSS, HOE_MINEABLE_SCULK, HOE_MINEABLE_NETHER};


    /** PICKAXES -------------------------------------------------------------   */
    public static final SortedTypedItemList PICKAXES = new SortedTypedItemList(new ItemType[]{ItemType.PICKAXE});
    public static final SortedMappedItemList PICKAXE_MINEABLE_ICE = new SortedMappedItemList(ItemSortingMaps.ITEMS_PICKAXE_ICE, ItemType.BLOCK_FULL);
    public static final SortedMappedItemList PICKAXE_MINEABLE_NETHER = new SortedMappedItemList(ItemSortingMaps.ITEMS_PICKAXE_NETHER_BLOCKS, ItemType.BLOCK_FULL);
    public static final SortedMappedItemList PICKAXE_MINEABLE_BLACKSTONE = new SortedMappedItemList(ItemSortingMaps.ITEMS_PICKAXE_BLACKSTONE, ItemType.BLOCK_FULL);
    public static final SortedMappedItemList PICKAXE_MINEABLE_GRAYS_BRICKS = new SortedMappedItemList(ItemSortingMaps.ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK, ItemType.BLOCK_FULL);
    public static final SortedMappedItemList PICKAXE_MINEABLE_GRAYS_NATURAL = new SortedMappedItemList(ItemSortingMaps.ITEMS_PICKAXE_GRAYS_NATURAL, ItemType.BLOCK_FULL);
    public static final SortedMappedItemList PICKAXE_MINEABLE_GRAYS_COBBLED = new SortedMappedItemList(ItemSortingMaps.ITEMS_PICKAXE_GRAYS_COBBLE, ItemType.BLOCK_FULL);
    public static final SortedMappedItemList PICKAXE_MINEABLE_CORAL = new SortedMappedItemList(ItemSortingMaps.ITEMS_PICKAXE_CORAL, ItemType.BLOCK_FULL);
    public static final SortedMappedItemList PICKAXE_MINEABLE_PALE = new SortedMappedItemList(ItemSortingMaps.ITEMS_PICKAXE_PALE_STONES, ItemType.BLOCK_FULL);
    public static final SortedMappedItemList PICKAXE_MINEABLE_WARM = new SortedMappedItemList(ItemSortingMaps.ITEMS_PICKAXE_WARM_BLOCKS, ItemType.BLOCK_FULL);
    public static final SortedMappedItemList PICKAXE_MINEABLE_TERRACOTTA = new SortedMappedItemList(ItemSortingMaps.ITEMS_PICKAXE_TERRACOTTA_BLOCKS, ItemType.BLOCK_FULL);
    public static final SortedBlockItemList PICKAXE_MINEABLE_GENERIC = new SortedBlockItemList(BlockTags.PICKAXE_MINEABLE, 0.25f, 51.0f);
    public static final SortedItemList[] PICKAXE_MINEABLES = {PICKAXE_MINEABLE_ICE, PICKAXE_MINEABLE_NETHER, PICKAXE_MINEABLE_BLACKSTONE, PICKAXE_MINEABLE_GRAYS_BRICKS, PICKAXE_MINEABLE_GRAYS_NATURAL, PICKAXE_MINEABLE_GRAYS_COBBLED, PICKAXE_MINEABLE_CORAL, PICKAXE_MINEABLE_PALE, PICKAXE_MINEABLE_WARM, PICKAXE_MINEABLE_TERRACOTTA, PICKAXE_MINEABLE_GENERIC};


    /** AXES -------------------------------------------------------------   */
    public static final SortedTypedItemList AXES = new SortedTypedItemList(new ItemType[]{ItemType.AXE});
    public static final SortedBlockItemList AXE_MINEABLE_PLANKS = new SortedBlockItemList(BlockTags.PLANKS);
    public static final SortedBlockItemList AXE_MINEABLE_LOGS = new SortedBlockItemList(BlockTags.LOGS);
    public static final SortedTaggedItemList AXE_MINEABLE_STAIRS = new SortedTaggedItemList(ItemTags.WOODEN_STAIRS, ItemType.BLOCK_STAIRS);
    public static final SortedTaggedItemList AXE_MINEABLE_SLABS = new SortedTaggedItemList(ItemTags.WOODEN_SLABS, ItemType.BLOCK_SLAB);
    public static final SortedTaggedItemList AXE_MINEABLE_DOORS = new SortedTaggedItemList(ItemTags.WOODEN_DOORS, ItemType.BLOCK_OTHER);
    public static final SortedTaggedItemList AXE_MINEABLE_TRAPDOORS = new SortedTaggedItemList(ItemTags.WOODEN_TRAPDOORS, ItemType.BLOCK_OTHER);
    public static final SortedTaggedItemList AXE_MINEABLE_BUTTONS = new SortedTaggedItemList(ItemTags.WOODEN_BUTTONS, ItemType.BLOCK_OTHER);
    public static final SortedBlockItemList AXE_MINEABLE_OTHER = new SortedBlockItemList(BlockTags.AXE_MINEABLE, 0.5f, 32.0f);
    public static final SortedItemList[] AXE_MINEABLES = {AXE_MINEABLE_PLANKS, AXE_MINEABLE_LOGS, AXE_MINEABLE_SLABS, AXE_MINEABLE_STAIRS, AXE_MINEABLE_DOORS, AXE_MINEABLE_TRAPDOORS, AXE_MINEABLE_BUTTONS, AXE_MINEABLE_OTHER};

}
