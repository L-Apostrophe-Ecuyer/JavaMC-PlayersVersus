package frootloops.versus.mixin.shields;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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

    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void isBlocking(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(!this.activeItemStack.isEmpty() && this.activeItemStack.getItem().getUseAction(this.activeItemStack) == UseAction.BLOCK);
    }

    @Inject(method = "blockedByShield", at = @At("HEAD"), cancellable = true)
    private void blockSonicBooms(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if(source.isOf(DamageTypes.SONIC_BOOM) && this.isBlocking()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "takeShieldHit", at = @At("TAIL"))
    private void shieldDisablingForMobs(LivingEntity attacker, CallbackInfo ci) {
        if (attacker.disablesShield() && this.getType() != EntityType.PLAYER) {
            this.world.sendEntityStatus(this, (byte)30);
            if(this.world instanceof ServerWorld) {

                // Drop the shield
                ItemStack shieldItemStack = ((LivingEntity) ((Object) this)).getOffHandStack();
                ItemEntity itemEntity = new ItemEntity(this.world, this.getX(), this.getY(), this.getZ(), shieldItemStack.copy());
                itemEntity.setPickupDelay(40);
                this.world.spawnEntity(itemEntity);
                shieldItemStack.setCount(0);

                // Stop blocking
                this.setPose(EntityPose.STANDING);
                ((LivingEntity) ((Object) this)).stopUsingItem();
            }
            this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.8F, 0.8F + this.world.random.nextFloat() * 0.4F);
        }
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void saveDamageAmount(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        damageAmount = amount;
    }

    @Inject(method = "damage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/damage/DamageSource;getSource()Lnet/minecraft/entity/Entity;"))
    public void shieldParry(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        ItemStack shieldItemStack = activeItemStack;
        Item shieldItem = activeItemStack.getItem();
        if(shieldItem.getUseAction(activeItemStack) != UseAction.BLOCK) {
            shieldItemStack = ((LivingEntity)((Object)this)).getOffHandStack();
            shieldItem = shieldItemStack.getItem();
        }

        // If your shield has thorns, deal some damage to the attacker
        boolean hasThorns = EnchantmentHelper.getLevel(Enchantments.THORNS, shieldItemStack) > 0;
        float reflectedDamage = hasThorns ? 1.0F : 0.0F;

        // If you blocked within 8 ticks of an attack, you reflect the attack back (partially)
        int useTime =  shieldItem.getMaxUseTime(shieldItemStack) - itemUseTimeLeft;
        boolean hasParried = useTime < PARRY_TIME_TICKS && useTime > 1;
        if(hasParried) {
            if ((LivingEntity) (Object) this instanceof PlayerEntity player) {
                player.getItemCooldownManager().set(shieldItem, PARRY_TIME_TICKS << 1);
                player.clearActiveItem();
                reflectedDamage += damageAmount * 0.2F;
                world.playSound(null, getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1F, 1F);
            }
            else if(useTime < PARRY_TIME_TICKS - 1 && useTime > 2) {
                ((LivingEntity) ((Object) this)).clearActiveItem();
                reflectedDamage += damageAmount * 0.1F;
                world.playSound(null, getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 0.5F, 0.4F);
            }
        }

        // Reflect damage back to attacker, in cases of thorns or parries:
        if(source.getName() == "thorns") return;
        if(reflectedDamage > 0 && source.getSource() instanceof LivingEntity attacker && !attacker.equals(this)) {
            if ((LivingEntity) (Object) this instanceof PlayerEntity player) attacker.damage(this.getDamageSources().playerAttack(player), reflectedDamage);
            else attacker.damage(this.getDamageSources().mobAttack((LivingEntity) ((Object)this)), reflectedDamage);
            attacker.takeKnockback(0.6, this.getX() - attacker.getX(), this.getZ() - attacker.getZ());
        }
    }
}