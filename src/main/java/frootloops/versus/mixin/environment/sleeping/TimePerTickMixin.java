package frootloops.versus.mixin.environment.sleeping;

import frootloops.versus.ServerSettings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(MinecraftServer.class)
public class TimePerTickMixin {

    @ModifyConstant(method = "runServer", constant = @Constant(longValue = 50L))
    private long modifyTimePerTick(long millisecondsPerTick) {
        return ServerSettings.isFastForwarding ? 5L: 50L;
    }

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 6000))
    private int modifyTicksPerAutosave(int millisecondsPerTick) {
        return ServerSettings.isFastForwarding ? 60000: 6000;
    }
}
