package frootloops.versus.mixin.items_and_effects.blight;

import frootloops.versus.mod.items_and_effects.blight.Undeath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Withering turns the living pale (Undeath#wither): wither damage leaving a villager, a spider, an illager, a witch or a
 * wandering trader below 8 health turns it. Zombies and creepers turn pale the same way in their own mixins
 * (ZombieMixin, CreeperMixin); players die of it and rise pale (ServerPlayerWitheredMixin).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityWitheringMixin {

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void playersVersus$witheredTurnPale(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!info.getReturnValueZ() || !source.is(DamageTypes.WITHER) || self.getHealth() >= 8.0F || self.isRemoved()) return;
        if (self instanceof Player || self instanceof Zombie || self instanceof Creeper) return;
        Undeath.wither(level, self);
    }
}
