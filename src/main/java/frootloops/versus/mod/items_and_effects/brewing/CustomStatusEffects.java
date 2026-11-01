package frootloops.versus.mod.items_and_effects.brewing;


import frootloops.versus.mod.items_and_effects.brewing.effects.*;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

import static frootloops.versus.VersusMod.MOD_ID;

public abstract class CustomStatusEffects {

    public static Holder<MobEffect> LARGENESS;
    public static Holder<MobEffect> SMALLNESS;
    public static Holder<MobEffect> VULNERABILITY;
    public static Holder<MobEffect> HAUNTING;
    public static Holder<MobEffect> BUOYANCY;
    /** Registered when this class loads: the concentrates' and biles' statics name it, whenever they load. */
    public static final Holder<MobEffect> BLIGHT = registerCustomEffect("blight", new BlightStatusEffect());

    public static void registerCustomStatusEffects() {
        VULNERABILITY = registerCustomEffect("vulnerability", new VulnerabilityStatusEffect("vulnerability"));
        LARGENESS = registerCustomEffect("largeness", new LargenessStatusEffect("largeness"));
        SMALLNESS = registerCustomEffect("smallness", new SmallnessStatusEffect("smallness"));
        BUOYANCY = registerCustomEffect("buoyancy", new BuoyancyStatusEffect("buoyancy"));
        HAUNTING = registerCustomEffect("haunting", new HauntingStatusEffect());
    }

    private static Holder<MobEffect> registerCustomEffect(String name, MobEffect effect) {
        Registry.register(BuiltInRegistries.MOB_EFFECT, Identifier.fromNamespaceAndPath(MOD_ID, name), effect);
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }

    public static Holder<MobEffect> registerOverhauledVanillaEffect(String name, MobEffect effect) {
        Registry.register(BuiltInRegistries.MOB_EFFECT, Identifier.fromNamespaceAndPath("minecraft", name), effect);
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }

}