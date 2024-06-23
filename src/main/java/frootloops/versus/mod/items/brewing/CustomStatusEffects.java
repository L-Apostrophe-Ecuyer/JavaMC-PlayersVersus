package frootloops.versus.mod.items.brewing;


import frootloops.versus.mod.items.brewing.effects.LargenessStatusEffect;
import frootloops.versus.mod.items.brewing.effects.PhantomStatusEffect;
import frootloops.versus.mod.items.brewing.effects.SmallnessStatusEffect;
import frootloops.versus.mod.items.brewing.effects.VulnerabilityStatusEffect;
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

    public static void registerCustomStatusEffects() {
        LARGENESS = registerCustomEffect(new LargenessStatusEffect("largeness"), "bigness");
        SMALLNESS = registerCustomEffect(new SmallnessStatusEffect("smallness"), "smallness");
        VULNERABILITY = registerCustomEffect(new VulnerabilityStatusEffect("vulnerability"), "vulnerability");
        HAUNTING = registerCustomEffect(new PhantomStatusEffect(), "haunting");
    }

    private static RegistryEntry<StatusEffect> registerCustomEffect(StatusEffect effect, String name) {
        Registry.register(Registries.STATUS_EFFECT, Identifier.of(MOD_ID, name), effect);
        return Registries.STATUS_EFFECT.getEntry(effect);
    }

}