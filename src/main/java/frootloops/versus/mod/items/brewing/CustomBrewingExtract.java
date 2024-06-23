package frootloops.versus.mod.items.brewing;

import frootloops.versus.mod.Combat;
import frootloops.versus.mod.items.equipment.copper.CopperToolMaterial;
import net.minecraft.item.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

import static frootloops.versus.VersusMod.MOD_ID;

public abstract class CustomBrewingExtract {

    public static final Item POWDER_OF_CORRUPTION = new Item(new Item.Settings());
    public static final Item POWDER_OF_OMEN = new Item(new Item.Settings());

    public static final Item POWDER_OF_HEALTH = new Item(new Item.Settings());
    public static final Item POWDER_OF_HARM = new Item(new Item.Settings());

    public static final Item POWDER_OF_REGENERATION = new Item(new Item.Settings());
    public static final Item POWDER_OF_POISON = new Item(new Item.Settings());

    public static final Item POWDER_OF_TOUGHNESS = new Item(new Item.Settings());
    public static final Item POWDER_OF_VULNERABILITY = new Item(new Item.Settings()); // New potion!

    public static final Item POWDER_OF_LEAPING = new Item(new Item.Settings());
    public static final Item POWDER_OF_SLOW_FALL = new Item(new Item.Settings());

    public static final Item POWDER_OF_SPEED = new Item(new Item.Settings());
    public static final Item POWDER_OF_SLOWNESS = new Item(new Item.Settings());

    public static final Item POWDER_OF_BREATH = new Item(new Item.Settings());
    public static final Item POWDER_OF_DROWN = new Item(new Item.Settings()); // New potion!

    public static final Item POWDER_OF_SWIMMING = new Item(new Item.Settings()); // New potion!
    public static final Item POWDER_OF_SINKING = new Item(new Item.Settings()); // New potion!

    public static final Item POWDER_OF_LARGENESS = new Item(new Item.Settings()); // New potion!
    public static final Item POWDER_OF_SMALLNESS = new Item(new Item.Settings()); // New potion!

    public static final Item POWDER_OF_INVISIBILITY = new Item(new Item.Settings());
    public static final Item POWDER_OF_GLOWING = new Item(new Item.Settings()); // New potion!

    public static final Item POWDER_OF_STRENGTH = new Item(new Item.Settings());
    public static final Item POWDER_OF_WEAKNESS = new Item(new Item.Settings());

    public static final Item POWDER_OF_FLAME = new Item(new Item.Settings());
    public static final Item POWDER_OF_BURNING = new Item(new Item.Settings()); // New potion!

    public static final Item POWDER_OF_WIND = new Item(new Item.Settings());
    public static final Item POWDER_OF_OOZE = new Item(new Item.Settings());
    public static final Item POWDER_OF_INFESTATION = new Item(new Item.Settings());
    public static final Item POWDER_OF_WEAVING = new Item(new Item.Settings());

}