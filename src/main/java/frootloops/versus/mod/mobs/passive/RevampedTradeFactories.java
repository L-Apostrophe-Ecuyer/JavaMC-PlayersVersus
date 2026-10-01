package frootloops.versus.mod.mobs.passive;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import frootloops.versus.mod.items_and_effects.brewing.CustomPotions;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.VillagerDataHolder;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RevampedTradeFactories {

    public static Factory createLibrarianTradeFactory(int experience) {
        return new TypedWrapperFactory(
                ImmutableMap.<ResourceKey<VillagerType>, Factory>builder()
                        .put(VillagerType.DESERT, new EnchantBookFactory(experience, EnchantmentTags.TRADES_DESERT_COMMON))
                        .put(VillagerType.JUNGLE, new EnchantBookFactory(experience, EnchantmentTags.TRADES_JUNGLE_COMMON))
                        .put(VillagerType.PLAINS, new EnchantBookFactory(experience, EnchantmentTags.TRADES_PLAINS_COMMON))
                        .put(VillagerType.SAVANNA, new EnchantBookFactory(experience, EnchantmentTags.TRADES_SAVANNA_COMMON))
                        .put(VillagerType.SNOW, new EnchantBookFactory(experience, EnchantmentTags.TRADES_SNOW_COMMON))
                        .put(VillagerType.SWAMP, new EnchantBookFactory(experience, EnchantmentTags.TRADES_SWAMP_COMMON))
                        .put(VillagerType.TAIGA, new EnchantBookFactory(experience, EnchantmentTags.TRADES_TAIGA_COMMON))
                        .build()
        );
    }

    public static Factory createMasterLibrarianTradeFactory() {
        return new TypedWrapperFactory(
                ImmutableMap.<ResourceKey<VillagerType>, Factory>builder()
                        .put(VillagerType.DESERT, new EnchantBookFactory(30, 3, 3, TradeKeys.TRADES_DESERT_SPECIAL))
                        .put(VillagerType.JUNGLE, new EnchantBookFactory(30, 2, 2, TradeKeys.TRADES_JUNGLE_SPECIAL))
                        .put(VillagerType.PLAINS, new EnchantBookFactory(30, 3, 3, TradeKeys.TRADES_PLAINS_SPECIAL))
                        .put(VillagerType.SAVANNA, new EnchantBookFactory(30, 3, 3, TradeKeys.TRADES_SAVANNA_SPECIAL))
                        .put(VillagerType.SNOW, new EnchantBookFactory(30, TradeKeys.TRADES_SNOW_SPECIAL))
                        .put(VillagerType.SWAMP, new EnchantBookFactory(30, TradeKeys.TRADES_SWAMP_SPECIAL))
                        .put(VillagerType.TAIGA, new EnchantBookFactory(30, 2, 2, TradeKeys.TRADES_TAIGA_SPECIAL))
                        .build()
        );
    }

    public static Int2ObjectMap<Factory[]> copyToFastUtilMap(ImmutableMap<Integer, Factory[]> map) {
        return new Int2ObjectOpenHashMap<>(map);
    }

    public static ItemCost createPotion(Holder<Potion> potion) {
        return new ItemCost(Items.POTION).withComponents(builder -> builder.expect(DataComponents.POTION_CONTENTS, new PotionContents(potion)));
    }

    public static ItemStack createPotionStack(Holder<Potion> potion) {
        return PotionContents.createItemStack(Items.POTION, potion);
    }

    public static ItemStack createSplashPotionStack(Holder<Potion> potion) {
        return PotionContents.createItemStack(Items.SPLASH_POTION, potion);
    }

    public static class BuyItemFactory implements Factory {
        private final ItemCost stack;
        private final int maxUses;
        private final int experience;
        private final int price;
        private final float multiplier;

        public BuyItemFactory(ItemLike item, int count, int experience) {
            this(item, count, 12, experience, 1);
        }


        public BuyItemFactory(ItemLike item, int count, int maxUses, int experience) {
            this(item, count, maxUses, experience, 1);
        }

        public BuyItemFactory(ItemLike item, int count, int maxUses, int experience, int price) {
            this(new ItemCost(item.asItem(), count), maxUses, experience, price);
        }

        public BuyItemFactory(ItemCost stack, int maxUses, int experience, int price) {
            this.stack = stack;
            this.maxUses = maxUses;
            this.experience = experience;
            this.price = price;
            this.multiplier = 0.05F;
        }

        @Override
        public MerchantOffer create(Entity entity, RandomSource random) {
            return new MerchantOffer(this.stack, new ItemStack(Items.EMERALD, this.price), this.maxUses, this.experience, this.multiplier);
        }
    }

    static class EmptyFactory implements Factory {
        private EmptyFactory() {
        }

        @Override
        public MerchantOffer create(Entity entity, RandomSource random) {
            return null;
        }
    }

    public static class EnchantBookFactory implements Factory {
        private final int experience;
        private final TagKey<Enchantment> possibleEnchantments;
        private final int minLevel;
        private final int maxLevel;

        public EnchantBookFactory(int experience, TagKey<Enchantment> possibleEnchantments) {
            this(experience, 0, 4 + 2 * experience, possibleEnchantments);
        }

        public EnchantBookFactory(int experience, int minLevel, int maxLevel, TagKey<Enchantment> possibleEnchantments) {
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.experience = experience;
            this.possibleEnchantments = possibleEnchantments;
        }

        @Override
        public MerchantOffer create(Entity entity, RandomSource random) {
            Optional<Holder<Enchantment>> optional = entity.level()
                    .registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getRandomElementOf(this.possibleEnchantments, random);
            int l;
            ItemStack itemStack;
            if (!optional.isEmpty()) {
                Holder<Enchantment> registryEntry = (Holder<Enchantment>)optional.get();
                Enchantment enchantment = registryEntry.value();
                int i = Math.max(enchantment.getMinLevel(), this.minLevel);
                int j = Math.min(enchantment.getMaxLevel(), this.maxLevel);
                int k = Mth.nextInt(random, i, j);
                itemStack = EnchantmentHelper.createBook(new EnchantmentInstance(registryEntry, k));
                l = 2 + random.nextInt(5 + k * 10) + 3 * k;
                if (registryEntry.is(EnchantmentTags.DOUBLE_TRADE_PRICE)) {
                    l *= 2;
                }

                if (l > 64) {
                    l = 64;
                }
            } else {
                l = 1;
                itemStack = new ItemStack(Items.BOOK);
            }

            return new MerchantOffer(new ItemCost(Items.EMERALD, l), Optional.of(new ItemCost(Items.BOOK)), itemStack, 12, this.experience, 0.2F);
        }
    }

    /**
     * A factory to create trade offers.
     */
    public interface Factory {
        /**
         * Creates a trade offer.
         *
         * @return a new trade offer, or {@code null} if none should be created
         */
        @Nullable
        MerchantOffer create(Entity entity, RandomSource random);
    }

    public static class ProcessItemFactory implements Factory {
        private final ItemCost toBeProcessed;
        private final int price;
        private final ItemStack processed;
        private final int maxUses;
        private final int experience;
        private final float multiplier;
        private final Optional<ResourceKey<EnchantmentProvider>> enchantmentProviderKey;

        public ProcessItemFactory(ItemLike item, int count, int price, Item processed, int processedCount, int maxUses, int experience, float multiplier) {
            this(item, count, price, new ItemStack(processed), processedCount, maxUses, experience, multiplier);
        }

        private ProcessItemFactory(ItemLike item, int count, int price, ItemStack processed, int processedCount, int maxUses, int experience, float multiplier) {
            this(new ItemCost(item, count), price, processed.copyWithCount(processedCount), maxUses, experience, multiplier, Optional.empty());
        }

        ProcessItemFactory(
                ItemLike item,
                int count,
                int price,
                ItemLike processed,
                int processedCount,
                int maxUses,
                int experience,
                float multiplier,
                ResourceKey<EnchantmentProvider> enchantmentProviderKey
        ) {
            this(new ItemCost(item, count), price, new ItemStack(processed, processedCount), maxUses, experience, multiplier, Optional.of(enchantmentProviderKey));
        }

        public ProcessItemFactory(
                ItemCost toBeProcessed,
                int count,
                ItemStack processed,
                int maxUses,
                int processedCount,
                float multiplier,
                Optional<ResourceKey<EnchantmentProvider>> enchantmentProviderKey
        ) {
            this.toBeProcessed = toBeProcessed;
            this.price = count;
            this.processed = processed;
            this.maxUses = maxUses;
            this.experience = processedCount;
            this.multiplier = multiplier;
            this.enchantmentProviderKey = enchantmentProviderKey;
        }

        @Nullable
        @Override
        public MerchantOffer create(Entity entity, RandomSource random) {
            ItemStack itemStack = this.processed.copy();
            if (!(entity.level() instanceof ServerLevel world)) return null;
            this.enchantmentProviderKey
                    .ifPresent(
                            key -> EnchantmentHelper.enchantItemFromProvider(itemStack, world.registryAccess(), key, world.getCurrentDifficultyAt(entity.blockPosition()), random)
                    );
            return new MerchantOffer(
                    new ItemCost(Items.EMERALD, this.price), Optional.of(this.toBeProcessed), itemStack, 0, this.maxUses, this.experience, this.multiplier
            );
        }
    }

    public static class SellDyedArmorFactory implements Factory {
        private final Item sell;
        private final int price;
        private final int maxUses;
        private final int experience;

        public SellDyedArmorFactory(Item item, int price) {
            this(item, price, 12, 1);
        }

        public SellDyedArmorFactory(Item item, int price, int maxUses, int experience) {
            this.sell = item;
            this.price = price;
            this.maxUses = maxUses;
            this.experience = experience;
        }

        @Override
        public MerchantOffer create(Entity entity, RandomSource random) {
            ItemCost tradedItem = new ItemCost(Items.EMERALD, this.price);
            ItemStack itemStack = new ItemStack(this.sell);
            if (itemStack.has(DataComponents.DYED_COLOR)) {
                List<DyeColor> list = Lists.<DyeColor>newArrayList();
                list.add(getDye(random));
                if (random.nextFloat() > 0.7F) {
                    list.add(getDye(random));
                }

                if (random.nextFloat() > 0.8F) {
                    list.add(getDye(random));
                }

                itemStack = DyedItemColor.applyDyes(itemStack, list);
            }

            return new MerchantOffer(tradedItem, itemStack, this.maxUses, this.experience, 0.2F);
        }

        private static DyeColor getDye(RandomSource random) {
            return DyeColor.byId(random.nextInt(16));
        }
    }

    public static class SellEnchantedToolFactory implements Factory {
        private final ItemStack tool;
        private final int basePrice;
        private final int maxUses;
        private final int experience;
        private final float multiplier;

        public SellEnchantedToolFactory(Item item, int basePrice, int maxUses, int experience) {
            this(item, basePrice, maxUses, experience, 0.05F);
        }

        public SellEnchantedToolFactory(Item item, int basePrice, int maxUses, int experience, float multiplier) {
            this.tool = new ItemStack(item);
            this.basePrice = basePrice;
            this.maxUses = maxUses;
            this.experience = experience;
            this.multiplier = multiplier;
        }

        @Override
        public MerchantOffer create(Entity entity, RandomSource random) {
            int enchantLevels = 2 + random.nextInt(this.experience);
            RegistryAccess dynamicRegistryManager = entity.level().registryAccess();
            Optional<HolderSet.Named<Enchantment>> optional = dynamicRegistryManager.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.ON_TRADED_EQUIPMENT);
            ItemStack itemStack = EnchantmentHelper.enchantItem(random, new ItemStack(this.tool.getItem()), enchantLevels, dynamicRegistryManager, optional);
            int j = Math.min(this.basePrice + enchantLevels, 64);
            ItemCost tradedItem = new ItemCost(Items.EMERALD, j);
            return new MerchantOffer(tradedItem, itemStack, this.maxUses, this.experience, this.multiplier);
        }
    }

    public static class SellItemFactory implements Factory {
        private final ItemStack sell;
        private final int price;
        private final int maxUses;
        private final int experience;
        private final float multiplier;
        private final Optional<ResourceKey<EnchantmentProvider>> enchantmentProviderKey;

        public SellItemFactory(Block block, int price, int count, int maxUses, int experience) {
            this(new ItemStack(block), price, count, maxUses, experience);
        }

        public SellItemFactory(Block block, int count, int experience) {
            this(new ItemStack(block), 1, count, 16, experience);
        }

        public SellItemFactory(Item item, int count, int experience) {
            this(new ItemStack(item), 1, count, 12, experience);
        }

        public SellItemFactory(Item item, int price, int count, int experience) {
            this(new ItemStack(item), price, count, 12, experience);
        }

        public SellItemFactory(Item item, int price, int count, int maxUses, int experience) {
            this(new ItemStack(item), price, count, maxUses, experience);
        }

        public SellItemFactory(ItemStack stack, int price, int count, int maxUses, int experience) {
            this(stack, price, count, maxUses, experience, 0.05F);
        }

        public SellItemFactory(Item item, int price, int count, int maxUses, int experience, float multiplier) {
            this(new ItemStack(item), price, count, maxUses, experience, multiplier);
        }

        public SellItemFactory(
                Item item, int price, int count, int maxUses, int experience, float multiplier, ResourceKey<EnchantmentProvider> enchantmentProviderKey
        ) {
            this(new ItemStack(item), price, count, maxUses, experience, multiplier, Optional.of(enchantmentProviderKey));
        }

        public SellItemFactory(ItemStack stack, int price, int count, int maxUses, int experience, float multiplier) {
            this(stack, price, count, maxUses, experience, multiplier, Optional.empty());
        }

        public SellItemFactory(
                ItemStack sell, int price, int count, int maxUses, int experience, float multiplier, Optional<ResourceKey<EnchantmentProvider>> enchantmentProviderKey
        ) {
            this.sell = sell;
            this.price = price;
            this.sell.setCount(count);
            this.maxUses = maxUses;
            this.experience = experience;
            this.multiplier = multiplier;
            this.enchantmentProviderKey = enchantmentProviderKey;
        }

        @Override
        public MerchantOffer create(Entity entity, RandomSource random) {
            ItemStack itemStack = this.sell.copy();
            if (!(entity.level() instanceof ServerLevel world)) return null;
            this.enchantmentProviderKey
                    .ifPresent(
                            key -> EnchantmentHelper.enchantItemFromProvider(itemStack, world.registryAccess(), key, world.getCurrentDifficultyAt(entity.blockPosition()), random)
                    );
            return new MerchantOffer(new ItemCost(Items.EMERALD, this.price), itemStack, this.maxUses, this.experience, this.multiplier);
        }
    }

    public static class SellMapFactory implements Factory {
        private final int price;
        private final TagKey<Structure> structure;
        private final String nameKey;
        private final Holder<MapDecorationType> decoration;
        private final int maxUses;
        private final int experience;

        public SellMapFactory(int price, TagKey<Structure> structure, String nameKey, Holder<MapDecorationType> decoration, int maxUses, int experience) {
            this.price = price;
            this.structure = structure;
            this.nameKey = nameKey;
            this.decoration = decoration;
            this.maxUses = maxUses;
            this.experience = experience;
        }

        @Nullable
        @Override
        public MerchantOffer create(Entity entity, RandomSource random) {
            if (entity.level() instanceof ServerLevel serverWorld) {
                BlockPos blockPos = serverWorld.findNearestMapStructure(this.structure, entity.blockPosition(), 100, true);
                if (blockPos != null) {
                    ItemStack itemStack = MapItem.create(serverWorld, blockPos.getX(), blockPos.getZ(), (byte)2, true, true);
                    MapItem.renderBiomePreviewMap(serverWorld, itemStack);
                    MapItemSavedData.addTargetDecoration(itemStack, blockPos, "+", this.decoration);
                    itemStack.set(DataComponents.ITEM_NAME, Component.translatable(this.nameKey));
                    return new MerchantOffer(
                            new ItemCost(Items.EMERALD, this.price), Optional.of(new ItemCost(Items.COMPASS)), itemStack, this.maxUses, this.experience, 0.2F
                    );
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    public static class SellPotionHoldingItemFactory implements Factory {
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
            this.priceMultiplier = 0.05F;
        }

        @Override
        public MerchantOffer create(Entity entity, RandomSource random) {
            ItemCost tradedItem = new ItemCost(Items.EMERALD, this.price);
            List<Holder<Potion>> list = BuiltInRegistries.POTION.listElements()
                    .filter(entry -> !(entry.value()).getEffects().isEmpty() && !entry.value().getEffects().getFirst().getEffect().value().isBeneficial() && entry.value() != CustomPotions.HAUNTING.value())
                    .collect(Collectors.toList());
            Holder<Potion> registryEntry = Util.getRandom(list, random);
            ItemStack itemStack = new ItemStack(this.sell.getItem(), this.sellCount);
            itemStack.set(DataComponents.POTION_CONTENTS, new PotionContents(registryEntry));
            return new MerchantOffer(
                    tradedItem, Optional.of(new ItemCost(this.secondBuy, this.secondCount)), itemStack, this.maxUses, this.experience, this.priceMultiplier
            );
        }
    }

    public static class SellSusStewFactory implements Factory {
        private final SuspiciousStewEffects stewEffects;
        private final int experience;
        private final float multiplier;

        public SellSusStewFactory(Holder<MobEffect> effect, int duration, int experience) {
            this(new SuspiciousStewEffects(List.of(new SuspiciousStewEffects.Entry(effect, duration))), experience, 0.05F);
        }

        public SellSusStewFactory(SuspiciousStewEffects stewEffects, int experience, float multiplier) {
            this.stewEffects = stewEffects;
            this.experience = experience;
            this.multiplier = multiplier;
        }

        @Nullable
        @Override
        public MerchantOffer create(Entity entity, RandomSource random) {
            ItemStack itemStack = new ItemStack(Items.SUSPICIOUS_STEW, 1);
            itemStack.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, this.stewEffects);
            return new MerchantOffer(new ItemCost(Items.EMERALD), itemStack, 12, this.experience, this.multiplier);
        }
    }

    public static class TypeAwareBuyForOneEmeraldFactory implements Factory {
        private final Map<ResourceKey<VillagerType>, Item> map;
        private final int count;
        private final int maxUses;
        private final int experience;

        public TypeAwareBuyForOneEmeraldFactory(int count, int maxUses, int experience, Map<ResourceKey<VillagerType>, Item> map) {
            BuiltInRegistries.VILLAGER_TYPE.registryKeySet().stream().filter(typeKey -> !map.containsKey(typeKey)).findAny().ifPresent(typeKey -> {
                throw new IllegalStateException("Missing trade for villager type: " + typeKey);
            });
            this.map = map;
            this.count = count;
            this.maxUses = maxUses;
            this.experience = experience;
        }

        @Nullable
        @Override
        public MerchantOffer create(Entity entity, RandomSource random) {
            if (entity instanceof VillagerDataHolder villagerDataContainer) {
                ResourceKey<VillagerType> registryKey = (ResourceKey<VillagerType>)villagerDataContainer.getVillagerData().type().unwrapKey().orElse(null);
                if (registryKey == null) {
                    return null;
                } else {
                    ItemCost tradedItem = new ItemCost((ItemLike)this.map.get(registryKey), this.count);
                    return new MerchantOffer(tradedItem, new ItemStack(Items.EMERALD), this.maxUses, this.experience, 0.05F);
                }
            } else {
                return null;
            }
        }
    }

    public record TypedWrapperFactory(Map<ResourceKey<VillagerType>, Factory> typeToFactory) implements Factory {
        @SafeVarargs
        public static TypedWrapperFactory of(Factory factory, ResourceKey<VillagerType>... types) {
            return new TypedWrapperFactory(
                    (Map<ResourceKey<VillagerType>, Factory>) Arrays.stream(types).collect(Collectors.toMap(registryKey -> registryKey, registryKey -> factory))
            );
        }

        @Nullable
        @Override
        public MerchantOffer create(Entity entity, RandomSource random) {
            if (entity instanceof VillagerDataHolder villagerDataContainer) {
                ResourceKey<VillagerType> registryKey = (ResourceKey<VillagerType>)villagerDataContainer.getVillagerData().type().unwrapKey().orElse(null);
                if (registryKey == null) {
                    return null;
                } else {
                    Factory factory = (Factory)this.typeToFactory.get(registryKey);
                    return factory == null ? null : factory.create(entity, random);
                }
            } else {
                return null;
            }
        }
    }
}
