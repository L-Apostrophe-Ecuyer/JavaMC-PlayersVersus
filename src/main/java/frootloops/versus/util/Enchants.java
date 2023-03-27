package frootloops.versus.util;

import frootloops.versus.VersusMod;
import frootloops.versus.util.enchantments.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;


public abstract class Enchants {


    public static final FrostAspectEnchantment FROST_ASPECT = new FrostAspectEnchantment();
    public static final TossingEnchantment TOSSING = new TossingEnchantment();
    public static final CleavingEnchantment CLEAVING = new CleavingEnchantment();
    public static final MagicProtectionEnchantment MAGIC_PROTECTION = new MagicProtectionEnchantment();
    public static final PhysicalProtectionEnchantment PHYSICAL_PROTECTION = new PhysicalProtectionEnchantment();
    public static void init(){
        Registry.register(Registries.ENCHANTMENT, new Identifier(VersusMod.MOD_ID, "frost_aspect"), FROST_ASPECT);
        Registry.register(Registries.ENCHANTMENT, new Identifier(VersusMod.MOD_ID, "tossing"), TOSSING);
        Registry.register(Registries.ENCHANTMENT, new Identifier(VersusMod.MOD_ID, "cleaving"), CLEAVING);
        Registry.register(Registries.ENCHANTMENT, new Identifier(VersusMod.MOD_ID, "magic_protection"), MAGIC_PROTECTION);
        Registry.register(Registries.ENCHANTMENT, new Identifier(VersusMod.MOD_ID, "physical_protection"), PHYSICAL_PROTECTION);
    }
}
