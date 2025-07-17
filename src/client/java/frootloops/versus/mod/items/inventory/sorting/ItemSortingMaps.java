package frootloops.versus.mod.items.inventory.sorting;

import frootloops.versus.mod.environment.CustomBlockItems;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.Map;

public abstract class ItemSortingMaps {

    public static final Map<Item, Integer> ITEMS_REDSTONE = new HashMap<>();
    static {
        int index = 1;
        ITEMS_REDSTONE.put(Items.TNT, index++);
        ITEMS_REDSTONE.put(Items.REDSTONE, index++);
        ITEMS_REDSTONE.put(Items.REDSTONE_BLOCK, index++);
        ITEMS_REDSTONE.put(Items.REDSTONE_TORCH, index++);
        ITEMS_REDSTONE.put(Items.TARGET, index++);
        ITEMS_REDSTONE.put(Items.REPEATER, index++);
        ITEMS_REDSTONE.put(Items.COMPARATOR, index++);
        ITEMS_REDSTONE.put(Items.OBSERVER, index++);
        ITEMS_REDSTONE.put(Items.PISTON, index++);
        ITEMS_REDSTONE.put(Items.STICKY_PISTON, index++);
        ITEMS_REDSTONE.put(Items.SLIME_BLOCK, index++);
        ITEMS_REDSTONE.put(Items.HONEY_BLOCK, index++);
        ITEMS_REDSTONE.put(Items.DISPENSER, index++);
        ITEMS_REDSTONE.put(Items.DROPPER, index++);
        ITEMS_REDSTONE.put(Items.HOPPER, index++);
        ITEMS_REDSTONE.put(Items.CRAFTER, index++);
        ITEMS_REDSTONE.put(Items.CHEST, index++);
        ITEMS_REDSTONE.put(Items.TRAPPED_CHEST, index++);
        ITEMS_REDSTONE.put(Items.BARREL, index++);
        ITEMS_REDSTONE.put(Items.NOTE_BLOCK, index++);
        ITEMS_REDSTONE.put(Items.COMPOSTER, index++);
        ITEMS_REDSTONE.put(Items.WAXED_COPPER_BULB, index++);
        ITEMS_REDSTONE.put(Items.WAXED_EXPOSED_COPPER_BULB, index++);
        ITEMS_REDSTONE.put(Items.WAXED_WEATHERED_COPPER_BULB, index++);
        ITEMS_REDSTONE.put(Items.WAXED_OXIDIZED_COPPER_BULB, index++);
        ITEMS_REDSTONE.put(Items.LEVER, index++);
        ITEMS_REDSTONE.put(Items.TRIPWIRE_HOOK, index++);
        ITEMS_REDSTONE.put(Items.STONE_PRESSURE_PLATE, index++);
        ITEMS_REDSTONE.put(Items.CALIBRATED_SCULK_SENSOR, index++);
    }

