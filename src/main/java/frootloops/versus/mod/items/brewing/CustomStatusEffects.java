package frootloops.versus.mod.items.brewing;


import frootloops.versus.mod.items.brewing.effects.BignessStatusEffect;
import frootloops.versus.mod.items.brewing.effects.SmallnessStatusEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static frootloops.versus.VersusMod.MOD_ID;

public abstract class CustomStatusEffects {

    public static void registerCustomStatusEffects() {
        registerCustomEffect(new BignessStatusEffect("bigness"), "bigness");
        registerCustomEffect(new SmallnessStatusEffect("smallness"), "smallness");
    }

    private static StatusEffect registerCustomEffect(StatusEffect effect, String name) {
        return Registry.register(Registries.STATUS_EFFECT, Identifier.of(MOD_ID, name), effect);
    }

}