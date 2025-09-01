package frootloops.versus.mod.items.inventory.sorting.groups;

import frootloops.versus.mod.items.inventory.sorting.lists.SortedItemLists;

public abstract class SortingGroups {

    // VIP: The main hotbar
    public static final MainHotbarGroup MAIN_HOTBAR = new MainHotbarGroup();

    // First class: Tool groups
    public static final ToolSortingGroup PICKAXE_GROUP = new ToolSortingGroup(SortedItemLists.PICKAXES, SortedItemLists.PICKAXE_MINEABLES, "GROUP: PICKAXES");
    public static final ToolSortingGroup AXE_GROUP = new ToolSortingGroup(SortedItemLists.AXES, SortedItemLists.AXE_MINEABLES, "GROUP: AXES");
    public static final ToolSortingGroup SHOVEL_GROUP = new ToolSortingGroup(SortedItemLists.SHOVELS, SortedItemLists.SHOVEL_MINEABLES, "GROUP: SHOVELS");
    public static final ToolSortingGroup SHEARS_GROUP = new ToolSortingGroup(SortedItemLists.SHEARS, SortedItemLists.SHEAR_MINEABLES, "GROUP: SHEARS");
    public static final ToolSortingGroup HOE_GROUP = new ToolSortingGroup(SortedItemLists.HOES, SortedItemLists.HOES_MINEABLE, "GROUP: HOES");

    // Second class: Valuables, misc
    public static final SimpleSortingGroup CONSUMABLES_GROUP = new SimpleSortingGroup(SortedItemLists.CONSUMEABLE_ITEMS, "GROUP: CONSUMABLES");
    public static final ToolSortingGroup COMBAT_GROUP = new ToolSortingGroup(SortedItemLists.WEAPONS, SortedItemLists.COMBAT_ITEMS, "GROUP: COMBAT");
    public static final SimpleSortingGroup REDSTONE_GROUP = new SimpleSortingGroup(SortedItemLists.REDSTONE_ITEMS, "GROUP: REDSTONE");
    public static final SimpleSortingGroup GOODIES_GROUP = new SimpleSortingGroup(SortedItemLists.MISC_GOODIES, "GROUP: GOODIES");
    public static final SimpleSortingGroup MINERALS_GROUP = new SimpleSortingGroup(SortedItemLists.MINERAL_RESSOURCE_ITEMS, "GROUP: MINERALS");
    public static final SimpleSortingGroup BREWING_GROUP = new SimpleSortingGroup(SortedItemLists.MISC_BREWING, "GROUP: BREWING");
    public static final SimpleSortingGroup WORLD_GROUP = new SimpleSortingGroup(SortedItemLists.MISC_EXPLORATION, "GROUP: WORLD");
    public static final SimpleSortingGroup RANDOM_GROUP = new SimpleSortingGroup(SortedItemLists.MISC_OTHER, "GROUP: MISC OTHER");

    public static final SortingGroup[] SORTING_GROUPS = {COMBAT_GROUP, CONSUMABLES_GROUP, GOODIES_GROUP, MINERALS_GROUP, REDSTONE_GROUP, PICKAXE_GROUP, AXE_GROUP, SHOVEL_GROUP, HOE_GROUP, SHEARS_GROUP, BREWING_GROUP, WORLD_GROUP, RANDOM_GROUP};
}
