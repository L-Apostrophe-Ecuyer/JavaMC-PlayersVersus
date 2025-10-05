package frootloops.versus.mod.mobs.passive;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import frootloops.versus.mod.environment.CustomBlockItems;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.items_and_effects.CustomBrewingItems;
import frootloops.versus.mod.items_and_effects.CustomEquipment;
import frootloops.versus.mod.items_and_effects.brewing.CustomPotions;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.provider.TradeRebalanceEnchantmentProviders;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.potion.Potions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.util.Util;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;

import java.util.Map;

import static frootloops.versus.mod.mobs.passive.RevampedTradeFactories.*;

public class RevampedVillagerOffers {

    public static final Map<RegistryKey<VillagerProfession>, Int2ObjectMap<Factory[]>> PROFESSION_TO_LEVELED_TRADE = Util.make(
            Maps.<RegistryKey<VillagerProfession>, Int2ObjectMap<Factory[]>>newHashMap(),
            map -> {
                map.put(VillagerProfession.FARMER, getFarmerOffers());
                map.put(VillagerProfession.FISHERMAN, getFishermanOffers());
                map.put(VillagerProfession.MASON, getMasonOffers());
                map.put(VillagerProfession.CLERIC, getClericOffers());
                map.put(VillagerProfession.LIBRARIAN, getLibrarianOffers());
                map.put(VillagerProfession.ARMORER, getArmorerOffers());


                // Sheperd is done!
                map.put(
                        VillagerProfession.SHEPHERD,
                        copyToFastUtilMap(
                                ImmutableMap.of(
                                        1, new Factory[]{
                                                new BuyItemFactory(Items.BROWN_DYE, 8, 6),
                                                new BuyItemFactory(Items.BLACK_DYE, 14, 4),
                                                new BuyItemFactory(Items.WHEAT, 32, 4),
                                                new SellItemFactory(Blocks.WHITE_WOOL, 16, 2),
                                                new SellItemFactory(Blocks.BROWN_WOOL, 8, 2),
                                                new SellItemFactory(Blocks.BLACK_WOOL, 12, 2),
                                                new SellItemFactory(Blocks.GRAY_WOOL, 16, 2),
                                                new SellItemFactory(Blocks.LIGHT_GRAY_WOOL, 16, 2),
                                                new SellItemFactory(Items.SHEARS, 1, 1, 4),
                                                new SellItemFactory(Items.LEAD, 1, 16, 4)
                                        },
                                        2, new Factory[]{
                                                new BuyItemFactory(Items.BLUE_DYE, 10, 12),
                                                new BuyItemFactory(Items.CYAN_DYE, 10, 10),
                                                new BuyItemFactory(Items.GREEN_DYE, 8, 12),
                                                new BuyItemFactory(Items.PURPLE_DYE, 12, 10),
                                                new SellItemFactory(Blocks.ORANGE_WOOL, 8, 4),
                                                new SellItemFactory(Blocks.MAGENTA_WOOL, 8, 4),
                                                new SellItemFactory(Blocks.LIGHT_BLUE_WOOL, 8, 4),
                                                new SellItemFactory(Blocks.YELLOW_WOOL, 8, 4),
                                                new SellItemFactory(Blocks.LIME_WOOL, 8, 4),
                                                new SellItemFactory(Blocks.PINK_WOOL, 8, 4),
                                                new SellItemFactory(Blocks.BROWN_WOOL, 8, 4),
                                                new SellItemFactory(Blocks.RED_WOOL, 8, 4)
                                        },
                                        3, new Factory[]{
                                                new SellItemFactory(Blocks.MAGENTA_WOOL, 8, 8),
                                                new SellItemFactory(Blocks.LIGHT_BLUE_WOOL, 8, 8),
                                                new SellItemFactory(Blocks.LIME_WOOL, 8, 8),
                                                new SellItemFactory(Blocks.CYAN_WOOL, 8, 8),
                                                new SellItemFactory(Blocks.PURPLE_WOOL, 8, 8),
                                                new SellItemFactory(Blocks.BLUE_WOOL, 8, 8),
                                                new SellItemFactory(Blocks.GREEN_WOOL, 8, 8)
                                        },
                                        4, new Factory[]{
                                                new SellItemFactory(Items.MAGENTA_DYE, 6, 12),
                                                new SellItemFactory(Items.PINK_DYE, 8, 12),
                                                new SellItemFactory(Items.RED_DYE, 8, 12),
                                                new SellItemFactory(Items.ORANGE_DYE, 8, 12),
                                                new SellItemFactory(Items.YELLOW_DYE, 8, 12)
                                        },
                                        5, new Factory[]{
                                                new SellItemFactory(Items.PAINTING, 1, 3, 30)
                                        }
                                )
                        )
                );


                // Fletcher is done!
                map.put(
                        VillagerProfession.FLETCHER,
                        copyToFastUtilMap(
                                ImmutableMap.of(
                                        1, new Factory[]{
                                                new BuyItemFactory(Items.FLINT, 16, 2),
                                                new BuyItemFactory(Items.FEATHER, 24, 2),
                                                new SellItemFactory(Items.ARROW, 16, 1),
                                                new SellItemFactory(Items.TARGET, 4, 1),
                                                new SellItemFactory(Items.BOW, 1, 3),
                                                new ProcessItemFactory(Blocks.GRAVEL, 8, 1, Items.ARROW, 32, 12, 1, 0.05F)
                                        },
                                        2, new Factory[]{
                                                new BuyItemFactory(CustomEquipment.COPPER_AXE_WAXED, 1, 3),
                                                new SellItemFactory(Items.CARVED_PUMPKIN, 6, 5),
                                                new SellItemFactory(Items.OAK_WOOD, 8, 4),
                                                TypedWrapperFactory.of(
                                                        new SellItemFactory(Items.DARK_OAK_WOOD, 8, 5),
                                                        VillagerType.SWAMP,
                                                        VillagerType.PLAINS
                                                ),
                                                TypedWrapperFactory.of(
                                                        new SellItemFactory(Items.JUNGLE_WOOD, 8, 5),
                                                        VillagerType.JUNGLE,
                                                        VillagerType.DESERT,
                                                        VillagerType.SAVANNA
                                                ),
                                                TypedWrapperFactory.of(
                                                        new SellItemFactory(Items.ACACIA_WOOD, 8, 5),
                                                        VillagerType.SAVANNA
                                                ),
                                                TypedWrapperFactory.of(
                                                        new SellItemFactory(Items.MANGROVE_WOOD, 8, 5),
                                                        VillagerType.SWAMP,
                                                        VillagerType.SAVANNA
                                                ),
                                                TypedWrapperFactory.of(
                                                        new SellItemFactory(Items.SPRUCE_WOOD, 8, 5),
                                                        VillagerType.SNOW,
                                                        VillagerType.TAIGA,
                                                        VillagerType.PLAINS
                                                ),
                                                TypedWrapperFactory.of(
                                                        new SellItemFactory(Items.BIRCH_WOOD, 8, 5),
                                                        VillagerType.PLAINS
                                                ),
                                        },
                                        3, new Factory[]{
                                                new BuyItemFactory(Items.GUNPOWDER, 6, 5),
                                                new SellItemFactory(Items.CROSSBOW, 3, 1, 12)},
                                        4, new Factory[]{
                                                new SellEnchantedToolFactory(Items.CROSSBOW, 3, 3, 18),
                                                new SellEnchantedToolFactory(Items.BOW, 2, 3, 18)},
                                        5, new Factory[]{
                                                new SellPotionHoldingItemFactory(Items.ARROW, 5, Items.TIPPED_ARROW, 5, 2, 12, 30)
                                        }
                                )
                        )
                );

                // TODO
                map.put(
                        VillagerProfession.CARTOGRAPHER,
                        copyToFastUtilMap(
                                ImmutableMap.of(
                                        1, new Factory[]{new BuyItemFactory(Items.PAPER, 24, 2), new SellItemFactory(Items.MAP, 7, 1, 12, 1, 0.05F)},
                                        2, new Factory[]{
                                                new BuyItemFactory(Items.GLASS_PANE, 11, 10),
                                                TypedWrapperFactory.of(
                                                        new SellMapFactory(8, StructureTags.ON_TAIGA_VILLAGE_MAPS, "filled_map.village_taiga", MapDecorationTypes.VILLAGE_TAIGA, 12, 5),
                                                        VillagerType.SWAMP,
                                                        VillagerType.SNOW,
                                                        VillagerType.PLAINS
                                                ),
                                                TypedWrapperFactory.of(
                                                        new SellMapFactory(8, StructureTags.ON_SWAMP_EXPLORER_MAPS, "filled_map.explorer_swamp", MapDecorationTypes.SWAMP_HUT, 12, 5),
                                                        VillagerType.TAIGA,
                                                        VillagerType.SNOW,
                                                        VillagerType.JUNGLE
                                                ),
                                                TypedWrapperFactory.of(
                                                        new SellMapFactory(8, StructureTags.ON_SNOWY_VILLAGE_MAPS, "filled_map.village_snowy", MapDecorationTypes.VILLAGE_SNOWY, 12, 5),
                                                        VillagerType.TAIGA,
                                                        VillagerType.SWAMP
                                                ),
                                                TypedWrapperFactory.of(
                                                        new SellMapFactory(8, StructureTags.ON_SAVANNA_VILLAGE_MAPS, "filled_map.village_savanna", MapDecorationTypes.VILLAGE_SAVANNA, 12, 5),
                                                        VillagerType.PLAINS,
                                                        VillagerType.JUNGLE,
                                                        VillagerType.DESERT
                                                ),
                                                TypedWrapperFactory.of(
                                                        new SellMapFactory(8, StructureTags.ON_PLAINS_VILLAGE_MAPS, "filled_map.village_plains", MapDecorationTypes.VILLAGE_PLAINS, 12, 5),
                                                        VillagerType.TAIGA,
                                                        VillagerType.SNOW,
                                                        VillagerType.SAVANNA,
                                                        VillagerType.DESERT
                                                ),
                                                TypedWrapperFactory.of(
                                                        new SellMapFactory(8, StructureTags.ON_JUNGLE_EXPLORER_MAPS, "filled_map.explorer_jungle", MapDecorationTypes.JUNGLE_TEMPLE, 12, 5),
                                                        VillagerType.SWAMP,
                                                        VillagerType.SAVANNA,
                                                        VillagerType.DESERT
                                                ),
                                                TypedWrapperFactory.of(
                                                        new SellMapFactory(8, StructureTags.ON_DESERT_VILLAGE_MAPS, "filled_map.village_desert", MapDecorationTypes.VILLAGE_DESERT, 12, 5),
                                                        VillagerType.SAVANNA,
                                                        VillagerType.JUNGLE
                                                )
                                        },
                                        3, new Factory[]{
                                                new BuyItemFactory(Items.COMPASS, 1, 20),
                                                new SellMapFactory(13, StructureTags.ON_OCEAN_EXPLORER_MAPS, "filled_map.monument", MapDecorationTypes.MONUMENT, 12, 10),
                                                new SellMapFactory(12, StructureTags.ON_TRIAL_CHAMBERS_MAPS, "filled_map.trial_chambers", MapDecorationTypes.TRIAL_CHAMBERS, 12, 10)
                                        },
                                        4, new Factory[]{
                                                new SellItemFactory(Items.ITEM_FRAME, 7, 1, 12, 15, 0.05F),
                                                TypedWrapperFactory.of(new SellItemFactory(Items.BLUE_TERRACOTTA, 16, 1), VillagerType.SNOW, VillagerType.TAIGA),
                                                TypedWrapperFactory.of(new SellItemFactory(Items.WHITE_TERRACOTTA, 16, 1), VillagerType.SNOW, VillagerType.PLAINS),
                                                TypedWrapperFactory.of(new SellItemFactory(Items.RED_TERRACOTTA, 16, 1), VillagerType.SNOW, VillagerType.SAVANNA),
                                                TypedWrapperFactory.of(new SellItemFactory(Items.GREEN_TERRACOTTA, 16, 1), VillagerType.DESERT, VillagerType.SAVANNA, VillagerType.JUNGLE),
                                                TypedWrapperFactory.of(new SellItemFactory(Items.LIME_TERRACOTTA, 16, 1), VillagerType.DESERT, VillagerType.TAIGA),
                                                TypedWrapperFactory.of(new SellItemFactory(Items.PURPLE_TERRACOTTA, 16, 1), VillagerType.TAIGA, VillagerType.SWAMP),
                                                TypedWrapperFactory.of(new SellItemFactory(Items.CYAN_TERRACOTTA, 16, 1), VillagerType.DESERT, VillagerType.SNOW),
                                                TypedWrapperFactory.of(new SellItemFactory(Items.YELLOW_TERRACOTTA, 16, 1), VillagerType.PLAINS, VillagerType.JUNGLE),
                                                TypedWrapperFactory.of(new SellItemFactory(Items.ORANGE_TERRACOTTA, 16, 1), VillagerType.SAVANNA, VillagerType.DESERT),
                                                TypedWrapperFactory.of(new SellItemFactory(Items.BROWN_TERRACOTTA, 16, 1), VillagerType.PLAINS, VillagerType.JUNGLE),
                                                TypedWrapperFactory.of(new SellItemFactory(Items.MAGENTA_TERRACOTTA, 16, 1), VillagerType.SAVANNA),
                                                TypedWrapperFactory.of(new SellItemFactory(Items.LIGHT_BLUE_TERRACOTTA, 16, 1), VillagerType.SNOW, VillagerType.SWAMP),
                                                TypedWrapperFactory.of(new SellItemFactory(Items.PINK_TERRACOTTA, 16, 1), VillagerType.TAIGA, VillagerType.PLAINS),
                                                TypedWrapperFactory.of(new SellItemFactory(Items.GRAY_TERRACOTTA, 16, 1), VillagerType.DESERT),
                                                TypedWrapperFactory.of(new SellItemFactory(Items.BLACK_TERRACOTTA, 16, 1), VillagerType.SWAMP)
                                        },
                                        5, new Factory[]{
                                                new SellItemFactory(Items.GLOBE_BANNER_PATTERN, 8, 1, 12, 30, 0.05F),
                                                new SellMapFactory(14, StructureTags.ON_WOODLAND_EXPLORER_MAPS, "filled_map.mansion", MapDecorationTypes.MANSION, 12, 30)
                                        }
                                )
                        )
                );

                // TODO
                map.put(
                        VillagerProfession.WEAPONSMITH,
                        copyToFastUtilMap(
                                ImmutableMap.of(
                                        1, new Factory[]{
                                                new BuyItemFactory(Items.COAL, 15, 2),
                                                new SellItemFactory(new ItemStack(Items.IRON_AXE), 3, 1, 12, 1, 0.2F),
                                                new SellEnchantedToolFactory(Items.IRON_SWORD, 2, 3, 1)
                                        },
                                        2, new Factory[]{
                                                new BuyItemFactory(Items.IRON_INGOT, 4, 10), new SellItemFactory(new ItemStack(Items.BELL), 36, 1, 12, 5, 0.2F)
                                        },
                                        3, new Factory[]{new BuyItemFactory(Items.FLINT, 24, 20)},
                                        4, new Factory[]{
                                                new BuyItemFactory(Items.DIAMOND, 1, 30), new SellEnchantedToolFactory(Items.DIAMOND_AXE, 12, 3, 15, 0.2F)
                                        },
                                        5, new Factory[]{new SellEnchantedToolFactory(Items.DIAMOND_SWORD, 8, 3, 30, 0.2F)}
                                )
                        )
                );
                map.put(
                        VillagerProfession.TOOLSMITH,
                        copyToFastUtilMap(
                                ImmutableMap.of(
                                        1, new Factory[]{
                                                new BuyItemFactory(Items.COAL, 15, 2),
                                                new SellItemFactory(new ItemStack(Items.STONE_AXE), 1, 1, 12, 1, 0.2F),
                                                new SellItemFactory(new ItemStack(Items.STONE_SHOVEL), 1, 1, 12, 1, 0.2F),
                                                new SellItemFactory(new ItemStack(Items.STONE_PICKAXE), 1, 1, 12, 1, 0.2F),
                                                new SellItemFactory(new ItemStack(Items.STONE_HOE), 1, 1, 12, 1, 0.2F)
                                        },
                                        2, new Factory[]{
                                                new BuyItemFactory(Items.IRON_INGOT, 4, 10), new SellItemFactory(new ItemStack(Items.BELL), 36, 1, 12, 5, 0.2F)
                                        },
                                        3, new Factory[]{
                                                new BuyItemFactory(Items.FLINT, 30, 20),
                                                new SellEnchantedToolFactory(Items.IRON_AXE, 1, 3, 10, 0.2F),
                                                new SellEnchantedToolFactory(Items.IRON_SHOVEL, 2, 3, 10, 0.2F),
                                                new SellEnchantedToolFactory(Items.IRON_PICKAXE, 3, 3, 10, 0.2F),
                                                new SellItemFactory(new ItemStack(Items.DIAMOND_HOE), 4, 1, 3, 10, 0.2F)
                                        },
                                        4, new Factory[]{
                                                new BuyItemFactory(Items.DIAMOND, 1, 30),
                                                new SellEnchantedToolFactory(Items.DIAMOND_AXE, 12, 3, 15, 0.2F),
                                                new SellEnchantedToolFactory(Items.DIAMOND_SHOVEL, 5, 3, 15, 0.2F)
                                        },
                                        5, new Factory[]{new SellEnchantedToolFactory(Items.DIAMOND_PICKAXE, 13, 3, 30, 0.2F)}
                                )
                        )
                );
                map.put(
                        VillagerProfession.BUTCHER,
                        copyToFastUtilMap(
                                ImmutableMap.of(
                                        1, new Factory[]{
                                                new BuyItemFactory(Items.CHICKEN, 14, 2),
                                                new BuyItemFactory(Items.PORKCHOP, 7, 2),
                                                new BuyItemFactory(Items.RABBIT, 4, 2),
                                                new SellItemFactory(Items.RABBIT_STEW, 1, 1, 1)
                                        },
                                        2, new Factory[]{
                                                new BuyItemFactory(Items.COAL, 15, 2),
                                                new SellItemFactory(Items.COOKED_PORKCHOP, 5, 5),
                                                new SellItemFactory(Items.COOKED_CHICKEN, 8, 5)
                                        },
                                        3, new Factory[]{new BuyItemFactory(Items.MUTTON, 7, 20), new BuyItemFactory(Items.BEEF, 10, 20)},
                                        4, new Factory[]{new BuyItemFactory(Items.DRIED_KELP_BLOCK, 10, 30)},
                                        5, new Factory[]{new BuyItemFactory(Items.SWEET_BERRIES, 10, 30)}
                                )
                        )
                );
                map.put(
                        VillagerProfession.LEATHERWORKER,
                        copyToFastUtilMap(
                                ImmutableMap.of(
                                        1, new Factory[]{
                                                new BuyItemFactory(Items.LEATHER, 6, 2),
                                                new SellDyedArmorFactory(Items.LEATHER_LEGGINGS, 3),
                                                new SellDyedArmorFactory(Items.LEATHER_CHESTPLATE, 7)
                                        },
                                        2, new Factory[]{
                                                new BuyItemFactory(Items.FLINT, 26, 10),
                                                new SellDyedArmorFactory(Items.LEATHER_HELMET, 5, 12, 5),
                                                new SellDyedArmorFactory(Items.LEATHER_BOOTS, 4, 12, 5)
                                        },
                                        3, new Factory[]{
                                                new BuyItemFactory(Items.RABBIT_HIDE, 9, 20), new SellDyedArmorFactory(Items.LEATHER_CHESTPLATE, 7)
                                        },
                                        4, new Factory[]{
                                                new BuyItemFactory(Items.TURTLE_SCUTE, 4, 30), new SellDyedArmorFactory(Items.LEATHER_HORSE_ARMOR, 6, 12, 15)
                                        },
                                        5, new Factory[]{
                                                new SellItemFactory(new ItemStack(Items.SADDLE), 6, 1, 12, 30, 0.2F), new SellDyedArmorFactory(Items.LEATHER_HELMET, 5, 12, 30)
                                        }
                                )
                        )
                );
            }
    );


