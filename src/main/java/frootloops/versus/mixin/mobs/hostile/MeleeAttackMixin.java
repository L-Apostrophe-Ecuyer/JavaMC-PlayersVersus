package frootloops.versus.mixin.mobs.hostile;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import frootloops.versus.mod.mobs.melee.MobMelee;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Brain mobs (piglins, brutes, hoglins, zoglins, the warden, the creaking) swing like goal mobs do ({@link MobMelee}):
 * where the brain's MeleeAttack would swing and hit at once, a hostile mob starts a swing that winds up and strikes
 * later, and the attack cooldown waits for it. {@code lambda$create$3} is the behaviour's trigger.
 */
@Mixin(MeleeAttack.class)
public abstract class MeleeAttackMixin {

    @WrapOperation(method = "lambda$create$3", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;swingForAttack(Lnet/minecraft/world/InteractionHand;)V"))
    private static void playersVersus$swingAtTheStrike(Mob mob, InteractionHand hand, Operation<Void> original) {
        if (!MobMelee.windsUp(mob)) original.call(mob, hand);
    }

    @WrapOperation(method = "lambda$create$3", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;doHurtTarget(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean playersVersus$windUp(Mob mob, ServerLevel level, Entity target, Operation<Boolean> original) {
        if (!MobMelee.windsUp(mob) || !(target instanceof LivingEntity livingTarget)) return original.call(mob, level, target);
        MobMelee.startSwing(mob, livingTarget);
        return false;
    }

    @SuppressWarnings("rawtypes")
    @WrapOperation(method = "lambda$create$3", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;setWithExpiry(Ljava/lang/Object;J)V"))
    private static void playersVersus$coolDownAfterTheSwing(MemoryAccessor cooldown, Object value, long ticks, Operation<Void> original, @Local(argsOnly = true) Mob mob) {
        original.call(cooldown, value, ticks + MobMelee.swingTicksLeft(mob));
    }
}
