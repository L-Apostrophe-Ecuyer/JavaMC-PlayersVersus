package frootloops.versus.mixin.players.shields;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityBlockingMixin extends Entity {
    @Unique private float damageAmount;
    @Shadow protected int itemUseTimeLeft;
    @Shadow protected ItemStack activeItemStack;
    @Shadow public abstract boolean blockedByShield(DamageSource source);

    @Shadow public boolean isBlocking() {return false;}

    public LivingEntityBlockingMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    private static final int PARRY_TIME_TICKS = 8;


    @Inject(method = "blockedByShield", at = @At("HEAD"), cancellable = true)
    private void blockSonicBooms(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if(source.getName() == "sonic_boom" && this.isBlocking()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isBlocking", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;getMaxUseTime(Lnet/minecraft/item/ItemStack;)I"), cancellable = true)
    private void instantlyBlock(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }


    @Inject(method = "damage", at = @At("HEAD"))
    private void saveDamageAmount(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        damageAmount = amount;
    }

    @Inject(method = "damage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/damage/DamageSource;getSource()Lnet/minecraft/entity/Entity;"))
    public void shieldParry(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        Item shieldItem = activeItemStack.getItem();
        if((LivingEntity) (Object) this instanceof PlayerEntity player) {

            // If your shield has thorns, deal some damage to the attacker
            boolean hasThorns = EnchantmentHelper.getLevel(Enchantments.THORNS, activeItemStack) > 0;
            float reflectedDamage = hasThorns ? 1.0F : 0.0F;

            // If you blocked within 8 ticks of an attack, you reflect the attack back (partially)
            boolean hasParried = shieldItem.getMaxUseTime(activeItemStack) - itemUseTimeLeft < PARRY_TIME_TICKS;
            if(hasParried) {
                player.getItemCooldownManager().set(shieldItem, PARRY_TIME_TICKS << 1);
                player.clearActiveItem();
                reflectedDamage += damageAmount * 0.5F;
                world.playSound(null, getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1F, 1F);
            }

            // Reflect damage back to attacker, in cases of thorns or parries:
            if(source.getName() == "thorns") return;
            if(reflectedDamage > 0 && source.getSource() instanceof LivingEntity attacker && !attacker.equals(this)) {
                attacker.damage(this.getDamageSources().playerAttack(player), reflectedDamage);
                attacker.takeKnockback(0.6, this.getX() - attacker.getX(), this.getZ() - attacker.getZ());
            }
        }
    }
}