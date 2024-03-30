package frootloops.versus.mod.enchantments.tools;

import frootloops.versus.mod.enchantments.Enchants;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;


public class FrostAspectEnchantment extends Enchantment {
    public FrostAspectEnchantment() {
        super(
                Enchantment.properties(ItemTags.AXES, ItemTags.SWORD_ENCHANTABLE, 3, 2,
                        Enchantment.leveledCost(10, 8),
                        Enchantment.leveledCost(18, 40), 1,
                        EquipmentSlot.MAINHAND)
        );
    }

    public boolean isTreasure() {
        return true;
    }

    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return false;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof AxeItem;
    }

    @Override
    public boolean canAccept(Enchantment other) {
        return !(other == Enchants.FROST_ASPECT || other == Enchantments.FIRE_ASPECT);
    }

    @Override
    public void onTargetDamaged(LivingEntity user, Entity target, int level) {
        if (target instanceof LivingEntity targetEntity && targetEntity.canFreeze()) {
            user.getWorld().playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, user.getSoundCategory(), 1.0f, 1.0f);
            targetEntity.extinguish();

            // Minimum ticks to get damaged is 140, for most. ModEntities get rid of 2 FrozenTicks per tick.
            target.setFrozenTicks(target.getFrozenTicks() + 220);
            if(user.getWorld() instanceof ServerWorld serverWorld) {
                target.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_HURT_FREEZE, target.getSoundCategory(), 1.0f, 1.0f);
                serverWorld.spawnParticles(ParticleTypes.WAX_OFF, target.getX(), target.getY() + 1, target.getZ(), 4, 0.2, 0.2, 0.2, 6.0f);
            }
        }
    }
}