package frootloops.versus.gametest;

import frootloops.versus.mod.items_and_effects.inventory.sorting.InventorySorter;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Inventories for the sorting game tests, and the one call that sorts them.
 */
public final class TestInventories {

    private TestInventories() {
    }

    /**
     * Sorts the stacks as an inventory of {@code numRows} rows. {@code result[i]} is the index in {@code stacks} of the
     * stack laid at position {@code i} ({@code row * 9 + column}; row 0 is a player's hotbar), or -1 for an empty slot.
     * Empty when the sort gives up.
     */
    public static Optional<int[]> sortIds(List<ItemStack> stacks, int numRows, boolean playerInventory, boolean inDeepDark, boolean inNether, boolean inWater) {
        List<ItemSlot> slots = new ArrayList<>(stacks.size());
        for (int i = 0; i < stacks.size(); i++) slots.add(ItemSlot.of(i, stacks.get(i)));
        InventorySorter.Situation situation = new InventorySorter.Situation(playerInventory, inDeepDark, inNether, inWater);
        return InventorySorter.sort(slots, numRows, situation).map(sorted -> {
            int[] ids = new int[sorted.length];
            for (int i = 0; i < sorted.length; i++) ids[i] = sorted[i] == null ? -1 : sorted[i].slotId();
            return ids;
        });
    }

    /**
     * Between 1 and {@code maxStacks} stacks drawn from a fixed pool that covers every item type, repeats included.
     */
    public static List<ItemStack> random(RandomSource random, int maxStacks) {
        List<ItemStack> pool = pool();
        int numStacks = random.nextIntBetweenInclusive(1, maxStacks);
        List<ItemStack> stacks = new ArrayList<>(numStacks);
        for (int i = 0; i < numStacks; i++) stacks.add(pool.get(random.nextInt(pool.size())).copy());
        return stacks;
    }

    /** The stacks, as "count name" for failure messages. */
    public static String describe(List<ItemStack> stacks) {
        return stacks.stream().map(stack -> stack.getCount() + " " + stack.getHoverName().getString()).collect(Collectors.joining(", ", "[", "]"));
    }

