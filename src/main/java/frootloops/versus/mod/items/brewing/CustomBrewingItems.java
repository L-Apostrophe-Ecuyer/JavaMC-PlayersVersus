package frootloops.versus.mod.items.brewing;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;

public abstract class CustomBrewingItems {

    public static final Item CONCENTRATE_OF_UNDYING = new ConcentrateItem(StatusEffects.WITHER, 2, 80);
    public static final Item CONCENTRATE_OF_OMEN = new ConcentrateItem(StatusEffects.UNLUCK);

    public static final Item CONCENTRATE_OF_HEALTH = new ConcentrateItem(StatusEffects.INSTANT_HEALTH);
    public static final Item CONCENTRATE_OF_HARM = new ConcentrateItem(StatusEffects.INSTANT_DAMAGE);

    public static final Item CONCENTRATE_OF_REGENERATION = new ConcentrateItem(StatusEffects.INSTANT_DAMAGE);
    public static final Item CONCENTRATE_OF_POISON = new ConcentrateItem(StatusEffects.POISON, 0, 200);

    public static final Item CONCENTRATE_OF_MINING_SPEED = new ConcentrateItem(StatusEffects.HASTE);            // New potion!
    public static final Item CONCENTRATE_OF_MINING_FATIGUE = new ConcentrateItem(StatusEffects.MINING_FATIGUE); // New potion!

    public static final Item CONCENTRATE_OF_TOUGHNESS = new ConcentrateItem(StatusEffects.RESISTANCE);
    public static final Item CONCENTRATE_OF_VULNERABILITY = new ConcentrateItem(CustomStatusEffects.VULNERABILITY); // New potion!

    public static final Item CONCENTRATE_OF_VISION = new ConcentrateItem(StatusEffects.NIGHT_VISION);
        public static final Item CONCENTRATE_OF_DARKNESS = new ConcentrateItem(StatusEffects.DARKNESS);         // New potion!

    public static final Item CONCENTRATE_OF_LEAPING = new ConcentrateItem(StatusEffects.JUMP_BOOST);
    public static final Item CONCENTRATE_OF_SLOW_FALL = new ConcentrateItem(StatusEffects.SLOW_FALLING);

    public static final Item CONCENTRATE_OF_SPEED = new ConcentrateItem(StatusEffects.SPEED);
    public static final Item CONCENTRATE_OF_SLOWNESS = new ConcentrateItem(StatusEffects.SLOWNESS);

    public static final Item CONCENTRATE_OF_BREATH = new ConcentrateItem(StatusEffects.WATER_BREATHING);
    public static final Item CONCENTRATE_OF_BUOYANCY = new ConcentrateItem(CustomStatusEffects.BUOYANCY);       // New potion & effect!

    public static final Item CONCENTRATE_OF_LARGENESS = new ConcentrateItem(CustomStatusEffects.LARGENESS);     // New potion & effect!
    public static final Item CONCENTRATE_OF_SMALLNESS = new ConcentrateItem(CustomStatusEffects.SMALLNESS);     // New potion & effect!

    public static final Item CONCENTRATE_OF_INVISIBILITY = new ConcentrateItem(StatusEffects.INVISIBILITY);
    public static final Item CONCENTRATE_OF_GLOWING = new ConcentrateItem(StatusEffects.GLOWING);

    public static final Item CONCENTRATE_OF_STRENGTH = new ConcentrateItem(StatusEffects.STRENGTH);
    public static final Item CONCENTRATE_OF_WEAKNESS = new ConcentrateItem(StatusEffects.WEAKNESS);

    public static final Item CONCENTRATE_OF_FIRE = new ConcentrateItem(StatusEffects.FIRE_RESISTANCE);

    public static final Item CONCENTRATE_OF_WIND = new ConcentrateItem(StatusEffects.WIND_CHARGED);
    public static final Item CONCENTRATE_OF_OOZE = new ConcentrateItem(StatusEffects.OOZING);
    public static final Item CONCENTRATE_OF_INFESTATION = new ConcentrateItem(StatusEffects.INFESTED);
    public static final Item CONCENTRATE_OF_WEAVING = new ConcentrateItem(StatusEffects.WEAVING);

}