package frootloops.mobs.mixin;

import net.minecraft.entity.ai.brain.task.SonicBoomTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SonicBoomTask.class)
public class WardenSonicBoomMixin {

    private static final double NEW_RANGE_HORIZONTAL = 6.0d, NEW_RANGE_VERTICAL = 8.0d;

    @ModifyConstant(method = "keepRunning()V", constant = @Constant(doubleValue = 15.0))
    private double newHorizontalRange(double value) {
        return NEW_RANGE_HORIZONTAL;
    }

    @ModifyConstant(method = "keepRunning()V", constant = @Constant(doubleValue = 20.0))
    private double newVerticalRange(double value) {
        return NEW_RANGE_VERTICAL;
    }

}
