package frootloops.versus.mod.items_and_effects.inventory.sorting;

import frootloops.versus.mod.environment.CustomBlockItems;
import frootloops.versus.mod.items_and_effects.CustomBrewingItems;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public abstract class ItemSortingMaps {

    public static final Map<Item, Integer> ITEMS_REDSTONE_COMPONENTS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_REDSTONE_COMPONENTS.put(Items.TNT, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.REDSTONE, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.REDSTONE_BLOCK, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.REDSTONE_TORCH, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.TARGET, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.REPEATER, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.COMPARATOR, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.OBSERVER, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.PISTON, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.STICKY_PISTON, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.SLIME_BLOCK, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.HONEY_BLOCK, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.DISPENSER, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.DROPPER, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.HOPPER, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.CRAFTER, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.CHEST, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.TRAPPED_CHEST, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.BARREL, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.NOTE_BLOCK, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.COMPOSTER, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.COPPER_BULB.waxed().unaffected(), index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.COPPER_BULB.waxed().exposed(), index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.COPPER_BULB.waxed().weathered(), index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.COPPER_BULB.waxed().oxidized(), index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.LEVER, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.TRIPWIRE_HOOK, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.STONE_PRESSURE_PLATE, index++);
        ITEMS_REDSTONE_COMPONENTS.put(Items.CALIBRATED_SCULK_SENSOR, index++);
    }

    public static final Map<Item, Integer> ITEMS_MINECARTS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_MINECARTS.put(Items.MINECART, index++);
        ITEMS_MINECARTS.put(Items.HOPPER_MINECART, index++);
        ITEMS_MINECARTS.put(Items.CHEST_MINECART, index++);
        ITEMS_MINECARTS.put(Items.FURNACE_MINECART, index++);
        ITEMS_MINECARTS.put(Items.TNT_MINECART, index++);
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
        ITEMS_TREASURE.put(Items.BEACON, index++);
        ITEMS_TREASURE.put(Items.ANCIENT_DEBRIS, index++);
        ITEMS_TREASURE.put(Items.NETHERITE_SCRAP, index++);
        ITEMS_TREASURE.put(Items.NETHERITE_INGOT, index++);
        ITEMS_TREASURE.put(Items.NETHERITE_BLOCK, index++);
    }

    public static final Map<Item, Integer> ITEMS_BREWING_MISC = new HashMap<>();
    static {
        int index = 1;
        ITEMS_BREWING_MISC.put(CustomBrewingItems.CORRUPTED_WART_POWDER, index++);
        ITEMS_BREWING_MISC.put(Items.GUNPOWDER, index++);
        ITEMS_BREWING_MISC.put(Items.SUGAR, index++);
        ITEMS_BREWING_MISC.put(Items.GLOWSTONE_DUST, index++);
        ITEMS_BREWING_MISC.put(Items.GLOWSTONE, index++);;
        ITEMS_BREWING_MISC.put(Items.ECHO_SHARD, index++);
        ITEMS_BREWING_MISC.put(Items.DRAGON_BREATH, index++);
        ITEMS_BREWING_MISC.put(Items.POPPED_CHORUS_FRUIT, index++);
        ITEMS_BREWING_MISC.put(Items.SHULKER_SHELL, index++);
        ITEMS_BREWING_MISC.put(Items.PHANTOM_MEMBRANE, index++);
        ITEMS_BREWING_MISC.put(CustomBrewingItems.LIVING_FLAME, index++);
        ITEMS_BREWING_MISC.put(Items.BLAZE_ROD, index++);
        ITEMS_BREWING_MISC.put(Items.BLAZE_POWDER, index++);
        ITEMS_BREWING_MISC.put(Items.BREEZE_ROD, index++);
        ITEMS_BREWING_MISC.put(Items.GHAST_TEAR, index++);
        ITEMS_BREWING_MISC.put(Items.PRISMARINE_CRYSTALS, index++);
        ITEMS_BREWING_MISC.put(Items.PRISMARINE_SHARD, index++);
        ITEMS_BREWING_MISC.put(Items.NAUTILUS_SHELL, index++);
        ITEMS_BREWING_MISC.put(Items.PUFFERFISH, index++);
        ITEMS_BREWING_MISC.put(Items.TURTLE_SCUTE, index++);
        ITEMS_BREWING_MISC.put(CustomBrewingItems.FOUR_LEAF_CLOVER, index++);
        ITEMS_BREWING_MISC.put(Items.RABBIT_FOOT, index++);
        ITEMS_BREWING_MISC.put(Items.ARMADILLO_SCUTE, index++);
    }

    public static final Map<Item, Integer> ITEMS_TRUE_TRASH = new HashMap<>();
    static {
        int index = 1;
        ITEMS_TRUE_TRASH.put(Items.GLASS_BOTTLE, index++);
        ITEMS_TRUE_TRASH.put(Items.LEATHER, index++);
        ITEMS_TRUE_TRASH.put(Items.RABBIT_HIDE, index++);
        ITEMS_TRUE_TRASH.put(Items.BOWL, index++);
        ITEMS_TRUE_TRASH.put(Items.STICK, index++);
        ITEMS_TRUE_TRASH.put(Items.LEAF_LITTER, index++);
        ITEMS_TRUE_TRASH.put(Items.INK_SAC, index++);
        ITEMS_TRUE_TRASH.put(Items.GLOW_INK_SAC, index++);
        ITEMS_TRUE_TRASH.put(Items.ROTTEN_FLESH, index++);
    }

    public static final Map<Item, Integer> ITEMS_FISHING = new HashMap<>();
    static {
        int index = 1;
        ITEMS_FISHING.put(Items.SHEARS, index++);
        ITEMS_FISHING.put(Items.FISHING_ROD, index++);
        ITEMS_FISHING.put(Items.CARROT_ON_A_STICK, index++);
        ITEMS_FISHING.put(Items.WARPED_FUNGUS_ON_A_STICK, index++);
    }

    public static final Map<Item, Integer> ITEMS_SHEARS_WOOL_BLOCKS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WOOL.white(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WOOL.lightGray(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WOOL.gray(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WOOL.black(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WOOL.brown(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WOOL.red(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WOOL.orange(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WOOL.yellow(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WOOL.lime(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WOOL.green(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WOOL.cyan(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WOOL.lightBlue(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WOOL.blue(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WOOL.purple(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WOOL.magenta(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.WOOL.pink(), index++);

        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CARPET.white(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CARPET.lightGray(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CARPET.gray(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CARPET.black(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CARPET.brown(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CARPET.red(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CARPET.orange(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CARPET.yellow(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CARPET.lime(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CARPET.green(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CARPET.cyan(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CARPET.lightBlue(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CARPET.blue(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CARPET.purple(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CARPET.magenta(), index++);
        ITEMS_SHEARS_WOOL_BLOCKS.put(Items.CARPET.pink(), index++);
    }

    public static final Map<Item, Integer> ITEMS_BANNERS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_BANNERS.put(Items.BANNER.white(), index++);
        ITEMS_BANNERS.put(Items.BANNER.lightGray(), index++);
        ITEMS_BANNERS.put(Items.BANNER.gray(), index++);
        ITEMS_BANNERS.put(Items.BANNER.black(), index++);
        ITEMS_BANNERS.put(Items.BANNER.brown(), index++);
        ITEMS_BANNERS.put(Items.BANNER.red(), index++);
        ITEMS_BANNERS.put(Items.BANNER.orange(), index++);
        ITEMS_BANNERS.put(Items.BANNER.yellow(), index++);
        ITEMS_BANNERS.put(Items.BANNER.lime(), index++);
        ITEMS_BANNERS.put(Items.BANNER.green(), index++);
        ITEMS_BANNERS.put(Items.BANNER.cyan(), index++);
        ITEMS_BANNERS.put(Items.BANNER.lightBlue(), index++);
        ITEMS_BANNERS.put(Items.BANNER.blue(), index++);
        ITEMS_BANNERS.put(Items.BANNER.purple(), index++);
        ITEMS_BANNERS.put(Items.BANNER.magenta(), index++);
        ITEMS_BANNERS.put(Items.BANNER.pink(), index++);
    }

    public static final Map<Item, Integer> ITEMS_CANDLES = new HashMap<>();
    static {
        int index = 1;
        ITEMS_CANDLES.put(Items.DYED_CANDLE.white(), index++);
        ITEMS_CANDLES.put(Items.DYED_CANDLE.lightGray(), index++);
        ITEMS_CANDLES.put(Items.DYED_CANDLE.gray(), index++);
        ITEMS_CANDLES.put(Items.DYED_CANDLE.black(), index++);
        ITEMS_CANDLES.put(Items.DYED_CANDLE.brown(), index++);
        ITEMS_CANDLES.put(Items.DYED_CANDLE.red(), index++);
        ITEMS_CANDLES.put(Items.DYED_CANDLE.orange(), index++);
        ITEMS_CANDLES.put(Items.DYED_CANDLE.yellow(), index++);
        ITEMS_CANDLES.put(Items.DYED_CANDLE.lime(), index++);
        ITEMS_CANDLES.put(Items.DYED_CANDLE.green(), index++);
        ITEMS_CANDLES.put(Items.DYED_CANDLE.cyan(), index++);
        ITEMS_CANDLES.put(Items.DYED_CANDLE.lightBlue(), index++);
        ITEMS_CANDLES.put(Items.DYED_CANDLE.blue(), index++);
        ITEMS_CANDLES.put(Items.DYED_CANDLE.purple(), index++);
        ITEMS_CANDLES.put(Items.DYED_CANDLE.magenta(), index++);
        ITEMS_CANDLES.put(Items.DYED_CANDLE.pink(), index++);
    }

    public static final Map<Item, Integer> ITEMS_BEDS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_BEDS.put(Items.BED.white(), index++);
        ITEMS_BEDS.put(Items.BED.lightGray(), index++);
        ITEMS_BEDS.put(Items.BED.gray(), index++);
        ITEMS_BEDS.put(Items.BED.black(), index++);
        ITEMS_BEDS.put(Items.BED.brown(), index++);
        ITEMS_BEDS.put(Items.BED.red(), index++);
        ITEMS_BEDS.put(Items.BED.orange(), index++);
        ITEMS_BEDS.put(Items.BED.yellow(), index++);
        ITEMS_BEDS.put(Items.BED.lime(), index++);
        ITEMS_BEDS.put(Items.BED.green(), index++);
        ITEMS_BEDS.put(Items.BED.cyan(), index++);
        ITEMS_BEDS.put(Items.BED.lightBlue(), index++);
        ITEMS_BEDS.put(Items.BED.blue(), index++);
        ITEMS_BEDS.put(Items.BED.purple(), index++);
        ITEMS_BEDS.put(Items.BED.magenta(), index++);
        ITEMS_BEDS.put(Items.BED.pink(), index++);
    }

    public static final Map<Item, Integer> ITEMS_SHEARS_SHRUBBERY = new HashMap<>();
    static {
        int index = 1;
        ITEMS_SHEARS_SHRUBBERY.put(Items.SHORT_GRASS, index++);
        ITEMS_SHEARS_SHRUBBERY.put(Items.TALL_GRASS, index++);
        ITEMS_SHEARS_SHRUBBERY.put(Items.FERN, index++);
        ITEMS_SHEARS_SHRUBBERY.put(Items.LARGE_FERN, index++);
        ITEMS_SHEARS_SHRUBBERY.put(CustomBlockItems.CLOVERS, index++);
        ITEMS_SHEARS_SHRUBBERY.put(CustomBlockItems.WHEAT_GRASS, index++);
        ITEMS_SHEARS_SHRUBBERY.put(CustomBlockItems.WILD_WHEAT, index++);
        ITEMS_SHEARS_SHRUBBERY.put(Items.DEAD_BUSH, index++);
        ITEMS_SHEARS_SHRUBBERY.put(Items.HANGING_ROOTS, index++);
        ITEMS_SHEARS_SHRUBBERY.put(Items.VINE, index++);
        ITEMS_SHEARS_SHRUBBERY.put(Items.CACTUS, index++);
        ITEMS_SHEARS_SHRUBBERY.put(Items.HANGING_ROOTS, index++);
        ITEMS_SHEARS_SHRUBBERY.put(Items.GLOW_LICHEN, index++);
    }

    public static final Map<Item, Integer> ITEMS_SHEARS_AQUATIC = new HashMap<>();
    static {
        int index = 1;
        ITEMS_SHEARS_AQUATIC.put(Items.LILY_PAD, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.SEA_PICKLE, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.SEAGRASS, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.KELP, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.DRIED_KELP_BLOCK, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.TUBE_CORAL, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.TUBE_CORAL_FAN, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.BRAIN_CORAL, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.BRAIN_CORAL_FAN, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.BUBBLE_CORAL, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.BUBBLE_CORAL_FAN, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.FIRE_CORAL, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.FIRE_CORAL_FAN, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.HORN_CORAL, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.HORN_CORAL_FAN, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.DEAD_TUBE_CORAL, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.DEAD_TUBE_CORAL_FAN, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.DEAD_BRAIN_CORAL, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.DEAD_BRAIN_CORAL_FAN, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.DEAD_BUBBLE_CORAL, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.DEAD_BUBBLE_CORAL_FAN, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.DEAD_FIRE_CORAL, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.DEAD_FIRE_CORAL_FAN, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.DEAD_HORN_CORAL, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.DEAD_HORN_CORAL_FAN, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.TUBE_CORAL_BLOCK, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.BRAIN_CORAL_BLOCK, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.FIRE_CORAL_BLOCK, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.HORN_CORAL_BLOCK, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.DEAD_TUBE_CORAL_BLOCK, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.DEAD_BRAIN_CORAL_BLOCK, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.DEAD_FIRE_CORAL_BLOCK, index++);
        ITEMS_SHEARS_AQUATIC.put(Items.DEAD_HORN_CORAL_BLOCK, index++);
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
        ITEMS_ANIMALS.put(Items.HARNESS.white(), index++);
        ITEMS_ANIMALS.put(Items.HARNESS.lightGray(), index++);
        ITEMS_ANIMALS.put(Items.HARNESS.gray(), index++);
        ITEMS_ANIMALS.put(Items.HARNESS.black(), index++);
        ITEMS_ANIMALS.put(Items.HARNESS.brown(), index++);
        ITEMS_ANIMALS.put(Items.HARNESS.red(), index++);
        ITEMS_ANIMALS.put(Items.HARNESS.orange(), index++);
        ITEMS_ANIMALS.put(Items.HARNESS.yellow(), index++);
        ITEMS_ANIMALS.put(Items.HARNESS.lime(), index++);
        ITEMS_ANIMALS.put(Items.HARNESS.green(), index++);
        ITEMS_ANIMALS.put(Items.HARNESS.cyan(), index++);
        ITEMS_ANIMALS.put(Items.HARNESS.lightBlue(), index++);
        ITEMS_ANIMALS.put(Items.HARNESS.blue(), index++);
        ITEMS_ANIMALS.put(Items.HARNESS.purple(), index++);
        ITEMS_ANIMALS.put(Items.HARNESS.magenta(), index++);
        ITEMS_ANIMALS.put(Items.HARNESS.pink(), index++);
        ITEMS_ANIMALS.put(Items.WOLF_ARMOR, index++);
        ITEMS_ANIMALS.put(Items.ARMADILLO_SCUTE, index++);
        ITEMS_ANIMALS.put(Items.TURTLE_SCUTE, index++);
        ITEMS_ANIMALS.put(Items.TURTLE_EGG, index++);
        ITEMS_ANIMALS.put(Items.DRIED_GHAST, index++);
    }

    public static final Map<Item, Integer> ITEMS_DYES = new HashMap<>();
    static {
        int index = 1;
        ITEMS_DYES.put(Items.DYE.white(), index++);
        ITEMS_DYES.put(Items.DYE.lightGray(), index++);
        ITEMS_DYES.put(Items.DYE.gray(), index++);
        ITEMS_DYES.put(Items.DYE.black(), index++);
        ITEMS_DYES.put(Items.DYE.brown(), index++);
        ITEMS_DYES.put(Items.DYE.red(), index++);
        ITEMS_DYES.put(Items.DYE.orange(), index++);
        ITEMS_DYES.put(Items.DYE.yellow(), index++);
        ITEMS_DYES.put(Items.DYE.lime(), index++);
        ITEMS_DYES.put(Items.DYE.green(), index++);
        ITEMS_DYES.put(Items.DYE.cyan(), index++);
        ITEMS_DYES.put(Items.DYE.lightBlue(), index++);
        ITEMS_DYES.put(Items.DYE.blue(), index++);
        ITEMS_DYES.put(Items.DYE.purple(), index++);
        ITEMS_DYES.put(Items.DYE.magenta(), index++);
        ITEMS_DYES.put(Items.DYE.pink(), index++);
    }

    public static final Map<Item, Integer> ITEMS_FLOWERS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_FLOWERS.put(Items.OXEYE_DAISY, index++);
        ITEMS_FLOWERS.put(Items.AZURE_BLUET, index++);
        ITEMS_FLOWERS.put(Items.LILY_OF_THE_VALLEY, index++);
        ITEMS_FLOWERS.put(Items.WHITE_TULIP, index++);
        ITEMS_FLOWERS.put(Items.PINK_TULIP, index++);
        ITEMS_FLOWERS.put(Items.ORANGE_TULIP, index++);
        ITEMS_FLOWERS.put(Items.RED_TULIP, index++);
        ITEMS_FLOWERS.put(Items.POPPY, index++);
        ITEMS_FLOWERS.put(Items.ROSE_BUSH, index++);
        ITEMS_FLOWERS.put(Items.LILAC, index++);
        ITEMS_FLOWERS.put(Items.PEONY, index++);
        ITEMS_FLOWERS.put(Items.ALLIUM, index++);
        ITEMS_FLOWERS.put(Items.PINK_PETALS, index++);
        ITEMS_FLOWERS.put(Items.SPORE_BLOSSOM, index++);
        ITEMS_FLOWERS.put(Items.BLUE_ORCHID, index++);
        ITEMS_FLOWERS.put(Items.CORNFLOWER, index++);
        ITEMS_FLOWERS.put(Items.SUNFLOWER, index++);
        ITEMS_FLOWERS.put(Items.DANDELION, index++);
        ITEMS_FLOWERS.put(Items.WITHER_ROSE, index++);
        ITEMS_FLOWERS.put(Items.TORCHFLOWER, index++);
        ITEMS_FLOWERS.put(Items.PITCHER_PLANT, index++);
    }

    public static final Map<Item, Integer> ITEMS_HOE_SPECIAL = new HashMap<>();
    static {
        int index = 1;
        ITEMS_HOE_SPECIAL.put(Items.SPONGE, index++);
        ITEMS_HOE_SPECIAL.put(Items.WET_SPONGE, index++);
    }


    public static final Map<Item, Integer> ITEMS_HOE_FARMING = new HashMap<>();
    static {
        int index = 1;
        ITEMS_HOE_FARMING.put(Items.BONE_MEAL, index++);
        ITEMS_HOE_FARMING.put(Items.BONE, index++);
        ITEMS_HOE_FARMING.put(Items.BONE_BLOCK, index++);
        ITEMS_HOE_FARMING.put(Items.WHEAT_SEEDS, index++);
        ITEMS_HOE_FARMING.put(Items.WHEAT, index++);
        ITEMS_HOE_FARMING.put(Items.HAY_BLOCK, index++);
        ITEMS_HOE_FARMING.put(Items.COCOA_BEANS, index++);
        ITEMS_HOE_FARMING.put(Items.SUGAR_CANE, index++);
        ITEMS_HOE_FARMING.put(Items.PUMPKIN_SEEDS, index++);
        ITEMS_HOE_FARMING.put(Items.PUMPKIN, index++);
        ITEMS_HOE_FARMING.put(Items.CARVED_PUMPKIN, index++);
        ITEMS_HOE_FARMING.put(Items.JACK_O_LANTERN, index++);
        ITEMS_HOE_FARMING.put(Items.BEE_NEST, index++);
        ITEMS_HOE_FARMING.put(Items.HONEYCOMB_BLOCK, index++);
        ITEMS_HOE_FARMING.put(Items.MELON_SEEDS, index++);
        ITEMS_HOE_FARMING.put(Items.MELON, index++);
        ITEMS_HOE_FARMING.put(Items.BEETROOT_SEEDS, index++);
        ITEMS_HOE_FARMING.put(Items.TORCHFLOWER_SEEDS, index++);
        ITEMS_HOE_FARMING.put(Items.PITCHER_POD, index++);
    }

    public static final Map<Item, Integer> ITEMS_HOE_MOSS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_HOE_MOSS.put(Items.MOSS_BLOCK, index++);
        ITEMS_HOE_MOSS.put(Items.MOSS_CARPET, index++);
        ITEMS_HOE_MOSS.put(Items.AZALEA, index++);
        ITEMS_HOE_MOSS.put(Items.FLOWERING_AZALEA, index++);
        ITEMS_HOE_MOSS.put(Items.GLOW_BERRIES, index++);
        ITEMS_HOE_MOSS.put(Items.PALE_MOSS_BLOCK, index++);
        ITEMS_HOE_MOSS.put(Items.PALE_MOSS_CARPET, index++);
        ITEMS_HOE_MOSS.put(Items.PALE_HANGING_MOSS, index++);
    }

    public static final Map<Item, Integer> ITEMS_HOE_SCULK = new HashMap<>();
    static {
        int index = 1;
        ITEMS_HOE_SCULK.put(Items.SCULK, index++);
        ITEMS_HOE_SCULK.put(Items.SCULK_CATALYST, index++);
        ITEMS_HOE_SCULK.put(Items.SCULK_SHRIEKER, index++);
        ITEMS_HOE_SCULK.put(Items.SCULK_SENSOR, index++);
        ITEMS_HOE_SCULK.put(Items.SCULK_VEIN, index++);
    }

    public static final Map<Item, Integer> ITEMS_HOE_NETHER_BLOCKS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_HOE_NETHER_BLOCKS.put(Items.SHROOMLIGHT, index++);
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
        ITEMS_HOE_NETHER_BLOCKS.put(Items.NETHER_SPROUTS, index++);
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
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.SMOOTH_BASALT, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.BASALT, index++);
        ITEMS_PICKAXE_NETHER_BLOCKS.put(Items.POLISHED_BASALT, index++);
    }

    public static final Map<Item, Integer> ITEMS_PICKAXE_BLACKSTONE = new HashMap<>();
    static {
        int index = 1;
        ITEMS_PICKAXE_BLACKSTONE.put(Items.BLACKSTONE, index++);
        ITEMS_PICKAXE_BLACKSTONE.put(Items.BLACKSTONE_SLAB, index++);
        ITEMS_PICKAXE_BLACKSTONE.put(Items.BLACKSTONE_STAIRS, index++);
        ITEMS_PICKAXE_BLACKSTONE.put(Items.BLACKSTONE_WALL, index++);
        ITEMS_PICKAXE_BLACKSTONE.put(Items.POLISHED_BLACKSTONE, index++);
        ITEMS_PICKAXE_BLACKSTONE.put(Items.POLISHED_BLACKSTONE_BRICKS, index++);
        ITEMS_PICKAXE_BLACKSTONE.put(Items.CRACKED_POLISHED_BLACKSTONE_BRICKS, index++);
        ITEMS_PICKAXE_BLACKSTONE.put(Items.CHISELED_POLISHED_BLACKSTONE, index++);
        ITEMS_PICKAXE_BLACKSTONE.put(Items.POLISHED_BLACKSTONE_SLAB, index++);
        ITEMS_PICKAXE_BLACKSTONE.put(Items.POLISHED_BLACKSTONE_BRICK_SLAB, index++);
        ITEMS_PICKAXE_BLACKSTONE.put(Items.POLISHED_BLACKSTONE_STAIRS, index++);
        ITEMS_PICKAXE_BLACKSTONE.put(Items.POLISHED_BLACKSTONE_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_BLACKSTONE.put(Items.POLISHED_BLACKSTONE_WALL, index++);
        ITEMS_PICKAXE_BLACKSTONE.put(Items.POLISHED_BLACKSTONE_BRICK_WALL, index++);
    }

    public static final Map<Item, Integer> ITEMS_PICKAXE_GRAYS_NATURAL = new HashMap<>();
    static {
        int index = 1;
        ITEMS_PICKAXE_GRAYS_NATURAL.put(Items.DEEPSLATE, index++);
        ITEMS_PICKAXE_GRAYS_NATURAL.put(Items.TUFF, index++);
        ITEMS_PICKAXE_GRAYS_NATURAL.put(Items.TUFF_SLAB, index++);
        ITEMS_PICKAXE_GRAYS_NATURAL.put(Items.TUFF_STAIRS, index++);
        ITEMS_PICKAXE_GRAYS_NATURAL.put(Items.TUFF_WALL, index++);
        ITEMS_PICKAXE_GRAYS_NATURAL.put(Items.STONE, index++);
        ITEMS_PICKAXE_GRAYS_NATURAL.put(Items.STONE_SLAB, index++);
        ITEMS_PICKAXE_GRAYS_NATURAL.put(Items.STONE_STAIRS, index++);
        ITEMS_PICKAXE_GRAYS_NATURAL.put(Items.ANDESITE, index++);
        ITEMS_PICKAXE_GRAYS_NATURAL.put(Items.ANDESITE_SLAB, index++);
        ITEMS_PICKAXE_GRAYS_NATURAL.put(Items.ANDESITE_STAIRS, index++);
        ITEMS_PICKAXE_GRAYS_NATURAL.put(Items.ANDESITE_WALL, index++);
    }

    public static final Map<Item, Integer> ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK = new HashMap<>();
    static {
        int index = 1;
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.CHISELED_DEEPSLATE, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.POLISHED_DEEPSLATE, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.POLISHED_DEEPSLATE_SLAB, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.POLISHED_DEEPSLATE_STAIRS, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.POLISHED_DEEPSLATE_WALL, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.DEEPSLATE_BRICKS, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.CRACKED_DEEPSLATE_BRICKS, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.DEEPSLATE_BRICK_SLAB, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.DEEPSLATE_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.DEEPSLATE_BRICK_WALL, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.DEEPSLATE_TILES, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.CRACKED_DEEPSLATE_TILES, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.DEEPSLATE_TILE_SLAB, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.DEEPSLATE_TILE_STAIRS, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.DEEPSLATE_TILE_WALL, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.CHISELED_TUFF, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.CHISELED_TUFF_BRICKS, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.POLISHED_TUFF, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.POLISHED_TUFF_SLAB, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.POLISHED_TUFF_STAIRS, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.POLISHED_TUFF_WALL, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.TUFF_BRICKS, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.TUFF_BRICK_SLAB, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.TUFF_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.TUFF_BRICK_WALL, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.MOSSY_STONE_BRICKS, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.MOSSY_STONE_BRICK_SLAB, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.MOSSY_STONE_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.MOSSY_STONE_BRICK_WALL, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(CustomBlockItems.POLISHED_STONE, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(CustomBlockItems.POLISHED_STONE_SLAB, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(CustomBlockItems.POLISHED_STONE_STAIRS, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.CHISELED_STONE_BRICKS, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.STONE_BRICKS, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.CRACKED_STONE_BRICKS, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.STONE_BRICK_SLAB, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.STONE_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.STONE_BRICK_WALL, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.POLISHED_ANDESITE, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.POLISHED_ANDESITE_SLAB, index++);
        ITEMS_PICKAXE_GRAYS_POLISHED_AND_BRICK.put(Items.POLISHED_ANDESITE_STAIRS, index++);
    }

    public static final Map<Item, Integer> ITEMS_PICKAXE_GRAYS_COBBLE = new HashMap<>();
    static {
        int index = 1;
        ITEMS_PICKAXE_GRAYS_COBBLE.put(Items.COBBLED_DEEPSLATE, index++);
        ITEMS_PICKAXE_GRAYS_COBBLE.put(Items.COBBLED_DEEPSLATE_SLAB, index++);
        ITEMS_PICKAXE_GRAYS_COBBLE.put(Items.COBBLED_DEEPSLATE_STAIRS, index++);
        ITEMS_PICKAXE_GRAYS_COBBLE.put(Items.COBBLED_DEEPSLATE_WALL, index++);
        ITEMS_PICKAXE_GRAYS_COBBLE.put(Items.MOSSY_COBBLESTONE, index++);
        ITEMS_PICKAXE_GRAYS_COBBLE.put(Items.MOSSY_COBBLESTONE_SLAB, index++);
        ITEMS_PICKAXE_GRAYS_COBBLE.put(Items.MOSSY_COBBLESTONE_STAIRS, index++);
        ITEMS_PICKAXE_GRAYS_COBBLE.put(Items.MOSSY_COBBLESTONE_WALL, index++);
        ITEMS_PICKAXE_GRAYS_COBBLE.put(Items.COBBLESTONE, index++);
        ITEMS_PICKAXE_GRAYS_COBBLE.put(Items.COBBLESTONE_SLAB, index++);
        ITEMS_PICKAXE_GRAYS_COBBLE.put(Items.COBBLESTONE_STAIRS, index++);
        ITEMS_PICKAXE_GRAYS_COBBLE.put(Items.COBBLESTONE_WALL, index++);
    }

    public static final Map<Item, Integer> ITEMS_PICKAXE_CORAL = new HashMap<>();
    static {
        int index = 1;
        ITEMS_PICKAXE_CORAL.put(Items.DEAD_TUBE_CORAL_BLOCK, index++);
        ITEMS_PICKAXE_CORAL.put(Items.DEAD_BRAIN_CORAL_BLOCK, index++);
        ITEMS_PICKAXE_CORAL.put(Items.DEAD_BUBBLE_CORAL_BLOCK, index++);
        ITEMS_PICKAXE_CORAL.put(Items.DEAD_FIRE_CORAL_BLOCK, index++);
        ITEMS_PICKAXE_CORAL.put(Items.DEAD_HORN_CORAL_BLOCK, index++);
        ITEMS_PICKAXE_CORAL.put(Items.TUBE_CORAL_BLOCK, index++);
        ITEMS_PICKAXE_CORAL.put(Items.BRAIN_CORAL_BLOCK, index++);
        ITEMS_PICKAXE_CORAL.put(Items.BUBBLE_CORAL_BLOCK, index++);
        ITEMS_PICKAXE_CORAL.put(Items.FIRE_CORAL_BLOCK, index++);
        ITEMS_PICKAXE_CORAL.put(Items.HORN_CORAL_BLOCK, index++);
    }

    public static final Map<Item, Integer> ITEMS_PICKAXE_BLUE_BLOCKS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_PICKAXE_BLUE_BLOCKS.put(Items.PRISMARINE, index++);
        ITEMS_PICKAXE_BLUE_BLOCKS.put(Items.PRISMARINE_SLAB, index++);
        ITEMS_PICKAXE_BLUE_BLOCKS.put(Items.PRISMARINE_STAIRS, index++);
        ITEMS_PICKAXE_BLUE_BLOCKS.put(Items.PRISMARINE_WALL, index++);
        ITEMS_PICKAXE_BLUE_BLOCKS.put(Items.PRISMARINE_BRICKS, index++);
        ITEMS_PICKAXE_BLUE_BLOCKS.put(Items.PRISMARINE_BRICK_SLAB, index++);
        ITEMS_PICKAXE_BLUE_BLOCKS.put(Items.PRISMARINE_BRICK_STAIRS, index++);
        ITEMS_PICKAXE_BLUE_BLOCKS.put(Items.DARK_PRISMARINE, index++);
        ITEMS_PICKAXE_BLUE_BLOCKS.put(Items.DARK_PRISMARINE_SLAB, index++);
        ITEMS_PICKAXE_BLUE_BLOCKS.put(Items.DARK_PRISMARINE_STAIRS, index++);
        ITEMS_PICKAXE_BLUE_BLOCKS.put(Items.LAPIS_BLOCK, index++);
        ITEMS_PICKAXE_BLUE_BLOCKS.put(CustomBlockItems.CUT_LAPIS, index++);
        ITEMS_PICKAXE_BLUE_BLOCKS.put(CustomBlockItems.CUT_LAPIS_SLAB, index++);
        ITEMS_PICKAXE_BLUE_BLOCKS.put(CustomBlockItems.CUT_LAPIS_STAIRS, index++);
    }

    public static final Map<Item, Integer> ITEMS_PICKAXE_ICE = new HashMap<>();
    static {
        int index = 1;
        ITEMS_PICKAXE_ICE.put(Items.BLUE_ICE, index++);
        ITEMS_PICKAXE_ICE.put(Items.PACKED_ICE, index++);
        ITEMS_PICKAXE_ICE.put(Items.ICE, index++);
    }

    public static final Map<Item, Integer> ITEMS_PICKAXE_PALE_STONES = new HashMap<>();
    static {
        int index = 1;

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
        ITEMS_PICKAXE_PALE_STONES.put(Items.CHISELED_QUARTZ_BLOCK, index++);

        ITEMS_PICKAXE_PALE_STONES.put(Items.SANDSTONE, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.SMOOTH_SANDSTONE, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.CUT_SANDSTONE, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.CHISELED_SANDSTONE, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.SANDSTONE_SLAB, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.SMOOTH_SANDSTONE_SLAB, index++);
        ITEMS_PICKAXE_PALE_STONES.put(Items.CUT_STANDSTONE_SLAB, index++);
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

        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.DYED_TERRACOTTA.orange(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.DYED_TERRACOTTA.red(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.DYED_TERRACOTTA.yellow(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.DYED_TERRACOTTA.white(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.DYED_TERRACOTTA.lightGray(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.DYED_TERRACOTTA.gray(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.DYED_TERRACOTTA.brown(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.DYED_TERRACOTTA.green(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.DYED_TERRACOTTA.lime(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.DYED_TERRACOTTA.cyan(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.DYED_TERRACOTTA.black(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.DYED_TERRACOTTA.pink(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.DYED_TERRACOTTA.magenta(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.DYED_TERRACOTTA.purple(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.DYED_TERRACOTTA.blue(), index++);

        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GLAZED_TERRACOTTA.orange(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GLAZED_TERRACOTTA.red(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GLAZED_TERRACOTTA.yellow(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GLAZED_TERRACOTTA.white(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GLAZED_TERRACOTTA.lightGray(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GLAZED_TERRACOTTA.gray(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GLAZED_TERRACOTTA.brown(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GLAZED_TERRACOTTA.green(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GLAZED_TERRACOTTA.lime(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GLAZED_TERRACOTTA.cyan(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GLAZED_TERRACOTTA.black(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GLAZED_TERRACOTTA.pink(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GLAZED_TERRACOTTA.magenta(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GLAZED_TERRACOTTA.purple(), index++);
        ITEMS_PICKAXE_TERRACOTTA_BLOCKS.put(Items.GLAZED_TERRACOTTA.blue(), index++);
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


    public static final Map<Item, Integer> ITEMS_ORE_BLOCKS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_ORE_BLOCKS.put(Items.DIAMOND_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.DEEPSLATE_DIAMOND_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.EMERALD_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.DEEPSLATE_EMERALD_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.LAPIS_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.DEEPSLATE_LAPIS_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.GOLD_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.DEEPSLATE_GOLD_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.NETHER_GOLD_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.GILDED_BLACKSTONE, index++);
        ITEMS_ORE_BLOCKS.put(Items.NETHER_QUARTZ_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.IRON_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.DEEPSLATE_IRON_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.COPPER_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.DEEPSLATE_COPPER_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.COAL_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.DEEPSLATE_COAL_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.REDSTONE_ORE, index++);
        ITEMS_ORE_BLOCKS.put(Items.DEEPSLATE_REDSTONE_ORE, index++);
    }

    public static final Map<Item, Integer> ITEMS_REFINED_MINERALS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_REFINED_MINERALS.put(Items.DIAMOND, index++);
        ITEMS_REFINED_MINERALS.put(Items.DIAMOND_BLOCK, index++);
        ITEMS_REFINED_MINERALS.put(Items.EMERALD, index++);
        ITEMS_REFINED_MINERALS.put(Items.EMERALD_BLOCK, index++);
        ITEMS_REFINED_MINERALS.put(Items.LAPIS_LAZULI, index++);
        ITEMS_REFINED_MINERALS.put(Items.LAPIS_BLOCK, index++);
        ITEMS_REFINED_MINERALS.put(CustomBlockItems.CUT_LAPIS, index++);
        ITEMS_REFINED_MINERALS.put(CustomBlockItems.CUT_LAPIS_SLAB, index++);
        ITEMS_REFINED_MINERALS.put(CustomBlockItems.CUT_LAPIS_STAIRS, index++);
        ITEMS_REFINED_MINERALS.put(Items.AMETHYST_BLOCK, index++);
        ITEMS_REFINED_MINERALS.put(Items.AMETHYST_SHARD, index++);
        ITEMS_REFINED_MINERALS.put(Items.AMETHYST_CLUSTER, index++);
    }

    public static final Map<Item, Integer> ITEMS_COMMON_MINERALS = new HashMap<>();
    static {
        int index = 1;
        ITEMS_COMMON_MINERALS.put(Items.COAL, index++);
        ITEMS_COMMON_MINERALS.put(Items.CHARCOAL, index++);
        ITEMS_COMMON_MINERALS.put(Items.COAL_BLOCK, index++);
        ITEMS_COMMON_MINERALS.put(Items.FLINT, index++);
    }

    public static final Map<Item, Integer> ITEMS_AND_BLOCKS_OF_REDSTONE = new HashMap<>();
    static {
        int index = 1;
        ITEMS_AND_BLOCKS_OF_REDSTONE.put(Items.REDSTONE, index++);
        ITEMS_AND_BLOCKS_OF_REDSTONE.put(Items.REDSTONE_BLOCK, index++);
    }

    public static final Map<Item, Integer> ITEMS_AND_BLOCKS_OF_GOLD = new HashMap<>();
    static {
        int index = 1;
        ITEMS_AND_BLOCKS_OF_GOLD.put(Items.GOLD_ORE, index++);
        ITEMS_AND_BLOCKS_OF_GOLD.put(Items.DEEPSLATE_GOLD_ORE, index++);
        ITEMS_AND_BLOCKS_OF_GOLD.put(Items.NETHER_GOLD_ORE, index++);
        ITEMS_AND_BLOCKS_OF_GOLD.put(Items.GILDED_BLACKSTONE, index++);
        ITEMS_AND_BLOCKS_OF_GOLD.put(Items.RAW_GOLD, index++);
        ITEMS_AND_BLOCKS_OF_GOLD.put(Items.RAW_GOLD_BLOCK, index++);
        ITEMS_AND_BLOCKS_OF_GOLD.put(Items.GOLD_NUGGET, index++);
        ITEMS_AND_BLOCKS_OF_GOLD.put(Items.GOLD_INGOT, index++);
        ITEMS_AND_BLOCKS_OF_GOLD.put(Items.GOLD_BLOCK, index++);
    }

    public static final Map<Item, Integer> ITEMS_AND_BLOCKS_OF_IRON = new HashMap<>();
    static {
        int index = 1;
        ITEMS_AND_BLOCKS_OF_IRON.put(Items.IRON_ORE, index++);
        ITEMS_AND_BLOCKS_OF_IRON.put(Items.DEEPSLATE_IRON_ORE, index++);
        ITEMS_AND_BLOCKS_OF_IRON.put(Items.RAW_IRON, index++);
        ITEMS_AND_BLOCKS_OF_IRON.put(Items.RAW_IRON_BLOCK, index++);
        ITEMS_AND_BLOCKS_OF_IRON.put(Items.IRON_NUGGET, index++);
        ITEMS_AND_BLOCKS_OF_IRON.put(Items.IRON_INGOT, index++);
        ITEMS_AND_BLOCKS_OF_IRON.put(Items.IRON_BLOCK, index++);

        ITEMS_AND_BLOCKS_OF_IRON.put(Items.IRON_TRAPDOOR, index++);
        ITEMS_AND_BLOCKS_OF_IRON.put(Items.IRON_BARS, index++);
        ITEMS_AND_BLOCKS_OF_IRON.put(Items.IRON_CHAIN, index++);
    }


    public static final Map<Item, Integer> ITEMS_AND_BLOCKS_OF_COPPER = new HashMap<>();
    static {
        int index = 1;
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_ORE, index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.DEEPSLATE_COPPER_ORE, index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.RAW_COPPER, index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.RAW_COPPER_BLOCK, index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_INGOT, index++);

        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_BLOCK.weathering().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_BLOCK.weathering().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_BLOCK.weathering().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_BLOCK.weathering().oxidized(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_BLOCK.waxed().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_BLOCK.waxed().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_BLOCK.waxed().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_BLOCK.waxed().oxidized(), index++);

        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER.weathering().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER.weathering().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER.weathering().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER.weathering().oxidized(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER.waxed().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER.waxed().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER.waxed().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER.waxed().oxidized(), index++);

        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER_SLAB.weathering().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER_SLAB.weathering().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER_SLAB.weathering().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER_SLAB.weathering().oxidized(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER_STAIRS.weathering().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER_STAIRS.weathering().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER_STAIRS.weathering().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER_STAIRS.weathering().oxidized(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_TRAPDOOR.weathering().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_TRAPDOOR.weathering().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_TRAPDOOR.weathering().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_TRAPDOOR.weathering().oxidized(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_DOOR.weathering().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_DOOR.weathering().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_DOOR.weathering().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_DOOR.weathering().oxidized(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_GRATE.weathering().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_GRATE.weathering().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_GRATE.weathering().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_GRATE.weathering().oxidized(), index++);

        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER_SLAB.waxed().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER_SLAB.waxed().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER_SLAB.waxed().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER_SLAB.waxed().oxidized(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER_STAIRS.waxed().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER_STAIRS.waxed().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER_STAIRS.waxed().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.CUT_COPPER_STAIRS.waxed().oxidized(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_TRAPDOOR.waxed().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_TRAPDOOR.waxed().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_TRAPDOOR.waxed().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_TRAPDOOR.waxed().oxidized(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_DOOR.waxed().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_DOOR.waxed().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_DOOR.waxed().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_DOOR.waxed().oxidized(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_GRATE.waxed().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_GRATE.waxed().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_GRATE.waxed().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_GRATE.waxed().oxidized(), index++);

        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_BARS.weathering().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_BARS.weathering().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_BARS.weathering().oxidized(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_BARS.weathering().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_BARS.waxed().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_BARS.waxed().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_BARS.waxed().oxidized(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_BARS.waxed().weathered(), index++);

        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_CHAIN.weathering().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_CHAIN.weathering().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_CHAIN.weathering().oxidized(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_CHAIN.weathering().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_CHAIN.waxed().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_CHAIN.waxed().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_CHAIN.waxed().oxidized(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.COPPER_CHAIN.waxed().weathered(), index++);

        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.LIGHTNING_ROD.weathering().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.LIGHTNING_ROD.weathering().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.LIGHTNING_ROD.weathering().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.LIGHTNING_ROD.weathering().oxidized(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.LIGHTNING_ROD.waxed().unaffected(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.LIGHTNING_ROD.waxed().exposed(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.LIGHTNING_ROD.waxed().weathered(), index++);
        ITEMS_AND_BLOCKS_OF_COPPER.put(Items.LIGHTNING_ROD.waxed().oxidized(), index++);
    }
}
