package frootloops.versus.mixin.environment.sleeping;

import frootloops.versus.ServerSettings;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(MinecraftServer.class)
public class TimePerTickMixin {

    @ModifyConstant(method = "runServer", constant = @Constant(longValue = 50L))
    private long modifyTimePerTick(long millisecondsPerTick) {
        return ServerSettings.isTimeFastForwarding ? 1L: 50L;
    }

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 6000))
    private int modifyTicksPerAutosave(int millisecondsPerTickToAutosave) {
        return ServerSettings.isTimeFastForwarding ? 300000: 6000;
    }
}
