package frootloops.versus.mod.items.inventory.sorting.lists;

import frootloops.versus.mod.items.inventory.sorting.ItemSortingMaps;
import frootloops.versus.mod.items.inventory.sorting.ItemType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.BlockSoundGroup;

public class SortedItemLists {

    /** MISC - GOODIES ------------------------------------------------------------   */
    public static final SortedTaggedItemList SHULKER_BOXES = new SortedTaggedItemList(ItemTags.SHULKER_BOXES);
    public static final SortedTaggedItemList BUNDLES = new SortedTaggedItemList(ItemTags.BUNDLES);
    public static final SortedMappedItemList TREASURE_ITEMS = new SortedMappedItemList(ItemSortingMaps.ITEMS_TREASURE);
    public static final SortedTypedItemList SMITHING_ITEMS = new SortedTypedItemList(new ItemType[]{ItemType.SMITHING_TEMPLATE});
    public static final SortedItemList[] MISC_GOODIES = {SHULKER_BOXES, BUNDLES, TREASURE_ITEMS, SMITHING_ITEMS};


    /** MINEABLE GOODIES ------------------------------------------------------------ */
    public static final SortedMappedItemList ORE_BLOCKS = new SortedMappedItemList(ItemSortingMaps.ITEMS_ORE_BLOCKS);
    public static final SortedMappedItemList REFINED_MINERALS = new SortedMappedItemList(ItemSortingMaps.ITEMS_REFINED_MINERALS);
    public static final SortedMappedItemList REDSTONE_RAW = new SortedMappedItemList(ItemSortingMaps.ITEMS_REDSTONE_COMPONENTS);
    public static final SortedBlockItemList GOLD_ITEMS_AND_BLOCKS = new SortedBlockItemList(ItemSortingMaps.ITEMS_AND_BLOCKS_OF_GOLD);
    public static final SortedBlockItemList IRON_ITEMS_AND_BLOCKS = new SortedBlockItemList(ItemSortingMaps.ITEMS_AND_BLOCKS_OF_IRON);
    public static final SortedBlockItemList COPPER_ITEMS_AND_BLOCKS = new SortedBlockItemList(BlockSoundGroup.COPPER, ItemSortingMaps.ITEMS_AND_BLOCKS_OF_COPPER);
    public static final SortedItemList[] MINERAL_RESSOURCE_ITEMS = {ORE_BLOCKS, REFINED_MINERALS, REDSTONE_RAW, GOLD_ITEMS_AND_BLOCKS, IRON_ITEMS_AND_BLOCKS, COPPER_ITEMS_AND_BLOCKS};


    /** REDSTONE -------------------------------------------------------------------- */
    public static final SortedMappedItemList REDSTONE_COMPONENTS = new SortedMappedItemList(ItemSortingMaps.ITEMS_REDSTONE_COMPONENTS);
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


    /** MISC - ANYTHING ELSE ---------------------------------------------------------   */
    public static final SortedTypedItemList MISC_BREWING = new SortedTypedItemList(new ItemType[]{ItemType.BREWING_INGREDIENT});
    public static final SortedMappedItemList MISC_MONSTER = new SortedMappedItemList(ItemSortingMaps.ITEMS_MONSTER_DROPS);
    public static final SortedMiscItemList MISC_EVERYTHING_BUT_MONSTER = new SortedMiscItemList(ItemSortingMaps.ITEMS_MONSTER_DROPS);
    public static final SortedItemList[] MISC_OTHER = {MISC_BREWING, MISC_EVERYTHING_BUT_MONSTER, MISC_MONSTER};


    /** SHEARS ------------------------------------------------------------   */
    public static final SortedMappedItemList SHEARS = new SortedMappedItemList(ItemSortingMaps.ITEMS_SHEAR_AND_FISHING);
    public static final SortedBlockItemList SHEARS_BLOCKS = new SortedBlockItemList(BlockTags.LEAVES, ItemSortingMaps.ITEMS_SHEARS_WOOL_BLOCKS);
    public static final SortedBlockItemList SHEARS_ANIMAL_HANDLING = new SortedBlockItemList(BlockTags.FLOWERS, ItemSortingMaps.ITEMS_ANIMALS);
    public static final SortedItemList[] SHEAR_MINEABLES = {SHEARS_BLOCKS, SHEARS_ANIMAL_HANDLING};


    /** SHOVELS ------------------------------------------------------------   */
    public static final SortedTaggedItemList SHOVELS = new SortedTaggedItemList(ItemTags.SHOVELS);
    public static final SortedItemList[] SHOVEL_MINEABLES = {new SortedBlockItemList(BlockTags.SHOVEL_MINEABLE, null, ItemSortingMaps.ITEMS_SHOVEL_MINEABLE, 0.2f, 16.0f)};