    public static final Map<Item, Integer> ITEMS_MINECARTS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_REDSTONE.put(Items.RAIL, index++);
        ITEMS_REDSTONE.put(Items.POWERED_RAIL, index++);
        ITEMS_REDSTONE.put(Items.DETECTOR_RAIL, index++);
        ITEMS_REDSTONE.put(Items.ACTIVATOR_RAIL, index++);
        ITEMS_REDSTONE.put(Items.MINECART, index++);
        ITEMS_REDSTONE.put(Items.HOPPER_MINECART, index++);
        ITEMS_REDSTONE.put(Items.CHEST_MINECART, index++);
        ITEMS_REDSTONE.put(Items.FURNACE_MINECART, index++);
        ITEMS_REDSTONE.put(Items.TNT_MINECART, index++);
    }

    public static final Map<Item, Integer> ITEMS_TREASURE = new HashMap<>();
    static {
        int index = 1;
        ITEMS_TREASURE.put(Items.RECOVERY_COMPASS, index++);
        ITEMS_TREASURE.put(Items.ENCHANTING_TABLE, index++);
        ITEMS_TREASURE.put(Items.ENCHANTED_BOOK, index++);
        ITEMS_TREASURE.put(Items.EXPERIENCE_BOTTLE, index++);
        ITEMS_TREASURE.put(Items.DRAGON_EGG, index++);
        ITEMS_TREASURE.put(Items.DRAGON_HEAD, index++);
        ITEMS_TREASURE.put(Items.DRAGON_BREATH, index++);
        ITEMS_TREASURE.put(Items.HEAVY_CORE, index++);
        ITEMS_TREASURE.put(Items.OMINOUS_BOTTLE, index++);
        ITEMS_TREASURE.put(Items.OMINOUS_TRIAL_KEY, index++);
        ITEMS_TREASURE.put(Items.TRIAL_KEY, index++);
        ITEMS_TREASURE.put(Items.NETHER_STAR, index++);
    }

    public static final Map<Item, Integer> ITEMS_MONSTER_DROPS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_MONSTER_DROPS.put(Items.BREEZE_ROD, index++);
        ITEMS_MONSTER_DROPS.put(Items.BLAZE_ROD, index++);
        ITEMS_MONSTER_DROPS.put(Items.BLAZE_POWDER, index++);
        ITEMS_MONSTER_DROPS.put(Items.GHAST_TEAR, index++);
        ITEMS_MONSTER_DROPS.put(Items.PHANTOM_MEMBRANE, index++);
        ITEMS_MONSTER_DROPS.put(Items.LEATHER, index++);
        ITEMS_MONSTER_DROPS.put(Items.STRING, index++);
        ITEMS_MONSTER_DROPS.put(Items.BONE, index++);
        ITEMS_MONSTER_DROPS.put(Items.BONE_BLOCK, index++);
        ITEMS_MONSTER_DROPS.put(Items.SPIDER_EYE, index++);
        ITEMS_MONSTER_DROPS.put(Items.ROTTEN_FLESH, index++);
    }

    public static final Map<Item, Integer> ITEMS_SHEAR_AND_FISHING = new HashMap<>();
    static {
        int index = 1;
        ITEMS_SHEAR_AND_FISHING.put(Items.SHEARS, index++);
        ITEMS_SHEAR_AND_FISHING.put(Items.FISHING_ROD, index++);
        ITEMS_SHEAR_AND_FISHING.put(Items.CARROT_ON_A_STICK, index++);
        ITEMS_SHEAR_AND_FISHING.put(Items.WARPED_FUNGUS_ON_A_STICK, index++);
    }

    public static final Map<Item, Integer> ITEMS_SHEARS_WOOL_BLOCKS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WHITE_WOOL, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.LIGHT_GRAY_WOOL, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.GRAY_WOOL, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.BLACK_WOOL, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.BROWN_WOOL, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.RED_WOOL, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.ORANGE_WOOL, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.YELLOW_WOOL, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.LIME_WOOL, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.GREEN_WOOL, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CYAN_WOOL, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.LIGHT_BLUE_WOOL, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.BLUE_WOOL, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.PURPLE_WOOL, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.MAGENTA_WOOL, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.PINK_WOOL, index++);

        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WHITE_CARPET, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.LIGHT_GRAY_CARPET, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.GRAY_CARPET, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.BLACK_CARPET, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.BROWN_CARPET, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.RED_CARPET, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.ORANGE_CARPET, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.YELLOW_CARPET, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.LIME_CARPET, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.GREEN_CARPET, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CYAN_CARPET, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.LIGHT_BLUE_CARPET, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.BLUE_CARPET, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.PURPLE_CARPET, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.MAGENTA_CARPET, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.PINK_CARPET, index++);

        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WHITE_BANNER, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.LIGHT_GRAY_BANNER, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.GRAY_BANNER, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.BLACK_BANNER, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.BROWN_BANNER, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.RED_BANNER, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.ORANGE_BANNER, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.YELLOW_BANNER, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.LIME_BANNER, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.GREEN_BANNER, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CYAN_BANNER, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.LIGHT_BLUE_BANNER, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.BLUE_BANNER, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.PURPLE_BANNER, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.MAGENTA_BANNER, index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.PINK_BANNER, index++);
    }

    public static final Map<Item, Integer> ITEMS_ANIMALS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_ANIMALS.put(Items.NAME_TAG, index++);
        ITEMS_ANIMALS.put(Items.LEAD, index++);
        ITEMS_ANIMALS.put(Items.SADDLE, index++);
        ITEMS_ANIMALS.put(Items.DIAMOND_HORSE_ARMOR, index++);
        ITEMS_ANIMALS.put(Items.GOLDEN_HORSE_ARMOR, index++);
        ITEMS_ANIMALS.put(Items.IRON_HORSE_ARMOR, index++);
        ITEMS_ANIMALS.put(Items.LEATHER_HORSE_ARMOR, index++);
        ITEMS_ANIMALS.put(Items.WOLF_ARMOR, index++);
        ITEMS_ANIMALS.put(Items.ARMADILLO_SCUTE, index++);
        ITEMS_ANIMALS.put(Items.TURTLE_SCUTE, index++);
        ITEMS_ANIMALS.put(Items.TURTLE_EGG, index++);
    }

    public static final Map<Item, Integer> ITEMS_HOE_NATURE_BLOCKS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_HOE_NATURE_BLOCKS.put(Items.BONE_MEAL, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.MOSS_BLOCK, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.MOSS_CARPET, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.AZALEA, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.FLOWERING_AZALEA, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.GLOW_BERRIES, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.PALE_MOSS_BLOCK, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.PALE_MOSS_CARPET, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.PALE_HANGING_MOSS, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.WHEAT, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.HAY_BLOCK, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.PUMPKIN, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.MELON, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.WHEAT_SEEDS, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.PUMPKIN_SEEDS, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.MELON_SEEDS, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.BEETROOT_SEEDS, index++);
        ITEMS_HOE_NATURE_BLOCKS.put(Items.TORCHFLOWER_SEEDS, index++);
    }

    public static final Map<Item, Integer> ITEMS_HOE_SCULK = new HashMap<>();
    static {
        int index = 1;
        ITEMS_HOE_SCULK.put(Items.SCULK, index++);
        ITEMS_HOE_SCULK.put(Items.SCULK_CATALYST, index++);
        ITEMS_HOE_SCULK.put(Items.SCULK_SHRIEKER, index++);
        ITEMS_HOE_SCULK.put(Items.SCULK_SENSOR, index++);
        ITEMS_HOE_SCULK.put(Items.SCULK_VEIN, index++);
        ITEMS_HOE_SCULK.put(Items.ECHO_SHARD, index++);
    }

    public static final Map<Item, Integer> ITEMS_HOE_NETHER_BLOCKS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_HOE_NETHER_BLOCKS.put(Items.NETHER_WART, index++);
        ITEMS_HOE_NETHER_BLOCKS.put(CustomBlockItems.CORRUPTED_WART, index++);
        ITEMS_HOE_NETHER_BLOCKS.put(CustomBlockItems.WITHERED_WART, index++);

        ITEMS_HOE_NETHER_BLOCKS.put(Items.NETHER_WART_BLOCK, index++);
        ITEMS_HOE_NETHER_BLOCKS.put(Items.CRIMSON_FUNGUS, index++);
        ITEMS_HOE_NETHER_BLOCKS.put(Items.CRIMSON_ROOTS, index++);
        ITEMS_HOE_NETHER_BLOCKS.put(Items.WEEPING_VINES, index++);

        ITEMS_HOE_NETHER_BLOCKS.put(Items.WARPED_WART_BLOCK, index++);
        ITEMS_HOE_NETHER_BLOCKS.put(Items.WARPED_FUNGUS, index++);
        ITEMS_HOE_NETHER_BLOCKS.put(Items.WARPED_ROOTS, index++);
        ITEMS_HOE_NETHER_BLOCKS.put(Items.TWISTING_VINES, index++);
    }


    public static final Map<Item, Integer> ITEMS_PICKAXE_NETHER_BLOCKS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.NETHERRACK, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.RED_NETHER_BRICKS, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.RED_NETHER_BRICK_SLAB, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.RED_NETHER_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.NETHER_BRICKS, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.NETHER_BRICK_SLAB, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.NETHER_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.NETHER_BRICK_FENCE, index++);

        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.GILDED_BLACKSTONE, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.BLACKSTONE, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.BLACKSTONE_SLAB, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.BLACKSTONE_STAIRS, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.BLACKSTONE_WALL, index++);

        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.POLISHED_BLACKSTONE, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.POLISHED_BLACKSTONE_BRICKS, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.CRACKED_POLISHED_BLACKSTONE_BRICKS, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.CHISELED_POLISHED_BLACKSTONE, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.POLISHED_BLACKSTONE_SLAB, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.POLISHED_BLACKSTONE_BRICK_SLAB, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.POLISHED_BLACKSTONE_STAIRS, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.POLISHED_BLACKSTONE_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.POLISHED_BLACKSTONE_WALL, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.POLISHED_BLACKSTONE_BRICK_WALL, index++);

        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.SMOOTH_BASALT, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.BASALT, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.POLISHED_BASALT, index++);
    }

    public static final Map<Item, Integer> ITEMS_PICKAXE_GRAY_STONES = new HashMap<>();
    static {
        int index = 1;
        ITEMS_PICKAXE_GRAY_STONES.put(Items.COBBLED_DEEPSLATE, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.DEEPSLATE, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.CRACKED_DEEPSLATE_TILES, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.DEEPSLATE_TILES, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.POLISHED_DEEPSLATE, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.CRACKED_DEEPSLATE_BRICKS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.DEEPSLATE_BRICKS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.COBBLED_DEEPSLATE_SLAB, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.POLISHED_DEEPSLATE_SLAB, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.DEEPSLATE_BRICK_SLAB, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.DEEPSLATE_TILE_SLAB, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.COBBLED_DEEPSLATE_STAIRS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.POLISHED_DEEPSLATE_STAIRS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.DEEPSLATE_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.DEEPSLATE_TILE_STAIRS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.COBBLED_DEEPSLATE_WALL, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.POLISHED_DEEPSLATE_WALL, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.DEEPSLATE_BRICK_WALL, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.DEEPSLATE_TILE_WALL, index++);

        ITEMS_PICKAXE_GRAY_STONES.put(Items.TUFF, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.POLISHED_TUFF, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.TUFF_BRICKS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.CHISELED_TUFF, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.CHISELED_TUFF_BRICKS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.TUFF_SLAB, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.POLISHED_TUFF_SLAB, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.TUFF_BRICK_SLAB, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.TUFF_STAIRS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.POLISHED_TUFF_STAIRS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.TUFF_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.TUFF_WALL, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.POLISHED_TUFF_WALL, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.TUFF_BRICK_WALL, index++);

        ITEMS_PICKAXE_GRAY_STONES.put(Items.MOSSY_STONE_BRICKS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.CRACKED_STONE_BRICKS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.STONE_BRICKS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.STONE_BRICK_SLAB, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.STONE_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.STONE_BRICK_WALL, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.CHISELED_STONE_BRICKS, index++);

        ITEMS_PICKAXE_GRAY_STONES.put(Items.MOSSY_COBBLESTONE, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.MOSSY_COBBLESTONE_SLAB, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.MOSSY_COBBLESTONE_STAIRS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.MOSSY_COBBLESTONE_WALL, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.COBBLESTONE, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.COBBLESTONE_SLAB, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.COBBLESTONE_STAIRS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.COBBLESTONE_WALL, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.DEAD_TUBE_CORAL_BLOCK, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.DEAD_BRAIN_CORAL_BLOCK, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.DEAD_BUBBLE_CORAL_BLOCK, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.DEAD_FIRE_CORAL_BLOCK, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.DEAD_HORN_CORAL_BLOCK, index++);

        ITEMS_PICKAXE_GRAY_STONES.put(Items.STONE, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.ANDESITE, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.STONE_SLAB, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.ANDESITE_SLAB, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.STONE_STAIRS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.ANDESITE_STAIRS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.ANDESITE_WALL, index++);

        ITEMS_PICKAXE_GRAY_STONES.put(CustomBlockItems.POLISHED_STONE, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.POLISHED_ANDESITE, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(CustomBlockItems.POLISHED_STONE_SLAB, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.POLISHED_ANDESITE_SLAB, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(CustomBlockItems.POLISHED_STONE_STAIRS, index++);
        ITEMS_PICKAXE_GRAY_STONES.put(Items.POLISHED_ANDESITE_STAIRS, index++);
    }

    public static final Map<Item, Integer> ITEMS_PICKAXE_PALE_STONES = new HashMap<>();
    static {
        int index = 1;
        ITEMS_PICKAXE_PALE_STONES.put(Items.BLUE_ICE, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.PACKED_ICE, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.ICE, index++);

        ITEMS_PICKAXE_PALE_STONES.put(Items.DIORITE, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.POLISHED_DIORITE, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.DIORITE_SLAB, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.POLISHED_DIORITE_SLAB, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.DIORITE_STAIRS, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.POLISHED_DIORITE_STAIRS, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.DIORITE_WALL, index++);

        ITEMS_PICKAXE_PALE_STONES.put(Items.CALCITE, index++);

        ITEMS_PICKAXE_PALE_STONES.put(Items.QUARTZ_BLOCK, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.QUARTZ_BRICKS, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.QUARTZ_PILLAR, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.SMOOTH_QUARTZ, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.QUARTZ_SLAB, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.SMOOTH_QUARTZ_SLAB, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.QUARTZ_STAIRS, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.SMOOTH_QUARTZ_STAIRS, index++);

        ITEMS_PICKAXE_PALE_STONES.put(Items.SANDSTONE, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.SMOOTH_SANDSTONE, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.CUT_SANDSTONE, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.CHISELED_SANDSTONE, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.SANDSTONE_SLAB, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.SMOOTH_SANDSTONE_SLAB, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.CUT_SANDSTONE_SLAB, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.SANDSTONE_STAIRS, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.SMOOTH_SANDSTONE_STAIRS, index++);
    }

    public static final Map<Item, Integer> ITEMS_PICKAXE_WARM_BLOCKS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.PACKED_MUD, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.BROWN_CLAY, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.MUD_BRICKS, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.BROWN_CLAY_BRICKS, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.BROWN_MUD_BRICKS, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.MUD_BRICK_SLAB, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.BROWN_CLAY_BRICK_SLAB, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.BROWN_MUD_BRICK_SLAB, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.MUD_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.BROWN_CLAY_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.BROWN_MUD_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.MUD_BRICK_WALL, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.BROWN_CLAY_BRICK_WALL, index++);

        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.DRIPSTONE_BLOCK, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.POLISHED_DRIPSTONE, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.DRIPSTONE_PILLAR, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.DRIPSTONE_BRICKS, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.DRIPSTONE_SLAB, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.POLISHED_DRIPSTONE_SLAB, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.DRIPSTONE_BRICK_SLAB, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.DRIPSTONE_STAIRS, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.POLISHED_DRIPSTONE_STAIRS, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.DRIPSTONE_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.DRIPSTONE_WALL, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(CustomBlockItems.DRIPSTONE_BRICK_WALL, index++);

        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.GRANITE, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.POLISHED_GRANITE, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.GRANITE_SLAB, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.POLISHED_GRANITE_SLAB, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.GRANITE_STAIRS, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.POLISHED_GRANITE_STAIRS, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.GRANITE_WALL, index++);

        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.BRICKS, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.BRICK_SLAB, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.BRICK_STAIRS, index++);
        ITEMS_PICKAXE_WARM_BLOCKS.put(Items.BRICK_WALL, index++);
    }

    public static final Map<Item, Integer> ITEMS_PICKAXE_TERRACOTTA_BLOCKS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(CustomBlockItems.TERRACOTTA_BRICKS, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(CustomBlockItems.TERRACOTTA_BRICK_SLAB, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(CustomBlockItems.TERRACOTTA_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(CustomBlockItems.TERRACOTTA_BRICK_WALL, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(CustomBlockItems.TERRACOTTA_TILES, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(CustomBlockItems.TERRACOTTA_TILE_SLAB, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(CustomBlockItems.TERRACOTTA_TILE_STAIRS, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(CustomBlockItems.CHISELED_TERRACOTTA, index++);

        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.ORANGE_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.RED_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.YELLOW_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.WHITE_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.LIGHT_GRAY_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GRAY_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.BROWN_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GREEN_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.LIME_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.CYAN_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.BLACK_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.PINK_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.MAGENTA_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.PURPLE_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.BLUE_TERRACOTTA, index++);

        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.ORANGE_GLAZED_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.RED_GLAZED_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.YELLOW_GLAZED_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.WHITE_GLAZED_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.LIGHT_GRAY_GLAZED_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GRAY_GLAZED_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.BROWN_GLAZED_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GREEN_GLAZED_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.LIME_GLAZED_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.CYAN_GLAZED_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.BLACK_GLAZED_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.PINK_GLAZED_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.MAGENTA_GLAZED_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.PURPLE_GLAZED_TERRACOTTA, index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.BLUE_GLAZED_TERRACOTTA, index++);
    }




    public static final Map<Item, Integer> ITEMS_SHOVEL_MINEABLE = new HashMap<>();
    static {
        int index = 1;
        ITEMS_SHOVEL_MINEABLE.put(Items.CLAY, index++);
        ITEMS_SHOVEL_MINEABLE.put(CustomBlockItems.GRAY_CLAY, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.MUD, index++);
        ITEMS_SHOVEL_MINEABLE.put(CustomBlockItems.GRAY_MUD, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.GRAVEL, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.SUSPICIOUS_GRAVEL, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.SAND, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.SUSPICIOUS_SAND, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.RED_SAND, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.DIRT, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.DIRT_PATH, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.MYCELIUM, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.PODZOL, index++);
        ITEMS_SHOVEL_MINEABLE.put(CustomBlockItems.BROWN_CLAY, index++);
        ITEMS_SHOVEL_MINEABLE.put(CustomBlockItems.BROWN_MUD, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.FARMLAND, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.COARSE_DIRT, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.ROOTED_DIRT, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.GRASS_BLOCK, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.MOSS_BLOCK, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.MOSS_CARPET, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.MOSS_BLOCK, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.MOSS_CARPET, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.SOUL_SAND, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.SOUL_SOIL, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.SNOW_BLOCK, index++);
        ITEMS_SHOVEL_MINEABLE.put(Items.SNOW, index++);
    }

    public static final Map<Item, Integer> ITEMS_COPPER_BLOCKS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_CUT_COPPER, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_EXPOSED_CUT_COPPER, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_WEATHERED_CUT_COPPER, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_OXIDIZED_CUT_COPPER, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_COPPER_BLOCK, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_EXPOSED_COPPER, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_WEATHERED_COPPER, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_OXIDIZED_COPPER, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_CUT_COPPER_SLAB, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_EXPOSED_CUT_COPPER_SLAB, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_WEATHERED_CUT_COPPER_SLAB, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_OXIDIZED_CUT_COPPER_SLAB, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_CUT_COPPER_STAIRS, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_EXPOSED_CUT_COPPER_STAIRS, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_WEATHERED_CUT_COPPER_STAIRS, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_OXIDIZED_CUT_COPPER_STAIRS, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_COPPER_TRAPDOOR, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_EXPOSED_COPPER_TRAPDOOR, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_WEATHERED_COPPER_TRAPDOOR, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_OXIDIZED_COPPER_TRAPDOOR, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_COPPER_DOOR, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_EXPOSED_COPPER_DOOR, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_WEATHERED_COPPER_DOOR, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_OXIDIZED_COPPER_DOOR, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_COPPER_GRATE, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_EXPOSED_COPPER_GRATE, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_WEATHERED_COPPER_GRATE, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_OXIDIZED_COPPER_GRATE, index++);
        ITEMS_COPPER_BLOCKS.put(Items.CUT_COPPER, index++);
        ITEMS_COPPER_BLOCKS.put(Items.EXPOSED_CUT_COPPER, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WEATHERED_CUT_COPPER, index++);
        ITEMS_COPPER_BLOCKS.put(Items.OXIDIZED_CUT_COPPER, index++);
        ITEMS_COPPER_BLOCKS.put(Items.COPPER_BLOCK, index++);
        ITEMS_COPPER_BLOCKS.put(Items.EXPOSED_COPPER, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WEATHERED_COPPER, index++);
        ITEMS_COPPER_BLOCKS.put(Items.OXIDIZED_COPPER, index++);
        ITEMS_COPPER_BLOCKS.put(Items.CUT_COPPER_SLAB, index++);
        ITEMS_COPPER_BLOCKS.put(Items.EXPOSED_CUT_COPPER_SLAB, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WEATHERED_CUT_COPPER_SLAB, index++);
        ITEMS_COPPER_BLOCKS.put(Items.OXIDIZED_CUT_COPPER_SLAB, index++);
        ITEMS_COPPER_BLOCKS.put(Items.CUT_COPPER_STAIRS, index++);
        ITEMS_COPPER_BLOCKS.put(Items.EXPOSED_CUT_COPPER_STAIRS, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WEATHERED_CUT_COPPER_STAIRS, index++);
        ITEMS_COPPER_BLOCKS.put(Items.OXIDIZED_CUT_COPPER_STAIRS, index++);
        ITEMS_COPPER_BLOCKS.put(Items.COPPER_TRAPDOOR, index++);
        ITEMS_COPPER_BLOCKS.put(Items.EXPOSED_COPPER_TRAPDOOR, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WEATHERED_COPPER_TRAPDOOR, index++);
        ITEMS_COPPER_BLOCKS.put(Items.OXIDIZED_COPPER_TRAPDOOR, index++);
        ITEMS_COPPER_BLOCKS.put(Items.COPPER_DOOR, index++);
        ITEMS_COPPER_BLOCKS.put(Items.EXPOSED_COPPER_DOOR, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WEATHERED_COPPER_DOOR, index++);
        ITEMS_COPPER_BLOCKS.put(Items.OXIDIZED_COPPER_DOOR, index++);
        ITEMS_COPPER_BLOCKS.put(Items.COPPER_GRATE, index++);
        ITEMS_COPPER_BLOCKS.put(Items.EXPOSED_COPPER_GRATE, index++);
        ITEMS_COPPER_BLOCKS.put(Items.WEATHERED_COPPER_GRATE, index++);
        ITEMS_COPPER_BLOCKS.put(Items.OXIDIZED_COPPER_GRATE, index++);
    }

    public static final Map<Item, Integer> ITEMS_MINERAL_RESSOURCES = new HashMap<>();
    static {
        int index = 1;
        ITEMS_MINERAL_RESSOURCES.put(Items.BEACON, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.ANCIENT_DEBRIS, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.NETHERITE_SCRAP, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.NETHERITE_INGOT, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.NETHERITE_BLOCK, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.GOLD_BLOCK, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.GOLD_INGOT, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.GOLD_NUGGET, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.RAW_GOLD_BLOCK, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.RAW_GOLD, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.GOLD_ORE, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.DEEPSLATE_GOLD_ORE, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.NETHER_GOLD_ORE, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.IRON_BLOCK, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.IRON_INGOT, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.IRON_NUGGET, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.RAW_IRON_BLOCK, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.RAW_IRON, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.IRON_ORE, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.DEEPSLATE_IRON_ORE, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.EMERALD_BLOCK, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.EMERALD, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.EMERALD_ORE, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.DEEPSLATE_EMERALD_ORE, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.DIAMOND_BLOCK, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.DIAMOND, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.DIAMOND_ORE, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.DEEPSLATE_DIAMOND_ORE, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.LAPIS_BLOCK, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.LAPIS_LAZULI, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.LAPIS_ORE, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.DEEPSLATE_LAPIS_ORE, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.COPPER_INGOT, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.RAW_COPPER_BLOCK, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.RAW_COPPER, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.COPPER_ORE, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.DEEPSLATE_COPPER_ORE, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.CHARCOAL, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.COAL, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.COAL_ORE, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.DEEPSLATE_COAL_ORE, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.AMETHYST_BLOCK, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.AMETHYST_SHARD, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.AMETHYST_CLUSTER, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.GLOWSTONE_DUST, index++);
        ITEMS_MINERAL_RESSOURCES.put(Items.GLOWSTONE, index++);
    }
}
