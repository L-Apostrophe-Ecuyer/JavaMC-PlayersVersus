package frootloops.versus.mixin.client.mobs.hostile;

import frootloops.versus.mod.mobs.hostile.end.WatchlingLureView;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every frame the mouse turns the view, a hunting watchling's gaze turns it a little too (WatchlingLureView). */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerLureMixin {

    @Inject(method = "turnPlayer", at = @At("TAIL"))
    private void playersVersus$watchlingLure(double movementTime, CallbackInfo info) {
        WatchlingLureView.frame(movementTime);
    }
}
