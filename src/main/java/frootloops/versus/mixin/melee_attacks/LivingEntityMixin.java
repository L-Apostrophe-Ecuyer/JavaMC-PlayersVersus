package frootloops.versus.mixin.melee_attacks;

import com.google.common.collect.Maps;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    private final Map<StatusEffect, StatusEffectInstance> activeStatusEffects = Maps.newHashMap();

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void getHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        ItemStack mainHand = ((LivingEntity)((Object)this)).getMainHandStack();
        if(((LivingEntity)((Object)this)) instanceof PathAwareEntity && mainHand != null){
            if(mainHand.getItem() instanceof AxeItem)
                cir.setReturnValue(24);
            else if(mainHand.getItem() instanceof HoeItem)
                cir.setReturnValue(10);
            else
                cir.setReturnValue(16);
        }
    }

    @Inject(method = "damage", at = @At("TAIL"))
    private void modifyInvincibilityFrames(DamageSource source, float amount, CallbackInfoReturnable cir) {
        if(timeUntilRegen > 0 && source.getAttacker() instanceof LivingEntity){
            if(source.isProjectile())
                timeUntilRegen = 0;
            else {
                ItemStack mainHand = ((LivingEntity) source.getAttacker()).getMainHandStack();
                if (!source.isMagic() && mainHand != null && mainHand.getItem().isDamageable())
                    timeUntilRegen = 7;
                else
                    timeUntilRegen = 10;
            }
        }
    }
}