    /** HOES -------------------------------------------------------------   */
    public static final SortedTaggedItemList HOES = new SortedTaggedItemList(ItemTags.HOES);
    public static final SortedBlockItemList HOE_MINEABLE_NATURE = new SortedBlockItemList(BlockTags.REPLACEABLE, ItemSortingMaps.ITEMS_HOE_NATURE_BLOCKS);
    public static final SortedMappedItemList HOE_MINEABLE_SCULK = new SortedMappedItemList(ItemSortingMaps.ITEMS_HOE_SCULK);
    public static final SortedMappedItemList HOE_MINEABLE_NETHER = new SortedMappedItemList(ItemSortingMaps.ITEMS_HOE_NETHER_BLOCKS);
    public static final SortedBlockItemList HOE_MINEABLE_GENERIC = new SortedBlockItemList(BlockTags.HOE_MINEABLE, -1.0f, 3.0f);
    public static final SortedItemList[] HOES_MINEABLE = {HOE_MINEABLE_NATURE, HOE_MINEABLE_SCULK, HOE_MINEABLE_NETHER, HOE_MINEABLE_GENERIC};


    /** PICKAXES -------------------------------------------------------------   */
    public static final SortedTaggedItemList PICKAXES = new SortedTaggedItemList(ItemTags.PICKAXES);
    public static final SortedMappedItemList PICKAXE_MINEABLE_NETHER = new SortedMappedItemList(ItemSortingMaps.ITEMS_PICKAXE_NETHER_BLOCKS);
    public static final SortedMappedItemList PICKAXE_MINEABLE_GRAY = new SortedMappedItemList(ItemSortingMaps.ITEMS_PICKAXE_GRAY_STONES);
    public static final SortedMappedItemList PICKAXE_MINEABLE_PALE = new SortedMappedItemList(ItemSortingMaps.ITEMS_PICKAXE_PALE_STONES);
    public static final SortedMappedItemList PICKAXE_MINEABLE_WARM = new SortedMappedItemList(ItemSortingMaps.ITEMS_PICKAXE_WARM_BLOCKS);
    public static final SortedMappedItemList PICKAXE_MINEABLE_TERRACOTTA = new SortedMappedItemList(ItemSortingMaps.ITEMS_PICKAXE_TERRACOTTA_BLOCKS);
    public static final SortedBlockItemList PICKAXE_MINEABLE_GENERIC = new SortedBlockItemList(BlockTags.PICKAXE_MINEABLE, 0.25f, 32.0f);
    public static final SortedItemList[] PICKAXE_MINEABLES = {PICKAXE_MINEABLE_NETHER, PICKAXE_MINEABLE_GRAY, PICKAXE_MINEABLE_PALE, PICKAXE_MINEABLE_WARM, PICKAXE_MINEABLE_TERRACOTTA, PICKAXE_MINEABLE_GENERIC};


    /** AXES -------------------------------------------------------------   */
    public static final SortedTaggedItemList AXES = new SortedTaggedItemList(ItemTags.AXES);
    public static final SortedBlockItemList AXE_MINEABLE_PLANKS = new SortedBlockItemList(BlockTags.PLANKS);
    public static final SortedBlockItemList AXE_MINEABLE_LOGS = new SortedBlockItemList(BlockTags.LOGS);
    public static final SortedTaggedItemList AXE_MINEABLE_STAIRS = new SortedTaggedItemList(ItemTags.WOODEN_STAIRS);
    public static final SortedTaggedItemList AXE_MINEABLE_SLABS = new SortedTaggedItemList(ItemTags.WOODEN_SLABS);
    public static final SortedTaggedItemList AXE_MINEABLE_DOORS = new SortedTaggedItemList(ItemTags.WOODEN_DOORS);
    public static final SortedTaggedItemList AXE_MINEABLE_TRAPDOORS = new SortedTaggedItemList(ItemTags.WOODEN_TRAPDOORS);
    public static final SortedTaggedItemList AXE_MINEABLE_BUTTONS = new SortedTaggedItemList(ItemTags.WOODEN_BUTTONS);
    public static final SortedBlockItemList AXE_MINEABLE_OTHER = new SortedBlockItemList(BlockTags.AXE_MINEABLE, 0.5f, 32.0f);
    public static final SortedItemList[] AXE_MINEABLES = {AXE_MINEABLE_PLANKS, AXE_MINEABLE_LOGS, AXE_MINEABLE_SLABS, AXE_MINEABLE_STAIRS, AXE_MINEABLE_DOORS, AXE_MINEABLE_TRAPDOORS, AXE_MINEABLE_BUTTONS, AXE_MINEABLE_OTHER};

}
