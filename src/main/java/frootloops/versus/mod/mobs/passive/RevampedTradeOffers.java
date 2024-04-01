package frootloops.versus.mod.mobs.passive;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import frootloops.versus.mod.enchantments.Enchants;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.SuspiciousStewEffectsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.item.map.MapState;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.*;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class RevampedTradeOffers {

    private static final SellMapFactory SELL_DESERT_VILLAGE_MAP_TRADE = new SellMapFactory(8, StructureTags.ON_DESERT_VILLAGE_MAPS, "filled_map.village_desert", MapDecorationTypes.VILLAGE_DESERT, 12, 5);
    private static final SellMapFactory SELL_SAVANNA_VILLAGE_MAP_TRADE = new SellMapFactory(8, StructureTags.ON_SAVANNA_VILLAGE_MAPS, "filled_map.village_savanna", MapDecorationTypes.VILLAGE_SAVANNA, 12, 5);
    private static final SellMapFactory SELL_PLAINS_VILLAGE_MAP_TRADE = new SellMapFactory(8, StructureTags.ON_PLAINS_VILLAGE_MAPS, "filled_map.village_plains", MapDecorationTypes.VILLAGE_PLAINS, 12, 5);
    private static final SellMapFactory SELL_TAIGA_VILLAGE_MAP_TRADE = new SellMapFactory(8, StructureTags.ON_TAIGA_VILLAGE_MAPS, "filled_map.village_taiga", MapDecorationTypes.VILLAGE_TAIGA, 12, 5);
    private static final SellMapFactory SELL_SNOWY_VILLAGE_MAP_TRADE = new SellMapFactory(8, StructureTags.ON_SNOWY_VILLAGE_MAPS, "filled_map.village_snowy", MapDecorationTypes.VILLAGE_SNOWY, 12, 5);
    private static final SellMapFactory SELL_JUNGLE_TEMPLE_MAP_TRADE = new SellMapFactory(8, StructureTags.ON_JUNGLE_EXPLORER_MAPS, "filled_map.explorer_jungle", MapDecorationTypes.JUNGLE_TEMPLE, 12, 5);
    private static final SellMapFactory SELL_SWAMP_HUT_MAP_TRADE = new SellMapFactory(8, StructureTags.ON_SWAMP_EXPLORER_MAPS, "filled_map.explorer_swamp", MapDecorationTypes.SWAMP_HUT, 12, 5);


    public static final Map<VillagerProfession, Int2ObjectMap<Factory[]>> REVAMPED_PROFESSION_TO_LEVELED_TRADE = Util.make(Maps.newHashMap(), map -> {
        map.put(VillagerProfession.FARMER, copyToFastUtilMap(
                ImmutableMap.of(
                        1, new Factory[]{
                                new SellItemFactory(Items.BONE_MEAL, 1, 12, 1),
                                new SellItemFactory(Items.WHEAT, 1, 24, 2),
                                new SellItemFactory(Items.POTATO, 1, 15, 2),
                                new SellItemFactory(Items.BEETROOT, 1, 16, 2),
                                new TypeAwareSellItemFactory(1, 8, 32, 2, ImmutableMap.builder().put(
                                        VillagerType.PLAINS, Items.DARK_OAK_SAPLING).put(
                                        VillagerType.TAIGA, Items.SPRUCE_SAPLING).put(
                                        VillagerType.SNOW, Items.SPRUCE_SAPLING).put(
                                        VillagerType.DESERT, Items.JUNGLE_SAPLING).put(
                                        VillagerType.JUNGLE, Items.JUNGLE_SAPLING).put(
                                        VillagerType.SAVANNA, Items.ACACIA_SAPLING).put(
                                        VillagerType.SWAMP, Items.MANGROVE_PROPAGULE).build()),
                                new TypeAwareSellItemFactory(1, 14, 32, 2, ImmutableMap.builder().put(
                                        VillagerType.PLAINS, Items.SUNFLOWER).put(
                                        VillagerType.TAIGA, Items.SWEET_BERRIES).put(
                                        VillagerType.SNOW, Items.SNOWBALL).put(
                                        VillagerType.DESERT, Items.CACTUS).put(
                                        VillagerType.JUNGLE, Items.MELON_SLICE).put(
                                        VillagerType.SAVANNA, Items.CLAY_BALL).put(
                                        VillagerType.SWAMP, Items.MUD).build())},
                        2, new Factory[]{
                                new BuyForMutlipleEmeraldsFactory(Items.BEE_NEST, 8, 12, 10),
                                new SellItemFactory(Items.HONEY_BOTTLE, 1, 8, 4),
                                new SellItemFactory(Items.MOSS_BLOCK, 1, 12, 3),
                                new TypeAwareSellItemFactory(1, 15, 32, 5, ImmutableMap.builder().put(
                                        VillagerType.PLAINS, Items.CARROT).put(
                                        VillagerType.TAIGA, Items.CARROT).put(
                                        VillagerType.SNOW, Items.SWEET_BERRIES).put(
                                        VillagerType.DESERT, Items.FLOWERING_AZALEA_LEAVES).put(
                                        VillagerType.JUNGLE, Items.COCOA_BEANS).put(
                                        VillagerType.SAVANNA, Items.SUGAR_CANE).put(
                                        VillagerType.SWAMP, Items.SUGAR_CANE).build()),
                                new TypeAwareSellItemFactory(1, 21, 32, 5, ImmutableMap.builder().put(
                                        VillagerType.PLAINS, Items.APPLE).put(
                                        VillagerType.TAIGA, Items.PODZOL).put(
                                        VillagerType.SNOW, Items.PACKED_ICE).put(
                                        VillagerType.DESERT, Items.FLOWERING_AZALEA).put(
                                        VillagerType.JUNGLE, Items.BAMBOO).put(
                                        VillagerType.SAVANNA, Items.ACACIA_LEAVES).put(
                                        VillagerType.SWAMP, Items.PACKED_MUD).build())},
                        3, new Factory[]{
                                new BuyForOneEmeraldFactory(Blocks.PUMPKIN, 18, 12, 15),
                                new SellItemFactory(Items.BROWN_MUSHROOM, 1, 16, 10),
                                new SellItemFactory(Items.RED_MUSHROOM, 1, 12, 10),
                                new SellItemFactory(Items.HONEY_BLOCK, 1, 3, 10),
                                new SellItemFactory(Items.BEEHIVE, 1, 1, 16, 15)},
                        4, new Factory[]{
                                new SellItemFactory(Blocks.ALLIUM, 1, 4, 16, 15),
                                new SellItemFactory(Blocks.CORNFLOWER, 1, 4, 16, 15),
                                new SellItemFactory(Blocks.ORANGE_TULIP, 1, 4, 16, 15),
                                new SellItemFactory(Blocks.RED_TULIP, 1, 4, 16, 15),
                                new SellItemFactory(Blocks.PINK_TULIP, 1, 4, 16, 15),
                                new SellItemFactory(Blocks.WHITE_TULIP, 1, 4, 16, 15),
                                new SellSuspiciousStewFactory(StatusEffects.NIGHT_VISION, 100, 15),
                                new SellSuspiciousStewFactory(StatusEffects.SPEED, 720, 15),
                                new SellSuspiciousStewFactory(StatusEffects.JUMP_BOOST, 160, 15),
                                new SellSuspiciousStewFactory(StatusEffects.WEAKNESS, 140, 15),
                                new SellSuspiciousStewFactory(StatusEffects.HUNGER, 1200, 15),
                                new SellSuspiciousStewFactory(StatusEffects.POISON, 300, 15),
                                new SellSuspiciousStewFactory(StatusEffects.BLINDNESS, 120, 15),
                                new SellSuspiciousStewFactory(StatusEffects.HASTE, 1200, 15),
                                new SellSuspiciousStewFactory(StatusEffects.REGENERATION, 100, 15),
                                new SellSuspiciousStewFactory(StatusEffects.RESISTANCE, 100, 15),
                                new SellSuspiciousStewFactory(StatusEffects.ABSORPTION, 300, 15),
                                new SellSuspiciousStewFactory(StatusEffects.INSTANT_HEALTH, 1, 15),
                                new SellSuspiciousStewFactory(StatusEffects.SATURATION, 7, 15)},
                        5, new Factory[]{
                                new SellItemFactory(Items.COOKIE, 1, 16, 20),
                                new SellItemFactory(Items.PUMPKIN_PIE, 1, 8, 20)}
                )));

        map.put(VillagerProfession.FISHERMAN, copyToFastUtilMap(
                ImmutableMap.of(
                        1, new Factory[]{
                                new BuyForOneEmeraldFactory(Items.COD, 8, 8, 1),
                                new SellItemFactory(Items.LILY_PAD, 1, 1, 4),
                                new SellItemFactory(Items.INK_SAC, 1, 3, 2),
                                new SellItemFactory(Items.CLAY_BALL, 1, 32, 2),
                                new SellItemFactory(Items.SAND, 1, 10, 3)},
                        2, new Factory[]{
                                new BuyForOneEmeraldFactory(Items.SALMON, 6, 16, 8),
                                new SellItemFactory(Items.SEAGRASS, 1, 26, 5),
                                new SellItemFactory(PotionContentsComponent.createStack(Items.POTION, Potions.WATER_BREATHING), 5, 1, 3, 10),
                                new SellItemFactory(Items.SEA_PICKLE, 1, 4, 8),
                                new BuyForMutlipleEmeraldsFactory(Items.NAUTILUS_SHELL, 18, 16, 50),
                                new BuyForMutlipleEmeraldsFactory(Items.AXOLOTL_BUCKET, 16, 16, 50),
                                new BuyForMutlipleEmeraldsFactory(Items.PUFFERFISH_BUCKET, 12, 16, 30)},
                        3, new Factory[]{
                                new SellItemFactory(Items.BRAIN_CORAL_BLOCK, 1, 1, 15),
                                new SellItemFactory(Items.BUBBLE_CORAL_BLOCK, 1, 1, 15),
                                new SellItemFactory(Items.FIRE_CORAL_BLOCK, 1, 1, 15),
                                new SellItemFactory(Items.HORN_CORAL_BLOCK, 1, 1, 15),
                                new SellItemFactory(Items.TUBE_CORAL_BLOCK, 1, 1, 15),
                                new BuyForOneEmeraldFactory(Items.TROPICAL_FISH, 4, 16, 15)},
                        4, new Factory[]{
                                new SellItemFactory(Items.TURTLE_SCUTE, 16, 1, 15),
                                new SellItemFactory(Items.PRISMARINE_SHARD, 1, 2, 15),
                                new SellItemFactory(Items.PRISMARINE_CRYSTALS, 1, 3, 15),
                                new BuyForMutlipleEmeraldsFactory(Items.PUFFERFISH, 3, 16, 15)},
                        5, new Factory[]{
                                new SellEnchantedToolFactory(Items.FISHING_ROD, 4, 1, 30, 0.2f)}
                )));

        map.put(VillagerProfession.SHEPHERD, copyToFastUtilMap(
                ImmutableMap.of(
                        1, new Factory[]{
                                new SellItemFactory(Blocks.WHITE_WOOL, 1, 12, 32, 2),
                                new SellItemFactory(Blocks.BROWN_WOOL, 1, 12, 32, 2),
                                new SellItemFactory(Blocks.BLACK_WOOL, 1, 12, 32, 2),
                                new SellItemFactory(Blocks.GRAY_WOOL, 1, 12, 32, 2),
                                new SellItemFactory(Blocks.LIGHT_GRAY_WOOL, 1, 12, 32, 2),
                                new SellItemFactory(Items.SHEARS, 1, 1, 1)},
                        2, new Factory[]{
                                new SellItemFactory(Items.PAINTING, 1, 8, 16, 8),
                                new SellItemFactory(Items.STRING, 1, 10, 16, 4),
                                new SellItemFactory(Items.LEAD, 1, 1, 6),
                                new BuyForMutlipleEmeraldsFactory(Items.GOAT_HORN, 12, 16, 15)},
                        3, new Factory[]{
                                new SellItemFactory(Items.POPPY, 1, 4, 5),
                                new SellItemFactory(Items.DANDELION, 1, 4, 5),
                                new SellItemFactory(Items.AZURE_BLUET, 1, 4, 5),
                                new SellItemFactory(Items.OXEYE_DAISY, 1, 4, 5),
                                new SellItemFactory(Blocks.ORANGE_WOOL, 1, 10, 16, 5),
                                new SellItemFactory(Blocks.MAGENTA_WOOL, 1, 10, 16, 5),
                                new SellItemFactory(Blocks.LIGHT_BLUE_WOOL, 1, 10, 16, 5),
                                new SellItemFactory(Blocks.YELLOW_WOOL, 1, 10, 16, 5),
                                new SellItemFactory(Blocks.LIME_WOOL, 1, 10, 16, 5),
                                new SellItemFactory(Blocks.PINK_WOOL, 1, 10, 16, 5),
                                new SellItemFactory(Blocks.CYAN_WOOL, 1, 10, 16, 5),
                                new SellItemFactory(Blocks.PURPLE_WOOL, 1, 10, 16, 5),
                                new SellItemFactory(Blocks.BLUE_WOOL, 1, 10, 16, 5),
                                new SellItemFactory(Blocks.GREEN_WOOL, 1, 10, 16, 5),
                                new SellItemFactory(Blocks.RED_WOOL, 1, 10, 16, 5)},
                        4, new Factory[]{
                                new SellItemFactory(Items.RED_TULIP, 1, 4, 10),
                                new SellItemFactory(Items.ORANGE_TULIP, 1, 4, 10),
                                new SellItemFactory(Items.WHITE_TULIP, 1, 4, 10),
                                new SellItemFactory(Items.PINK_TULIP, 1, 4, 10),
                                new SellItemFactory(Blocks.WHITE_CARPET, 1, 36, 16, 10),
                                new SellItemFactory(Blocks.ORANGE_CARPET, 1, 36, 16, 10),
                                new SellItemFactory(Blocks.MAGENTA_CARPET, 1, 36, 16, 10),
                                new SellItemFactory(Blocks.LIGHT_BLUE_CARPET, 1, 36, 16, 10),
                                new SellItemFactory(Blocks.YELLOW_CARPET, 1, 36, 16, 10),
                                new SellItemFactory(Blocks.LIME_CARPET, 1, 36, 16, 10),
                                new SellItemFactory(Blocks.PINK_CARPET, 1, 36, 16, 10),
                                new SellItemFactory(Blocks.GRAY_CARPET, 1, 36, 16, 10),
                                new SellItemFactory(Blocks.LIGHT_GRAY_CARPET, 1, 36, 16, 10),
                                new SellItemFactory(Blocks.CYAN_CARPET, 1, 36, 16, 10),
                                new SellItemFactory(Blocks.PURPLE_CARPET, 1, 36, 16, 10),
                                new SellItemFactory(Blocks.BLUE_CARPET, 1, 36, 16, 10),
                                new SellItemFactory(Blocks.BROWN_CARPET, 1, 36, 16, 10),
                                new SellItemFactory(Blocks.GREEN_CARPET, 1, 36, 16, 10),
                                new SellItemFactory(Blocks.RED_CARPET, 1, 36, 16, 10),
                                new SellItemFactory(Blocks.BLACK_CARPET, 1, 36, 16, 10)},
                        5, new Factory[]{
                                new SellItemFactory(Items.ALLIUM, 1, 4, 10),
                                new SellItemFactory(Items.CORNFLOWER, 1, 4, 15),
                                new SellItemFactory(Items.LILY_OF_THE_VALLEY, 1, 4, 15),
                                new SellItemFactory(Items.ROSE_BUSH, 1, 4, 15),
                                new SellItemFactory(Items.PEONY, 1, 4, 15),
                                new SellItemFactory(Items.LILAC, 1, 4, 15),
                                new SellItemFactory(Items.SUNFLOWER, 1, 4, 15),
                                new SellItemFactory(Items.WHITE_BANNER, 1, 4, 12, 15),
                                new SellItemFactory(Items.BLUE_BANNER, 1, 4, 12, 15),
                                new SellItemFactory(Items.LIGHT_BLUE_BANNER, 4, 1, 12, 15),
                                new SellItemFactory(Items.RED_BANNER, 1, 4, 12, 15),
                                new SellItemFactory(Items.PINK_BANNER, 1, 4, 12, 15),
                                new SellItemFactory(Items.GREEN_BANNER, 1, 4, 12, 15),
                                new SellItemFactory(Items.LIME_BANNER, 1, 4, 12, 15),
                                new SellItemFactory(Items.GRAY_BANNER, 1, 4, 12, 15),
                                new SellItemFactory(Items.BLACK_BANNER, 1, 4, 12, 15),
                                new SellItemFactory(Items.PURPLE_BANNER, 1, 4, 12, 15),
                                new SellItemFactory(Items.MAGENTA_BANNER, 1, 4, 12, 15),
                                new SellItemFactory(Items.CYAN_BANNER, 1, 4, 12, 15),
                                new SellItemFactory(Items.BROWN_BANNER, 1, 4, 12, 15),
                                new SellItemFactory(Items.YELLOW_BANNER, 1, 4, 12, 15),
                                new SellItemFactory(Items.ORANGE_BANNER, 1, 4, 12, 15),
                                new SellItemFactory(Items.LIGHT_GRAY_BANNER, 1, 4, 12, 15)}
                )));

        map.put(VillagerProfession.FLETCHER, copyToFastUtilMap(
                ImmutableMap.of(
                        1, new Factory[]{
                                new SellItemFactory(Items.ARROW, 1, 16, 2),
                                new SellItemFactory(Items.FLINT, 1, 10, 2),
                                new SellItemFactory(Items.FEATHER, 1, 6, 3),
                                new SellItemFactory(Items.GRAVEL, 1, 17, 2),
                                new TypeAwareSellItemFactory(1, 6, 32, 2, ImmutableMap.builder().put(
                                        VillagerType.PLAINS, Items.OAK_WOOD).put(
                                        VillagerType.TAIGA, Items.SPRUCE_WOOD).put(
                                        VillagerType.SNOW, Items.SPRUCE_WOOD).put(
                                        VillagerType.DESERT, Items.JUNGLE_WOOD).put(
                                        VillagerType.JUNGLE, Items.JUNGLE_WOOD).put(
                                        VillagerType.SAVANNA, Items.ACACIA_WOOD).put(
                                        VillagerType.SWAMP, Items.MANGROVE_WOOD).build())},
                        2, new Factory[]{
                                new SellItemFactory(Items.FIRE_CHARGE, 1, 4, 8),
                                new SellItemFactory(Items.STRING, 1, 6, 16, 4),
                                new SellItemFactory(Items.GUNPOWDER, 1, 3, 5),
                                new BuyForMutlipleEmeraldsFactory(Items.CROSSBOW, 16, 16, 10),
                                new BuyForMutlipleEmeraldsFactory(Items.BOW, 16, 16, 10)},
                        3, new Factory[]{
                                new SellPotionHoldingItemFactory(Items.ARROW, 4, Items.TIPPED_ARROW, 4, 2, 12, 8),
                                new SellItemFactory(Items.TARGET, 1, 1, 10),
                                new SellItemFactory(Items.DISPENSER, 2, 1, 10),
                                new SellItemFactory(Items.DROPPER, 1, 1, 10)},
                        4, new Factory[]{
                                new SellPotionHoldingItemFactory(Items.GLASS_BOTTLE, 1, Items.SPLASH_POTION, 1, 24, 3, 18),
                                new SellPotionHoldingItemFactory(Items.ARROW, 4, Items.TIPPED_ARROW, 4, 1, 12, 18)},
                        5, new Factory[]{
                                new SellItemFactory(Items.TNT, 1, 5, 20)}
                )));

        map.put(VillagerProfession.LIBRARIAN, copyToFastUtilMap(
                ImmutableMap.of(
                        1, new Factory[]{
                                new BuyForMutlipleEmeraldsFactory(Items.LAPIS_LAZULI, 4, 12, 8),
                                new BuyForOneEmeraldFactory(Items.AMETHYST_SHARD, 1, 12, 5),
                                new SellItemFactory(Items.BOOK, 1, 3, 3),
                                new SellItemFactory(Items.LANTERN, 1, 3, 4),
                                new SellItemFactory(Items.CLOCK, 1, 1, 4),
                                new SellItemFactory(Items.GLASS, 1, 6, 3),
                                new SellItemFactory(Blocks.BOOKSHELF, 3, 1, 12, 6),
                                new EnchantBookFactory(10),
                                new TypeAwareSellItemFactory(24, 1, 3, 16, ImmutableMap.builder().put(
                                        VillagerType.PLAINS, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.BLAST_PROTECTION, 1))).put(
                                        VillagerType.TAIGA, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchants.TOSSING, 2))).put(
                                        VillagerType.SNOW, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SILK_TOUCH, 1))).put(
                                        VillagerType.DESERT, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.FIRE_PROTECTION, 1))).put(
                                        VillagerType.JUNGLE, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.POWER, 2))).put(
                                        VillagerType.SAVANNA, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.KNOCKBACK, 2))).put(
                                        VillagerType.SWAMP, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.LURE, 3))).build()),
                                new TypeAwareSellItemFactory(18, 1, 3, 14, ImmutableMap.builder().put(
                                        VillagerType.PLAINS, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SMITE, 2))).put(
                                        VillagerType.TAIGA, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SWEEPING_EDGE, 2))).put(
                                        VillagerType.SNOW, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchants.FROST_ASPECT, 1))).put(
                                        VillagerType.DESERT, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.FLAME, 1))).put(
                                        VillagerType.JUNGLE, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.PIERCING, 3))).put(
                                        VillagerType.SAVANNA, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.KNOCKBACK, 2))).put(
                                        VillagerType.SWAMP, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.RESPIRATION, 1))).build())},
                        2, new Factory[]{
                                new TypeAwareSellItemFactory(27, 1, 3, 18, ImmutableMap.builder().put(
                                        VillagerType.PLAINS, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchants.RIPOSTE, 3))).put(
                                        VillagerType.TAIGA, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.BINDING_CURSE, 1))).put(
                                        VillagerType.SNOW, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.FROST_WALKER, 2))).put(
                                        VillagerType.DESERT, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.FIRE_ASPECT, 2))).put(
                                        VillagerType.JUNGLE, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.QUICK_CHARGE, 2))).put(
                                        VillagerType.SAVANNA, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchants.BOUNDING_STRIDES, 1))).put(
                                        VillagerType.SWAMP, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.RIPTIDE, 2))).build()),
                                new BuyForMutlipleEmeraldsFactory(Items.MUSIC_DISC_11, 8, 1, 20),
                                new BuyForMutlipleEmeraldsFactory(Items.EXPERIENCE_BOTTLE, 3, 12, 15),
                                new BuyForOneEmeraldFactory(Items.SOUL_SAND, 8, 12, 10),
                                new BuyForOneEmeraldFactory(Items.SOUL_SOIL, 8, 12, 10),
                                new BuyForOneEmeraldFactory(Items.GLOW_INK_SAC, 5, 12, 10),
                                new SellItemFactory(Items.WRITABLE_BOOK, 3, 1, 15),
                                new SellItemFactory(Items.CANDLE, 1, 8, 5),
                                new SellItemFactory(Items.BLACK_CANDLE, 1, 8, 5),
                                new SellItemFactory(Items.GRAY_CANDLE, 1, 8, 5)},
                        3, new Factory[]{
                                new TypeAwareSellItemFactory(32, 1, 3, 20, ImmutableMap.builder().put(
                                        VillagerType.PLAINS, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchants.CLEAVING, 2))).put(
                                        VillagerType.TAIGA, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.PUNCH, 2))).put(
                                        VillagerType.SNOW, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.CHANNELING, 1))).put(
                                        VillagerType.DESERT, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.INFINITY, 1))).put(
                                        VillagerType.JUNGLE, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.MULTISHOT, 1))).put(
                                        VillagerType.SAVANNA, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.BINDING_CURSE, 1))).put(
                                        VillagerType.SWAMP, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.DEPTH_STRIDER, 2))).build()),
                                new EnchantBookFactory(15),
                                new EnchantBookFactory(15),
                                new BuyForMutlipleEmeraldsFactory(Items.SCULK_CATALYST, 6, 16, 20),
                                new SellItemFactory(Items.NAME_TAG, 20, 1, 15),
                                new SellItemFactory(Items.TINTED_GLASS, 1, 2, 10)},
                        4, new Factory[]{
                                new EnchantBookFactory(20),
                                new EnchantBookFactory(20),
                                new EnchantBookFactory(20),
                                new EnchantBookFactory(20),
                                new BuyForMutlipleEmeraldsFactory(Items.MUSIC_DISC_5, 52, 1, 50),
                                new BuyForMutlipleEmeraldsFactory(Items.ENDER_EYE, 4, 12, 20),
                                new BuyForOneEmeraldFactory(Items.ENDER_PEARL, 1, 12, 15)},
                        5, new Factory[]{
                                new EnchantBookFactory(30),
                                new TypeAwareSellItemFactory(48, 1, 3, 20, ImmutableMap.builder().put(
                                        VillagerType.PLAINS, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchants.IMPACT_PROTECTION, 2))).put(
                                        VillagerType.TAIGA, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.THORNS, 3))).put(
                                        VillagerType.SNOW, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.UNBREAKING, 3))).put(
                                        VillagerType.DESERT, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.EFFICIENCY, 4))).put(
                                        VillagerType.JUNGLE, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.FORTUNE, 2))).put(
                                        VillagerType.SAVANNA, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 3))).put(
                                        VillagerType.SWAMP, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.MENDING, 1))).build()),
                                new TypeAwareSellItemFactory(48, 1, 3, 20, ImmutableMap.builder().put(
                                        VillagerType.PLAINS, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchants.PIERCING_PROTECTION, 2))).put(
                                        VillagerType.TAIGA, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.WIND_BURST, 3))).put(
                                        VillagerType.SNOW, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.RIPTIDE, 3))).put(
                                        VillagerType.DESERT, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SWEEPING_EDGE, 4))).put(
                                        VillagerType.JUNGLE, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.DENSITY, 3))).put(
                                        VillagerType.SAVANNA, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.PROJECTILE_PROTECTION, 2))).put(
                                        VillagerType.SWAMP, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.VANISHING_CURSE, 1))).build())
                        }
                )));

        map.put(VillagerProfession.CARTOGRAPHER, copyToFastUtilMap(
                ImmutableMap.of(
                        1, new Factory[]{
                                new BuyForMutlipleEmeraldsFactory(Items.OMINOUS_BOTTLE, 24, 16, 15),
                                new BuyForOneEmeraldFactory(Items.REDSTONE, 16, 8, 1),
                                new SellItemFactory(Items.SPYGLASS, 2, 1, 8),
                                new SellItemFactory(Items.COMPASS, 1, 1, 3),
                                new SellItemFactory(Items.PAPER, 1, 12, 3),
                                new SellItemFactory(Items.MAP, 1, 1, 4),
                                new SellItemFactory(Items.CAMPFIRE, 1, 1, 10),
                                new TypedWrapperFactory(ImmutableMap.of(
                                        VillagerType.DESERT, SELL_SAVANNA_VILLAGE_MAP_TRADE,
                                        VillagerType.SAVANNA, SELL_PLAINS_VILLAGE_MAP_TRADE,
                                        VillagerType.PLAINS, SELL_TAIGA_VILLAGE_MAP_TRADE,
                                        VillagerType.TAIGA, SELL_SNOWY_VILLAGE_MAP_TRADE,
                                        VillagerType.SNOW, SELL_PLAINS_VILLAGE_MAP_TRADE,
                                        VillagerType.JUNGLE, SELL_SAVANNA_VILLAGE_MAP_TRADE,
                                        VillagerType.SWAMP, SELL_SNOWY_VILLAGE_MAP_TRADE)),
                                new TypedWrapperFactory(ImmutableMap.of(
                                        VillagerType.DESERT, SELL_PLAINS_VILLAGE_MAP_TRADE,
                                        VillagerType.SAVANNA, SELL_DESERT_VILLAGE_MAP_TRADE,
                                        VillagerType.PLAINS, SELL_SAVANNA_VILLAGE_MAP_TRADE,
                                        VillagerType.TAIGA, SELL_PLAINS_VILLAGE_MAP_TRADE,
                                        VillagerType.SNOW, SELL_TAIGA_VILLAGE_MAP_TRADE,
                                        VillagerType.JUNGLE, SELL_DESERT_VILLAGE_MAP_TRADE,
                                        VillagerType.SWAMP, SELL_TAIGA_VILLAGE_MAP_TRADE)),
                                new TypedWrapperFactory(ImmutableMap.of(
                                        VillagerType.DESERT, SELL_JUNGLE_TEMPLE_MAP_TRADE,
                                        VillagerType.SAVANNA, SELL_JUNGLE_TEMPLE_MAP_TRADE,
                                        VillagerType.PLAINS, SELL_JUNGLE_TEMPLE_MAP_TRADE,
                                        VillagerType.TAIGA, SELL_SWAMP_HUT_MAP_TRADE,
                                        VillagerType.SNOW, SELL_SWAMP_HUT_MAP_TRADE,
                                        VillagerType.JUNGLE, SELL_SWAMP_HUT_MAP_TRADE,
                                        VillagerType.SWAMP, SELL_JUNGLE_TEMPLE_MAP_TRADE))
                        },
                        2, new Factory[]{
                                new SellItemFactory(Items.FLOWER_BANNER_PATTERN, 18, 1, 15),
                                new SellItemFactory(Items.GLASS_PANE, 1, 16, 8),
                                new SellMapFactory(10,
                                        StructureTags.ON_TRIAL_CHAMBERS_MAPS, "filled_map.trial_chambers",
                                        MapDecorationTypes.TRIAL_CHAMBERS, 1, 12),
                                new SellMapFactory(
                                        8,
                                        StructureTags.SHIPWRECK, "filled_map.custom.shipwreck",
                                        MapDecorationTypes.TARGET_X, 3, 18),
                                new SellMapFactory(
                                        6,
                                        StructureTags.OCEAN_RUIN, "Ruins Explorer Map",
                                        MapDecorationTypes.TARGET_X, 3, 14),
                                new SellMapFactory(
                                        6,
                                        StructureTags.RUINED_PORTAL, "Ruins Explorer Map",
                                        MapDecorationTypes.TARGET_X, 3, 14)},
                        3, new Factory[]{
                                new SellItemFactory(Items.SKULL_BANNER_PATTERN, 21, 1, 15),
                                new SellItemFactory(Items.MUSIC_DISC_WAIT, 32, 1, 15),
                                new SellMapFactory(
                                        13,
                                        StructureTags.ON_OCEAN_EXPLORER_MAPS, "filled_map.monument",
                                        MapDecorationTypes.MONUMENT, 3, 60)},
                        4, new Factory[]{
                                new SellItemFactory(Items.CREEPER_BANNER_PATTERN, 24, 1, 15),
                                new SellItemFactory(Items.MUSIC_DISC_FAR, 32, 1, 15),
                                new SellMapFactory(
                                        14,
                                        StructureTags.ON_WOODLAND_EXPLORER_MAPS, "filled_map.mansion",
                                        MapDecorationTypes.MANSION, 3, 100)},
                        5, new Factory[]{
                                new SellItemFactory(Items.GLOBE_BANNER_PATTERN, 8, 1, 30)}
                )));

        map.put(VillagerProfession.CLERIC, copyToFastUtilMap(
                ImmutableMap.of(
                        1, new Factory[]{
                                new BuyForMutlipleEmeraldsFactory(Items.GOLDEN_APPLE, 5, 16, 5),
                                new BuyForOneEmeraldFactory(Items.SPIDER_EYE, 8, 12, 6),
                                new BuyForOneEmeraldFactory(Items.ROTTEN_FLESH, 8, 12, 8),
                                new BuyForOneEmeraldFactory(Items.GOLD_INGOT, 3, 24, 10),
                                new SellItemFactory(Items.GLASS_BOTTLE, 1, 12, 2),
                                new SellItemFactory(Items.SUGAR, 1, 24, 1),
                                new SellItemFactory(Items.REDSTONE, 1, 8, 16, 2),
                                new SellItemFactory(Items.GUNPOWDER, 1, 3, 2)},
                        2, new Factory[]{
                                new BuyForMutlipleEmeraldsFactory(Items.BREEZE_ROD, 3, 16, 5),
                                new BuyForOneEmeraldFactory(Items.GLOWSTONE_DUST, 16, 12, 5),
                                new BuyForOneEmeraldFactory(Items.BLAZE_POWDER, 1, 16, 5),
                                new BuyForOneEmeraldFactory(Blocks.NETHER_WART, 21, 16, 5),
                                new SellItemFactory(Items.RABBIT_HIDE, 1, 1, 12, 5),
                                new SellItemFactory(Items.FERMENTED_SPIDER_EYE, 1, 1, 12, 5)},
                        3, new Factory[]{
                                new SellItemFactory(createPotionStack(Potions.WATER_BREATHING), 3, 1, 6, 10),
                                new SellItemFactory(createSplashPotionStack(Potions.WEAKNESS), 15, 1, 6,10),
                                new SellItemFactory(createPotionStack(Potions.HEALING), 4, 1, 6,10),
                                new SellItemFactory(createPotionStack(Potions.SWIFTNESS), 5, 1, 6,10),
                                new SellItemFactory(Items.MAGMA_CREAM, 1, 1, 12, 3),
                                new ProcessItemFactory(Items.CARROT, 16, Items.GOLDEN_CARROT, 16, 12, 3, 1)},
                        4, new Factory[]{
                                new SellItemFactory(createSplashPotionStack(Potions.OOZING), 12, 1, 6,11),
                                new SellItemFactory(createSplashPotionStack(Potions.WIND_CHARGED), 14, 1, 6,11),
                                new SellItemFactory(createPotionStack(Potions.LEAPING), 8, 1, 6,10),
                                new SellItemFactory(createPotionStack(Potions.LONG_INVISIBILITY), 24, 1, 6,15),
                                new SellItemFactory(createPotionStack(Potions.LONG_SLOW_FALLING), 38, 1, 6,15),
                                new SellItemFactory(createPotionStack(Potions.REGENERATION), 12, 1, 6,11),
                                new SellItemFactory(createSplashPotionStack(Potions.STRONG_HEALING), 10, 1, 6,11)},
                        5, new Factory[]{
                                new SellItemFactory(createPotionStack(Potions.LONG_STRENGTH), 21, 1, 6,15),
                                new SellItemFactory(createPotionStack(Potions.LONG_SWIFTNESS), 21, 1, 6,15),
                                new SellItemFactory(createPotionStack(Potions.STRONG_TURTLE_MASTER), 44, 1, 6,15)}
                )));

        map.put(VillagerProfession.ARMORER, copyToFastUtilMap(
                ImmutableMap.of(
                        1, new Factory[]{
                                new BuyItemFactory(Items.COAL, 15, 12, 2), new BuyItemFactory(Items.IRON_INGOT, 5, 12, 2)},
                        2, new Factory[]{
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_BOOTS, 4, 1, 12, 5, 0.05f), VillagerType.DESERT, VillagerType.PLAINS, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.TAIGA),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_BOOTS, 4, 1, 12, 5, 0.05f), VillagerType.JUNGLE, VillagerType.SWAMP),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_HELMET, 5, 1, 12, 5, 0.05f), VillagerType.DESERT, VillagerType.PLAINS, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.TAIGA),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_HELMET, 5, 1, 12, 5, 0.05f), VillagerType.JUNGLE, VillagerType.SWAMP),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_LEGGINGS, 7, 1, 12, 5, 0.05f), VillagerType.DESERT, VillagerType.PLAINS, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.TAIGA),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_LEGGINGS, 7, 1, 12, 5, 0.05f), VillagerType.JUNGLE, VillagerType.SWAMP),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_CHESTPLATE, 9, 1, 12, 5, 0.05f), VillagerType.DESERT, VillagerType.PLAINS, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.TAIGA),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_CHESTPLATE, 9, 1, 12, 5, 0.05f), VillagerType.JUNGLE, VillagerType.SWAMP)},
                        3, new Factory[]{
                                new BuyItemFactory(Items.LAVA_BUCKET, 1, 12, 20),
                                new SellItemFactory(Items.SHIELD, 5, 1, 12, 10, 0.05f),
                                new SellItemFactory(Items.BELL, 36, 1, 12, 10, 0.2f)},
                        4, new Factory[]{
                                TypedWrapperFactory.of(new SellItemFactory(enchant(Items.IRON_BOOTS, Enchantments.THORNS, 1), 8, 1, 3, 15, 0.05f), VillagerType.DESERT), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.IRON_HELMET, Enchantments.THORNS, 1), 9, 1, 3, 15, 0.05f), VillagerType.DESERT), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.IRON_LEGGINGS, Enchantments.THORNS, 1), 11, 1, 3, 15, 0.05f), VillagerType.DESERT), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.IRON_CHESTPLATE, Enchantments.THORNS, 1), 13, 1, 3, 15, 0.05f), VillagerType.DESERT),
                                TypedWrapperFactory.of(new SellItemFactory(enchant(Items.IRON_BOOTS, Enchantments.PROTECTION, 1), 8, 1, 3, 15, 0.05f), VillagerType.PLAINS), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.IRON_HELMET, Enchantments.PROTECTION, 1), 9, 1, 3, 15, 0.05f), VillagerType.PLAINS), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.IRON_LEGGINGS, Enchantments.PROTECTION, 1), 11, 1, 3, 15, 0.05f), VillagerType.PLAINS), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.IRON_CHESTPLATE, Enchantments.PROTECTION, 1), 13, 1, 3, 15, 0.05f), VillagerType.PLAINS),
                                TypedWrapperFactory.of(new SellItemFactory(enchant(Items.IRON_BOOTS, Enchantments.BINDING_CURSE, 1), 2, 1, 3, 15, 0.05f), VillagerType.SAVANNA), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.IRON_HELMET, Enchantments.BINDING_CURSE, 1), 3, 1, 3, 15, 0.05f), VillagerType.SAVANNA), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.IRON_LEGGINGS, Enchantments.BINDING_CURSE, 1), 5, 1, 3, 15, 0.05f), VillagerType.SAVANNA), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.IRON_CHESTPLATE, Enchantments.BINDING_CURSE, 1), 7, 1, 3, 15, 0.05f), VillagerType.SAVANNA),
                                TypedWrapperFactory.of(new SellItemFactory(enchant(Items.IRON_BOOTS, Enchantments.FROST_WALKER, 1), 8, 1, 3, 15, 0.05f), VillagerType.SNOW), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.IRON_HELMET, Enchantments.AQUA_AFFINITY, 1), 9, 1, 3, 15, 0.05f), VillagerType.SNOW),
                                TypedWrapperFactory.of(new SellItemFactory(enchant(Items.CHAINMAIL_BOOTS, Enchantments.UNBREAKING, 1), 8, 1, 3, 15, 0.05f), VillagerType.JUNGLE), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.CHAINMAIL_HELMET, Enchantments.UNBREAKING, 1), 9, 1, 3, 15, 0.05f), VillagerType.JUNGLE), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.CHAINMAIL_LEGGINGS, Enchantments.UNBREAKING, 1), 11, 1, 3, 15, 0.05f), VillagerType.JUNGLE), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.CHAINMAIL_CHESTPLATE, Enchantments.UNBREAKING, 1), 13, 1, 3, 15, 0.05f), VillagerType.JUNGLE),
                                TypedWrapperFactory.of(new SellItemFactory(enchant(Items.CHAINMAIL_BOOTS, Enchantments.MENDING, 1), 8, 1, 3, 15, 0.05f), VillagerType.SWAMP), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.CHAINMAIL_HELMET, Enchantments.MENDING, 1), 9, 1, 3, 15, 0.05f), VillagerType.SWAMP), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.CHAINMAIL_LEGGINGS, Enchantments.MENDING, 1), 11, 1, 3, 15, 0.05f), VillagerType.SWAMP), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.CHAINMAIL_CHESTPLATE, Enchantments.MENDING, 1), 13, 1, 3, 15, 0.05f), VillagerType.SWAMP),
                                TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND_BOOTS, 1, 4, Items.DIAMOND_LEGGINGS, 1, 3, 15, 0.05f), VillagerType.TAIGA), TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND_LEGGINGS, 1, 4, Items.DIAMOND_CHESTPLATE, 1, 3, 15, 0.05f), VillagerType.TAIGA), TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND_HELMET, 1, 4, Items.DIAMOND_BOOTS, 1, 3, 15, 0.05f), VillagerType.TAIGA), TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND_CHESTPLATE, 1, 2, Items.DIAMOND_HELMET, 1, 3, 15, 0.05f), VillagerType.TAIGA)},
                        5, new Factory[]{
                                TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 4, 16, enchant(Items.DIAMOND_CHESTPLATE, Enchantments.THORNS, 1), 1, 3, 30, 0.05f), VillagerType.DESERT), TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 3, 16, enchant(Items.DIAMOND_LEGGINGS, Enchantments.THORNS, 1), 1, 3, 30, 0.05f), VillagerType.DESERT), TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 3, 16, enchant(Items.DIAMOND_LEGGINGS, Enchantments.PROTECTION, 1), 1, 3, 30, 0.05f), VillagerType.PLAINS), TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 2, 12, enchant(Items.DIAMOND_BOOTS, Enchantments.PROTECTION, 1), 1, 3, 30, 0.05f), VillagerType.PLAINS), TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 2, 6, enchant(Items.DIAMOND_HELMET, Enchantments.BINDING_CURSE, 1), 1, 3, 30, 0.05f), VillagerType.SAVANNA), TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 3, 8, enchant(Items.DIAMOND_CHESTPLATE, Enchantments.BINDING_CURSE, 1), 1, 3, 30, 0.05f), VillagerType.SAVANNA), TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 2, 12, enchant(Items.DIAMOND_BOOTS, Enchantments.FROST_WALKER, 1), 1, 3, 30, 0.05f), VillagerType.SNOW), TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 3, 12, enchant(Items.DIAMOND_HELMET, Enchantments.AQUA_AFFINITY, 1), 1, 3, 30, 0.05f), VillagerType.SNOW), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.CHAINMAIL_HELMET, Enchantments.PROJECTILE_PROTECTION, 1), 9, 1, 3, 30, 0.05f), VillagerType.JUNGLE), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.CHAINMAIL_BOOTS, Enchantments.FEATHER_FALLING, 1), 8, 1, 3, 30, 0.05f), VillagerType.JUNGLE), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.CHAINMAIL_HELMET, Enchantments.RESPIRATION, 1), 9, 1, 3, 30, 0.05f), VillagerType.SWAMP), TypedWrapperFactory.of(new SellItemFactory(enchant(Items.CHAINMAIL_BOOTS, Enchantments.DEPTH_STRIDER, 1), 8, 1, 3, 30, 0.05f), VillagerType.SWAMP), TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 4, 18, enchant(Items.DIAMOND_CHESTPLATE, Enchantments.BLAST_PROTECTION, 1), 1, 3, 30, 0.05f), VillagerType.TAIGA), TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 3, 18, enchant(Items.DIAMOND_LEGGINGS, Enchantments.BLAST_PROTECTION, 1), 1, 3, 30, 0.05f), VillagerType.TAIGA), TypedWrapperFactory.of(new BuyItemFactory(Items.DIAMOND_BLOCK, 1, 12, 30, 42), VillagerType.TAIGA), TypedWrapperFactory.of(new BuyItemFactory(Items.IRON_BLOCK, 1, 12, 30, 4), VillagerType.DESERT, VillagerType.JUNGLE, VillagerType.PLAINS, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.SWAMP)
                        }
                )
        ));

        map.put(VillagerProfession.WEAPONSMITH, copyToFastUtilMap(ImmutableMap.of(
                1, new Factory[]{
                        new BuyForOneEmeraldFactory(Items.COAL, 22, 16, 2),
                        new BuyForOneEmeraldFactory(Items.RAW_IRON, 15, 16, 2),
                        new BuyForOneEmeraldFactory(Items.RAW_GOLD, 14, 16, 2),
                        new SellItemFactory(Items.ARMOR_STAND, 1, 1, 3),
                        new SellItemFactory(Items.SHIELD, 1, 1, 2),
                        new SellItemFactory(new ItemStack(Items.IRON_SWORD), 6, 1, 12, 1, 0.2f),
                        new SellItemFactory(new ItemStack(Items.IRON_AXE), 7, 1, 12, 1, 0.2f)},
                2, new Factory[]{
                        new BuyForMutlipleEmeraldsFactory(Items.EXPERIENCE_BOTTLE, 5, 12, 10),
                        new SellItemFactory(EnchantedBookItem.forEnchantment(
                                new EnchantmentLevelEntry(Enchantments.SHARPNESS, 2)), 11, 1, 6, 6),
                        new SellItemFactory(EnchantedBookItem.forEnchantment(
                                new EnchantmentLevelEntry(Enchantments.SMITE, 3)), 14, 1, 6,6)},
                3, new Factory[]{
                        new BuyForMutlipleEmeraldsFactory(Items.DIAMOND, 14, 12, 15),
                        new SellEnchantedToolFactory(Items.IRON_AXE, 4, 3, 10),
                        new SellEnchantedToolFactory(Items.IRON_SWORD, 4, 3, 10),
                        new SellItemFactory(EnchantedBookItem.forEnchantment(
                                new EnchantmentLevelEntry(Enchantments.KNOCKBACK, 1)), 24, 1, 6,10),
                        new SellItemFactory(EnchantedBookItem.forEnchantment(
                                new EnchantmentLevelEntry(Enchantments.LOOTING, 3)), 40, 1, 6,15)},
                4, new Factory[]{
                        new SellItemFactory(new ItemStack(Items.DIAMOND_HOE), 19, 1, 3, 15, 0.2f),
                        new SellItemFactory(new ItemStack(Items.DIAMOND_AXE), 28, 1, 3, 15, 0.2f),
                        new SellItemFactory(EnchantedBookItem.forEnchantment(
                                new EnchantmentLevelEntry(Enchantments.RIPTIDE, 3)), 36, 1, 6,20),
                        new SellItemFactory(EnchantedBookItem.forEnchantment(
                                new EnchantmentLevelEntry(Enchantments.LOYALTY, 3)), 36, 1,6, 20),
                        new SellItemFactory(EnchantedBookItem.forEnchantment(
                                new EnchantmentLevelEntry(Enchants.FROST_ASPECT, 1)), 36, 1, 6,20),
                        new SellItemFactory(EnchantedBookItem.forEnchantment(
                                new EnchantmentLevelEntry(Enchants.TOSSING, 1)), 10, 1, 6,20)},
                5, new Factory[]{
                        new SellEnchantedToolFactory(Items.DIAMOND_SWORD, 14, 1, 30)}
        )));

        map.put(VillagerProfession.TOOLSMITH, copyToFastUtilMap(ImmutableMap.of(
                1, new Factory[]{
                        new SellItemFactory(Items.DIRT, 1, 17, 1),
                        new SellItemFactory(Items.COBBLESTONE, 1, 12, 1),
                        new SellItemFactory(Items.COBBLED_DEEPSLATE, 1, 9, 1),
                        new SellItemFactory(Items.LANTERN, 1, 6, 2),
                        new SellItemFactory(new ItemStack(Items.STONE_SHOVEL), 1, 1, 12, 1, 0.2f),
                        new SellItemFactory(new ItemStack(Items.STONE_PICKAXE), 1, 1, 12, 1, 0.2f),
                        new TypeAwareSellItemFactory(1, 5, 32, 1, ImmutableMap.builder().put(
                                VillagerType.PLAINS, Items.OAK_LOG).put(
                                VillagerType.TAIGA, Items.SPRUCE_LOG).put(
                                VillagerType.SNOW, Items.SPRUCE_LOG).put(
                                VillagerType.DESERT, Items.JUNGLE_LOG).put(
                                VillagerType.JUNGLE, Items.JUNGLE_LOG).put(
                                VillagerType.SAVANNA, Items.ACACIA_LOG).put(
                                VillagerType.SWAMP, Items.MANGROVE_LOG).build()),
                        new TypeAwareSellItemFactory(1, 5, 32, 1, ImmutableMap.builder().put(
                                VillagerType.PLAINS, Items.STRIPPED_OAK_LOG).put(
                                VillagerType.TAIGA, Items.STRIPPED_SPRUCE_LOG).put(
                                VillagerType.SNOW, Items.STRIPPED_SPRUCE_LOG).put(
                                VillagerType.DESERT, Items.STRIPPED_JUNGLE_LOG).put(
                                VillagerType.JUNGLE, Items.STRIPPED_JUNGLE_LOG).put(
                                VillagerType.SAVANNA, Items.STRIPPED_ACACIA_LOG).put(
                                VillagerType.SWAMP, Items.STRIPPED_MANGROVE_LOG).build())},
                2, new Factory[]{
                        new BuyForOneEmeraldFactory(Items.RAW_GOLD, 16, 16, 5),
                        new SellItemFactory(new ItemStack(Items.IRON_HOE), 3, 1, 12, 4, 0.2f),
                        new SellItemFactory(new ItemStack(Items.GOLDEN_PICKAXE), 4, 1, 12, 4, 0.2f),
                        new SellItemFactory(new ItemStack(Items.GOLDEN_AXE), 4, 1, 12, 4, 0.2f),
                        new SellItemFactory(new ItemStack(Items.GOLDEN_SHOVEL), 2, 1, 12, 4, 0.2f),
                        new SellItemFactory(new ItemStack(Items.BELL), 15, 1, 12, 10, 0.2f)},
                3, new Factory[]{
                        new BuyForMutlipleEmeraldsFactory(Items.DIAMOND, 13, 12, 15),
                        new SellEnchantedToolFactory(Items.IRON_AXE, 4, 3, 10, 0.2f),
                        new SellEnchantedToolFactory(Items.IRON_SHOVEL, 2, 3, 10, 0.2f),
                        new SellEnchantedToolFactory(Items.IRON_PICKAXE, 4, 3, 10, 0.2f)},
                4, new Factory[]{
                        new BuyForMutlipleEmeraldsFactory(Items.IRON_BLOCK, 8, 12, 20),
                        new SellEnchantedToolFactory(Items.DIAMOND_HOE, 6, 3, 15, 0.2f),
                        new SellEnchantedToolFactory(Items.DIAMOND_SHOVEL, 7, 3, 15, 0.2f)},
                5, new Factory[]{
                        new SellEnchantedToolFactory(Items.DIAMOND_AXE, 12, 3, 30, 0.2f),
                        new SellEnchantedToolFactory(Items.DIAMOND_PICKAXE, 13, 3, 30, 0.2f)}
        )));

        map.put(VillagerProfession.BUTCHER, copyToFastUtilMap(ImmutableMap.of(
                1, new Factory[]{
                        new BuyForOneEmeraldFactory(Items.RABBIT, 6, 8, 2),
                        new SellItemFactory(Items.WHITE_WOOL, 1, 9, 1),
                        new SellItemFactory(Items.BONE, 1, 8, 1),
                        new SellItemFactory(Items.MILK_BUCKET, 1, 1, 1),
                        new SellItemFactory(Items.CAMPFIRE, 1, 1, 1),
                        new SellItemFactory(Items.COOKED_RABBIT, 1, 4, 1),
                        new SellItemFactory(Items.COOKED_SALMON, 1, 4, 1),
                        new SellItemFactory(Items.COOKED_COD, 1, 4, 1)},
                2, new Factory[]{
                        new BuyForOneEmeraldFactory(Items.MUTTON, 7, 16, 8),
                        new SellItemFactory(Items.EGG, 1, 16, 3),
                        new SellItemFactory(Items.COOKED_MUTTON, 1, 4, 4),
                        new SellItemFactory(Items.COOKED_CHICKEN, 1, 4, 16, 4)},
                3, new Factory[]{
                        new SellItemFactory(Items.RABBIT_STEW, 1, 1, 18),
                        new SellItemFactory(Items.COOKED_PORKCHOP, 1, 3, 16, 6),
                        new SellItemFactory(Items.COOKED_BEEF, 1, 3, 6)},
                4, new Factory[]{
                        new SellItemFactory(Items.DRIED_KELP, 1, 9, 10),
                        new SellItemFactory(Items.TURTLE_EGG, 1, 9, 10),
                        new SellItemFactory(Items.RABBIT_FOOT, 1, 1, 10)},
                5, new Factory[]{
                        new BuyForOneEmeraldFactory(Items.SWEET_BERRIES, 10, 12, 30)}
        )));

        map.put(VillagerProfession.LEATHERWORKER, copyToFastUtilMap(ImmutableMap.of(
                1, new Factory[]{
                        new SellItemFactory(Items.RED_DYE, 1, 8, 1),
                        new SellItemFactory(Items.YELLOW_DYE, 1, 7, 1),
                        new SellItemFactory(Items.BROWN_DYE, 1, 5, 2),
                        new BuyForOneEmeraldFactory(Items.LEATHER, 6, 16, 6),
                        new BuyForOneEmeraldFactory(Items.RABBIT_HIDE, 9, 12, 6),
                        //new SellSpecialLeatherArmorFactory(Items.LEATHER_HORSE_ARMOR, 5),
                        //new SellSpecialLeatherArmorFactory(Items.LEATHER_HELMET, 5, 12, 5),
                        //new SellSpecialLeatherArmorFactory(Items.LEATHER_BOOTS, 4, 12, 5),
                        new SellItemFactory(Items.BUNDLE, 4, 1, 2),
                        new SellItemFactory(Items.BUNDLE, 3, 1, 2)},
                2, new Factory[]{
                        new SellItemFactory(Items.GRAY_DYE, 1, 8, 4),
                        new SellItemFactory(Items.BLACK_DYE, 1, 8, 4),
                        new SellItemFactory(Items.LIGHT_BLUE_DYE, 1, 8, 4),
                        new SellItemFactory(Items.LIME_DYE, 1, 8, 4),
                        new SellItemFactory(Items.ITEM_FRAME, 1, 5, 6),
                        new SellItemFactory(Items.GLOW_ITEM_FRAME, 1, 3, 8),
                        //new SellSpecialLeatherArmorFactory(Items.LEATHER_CHESTPLATE, 12),
                        //new SellSpecialLeatherArmorFactory(Items.LEATHER_LEGGINGS, 12),
                        //new SellSpecialLeatherArmorFactory(Items.LEATHER_BOOTS, 4, 12, 12)
                },
                3, new Factory[]{
                        new SellItemFactory(Items.GREEN_DYE, 1, 8, 8),
                        new SellItemFactory(Items.LIGHT_GRAY_DYE, 1, 8, 8),
                        new SellItemFactory(Items.ORANGE_DYE, 1, 8, 8),
                        new SellItemFactory(Items.PINK_DYE, 1, 8, 8),
                        //new SellSpecialLeatherArmorFactory(Items.LEATHER_BOOTS, 4, 12, 16),
                        //new SellSpecialLeatherArmorFactory(Items.LEATHER_CHESTPLATE, 16)
                },
                4, new Factory[]{
                        new SellItemFactory(Items.PURPLE_DYE, 1, 8, 9),
                        new SellItemFactory(Items.BLUE_DYE, 1, 8, 9),
                        new SellItemFactory(Items.MAGENTA_DYE, 1, 8, 9),
                        new SellItemFactory(Items.CYAN_DYE, 1, 8, 9)//,
                        //new SellSpecialLeatherArmorFactory(Items.LEATHER_HELMET, 5, 12, 20),
                        //new SellSpecialLeatherArmorFactory(Items.LEATHER_LEGGINGS, 7, 12, 20),
                        //new SellSpecialLeatherArmorFactory(Items.LEATHER_CHESTPLATE, 8, 12, 20)
                },
                5, new Factory[]{
                        new SellItemFactory(new ItemStack(Items.SADDLE), 6, 1, 12, 30, 0.2f)//,
                        //new SellSpecialLeatherArmorFactory(Items.LEATHER_HORSE_ARMOR, 6, 12, 30)
                }
        )));

        map.put(VillagerProfession.MASON, copyToFastUtilMap(ImmutableMap.of(
                1, new Factory[]{
                        new BuyForOneEmeraldFactory(Blocks.DIORITE, 32, 16, 2),
                        new BuyForOneEmeraldFactory(Blocks.ANDESITE, 32, 16, 2),
                        new SellItemFactory(Blocks.BRICKS, 1, 24, 32, 1),
                        new SellItemFactory(Blocks.MUD_BRICKS, 1, 12, 32, 2),
                        new SellItemFactory(Blocks.TERRACOTTA, 1, 24, 32, 1),
                        new SellItemFactory(Blocks.SMOOTH_STONE, 1, 16, 32, 2),
                        new SellItemFactory(Blocks.STONE, 1, 48, 32, 1),
                        new TypeAwareSellItemFactory(1, 8, 16, 1, ImmutableMap.builder().put(
                                VillagerType.PLAINS, Items.CUT_SANDSTONE).put(
                                VillagerType.TAIGA, Items.COPPER_BLOCK).put(
                                VillagerType.SNOW, Items.PRISMARINE).put(
                                VillagerType.DESERT, Items.CUT_SANDSTONE).put(
                                VillagerType.JUNGLE, Items.CUT_RED_SANDSTONE).put(
                                VillagerType.SAVANNA, Items.CUT_RED_SANDSTONE).put(
                                VillagerType.SWAMP, Items.CLAY).build())},
                2, new Factory[]{
                        new BuyForOneEmeraldFactory(Items.QUARTZ, 12, 12, 5),
                        new SellItemFactory(Blocks.POLISHED_ANDESITE, 1, 24, 32, 3),
                        new SellItemFactory(Blocks.POLISHED_DIORITE, 1, 24, 32, 3),
                        new SellItemFactory(Blocks.POLISHED_GRANITE, 1, 24, 32, 3),
                        new SellItemFactory(Blocks.CHISELED_STONE_BRICKS, 1, 4, 32, 3)},
                3, new Factory[]{
                        new SellItemFactory(Blocks.DEEPSLATE_TILES, 1, 32, 32, 8),
                        new SellItemFactory(Blocks.DEEPSLATE_BRICKS, 1, 32, 32, 8),
                        new SellItemFactory(Blocks.POLISHED_BLACKSTONE_BRICKS, 1, 24, 32, 8),
                        new SellItemFactory(Blocks.SMOOTH_BASALT, 1, 32, 32, 8)},
                4, new Factory[]{
                        new SellItemFactory(Blocks.CUT_COPPER, 1, 48, 32, 14),
                        new SellItemFactory(Blocks.PRISMARINE_BRICKS, 1, 4, 32, 14),
                        new SellItemFactory(Blocks.SMOOTH_BASALT, 1, 16, 12, 14)},
                5, new Factory[]{
                        new SellItemFactory(Blocks.CALCITE, 1, 10, 32, 18),
                        new SellItemFactory(Blocks.SMOOTH_QUARTZ, 1, 16, 32, 18),
                        new SellItemFactory(Blocks.QUARTZ_PILLAR, 1, 24, 12, 18)}
        )));
    });















    //---------------------------------------------------------------------------------------

    private static Int2ObjectMap<Factory[]> copyToFastUtilMap(ImmutableMap<Integer, Factory[]> map) {
        return new Int2ObjectOpenHashMap<Factory[]>(map);
    }

    private static TradedItem createPotion(RegistryEntry<Potion> potion) {
        return new TradedItem(Items.POTION).withComponents(builder -> builder.add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(potion)));
    }

    private static ItemStack createPotionStack(RegistryEntry<Potion> potion) {
        return PotionContentsComponent.createStack(Items.POTION, potion);
    }

    private static ItemStack createSplashPotionStack(RegistryEntry<Potion> potion) {
        return PotionContentsComponent.createStack(Items.SPLASH_POTION, potion);
    }

    private static ItemStack enchant(Item item, Enchantment enchantment, int level) {
        ItemStack itemStack = new ItemStack(item);
        itemStack.addEnchantment(enchantment, level);
        return itemStack;
    }


    public interface Factory {
        @Nullable TradeOffer create(Entity var1, Random var2);
    }

    static class BuyForOneEmeraldFactory
            implements Factory {
        private final Item buy;
        private final int price;
        private final int maxUses;
        private final int experience;
        private final float multiplier;

        public BuyForOneEmeraldFactory(ItemConvertible item, int price, int maxUses, int experience) {
            this.buy = item.asItem();
            this.price = price;
            this.maxUses = maxUses;
            this.experience = experience;
            this.multiplier = 0.05f;
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            return new TradeOffer(new TradedItem(this.buy, this.price) , new ItemStack(Items.EMERALD), this.maxUses, this.experience, this.multiplier);
        }
    }

    static class BuyForMutlipleEmeraldsFactory
            implements Factory {
        private final Item buy;
        private final int emeralds;
        private final int maxUses;
        private final int experience;
        private final float multiplier;

        public BuyForMutlipleEmeraldsFactory(Item item, int emeralds, int maxUses, int experience) {
            this.buy = item;
            this.emeralds = emeralds;
            this.maxUses = maxUses;
            this.experience = experience;
            this.multiplier = 0.05f;
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            ItemStack offeredEmeralds = new ItemStack(Items.EMERALD, emeralds);
            return new TradeOffer(new TradedItem(this.buy, 1), offeredEmeralds, this.maxUses, this.experience, this.multiplier);
        }
    }

    public static class BuyItemFactory
            implements Factory {
        private final TradedItem stack;
        private final int maxUses;
        private final int experience;
        private final int price;
        private final float multiplier;

        public BuyItemFactory(ItemConvertible item, int count, int maxUses, int experience) {
            this(item, count, maxUses, experience, 1);
        }

        public BuyItemFactory(ItemConvertible item, int count, int maxUses, int experience, int price) {
            this(new TradedItem(item.asItem(), count), maxUses, experience, price);
        }

        public BuyItemFactory(TradedItem stack, int maxUses, int experience, int price) {
            this.stack = stack;
            this.maxUses = maxUses;
            this.experience = experience;
            this.price = price;
            this.multiplier = 0.05f;
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            return new TradeOffer(this.stack, new ItemStack(Items.EMERALD, this.price), this.maxUses, this.experience, this.multiplier);
        }
    }

    static class SellItemFactory
            implements Factory {
        private final ItemStack sell;
        private final int price;
        private final int maxUses;
        private final int experience;
        private final float multiplier;

        public SellItemFactory(Block block, int price, int count, int maxUses, int experience) {
            this(new ItemStack(block), price, count, maxUses, experience);
        }

        public SellItemFactory(Item item, int price, int count, int experience) {
            this(new ItemStack(item), price, count, 16, experience);
        }

        public SellItemFactory(Item item, int price, int count, int maxUses, int experience) {
            this(new ItemStack(item), price, count, maxUses, experience);
        }

        public SellItemFactory(ItemStack stack, int price, int count, int maxUses, int experience) {
            this(stack, price, count, maxUses, experience, 0.05f);
        }

        public SellItemFactory(ItemStack stack, int price, int count, int maxUses, int experience, float multiplier) {
            this.sell = stack;
            this.sell.setCount(count);
            this.price = price;
            this.maxUses = maxUses;
            this.experience = experience;
            this.multiplier = multiplier;
        }

        public SellItemFactory(Item itemSold, int price, int count, int maxUses, int experience, float multiplier) {
            this.sell = new ItemStack(itemSold, count);
            this.price = price;
            this.maxUses = maxUses;
            this.experience = experience;
            this.multiplier = multiplier;
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            return new TradeOffer(new TradedItem(Items.EMERALD, this.price), this.sell, this.maxUses, this.experience, this.multiplier);
        }
    }

    public static class SellSuspiciousStewFactory
            implements Factory {
        private final SuspiciousStewEffectsComponent stewEffects;
        private final int experience;
        private final float multiplier;

        public SellSuspiciousStewFactory(RegistryEntry<StatusEffect> effect, int duration, int experience) {
            this(new SuspiciousStewEffectsComponent(List.of(new SuspiciousStewEffectsComponent.StewEffect(effect, duration))), experience, 0.05f);
        }

        public SellSuspiciousStewFactory(SuspiciousStewEffectsComponent stewEffects, int experience, float multiplier) {
            this.stewEffects = stewEffects;
            this.experience = experience;
            this.multiplier = multiplier;
        }

        @Override
        @Nullable
        public TradeOffer create(Entity entity, Random random) {
            ItemStack itemStack = new ItemStack(Items.SUSPICIOUS_STEW, 1);
            itemStack.set(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, this.stewEffects);
            return new TradeOffer(new TradedItem(Items.EMERALD), itemStack, 12, this.experience, this.multiplier);
        }
    }

    public static class ProcessItemFactory
            implements Factory {
        private final TradedItem toBeProcessed;
        private final int price;
        private final ItemStack processed;
        private final int maxUses;
        private final int experience;
        private final float multiplier;

        public ProcessItemFactory(Item item, int count, Item processed, int processedCount, int maxUses, int experience, int price) {
            this(item, count, price, processed, processedCount, maxUses, experience, 0.08f);
        }

        public ProcessItemFactory(Item item, int count, int price, Item processed, int processedCount, int maxUses, int experience, float multiplier) {
            this.toBeProcessed = new TradedItem(item, count);
            this.price = price;
            this.processed = new ItemStack(processed, processedCount);
            this.maxUses = maxUses;
            this.experience = experience;
            this.multiplier = multiplier;
        }

        public ProcessItemFactory(Item item, int count, int price, ItemStack processed, int processedCount, int maxUses, int experience, float multiplier) {
            this.toBeProcessed = new TradedItem(item, count);
            this.price = price;
            this.processed = processed.copy();
            this.maxUses = maxUses;
            this.experience = experience;
            this.multiplier = multiplier;
        }

        @Override
        @Nullable
        public TradeOffer create(Entity entity, Random random) {
            return new TradeOffer(new TradedItem(Items.EMERALD, this.price), Optional.of(this.toBeProcessed), this.processed.copy(), 0, this.maxUses, this.experience, this.multiplier, 0);
        }
    }

    public static class SellEnchantedToolFactory
            implements Factory {
        private final ItemStack tool;
        private final int basePrice;
        private final int maxUses;
        private final int experience;
        private final float multiplier;

        public SellEnchantedToolFactory(Item item, int basePrice, int maxUses, int experience) {
            this(item, basePrice, maxUses, experience, 0.05f);
        }

        public SellEnchantedToolFactory(Item item, int basePrice, int maxUses, int experience, float multiplier) {
            this.tool = new ItemStack(item);
            this.basePrice = basePrice;
            this.maxUses = maxUses;
            this.experience = experience;
            this.multiplier = multiplier;
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            int i = 5 + random.nextInt(15);
            ItemStack itemStack = EnchantmentHelper.enchant(entity.getWorld().getEnabledFeatures(), random, new ItemStack(this.tool.getItem()), i, false);
            int j = Math.min(this.basePrice + i, 64);
            TradedItem tradedItem = new TradedItem(Items.EMERALD, j);
            return new TradeOffer(tradedItem, itemStack, this.maxUses, this.experience, this.multiplier);
        }
    }

    static class TypeAwareSellItemFactory
            implements Factory {
        private final ImmutableMap<Object, Object> map;
        private final int price;

        private final int count;
        private final int maxUses;
        private final int experience;

        public TypeAwareSellItemFactory(int price, int count, int maxUses, int experience, ImmutableMap<Object, Object> map) {
            Registries.VILLAGER_TYPE.stream().filter(villagerType -> !map.containsKey(villagerType)).findAny().ifPresent(villagerType -> {
                throw new IllegalStateException("Missing trade for villager type: " + Registries.VILLAGER_TYPE.getId(villagerType));
            });
            this.map = map;
            this.price = price;
            this.count = count;
            this.maxUses = maxUses;
            this.experience = experience;
        }

        @Override
        @Nullable
        public TradeOffer create(Entity entity, Random random) {
            if (entity instanceof VillagerDataContainer) {
                if(this.map.get(((VillagerDataContainer)(entity)).getVillagerData().getType()) instanceof Item item) {
                    return new TradeOffer(new TradedItem(Items.EMERALD, this.price), new ItemStack(item), this.maxUses, this.experience, 0.05f);
                }
                else if(this.map.get(((VillagerDataContainer)(entity)).getVillagerData().getType()) instanceof ItemStack itemStack) {
                    return new TradeOffer(new TradedItem(Items.EMERALD, this.price), itemStack, this.maxUses, this.experience, 0.05f);
                }
            }
            return null;
        }
    }

    public record TypedWrapperFactory(Map<VillagerType, Factory> typeToFactory) implements Factory
    {
        public static TypedWrapperFactory of(Factory factory, VillagerType ... types) {
            return new TypedWrapperFactory(Arrays.stream(types).collect(Collectors.toMap(type -> type, type -> factory)));
        }

        @Override
        @Nullable
        public TradeOffer create(Entity entity, Random random) {
            if (entity instanceof VillagerDataContainer) {
                VillagerDataContainer villagerDataContainer = (VillagerDataContainer)((Object)entity);
                VillagerType villagerType = villagerDataContainer.getVillagerData().getType();
                Factory factory = this.typeToFactory.get(villagerType);
                if (factory == null) {
                    return null;
                }
                return factory.create(entity, random);
            }
            return null;
        }
    }

    public static class SellPotionHoldingItemFactory
            implements Factory {
        private final ItemStack sell;
        private final int sellCount;
        private final int price;
        private final int maxUses;
        private final int experience;
        private final Item secondBuy;
        private final int secondCount;
        private final float priceMultiplier;

        public SellPotionHoldingItemFactory(Item arrow, int secondCount, Item tippedArrow, int sellCount, int price, int maxUses, int experience) {
            this.sell = new ItemStack(tippedArrow);
            this.price = price;
            this.maxUses = maxUses;
            this.experience = experience;
            this.secondBuy = arrow;
            this.secondCount = secondCount;
            this.sellCount = sellCount;
            this.priceMultiplier = 0.05f;
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            TradedItem tradedItem = new TradedItem(Items.EMERALD, this.price);
            List list = Registries.POTION.streamEntries().filter(potion -> !((Potion)potion.value()).getEffects().isEmpty() && BrewingRecipeRegistry.isBrewable(potion)).collect(Collectors.toList());
            RegistryEntry registryEntry = (RegistryEntry)Util.getRandom(list, random);
            ItemStack itemStack = new ItemStack(this.sell.getItem(), this.sellCount);
            itemStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(registryEntry));
            return new TradeOffer(tradedItem, Optional.of(new TradedItem(this.secondBuy, this.secondCount)), itemStack, this.maxUses, this.experience, this.priceMultiplier);
        }
    }

    public static class EnchantBookFactory
            implements Factory {
        private final int experience;
        private final List<Enchantment> possibleEnchantments;
        private final int minLevel;
        private final int maxLevel;

        public EnchantBookFactory(int experience) {
            this(experience, (Enchantment[])Registries.ENCHANTMENT.stream().filter(Enchantment::isAvailableForEnchantedBookOffer).toArray(Enchantment[]::new));
        }

        public EnchantBookFactory(int experience, Enchantment ... possibleEnchantments) {
            this(experience, 0, Integer.MAX_VALUE, possibleEnchantments);
        }

        public EnchantBookFactory(int experience, int minLevel, int maxLevel, Enchantment ... possibleEnchantments) {
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.experience = experience;
            this.possibleEnchantments = Arrays.asList(possibleEnchantments);
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            Enchantment enchantment = this.possibleEnchantments.get(random.nextInt(this.possibleEnchantments.size()));
            int i = Math.max(enchantment.getMinLevel(), this.minLevel);
            int j = Math.min(enchantment.getMaxLevel(), this.maxLevel);
            int k = MathHelper.nextInt(random, i, j);
            ItemStack itemStack = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(enchantment, k));
            int l = 2 + random.nextInt(5 + k * 10) + 3 * k;
            if (enchantment.isTreasure()) {
                l *= 2;
            }
            if (l > 64) {
                l = 64;
            }
            return new TradeOffer(new TradedItem(Items.EMERALD, l), Optional.of(new TradedItem(Items.BOOK)), itemStack, 12, this.experience, 0.2f);
        }
    }

    public static class SellMapFactory
            implements Factory {
        private final int price;
        private final TagKey<Structure> structure;
        private final String nameKey;
        private final RegistryEntry<MapDecorationType> decoration;
        private final int maxUses;
        private final int experience;

        public SellMapFactory(int price, TagKey<Structure> structure, String nameKey, RegistryEntry<MapDecorationType> decoration, int maxUses, int experience) {
            this.price = price;
            this.structure = structure;
            this.nameKey = nameKey;
            this.decoration = decoration;
            this.maxUses = maxUses;
            this.experience = experience;
        }

        @Override
        @Nullable
        public TradeOffer create(Entity entity, Random random) {
            if (!(entity.getWorld() instanceof ServerWorld)) {
                return null;
            }
            ServerWorld serverWorld = (ServerWorld)entity.getWorld();
            BlockPos blockPos = serverWorld.locateStructure(this.structure, entity.getBlockPos(), 100, true);
            if (blockPos != null) {
                ItemStack itemStack = FilledMapItem.createMap(serverWorld, blockPos.getX(), blockPos.getZ(), (byte)2, true, true);
                FilledMapItem.fillExplorationMap(serverWorld, itemStack);
                MapState.addDecorationsNbt(itemStack, blockPos, "+", this.decoration);
                itemStack.set(DataComponentTypes.ITEM_NAME, Text.translatable(this.nameKey));
                return new TradeOffer(new TradedItem(Items.EMERALD, this.price), Optional.of(new TradedItem(Items.COMPASS)), itemStack, this.maxUses, this.experience, 0.2f);
            }
            return null;
        }
    }

}