    private static final Int2ObjectMap<Factory[]> getFarmerOffers() {
        return copyToFastUtilMap(
                ImmutableMap.of(
                        1, new Factory[]{
                                new BuyItemFactory(Items.BONE_MEAL, 12, 1, 2, 1),
                                new BuyItemFactory(Items.WHEAT, 32, 1), new SellItemFactory(Items.APPLE, 16, 3),
                                new BuyItemFactory(Items.POTATO, 26, 2), new BuyItemFactory(Items.POTATO, 24, 2),
                                new BuyItemFactory(Items.CARROT, 24, 2), new BuyItemFactory(Items.CARROT, 22, 2),
                                new BuyItemFactory(Items.BEETROOT, 18, 2), new BuyItemFactory(Items.BEETROOT, 16, 2),
                                new SellItemFactory(Items.BREAD, 8, 2), new SellItemFactory(Items.BREAD, 9, 2),
                                new SellItemFactory(Items.HONEYCOMB, 12, 2), new SellItemFactory(Items.HONEY_BOTTLE, 4, 2)
                        },
                        2, new Factory[]{
                                new BuyItemFactory(Blocks.PUMPKIN, 12, 4), new BuyItemFactory(Items.COCOA_BEANS, 18, 4),
                                new BuyItemFactory(Items.GLOW_BERRIES, 24, 4), new BuyItemFactory(Items.SWEET_BERRIES, 32, 4),
                                new SellItemFactory(Items.PUMPKIN_PIE, 1, 8, 5),
                                new SellItemFactory(Items.APPLE, 16, 4)
                        },
                        3, new Factory[]{
                                new BuyItemFactory(Items.SUGAR, 22, 3),
                                new BuyItemFactory(Blocks.MELON, 18, 6),
                                new SellItemFactory(Blocks.CAKE, 1, 1, 12, 16),
                                new SellItemFactory(Items.COOKIE, 1, 16, 8),
                                new SellItemFactory(Items.PUMPKIN_PIE, 1, 8, 8)
                        },
                        4, new Factory[]{
                                new SellSusStewFactory(StatusEffects.NIGHT_VISION, 100, 12),
                                new SellSusStewFactory(StatusEffects.JUMP_BOOST, 160, 12), new SellSusStewFactory(StatusEffects.STRENGTH, 140, 12),
                                new SellSusStewFactory(StatusEffects.REGENERATION, 120, 12), new SellSusStewFactory(StatusEffects.SLOW_FALLING, 280, 12),
                                new SellSusStewFactory(StatusEffects.LEVITATION, 120, 12), new SellSusStewFactory(StatusEffects.RESISTANCE, 120, 12),
                                new SellSusStewFactory(StatusEffects.SATURATION, 7, 12), new SellSusStewFactory(StatusEffects.SATURATION, 7, 12),
                                new SellSusStewFactory(CustomStatusEffects.BUOYANCY, 280, 12), new SellSusStewFactory(CustomStatusEffects.SMALLNESS, 280, 12),
                        },
                        5, new Factory[]{
                                new SellItemFactory(Items.HONEYCOMB, 24, 8),
                                new SellItemFactory(Items.GOLDEN_CARROT, 1, 1, 15),
                                new SellItemFactory(Items.GLISTERING_MELON_SLICE, 1, 1, 15)
                        }
                )
        );
    }
    
