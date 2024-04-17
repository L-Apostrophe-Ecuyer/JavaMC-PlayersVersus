package frootloops.versus.mixin.mobs.hostile.overworld;

import net.minecraft.entity.ai.brain.task.SniffTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SniffTask.class)
public abstract class WardenSniffMixin {

    @ModifyConstant(method = "finishRunning", constant = @Constant(doubleValue = 20.0))
    private double lessAngrySniffing(double verticalAngerRadius) {return 8.0;}

    @ModifyConstant(method = "finishRunning", constant = @Constant(doubleValue = 6.0))
    private double sniffDontBiff(double horizontalAngerRadius) {return 4.0;}
}
