package frootloops.mobs.mixin.environment;

import net.minecraft.block.entity.SculkShriekerWarningManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SculkShriekerWarningManager.class)
public class SculkShriekerMixin {

    @ModifyConstant(method = "increaseWarningLevel()V", constant = @Constant(intValue = 1))
    private int increaseWarningLevel(int value) {
        return value + (int)Math.ceil(Math.random());
    }

}
