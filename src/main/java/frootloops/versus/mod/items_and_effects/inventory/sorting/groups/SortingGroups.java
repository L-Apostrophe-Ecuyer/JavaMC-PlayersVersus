package frootloops.versus.mod.items_and_effects.inventory.sorting.groups;

import frootloops.versus.mod.items_and_effects.inventory.sorting.lists.SortingLists;

/**
 * The groups of one sort, over that sort's lists.
 */
public class SortingGroups {
    // VIP: The main hotbar
    public final MainHotbarGroup hotbar = new MainHotbarGroup();

    // First class: Tool groups
    public final ToolSortingGroup pickaxes;
    public final ToolSortingGroup axes;
    public final ToolSortingGroup shovels;
    public final ToolSortingGroup shears;
    public final ToolSortingGroup hoes;

    // Second class: Valuables, misc
    public final SimpleSortingGroup consumables;
    public final ToolSortingGroup combat;
    public final SimpleSortingGroup redstone;
    public final SimpleSortingGroup goodies;
    public final SimpleSortingGroup containers;
    public final SimpleSortingGroup rareMinerals;
    public final SimpleSortingGroup commonMinerals;
    public final SimpleSortingGroup brewing;
    public final ToolSortingGroup world;
    public final SimpleSortingGroup random;

    private final SortingGroup[] all;

    public SortingGroups(SortingLists lists) {
        this.pickaxes = new ToolSortingGroup(lists.PICKAXES, lists.PICKAXE_MINEABLES, "GROUP: PICKAXES");
        this.axes = new ToolSortingGroup(lists.AXES, lists.AXE_MINEABLES, "GROUP: AXES");
        this.shovels = new ToolSortingGroup(lists.SHOVELS, lists.SHOVEL_MINEABLES, "GROUP: SHOVELS");
        this.shears = new ToolSortingGroup(lists.SHEARS, lists.SHEAR_MINEABLES, "GROUP: SHEARS");
        this.hoes = new ToolSortingGroup(lists.HOES, lists.HOES_MINEABLE, "GROUP: HOES");
        this.consumables = new SimpleSortingGroup(lists.CONSUMEABLE_ITEMS, "GROUP: CONSUMABLES");
        this.combat = new ToolSortingGroup(lists.WEAPONS, lists.COMBAT_ITEMS, "GROUP: COMBAT");
        this.redstone = new SimpleSortingGroup(lists.REDSTONE_ITEMS, "GROUP: REDSTONE");
        this.goodies = new SimpleSortingGroup(lists.MISC_GOODIES, "GROUP: GOODIES");
        this.containers = new SimpleSortingGroup(lists.MISC_GONTAINERS, "GROUP: CONTAINERS");
        this.rareMinerals = new SimpleSortingGroup(lists.MINERAL_RESSOURCE_ITEMS, "GROUP: RARE MINERALS");
        this.commonMinerals = new SimpleSortingGroup(lists.MINERAL_RESSOURCE_COMMON_ITEMS, "GROUP: COMMON MINERALS");
        this.brewing = new SimpleSortingGroup(lists.MISC_BREWING, "GROUP: BREWING");
        this.world = new ToolSortingGroup(lists.FISHING, lists.MISC_EXPLORATION, "GROUP: WORLD");
        this.random = new SimpleSortingGroup(lists.MISC_OTHER, "GROUP: MISC OTHER");
        this.all = new SortingGroup[]{combat, consumables, goodies, rareMinerals, commonMinerals, redstone, pickaxes, axes, shovels, hoes, shears, brewing, world, containers, random};
    }

    /** Every group but the hotbar, in the order stacks try them. */
    public SortingGroup[] all() {
        return all;
    }
}
