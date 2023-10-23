package frootloops.versus.mixin.hostile_mobs;

import frootloops.versus.mod.hostile_mobs.creeper.CreeperDecisionHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.CreeperIgniteGoal;
import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreeperIgniteGoal.class)
public class CreeperIgniteMixin {
    @Shadow @Final private CreeperEntity creeper;

    @Inject(at=@At("RETURN"), method="canStart", cancellable = true)
    private void onCanStart(CallbackInfoReturnable<Boolean> cir) {
        CreeperEntity peeper = this.creeper;
        LivingEntity seer = peeper.getTarget();

        //Always continue when already exploding, but never when no target
        if (peeper.getFuseSpeed() > 0) {cir.setReturnValue(true); return; }
        if (seer == null) {cir.setReturnValue(false); return; }

        //If method thinks creeper can start exploding, check if the player can see it
        if (cir.getReturnValue() && !CreeperDecisionHelper.canSee(seer, peeper)) {
            cir.setReturnValue(false);
            return;
        }

        //If method thinks creeper can't start exploding, check if the creeper can try breaching
        if (!cir.getReturnValue() && CreeperDecisionHelper.shouldBreach(seer, peeper)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(at=@At("RETURN"), method="tick", cancellable = true)
    private void onTick(CallbackInfo ci) {
        CreeperEntity breacher = this.creeper;
        LivingEntity target = breacher.getTarget();

        //If creeper should breach, fuse speed should be 1
        if (target != null && CreeperDecisionHelper.shouldBreach(target, breacher)) {
            breacher.setFuseSpeed(1);
        }
    }
}