    private static final Int2ObjectMap<Factory[]> getFishermanOffers() {
        return copyToFastUtilMap(
                ImmutableMap.of(
                        1, new Factory[]{
                                new BuyItemFactory(Items.SALMON, 14, 4),
                                new BuyItemFactory(Items.COD, 15, 4),
                                new SellItemFactory(Items.CAMPFIRE, 1, 5),
                                new SellItemFactory(Items.COD_BUCKET, 3, 1, 16, 1),
                                new SellEnchantedToolFactory(Items.FISHING_ROD, 3, 3, 8, 0.2F)
                        },
                        2, new Factory[]{
                                new BuyItemFactory(Items.INK_SAC, 8, 6),
                                new BuyItemFactory(Items.LILY_PAD, 8, 8),
                                new SellItemFactory(Items.CAMPFIRE, 2, 1, 5),
                                new SellItemFactory(Items.DRIED_KELP_BLOCK, 1, 16, 5)
                        },
                        3, new Factory[]{
                                new BuyItemFactory(Items.PUFFERFISH, 4, 10),
                                new BuyItemFactory(Items.TROPICAL_FISH, 6, 8),
                                new BuyItemFactory(Items.NAUTILUS_SHELL, 1, 20),
                                new SellItemFactory(Items.MOSS_BLOCK, 18, 7),
                                new SellItemFactory(Items.MOSS_BLOCK, 16, 7)
                        },
                        4, new Factory[]{
                                new SellEnchantedToolFactory(Items.FISHING_ROD, 6, 3, 12, 0.2F),
                                new SellItemFactory(createPotionStack(Potions.WATER_BREATHING), 6, 1,  3, 12),
                                new SellItemFactory(createPotionStack(Potions.LONG_WATER_BREATHING), 8, 1,  3, 14),
                                new SellItemFactory(createPotionStack(CustomPotions.BUOYANCY), 3, 1,  3, 10),
                                new SellItemFactory(createPotionStack(CustomPotions.BUOYANCY_STRONG), 3, 1,  3, 12)
                        },
                        5, new Factory[]{
                                new SellEnchantedToolFactory(Items.FISHING_ROD, 12, 3, 20, 0.2F)
                        }
                )
        );
    }

