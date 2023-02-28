package frootloops.versus.mixin.players.shields;

import frootloops.versus.util.enchantments.Enchants;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityBlockingMixin extends LivingEntity {
    protected PlayerEntityBlockingMixin(EntityType<? extends LivingEntity> entityType, World world, ItemCooldownManager itemCooldownManager, LivingEntity attacker) {
        super(entityType, world);
        this.itemCooldownManager = itemCooldownManager;
    }

    private static final int PARRY_TIME_TICKS = 8;

    @Shadow
    private final ItemCooldownManager itemCooldownManager;


    @Inject(method = "disableShield", at = @At(value = "HEAD"), cancellable = true)
    private void disableShield(boolean sprinting, CallbackInfo info) {

        int disableForTicks = 20;
        if(!this.isSneaking()) disableForTicks += 40;
        if(this.getAttacker() != null)
            disableForTicks += 10 * EnchantmentHelper.getLevel(Enchants.CLEAVING, this.getAttacker().getMainHandStack());

        this.itemCooldownManager.set(Items.SHIELD, disableForTicks);
        this.clearActiveItem();
        this.world.sendEntityStatus(this, (byte)30);
        info.cancel();
    }


    @Inject(method = "applyDamage", at = @At(value = "HEAD"), cancellable = true)
    private void noDamageOnShieldParries(DamageSource source, float amount, CallbackInfo info) {
        if (!this.isInvulnerableTo(source) && this.blockedByShield(source)) {

            // If you blocked within 8 ticks of an attack, you take no damage:
            if (activeItemStack.getItem().getMaxUseTime(activeItemStack) - itemUseTimeLeft < PARRY_TIME_TICKS)
                info.cancel();
        }
    }
}