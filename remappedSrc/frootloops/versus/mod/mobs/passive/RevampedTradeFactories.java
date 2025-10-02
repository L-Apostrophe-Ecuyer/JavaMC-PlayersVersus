package frootloops.versus.mod.mobs.passive;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import frootloops.versus.mod.items_and_effects.brewing.CustomPotions;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.SuspiciousStewEffectsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.provider.EnchantmentProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.*;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.item.map.MapState;
import net.minecraft.potion.Potion;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.*;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RevampedTradeFactories {

    public static Factory createLibrarianTradeFactory(int experience) {
        return new TypedWrapperFactory(
                ImmutableMap.<RegistryKey<VillagerType>, Factory>builder()
                        .put(VillagerType.DESERT, new EnchantBookFactory(experience, EnchantmentTags.DESERT_COMMON_TRADE))
                        .put(VillagerType.JUNGLE, new EnchantBookFactory(experience, EnchantmentTags.JUNGLE_COMMON_TRADE))
                        .put(VillagerType.PLAINS, new EnchantBookFactory(experience, EnchantmentTags.PLAINS_COMMON_TRADE))
                        .put(VillagerType.SAVANNA, new EnchantBookFactory(experience, EnchantmentTags.SAVANNA_COMMON_TRADE))
                        .put(VillagerType.SNOW, new EnchantBookFactory(experience, EnchantmentTags.SNOW_COMMON_TRADE))
                        .put(VillagerType.SWAMP, new EnchantBookFactory(experience, EnchantmentTags.SWAMP_COMMON_TRADE))
                        .put(VillagerType.TAIGA, new EnchantBookFactory(experience, EnchantmentTags.TAIGA_COMMON_TRADE))
                        .build()
        );
    }

    public static Factory createMasterLibrarianTradeFactory() {
        return new TypedWrapperFactory(
                ImmutableMap.<RegistryKey<VillagerType>, Factory>builder()
                        .put(VillagerType.DESERT, new EnchantBookFactory(30, 3, 3, EnchantmentTags.DESERT_SPECIAL_TRADE))
                        .put(VillagerType.JUNGLE, new EnchantBookFactory(30, 2, 2, EnchantmentTags.JUNGLE_SPECIAL_TRADE))
                        .put(VillagerType.PLAINS, new EnchantBookFactory(30, 3, 3, EnchantmentTags.PLAINS_SPECIAL_TRADE))
                        .put(VillagerType.SAVANNA, new EnchantBookFactory(30, 3, 3, EnchantmentTags.SAVANNA_SPECIAL_TRADE))
                        .put(VillagerType.SNOW, new EnchantBookFactory(30, EnchantmentTags.SNOW_SPECIAL_TRADE))
                        .put(VillagerType.SWAMP, new EnchantBookFactory(30, EnchantmentTags.SWAMP_SPECIAL_TRADE))
                        .put(VillagerType.TAIGA, new EnchantBookFactory(30, 2, 2, EnchantmentTags.TAIGA_SPECIAL_TRADE))
                        .build()
        );
    }

    public static Int2ObjectMap<Factory[]> copyToFastUtilMap(ImmutableMap<Integer, Factory[]> map) {
        return new Int2ObjectOpenHashMap<>(map);
    }

    public static TradedItem createPotion(RegistryEntry<Potion> potion) {
        return new TradedItem(Items.POTION).withComponents(builder -> builder.add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(potion)));
    }

    public static ItemStack createPotionStack(RegistryEntry<Potion> potion) {
        return PotionContentsComponent.createStack(Items.POTION, potion);
    }

    public static class BuyItemFactory implements Factory {
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
            this.multiplier = 0.05F;
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            return new TradeOffer(this.stack, new ItemStack(Items.EMERALD, this.price), this.maxUses, this.experience, this.multiplier);
        }
    }

    static class EmptyFactory implements Factory {
        private EmptyFactory() {
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            return null;
        }
    }

    public static class EnchantBookFactory implements Factory {
        private final int experience;
        private final TagKey<Enchantment> possibleEnchantments;
        private final int minLevel;
        private final int maxLevel;

        public EnchantBookFactory(int experience, TagKey<Enchantment> possibleEnchantments) {
            this(experience, 0, Integer.MAX_VALUE, possibleEnchantments);
        }

        public EnchantBookFactory(int experience, int minLevel, int maxLevel, TagKey<Enchantment> possibleEnchantments) {
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.experience = experience;
            this.possibleEnchantments = possibleEnchantments;
        }

        @Override
        public TradeOffer create(Entity entity, Random random) {
            Optional<RegistryEntry<Enchantment>> optional = entity.getWorld()
                    .getRegistryManager()
                    .getOrThrow(RegistryKeys.ENCHANTMENT)
                    .getRandomEntry(this.possibleEnchantments, random);
            int l;
            ItemStack itemStack;
            if (!optional.isEmpty()) {
                RegistryEntry<Enchantment> registryEntry = (RegistryEntry<Enchantment>)optional.get();
                Enchantment enchantment = registryEntry.value();
                int i = Math.max(enchantment.getMinLevel(), this.minLevel);
                int j = Math.min(enchantment.getMaxLevel(), this.maxLevel);
                int k = MathHelper.nextInt(random, i, j);
                itemStack = EnchantmentHelper.getEnchantedBookWith(new EnchantmentLevelEntry(registryEntry, k));
                l = 2 + random.nextInt(5 + k * 10) + 3 * k;
                if (registryEntry.isIn(EnchantmentTags.DOUBLE_TRADE_PRICE)) {
                    l *= 2;
                }

                if (l > 64) {
                    l = 64;
                }
            } else {
                l = 1;
                itemStack = new ItemStack(Items.BOOK);
            }

            return new TradeOffer(new TradedItem(Items.EMERALD, l), Optional.of(new TradedItem(Items.BOOK)), itemStack, 12, this.experience, 0.2F);
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
        TradeOffer create(Entity entity, Random random);
    }

    public static class ProcessItemFactory implements Factory {
        private final TradedItem toBeProcessed;
        private final int price;
        private final ItemStack processed;
        private final int maxUses;
        private final int experience;
        private final float multiplier;
        private final Optional<RegistryKey<EnchantmentProvider>> enchantmentProviderKey;

        public ProcessItemFactory(ItemConvertible item, int count, int price, Item processed, int processedCount, int maxUses, int experience, float multiplier) {
            this(item, count, price, new ItemStack(processed), processedCount, maxUses, experience, multiplier);
        }

        private ProcessItemFactory(ItemConvertible item, int count, int price, ItemStack processed, int processedCount, int maxUses, int experience, float multiplier) {
            this(new TradedItem(item, count), price, processed.copyWithCount(processedCount), maxUses, experience, multiplier, Optional.empty());
        }

        ProcessItemFactory(
                ItemConvertible item,
                int count,
                int price,
                ItemConvertible processed,
                int processedCount,
                int maxUses,
                int experience,
                float multiplier,
                RegistryKey<EnchantmentProvider> enchantmentProviderKey
        ) {
            this(new TradedItem(item, count), price, new ItemStack(processed, processedCount), maxUses, experience, multiplier, Optional.of(enchantmentProviderKey));
        }

        public ProcessItemFactory(
                TradedItem toBeProcessed,
                int count,
                ItemStack processed,
                int maxUses,
                int processedCount,
                float multiplier,
                Optional<RegistryKey<EnchantmentProvider>> enchantmentProviderKey
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
        public TradeOffer create(Entity entity, Random random) {
            ItemStack itemStack = this.processed.copy();
            World world = entity.getWorld();
            this.enchantmentProviderKey
                    .ifPresent(
                            key -> EnchantmentHelper.applyEnchantmentProvider(itemStack, world.getRegistryManager(), key, world.getLocalDifficulty(entity.getBlockPos()), random)
                    );
            return new TradeOffer(
                    new TradedItem(Items.EMERALD, this.price), Optional.of(this.toBeProcessed), itemStack, 0, this.maxUses, this.experience, this.multiplier
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
        public TradeOffer create(Entity entity, Random random) {
            TradedItem tradedItem = new TradedItem(Items.EMERALD, this.price);
            ItemStack itemStack = new ItemStack(this.sell);
            if (itemStack.isIn(ItemTags.DYEABLE)) {
                List<DyeItem> list = Lists.<DyeItem>newArrayList();
                list.add(getDye(random));
                if (random.nextFloat() > 0.7F) {
                    list.add(getDye(random));
                }

                if (random.nextFloat() > 0.8F) {
                    list.add(getDye(random));
                }

                itemStack = DyedColorComponent.setColor(itemStack, list);
            }

            return new TradeOffer(tradedItem, itemStack, this.maxUses, this.experience, 0.2F);
        }

        private static DyeItem getDye(Random random) {
            return DyeItem.byColor(DyeColor.byIndex(random.nextInt(16)));
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
        public TradeOffer create(Entity entity, Random random) {
            int i = 5 + random.nextInt(15);
            DynamicRegistryManager dynamicRegistryManager = entity.getWorld().getRegistryManager();
            Optional<RegistryEntryList.Named<Enchantment>> optional = dynamicRegistryManager.getOrThrow(RegistryKeys.ENCHANTMENT)
                    .getOptional(EnchantmentTags.ON_TRADED_EQUIPMENT);
            ItemStack itemStack = EnchantmentHelper.enchant(random, new ItemStack(this.tool.getItem()), i, dynamicRegistryManager, optional);
            int j = Math.min(this.basePrice + i, 64);
            TradedItem tradedItem = new TradedItem(Items.EMERALD, j);
            return new TradeOffer(tradedItem, itemStack, this.maxUses, this.experience, this.multiplier);
        }
    }

    public static class SellItemFactory implements Factory {
        private final ItemStack sell;
        private final int price;
        private final int maxUses;
        private final int experience;
        private final float multiplier;
        private final Optional<RegistryKey<EnchantmentProvider>> enchantmentProviderKey;

        public SellItemFactory(Block block, int price, int count, int maxUses, int experience) {
            this(new ItemStack(block), price, count, maxUses, experience);
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
                Item item, int price, int count, int maxUses, int experience, float multiplier, RegistryKey<EnchantmentProvider> enchantmentProviderKey
        ) {
            this(new ItemStack(item), price, count, maxUses, experience, multiplier, Optional.of(enchantmentProviderKey));
        }

        public SellItemFactory(ItemStack stack, int price, int count, int maxUses, int experience, float multiplier) {
            this(stack, price, count, maxUses, experience, multiplier, Optional.empty());
        }

        public SellItemFactory(
                ItemStack sell, int price, int count, int maxUses, int experience, float multiplier, Optional<RegistryKey<EnchantmentProvider>> enchantmentProviderKey
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
        public TradeOffer create(Entity entity, Random random) {
            ItemStack itemStack = this.sell.copy();
            World world = entity.getWorld();
            this.enchantmentProviderKey
                    .ifPresent(
                            key -> EnchantmentHelper.applyEnchantmentProvider(itemStack, world.getRegistryManager(), key, world.getLocalDifficulty(entity.getBlockPos()), random)
                    );
            return new TradeOffer(new TradedItem(Items.EMERALD, this.price), itemStack, this.maxUses, this.experience, this.multiplier);
        }
    }

    public static class SellMapFactory implements Factory {
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

        @Nullable
        @Override
        public TradeOffer create(Entity entity, Random random) {
            if (entity.getWorld() instanceof ServerWorld serverWorld) {
                BlockPos blockPos = serverWorld.locateStructure(this.structure, entity.getBlockPos(), 100, true);
                if (blockPos != null) {
                    ItemStack itemStack = FilledMapItem.createMap(serverWorld, blockPos.getX(), blockPos.getZ(), (byte)2, true, true);
                    FilledMapItem.fillExplorationMap(serverWorld, itemStack);
                    MapState.addDecorationsNbt(itemStack, blockPos, "+", this.decoration);
                    itemStack.set(DataComponentTypes.ITEM_NAME, Text.translatable(this.nameKey));
                    return new TradeOffer(
                            new TradedItem(Items.EMERALD, this.price), Optional.of(new TradedItem(Items.COMPASS)), itemStack, this.maxUses, this.experience, 0.2F
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
        public TradeOffer create(Entity entity, Random random) {
            TradedItem tradedItem = new TradedItem(Items.EMERALD, this.price);
            List<RegistryEntry<Potion>> list = Registries.POTION.streamEntries()
                    .filter(entry -> !(entry.value()).getEffects().isEmpty() && entity.getWorld().getBrewingRecipeRegistry().isBrewable(entry) && entry.value() != CustomPotions.HAUNTING.value())
                    .collect(Collectors.toList());
            RegistryEntry<Potion> registryEntry = Util.getRandom(list, random);
            ItemStack itemStack = new ItemStack(this.sell.getItem(), this.sellCount);
            itemStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(registryEntry));
            return new TradeOffer(
                    tradedItem, Optional.of(new TradedItem(this.secondBuy, this.secondCount)), itemStack, this.maxUses, this.experience, this.priceMultiplier
            );
        }
    }

    public static class SellSuspiciousStewFactory implements Factory {
        private final SuspiciousStewEffectsComponent stewEffects;
        private final int experience;
        private final float multiplier;

        public SellSuspiciousStewFactory(RegistryEntry<StatusEffect> effect, int duration, int experience) {
            this(new SuspiciousStewEffectsComponent(List.of(new SuspiciousStewEffectsComponent.StewEffect(effect, duration))), experience, 0.05F);
        }

        public SellSuspiciousStewFactory(SuspiciousStewEffectsComponent stewEffects, int experience, float multiplier) {
            this.stewEffects = stewEffects;
            this.experience = experience;
            this.multiplier = multiplier;
        }

        @Nullable
        @Override
        public TradeOffer create(Entity entity, Random random) {
            ItemStack itemStack = new ItemStack(Items.SUSPICIOUS_STEW, 1);
            itemStack.set(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, this.stewEffects);
            return new TradeOffer(new TradedItem(Items.EMERALD), itemStack, 12, this.experience, this.multiplier);
        }
    }

    public static class TypeAwareBuyForOneEmeraldFactory implements Factory {
        private final Map<RegistryKey<VillagerType>, Item> map;
        private final int count;
        private final int maxUses;
        private final int experience;

        public TypeAwareBuyForOneEmeraldFactory(int count, int maxUses, int experience, Map<RegistryKey<VillagerType>, Item> map) {
            Registries.VILLAGER_TYPE.getKeys().stream().filter(typeKey -> !map.containsKey(typeKey)).findAny().ifPresent(typeKey -> {
                throw new IllegalStateException("Missing trade for villager type: " + typeKey);
            });
            this.map = map;
            this.count = count;
            this.maxUses = maxUses;
            this.experience = experience;
        }

        @Nullable
        @Override
        public TradeOffer create(Entity entity, Random random) {
            if (entity instanceof VillagerDataContainer villagerDataContainer) {
                RegistryKey<VillagerType> registryKey = (RegistryKey<VillagerType>)villagerDataContainer.getVillagerData().type().getKey().orElse(null);
                if (registryKey == null) {
                    return null;
                } else {
                    TradedItem tradedItem = new TradedItem((ItemConvertible)this.map.get(registryKey), this.count);
                    return new TradeOffer(tradedItem, new ItemStack(Items.EMERALD), this.maxUses, this.experience, 0.05F);
                }
            } else {
                return null;
            }
        }
    }

    public record TypedWrapperFactory(Map<RegistryKey<VillagerType>, Factory> typeToFactory) implements Factory {
        @SafeVarargs
        public static TypedWrapperFactory of(Factory factory, RegistryKey<VillagerType>... types) {
            return new TypedWrapperFactory(
                    (Map<RegistryKey<VillagerType>, Factory>) Arrays.stream(types).collect(Collectors.toMap(registryKey -> registryKey, registryKey -> factory))
            );
        }

        @Nullable
        @Override
        public TradeOffer create(Entity entity, Random random) {
            if (entity instanceof VillagerDataContainer villagerDataContainer) {
                RegistryKey<VillagerType> registryKey = (RegistryKey<VillagerType>)villagerDataContainer.getVillagerData().type().getKey().orElse(null);
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
