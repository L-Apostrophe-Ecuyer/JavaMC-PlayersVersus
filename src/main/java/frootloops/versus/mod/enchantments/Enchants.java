package frootloops.versus.mod.enchantments;

import frootloops.versus.VersusMod;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Iterator;
import java.util.Optional;


public abstract class Enchants {

    private static RegistryEntryLookup enchRegistryLookup = null;

    public static RegistryEntry<Enchantment> getRegistryEntry(World world, RegistryKey<Enchantment> enchantment) {
        if(enchRegistryLookup == null) enchRegistryLookup = world.getRegistryManager().createRegistryLookup().getOrThrow(RegistryKeys.ENCHANTMENT);
        Optional<RegistryEntry.Reference<Enchantment>> enchantmentEntry = enchRegistryLookup.getOptional(enchantment);
        if(enchantmentEntry.isPresent()) return enchantmentEntry.get();
        else {
            enchRegistryLookup = world.getRegistryManager().createRegistryLookup().getOrThrow(RegistryKeys.ENCHANTMENT);
            return enchRegistryLookup.getOrThrow(enchantment);
        }
    }

    public static int getLevel(World world, ItemStack stack, RegistryKey<Enchantment> enchantment) {
        if(!stack.hasEnchantments()) return 0;
        return EnchantmentHelper.getLevel(getRegistryEntry(world, enchantment), stack);
    }

    public static int getLevel(ItemStack stack, RegistryEntry<Enchantment> enchantmentRegistryEntry) {
        ItemEnchantmentsComponent itemEnchantmentsComponent = stack.get(DataComponentTypes.ENCHANTMENTS);
        if(itemEnchantmentsComponent == null || itemEnchantmentsComponent.isEmpty()) return 0;
        return itemEnchantmentsComponent.getLevel(enchantmentRegistryEntry);
    }

    public static boolean hasEnchantment(ItemStack stack, RegistryKey<Enchantment> enchantment) {
        if(!stack.hasEnchantments()) return false;
        Iterator<RegistryEntry<Enchantment>> iterator = stack.getEnchantments().getEnchantments().iterator();
        while (iterator.hasNext()) {
            RegistryEntry<Enchantment> enchant = iterator.next();
            if (enchant.getKey().get() == enchantment) return true;
        }
        return false;
    }

    public static int getEquipmentLevel(World world, LivingEntity user, RegistryKey<Enchantment> enchantment) {
        return EnchantmentHelper.getEquipmentLevel(getRegistryEntry(world, enchantment), user);
    }
}
