package frootloops.versus.util.enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.sound.SoundEvents;


public class TossingEnchantment extends Enchantment {
    public TossingEnchantment() {
        super(Rarity.COMMON, EnchantmentTarget.WEAPON, new EquipmentSlot[] {EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinPower(int level) {
        return 5 + 10 * (level - 1);
    }

    @Override
    public int getMaxPower(int level) {
        return super.getMinPower(level) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof ShovelItem;
    }

    @Override
    public void onTargetDamaged(LivingEntity user, Entity target, int level) {
        if(!(user instanceof PlayerEntity) && user.isOnGround()) {
            performTossAttack(user, target, 0.1 + (double)level * 0.1);
        }
        super.onTargetDamaged(user, target, level);
    }

    public final static void performTossAttack(LivingEntity user, Entity target, double magnitude){
        target.addVelocity(0.0, magnitude, 0.0);
        target.getWorld().playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, user.getSoundCategory(), 1.2f, 1.2f);
        target.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, user.getSoundCategory(), 1.0f, 1.0f);
    }
}