    private static List<ItemStack> pool() {
        List<ItemStack> pool = new ArrayList<>();
        // Tools, weapons, armour and other unstackables
        for (Item item : List.of(Items.DIAMOND_SWORD, Items.IRON_SWORD, Items.STONE_SWORD, Items.DIAMOND_PICKAXE, Items.IRON_PICKAXE,
                Items.STONE_PICKAXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.IRON_SHOVEL, Items.WOODEN_SHOVEL,
                Items.IRON_HOE, Items.NETHERITE_HOE, Items.TRIDENT, Items.MACE, Items.BOW, Items.CROSSBOW, Items.TOTEM_OF_UNDYING,
                Items.SHIELD, Items.SHEARS, Items.FISHING_ROD, Items.SPYGLASS, Items.BRUSH, Items.FLINT_AND_STEEL, Items.ELYTRA,
                Items.IRON_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.LEATHER_BOOTS, Items.IRON_HELMET, Items.BUNDLE, Items.SHULKER_BOX,
                Items.DYED_SHULKER_BOX.red(), Items.WATER_BUCKET, Items.MILK_BUCKET, Items.COMPASS, Items.MUSIC_DISC_CAT,
                Items.ENCHANTED_GOLDEN_APPLE, Items.CRAFTING_TABLE, Items.ANVIL, Items.REDSTONE_BLOCK, Items.MINECART,
                Items.ANGLER_POTTERY_SHERD)) {
            pool.add(new ItemStack(item));
        }
        pool.add(PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS));
        pool.add(PotionContents.createItemStack(Items.POTION, Potions.LONG_FIRE_RESISTANCE));
        pool.add(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.STRONG_HEALING));
        pool.add(PotionContents.createItemStack(Items.POTION, Potions.WATER_BREATHING));
        pool.add(PotionContents.createItemStack(Items.POTION, Potions.STRENGTH));
        pool.add(PotionContents.createItemStack(Items.POTION, Potions.WATER));
        // Stackables, with the counts a player would carry
        add(pool, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2);
        add(pool, Items.BUCKET, 3);
        add(pool, Items.MAP, 5);
        add(pool, Items.LEAD, 8);
        add(pool, Items.END_CRYSTAL, 4);
        add(pool, Items.COBWEB, 12);
        add(pool, Items.SNOWBALL, 16);
        add(pool, Items.FIRE_CHARGE, 8);
        add(pool, Items.ENDER_PEARL, 16);
        add(pool, Items.WIND_CHARGE, 32);
        add(pool, Items.ARROW, 64);
        add(pool, Items.SPECTRAL_ARROW, 24);
        add(pool, Items.TORCH, 64);
        add(pool, Items.TORCH, 32);
        add(pool, Items.LANTERN, 8);
        add(pool, Items.SOUL_TORCH, 16);
        add(pool, Items.GOLDEN_APPLE, 4);
        add(pool, Items.BREAD, 16);
        add(pool, Items.BREAD, 64);
        add(pool, Items.COOKED_BEEF, 32);
        add(pool, Items.CARROT, 24);
        add(pool, Items.APPLE, 7);
        add(pool, Items.BAKED_POTATO, 40);
        add(pool, Items.COOKED_SALMON, 10);
        add(pool, Items.BLAZE_POWDER, 10);
        add(pool, Items.NETHER_WART, 30);
        add(pool, Items.SUGAR, 20);
        add(pool, Items.FERMENTED_SPIDER_EYE, 5);
        add(pool, Items.GLISTERING_MELON_SLICE, 3);
        add(pool, Items.FURNACE, 3);
        add(pool, Items.CHEST, 2);
        add(pool, Items.COBBLESTONE, 64);
        add(pool, Items.COBBLESTONE, 64);
        add(pool, Items.COBBLESTONE, 64);
        add(pool, Items.STONE, 64);
        add(pool, Items.DIRT, 64);
        add(pool, Items.DIRT, 64);
        add(pool, Items.OAK_PLANKS, 64);
        add(pool, Items.OAK_PLANKS, 23);
        add(pool, Items.OAK_LOG, 32);
        add(pool, Items.WOOL.white(), 64);
        add(pool, Items.WOOL.red(), 12);
        add(pool, Items.SAND, 64);
        add(pool, Items.GRAVEL, 40);
        add(pool, Items.DEEPSLATE, 64);
        add(pool, Items.NETHERRACK, 64);
        add(pool, Items.GRANITE, 30);
        add(pool, Items.IRON_ORE, 9);
        add(pool, Items.COAL_BLOCK, 3);
        add(pool, Items.IRON_BLOCK, 2);
        add(pool, Items.OAK_SLAB, 20);
        add(pool, Items.STONE_SLAB, 64);
        add(pool, Items.OAK_STAIRS, 16);
        add(pool, Items.COBBLESTONE_STAIRS, 8);
        add(pool, Items.COBBLESTONE_WALL, 10);
        add(pool, Items.OAK_FENCE, 12);
        add(pool, Items.OBSIDIAN, 14);
        add(pool, Items.OAK_DOOR, 3);
        add(pool, Items.RAIL, 32);
        add(pool, Items.POWERED_RAIL, 8);
        add(pool, Items.OAK_TRAPDOOR, 4);
        add(pool, Items.LADDER, 20);
        add(pool, Items.OAK_LEAVES, 30);
        add(pool, Items.MOSS_BLOCK, 16);
        add(pool, Items.SCULK, 12);
        add(pool, Items.REPEATER, 4);
        add(pool, Items.BRICK, 30);
        add(pool, Items.FLOWER_POT, 2);
        add(pool, Items.DISC_FRAGMENT_5, 2);
        add(pool, Items.DIAMOND, 8);
        add(pool, Items.IRON_INGOT, 27);
        add(pool, Items.COAL, 40);
        add(pool, Items.STRING, 13);
        add(pool, Items.FEATHER, 9);
        add(pool, Items.GUNPOWDER, 22);
        add(pool, Items.BONE, 17);
        add(pool, Items.PAPER, 30);
        add(pool, Items.BOOK, 5);
        add(pool, Items.EMERALD, 14);
        add(pool, Items.GOLD_INGOT, 6);
        add(pool, Items.RAW_IRON, 33);
        add(pool, Items.RAW_GOLD, 7);
        add(pool, Items.COPPER_INGOT, 20);
        add(pool, Items.LEATHER, 11);
        add(pool, Items.SLIME_BALL, 4);
        add(pool, Items.REDSTONE, 50);
        add(pool, Items.LAPIS_LAZULI, 18);
        add(pool, Items.QUARTZ, 25);
        add(pool, Items.DANDELION, 5);
        add(pool, Items.POPPY, 3);
        add(pool, Items.OAK_SAPLING, 9);
        add(pool, Items.SHORT_GRASS, 12);
        add(pool, Items.WHEAT_SEEDS, 30);
        add(pool, Items.ROTTEN_FLESH, 41);
        add(pool, Items.SPIDER_EYE, 6);
        add(pool, Items.POISONOUS_POTATO, 2);
        add(pool, Items.INK_SAC, 4);
        return pool;
    }

    private static void add(List<ItemStack> pool, Item item, int count) {
        pool.add(new ItemStack(item, count));
    }
}
