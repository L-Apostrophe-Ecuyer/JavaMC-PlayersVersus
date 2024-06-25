package frootloops.versus.mod.items.brewing;

import frootloops.versus.VersusMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import static frootloops.versus.VersusMod.MOD_ID;
import static frootloops.versus.mod.items.ItemsAndStacks.MAX_POTION_STACK_SIZE;

public abstract class CustomPotions {

    public static RegistryEntry<Potion> LARGENESS;
    public static RegistryEntry<Potion> LARGENESS_LONG;
    public static RegistryEntry<Potion> LARGENESS_STRONG;

    public static RegistryEntry<Potion> SMALLNESS;
    public static RegistryEntry<Potion> SMALLNESS_LONG;
    public static RegistryEntry<Potion> SMALLNESS_STRONG;

    public static RegistryEntry<Potion> VULNERABILITY;
    public static RegistryEntry<Potion> VULNERABILITY_LONG;
    public static RegistryEntry<Potion> VULNERABILITY_STRONG;

    public static RegistryEntry<Potion> BUOYANCY;
    public static RegistryEntry<Potion> BUOYANCY_LONG;
    public static RegistryEntry<Potion> BUOYANCY_STRONG;

    public static RegistryEntry<Potion> HAUNTING;

    public static Item BOTTLE_OF_ENDER;

    public static void registerCustomPotions() {
        LARGENESS = registerCustomPotion( "largeness", CustomStatusEffects.LARGENESS, 0, 2400);
        SMALLNESS = registerCustomPotion("smallness", CustomStatusEffects.SMALLNESS, 0, 2400);

        LARGENESS_LONG = registerCustomPotion( "largeness_long", CustomStatusEffects.LARGENESS, 0, 4200);
        SMALLNESS_LONG = registerCustomPotion("smallness_long", CustomStatusEffects.SMALLNESS, 0, 4200);

        LARGENESS_STRONG = registerCustomPotion( "largeness_strong", CustomStatusEffects.LARGENESS, 1, 1200);
        SMALLNESS_STRONG = registerCustomPotion("smallness_strong", CustomStatusEffects.SMALLNESS, 1, 1200);

        VULNERABILITY = registerCustomPotion("vulnerability", CustomStatusEffects.VULNERABILITY, 0, 3600);
        VULNERABILITY_LONG = registerCustomPotion("vulnerability_long", CustomStatusEffects.VULNERABILITY, 0, 6400);
        VULNERABILITY_STRONG = registerCustomPotion("vulnerability_strong", CustomStatusEffects.VULNERABILITY, 0, 2400);

        BUOYANCY = registerCustomPotion("buoyancy", CustomStatusEffects.BUOYANCY, 0, 3000);
        BUOYANCY_LONG = registerCustomPotion("buoyancy_long", CustomStatusEffects.BUOYANCY, 0, 7200);
        BUOYANCY_STRONG = registerCustomPotion("buoyancy_strong", CustomStatusEffects.BUOYANCY, 1, 1800);

        HAUNTING = registerCustomPotion("haunting", CustomStatusEffects.HAUNTING, 0, 320);

        BOTTLE_OF_ENDER = new PotionItem(new Item.Settings().maxCount(MAX_POTION_STACK_SIZE).component(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(CustomPotions.HAUNTING)));
    }

    private static RegistryEntry<Potion> registerCustomPotion(String name, RegistryEntry<StatusEffect> effect, int amplifier, int duration) {
        Potion customPotion = new Potion(new StatusEffectInstance(effect, duration, amplifier));
        Registry.register(Registries.POTION, Identifier.of(MOD_ID, name), customPotion);
        RegistryEntry<Potion> entry = Registries.POTION.getEntry(customPotion);
        if(entry == null) VersusMod.MOD_LOGGER.error("ERROR: Potion Entry for '" + name + "' was returned as null by the registry upon launching the game.");
        return entry;
    }

}