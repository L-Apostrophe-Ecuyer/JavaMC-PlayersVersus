package frootloops.versus.util.trading;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import frootloops.versus.util.Enchants;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSetKeys;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerDataContainer;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RevampedTradeOffers {

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
                                new SellItemFactory(PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER_BREATHING).getItem(), 5, 1, 10),
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
                                new SellItemFactory(Items.SCUTE, 16, 1, 15),
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
                                new BuyForMutlipleEmeraldsFactory(Items.ENCHANTED_BOOK, 2, 12, 8),
                                new BuyForMutlipleEmeraldsFactory(Items.LAPIS_LAZULI, 3, 12, 8),
                                new BuyForOneEmeraldFactory(Items.AMETHYST_SHARD, 4, 12, 5),
                                new SellItemFactory(Items.BOOK, 1, 4, 3),
                                new SellItemFactory(Items.PAPER, 1, 24, 2),
                                new SellItemFactory(Items.LANTERN, 1, 3, 5),
                                new SellItemFactory(Items.CLOCK, 1, 1, 5),
                                new SellItemFactory(Items.GLASS, 1, 6, 3),
                                new SellItemFactory(Blocks.BOOKSHELF, 3, 1, 12, 5)},
                        2, new Factory[]{
                                new EnchantBookFactory(10),
                                new BuyForMutlipleEmeraldsFactory(Items.MUSIC_DISC_11, 8, 1, 20),
                                new BuyForOneEmeraldFactory(Items.EXPERIENCE_BOTTLE, 1, 32, 15),
                                new BuyForOneEmeraldFactory(Items.SOUL_SAND, 8, 12, 10),
                                new BuyForOneEmeraldFactory(Items.SOUL_SOIL, 8, 12, 10),
                                new BuyForOneEmeraldFactory(Items.GLOW_INK_SAC, 5, 12, 10),
                                new SellItemFactory(Items.WRITABLE_BOOK, 3, 1, 10),
                                new SellItemFactory(Items.CANDLE, 1, 3, 5),
                                new SellItemFactory(Items.BLACK_CANDLE, 1, 3, 5),
                                new SellItemFactory(Items.GRAY_CANDLE, 1, 3, 5)},
                        3, new Factory[]{
                                new EnchantBookFactory(10),
                                new EnchantBookFactory(10),
                                new BuyForMutlipleEmeraldsFactory(Items.SCULK_CATALYST, 6, 16, 20),
                                new BuyForOneEmeraldFactory(Items.SOUL_TORCH, 2, 16, 20),
                                new BuyForOneEmeraldFactory(Items.EXPERIENCE_BOTTLE, 1, 32, 20),
                                new SellItemFactory(Items.NAME_TAG, 20, 1, 15),
                                new SellItemFactory(Items.TINTED_GLASS, 1, 2, 10)},
                        4, new Factory[]{
                                new EnchantBookFactory(15),
                                new EnchantBookFactory(15),
                                new EnchantBookFactory(15),
                                new EnchantBookFactory(15),
                                new BuyForMutlipleEmeraldsFactory(Items.MUSIC_DISC_5, 52, 1, 50),
                                new BuyForMutlipleEmeraldsFactory(Items.ENDER_EYE, 5, 12, 20),
                                new BuyForOneEmeraldFactory(Items.ENDER_PEARL, 1, 12, 15)},
                        5, new Factory[]{
                                new ProcessItemFactory(Items.ECHO_SHARD, 1, Items.RECOVERY_COMPASS,1, 1, 20),
                                new SellItemFactory(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.MENDING, 1)), 48, 1, 1, 30)}
                )));

        map.put(VillagerProfession.CARTOGRAPHER, copyToFastUtilMap(
                ImmutableMap.of(
                        1, new Factory[]{
                                new BuyForMutlipleEmeraldsFactory(Raid.getOminousBanner(), 24, 16, 5),
                                new SellItemFactory(Items.SPYGLASS, 2, 1, 8),
                                new SellItemFactory(Items.COMPASS, 1, 1, 1),
                                new SellItemFactory(Items.MAP, 1, 1, 3)},
                        2, new Factory[]{
                                new SellItemFactory(Items.FLOWER_BANNER_PATTERN, 18, 1, 15),
                                new SellItemFactory(Items.GLASS_PANE, 1, 16, 8),
                                new SellItemFactory(Items.CAMPFIRE, 1, 1, 10),
                                new SellMapFactory(
                                        8,
                                        StructureTags.SHIPWRECK, "Shipwreck Explorer Map",
                                        MapIcon.Type.TARGET_X, 3, 10),
                                new SellMapFactory(
                                        6,
                                        StructureTags.OCEAN_RUIN, "Ruins Explorer Map",
                                        MapIcon.Type.TARGET_X, 3, 10),
                                new SellMapFactory(
                                        6,
                                        StructureTags.EYE_OF_ENDER_LOCATED, "Ruins Explorer Map",
                                        MapIcon.Type.TARGET_X, 1, 20),
                                new SellMapFactory(
                                        6,
                                        StructureTags.RUINED_PORTAL, "Ruins Explorer Map",
                                        MapIcon.Type.TARGET_X, 3, 10)},
                        3, new Factory[]{
                                new SellItemFactory(Items.SKULL_BANNER_PATTERN, 21, 1, 15),
                                new SellItemFactory(Items.MUSIC_DISC_WAIT, 32, 1, 15),
                                new SellMapFactory(
                                        13,
                                        StructureTags.ON_OCEAN_EXPLORER_MAPS, "filled_map.monument",
                                        MapIcon.Type.MONUMENT, 3, 5)},
                        4, new Factory[]{
                                new SellItemFactory(Items.CREEPER_BANNER_PATTERN, 24, 1, 15),
                                new SellItemFactory(Items.MUSIC_DISC_FAR, 32, 1, 15),
                                new SellMapFactory(
                                        14,
                                        StructureTags.ON_WOODLAND_EXPLORER_MAPS, "filled_map.mansion",
                                        MapIcon.Type.MANSION, 3, 10)},
                        5, new Factory[]{
                                new SellItemFactory(Items.GLOBE_BANNER_PATTERN, 8, 1, 30)}
                )));

        map.put(VillagerProfession.CLERIC, copyToFastUtilMap(
                ImmutableMap.of(
                        1, new Factory[]{
                                new BuyForOneEmeraldFactory(Items.SPIDER_EYE, 8, 12, 1),
                                new BuyForOneEmeraldFactory(Items.ROTTEN_FLESH, 8, 12, 1),
                                new BuyForOneEmeraldFactory(Items.GOLD_INGOT, 3, 24, 10),
                                new SellItemFactory(Items.GLASS_BOTTLE, 1, 12, 1),
                                new SellItemFactory(Items.SUGAR, 1, 24, 1),
                                new SellItemFactory(Items.GUNPOWDER, 1, 3, 1)},
                        2, new Factory[]{
                                new BuyForMutlipleEmeraldsFactory(Items.GOLDEN_APPLE, 5, 16, 5),
                                new BuyForOneEmeraldFactory(Items.GLOWSTONE_DUST, 16, 12, 3),
                                new BuyForOneEmeraldFactory(Items.BLAZE_POWDER, 1, 16, 8),
                                new BuyForOneEmeraldFactory(Blocks.NETHER_WART, 21, 16, 5),
                                new SellItemFactory(Items.RABBIT_FOOT, 1, 1, 12, 5),
                                new SellItemFactory(Items.FERMENTED_SPIDER_EYE, 1, 1, 12, 5)},
                        3, new Factory[]{
                                new SellItemFactory(PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER_BREATHING), 3, 1, 6, 10),
                                new SellItemFactory(PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WEAKNESS), 15, 1, 6,10),
                                new SellItemFactory(PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.HEALING), 4, 1, 6,10),
                                new SellItemFactory(PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.SWIFTNESS), 5, 1, 6,10),
                                new SellItemFactory(Items.MAGMA_CREAM, 1, 1, 12, 3),
                                new SellItemFactory(Items.REDSTONE, 1, 14, 12, 3),
                                new ProcessItemFactory(Items.CARROT, 8, Items.GOLDEN_CARROT,8, 16, 8),
                                new ProcessItemFactory(Items.MELON_SLICE, 8, Items.GLISTERING_MELON_SLICE,8, 16, 8)},
                        4, new Factory[]{
                                new SellItemFactory(PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.LEAPING), 8, 1, 6,10),
                                new SellItemFactory(PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.STRENGTH), 21, 1, 6,15),
                                new SellItemFactory(PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.INVISIBILITY), 24, 1, 6,15),
                                new SellItemFactory(PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.SLOW_FALLING), 38, 1, 6,15),
                                new SellItemFactory(PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.REGENERATION), 12, 1, 6,15),
                                new SellItemFactory(PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.TURTLE_MASTER), 44, 1, 6,15)},
                        5, new Factory[]{
                                new TotemBlessingFactory()}
                )));

        map.put(VillagerProfession.ARMORER, copyToFastUtilMap(ImmutableMap.of(
                1, new Factory[]{
                        new BuyForOneEmeraldFactory(Items.RAW_IRON, 15, 16, 2),
                        new BuyForOneEmeraldFactory(Items.RAW_GOLD, 12, 16, 2),
                        new ProcessItemFactory(Items.RAW_COPPER, 64, Items.COPPER_BLOCK,16, 16, 4),
                        new SellItemFactory(Items.BELL, 8, 1, 1),
                        new SellItemFactory(Items.CHAIN, 1, 6, 1),
                        new SellItemFactory(new ItemStack(Items.CHAINMAIL_CHESTPLATE), 5, 1, 12, 1, 0.2f),
                        new SellItemFactory(new ItemStack(Items.CHAINMAIL_LEGGINGS), 4, 1, 12, 1, 0.2f),
                        new SellItemFactory(new ItemStack(Items.CHAINMAIL_HELMET), 3, 1, 12, 1, 0.2f),
                        new SellItemFactory(new ItemStack(Items.CHAINMAIL_BOOTS), 2, 1, 12, 1, 0.2f)},
                2, new Factory[]{
                        new SellItemFactory(Items.IRON_HORSE_ARMOR, 8, 1, 5),
                        new SellSpecialHeavyArmorFactory(((ArmorItem) Items.CHAINMAIL_HELMET), 9, 1, 7),
                        new SellSpecialHeavyArmorFactory(((ArmorItem) Items.CHAINMAIL_BOOTS), 8, 1, 7),
                        new SellItemFactory(new ItemStack(Items.IRON_LEGGINGS), 11, 1, 12, 10, 0.2f),
                        new SellItemFactory(new ItemStack(Items.IRON_BOOTS), 8, 1, 12, 5, 0.2f),
                        new SellItemFactory(new ItemStack(Items.IRON_HELMET), 9, 1, 12, 5, 0.2f)},
                3, new Factory[]{
                        new BuyForMutlipleEmeraldsFactory(Items.GOLDEN_HORSE_ARMOR, 13, 12, 10),
                        new SellItemFactory(new ItemStack(Items.LAVA_BUCKET), 10, 1, 12, 5, 0.2f),
                        new SellSpecialHeavyArmorFactory(((ArmorItem) Items.CHAINMAIL_CHESTPLATE), 12, 1, 12),
                        new SellSpecialHeavyArmorFactory(((ArmorItem) Items.CHAINMAIL_LEGGINGS), 10, 1, 12),
                        new SellItemFactory(new ItemStack(Items.IRON_CHESTPLATE), 12, 1, 12, 10, 0.2f)},
                4, new Factory[]{
                        new BuyForMutlipleEmeraldsFactory(Items.DIAMOND, 14, 12, 20),
                        new SellSpecialHeavyArmorFactory(((ArmorItem) Items.IRON_LEGGINGS), 14, 1, 12),
                        new SellSpecialHeavyArmorFactory(((ArmorItem) Items.IRON_HELMET), 10, 1, 12),
                        new SellSpecialHeavyArmorFactory(((ArmorItem) Items.IRON_BOOTS), 8, 1, 12)},
                5, new Factory[]{
                        new BuyForMutlipleEmeraldsFactory(Items.DIAMOND_HORSE_ARMOR, 38, 12, 30),
                        new SellSpecialHeavyArmorFactory(((ArmorItem) Items.IRON_CHESTPLATE), 16, 1, 30),
                        new SellSpecialHeavyArmorFactory(((ArmorItem) Items.DIAMOND_CHESTPLATE), 24, 1, 30)}
        )));

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
                            VillagerType.SWAMP, Items.MANGROVE_LOG).build())},
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
                        new SellSpecialLeatherArmorFactory(Items.LEATHER_HORSE_ARMOR, 5),
                        new SellSpecialLeatherArmorFactory(Items.LEATHER_HELMET, 5, 12, 5),
                        new SellSpecialLeatherArmorFactory(Items.LEATHER_BOOTS, 4, 12, 5),
                        new SellItemFactory(Items.BUNDLE, 4, 1, 2),
                        new SellItemFactory(Items.BUNDLE, 3, 1, 2)},
                2, new Factory[]{
                        new SellItemFactory(Items.GRAY_DYE, 1, 8, 4),
                        new SellItemFactory(Items.BLACK_DYE, 1, 8, 4),
                        new SellItemFactory(Items.LIGHT_BLUE_DYE, 1, 8, 4),
                        new SellItemFactory(Items.LIME_DYE, 1, 8, 4),
                        new SellItemFactory(Items.ITEM_FRAME, 1, 5, 6),
                        new SellItemFactory(Items.GLOW_ITEM_FRAME, 1, 3, 8),
                        new SellSpecialLeatherArmorFactory(Items.LEATHER_CHESTPLATE, 12),
                        new SellSpecialLeatherArmorFactory(Items.LEATHER_LEGGINGS, 12),
                        new SellSpecialLeatherArmorFactory(Items.LEATHER_BOOTS, 4, 12, 12)},
                3, new Factory[]{
                        new SellItemFactory(Items.GREEN_DYE, 1, 8, 8),
                        new SellItemFactory(Items.LIGHT_GRAY_DYE, 1, 8, 8),
                        new SellItemFactory(Items.ORANGE_DYE, 1, 8, 8),
                        new SellItemFactory(Items.PINK_DYE, 1, 8, 8),
                        new SellSpecialLeatherArmorFactory(Items.LEATHER_BOOTS, 4, 12, 16),
                        new SellSpecialLeatherArmorFactory(Items.LEATHER_CHESTPLATE, 16)},
                4, new Factory[]{
                        new SellItemFactory(Items.PURPLE_DYE, 1, 8, 9),
                        new SellItemFactory(Items.BLUE_DYE, 1, 8, 9),
                        new SellItemFactory(Items.MAGENTA_DYE, 1, 8, 9),
                        new SellItemFactory(Items.CYAN_DYE, 1, 8, 9),
                        new SellSpecialLeatherArmorFactory(Items.LEATHER_HELMET, 5, 12, 20),
                        new SellSpecialLeatherArmorFactory(Items.LEATHER_LEGGINGS, 7, 12, 20),
                        new SellSpecialLeatherArmorFactory(Items.LEATHER_CHESTPLATE, 8, 12, 20)},
                5, new Factory[]{
                        new SellItemFactory(new ItemStack(Items.SADDLE), 6, 1, 12, 30, 0.2f),
                        new SellSpecialLeatherArmorFactory(Items.LEATHER_HORSE_ARMOR, 6, 12, 30)}
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

    private static Int2ObjectMap<Factory[]> copyToFastUtilMap(ImmutableMap<Integer, Factory[]> map) {
        return new Int2ObjectOpenHashMap<Factory[]>(map);
    }

    public static interface Factory {
        @Nullable
        public TradeOffer create(Entity var1, Random var2);
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
            ItemStack itemStack = new ItemStack(this.buy, this.price);
            return new TradeOffer(itemStack, new ItemStack(Items.EMERALD), this.maxUses, this.experience, this.multiplier);
        }
    }

    static class BuyForMutlipleEmeraldsFactory
            implements Factory {
        private final ItemStack buy;
        private final int emeralds;
        private final int maxUses;
        private final int experience;
        private final float multiplier;

        public BuyForMutlipleEmeraldsFactory(ItemStack stack, int emeralds, int maxUses, int experience) {
            this.buy = stack;
            this.emeralds = emeralds;
            this.maxUses = maxUses;
            this.experience = experience;
            this.multiplier = 0.05f;
        }

        public BuyForMutlipleEmeraldsFactory(Item item, int emeralds, int maxUses, int experience) {
            this.buy = new ItemStack(item, 1);
            this.emeralds = emeralds;
            this.maxUses = maxUses;
            this.experience = experience;
            this.multiplier = 0.05f;
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            ItemStack offeredEmeralds = new ItemStack(Items.EMERALD, emeralds);
            return new TradeOffer(this.buy, offeredEmeralds, this.maxUses, this.experience, this.multiplier);
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

        @Override
        public TradeOffer create(Entity entity, Random random) {
            return new TradeOffer(new ItemStack(Items.EMERALD, this.price), this.sell, this.maxUses, this.experience, this.multiplier);
        }
    }

    static class SellSuspiciousStewFactory
            implements Factory {
        final StatusEffect effect;
        final int duration;
        final int experience;
        private final float multiplier;

        public SellSuspiciousStewFactory(StatusEffect effect, int duration, int experience) {
            this.effect = effect;
            this.duration = duration;
            this.experience = experience;
            this.multiplier = 0.05f;
        }

        @Override
        @Nullable
        public TradeOffer create(Entity entity, Random random) {
            ItemStack itemStack = new ItemStack(Items.SUSPICIOUS_STEW, 1);
            SuspiciousStewItem.addEffectToStew(itemStack, this.effect, this.duration);
            return new TradeOffer(new ItemStack(Items.EMERALD, 1), itemStack, 12, this.experience, this.multiplier);
        }
    }

    static class ProcessItemFactory
            implements Factory {
        private final ItemStack secondBuy;
        private final int secondCount;
        private final int price;
        private final ItemStack sell;
        private final int sellCount;
        private final int maxUses;
        private final int experience;
        private final float multiplier;

        public ProcessItemFactory(ItemConvertible item, int secondCount, Item sellItem, int sellCount, int maxUses, int experience) {
            this(item, secondCount, 1, sellItem, sellCount, maxUses, experience);
        }

        public ProcessItemFactory(ItemConvertible item, int secondCount, int price, Item sellItem, int sellCount, int maxUses, int experience) {
            this.secondBuy = new ItemStack(item);
            this.secondCount = secondCount;
            this.price = price;
            this.sell = new ItemStack(sellItem);
            this.sellCount = sellCount;
            this.maxUses = maxUses;
            this.experience = experience;
            this.multiplier = 0.05f;
        }

        @Override
        @Nullable
        public TradeOffer create(Entity entity, Random random) {
            return new TradeOffer(new ItemStack(Items.EMERALD, this.price), new ItemStack(this.secondBuy.getItem(), this.secondCount), new ItemStack(this.sell.getItem(), this.sellCount), this.maxUses, this.experience, this.multiplier);
        }
    }

    static class SellEnchantedToolFactory
            implements Factory {
        private final Item tool;
        private final int basePrice;
        private final int maxUses;
        private final int experience;
        private final float multiplier;

        public SellEnchantedToolFactory(Item item, int basePrice, int maxUses, int experience) {
            this(item, basePrice, maxUses, experience, 0.05f);
        }

        public SellEnchantedToolFactory(Item item, int basePrice, int maxUses, int experience, float multiplier) {
            this.tool = item;
            this.basePrice = basePrice;
            this.maxUses = maxUses;
            this.experience = experience;
            this.multiplier = multiplier;
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            ItemStack unenchantedStack = new ItemStack(this.tool, 1);

            int i = 5 + random.nextInt(15);
            ItemStack enchantedStack = EnchantmentHelper.enchant(random, new ItemStack(this.tool), i, false);
            ItemStack emeraldStack = new ItemStack(Items.EMERALD, Math.min(this.basePrice + i, 64));

            return new TradeOffer(emeraldStack, unenchantedStack, enchantedStack, this.maxUses, this.experience, this.multiplier);
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
                throw new IllegalStateException("Missing trade for villager type: " + Registries.VILLAGER_TYPE.getId((VillagerType)villagerType));
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
                ItemStack soldItem = new ItemStack((Item) this.map.get(((VillagerDataContainer)(entity)).getVillagerData().getType()), this.count);
                return new TradeOffer(new ItemStack(Items.EMERALD, this.price), soldItem, this.maxUses, this.experience, 0.05f);
            }
            return null;
        }
    }

    static class SellPotionHoldingItemFactory
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
            ItemStack itemStack = new ItemStack(Items.EMERALD, this.price);
            List list = Registries.POTION.stream().filter(potion -> !potion.getEffects().isEmpty() && BrewingRecipeRegistry.isBrewable(potion)).collect(Collectors.toList());
            Potion potion2 = (Potion)list.get(random.nextInt(list.size()));
            ItemStack itemStack2 = PotionUtil.setPotion(new ItemStack(this.sell.getItem(), this.sellCount), potion2);
            return new TradeOffer(itemStack, new ItemStack(this.secondBuy, this.secondCount), itemStack2, this.maxUses, this.experience, this.priceMultiplier);
        }
    }

    static class EnchantBookFactory
            implements Factory {
        private final int experience;

        public EnchantBookFactory(int experience) {
            this.experience = experience;
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            List list = Registries.ENCHANTMENT
                    .stream()
                    .filter(enchantment -> enchantment.isAvailableForEnchantedBookOffer() && !enchantment.isTreasure())
                    .collect(Collectors.toList());
            Enchantment enchantment = (Enchantment)list.get(random.nextInt(list.size()));
            int i = MathHelper.nextInt(random, enchantment.getMinLevel(), enchantment.getMaxLevel());
            ItemStack itemStack = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(enchantment, i));
            int j = 2 + random.nextInt(5 + i * 10) + 3 * i;
            if (j > 64) {
                j = 64;
            }
            return new TradeOffer(new ItemStack(Items.EMERALD, j), new ItemStack(Items.BOOK), itemStack, 12, this.experience, 0.2f);
        }
    }

    static class SellMapFactory
            implements Factory {
        private final int price;
        private final TagKey<Structure> structure;
        private final String nameKey;
        private final MapIcon.Type iconType;
        private final int maxUses;
        private final int experience;

        public SellMapFactory(int price, TagKey<Structure> structure, String nameKey, MapIcon.Type iconType, int maxUses, int experience) {
            this.price = price;
            this.structure = structure;
            this.nameKey = nameKey;
            this.iconType = iconType;
            this.maxUses = maxUses;
            this.experience = experience;
        }

        @Override
        @Nullable
        public TradeOffer create(Entity entity, Random random) {
            if (!(entity.world instanceof ServerWorld)) {
                return null;
            }
            ServerWorld serverWorld = (ServerWorld)entity.world;
            BlockPos blockPos = serverWorld.locateStructure(this.structure, entity.getBlockPos(), 100, true);
            if (blockPos != null) {
                ItemStack itemStack = FilledMapItem.createMap(serverWorld, blockPos.getX(), blockPos.getZ(), (byte)2, true, true);
                FilledMapItem.fillExplorationMap(serverWorld, itemStack);
                MapState.addDecorationsNbt(itemStack, blockPos, "+", this.iconType);
                itemStack.setCustomName(Text.translatable(this.nameKey));
                return new TradeOffer(new ItemStack(Items.EMERALD, this.price), new ItemStack(Items.COMPASS, 1), itemStack, this.maxUses, this.experience, 0.2f);
            }
            return null;
        }
    }

    static class SellSpecialHeavyArmorFactory
            implements Factory {
        private final ArmorItem armorPiece;
        private final int basePrice;
        private final int maxUses;
        private final int experience;

        public SellSpecialHeavyArmorFactory(ArmorItem armorPiece, int basePrice, int maxUses, int experience) {
            this.armorPiece = armorPiece;
            this.basePrice = basePrice;
            this.maxUses = maxUses;
            this.experience = experience;
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            ItemStack unenchantedStack = new ItemStack(this.armorPiece, 1);

            int i = 2 + random.nextInt(8);
            ItemStack enchantedStack = EnchantmentHelper.enchant(random, new ItemStack(this.armorPiece), i, false);
            ItemStack emeraldStack = new ItemStack(Items.EMERALD, Math.min(this.basePrice + i, 64));

            double armorAmount = ((ArmorItem)this.armorPiece).getProtection();
            double toughnessAmount = this.armorPiece.getToughness();
            double knockbackResistance = this.armorPiece.getMaterial().getKnockbackResistance();
            EquipmentSlot slot = this.armorPiece.getSlotType();

            float rand = random.nextFloat();
            if (rand > 0.9f) {
                armorAmount += 1;
                toughnessAmount += 2;
                enchantedStack.addAttributeModifier(
                        EntityAttributes.GENERIC_MOVEMENT_SPEED,
                        new EntityAttributeModifier("Movement speed", -0.01, EntityAttributeModifier.Operation.ADDITION),
                        slot);
            }
            else if (rand > 0.8f) {
                armorAmount += 1;
                toughnessAmount += 2;
                enchantedStack.addAttributeModifier(
                        EntityAttributes.GENERIC_ATTACK_SPEED,
                        new EntityAttributeModifier("Tool modifier", -0.25, EntityAttributeModifier.Operation.ADDITION),
                        slot);
            }
            else if (rand > 0.6f) {
                enchantedStack.addAttributeModifier(
                        EntityAttributes.GENERIC_ATTACK_DAMAGE,
                        new EntityAttributeModifier("Tool modifier", 1.0, EntityAttributeModifier.Operation.ADDITION),
                        EquipmentSlot.CHEST);
                enchantedStack.addAttributeModifier(
                        EntityAttributes.GENERIC_ATTACK_SPEED,
                        new EntityAttributeModifier("Tool modifier", -0.25, EntityAttributeModifier.Operation.ADDITION),
                        slot);
            }
            else if (rand > 0.5f) {
                armorAmount += 2;
                knockbackResistance += 0.2;
            }
            else if (rand > 0.3f) {
                knockbackResistance += 0.1;
                enchantedStack.addAttributeModifier(
                        EntityAttributes.GENERIC_ATTACK_DAMAGE,
                        new EntityAttributeModifier("Tool modifier", 1.0, EntityAttributeModifier.Operation.ADDITION),
                        slot);
                enchantedStack.addAttributeModifier(
                        EntityAttributes.GENERIC_ATTACK_SPEED,
                        new EntityAttributeModifier("Tool modifier", -0.5, EntityAttributeModifier.Operation.ADDITION),
                        slot);
            }
            else if (rand > 0.2f) {
                knockbackResistance += 0.3;
                enchantedStack.addAttributeModifier(
                        EntityAttributes.GENERIC_MOVEMENT_SPEED,
                        new EntityAttributeModifier("Movement speed", -0.01, EntityAttributeModifier.Operation.ADDITION),
                        slot);
            }
            else if (rand > 0.1f) {
                toughnessAmount += 1;
            }

            enchantedStack.addAttributeModifier(
                    EntityAttributes.GENERIC_ARMOR,
                    new EntityAttributeModifier("Armor", armorAmount, EntityAttributeModifier.Operation.ADDITION),
                    slot);
            enchantedStack.addAttributeModifier(
                    EntityAttributes.GENERIC_ARMOR_TOUGHNESS,
                    new EntityAttributeModifier("Armor toughness", toughnessAmount, EntityAttributeModifier.Operation.ADDITION),
                    slot);
            enchantedStack.addAttributeModifier(
                    EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,
                    new EntityAttributeModifier("Armor knockback resistance", knockbackResistance, EntityAttributeModifier.Operation.ADDITION),
                    slot);

            return new TradeOffer(emeraldStack, unenchantedStack, enchantedStack, this.maxUses, this.experience,  0.2f);
        }
    }

    static class SellSpecialLeatherArmorFactory
            implements Factory {
        private final Item sell;
        private final int price;
        private final int maxUses;
        private final int experience;

        public SellSpecialLeatherArmorFactory(Item item, int price) {
            this(item, price, 12, 1);
        }

        public SellSpecialLeatherArmorFactory(Item item, int price, int maxUses, int experience) {
            this.sell = item;
            this.price = price;
            this.maxUses = maxUses;
            this.experience = experience;
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            ItemStack emeraldStack = new ItemStack(Items.EMERALD, this.price);
            ItemStack armorStack = new ItemStack(this.sell);

            if (this.sell instanceof DyeableItem) {
                ArrayList<DyeItem> list = Lists.newArrayList();
                list.add(SellSpecialLeatherArmorFactory.getDye(random));
                if (random.nextFloat() > 0.7f) {
                    list.add(SellSpecialLeatherArmorFactory.getDye(random));
                }
                if (random.nextFloat() > 0.8f) {
                    list.add(SellSpecialLeatherArmorFactory.getDye(random));
                }
                armorStack = DyeableItem.blendAndSetColor(armorStack, list);
            }

            if (this.sell instanceof DyeableArmorItem) {
                EquipmentSlot equipmentSlot = ((ArmorItem)this.sell).getSlotType();
                if(equipmentSlot == EquipmentSlot.CHEST) {
                    float rand = random.nextFloat();
                    if (rand > 0.8f) {
                        armorStack.addAttributeModifier(
                                EntityAttributes.GENERIC_ARMOR,
                                new EntityAttributeModifier("Armor", 1, EntityAttributeModifier.Operation.ADDITION),
                                equipmentSlot);
                        armorStack.addAttributeModifier(
                                EntityAttributes.GENERIC_ATTACK_SPEED,
                                new EntityAttributeModifier("Tool modifier", 0.5, EntityAttributeModifier.Operation.ADDITION),
                                equipmentSlot);
                    }
                    else if(rand > 0.6f) {
                        armorStack.addAttributeModifier(
                                EntityAttributes.GENERIC_ARMOR,
                                new EntityAttributeModifier("Armor", 1, EntityAttributeModifier.Operation.ADDITION),
                                equipmentSlot);
                        armorStack.addAttributeModifier(
                                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                                new EntityAttributeModifier("Tool modifier", 0.5, EntityAttributeModifier.Operation.ADDITION),
                                equipmentSlot);
                    }
                    else if(rand > 0.4f) {
                        armorStack.addAttributeModifier(
                                EntityAttributes.GENERIC_ARMOR,
                                new EntityAttributeModifier("Armor", 3, EntityAttributeModifier.Operation.ADDITION),
                                equipmentSlot);
                        armorStack.addAttributeModifier(
                                EntityAttributes.GENERIC_ARMOR_TOUGHNESS,
                                new EntityAttributeModifier("Armor toughness", 1.0, EntityAttributeModifier.Operation.ADDITION),
                                equipmentSlot);
                    }
                    else if(rand > 0.1f) {
                        armorStack.addAttributeModifier(
                                EntityAttributes.GENERIC_ARMOR,
                                new EntityAttributeModifier("Armor", 1, EntityAttributeModifier.Operation.ADDITION),
                                equipmentSlot);
                        armorStack.addAttributeModifier(
                                EntityAttributes.GENERIC_ATTACK_SPEED,
                                new EntityAttributeModifier("Tool modifier", 0.25, EntityAttributeModifier.Operation.ADDITION),
                                equipmentSlot);
                    }
                }
                else if(equipmentSlot == EquipmentSlot.LEGS || equipmentSlot == EquipmentSlot.FEET) {
                    float rand = random.nextFloat();
                    if (rand > 0.9f) {
                        armorStack.addAttributeModifier(
                                EntityAttributes.GENERIC_MOVEMENT_SPEED,
                                new EntityAttributeModifier("Movement speed", 0.02, EntityAttributeModifier.Operation.ADDITION),
                                equipmentSlot);
                        armorStack.addAttributeModifier(
                                EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,
                                new EntityAttributeModifier("Armor knockback resistance", -0.1, EntityAttributeModifier.Operation.ADDITION),
                                equipmentSlot);
                    }
                    else if (rand > 0.6f) {
                        armorStack.addAttributeModifier(
                                EntityAttributes.GENERIC_ARMOR,
                                new EntityAttributeModifier("Armor", 1, EntityAttributeModifier.Operation.ADDITION),
                                equipmentSlot);
                        armorStack.addAttributeModifier(
                                EntityAttributes.GENERIC_MOVEMENT_SPEED,
                                new EntityAttributeModifier("Movement speed", 0.01, EntityAttributeModifier.Operation.ADDITION),
                                equipmentSlot);
                    }
                    else if(rand > 0.4f) {
                        armorStack.addAttributeModifier(
                                EntityAttributes.GENERIC_ARMOR,
                                new EntityAttributeModifier("Armor", 1, EntityAttributeModifier.Operation.ADDITION),
                                equipmentSlot);
                        armorStack.addAttributeModifier(
                                EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,
                                new EntityAttributeModifier("Armor knockback resistance", 0.1, EntityAttributeModifier.Operation.ADDITION),
                                equipmentSlot);
                    }
                }
            }
            return new TradeOffer(emeraldStack, armorStack, this.maxUses, this.experience, 0.2f);
        }

        private static DyeItem getDye(Random random) {
            return DyeItem.byColor(DyeColor.byId(random.nextInt(16)));
        }
    }

    static class TotemBlessingFactory
            implements Factory {

        public TotemBlessingFactory() {
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {

            ItemStack emeraldStack = new ItemStack(Items.EMERALD, 48);
            ItemStack totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);

            float rand = random.nextFloat();
            if (rand > 0.8f) {
                totemStack.addAttributeModifier(
                        EntityAttributes.GENERIC_ATTACK_SPEED,
                        new EntityAttributeModifier("Tool modifier", 0.25, EntityAttributeModifier.Operation.ADDITION),
                        EquipmentSlot.OFFHAND);
            }
            else if(rand > 0.5f) {
                totemStack.addAttributeModifier(
                        EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,
                        new EntityAttributeModifier("Armor knockback resistance", 0.05, EntityAttributeModifier.Operation.ADDITION),
                        EquipmentSlot.OFFHAND);
            }
            else if(rand > 0.4f) {
                totemStack.addAttributeModifier(
                        EntityAttributes.GENERIC_ATTACK_DAMAGE,
                        new EntityAttributeModifier("Tool modifier", 0.5, EntityAttributeModifier.Operation.ADDITION),
                        EquipmentSlot.OFFHAND);
            }
            else if(rand > 0.2f) {
                totemStack.addAttributeModifier(
                        EntityAttributes.GENERIC_ARMOR,
                        new EntityAttributeModifier("Armor", 1.0, EntityAttributeModifier.Operation.ADDITION),
                        EquipmentSlot.OFFHAND);
            }
            else {
                totemStack.addAttributeModifier(
                        EntityAttributes.GENERIC_MOVEMENT_SPEED,
                        new EntityAttributeModifier("Movement speed", 0.01, EntityAttributeModifier.Operation.ADDITION),
                        EquipmentSlot.OFFHAND);
            }
            return new TradeOffer(emeraldStack, new ItemStack(Items.TOTEM_OF_UNDYING), totemStack, 1, 30, 0.0f);
        }

        private static DyeItem getDye(Random random) {
            return DyeItem.byColor(DyeColor.byId(random.nextInt(16)));
        }
    }
}
