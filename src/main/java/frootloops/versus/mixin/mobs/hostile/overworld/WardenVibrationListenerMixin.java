package frootloops.versus.mixin.mobs.hostile.overworld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

// Use fully qualified name with '$' for inner class
@Mixin(targets = "net.minecraft.entity.mob.WardenEntity$VibrationCallback")
public abstract class WardenVibrationListenerMixin {

    @Overwrite
    public int getRange() {
        return 36;
    }
}