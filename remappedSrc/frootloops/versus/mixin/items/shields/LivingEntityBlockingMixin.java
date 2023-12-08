package frootloops.versus.mixin.items.shields;

import frootloops.versus.mod.enchantments.Enchants;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.world.ServerWorld;
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

    private static final int PARRY_TIME_TICKS = 6;

    @Inject(method = "handleStatus", at = @At("HEAD"), cancellable = true)
    private void blockingSound(byte status, CallbackInfo info) {
        if(status == 29) {
            if(this.activeItemStack.getItem() instanceof SwordItem) {
                this.playSound(SoundEvents.ITEM_AXE_SCRAPE, 0.3F, 0.6F + this.getWorld().random.nextFloat() * 0.4F);
                this.playSound(SoundEvents.BLOCK_NETHERITE_BLOCK_PLACE, 1.2F, 0.8F + this.getWorld().random.nextFloat() * 0.4F);
                this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.3F, 0.8F + this.getWorld().random.nextFloat() * 0.4F);
            }
            else this.playSound(SoundEvents.ITEM_SHIELD_BLOCK, 1.0F, 0.8F + this.getWorld().random.nextFloat() * 0.4F);
            info.cancel();
        }
    }

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
        if (this.getType() != EntityType.PLAYER) {
            this.playSound(SoundEvents.ITEM_SHIELD_BLOCK, 1.0F, 0.8F + this.getWorld().random.nextFloat() * 0.4F);
            if (attacker.disablesShield()) {
                if (this.getWorld() instanceof ServerWorld) {

                    // Drop the shield

                    ItemStack shieldItemStack = ((LivingEntity) ((Object) this)).getOffHandStack();
                    ItemEntity itemEntity = new ItemEntity(this.getWorld(), this.getX(), this.getY(), this.getZ(), shieldItemStack.copy());
                    itemEntity.setPickupDelay(40);
                    this.getWorld().spawnEntity(itemEntity);
                    shieldItemStack.setCount(0);

                    // Stop blocking
                    this.setPose(EntityPose.STANDING);
                    ((LivingEntity) ((Object) this)).stopUsingItem();
                }
                this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.8F, 0.8F + this.getWorld().random.nextFloat() * 0.4F);
            }
        }
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void saveDamageAmount(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        damageAmount = amount;
    }

    @ModifyVariable(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSource;isIn(Lnet/minecraft/registry/tag/TagKey;)Z", ordinal = 1), argsOnly = true)
    private float reduceDamageIfBlocked(float amount2, DamageSource source, float amount) {
        if( activeItemStack.getItem() instanceof ShieldItem) return 0.0f;

        int levelRiposte = EnchantmentHelper.getLevel(Enchants.RIPOSTE, activeItemStack);
        int useTime =  activeItemStack.getMaxUseTime() - itemUseTimeLeft;
        boolean hasParried = useTime < PARRY_TIME_TICKS + levelRiposte && useTime > 0;
        if(hasParried) return 0;

        float damageAfterBlocking = Math.max(damageAmount/2.0f, damageAmount - 5.0f);
        return Math.max(0.0f, damageAfterBlocking - levelRiposte);
    }

    @Inject(method = "damage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/damage/DamageSource;getSource()Lnet/minecraft/entity/Entity;"))
    public void shieldBlockingLogic(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        ItemStack shieldItemStack = activeItemStack;
        Item shieldItem = activeItemStack.getItem();
        if(shieldItem.getUseAction(activeItemStack) != UseAction.BLOCK) {
            shieldItemStack = ((LivingEntity)((Object)this)).getOffHandStack();
            shieldItem = shieldItemStack.getItem();
        }

        // If your shield has thorns, deal some damage to the attacker
        int levelThorns = EnchantmentHelper.getLevel(Enchantments.THORNS, shieldItemStack);
        float reflectedDamage = 0.1F * damageAmount * levelThorns;

        // If your shield has riposte, deal some damage to the attacker, on parrying
        int levelRiposte = EnchantmentHelper.getLevel(Enchants.RIPOSTE, shieldItemStack);
        float paryingDamage = levelRiposte > 0 ? 0.2F * damageAmount * levelRiposte : 0.0F;

        // If you blocked within 8 ticks of an attack, you reflect the attack back (partially)
        int useTime =  shieldItem.getMaxUseTime(shieldItemStack) - itemUseTimeLeft;
        boolean hasParried = useTime < PARRY_TIME_TICKS + levelRiposte && useTime > 0;
        if(hasParried) {
            if ((LivingEntity) (Object) this instanceof PlayerEntity player) player.getItemCooldownManager().set(shieldItem, PARRY_TIME_TICKS << 1);
            ((LivingEntity) ((Object) this)).clearActiveItem();
            if(paryingDamage > 0f) {
                reflectedDamage += paryingDamage;
                world.playSound(null, getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, this.getSoundCategory(), 1F, 1F);
            }
        }

        // Reflect damage back to attacker, in cases of thorns or parries:
        double knockbackStrength = hasParried ? (paryingDamage > 0.0f ? 0.8 : 0.6) : 0.4;
        if((hasParried || reflectedDamage > 0.0f) && source.getSource() instanceof LivingEntity attacker && !attacker.equals(this)) {
            if(reflectedDamage > 0 && source.getName() != "thorns") {
                if ((LivingEntity) (Object) this instanceof PlayerEntity player) attacker.damage(this.getDamageSources().playerAttack(player), reflectedDamage);
                else attacker.damage(this.getDamageSources().mobAttack((LivingEntity) ((Object)this)), reflectedDamage);
            }
            attacker.takeKnockback(knockbackStrength, this.getX() - attacker.getX(), this.getZ() - attacker.getZ());
        }
    }
}