package frootloops.versus.mixin.items.equipment.shields;

import frootloops.versus.mod.enchantments.Enchants;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityBlockingMixin extends LivingEntity {
    protected PlayerEntityBlockingMixin(EntityType<? extends LivingEntity> entityType, World world, ItemCooldownManager itemCooldownManager, LivingEntity attacker) {
        super(entityType, world);
        this.itemCooldownManager = itemCooldownManager;
    }

    private static final int PARRY_TIME_TICKS = 8;

    @Shadow private final ItemCooldownManager itemCooldownManager;
    @Shadow public void incrementStat(Stat<?> stat) {}

    @Override
    public void damageShield(float amount) {
        if (!this.method_48926().isClient) {
            this.incrementStat(Stats.USED.getOrCreateStat(this.activeItemStack.getItem()));
        }

        if (amount >= 3.0F) {
            int i = 1 + MathHelper.floor(amount);
            Hand hand = this.getActiveHand();
            this.activeItemStack.damage(i, this, LivingEntity.getSlotForHand(hand));
            if (this.activeItemStack.isEmpty()) {
                if (hand == Hand.MAIN_HAND) {
                    this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                } else {
                    this.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                }

                this.activeItemStack = ItemStack.EMPTY;
                if (this.activeItemStack.isOf(Items.SHIELD)) this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.8F, 0.8F + this.method_48926().random.nextFloat() * 0.4F);
                if (this.activeItemStack.getItem() instanceof SwordItem) this.playSound(SoundEvents.ENTITY_ITEM_BREAK, 0.6F, 0.5F + this.method_48926().random.nextFloat() * 0.3F);
            }
        }
    }

    @Inject(method = "takeShieldHit", at = @At(value = "HEAD"), cancellable = false)
    protected void takeShieldHitMixin(LivingEntity attacker, CallbackInfo info) {
        if (activeItemStack.getItem() instanceof SwordItem)  ((PlayerEntity)((Object)this)).disableShield();

    }

    @Inject(method = "disableShield", at = @At(value = "HEAD"), cancellable = true)
    private void disableShieldMixin(CallbackInfo info) {

        int disableForTicks = 40;
        if(this.getAttacker() != null) {
            ItemStack mainhandStack = this.getAttacker().getMainHandStack();
            if(!mainhandStack.isEmpty() && mainhandStack.hasEnchantments()) {
                int cleaveLevel = Enchants.getLevel(method_48926(), mainhandStack, Enchants.CLEAVING);
                if (cleaveLevel > 0)  disableForTicks += 20 * cleaveLevel;
            }
        }

        this.itemCooldownManager.set(this.activeItemStack.getItem(), disableForTicks);
        this.clearActiveItem();
        this.method_48926().sendEntityStatus(this, (byte)30);
        info.cancel();
    }


    @Inject(method = "applyDamage", at = @At(value = "HEAD"), cancellable = true)
    private void noDamageOnShieldParries(DamageSource source, float amount, CallbackInfo info) {
        if (!this.isInvulnerableTo(source) && this.blockedByShield(source)) {

            // If you blocked within 8 ticks of an attack, you take no damage:
            if (activeItemStack.getItem().getMaxUseTime(activeItemStack, this) - itemUseTimeLeft < PARRY_TIME_TICKS)
                info.cancel();
        }
    }
}