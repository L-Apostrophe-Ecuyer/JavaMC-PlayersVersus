package frootloops.versus;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class ServerSettings {

    public static void onInitialize() {

    }

    public static boolean isTimeFastForwarding = false; // Accessed and modified by mixins in "environment.sleeping"

}
