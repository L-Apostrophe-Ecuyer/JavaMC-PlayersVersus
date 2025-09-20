package frootloops.versus.mod.items_and_effects.brewing;


import frootloops.versus.mod.items_and_effects.brewing.effects.*;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import static frootloops.versus.VersusMod.MOD_ID;

public abstract class CustomStatusEffects {

    public static RegistryEntry<StatusEffect> LARGENESS;
    public static RegistryEntry<StatusEffect> SMALLNESS;
    public static RegistryEntry<StatusEffect> VULNERABILITY;
    public static RegistryEntry<StatusEffect> HAUNTING;
    public static RegistryEntry<StatusEffect> BUOYANCY;

    public static void registerCustomStatusEffects() {
        VULNERABILITY = registerCustomEffect("vulnerability", new VulnerabilityStatusEffect("vulnerability"));
        LARGENESS = registerCustomEffect("largeness", new LargenessStatusEffect("largeness"));
        SMALLNESS = registerCustomEffect("smallness", new SmallnessStatusEffect("smallness"));
        BUOYANCY = registerCustomEffect("buoyancy", new BuoyancyStatusEffect("buoyancy"));
        HAUNTING = registerCustomEffect("haunting", new HauntingStatusEffect());
    }

    private static RegistryEntry<StatusEffect> registerCustomEffect(String name, StatusEffect effect) {
        Registry.register(Registries.STATUS_EFFECT, Identifier.of(MOD_ID, name), effect);
        return Registries.STATUS_EFFECT.getEntry(effect);
    }

    public static RegistryEntry<StatusEffect> registerOverhauledVanillaEffect(String name, StatusEffect effect) {
        Registry.register(Registries.STATUS_EFFECT, Identifier.of("minecraft", name), effect);
        return Registries.STATUS_EFFECT.getEntry(effect);
    }

}