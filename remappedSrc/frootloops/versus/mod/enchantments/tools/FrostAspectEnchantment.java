package frootloops.versus.mod.enchantments.tools;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.FireAspectEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;


public class FrostAspectEnchantment extends Enchantment {
    public FrostAspectEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.WEAPON, new EquipmentSlot[] {EquipmentSlot.MAINHAND});
    }

    public boolean isTreasure() {
        return true;
    }

    @Override
    public int getMinPower(int level) {
        return 10 + 12 * (level - 1);
    }

    @Override
    public int getMaxPower(int level) {
        return super.getMinPower(level) + 50;
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
        return !(other instanceof FrostAspectEnchantment || other instanceof FireAspectEnchantment);
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }

    @Override
    public void onTargetDamaged(LivingEntity user, Entity target, int level) {
        if (target instanceof LivingEntity targetEntity && targetEntity.canFreeze()) {
            user.method_48926().playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, user.getSoundCategory(), 1.0f, 1.0f);
            targetEntity.extinguish();

            // Minimum ticks to get damaged is 140, for most. ModEntities get rid of 2 FrozenTicks per tick.
            target.setFrozenTicks(target.getFrozenTicks() + 220);
            if(user.method_48926() instanceof ServerWorld serverWorld) {
                target.method_48926().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_HURT_FREEZE, target.getSoundCategory(), 1.0f, 1.0f);
                serverWorld.spawnParticles(ParticleTypes.WAX_OFF, target.getX(), target.getY() + 1, target.getZ(), 4, 0.2, 0.2, 0.2, 6.0f);
            }
        }
    }
}