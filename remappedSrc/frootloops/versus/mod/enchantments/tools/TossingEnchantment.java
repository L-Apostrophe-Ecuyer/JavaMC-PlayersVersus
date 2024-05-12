package frootloops.versus.mod.enchantments.tools;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvents;

import java.util.Optional;


public class TossingEnchantment extends Enchantment {
    public TossingEnchantment() {
        super(
                Enchantment.properties(ItemTags.SHOVELS, 4, 2,
                        Enchantment.leveledCost(5, 10),
                        Enchantment.leveledCost(10, 10), 2,
                        EquipmentSlot.MAINHAND)
        );
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
        target.method_48926().playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, user.getSoundCategory(), 1.2f, 1.2f);
        target.method_48926().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, user.getSoundCategory(), 1.0f, 1.0f);
    }
}