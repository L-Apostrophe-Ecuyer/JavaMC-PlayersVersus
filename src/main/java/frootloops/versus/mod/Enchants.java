package frootloops.versus.mod;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.enchantments.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;


public abstract class Enchants {

    public static final TossingEnchantment TOSSING = new TossingEnchantment();
    public static final CleavingEnchantment CLEAVING = new CleavingEnchantment();
    public static final RiposteEnchantment RIPOSTE = new RiposteEnchantment();
    public static final FrostAspectEnchantment FROST_ASPECT = new FrostAspectEnchantment();
    public static final EnderCurseEnchantment CURSE_OF_ENDER = new EnderCurseEnchantment();
    public static final BoundingStridesEnchantment BOUNDING_STRIDES = new BoundingStridesEnchantment();
    public static final MagicProtectionEnchantment MAGIC_PROTECTION = new MagicProtectionEnchantment();
    public static final PhysicalProtectionEnchantment PHYSICAL_PROTECTION = new PhysicalProtectionEnchantment();

    public static void init(){

        Registry.register(Registries.ENCHANTMENT, new Identifier(VersusMod.MOD_ID, "tossing"), TOSSING);
        Registry.register(Registries.ENCHANTMENT, new Identifier(VersusMod.MOD_ID, "cleaving"), CLEAVING);
        Registry.register(Registries.ENCHANTMENT, new Identifier(VersusMod.MOD_ID, "riposte"), RIPOSTE);
        Registry.register(Registries.ENCHANTMENT, new Identifier(VersusMod.MOD_ID, "frost_aspect"), FROST_ASPECT);
        Registry.register(Registries.ENCHANTMENT, new Identifier(VersusMod.MOD_ID, "ender_curse"), CURSE_OF_ENDER);
        Registry.register(Registries.ENCHANTMENT, new Identifier(VersusMod.MOD_ID, "bounding_strides"), BOUNDING_STRIDES);
        Registry.register(Registries.ENCHANTMENT, new Identifier(VersusMod.MOD_ID, "magic_protection"), MAGIC_PROTECTION);
        Registry.register(Registries.ENCHANTMENT, new Identifier(VersusMod.MOD_ID, "physical_protection"), PHYSICAL_PROTECTION);
    }
}