    private static final Int2ObjectMap<Factory[]> getMasonOffers() {
        return  copyToFastUtilMap(ImmutableMap.of(
                1, new Factory[]{
                        new BuyItemFactory(Items.QUARTZ, 16, 16, 3),
                        new BuyItemFactory(Items.CLAY_BALL, 18, 16, 3),
                        new BuyItemFactory(Items.CLAY_BALL, 16, 16, 3),
                        new SellItemFactory(CustomBlocks.POLISHED_STONE, 32, 2),
                        new SellItemFactory(Blocks.SMOOTH_STONE, 16, 3),
                        new SellItemFactory(Blocks.STONE_BRICKS, 32, 2),
                        new SellItemFactory(Blocks.MOSSY_STONE_BRICKS, 32, 2)
                },
                2, new Factory[]{
                        new BuyItemFactory(CustomBlockItems.BROWN_CLAY_BALL, 16, 16, 3),
                        new BuyItemFactory(CustomBlockItems.BROWN_CLAY_BALL, 16, 16, 3),
                        new BuyItemFactory(CustomBlockItems.BROWN_CLAY_BALL, 16, 16, 3),
                        new SellItemFactory(CustomBlocks.BROWN_CLAY_BRICKS, 8, 3),
                        new SellItemFactory(CustomBlocks.BROWN_CLAY_BRICKS, 9, 3),
                        new SellItemFactory(Blocks.BRICKS, 22, 3),
                        new SellItemFactory(Blocks.BRICKS, 24, 3),
                        new SellItemFactory(Items.BRICK, 64, 2),
                        new SellItemFactory(Blocks.LIGHT_GRAY_TERRACOTTA, 22, 3),
                        new SellItemFactory(Blocks.TERRACOTTA, 24, 3),
                        new SellItemFactory(Blocks.TERRACOTTA, 26, 3),
                        new SellItemFactory(CustomBlocks.TERRACOTTA_BRICKS, 16, 3),
                        new SellItemFactory(CustomBlocks.TERRACOTTA_TILES, 18, 3),
                                TypedWrapperFactory.of(new SellItemFactory(Items.BLUE_TERRACOTTA, 16, 3), VillagerType.SNOW, VillagerType.TAIGA),
                                TypedWrapperFactory.of(new SellItemFactory(Items.WHITE_TERRACOTTA, 16, 3), VillagerType.SNOW, VillagerType.PLAINS),
                                TypedWrapperFactory.of(new SellItemFactory(Items.RED_TERRACOTTA, 16, 3), VillagerType.SNOW, VillagerType.SAVANNA),
                                TypedWrapperFactory.of(new SellItemFactory(Items.GREEN_TERRACOTTA, 16, 3), VillagerType.DESERT, VillagerType.SAVANNA, VillagerType.JUNGLE),
                                TypedWrapperFactory.of(new SellItemFactory(Items.LIME_TERRACOTTA, 16, 3), VillagerType.DESERT, VillagerType.TAIGA),
                                TypedWrapperFactory.of(new SellItemFactory(Items.PURPLE_TERRACOTTA, 16, 3), VillagerType.TAIGA, VillagerType.SWAMP),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CYAN_TERRACOTTA, 16, 3), VillagerType.DESERT, VillagerType.SNOW),
                                TypedWrapperFactory.of(new SellItemFactory(Items.YELLOW_TERRACOTTA, 16, 3), VillagerType.PLAINS, VillagerType.JUNGLE),
                                TypedWrapperFactory.of(new SellItemFactory(Items.ORANGE_TERRACOTTA, 16, 3), VillagerType.SAVANNA, VillagerType.DESERT),
                                TypedWrapperFactory.of(new SellItemFactory(Items.BROWN_TERRACOTTA, 16, 3), VillagerType.PLAINS, VillagerType.JUNGLE),
                                TypedWrapperFactory.of(new SellItemFactory(Items.MAGENTA_TERRACOTTA, 16, 3), VillagerType.SAVANNA),
                                TypedWrapperFactory.of(new SellItemFactory(Items.LIGHT_BLUE_TERRACOTTA, 16, 3), VillagerType.SNOW, VillagerType.SWAMP),
                                TypedWrapperFactory.of(new SellItemFactory(Items.PINK_TERRACOTTA, 16, 3), VillagerType.TAIGA, VillagerType.PLAINS),
                                TypedWrapperFactory.of(new SellItemFactory(Items.GRAY_TERRACOTTA, 16, 3), VillagerType.DESERT),
                                TypedWrapperFactory.of(new SellItemFactory(Items.BLACK_TERRACOTTA, 16, 3), VillagerType.SWAMP)},
                3, new Factory[]{
                        new SellItemFactory(Blocks.TUFF, 36, 4),
                        new SellItemFactory(Blocks.POLISHED_TUFF, 32, 4),
                        new SellItemFactory(Blocks.POLISHED_TUFF, 32, 4),
                        new SellItemFactory(Blocks.CHISELED_TUFF, 28, 4),
                        new SellItemFactory(Blocks.CHISELED_TUFF_BRICKS, 24, 4),
                        new SellItemFactory(Blocks.TUFF_BRICKS, 32, 4),
                        new SellItemFactory(Blocks.TUFF_BRICKS, 32, 4)},
                4, new Factory[]{
                        new SellItemFactory(Blocks.CUT_COPPER, 18, 5),
                        new SellItemFactory(Blocks.WAXED_CUT_COPPER, 12, 5),
                        new SellItemFactory(Blocks.WEATHERED_CUT_COPPER, 8, 6),
                        new SellItemFactory(Blocks.WEATHERED_CUT_COPPER, 8, 6)},
                5, new Factory[]{
                        new SellItemFactory(Blocks.CALCITE, 10, 7),
                        new SellItemFactory(Blocks.SMOOTH_QUARTZ, 16, 7),
                        new SellItemFactory(Blocks.QUARTZ_PILLAR, 24, 7)}
        ));
    }

    private static final Int2ObjectMap<Factory[]> getClericOffers() {
        return copyToFastUtilMap(
                ImmutableMap.of(
                1, new Factory[]{
                        new BuyItemFactory(Items.GOLDEN_APPLE, 1, 8, 6, 5),
                        new BuyItemFactory(Items.SPIDER_EYE, 8, 8, 3),
                        new BuyItemFactory(Items.ROTTEN_FLESH, 8, 8, 1),
                        new BuyItemFactory(Items.GOLD_INGOT, 3, 24, 2),
                        new SellItemFactory(Items.GLASS_BOTTLE, 1, 8, 1),
                        new SellItemFactory(Items.SUGAR, 1, 24, 1),
                        new SellItemFactory(Items.REDSTONE, 8, 1),
                        new SellItemFactory(Items.GUNPOWDER, 1, 3, 1)
                        },
                2, new Factory[]{
                        new BuyItemFactory(Items.BREEZE_ROD, 3, 16, 6),
                        new BuyItemFactory(Items.BLAZE_ROD, 1, 16, 8),
                        new BuyItemFactory(Items.GLOWSTONE_DUST, 16, 8, 4),
                        new BuyItemFactory(Blocks.NETHER_WART, 21, 16, 4)},
                3, new Factory[]{
                        new SellItemFactory(createPotionStack(Potions.WATER_BREATHING), 3, 1, 6, 6),
                        new SellItemFactory(createSplashPotionStack(Potions.WEAKNESS), 15, 1, 6,6),
                        new SellItemFactory(createPotionStack(Potions.HEALING), 4, 1, 6,6),
                        new SellItemFactory(createPotionStack(Potions.SWIFTNESS), 5, 1, 6,6),
                        new SellItemFactory(Items.MAGMA_CREAM, 1, 1, 8, 1),
                        new ProcessItemFactory(Items.CARROT, 16, 2, Items.GOLDEN_CARROT, 16, 12, 1, 1)},
                4, new Factory[]{
                        new SellItemFactory(createSplashPotionStack(Potions.OOZING), 12, 1, 6,9),
                        new SellItemFactory(createSplashPotionStack(Potions.WIND_CHARGED), 14, 1, 6,9),
                        new SellItemFactory(createPotionStack(Potions.LEAPING), 8, 1, 6,9),
                        new SellItemFactory(createPotionStack(Potions.LONG_INVISIBILITY), 24, 1, 6,9),
                        new SellItemFactory(createPotionStack(Potions.LONG_SLOW_FALLING), 38, 1, 6,9),
                        new SellItemFactory(createPotionStack(Potions.REGENERATION), 12, 1, 6,9),
                        new SellItemFactory(createSplashPotionStack(Potions.STRONG_HEALING), 10, 1, 6,9)},
                5, new Factory[]{
                        new SellItemFactory(createPotionStack(Potions.LONG_STRENGTH), 21, 1, 6,15),
                        new SellItemFactory(createPotionStack(Potions.LONG_SWIFTNESS), 21, 1, 6,15),
                        new SellItemFactory(createPotionStack(Potions.STRONG_TURTLE_MASTER), 44, 1, 6,15)}
                ));
    }

    private static final Int2ObjectMap<Factory[]> getLibrarianOffers() {
        return copyToFastUtilMap(
                ImmutableMap.<Integer, Factory[]>builder()
                        .put(1,
                                new Factory[]{
                                        new BuyItemFactory(Items.WRITABLE_BOOK, 1, 15),
                                        new BuyItemFactory(Items.PAPER, 24, 3),
                                        new BuyItemFactory(Items.PAPER, 22, 3),
                                        new BuyItemFactory(Items.BOOK, 16, 4),
                                        new BuyItemFactory(Items.BOOK, 14, 4),
                                        new SellItemFactory(Blocks.BOOKSHELF, 3, 1, 12, 12),
                                        new SellItemFactory(Blocks.CHISELED_BOOKSHELF, 3, 5),
                                        new EnchantBookFactory(1, EnchantmentTags.IN_ENCHANTING_TABLE),
                                        new EnchantBookFactory(5, EnchantmentTags.MINING_EXCLUSIVE_SET),
                                        createLibrarianTradeFactory(5)
                                }
                        )
                        .put(2,
                                new Factory[]{
                                        new EnchantBookFactory(5, EnchantmentTags.IN_ENCHANTING_TABLE),
                                        new EnchantBookFactory(10, EnchantmentTags.TRADEABLE),
                                        new BuyItemFactory(Items.LAPIS_LAZULI, 6, 4),
                                        new SellItemFactory(Items.LANTERN, 1, 8, 5),
                                        new SellItemFactory(Items.SOUL_LANTERN, 1, 6, 5),
                                        new SellItemFactory(Items.COPPER_LANTERNS.weathered(), 1, 6, 5),
                                        new SellItemFactory(Items.NAME_TAG, 6, 1, 12)
                                }
                        )
                        .put(3,
                                new Factory[]{
                                        new BuyItemFactory(Items.LAPIS_LAZULI, 5, 5),
                                        new BuyItemFactory(Items.AMETHYST_SHARD, 12, 6),
                                        new BuyItemFactory(Items.EXPERIENCE_BOTTLE, 1, 6),
                                        new SellItemFactory(Items.GLASS, 16, 6),
                                        new SellItemFactory(Items.TINTED_GLASS, 12, 8),
                                        new EnchantBookFactory(10, EnchantmentTags.TRADEABLE)
                                }
                        )
                        .put(4,
                                new Factory[]{
                                        new BuyItemFactory(Items.ECHO_SHARD, 1, 5),
                                        new SellItemFactory(Items.OBSIDIAN, 4, 4),
                                        createMasterLibrarianTradeFactory()
                                }
                        )
                        .put(5,
                                new Factory[]{
                                        new EnchantBookFactory(18, EnchantmentTags.TRADEABLE),
                                        new EnchantBookFactory(16, EnchantmentTags.BOOTS_EXCLUSIVE_SET), // Mending or infinity
                                }
                        )
                        .build()
        );
    }

    private static final Int2ObjectMap<Factory[]> getArmorerOffers() {
        return copyToFastUtilMap(
                ImmutableMap.of(
                        1, new Factory[]{
                                new BuyItemFactory(Items.COAL, 22, 16, 2),
                                new BuyItemFactory(Items.RAW_IRON, 15, 16, 2),
                                new SellItemFactory(Items.BUCKET, 1, 2, 12, 1, 0.2F),
                                new SellItemFactory(Items.BELL, 36, 1, 12, 8, 0.2F)},
                        2, new Factory[]{
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_BOOTS, 4, 1, 12, 2, 0.05F), VillagerType.DESERT, VillagerType.PLAINS, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.TAIGA),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_BOOTS, 4, 1, 12, 2, 0.05F), VillagerType.JUNGLE, VillagerType.SWAMP),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_HELMET, 5, 1, 12, 2, 0.05F), VillagerType.DESERT, VillagerType.PLAINS, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.TAIGA),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_HELMET, 5, 1, 12, 2, 0.05F), VillagerType.JUNGLE, VillagerType.SWAMP),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_LEGGINGS, 7, 1, 12, 2, 0.05F), VillagerType.DESERT, VillagerType.PLAINS, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.TAIGA),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_LEGGINGS, 7, 1, 12, 2, 0.05F), VillagerType.JUNGLE, VillagerType.SWAMP),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_CHESTPLATE, 9, 1, 12, 2, 0.05F), VillagerType.DESERT, VillagerType.PLAINS, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.TAIGA),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_CHESTPLATE, 9, 1, 12, 2, 0.05F), VillagerType.JUNGLE, VillagerType.SWAMP)},
                        3, new Factory[]{
                                new BuyItemFactory(Items.DIAMOND, 1, 8, 15, 14),
                                new BuyItemFactory(Items.LAVA_BUCKET, 1, 1, 10),
                                new SellItemFactory(Items.SHIELD, 5, 1, 12, 1),
                                new SellItemFactory(Items.BELL, 36, 1, 12, 5, 0.2F)},
                        4, new Factory[]{
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_BOOTS, 8, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.DESERT_ARMORER_BOOTS_4), VillagerType.DESERT),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_HELMET, 9, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.DESERT_ARMORER_HELMET_4), VillagerType.DESERT),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_LEGGINGS, 11, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.DESERT_ARMORER_LEGGINGS_4), VillagerType.DESERT),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_CHESTPLATE, 13, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.DESERT_ARMORER_CHESTPLATE_4), VillagerType.DESERT),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_BOOTS, 8, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.PLAINS_ARMORER_BOOTS_4), VillagerType.PLAINS),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_HELMET, 9, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.PLAINS_ARMORER_HELMET_4), VillagerType.PLAINS),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_LEGGINGS, 11, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.PLAINS_ARMORER_LEGGINGS_4), VillagerType.PLAINS),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_CHESTPLATE, 13, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.PLAINS_ARMORER_CHESTPLATE_4), VillagerType.PLAINS),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_BOOTS, 2, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.SAVANNA_ARMORER_BOOTS_4), VillagerType.SAVANNA),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_HELMET, 3, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.SAVANNA_ARMORER_HELMET_4), VillagerType.SAVANNA),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_LEGGINGS, 5, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.SAVANNA_ARMORER_LEGGINGS_4), VillagerType.SAVANNA),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_CHESTPLATE, 7, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.SAVANNA_ARMORER_CHESTPLATE_4), VillagerType.SAVANNA),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_BOOTS, 8, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.SNOW_ARMORER_BOOTS_4), VillagerType.SNOW),
                                TypedWrapperFactory.of(new SellItemFactory(Items.IRON_HELMET, 9, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.SNOW_ARMORER_HELMET_4), VillagerType.SNOW),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_BOOTS, 8, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.JUNGLE_ARMORER_BOOTS_4), VillagerType.JUNGLE),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_HELMET, 9, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.JUNGLE_ARMORER_HELMET_4), VillagerType.JUNGLE),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_LEGGINGS, 11, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.JUNGLE_ARMORER_LEGGINGS_4), VillagerType.JUNGLE),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_CHESTPLATE, 13, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.JUNGLE_ARMORER_CHESTPLATE_4), VillagerType.JUNGLE),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_BOOTS, 8, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.SWAMP_ARMORER_BOOTS_4), VillagerType.SWAMP),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_HELMET, 9, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.SWAMP_ARMORER_HELMET_4), VillagerType.SWAMP),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_LEGGINGS, 11, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.SWAMP_ARMORER_LEGGINGS_4), VillagerType.SWAMP),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_CHESTPLATE, 13, 1, 3, 6, 0.05F, TradeRebalanceEnchantmentProviders.SWAMP_ARMORER_CHESTPLATE_4), VillagerType.SWAMP),
                                TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND_BOOTS, 1, 4, Items.DIAMOND_LEGGINGS, 1, 3, 6, 0.05F), VillagerType.TAIGA),
                                TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND_LEGGINGS, 1, 4, Items.DIAMOND_CHESTPLATE, 1, 3, 6, 0.05F), VillagerType.TAIGA),
                                TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND_HELMET, 1, 4, Items.DIAMOND_BOOTS, 1, 3, 6, 0.05F), VillagerType.TAIGA),
                                TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND_CHESTPLATE, 1, 2, Items.DIAMOND_HELMET, 1, 3, 6, 0.05F), VillagerType.TAIGA)},
                        5, new Factory[]{
                                TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 4, 16, Items.DIAMOND_CHESTPLATE, 1, 3, 12, 0.05F, TradeRebalanceEnchantmentProviders.DESERT_ARMORER_CHESTPLATE_5), VillagerType.DESERT),
                                TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 3, 16, Items.DIAMOND_LEGGINGS, 1, 3, 12, 0.05F, TradeRebalanceEnchantmentProviders.DESERT_ARMORER_LEGGINGS_5), VillagerType.DESERT),
                                TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 3, 16, Items.DIAMOND_LEGGINGS, 1, 3, 12, 0.05F, TradeRebalanceEnchantmentProviders.PLAINS_ARMORER_LEGGINGS_5), VillagerType.PLAINS),
                                TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 2, 12, Items.DIAMOND_BOOTS, 1, 3, 12, 0.05F, TradeRebalanceEnchantmentProviders.PLAINS_ARMORER_BOOTS_5), VillagerType.PLAINS),
                                TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 2, 6, Items.DIAMOND_HELMET, 1, 3, 12, 0.05F, TradeRebalanceEnchantmentProviders.SAVANNA_ARMORER_HELMET_5), VillagerType.SAVANNA),
                                TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 3, 8, Items.DIAMOND_CHESTPLATE, 1, 3, 12, 0.05F, TradeRebalanceEnchantmentProviders.SAVANNA_ARMORER_CHESTPLATE_5), VillagerType.SAVANNA),
                                TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 2, 12, Items.DIAMOND_BOOTS, 1, 3, 12, 0.05F, TradeRebalanceEnchantmentProviders.SNOW_ARMORER_BOOTS_5), VillagerType.SNOW),
                                TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 3, 12, Items.DIAMOND_HELMET, 1, 3, 12, 0.05F, TradeRebalanceEnchantmentProviders.SNOW_ARMORER_HELMET_5), VillagerType.SNOW),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_HELMET, 9, 1, 3, 12, 0.05F, TradeRebalanceEnchantmentProviders.JUNGLE_ARMORER_HELMET_5), VillagerType.JUNGLE),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_BOOTS, 8, 1, 3, 12, 0.05F, TradeRebalanceEnchantmentProviders.JUNGLE_ARMORER_BOOTS_5), VillagerType.JUNGLE),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_HELMET, 9, 1, 3, 12, 0.05F, TradeRebalanceEnchantmentProviders.SWAMP_ARMORER_HELMET_5), VillagerType.SWAMP),
                                TypedWrapperFactory.of(new SellItemFactory(Items.CHAINMAIL_BOOTS, 8, 1, 3, 12, 0.05F, TradeRebalanceEnchantmentProviders.SWAMP_ARMORER_BOOTS_5), VillagerType.SWAMP),
                                TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 4, 18, Items.DIAMOND_CHESTPLATE, 1, 3, 12, 0.05F, TradeRebalanceEnchantmentProviders.TAIGA_ARMORER_CHESTPLATE_5), VillagerType.TAIGA),
                                TypedWrapperFactory.of(new ProcessItemFactory(Items.DIAMOND, 3, 18, Items.DIAMOND_LEGGINGS, 1, 3, 12, 0.05F, TradeRebalanceEnchantmentProviders.TAIGA_ARMORER_LEGGINGS_5), VillagerType.TAIGA),
                                TypedWrapperFactory.of(new BuyItemFactory(Items.GOLD_BLOCK, 1, 12, 8, 5), VillagerType.TAIGA),
                                TypedWrapperFactory.of(new BuyItemFactory(Items.IRON_BLOCK, 1, 12, 8, 4), VillagerType.DESERT, VillagerType.JUNGLE, VillagerType.PLAINS, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.SWAMP)}
                ));
    }
}
