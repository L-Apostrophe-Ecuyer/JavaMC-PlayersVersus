package frootloops.versus.mixin.players;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntitySprintingMixin {

    @ModifyConstant(method = "tickMovement", constant = @Constant(floatValue = 6.0f))
    private float sprintingNeverDisables(float threeHaunches) {
        return 0.0f;
    }
}
