package frootloops.versus.mod.items.brewing;

import frootloops.versus.VersusMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.List;

import static frootloops.versus.VersusMod.MOD_ID;
import static frootloops.versus.mod.items.ItemsAndStacks.MAX_POTION_STACK_SIZE;

public abstract class CustomPotions {

    private static boolean werePotionsRegistered = false;

    public static RegistryEntry<Potion> LARGENESS, LARGENESS_LONG, LARGENESS_STRONG;
    public static RegistryEntry<Potion> SMALLNESS, SMALLNESS_LONG, SMALLNESS_STRONG;
    public static RegistryEntry<Potion> VULNERABILITY, VULNERABILITY_LONG, VULNERABILITY_STRONG;
    public static RegistryEntry<Potion> BUOYANCY, BUOYANCY_LONG, BUOYANCY_STRONG;
    public static RegistryEntry<Potion> HASTE, HASTE_LONG, HASTE_STRONG;
    public static RegistryEntry<Potion> MINING_FATIGUE, MINING_FATIGUE_LONG, MINING_FATIGUE_STRONG;
    public static RegistryEntry<Potion> DARKNESS, DARKNESS_LONG, DARKNESS_STRONG;
    public static RegistryEntry<Potion> GLOWING, GLOWING_LONG, GLOWING_STRONG;
    public static RegistryEntry<Potion> DECAY, DECAY_LONG, DECAY_STRONG;
    public static RegistryEntry<Potion> HAUNTING, HAUNTING_SHORT;

    public static PotionItem BOTTLE_OF_ENDER;
    public static SplashPotionItem SPLASH_BOTTLE_OF_ENDER;
    public static LingeringPotionItem LINGERING_BOTTLE_OF_ENDER;

    public static void registerCustomPotions() {
        if(werePotionsRegistered) return;
        werePotionsRegistered = true;

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

        HASTE = registerCustomPotion("haste", StatusEffects.HASTE, 0, 3000);
        HASTE_LONG = registerCustomPotion("haste_long", StatusEffects.HASTE, 0, 7200);
        HASTE_STRONG = registerCustomPotion("haste_strong", StatusEffects.HASTE, 1, 1800);

        MINING_FATIGUE = registerCustomPotion("mining_fatigue", StatusEffects.MINING_FATIGUE, 0, 3000);
        MINING_FATIGUE_LONG = registerCustomPotion("mining_fatigue_long", StatusEffects.MINING_FATIGUE, 0, 7200);
        MINING_FATIGUE_STRONG = registerCustomPotion("mining_fatigue_strong", StatusEffects.MINING_FATIGUE, 1, 1800);

        DARKNESS = registerCustomPotion("darkness", StatusEffects.DARKNESS, 0, 480);
        DARKNESS_LONG = registerCustomPotion("darkness_long", StatusEffects.DARKNESS, 0, 720);
        DARKNESS_STRONG = registerCustomPotion("darkness_strong", new Potion(new StatusEffectInstance[]{new StatusEffectInstance(StatusEffects.DARKNESS, 360), new StatusEffectInstance(StatusEffects.BLINDNESS, 80)}));

        GLOWING = registerCustomPotion("glowing", StatusEffects.GLOWING, 0, 3000);
        GLOWING_LONG = registerCustomPotion("glowing_long", StatusEffects.GLOWING, 0, 7200);
        GLOWING_STRONG = registerCustomPotion("glowing_strong", new Potion(new StatusEffectInstance[]{new StatusEffectInstance(StatusEffects.GLOWING, 800), new StatusEffectInstance(StatusEffects.NIGHT_VISION, 800)}));

        DECAY = registerCustomPotion("decay", StatusEffects.WITHER, 0, 240);
        DECAY_LONG = registerCustomPotion("decay_long", StatusEffects.WITHER, 0, 360);
        DECAY_STRONG = registerCustomPotion("decay_strong", StatusEffects.WITHER, 1, 160);

        HAUNTING = registerCustomPotion("haunting", CustomStatusEffects.HAUNTING, 0, 320);
        HAUNTING_SHORT = registerCustomPotion("haunting_short", CustomStatusEffects.HAUNTING, 0, 120);
        BOTTLE_OF_ENDER = new PotionItem(new Item.Settings().maxCount(MAX_POTION_STACK_SIZE).component(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(CustomPotions.HAUNTING)));
        SPLASH_BOTTLE_OF_ENDER = new SplashPotionItem(new Item.Settings().maxCount(1).component(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(CustomPotions.HAUNTING_SHORT)));
        LINGERING_BOTTLE_OF_ENDER = new LingeringPotionItem(new Item.Settings().maxCount(1).component(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(CustomPotions.HAUNTING_SHORT)));
    }

    private static RegistryEntry<Potion> registerCustomPotion(String name, RegistryEntry<StatusEffect> effect, int amplifier, int duration) {
        return registerCustomPotion(name, new Potion(new StatusEffectInstance(effect, duration, amplifier)));
    }

    private static RegistryEntry<Potion> registerCustomPotion(String name, Potion customPotion) {
        Registry.register(Registries.POTION, Identifier.of(MOD_ID, name), customPotion);
        RegistryEntry<Potion> entry = Registries.POTION.getEntry(customPotion);
        if(entry == null) VersusMod.MOD_LOGGER.error("ERROR: Potion Entry for '" + name + "' was returned as null by the registry upon launching the game.");
        return entry;
    }

}