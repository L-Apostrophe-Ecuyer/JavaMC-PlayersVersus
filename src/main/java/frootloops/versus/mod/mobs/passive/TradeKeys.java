package frootloops.versus.mod.mobs.passive;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;

/**
 * The keys of the trade enchantments Players Versus ships as data under {@code data/minecraft}: the enchantment
 * providers of armor and tool trades ({@code enchantment_provider/trades}) and the special enchantments of the
 * librarians' biome books ({@code tags/enchantment/trades}). 26.3 no longer has vanilla constants for them.
 */
public final class TradeKeys {

    public static final ResourceKey<EnchantmentProvider> TRADES_DESERT_ARMORER_BOOTS_4 = provider("desert_armorer_boots_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_DESERT_ARMORER_CHESTPLATE_4 = provider("desert_armorer_chestplate_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_DESERT_ARMORER_CHESTPLATE_5 = provider("desert_armorer_chestplate_5");
    public static final ResourceKey<EnchantmentProvider> TRADES_DESERT_ARMORER_HELMET_4 = provider("desert_armorer_helmet_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_DESERT_ARMORER_LEGGINGS_4 = provider("desert_armorer_leggings_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_DESERT_ARMORER_LEGGINGS_5 = provider("desert_armorer_leggings_5");
    public static final ResourceKey<EnchantmentProvider> TRADES_JUNGLE_ARMORER_BOOTS_4 = provider("jungle_armorer_boots_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_JUNGLE_ARMORER_BOOTS_5 = provider("jungle_armorer_boots_5");
    public static final ResourceKey<EnchantmentProvider> TRADES_JUNGLE_ARMORER_CHESTPLATE_4 = provider("jungle_armorer_chestplate_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_JUNGLE_ARMORER_HELMET_4 = provider("jungle_armorer_helmet_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_JUNGLE_ARMORER_HELMET_5 = provider("jungle_armorer_helmet_5");
    public static final ResourceKey<EnchantmentProvider> TRADES_JUNGLE_ARMORER_LEGGINGS_4 = provider("jungle_armorer_leggings_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_PLAINS_ARMORER_BOOTS_4 = provider("plains_armorer_boots_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_PLAINS_ARMORER_BOOTS_5 = provider("plains_armorer_boots_5");
    public static final ResourceKey<EnchantmentProvider> TRADES_PLAINS_ARMORER_CHESTPLATE_4 = provider("plains_armorer_chestplate_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_PLAINS_ARMORER_HELMET_4 = provider("plains_armorer_helmet_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_PLAINS_ARMORER_LEGGINGS_4 = provider("plains_armorer_leggings_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_PLAINS_ARMORER_LEGGINGS_5 = provider("plains_armorer_leggings_5");
    public static final ResourceKey<EnchantmentProvider> TRADES_SAVANNA_ARMORER_BOOTS_4 = provider("savanna_armorer_boots_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_SAVANNA_ARMORER_CHESTPLATE_4 = provider("savanna_armorer_chestplate_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_SAVANNA_ARMORER_CHESTPLATE_5 = provider("savanna_armorer_chestplate_5");
    public static final ResourceKey<EnchantmentProvider> TRADES_SAVANNA_ARMORER_HELMET_4 = provider("savanna_armorer_helmet_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_SAVANNA_ARMORER_HELMET_5 = provider("savanna_armorer_helmet_5");
    public static final ResourceKey<EnchantmentProvider> TRADES_SAVANNA_ARMORER_LEGGINGS_4 = provider("savanna_armorer_leggings_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_SNOW_ARMORER_BOOTS_4 = provider("snow_armorer_boots_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_SNOW_ARMORER_BOOTS_5 = provider("snow_armorer_boots_5");
    public static final ResourceKey<EnchantmentProvider> TRADES_SNOW_ARMORER_HELMET_4 = provider("snow_armorer_helmet_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_SNOW_ARMORER_HELMET_5 = provider("snow_armorer_helmet_5");
    public static final ResourceKey<EnchantmentProvider> TRADES_SWAMP_ARMORER_BOOTS_4 = provider("swamp_armorer_boots_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_SWAMP_ARMORER_BOOTS_5 = provider("swamp_armorer_boots_5");
    public static final ResourceKey<EnchantmentProvider> TRADES_SWAMP_ARMORER_CHESTPLATE_4 = provider("swamp_armorer_chestplate_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_SWAMP_ARMORER_HELMET_4 = provider("swamp_armorer_helmet_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_SWAMP_ARMORER_HELMET_5 = provider("swamp_armorer_helmet_5");
    public static final ResourceKey<EnchantmentProvider> TRADES_SWAMP_ARMORER_LEGGINGS_4 = provider("swamp_armorer_leggings_4");
    public static final ResourceKey<EnchantmentProvider> TRADES_TAIGA_ARMORER_CHESTPLATE_5 = provider("taiga_armorer_chestplate_5");
    public static final ResourceKey<EnchantmentProvider> TRADES_TAIGA_ARMORER_LEGGINGS_5 = provider("taiga_armorer_leggings_5");

    public static final TagKey<Enchantment> TRADES_DESERT_SPECIAL = special("desert");
    public static final TagKey<Enchantment> TRADES_JUNGLE_SPECIAL = special("jungle");
    public static final TagKey<Enchantment> TRADES_PLAINS_SPECIAL = special("plains");
    public static final TagKey<Enchantment> TRADES_SAVANNA_SPECIAL = special("savanna");
    public static final TagKey<Enchantment> TRADES_SNOW_SPECIAL = special("snow");
    public static final TagKey<Enchantment> TRADES_SWAMP_SPECIAL = special("swamp");
    public static final TagKey<Enchantment> TRADES_TAIGA_SPECIAL = special("taiga");

    private TradeKeys() {
    }

    private static ResourceKey<EnchantmentProvider> provider(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT_PROVIDER, Identifier.withDefaultNamespace("trades/" + name));
    }

    private static TagKey<Enchantment> special(String biome) {
        return TagKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace("trades/" + biome + "_special"));
    }
}
