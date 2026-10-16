package frootloops.versus.mod.mobs.passive;

import com.google.common.collect.ImmutableList;
import frootloops.versus.mod.items_and_effects.CustomBrewingItems;
import frootloops.versus.mod.mobs.passive.RevampedTradeFactories.BuyItemFactory;
import frootloops.versus.mod.mobs.passive.RevampedTradeFactories.Factory;
import frootloops.versus.mod.mobs.passive.RevampedTradeFactories.SellEnchantedToolFactory;
import frootloops.versus.mod.mobs.passive.RevampedTradeFactories.SellItemFactory;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;

import static frootloops.versus.mod.mobs.passive.RevampedTradeFactories.*;

public class RevampedWandererOffers {

    public static final List<Pair<Factory[], Integer>> WANDERING_TRADER_TRADES = ImmutableList.<Pair<Factory[], Integer>>builder()
            .add(
                    Pair.of(
                            new Factory[]{
                                    new RevampedTradeFactories.BuyItemFactory(createPotion(Potions.WATER), 2, 1, 1),
                                    new BuyItemFactory(Items.WATER_BUCKET, 1, 2, 1, 2),
                                    new BuyItemFactory(Items.MILK_BUCKET, 1, 2, 1, 2),
                                    new BuyItemFactory(CustomBrewingItems.CORRUPTED_WART_POWDER, 1, 2, 1, 3), // Fermented spider eyes' replacement
                                    new BuyItemFactory(Items.BAKED_POTATO, 4, 2, 1),
                                    new BuyItemFactory(Items.HAY_BLOCK, 1, 2, 1)
                            },
                            2
                    )
            )
            .add(
                    Pair.of(
                            new Factory[]{
                                    new SellItemFactory(Items.PACKED_ICE, 1, 1, 6, 1),
                                    new SellItemFactory(Items.BLUE_ICE, 6, 1, 6, 1),
                                    new SellItemFactory(Items.GUNPOWDER, 1, 4, 2, 1),
                                    new SellItemFactory(Items.PODZOL, 3, 3, 6, 1),
                                    new SellItemFactory(Blocks.ACACIA_LOG, 1, 8, 4, 1),
                                    new SellItemFactory(Blocks.BIRCH_LOG, 1, 8, 4, 1),
                                    new SellItemFactory(Blocks.DARK_OAK_LOG, 1, 8, 4, 1),
                                    new SellItemFactory(Blocks.JUNGLE_LOG, 1, 8, 4, 1),
                                    new SellItemFactory(Blocks.OAK_LOG, 1, 8, 4, 1),
                                    new SellItemFactory(Blocks.SPRUCE_LOG, 1, 8, 4, 1),
                                    new SellItemFactory(Blocks.CHERRY_LOG, 1, 8, 4, 1),
                                    new SellItemFactory(Blocks.MANGROVE_LOG, 1, 8, 4, 1),
                                    new SellItemFactory(Blocks.PALE_OAK_LOG, 1, 8, 4, 1),
                                    new SellEnchantedToolFactory(Items.IRON_PICKAXE, 1, 1, 1, 0.2F),
                                    new SellItemFactory(createPotionStack(Potions.LONG_INVISIBILITY), 5, 1, 1, 1)
                            },
                            2
                    )
            )
            .add(
                    Pair.of(
                            new Factory[]{
                                    new SellItemFactory(Items.TROPICAL_FISH_BUCKET, 3, 1, 4, 1),
                                    new SellItemFactory(Items.PUFFERFISH_BUCKET, 3, 1, 4, 1),
                                    new SellItemFactory(Items.SEA_PICKLE, 2, 1, 5, 1),
                                    new SellItemFactory(Items.SLIME_BALL, 4, 1, 5, 1),
                                    new SellItemFactory(Items.GLOWSTONE, 2, 1, 5, 1),
                                    new SellItemFactory(Items.NAUTILUS_SHELL, 5, 1, 5, 1),
                                    new SellItemFactory(Items.FERN, 1, 1, 12, 1),
                                    new SellItemFactory(Items.SUGAR_CANE, 1, 1, 8, 1),
                                    new SellItemFactory(Items.PUMPKIN, 1, 1, 4, 1),
                                    new SellItemFactory(Items.KELP, 3, 1, 12, 1),
                                    new SellItemFactory(Items.CACTUS, 3, 1, 8, 1),
                                    new SellItemFactory(Items.DANDELION, 1, 1, 12, 1),
                                    new SellItemFactory(Items.POPPY, 1, 1, 12, 1),
                                    new SellItemFactory(Items.BLUE_ORCHID, 1, 1, 8, 1),
                                    new SellItemFactory(Items.ALLIUM, 1, 1, 12, 1),
                                    new SellItemFactory(Items.AZURE_BLUET, 1, 1, 12, 1),
                                    new SellItemFactory(Items.RED_TULIP, 1, 1, 12, 1),
                                    new SellItemFactory(Items.ORANGE_TULIP, 1, 1, 12, 1),
                                    new SellItemFactory(Items.WHITE_TULIP, 1, 1, 12, 1),
                                    new SellItemFactory(Items.PINK_TULIP, 1, 1, 12, 1),
                                    new SellItemFactory(Items.OXEYE_DAISY, 1, 1, 12, 1),
                                    new SellItemFactory(Items.CORNFLOWER, 1, 1, 12, 1),
                                    new SellItemFactory(Items.LILY_OF_THE_VALLEY, 1, 1, 7, 1),
                                    new SellItemFactory(Items.OPEN_EYEBLOSSOM, 1, 1, 7, 1),
                                    new SellItemFactory(Items.WHEAT_SEEDS, 1, 1, 12, 1),
                                    new SellItemFactory(Items.BEETROOT_SEEDS, 1, 1, 12, 1),
                                    new SellItemFactory(Items.PUMPKIN_SEEDS, 1, 1, 12, 1),
                                    new SellItemFactory(Items.MELON_SEEDS, 1, 1, 12, 1),
                                    new SellItemFactory(Items.ACACIA_SAPLING, 5, 1, 8, 1),
                                    new SellItemFactory(Items.BIRCH_SAPLING, 5, 1, 8, 1),
                                    new SellItemFactory(Items.DARK_OAK_SAPLING, 5, 1, 8, 1),
                                    new SellItemFactory(Items.JUNGLE_SAPLING, 5, 1, 8, 1),
                                    new SellItemFactory(Items.OAK_SAPLING, 5, 1, 8, 1),
                                    new SellItemFactory(Items.SPRUCE_SAPLING, 5, 1, 8, 1),
                                    new SellItemFactory(Items.CHERRY_SAPLING, 5, 1, 8, 1),
                                    new SellItemFactory(Items.PALE_OAK_SAPLING, 5, 1, 8, 1),
                                    new SellItemFactory(Items.MANGROVE_PROPAGULE, 5, 1, 8, 1),
                                    new SellItemFactory(Items.DYE.red(), 1, 3, 12, 1),
                                    new SellItemFactory(Items.DYE.white(), 1, 3, 12, 1),
                                    new SellItemFactory(Items.DYE.blue(), 1, 3, 12, 1),
                                    new SellItemFactory(Items.DYE.pink(), 1, 3, 12, 1),
                                    new SellItemFactory(Items.DYE.black(), 1, 3, 12, 1),
                                    new SellItemFactory(Items.DYE.green(), 1, 3, 12, 1),
                                    new SellItemFactory(Items.DYE.lightGray(), 1, 3, 12, 1),
                                    new SellItemFactory(Items.DYE.magenta(), 1, 3, 12, 1),
                                    new SellItemFactory(Items.DYE.yellow(), 1, 3, 12, 1),
                                    new SellItemFactory(Items.DYE.gray(), 1, 3, 12, 1),
                                    new SellItemFactory(Items.DYE.purple(), 1, 3, 12, 1),
                                    new SellItemFactory(Items.DYE.lightBlue(), 1, 3, 12, 1),
                                    new SellItemFactory(Items.DYE.lime(), 1, 3, 12, 1),
                                    new SellItemFactory(Items.DYE.orange(), 1, 3, 12, 1),
                                    new SellItemFactory(Items.DYE.brown(), 1, 3, 12, 1),
                                    new SellItemFactory(Items.DYE.cyan(), 1, 3, 12, 1),
                                    new SellItemFactory(Items.BRAIN_CORAL_BLOCK, 3, 1, 8, 1),
                                    new SellItemFactory(Items.BUBBLE_CORAL_BLOCK, 3, 1, 8, 1),
                                    new SellItemFactory(Items.FIRE_CORAL_BLOCK, 3, 1, 8, 1),
                                    new SellItemFactory(Items.HORN_CORAL_BLOCK, 3, 1, 8, 1),
                                    new SellItemFactory(Items.TUBE_CORAL_BLOCK, 3, 1, 8, 1),
                                    new SellItemFactory(Items.VINE, 1, 3, 4, 1),
                                    new SellItemFactory(Items.PALE_HANGING_MOSS, 1, 3, 4, 1),
                                    new SellItemFactory(Items.BROWN_MUSHROOM, 1, 3, 4, 1),
                                    new SellItemFactory(Items.RED_MUSHROOM, 1, 3, 4, 1),
                                    new SellItemFactory(Items.LILY_PAD, 1, 5, 2, 1),
                                    new SellItemFactory(Items.SMALL_DRIPLEAF, 1, 2, 5, 1),
                                    new SellItemFactory(Items.SAND, 1, 8, 8, 1),
                                    new SellItemFactory(Items.RED_SAND, 1, 4, 6, 1),
                                    new SellItemFactory(Items.POINTED_DRIPSTONE, 1, 2, 5, 1),
                                    new SellItemFactory(Items.ROOTED_DIRT, 1, 2, 5, 1),
                                    new SellItemFactory(Items.MOSS_BLOCK, 1, 2, 5, 1),
                                    new SellItemFactory(Items.PALE_MOSS_BLOCK, 1, 2, 5, 1),
                                    new SellItemFactory(Items.WILDFLOWERS, 1, 1, 12, 1),
                                    new SellItemFactory(Items.DRY_TALL_GRASS, 1, 1, 12, 1),
                                    new SellItemFactory(Items.FIREFLY_BUSH, 3, 1, 12, 1)
                            },
                            5
                    )
            )
            .build();
}
