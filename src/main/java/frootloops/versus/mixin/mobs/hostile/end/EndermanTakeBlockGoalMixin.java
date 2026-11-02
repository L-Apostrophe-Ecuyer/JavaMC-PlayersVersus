package frootloops.versus.mixin.mobs.hostile.end;

import frootloops.versus.mod.mobs.hostile.end.EndermanShulkerCarrier;
import net.minecraft.world.entity.monster.Enderman;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** An enderman with a shulker in its arms (EndermanShulkerCarrier) has no hand free for a block. */
@Mixin(targets = "net.minecraft.world.entity.monster.Enderman$EndermanTakeBlockGoal")
public abstract class EndermanTakeBlockGoalMixin {
    @Shadow @Final private Enderman enderman;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void playersVersus$handsFull(CallbackInfoReturnable<Boolean> info) {
        if (EndermanShulkerCarrier.carries(this.enderman)) info.setReturnValue(false);
    }
}
